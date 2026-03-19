import { CommunicationLog, DashboardSummary, PickingOrder, WesCommandForm } from '@/types/dashboard';

const API_BASE = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers || {}),
    },
    ...init,
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `${response.status}`);
  }

  return response.json() as Promise<T>;
}

export function wsUrl(path: string) {
  const override = import.meta.env.VITE_WS_BASE_URL;
  if (override) {
    return `${override.replace(/\/$/, '')}${path}`;
  }
  const url = new URL(API_BASE);
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
  url.pathname = path;
  url.search = '';
  return url.toString();
}

export async function fetchSummary() {
  return request<DashboardSummary>('/api/v1/dashboard/summary');
}

export async function fetchOrders(limit = 50) {
  return request<PickingOrder[]>(`/api/v1/dashboard/orders?limit=${limit}`);
}

export async function fetchQueue() {
  return request<PickingOrder[]>('/api/v1/dashboard/queue');
}

export async function fetchLogs(limit = 100) {
  return request<CommunicationLog[]>(`/api/v1/dashboard/logs?limit=${limit}`);
}

export async function fetchDbStatus() {
  return request<{ status: string; orders: number; users: number }>('/api/v1/dashboard/db-status');
}

export async function createCommand(payload: WesCommandForm) {
  const commandCode = String(payload.commandControlCode || '').trim().toUpperCase();

  let path = '/api/v1/commands/pick';
  let requestBody: Record<string, unknown> = {
    command_no: payload.commandNo,
    order_no: payload.orderNo,
    orderline_no: payload.orderlineNo,
    dc_id: payload.dcId,
    workstation_id: payload.workStationId,
    inbound_carrier_id: payload.inboundCarrierId,
    product_id: payload.productId,
    on_hand_qty: Number(payload.onHandQty),
    picking_qty: Number(payload.pickingQty),
    outbound_carrier_id: payload.outboundCarrierId,
  };

  if (commandCode === 'DBOX') {
    path = '/api/v1/commands/pick/dbox';
  } else if (commandCode === 'DBXX' || commandCode === 'DBRS') {
    path = '/api/v1/commands/control';
    requestBody = {
      command_no: payload.commandNo,
      dc_id: payload.dcId,
      workstation_id: payload.workStationId,
      control_code: commandCode,
    };
  }

  return request<{ message: string; data: PickingOrder }>(path, {
    method: 'POST',
    body: JSON.stringify(requestBody),
  });
}

export async function injectMockAck(commandNo: string, payload: Partial<{ commandStatus: string; actualQty: number; abnormalReasonCode: number; commandTime: number; errorDetail: string }>) {
  return request<{ data: PickingOrder }>(`/api/v1/mock/commands/${encodeURIComponent(commandNo)}/ack`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}
