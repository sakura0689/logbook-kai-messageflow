// heatbeat専用
chrome.runtime.onConnect.addListener((port) => {
  if (port.name === "logbook-kai-messageflow-keepalive") {
    port.onMessage.addListener((msg) => {
      if (msg && msg.type === "ping") {
        // 非同期APIを呼び出すことで、Service Workerがアクティブに動作していることをChromeに伝達し、
        // 30秒のアイドルタイムアウトタイマーをリセットする
        chrome.runtime.getPlatformInfo(() => {
          // NOP
        });
        
        try {
          port.postMessage({ type: 'pong' });
        } catch (e) {
          // NOP
        }
      }
    });

    port.onDisconnect.addListener(() => {
        // NOP
    });
  }
});