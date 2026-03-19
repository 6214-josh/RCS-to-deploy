export type SocketTarget = 'wes' | 'accupick';

export interface WesOrder {
  id: string;
  sceneCode: string;
  sceneDescription: string;
  inbound: string;
  productCode: string;
  productName: string;
  qty: number;
}

export interface RcsReply {
  id: string;
  sceneCode: string;
  sceneDescription: string;
  normalConfirmCode?: string | null;
  normalCompleteQty?: number | null;
  abnormalCompleteCode?: string | null;
  abnormalReasonCode?: string | null;
  abnormalCompleteQty?: number | null;
}

export interface AccuPickCommand {
  id: string;
  commandName: string;
  carrierId: string;
  expectedReply: string;
  sceneCode: string;
}

export interface SocketTargetConfig {
  endpointUrl: string;
  websocketUrl: string;
  autoEnabled: boolean;
  intervalSec: number;
}

export interface SocketLog {
  id: string;
  target: SocketTarget;
  direction: 'OUT' | 'IN' | 'SYSTEM';
  status: 'SUCCESS' | 'FAILED' | 'INFO';
  payload: string;
  createdAt: string;
  endpointUrl?: string;
  message?: string;
}

const SETTINGS_KEY = 'rcs-socket-settings-v1';
const LOGS_KEY = 'rcs-socket-logs-v1';

export const wesOrders: WesOrder[] = [
  {
    id: 'WES-DB01-01',
    sceneCode: 'DB01',
    sceneDescription: '正常出貨',
    inbound: '產品物流箱裝',
    productCode: '35010466',
    productName: '阿奇儂鮮乳坊冰淇淋4入',
    qty: 5,
  },
  {
    id: 'WES-DB02-01',
    sceneCode: 'DB02',
    sceneDescription: '正常出貨滿箱',
    inbound: '產品物流箱裝',
    productCode: '35010468',
    productName: '雙葉 WA!COOKIE S 雪糕-濃泰式奶茶奶蓋95g*4入',
    qty: 3,
  },
  {
    id: 'WES-DBXX-01',
    sceneCode: 'DBXX',
    sceneDescription: '關閉本站',
    inbound: '',
    productCode: '',
    productName: '',
    qty: 0,
  },
];

export const rcsReplies: RcsReply[] = [
  {
    id: 'RCS-DB01-OK',
    sceneCode: 'DB01',
    sceneDescription: '正常出貨',
    normalConfirmCode: 'DB01OK',
    normalCompleteQty: 5,
    abnormalCompleteCode: 'DB01NG',
    abnormalReasonCode: null,
    abnormalCompleteQty: null,
  },
  {
    id: 'RCS-DB01-SHORTAGE',
    sceneCode: 'DB01',
    sceneDescription: '正常出貨',
    normalConfirmCode: null,
    normalCompleteQty: null,
    abnormalCompleteCode: 'DB01NG',
    abnormalReasonCode: 'DBSHORTAGE',
    abnormalCompleteQty: 3,
  },
  {
    id: 'RCS-DB01-RBTFAIL',
    sceneCode: 'DB01',
    sceneDescription: '正常出貨',
    normalConfirmCode: null,
    normalCompleteQty: null,
    abnormalCompleteCode: 'DB01NG',
    abnormalReasonCode: 'DBRBTFAIL',
    abnormalCompleteQty: 3,
  },
  {
    id: 'RCS-DB02-OK',
    sceneCode: 'DB02',
    sceneDescription: '異常出貨',
    normalConfirmCode: 'DB02OK',
    normalCompleteQty: 3,
    abnormalCompleteCode: null,
    abnormalReasonCode: null,
    abnormalCompleteQty: null,
  },
  {
    id: 'RCS-DB02-SHORTAGE',
    sceneCode: 'DB02',
    sceneDescription: '異常出貨',
    normalConfirmCode: null,
    normalCompleteQty: null,
    abnormalCompleteCode: 'DB02NG',
    abnormalReasonCode: 'DBSHORTAGE',
    abnormalCompleteQty: 2,
  },
  {
    id: 'RCS-DB02-RBTFAIL',
    sceneCode: 'DB02',
    sceneDescription: '正常出貨',
    normalConfirmCode: null,
    normalCompleteQty: null,
    abnormalCompleteCode: 'DB02NG',
    abnormalReasonCode: 'DBRBTFAIL',
    abnormalCompleteQty: 2,
  },
  {
    id: 'RCS-DBXX-AK',
    sceneCode: 'DBXX',
    sceneDescription: '關閉本站',
    normalConfirmCode: 'DBXXAK',
    normalCompleteQty: null,
    abnormalCompleteCode: null,
    abnormalReasonCode: null,
    abnormalCompleteQty: null,
  },
];

export const accupickCommands: AccuPickCommand[] = [
  {
    id: 'AP-001',
    commandName: 'Inbound_Carrier_ID',
    carrierId: '3',
    expectedReply: 'Inbound_Carrier_ID,3,ACK',
    sceneCode: 'DB01',
  },
  {
    id: 'AP-002',
    commandName: 'Inbound_Carrier_ID',
    carrierId: '002',
    expectedReply: 'Inbound_Carrier_ID,002,ACK',
    sceneCode: 'DB02',
  },
  {
    id: 'AP-003',
    commandName: 'Close_Station',
    carrierId: 'DBXX',
    expectedReply: 'Close_Station,DBXX,ACK',
    sceneCode: 'DBXX',
  },
];

export function getDefaultSettings(): Record<SocketTarget, SocketTargetConfig> {
  return {
    wes: {
      endpointUrl: '/api/socket/wes/send',
      websocketUrl: 'ws://localhost:8080/ws/wes',
      autoEnabled: false,
      intervalSec: 10,
    },
    accupick: {
      endpointUrl: '/api/socket/accupick/send',
      websocketUrl: 'ws://localhost:8080/ws/accupick',
      autoEnabled: false,
      intervalSec: 8,
    },
  };
}

export function loadSettings(): Record<SocketTarget, SocketTargetConfig> {
  if (typeof window === 'undefined') return getDefaultSettings();
  try {
    const raw = window.localStorage.getItem(SETTINGS_KEY);
    if (!raw) return getDefaultSettings();
    const parsed = JSON.parse(raw);
    return {
      wes: { ...getDefaultSettings().wes, ...(parsed.wes ?? {}) },
      accupick: { ...getDefaultSettings().accupick, ...(parsed.accupick ?? {}) },
    };
  } catch {
    return getDefaultSettings();
  }
}

export function saveSettings(settings: Record<SocketTarget, SocketTargetConfig>) {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
}

export function loadLogs(): SocketLog[] {
  if (typeof window === 'undefined') return [];
  try {
    const raw = window.localStorage.getItem(LOGS_KEY);
    if (!raw) return [];
    return JSON.parse(raw);
  } catch {
    return [];
  }
}

export function appendLog(log: SocketLog) {
  const logs = [log, ...loadLogs()].slice(0, 200);
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(LOGS_KEY, JSON.stringify(logs));
  }
  return logs;
}

export function clearLogs() {
  if (typeof window !== 'undefined') {
    window.localStorage.removeItem(LOGS_KEY);
  }
}

export function buildWesPayload(order: WesOrder) {
  return `WES|${order.sceneCode}|${order.sceneDescription}|${order.inbound}|${order.productCode}|${order.productName}|${order.qty}`;
}

export function buildRcsReplyPayload(reply: RcsReply) {
  return `RCS|${reply.sceneCode}|${reply.sceneDescription}|${reply.normalConfirmCode ?? ''}|${reply.normalCompleteQty ?? ''}|${reply.abnormalCompleteCode ?? ''}|${reply.abnormalReasonCode ?? ''}|${reply.abnormalCompleteQty ?? ''}`;
}

export function buildAccupickPayload(command: AccuPickCommand) {
  return `${command.commandName},${command.carrierId}`;
}

export async function sendSocketPayload(target: SocketTarget, endpointUrl: string, payload: string) {
  const createdAt = new Date().toISOString();

  try {
    const response = await fetch(endpointUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ target, payload }),
    });

    const text = await response.text();
    const log: SocketLog = {
      id: `${target}-${Date.now()}`,
      target,
      direction: 'OUT',
      status: response.ok ? 'SUCCESS' : 'FAILED',
      payload,
      endpointUrl,
      createdAt,
      message: text || response.statusText,
    };
    appendLog(log);
    return log;
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Network error';
    const log: SocketLog = {
      id: `${target}-${Date.now()}`,
      target,
      direction: 'OUT',
      status: 'FAILED',
      payload,
      endpointUrl,
      createdAt,
      message,
    };
    appendLog(log);
    return log;
  }
}

export function createSystemLog(target: SocketTarget, payload: string, message: string): SocketLog {
  const log: SocketLog = {
    id: `${target}-sys-${Date.now()}`,
    target,
    direction: 'SYSTEM',
    status: 'INFO',
    payload,
    createdAt: new Date().toISOString(),
    message,
  };
  appendLog(log);
  return log;
}
