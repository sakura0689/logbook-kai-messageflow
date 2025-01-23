// WebSocketの設定
const createWebSocket = (path, statusElementId) => {
    let hasConnected = false;
    let reconnectAttempts = 0;
    let socket = null;

    const maxRetryCount = 12;
    const statusElement = document.getElementById(statusElementId);
    const indicator = statusElement.querySelector(".status-indicator");
    const statusText = statusElement.querySelector(".status-text");

    const updateStatus = (status, attempts = null) => {
        indicator.className = `status-indicator ${status}`;
        let statusMessage = `${path.substring(1)}: ${status}`;
        if (attempts !== null) {
            statusMessage += ` (${attempts} retries)`;
        }
        statusText.textContent = statusMessage;
    };

    const connect = () => {
        socket = new WebSocket(`ws://localhost:8890${path}`);
        updateStatus("connecting");

        socket.onopen = () => {
            console.log(`${path} WebSocket opened.`);
            hasConnected = true;
            reconnectAttempts = 0;
            updateStatus("connect");
        };

        socket.onerror = (error) => {
            console.error(`${path} WebSocket Error:`, error);
            handleErrorOrClose();
        };

        socket.onclose = (event) => {
            console.log(`${path} WebSocket closed. Code: ${event.code}, Reason: ${event.reason}`);
            handleErrorOrClose();
        };
    };

    const handleErrorOrClose = () => {
        if (!hasConnected) {
            console.log(`${path} WebSocket initial connection failed. Not retrying.`);
            updateStatus("disconnect");
        } else {
            reconnectAttempts++;
            if (reconnectAttempts <= maxRetryCount) {
                console.log(`${path} WebSocket error/close. Attempting to reconnect (${reconnectAttempts}/${maxRetryCount})...`);
                updateStatus("reconnect", reconnectAttempts);
                reconnect(path, statusElementId, reconnectAttempts);
            } else {
                console.log(`${path} WebSocket reconnection attempts exceeded. Not retrying.`);
                updateStatus("disconnect");
                reconnectAttempts = 0;
            }
        }
    };

    connect(); // 初回接続

    return { // socketオブジェクトを返すように修正
        connect: connect,
        getSocket: () => socket // socketを取得するgetterを追加
    };
};

// リトライ関数
const reconnect = (path, statusElementId, reconnectAttempts) => {
    setTimeout(() => {
        console.log(`Attempting to reconnect to ${path}... (Retry ${reconnectAttempts})`);
        if (path === '/api') {
            apiSocket.connect(); // connect関数を呼び出す
        } else if (path === '/image') {
            imageSocket.connect();
        } else if (path === '/imageJson') {
            imageJsonSocket.connect();
        }
    }, 10000); // 10秒
};

// WebSocketの初期接続
let apiSocket = createWebSocket('/api', "api-status");
let imageSocket = createWebSocket('/image', "image-status");
let imageJsonSocket = createWebSocket('/imageJson', "imageJson-status");

// 最大表示件数
const MAX_LOGS = 20;

// 現在のリクエストログのリスト
const logs = [];

// ネットワークリクエストの監視を開始
chrome.devtools.network.onRequestFinished.addListener((request) => {
  const fullUrl = request.request.url;
  const url = new URL(fullUrl);

  const uri = url.pathname;
  
  if (
    uri.includes("/kcsapi/") ||
    uri.includes("/kcs2/resources/ship/") ||
    uri.includes("/kcs2/img/common/") ||
    uri.includes("/kcs2/img/duty/") ||
    uri.includes("/kcs2/img/sally/")
  ) {
    const method = request.request.method;
    const endpoint = `${url.origin}${url.pathname}`;
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
      if (method === "POST" && request.request.postData) {
        const postData = request.request.postData.text || "(No Request Body)";
        requestBodyHtml = `
          <h4>Request Body:</h4>
          <pre>${postData}</pre>
        `;
      }
      
      let webSocketStatus = "disconnect";
      if (uri.includes("/kcsapi/")) {
          if (apiSocket && apiSocket.getSocket() && apiSocket.getSocket().readyState === WebSocket.OPEN) {
              webSocketStatus = "connect";
          }
      } else {
          if (encoding === "base64") {
              if (imageSocket && imageSocket.getSocket() && imageSocket.getSocket().readyState === WebSocket.OPEN) {
                  webSocketStatus = "connect";
              }
          } else {
              if (imageJsonSocket && imageJsonSocket.getSocket() && imageJsonSocket.getSocket().readyState === WebSocket.OPEN) {
                  webSocketStatus = "connect";
              }
          }
      }

      // WebSocketステータス表示のスタイル
      const webSocketStatusStyle =
        webSocketStatus === "connect"
        ? "font-weight: bold; color: green;"
        : "font-weight: bold; color: red;";

      // レスポンスの表示色を設定
      const responseColor =
        encoding === "base64" ? "rgba(255, 200, 200, 0.5)" : "rgba(200, 220, 255, 0.5)";

      // URIが"/kcs2/"かつ、request.timings内の通信情報がない場合キャッシュからの取得とする
      const isConnect = request.timings && request.timings.connect > 0 && request.timings.send > 0;
      const isCacheLike = uri.includes("/kcs2/") && !isConnect;

      // 折り畳み時の背景色（キャッシュの場合は濃いグレー）
      const headerColor = isCacheLike ? "rgba(50, 50, 50, 0.8)" : "#eee";
      const endpointDisplay = isCacheLike ? `${endpoint} (Cache)` : `${endpoint} ${request.time.toFixed(3)}(ms)`;
      const webSocketStatusDisplay = isCacheLike ? `` : `<span style='${webSocketStatusStyle}'>${webSocketStatus}</span>`;

      const timings = request.timings
        ? `
          <h4>Timing Information:</h4>
          <pre>${JSON.stringify(request.timings, null, 2)}</pre>
        `
        : "<p><strong>Timing Information:</strong> (No timing data available)</p>";

      const requestHtml = `
        <div class="request-header" style="cursor: pointer; background-color: ${headerColor};">
          <strong>Endpoint:</strong> ${endpointDisplay} ${webSocketStatusDisplay}
        </div>
        <div class="request-body" style="display: none;">
          <h4>Request</h4>
          <p><strong>Method:</strong> ${method}</p>
          <p><strong>URL:</strong> ${fullUrl}</p>
          <p><strong>Endpoint:</strong> ${endpoint}</p>
          <p><strong>URI:</strong> ${uri}</p>
          <p><strong>Query String:</strong> ${queryString}</p>
          <h4>Query Parameters:</h4>
          <pre>${JSON.stringify(queryParams, null, 2)}</pre>
          ${requestBodyHtml}
          <h4>Response Body:</h4>
          <pre style="background-color: ${responseColor};">${content || "(No Response Body)"}</pre>
          ${timings}
        </div>
      `;

      const requestsContainer = document.getElementById("request");

      // 新しいリクエストを追加
      const requestElement = document.createElement("div");
      requestElement.className = "request";
      requestElement.innerHTML = requestHtml;
      requestsContainer.appendChild(requestElement);

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
