// WebSocketの設定
const createWebSocket = (path, statusElementId) => {
    let hasConnected = false; //初回起動時に接続できたかフラグ
    let socket = null;
    let nowRetryCount = 0; //リトライカウント
    const maxRetryCount = 12;
    let isReconnecting = true; //リトライフラグ Maxリトライを越えて
    let reconnectDelay = 3000; // 初期遅延
    const maxReconnectDelay = 60000; // 最大遅延
    
    const statusElement = document.getElementById(statusElementId);
    const indicator = statusElement.querySelector(".status-indicator");
    const statusText = statusElement.querySelector(".status-text");

    const updateStatus = (status, retryCount = null, reconnectDelay = null) => {
        indicator.className = `status-indicator ${status}`;
        let statusMessage = `${path.substring(1)}: ${status}`;
        if (retryCount !== null) {
            statusMessage += ` (${retryCount} retries next ${reconnectDelay}ms)`;
        }
        statusText.textContent = statusMessage;
    };

    const connect = () => {
        if (!isReconnecting) return;

        socket = new SockJS(`http://localhost:8890${path}`);
        updateStatus("connecting");

        socket.onopen = () => {
            console.log(`${path} SockJS WebSocket opened.`);
            hasConnected = true;
            nowRetryCount = 0;
            reconnectDelay = 3000;
            updateStatus("connect");
        };

        socket.onerror = (error) => {
            console.error(`${path} SockJS WebSocket Error:`, error);
            handleErrorOrClose();
        };

        socket.onclose = (event) => {
            console.log(`${path} SockJS WebSocket closed. Code: ${event.code}, Reason: ${event.reason}`);
            handleErrorOrClose();
        };
    };
    
    const handleErrorOrClose = () => {
        if (!hasConnected) {
            isReconnecting = false;
            console.log(`${path} SockJS WebSocket initial connection failed. Not retrying.`);
            updateStatus("disconnect");
        } else {
            if (!isReconnecting) return;

            nowRetryCount++;
            if (nowRetryCount <= maxRetryCount) {
                console.log(`${path} SockJS WebSocket error/close. Attempting to reconnect (${nowRetryCount}/${maxRetryCount})...`);
                updateStatus("reconnect", nowRetryCount, reconnectDelay);

                setTimeout(() => {
                    console.log(`Attempting to reconnect to ${path}... (Retry ${nowRetryCount})`);
                    if (path === '/api') {
                        apiSocket.connect();
                    } else if (path === '/image') {
                        imageSocket.connect();
                    } else if (path === '/imageJson') {
                        imageJsonSocket.connect();
                    }
                    reconnectDelay = Math.min(reconnectDelay * 2, maxReconnectDelay); // 指数関数的バックオフ
                }, reconnectDelay);
            } else {
                isReconnecting = false;
                console.log(`${path} SockJS WebSocket reconnection attempts exceeded. Not retrying.`);
                updateStatus("disconnect");
            }
        }
    };
    
    connect(); // 初回接続

    return { // socketオブジェクトを返すように修正
        connect: connect,
        getSocket: () => socket // socketを取得するgetterを追加
    };
};

// WebSocketの初期接続
let apiSocket = createWebSocket('/api', "api-status");
let imageSocket = createWebSocket('/image', "image-status");
let imageJsonSocket = createWebSocket('/imageJson', "imageJson-status");

// 最大表示件数
const MAX_LOGS = 20;

// 現在のリクエストログのリスト
const logs = [];

const createSendData = (method, encoding, uri, queryString, queryParams, postData, responseBody) => {

    let escapedResponseBody = "";
    if (typeof responseBody === 'string') {
        if (encoding === 'base64') {
            escapedResponseBody = responseBody; // base64の場合はそのまま
        } else {
            escapedResponseBody = responseBody; //svdata=JSON
        }
    } else if (responseBody) {
        escapedResponseBody = JSON.stringify(responseBody); //svdata=JSONの形なのでエスケープ実施(多分到達しないはず)
    }
    
    const sendData = {
        method: method,
        encoding: encoding,
        uri: uri,
        queryString: queryString,
        queryParams: queryParams,
        postData: postData,
        responseBody: escapedResponseBody
    };

    return JSON.stringify(sendData);
};

// ネットワークリクエストの監視を開始
chrome.devtools.network.onRequestFinished.addListener((request) => {
  const fullUrl = request.request.url;
  const url = new URL(fullUrl);

  const uri = url.pathname;
  
  if (
    uri.includes("/kcsapi/") ||
    uri.includes("/kcs2/resources/ship/") ||
    uri.includes("/kcs2/resources/map/") ||
    uri.includes("/kcs2/resources/gauge/") ||
    uri.includes("/kcs2/img/common/") ||
    uri.includes("/kcs2/img/duty/") ||
    uri.includes("/kcs2/img/sally/")
  ) {
    const method = request.request.method;
    const queryString = url.search;

    const queryParams = {};
    url.searchParams.forEach((value, key) => {
      queryParams[key] = value;
    });

    // レスポンスボディを取得
    request.getContent((content, encoding) => {
      // 古いログの削除
      if (logs.length >= MAX_LOGS) {
        logs.shift(); // 最も古いログを削除
        const requestsContainer = document.getElementById("request");
        requestsContainer.removeChild(requestsContainer.firstChild);
      }

      // リクエストボディの取得（POSTの場合のみ）
      let requestBodyHtml = "";
      let postData = "";
      if (method === "POST" && request.request.postData) {
        postData = request.request.postData.text || "";
        requestBodyHtml = `
          <h4>Request Body:</h4>
          <pre>${postData}</pre>
        `;
      }
      
      // URIが"/kcs2/"かつ、request.timings内の通信情報がない場合キャッシュからの取得とする
      const isConnect = request.timings && request.timings.connect > 0 && request.timings.send > 0;
      const isCacheLike = uri.includes("/kcs2/") && !isConnect;

      const cacheDispCheckBox = document.getElementById("show-cache-checkbox");
      const cacheSendCheckBox = document.getElementById("send-cache-checkbox");
      if (!cacheDispCheckBox.checked && isCacheLike) {
        return;
      }
      

      //送信処理
      let webSocketStatus = "disconnect";
      if (uri.includes("/kcsapi/")) {
          if (apiSocket && apiSocket.getSocket() && apiSocket.getSocket().readyState === SockJS.OPEN) {
              webSocketStatus = "connect";
              const encodeToSend = encoding || ""; 
              sendData = createSendData(method, encodeToSend, uri, queryString, queryParams, postData, content);
              try {
                  apiSocket.getSocket().send(sendData);
              } catch (error) {
                  console.log(`apiSocket error send data : ${sendData}` , error);            
              }
          }
      } else if (!isCacheLike || cacheSendCheckBox.checked) {
          //キャッシュの場合は処理しない
          if (encoding === "base64") {
              if (imageSocket && imageSocket.getSocket() && imageSocket.getSocket().readyState === SockJS.OPEN) {
                  webSocketStatus = "connect";
                  sendData = createSendData(method, encoding, uri, queryString, queryParams, postData, content);
                  try {
                      imageSocket.getSocket().send(sendData);
                  } catch (error) {
                      console.log(`imageSocket error send data : ${sendData}` , error);            
                  }
              }
          } else if (uri.endsWith(".json")) {
              if (imageJsonSocket && imageJsonSocket.getSocket() && imageJsonSocket.getSocket().readyState === SockJS.OPEN) {
                  webSocketStatus = "connect";
                  const encodeToSend = encoding || ""; 
                  sendData = createSendData(method, encodeToSend, uri, queryString, queryParams, postData, content);
                  try {
                      imageJsonSocket.getSocket().send(sendData);
                  } catch (error) {
                      console.log(`imageJsonSocket error send data : ${sendData}` , error);            
                  }
              }
          }
      }

      const logDispCheckBox = document.getElementById("log-disp-checkbox");
      if (!logDispCheckBox.checked) {
        return;
      }
      
      // WebSocketステータス表示のスタイル
      const webSocketStatusStyle =
        webSocketStatus === "connect"
        ? "font-weight: bold; color: green;"
        : "font-weight: bold; color: red;";

      // レスポンスの表示色を設定
      const responseColor =
        encoding === "base64" ? "rgba(255, 200, 200, 0.5)" : "rgba(200, 220, 255, 0.5)";

      // 折り畳み時の背景色（キャッシュの場合は濃いグレー）
      const headerColor = isCacheLike ? "rgba(50, 50, 50, 0.8)" : "#eee";
      const uriDisplay = isCacheLike ? `${uri} (Cache)` : `${uri} ${request.time.toFixed(3)}(ms)`;
      const webSocketStatusDisplay = isCacheLike ? `` : `<span style='${webSocketStatusStyle}'>${webSocketStatus}</span>`;

      const timings = request.timings
        ? `
          <h4>Timing Information:</h4>
          <pre>${JSON.stringify(request.timings, null, 2)}</pre>
        `
        : "<p><strong>Timing Information:</strong> (No timing data available)</p>";

      const contentSize = content ? new Blob([content]).size : 0;
        
      const requestHtml = `
        <div class="request-header" style="cursor: pointer; background-color: ${headerColor};">
          <strong>URI:</strong> ${uriDisplay} ${webSocketStatusDisplay}
        </div>
        <div class="request-body" style="display: none;">
          <h4>Request</h4>
          <p><strong>Method:</strong> ${method}</p>
          <p><strong>URL:</strong> ${fullUrl}</p>
          <p><strong>URI:</strong> ${uri}</p>
          <p><strong>Query String:</strong> ${queryString}</p>
          <h4>Query Parameters:</h4>
          <pre>${JSON.stringify(queryParams, null, 2)}</pre>
          ${requestBodyHtml}
          <h4>Response Body: <button class="copy-button" data-content="${encodeURIComponent(content || '')}">Copy</button></h4>
          <pre style="background-color: ${responseColor};">${content || "(No Response Body)"}</pre>
          <p><strong>Content Size:</strong> ${contentSize || "(Unknown Size)"}</p>
          ${timings}
        </div>
      `;

      const requestsContainer = document.getElementById("request");
      
      // 新しいリクエストを追加
      const requestElement = document.createElement("div");
      requestElement.className = "request";
      requestElement.innerHTML = requestHtml;
      requestsContainer.appendChild(requestElement);

      requestElement.querySelector(".copy-button").addEventListener("click", function () {
        const textToCopy = decodeURIComponent(this.getAttribute("data-content"));
        // テキストエリアを作成してコピー
        //navigator.clipboard.writeText()がまれに動かないため、確実に稼働するこの方式にした
        const textArea = document.createElement("textarea");
        textArea.value = textToCopy;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand("copy");
        document.body.removeChild(textArea);
      });      
      
      requestsContainer.scrollTop = requestsContainer.scrollHeight;

      // 折り畳みの設定
      const header = requestElement.querySelector(".request-header");
      const body = requestElement.querySelector(".request-body");
      header.addEventListener("click", () => {
        body.style.display = body.style.display === "none" ? "block" : "none";
      });

      // ログを保持
      logs.push(requestElement);
    });
  }
});
