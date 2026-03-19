let ws: WebSocket | null = null;
let onMessageCallback: ((event: MessageEvent) => void) | null = null;
let onOpenCallback: (() => void) | null = null;
let onCloseCallback: (() => void) | null = null;

export const connect = (
  url: string,
  messageCallback: (event: MessageEvent) => void,
  openCallback: () => void,
  closeCallback: () => void
) => {
  if (ws && ws.readyState === WebSocket.OPEN) {
    console.log('WebSocket is already connected.');
    openCallback();
    return;
  }

  onMessageCallback = messageCallback;
  onOpenCallback = openCallback;
  onCloseCallback = closeCallback;

  ws = new WebSocket(url);

  ws.onopen = () => {
    console.log('WebSocket connected.');
    if (onOpenCallback) {
      onOpenCallback();
    }
  };

  ws.onmessage = (event) => {
    if (onMessageCallback) {
      onMessageCallback(event);
    }
  };

  ws.onclose = () => {
    console.log('WebSocket disconnected.');
    if (onCloseCallback) {
      onCloseCallback();
    }
  };

  ws.onerror = (error) => {
    console.error('WebSocket error:', error);
  };
};

export const disconnect = () => {
  if (ws) {
    ws.close();
    ws = null;
  }
};

export const sendMessage = (message: string) => {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(message);
  } else {
    console.error('WebSocket is not connected. Message not sent:', message);
  }
};

export const isConnected = (): boolean => {
  return ws !== null && ws.readyState === WebSocket.OPEN;
};
