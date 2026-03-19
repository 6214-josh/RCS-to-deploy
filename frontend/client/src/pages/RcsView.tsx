import { ReactNode, useEffect, useMemo, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-quartz.css';
import { accupickCommands, loadLogs, rcsReplies, wesOrders } from '@/lib/integration';

interface DataRow {
  id: number;
  oder_no: string;
  orderline_no: string;
  dc_id: string;
  workstation_id: string;
  inbound_carrier_id: string;
  product_id: string;
  on_hand_qty: number;
  picking_qty: number;
  outbound_carrier_id: string;
  command_no: string;
  command_time?: string | null;
  command_status: 'Pending' | 'Processing' | 'Success' | 'NG';
  abnomal_reason_code?: string | null;
  command_control_code?: string;
}

interface RcsViewProps {
  currentTab?: 'dashboard' | 'queue';
}

const mockRows: DataRow[] = [
  {
    id: 1,
    oder_no: 'ORD-DB01-001',
    orderline_no: 'LN-01',
    dc_id: 'N1',
    workstation_id: 'N101',
    inbound_carrier_id: '3',
    product_id: '35010466',
    on_hand_qty: 5,
    picking_qty: 5,
    outbound_carrier_id: 'OBX-001',
    command_no: 'CMD-DB01-001',
    command_time: new Date().toISOString(),
    command_status: 'Success',
    abnomal_reason_code: null,
    command_control_code: 'DB01OK',
  },
  {
    id: 2,
    oder_no: 'ORD-DB01-002',
    orderline_no: 'LN-01',
    dc_id: 'N1',
    workstation_id: 'N101',
    inbound_carrier_id: '3',
    product_id: '35010466',
    on_hand_qty: 5,
    picking_qty: 3,
    outbound_carrier_id: 'OBX-002',
    command_no: 'CMD-DB01-002',
    command_time: new Date(Date.now() - 120000).toISOString(),
    command_status: 'NG',
    abnomal_reason_code: 'DBSHORTAGE',
    command_control_code: 'DB01NG',
  },
  {
    id: 3,
    oder_no: 'ORD-DB02-001',
    orderline_no: 'LN-01',
    dc_id: 'N1',
    workstation_id: 'N102',
    inbound_carrier_id: '002',
    product_id: '35010468',
    on_hand_qty: 3,
    picking_qty: 3,
    outbound_carrier_id: 'OBX-003',
    command_no: 'CMD-DB02-001',
    command_time: new Date(Date.now() - 30000).toISOString(),
    command_status: 'Processing',
    abnomal_reason_code: null,
    command_control_code: 'DB02OK',
  },
  {
    id: 4,
    oder_no: 'ORD-DBXX-001',
    orderline_no: 'LN-99',
    dc_id: 'N1',
    workstation_id: 'N103',
    inbound_carrier_id: 'DBXX',
    product_id: '',
    on_hand_qty: 0,
    picking_qty: 0,
    outbound_carrier_id: '',
    command_no: 'CMD-DBXX-001',
    command_time: null,
    command_status: 'Pending',
    abnomal_reason_code: null,
    command_control_code: 'DBXXAK',
  },
];

export default function RcsView({ currentTab: propCurrentTab = 'dashboard' }: RcsViewProps) {
  const [rowData] = useState<DataRow[]>(mockRows);
  const [logs, setLogs] = useState(loadLogs());
  const displayMode = propCurrentTab === 'queue' ? 'queue' : 'orders';

  useEffect(() => {
    const timer = window.setInterval(() => setLogs(loadLogs()), 1500);
    return () => window.clearInterval(timer);
  }, []);

  const filteredData = useMemo(
    () => (displayMode === 'queue' ? rowData.filter((item) => item.command_status === 'Pending' || item.command_status === 'Processing') : rowData),
    [displayMode, rowData],
  );

  const stats = [
    { label: 'RCS Orders', value: String(rowData.length) },
    { label: 'WES Scenarios', value: String(wesOrders.length) },
    { label: 'AccuPick Templates', value: String(accupickCommands.length) },
    { label: 'Success / NG', value: `${rowData.filter((item) => item.command_status === 'Success').length} / ${rowData.filter((item) => item.command_status === 'NG').length}` },
    { label: 'Socket Logs', value: String(logs.length) },
  ];

  const columnDefs = [
    { field: 'oder_no', headerName: 'Order No', width: 150 },
    { field: 'orderline_no', headerName: 'Line', width: 100 },
    { field: 'workstation_id', headerName: 'Station', width: 120 },
    { field: 'inbound_carrier_id', headerName: 'Inbound Carrier', width: 140 },
    { field: 'product_id', headerName: 'Product ID', width: 130 },
    { field: 'picking_qty', headerName: 'Pick Qty', width: 100 },
    { field: 'command_no', headerName: 'Command No', width: 150 },
    {
      field: 'command_status',
      headerName: 'Status',
      width: 110,
      cellStyle: (params: { value: string }) => {
        if (params.value === 'Success') return { color: '#4ade80', fontWeight: 'bold' };
        if (params.value === 'NG') return { color: '#f87171', fontWeight: 'bold' };
        if (params.value === 'Processing') return { color: '#facc15', fontWeight: 'bold' };
        return { color: '#60a5fa', fontWeight: 'bold' };
      },
    },
    { field: 'abnomal_reason_code', headerName: 'NG Code', width: 130 },
    { field: 'command_control_code', headerName: 'Control Code', width: 130 },
  ];

  return (
    <div className="h-full flex flex-col space-y-6">
      <div className="grid grid-cols-5 gap-4">
        {stats.map((stat) => (
          <div key={stat.label} className="bg-[#2d2d2d] p-4 border-l-2 border-[#f39c12] rounded shadow-sm">
            <p className="text-[10px] text-gray-500 uppercase">{stat.label}</p>
            <p className="text-xl font-bold mt-1 text-[#ecf0f1] break-all">{stat.value}</p>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-3 gap-6">
        <Panel title="WES 對應重點">
          <ul className="space-y-2 text-sm text-[#ecf0f1]">
            <li>• 已納入 DB01 / DB02 / DBXX 場景顯示</li>
            <li>• 可選擇 WES 訂單模板手動送出</li>
            <li>• 可切換 RCS 回覆模板送回 WES</li>
            <li>• 可設定自動送出秒數與 endpoint</li>
          </ul>
        </Panel>
        <Panel title="AccuPick 對應重點">
          <ul className="space-y-2 text-sm text-[#ecf0f1]">
            <li>• 已顯示 Inbound_Carrier_ID 指令模板</li>
            <li>• 支援 WebSocket 監看後端回傳訊息</li>
            <li>• 可手動 / 自動發送 Socket 到 AccuPick</li>
            <li>• 可保留原本 AccuPick TCP 流程</li>
          </ul>
        </Panel>
        <Panel title="資料庫對應重點">
          <ul className="space-y-2 text-sm text-[#ecf0f1]">
            <li>• picking_orders：主訂單中介資料</li>
            <li>• communication_log：通訊紀錄</li>
            <li>• users：登入</li>
            <li>• 前端目前以模板資料與通訊 API 串接</li>
          </ul>
        </Panel>
      </div>

      <div className="flex-1 bg-[#2d2d2d] rounded border border-[#3f3f3f] flex flex-col overflow-hidden min-h-[320px]">
        <div className="p-4 border-b border-[#3f3f3f] bg-[#1a1a1a]/30 flex justify-between items-center">
          <div className="text-xs font-bold uppercase tracking-widest text-[#ecf0f1]">
            {displayMode === 'queue' ? 'RCS Queue Monitor' : 'RCS / WES / AccuPick 綜合訂單視圖'}
          </div>
          <div className="text-xs text-gray-400">資料來源：目前資料庫 schema + WES / AccuPick 範例專案</div>
        </div>
        <div className="flex-1 ag-theme-quartz-dark">
          <AgGridReact rowData={filteredData} columnDefs={columnDefs} defaultColDef={{ sortable: true, filter: true, resizable: true }} />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-6">
        <Panel title="RCS 回覆模板">
          <div className="space-y-2 max-h-[240px] overflow-auto">
            {rcsReplies.map((reply) => (
              <div key={reply.id} className="bg-[#1a1a1a] border border-[#3a3a3a] rounded p-3 text-sm">
                <div className="font-semibold text-[#ecf0f1]">{reply.sceneCode} | {reply.sceneDescription}</div>
                <div className="text-xs text-gray-400 mt-1">
                  OK: {reply.normalConfirmCode ?? '-'} / {reply.normalCompleteQty ?? '-'} | NG: {reply.abnormalCompleteCode ?? '-'} / {reply.abnormalReasonCode ?? '-'} / {reply.abnormalCompleteQty ?? '-'}
                </div>
              </div>
            ))}
          </div>
        </Panel>
        <Panel title="最新 Socket 紀錄">
          <div className="space-y-2 max-h-[240px] overflow-auto">
            {logs.length === 0 ? (
              <div className="text-sm text-gray-400">尚無通訊紀錄。</div>
            ) : (
              logs.slice(0, 12).map((log) => (
                <div key={log.id} className="bg-[#1a1a1a] border border-[#3a3a3a] rounded p-3 text-sm">
                  <div className="flex justify-between text-xs text-gray-400 mb-1">
                    <span>{log.target.toUpperCase()} | {log.direction}</span>
                    <span>{log.status}</span>
                  </div>
                  <div className="font-mono text-xs break-all text-[#ecf0f1]">{log.payload}</div>
                </div>
              ))
            )}
          </div>
        </Panel>
      </div>
    </div>
  );
}

function Panel({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="bg-[#2d2d2d] p-5 rounded border border-[#3f3f3f]">
      <h3 className="text-lg font-bold text-[#f39c12] mb-4">{title}</h3>
      {children}
    </section>
  );
}
