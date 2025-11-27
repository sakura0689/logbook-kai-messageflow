// heatbeat専用
chrome.runtime.onConnect.addListener((port) => {
  if (port.name === "logbook-kai-messageflow-keepalive") {
    port.onMessage.addListener((msg) => {
      // NOP
    });

    port.onDisconnect.addListener(() => {
        // NOP
    });
  }
});