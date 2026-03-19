export type CommandStatus = 'Pending' | 'Processing' | 'OK' | 'NG' | 'COMM_TIMEOUT';

export interface PickingOrder {
  id: number;
  commandNo: string;
  orderNo: string;
  orderlineNo: string;
  dcId: string;
  workstationId: string;
  inboundCarrierId: string;
  productId: string;
  onHandQty: number | null;
  pickingQty: number | null;
  outboundCarrierId: string;
  commandControlCode: string;
  commandStatus: CommandStatus;
  actualQty: number | null;
  abnormalReasonCode: number | null;
  commandTime: number | null;
  errorDetail: string | null;
  callbackAcknowledged: boolean;
  jobNo: string;
  jobStatus: string;
  sourcePayload?: string | null;
  ackPayload?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface DashboardSummary {
  totalOrders: number;
  inQueue: number;
  processing: number;
  success: number;
  ngErrors: number;
  accupickMode: string;
  dashboardWebsocket: string;
}

export interface CommunicationLog {
  id: number;
  timestamp: string;
  direction: string;
  protocol: string;
  messageType: string;
  content: string;
  jobNo?: string | null;
  carrierId?: string | null;
  remoteAddress?: string | null;
}

export interface DashboardInitMessage {
  type: 'dashboard:init';
  summary: DashboardSummary;
  orders: PickingOrder[];
  logs: CommunicationLog[];
}

export interface DashboardOrderMessage {
  type: 'dashboard:order-updated';
  order: PickingOrder;
  summary: DashboardSummary;
}

export interface DashboardLogMessage {
  type: 'dashboard:log';
  log: CommunicationLog;
  summary: DashboardSummary;
}

export interface DashboardSummaryMessage {
  type: 'dashboard:summary';
  summary: DashboardSummary;
}

export type DashboardSocketMessage =
  | DashboardInitMessage
  | DashboardOrderMessage
  | DashboardLogMessage
  | DashboardSummaryMessage;

export interface WesCommandForm {
  orderNo: string;
  orderlineNo: string;
  dcId: string;
  workStationId: string;
  inboundCarrierId: string;
  productId: string;
  onHandQty: number;
  pickingQty: number;
  outboundCarrierId: string;
  commandNo: string;
  commandControlCode: string;
}
