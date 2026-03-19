import { type ComponentType, useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle,
  Boxes,
  Cable,
  Cpu,
  Database,
  LayoutDashboard,
  ListOrdered,
  RefreshCw,
  Send,
  ShieldCheck,
  Truck,
  ArrowRightLeft,
} from 'lucide-react';
import { createCommand, fetchDbStatus, injectMockAck } from '@/lib/api';
import { useDashboardSocket } from '@/hooks/useDashboardSocket';
import { PickingOrder, WesCommandForm } from '@/types/dashboard';

const defaultForm: WesCommandForm = {
  orderNo: 'ORD-0042',
  orderlineNo: 'LN01',
  dcId: 'N1',
  workStationId: 'N101',
  inboundCarrierId: 'IN-B001',
  productId: '35010466',
  onHandQty: 12,
  pickingQty: 5,
  outboundCarrierId: 'OUT-B003',
  commandNo: `DB01-${new Date().getTime().toString().slice(-4)}`,
  commandControlCode: 'NORMAL',
};

const defaultAccupickAckForm = {
  commandNo: 'DB01-0042',
  commandStatus: 'OK',
  actualQty: 5,
  abnormalReasonCode: 0,
  commandTime: 1200,
  errorDetail: '',
};

type TabId = 'dashboard' | 'queue' | 'db' | 'wes' | 'wesSimulator' | 'accupick' | 'accupickSimulator';

const tabs: Array<{ id: TabId; label: string; icon: ComponentType<{ className?: string }> }> = [
  { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { id: 'queue', label: 'Queue Status', icon: ListOrdered },
  { id: 'db', label: 'DB Status', icon: Database },
  { id: 'wes', label: 'WES Status', icon: Truck },
  { id: 'wesSimulator', label: 'WES Simulator', icon: Send },
  { id: 'accupick', label: 'AccuPick Status', icon: Cpu },
  { id: 'accupickSimulator', label: 'AccuPick Simulator', icon: ArrowRightLeft },
];

export default function Dashboard() {
  const { summary, orders, queueOrders, logs, connected } = useDashboardSocket();
  const [currentTab, setCurrentTab] = useState<TabId>('dashboard');
  const [selectedOrder, setSelectedOrder] = useState<PickingOrder | null>(null);
  const [form, setForm] = useState<WesCommandForm>(defaultForm);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string>('');
  const [dbStatus, setDbStatus] = useState<{ status: string; orders: number; users: number } | null>(null);
  const [apiLogs, setApiLogs] = useState<any[]>([]);
  const [ackForm, setAckForm] = useState(defaultAccupickAckForm);

  const visibleOrders = currentTab === 'queue' ? queueOrders : orders;
  const currentTitle = tabs.find((item) => item.id === currentTab)?.label || 'Dashboard';

  const abnormalName = (code?: number | null) => {
    switch (code ?? 0) {
      case 0:
        return 'NONE';
      case 1:
        return 'SHORTAGE';
      case 2:
        return 'PICKING_FAIL';
      case 3:
        return 'OVERFLOW';
      case 4:
        return 'COMM_TIMEOUT';
      case 5:
        return 'CARRIER_NOT_FOUND';
      case 6:
        return 'BOX_HEIGHT_EXCEEDED';
      default:
        return '-';
    }
  };

  const stats = useMemo(
    () => [
      { label: 'TOTAL ORDERS', value: summary?.totalOrders ?? 0 },
      { label: 'IN QUEUE', value: summary?.inQueue ?? 0 },
      { label: 'PROCESSING', value: summary?.processing ?? 0 },
      { label: 'SUCCESS', value: summary?.success ?? 0 },
      { label: 'NG / ERRORS', value: summary?.ngErrors ?? 0 },
    ],
    [summary],
  );

  const submitCommand = async () => {
    setBusy(true);
    setMessage('');
    try {
      const result = await createCommand(form);
      setMessage(result?.message || '命令已送出到 RCS。');
      setForm((prev) => ({
        ...prev,
        commandNo: `${prev.commandControlCode === 'DBOX' ? 'DB02' : prev.commandControlCode === 'DBXX' || prev.commandControlCode === 'DBRS' ? 'CTRL' : 'DB01'}-${Date.now().toString().slice(-4)}`,
      }));
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '送出失敗');
    } finally {
      setBusy(false);
    }
  };

  const sendMockAck = async (order: PickingOrder, type: 'OK' | 'SHORTAGE' | 'OVERFLOW' | 'NOT_FOUND') => {
    setBusy(true);
    try {
      if (type === 'OK') {
        await injectMockAck(order.commandNo, {
          commandStatus: 'OK',
          actualQty: order.commandControlCode === 'DBXX' || order.commandControlCode === 'DBRS' ? undefined : order.pickingQty ?? 0,
          abnormalReasonCode: 0,
          commandTime: 1200,
        });
      }
      if (type === 'SHORTAGE') {
        await injectMockAck(order.commandNo, {
          commandStatus: 'NG',
          actualQty: Math.max((order.pickingQty ?? 1) - 2, 0),
          abnormalReasonCode: 1,
          commandTime: 4230,
          errorDetail: 'SHORTAGE',
        });
      }
      if (type === 'OVERFLOW') {
        await injectMockAck(order.commandNo, {
          commandStatus: 'NG',
          actualQty: Math.max((order.pickingQty ?? 1) - 3, 0),
          abnormalReasonCode: 3,
          commandTime: 5120,
          errorDetail: 'OVERFLOW',
        });
      }
      if (type === 'NOT_FOUND') {
        await injectMockAck(order.commandNo, {
          commandStatus: 'NG',
          actualQty: 0,
          abnormalReasonCode: 5,
          commandTime: 1800,
          errorDetail: 'CARRIER_NOT_FOUND',
        });
      }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'ACK 模擬失敗');
    } finally {
      setBusy(false);
    }
  };

  const loadDbStatus = async () => {
    const result = await fetchDbStatus();
    setDbStatus(result);
  };

  const loadApiLogs = async () => {
    try {
      const response = await fetch('/api/logs');
      if (!response.ok) {
        throw new Error(`GET /api/logs ${response.status}`);
      }
      const data = await response.json();
      setApiLogs(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('load /api/logs failed', error);
      setApiLogs([]);
    }
  };

  useEffect(() => {
    loadApiLogs();
    const timer = window.setInterval(loadApiLogs, 3000);
    return () => window.clearInterval(timer);
  }, []);

  const submitAccupickAck = async () => {
    setBusy(true);
    setMessage('');
    try {
      await injectMockAck(ackForm.commandNo, {
        commandStatus: ackForm.commandStatus,
        actualQty: ackForm.commandStatus === 'OK' ? ackForm.actualQty : ackForm.actualQty,
        abnormalReasonCode: Number(ackForm.abnormalReasonCode),
        commandTime: Number(ackForm.commandTime),
        errorDetail: ackForm.errorDetail || undefined,
      });
      setMessage(`AccuPick ACK 已送出：${ackForm.commandNo}`);
      await loadApiLogs();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'AccuPick ACK 模擬失敗');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="rcs-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <h1>RCS SYSTEM</h1>
          <p>Frozen Sorting Control v1.5.1</p>
        </div>
        <nav className="nav-list">
          {tabs.map((item) => {
            const Icon = item.icon;
            return (
              <button
                key={item.id}
                className={`nav-btn ${currentTab === item.id ? 'active' : ''}`}
                onClick={() => setCurrentTab(item.id)}
              >
                <Icon className="nav-icon" />
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>
        <div className="sidebar-footer">
          <div className={`status-pill ${connected ? 'online' : 'offline'}`}>
            <Cable className="w-4 h-4" />
            <span>{connected ? 'WEBSOCKET ONLINE' : 'WEBSOCKET OFFLINE'}</span>
          </div>
          <div className="sidebar-meta">{summary?.accupickMode || 'AccuPick mode loading...'}</div>
        </div>
      </aside>

      <main className="main-panel">
        <header className="topbar">
          <div>
            <div className="topbar-title">{currentTitle}</div>
            <div className="topbar-subtitle">欄位依 SDD DOCX：WES JSON / RCS-AccuPick KV-CSV</div>
          </div>
          <div className="operator-block">
            <span>OPERATOR: ADMIN</span>
            <button className="ghost-btn" onClick={() => window.location.reload()}>
              <RefreshCw className="w-4 h-4" /> Refresh
            </button>
          </div>
        </header>

        <section className="content-scroll">
          <div className="stats-grid">
            {stats.map((item) => (
              <div key={item.label} className="metric-card">
                <div className="metric-label">{item.label}</div>
                <div className="metric-value">{item.value}</div>
              </div>
            ))}
          </div>

          {(currentTab === 'dashboard' || currentTab === 'queue') && (
            <>
              <div className="layout-two-col">
                <section className="panel">
                  <div className="panel-header">
                    <h3>LIVE COMMAND STREAM</h3>
                    <span>{currentTab === 'queue' ? 'Pending / Processing' : 'Recent Commands'}</span>
                  </div>
                  <div className="table-wrap">
                    <table className="rcs-table">
                      <thead>
                        <tr>
                          <th>Command_No</th>
                          <th>Order_No</th>
                          <th>Orderline_No</th>
                          <th>DC_ID</th>
                          <th>WorkStation_ID</th>
                          <th>Inbound_Carrier_ID</th>
                          <th>Product_ID</th>
                          <th>On_hand_qty</th>
                          <th>Picking_qty</th>
                          <th>Outbound_Carrier_ID</th>
                          <th>Command_Control_Code</th>
                          <th>Command_Status</th>
                          <th>Actual_qty</th>
                          <th>Abnormal_reason_code</th>
                          <th>Command_time</th>
                          <th>Action</th>
                        </tr>
                      </thead>
                      <tbody>
                        {visibleOrders.map((order) => (
                          <tr key={order.commandNo}>
                            <td>{order.commandNo}</td>
                            <td>{order.orderNo}</td>
                            <td>{order.orderlineNo}</td>
                            <td>{order.dcId}</td>
                            <td>{order.workstationId}</td>
                            <td>{order.inboundCarrierId}</td>
                            <td>{order.productId}</td>
                            <td>{safe(order.onHandQty)}</td>
                            <td>{safe(order.pickingQty)}</td>
                            <td>{order.outboundCarrierId}</td>
                            <td>{order.commandControlCode}</td>
                            <td><StatusText status={order.commandStatus} /></td>
                            <td>{safe(order.actualQty)}</td>
                            <td>{abnormalName(order.abnormalReasonCode)}</td>
                            <td>{order.commandTime ? `${order.commandTime} ms` : '-'}</td>
                            <td>
                              <button className="view-btn" onClick={() => setSelectedOrder(order)}>
                                VIEW
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </section>

                <section className="panel compact">
                  <div className="panel-header">
                    <h3>FLOW OVERVIEW</h3>
                    <span>WES → RCS → AccuPick → WES</span>
                  </div>
                  <div className="status-grid">
                    <Card icon={Send} title="STEP 1" value="WES 呼叫 RCS REST API 下發命令" />
                    <Card icon={Truck} title="STEP 2" value="RCS 轉換為 TCP KV-CSV CMD 發給 AccuPick" />
                    <Card icon={Cpu} title="STEP 3" value="AccuPick 回 ACK：OK / NG / actual_qty / abnormal_reason_code" />
                    <Card icon={ShieldCheck} title="STEP 4" value="RCS 回呼 WES callbacks/*，等待 WES ACK" />
                  </div>
                  {message && <div className="inline-message">{message}</div>}
                </section>
              </div>

              <div className="layout-two-col bottom">
                <section className="panel compact">
                  <div className="panel-header">
                    <h3>LIVE SOCKET / TCP LOG</h3>
                    <span>Newest 100 records</span>
                  </div>
                  <div className="log-list">
                    {logs.slice(0, 16).map((log) => (
                      <div key={log.id} className="log-card">
                        <div className="log-meta">
                          <span>{formatTs(log.timestamp)}</span>
                          <span>{log.protocol} / {log.direction}</span>
                        </div>
                        <div className="log-type">{log.messageType}</div>
                        <div className="log-content">{log.content}</div>
                      </div>
                    ))}
                  </div>
                </section>

                <section className="panel compact">
                  <div className="panel-header">
                    <h3>FIELD NOTES</h3>
                    <span>DOCX-aligned behavior</span>
                  </div>
                  <ul className="note-list">
                    <li>WES → RCS 使用 JSON 欄位：Order_No ~ Command_Control_Code。</li>
                    <li>RCS → AccuPick 使用 KV-CSV CMD；控制命令 DBXX / DBRS 可省略 Picking_qty。</li>
                    <li>AccuPick ACK 顯示 Command_Status / Actual_qty / Abnormal_reason_code / Command_time。</li>
                    <li>COMM_TIMEOUT 在後端會轉為 503，並於表格列為 COMM_TIMEOUT。</li>
                  </ul>
                </section>
              </div>
            </>
          )}

          {currentTab === 'wes' && (
            <section className="panel status-panel">
              <div className="panel-header"><h3>WES STATUS</h3><span>REST / JSON</span></div>
              <div className="panel-header"><h3>WES LOG LIST</h3><span>/api/logs</span></div>
              <div className="log-list">
                {apiLogs.filter((log) => String(log.type || '').includes('WES')).length > 0 ? (
                  apiLogs
                    .filter((log) => String(log.type || '').includes('WES'))
                    .slice(0, 16)
                    .map((log, index) => (
                      <div key={`${log.id ?? log.commandNo ?? 'wes'}-${index}`} className="log-card">
                        <div className="log-meta">
                          <span>{formatTs(log.createdAt || log.timestamp || '')}</span>
                          <span>{log.type || 'WES'}</span>
                        </div>
                        <div className="log-type">{log.commandNo || '-'}</div>
                        <div className="log-content">{log.message || '-'}</div>
                      </div>
                    ))
                ) : (
                  <div className="log-card"><div className="log-content">No WES log from /api/logs yet.</div></div>
                )}
              </div>
              <div className="status-grid">
                <Card icon={Truck} title="Request Fields" value="command_no, order_no, orderline_no, dc_id, workstation_id, inbound_carrier_id, product_id, on_hand_qty, picking_qty, outbound_carrier_id" />
                <Card icon={ShieldCheck} title="Callback Fields" value="command_no, command_status, actual_qty, abnormal_reason_code / control_code" />
                <Card icon={Boxes} title="Supported APIs" value="/commands/pick / pick/dbox / requeue / control / {command_no}/ack" />
                <Card icon={AlertTriangle} title="HTTP Status" value="201 Created / 202 Accepted / 200 OK / 400 Bad Request / 503 Service Unavailable" />
              </div>
            </section>
          )}


          {currentTab === 'wesSimulator' && (
            <section className="panel status-panel">
              <div className="panel-header"><h3>WES SIMULATOR</h3><span>POST /api/v1/commands/*</span></div>
              <div className="form-grid">
                {(
                  [
                    ['orderNo', 'Order_No'],
                    ['orderlineNo', 'Orderline_No'],
                    ['dcId', 'DC_ID'],
                    ['workStationId', 'WorkStation_ID'],
                    ['inboundCarrierId', 'Inbound_Carrier_ID'],
                    ['productId', 'Product_ID'],
                    ['onHandQty', 'On_hand_qty'],
                    ['pickingQty', 'Picking_qty'],
                    ['outboundCarrierId', 'Outbound_Carrier_ID'],
                    ['commandNo', 'Command_No'],
                  ] as Array<[keyof WesCommandForm, string]>
                ).map(([key, label]) => (
                  <label key={key} className="field-block">
                    <span>{label}</span>
                    <input
                      value={String(form[key] ?? '')}
                      onChange={(e) =>
                        setForm((prev) => ({
                          ...prev,
                          [key]: key === 'onHandQty' || key === 'pickingQty' ? Number(e.target.value) : e.target.value,
                        }))
                      }
                    />
                  </label>
                ))}
                <label className="field-block">
                  <span>Command_Control_Code</span>
                  <select
                    value={form.commandControlCode}
                    onChange={(e) => setForm((prev) => ({ ...prev, commandControlCode: e.target.value }))}
                  >
                    <option value="NORMAL">NORMAL / pick</option>
                    <option value="DBOX">DBOX / pick/dbox</option>
                    <option value="DBXX">DBXX / control</option>
                    <option value="DBRS">DBRS / control</option>
                  </select>
                </label>
              </div>
              <div className="button-row">
                <button className="primary-btn" onClick={submitCommand} disabled={busy}>
                  <Send className="w-4 h-4" /> Send WES Command
                </button>
              </div>
              {message && <div className="inline-message">{message}</div>}
            </section>
          )}

          {currentTab === 'accupick' && (
            <section className="panel status-panel">
              <div className="panel-header"><h3>ACCUPICK STATUS</h3><span>TCP / KV-CSV</span></div>
              <div className="panel-header"><h3>ACCUPICK LOG LIST</h3><span>/api/logs</span></div>
              <div className="log-list">
                {apiLogs.filter((log) => String(log.type || '').includes('ACCUPICK')).length > 0 ? (
                  apiLogs
                    .filter((log) => String(log.type || '').includes('ACCUPICK'))
                    .slice(0, 16)
                    .map((log, index) => (
                      <div key={`${log.id ?? log.commandNo ?? 'accupick'}-${index}`} className="log-card">
                        <div className="log-meta">
                          <span>{formatTs(log.createdAt || log.timestamp || '')}</span>
                          <span>{log.type || 'ACCUPICK'}</span>
                        </div>
                        <div className="log-type">{log.commandNo || '-'}</div>
                        <div className="log-content">{log.message || '-'}</div>
                      </div>
                    ))
                ) : (
                  <div className="log-card"><div className="log-content">No AccuPick log from /api/logs yet.</div></div>
                )}
              </div>
              <div className="status-grid">
                <Card icon={Cpu} title="CMD Example" value="command_no,DB01-0042,picking_qty,5,outbound_carrier_id,OUT-B003" />
                <Card icon={Cpu} title="ACK Example" value="command_no,DB01-0042,command_status,NG,actual_qty,3,abnormal_reason_code,1,command_time,4230" />
                <Card icon={Cable} title="Mode" value={summary?.accupickMode || '-'} />
                <Card icon={LayoutDashboard} title="Dashboard WS" value={summary?.dashboardWebsocket || '-'} />
              </div>
            </section>
          )}


          {currentTab === 'accupickSimulator' && (
            <section className="panel status-panel">
              <div className="panel-header"><h3>ACCUPICK SIMULATOR</h3><span>TCP ACK → RCS Mock API</span></div>
              <div className="form-grid">
                <label className="field-block">
                  <span>Command_No</span>
                  <select
                    value={ackForm.commandNo}
                    onChange={(e) => setAckForm((prev) => ({ ...prev, commandNo: e.target.value }))}
                  >
                    {orders.map((order) => (
                      <option key={order.commandNo} value={order.commandNo}>{order.commandNo}</option>
                    ))}
                    {orders.length === 0 && <option value={ackForm.commandNo}>{ackForm.commandNo}</option>}
                  </select>
                </label>
                <label className="field-block">
                  <span>Command_Status</span>
                  <select
                    value={ackForm.commandStatus}
                    onChange={(e) => setAckForm((prev) => ({ ...prev, commandStatus: e.target.value }))}
                  >
                    <option value="OK">OK</option>
                    <option value="NG">NG</option>
                  </select>
                </label>
                <label className="field-block">
                  <span>Actual_qty</span>
                  <input
                    type="number"
                    value={ackForm.actualQty}
                    onChange={(e) => setAckForm((prev) => ({ ...prev, actualQty: Number(e.target.value) }))}
                  />
                </label>
                <label className="field-block">
                  <span>Abnormal_reason_code</span>
                  <select
                    value={ackForm.abnormalReasonCode}
                    onChange={(e) => setAckForm((prev) => ({ ...prev, abnormalReasonCode: Number(e.target.value) }))}
                  >
                    <option value={0}>0 NONE</option>
                    <option value={1}>1 SHORTAGE</option>
                    <option value={2}>2 PICKING_FAIL</option>
                    <option value={3}>3 OVERFLOW</option>
                    <option value={4}>4 COMM_TIMEOUT</option>
                    <option value={5}>5 CARRIER_NOT_FOUND</option>
                    <option value={6}>6 BOX_HEIGHT_EXCEEDED</option>
                  </select>
                </label>
                <label className="field-block">
                  <span>Command_time</span>
                  <input
                    type="number"
                    value={ackForm.commandTime}
                    onChange={(e) => setAckForm((prev) => ({ ...prev, commandTime: Number(e.target.value) }))}
                  />
                </label>
                <label className="field-block">
                  <span>Error_Detail</span>
                  <input
                    value={ackForm.errorDetail}
                    onChange={(e) => setAckForm((prev) => ({ ...prev, errorDetail: e.target.value }))}
                    placeholder="Optional"
                  />
                </label>
              </div>
              <div className="button-row">
                <button className="primary-btn" onClick={submitAccupickAck} disabled={busy}>
                  <ArrowRightLeft className="w-4 h-4" /> Send AccuPick ACK
                </button>
              </div>
              {message && <div className="inline-message">{message}</div>}
              <div className="status-grid">
                <Card icon={Cpu} title="ACK Fields" value="command_no, command_status, actual_qty, abnormal_reason_code, command_time, error_detail" />
                <Card icon={AlertTriangle} title="Abnormal Codes" value="0 NONE / 1 SHORTAGE / 2 PICKING_FAIL / 3 OVERFLOW / 4 COMM_TIMEOUT / 5 CARRIER_NOT_FOUND / 6 BOX_HEIGHT_EXCEEDED" />
                <Card icon={ShieldCheck} title="Spec" value="AccuPick 作為 TCP Server；此頁模擬 ACK 回傳至 RCS" />
                <Card icon={Truck} title="Integration" value="對接現有 injectMockAck / CommandAckRequest" />
              </div>
            </section>
          )}

          {currentTab === 'db' && (
            <section className="panel status-panel">
              <div className="panel-header"><h3>DB STATUS</h3><span>PostgreSQL / JPA</span></div>
              <div className="button-row">
                <button className="primary-btn" onClick={loadDbStatus}><Database className="w-4 h-4" /> Load DB Status</button>
              </div>
              {dbStatus && (
                <div className="status-grid">
                  <Card icon={Database} title="Connection" value={dbStatus.status} />
                  <Card icon={ListOrdered} title="Order Rows" value={String(dbStatus.orders)} />
                  <Card icon={ShieldCheck} title="Users" value={String(dbStatus.users)} />
                </div>
              )}
            </section>
          )}
        </section>
      </main>

      {selectedOrder && (
        <div className="modal-backdrop" onClick={() => setSelectedOrder(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="panel-header modal-header">
              <h3>{selectedOrder.commandNo}</h3>
              <span>{selectedOrder.commandStatus}</span>
            </div>
            <div className="detail-grid">
              <Detail label="Order_No" value={selectedOrder.orderNo} />
              <Detail label="Orderline_No" value={selectedOrder.orderlineNo} />
              <Detail label="DC_ID" value={selectedOrder.dcId} />
              <Detail label="WorkStation_ID" value={selectedOrder.workstationId} />
              <Detail label="Inbound_Carrier_ID" value={selectedOrder.inboundCarrierId} />
              <Detail label="Product_ID" value={selectedOrder.productId} />
              <Detail label="On_hand_qty" value={safe(selectedOrder.onHandQty)} />
              <Detail label="Picking_qty" value={safe(selectedOrder.pickingQty)} />
              <Detail label="Outbound_Carrier_ID" value={selectedOrder.outboundCarrierId} />
              <Detail label="Command_Control_Code" value={selectedOrder.commandControlCode} />
              <Detail label="Actual_qty" value={safe(selectedOrder.actualQty)} />
              <Detail label="Abnormal_reason_code" value={abnormalName(selectedOrder.abnormalReasonCode)} />
            </div>
            <div className="payload-row">
              <div>
                <div className="payload-title">Source Payload</div>
                <pre>{selectedOrder.sourcePayload || '-'}</pre>
              </div>
              <div>
                <div className="payload-title">ACK Payload</div>
                <pre>{selectedOrder.ackPayload || '-'}</pre>
              </div>
            </div>
            <div className="button-row wrap">
              <button className="primary-btn" disabled={busy} onClick={() => sendMockAck(selectedOrder, 'OK')}>Mock OK</button>
              <button className="secondary-btn" disabled={busy} onClick={() => sendMockAck(selectedOrder, 'SHORTAGE')}>Mock SHORTAGE</button>
              <button className="secondary-btn" disabled={busy} onClick={() => sendMockAck(selectedOrder, 'OVERFLOW')}>Mock OVERFLOW</button>
              <button className="secondary-btn" disabled={busy} onClick={() => sendMockAck(selectedOrder, 'NOT_FOUND')}>Mock NOT_FOUND</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function safe(value: unknown) {
  return value === null || value === undefined || value === '' ? '-' : String(value);
}

function formatTs(ts: string) {
  const date = new Date(ts);
  return Number.isNaN(date.getTime()) ? ts : date.toLocaleString();
}

function StatusText({ status }: { status: string }) {
  const className =
    status === 'OK'
      ? 'ok'
      : status === 'NG' || status === 'COMM_TIMEOUT'
        ? 'ng'
        : status === 'Processing'
          ? 'processing'
          : 'pending';
  return <span className={`status-text ${className}`}>{status}</span>;
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className="detail-item">
      <div className="detail-label">{label}</div>
      <div className="detail-value">{value}</div>
    </div>
  );
}

function Card({ icon: Icon, title, value }: { icon: ComponentType<{ className?: string }>; title: string; value: string }) {
  return (
    <div className="info-card">
      <div className="info-head"><Icon className="w-4 h-4" /> {title}</div>
      <div className="info-value">{value}</div>
    </div>
  );
}
