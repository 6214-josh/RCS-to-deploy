import { useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';
import {
  buildRcsReplyPayload,
  buildWesPayload,
  clearLogs,
  createSystemLog,
  loadLogs,
  loadSettings,
  rcsReplies,
  saveSettings,
  sendSocketPayload,
  wesOrders,
} from '@/lib/integration';

export default function WesView() {
  const [selectedOrderId, setSelectedOrderId] = useState(wesOrders[0]?.id ?? '');
  const [selectedReplyId, setSelectedReplyId] = useState(rcsReplies[0]?.id ?? '');
  const [settings, setSettings] = useState(loadSettings());
  const [logs, setLogs] = useState(loadLogs());
  const [sending, setSending] = useState(false);

  const selectedOrder = useMemo(
    () => wesOrders.find((item) => item.id === selectedOrderId) ?? wesOrders[0],
    [selectedOrderId],
  );
  const selectedReply = useMemo(
    () => rcsReplies.find((item) => item.id === selectedReplyId) ?? rcsReplies[0],
    [selectedReplyId],
  );

  useEffect(() => {
    saveSettings(settings);
  }, [settings]);

  useEffect(() => {
    if (!settings.wes.autoEnabled) return;
    createSystemLog('wes', buildRcsReplyPayload(selectedReply), 'Auto send to WES started');
    setLogs(loadLogs());
    const timer = window.setInterval(async () => {
      const log = await sendSocketPayload('wes', settings.wes.endpointUrl, buildRcsReplyPayload(selectedReply));
      setLogs(loadLogs());
      toast(log.status === 'SUCCESS' ? '已自動送出至 WES' : '自動送出至 WES 失敗');
    }, Math.max(settings.wes.intervalSec, 1) * 1000);

    return () => {
      window.clearInterval(timer);
      createSystemLog('wes', buildRcsReplyPayload(selectedReply), 'Auto send to WES stopped');
      setLogs(loadLogs());
    };
  }, [selectedReply, settings.wes.autoEnabled, settings.wes.endpointUrl, settings.wes.intervalSec]);

  const sendOrderToRcs = async () => {
    if (!selectedOrder) return;
    setSending(true);
    const log = await sendSocketPayload('wes', settings.wes.endpointUrl, buildWesPayload(selectedOrder));
    setLogs(loadLogs());
    setSending(false);
    toast(log.status === 'SUCCESS' ? 'WES 訂單已送出' : `WES 訂單送出失敗：${log.message ?? ''}`);
  };

  const sendReplyToWes = async () => {
    if (!selectedReply) return;
    setSending(true);
    const log = await sendSocketPayload('wes', settings.wes.endpointUrl, buildRcsReplyPayload(selectedReply));
    setLogs(loadLogs());
    setSending(false);
    toast(log.status === 'SUCCESS' ? 'RCS 回覆已送回 WES' : `RCS 回覆送出失敗：${log.message ?? ''}`);
  };

  const wesLogs = logs.filter((item) => item.target === 'wes');

  return (
    <div className="h-full flex flex-col space-y-6">
      <div className="grid grid-cols-4 gap-4">
        <MetricCard label="WES 場景筆數" value={String(wesOrders.length)} />
        <MetricCard label="RCS 回覆模板" value={String(rcsReplies.length)} />
        <MetricCard label="WES Endpoint" value={settings.wes.endpointUrl} small />
        <MetricCard label="Auto Send" value={settings.wes.autoEnabled ? `ON / ${settings.wes.intervalSec}s` : 'OFF'} />
      </div>

      <div className="grid grid-cols-2 gap-6">
        <section className="bg-[#2d2d2d] p-5 rounded border border-[#3f3f3f]">
          <h3 className="text-lg font-bold text-[#f39c12] mb-4">WES → RCS 場景資料</h3>
          <div className="mb-4">
            <label className="block text-xs text-gray-400 mb-2">選擇 WES 場景</label>
            <select
              value={selectedOrderId}
              onChange={(e) => setSelectedOrderId(e.target.value)}
              className="w-full bg-[#1a1a1a] border border-[#3f3f3f] rounded px-3 py-2 text-sm"
            >
              {wesOrders.map((item) => (
                <option key={item.id} value={item.id}>{`${item.sceneCode} | ${item.productCode || '-'} | ${item.productName || '關閉本站'}`}</option>
              ))}
            </select>
          </div>

          <PayloadBlock title="WES 原始 Socket Payload" payload={selectedOrder ? buildWesPayload(selectedOrder) : ''} />

          <button
            onClick={sendOrderToRcs}
            disabled={sending}
            className="mt-4 px-4 py-2 bg-[#f39c12] text-[#1a1a1a] rounded font-semibold hover:bg-yellow-500 disabled:opacity-50"
          >
            手動送出 WES Socket
          </button>

          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="text-left text-gray-400 border-b border-[#3f3f3f]">
                <tr>
                  <th className="py-2">場景</th>
                  <th>產品代號</th>
                  <th>產品名稱</th>
                  <th>數量</th>
                </tr>
              </thead>
              <tbody>
                {wesOrders.map((item) => (
                  <tr key={item.id} className="border-b border-[#3a3a3a]">
                    <td className="py-2">{item.sceneCode}</td>
                    <td>{item.productCode || '-'}</td>
                    <td>{item.productName || item.sceneDescription}</td>
                    <td>{item.qty || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <section className="bg-[#2d2d2d] p-5 rounded border border-[#3f3f3f]">
          <h3 className="text-lg font-bold text-[#f39c12] mb-4">RCS → WES 回覆控制</h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs text-gray-400 mb-2">送出 Endpoint</label>
              <input
                value={settings.wes.endpointUrl}
                onChange={(e) => setSettings((prev) => ({ ...prev, wes: { ...prev.wes, endpointUrl: e.target.value } }))}
                className="w-full bg-[#1a1a1a] border border-[#3f3f3f] rounded px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-400 mb-2">WES WebSocket URL</label>
              <input
                value={settings.wes.websocketUrl}
                onChange={(e) => setSettings((prev) => ({ ...prev, wes: { ...prev.wes, websocketUrl: e.target.value } }))}
                className="w-full bg-[#1a1a1a] border border-[#3f3f3f] rounded px-3 py-2 text-sm"
              />
            </div>
          </div>

          <div className="grid grid-cols-[1fr_120px_120px] gap-3 mt-4 items-end">
            <div>
              <label className="block text-xs text-gray-400 mb-2">選擇回覆模板</label>
              <select
                value={selectedReplyId}
                onChange={(e) => setSelectedReplyId(e.target.value)}
                className="w-full bg-[#1a1a1a] border border-[#3f3f3f] rounded px-3 py-2 text-sm"
              >
                {rcsReplies.map((item) => (
                  <option key={item.id} value={item.id}>{`${item.sceneCode} | ${item.normalConfirmCode ?? item.abnormalCompleteCode ?? '-'} | ${item.abnormalReasonCode ?? 'OK'}`}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs text-gray-400 mb-2">間隔(秒)</label>
              <input
                type="number"
                min={1}
                value={settings.wes.intervalSec}
                onChange={(e) => setSettings((prev) => ({ ...prev, wes: { ...prev.wes, intervalSec: Number(e.target.value || 1) } }))}
                className="w-full bg-[#1a1a1a] border border-[#3f3f3f] rounded px-3 py-2 text-sm"
              />
            </div>
            <label className="flex items-center gap-2 text-sm text-[#ecf0f1] pb-2">
              <input
                type="checkbox"
                checked={settings.wes.autoEnabled}
                onChange={(e) => setSettings((prev) => ({ ...prev, wes: { ...prev.wes, autoEnabled: e.target.checked } }))}
              />
              自動送出
            </label>
          </div>

          <PayloadBlock title="RCS 回 WES Payload" payload={selectedReply ? buildRcsReplyPayload(selectedReply) : ''} />

          <div className="mt-4 flex gap-3">
            <button onClick={sendReplyToWes} disabled={sending} className="px-4 py-2 bg-[#f39c12] text-[#1a1a1a] rounded font-semibold hover:bg-yellow-500 disabled:opacity-50">
              手動送出 RCS 回覆
            </button>
            <button onClick={() => { clearLogs(); setLogs([]); toast('已清空 WES / AccuPick 通訊紀錄'); }} className="px-4 py-2 bg-[#4b5563] text-white rounded hover:bg-[#6b7280]">
              清空通訊紀錄
            </button>
          </div>
        </section>
      </div>

      <section className="bg-[#2d2d2d] p-5 rounded border border-[#3f3f3f] flex-1 min-h-[280px]">
        <h3 className="text-lg font-bold text-[#f39c12] mb-4">WES 通訊紀錄</h3>
        <div className="space-y-2 max-h-[340px] overflow-auto">
          {wesLogs.length === 0 ? (
            <div className="text-sm text-gray-400">目前尚無 WES 通訊紀錄。</div>
          ) : (
            wesLogs.map((log) => (
              <div key={log.id} className="bg-[#1a1a1a] border border-[#3a3a3a] rounded p-3 text-sm">
                <div className="flex justify-between text-xs text-gray-400 mb-1">
                  <span>{log.createdAt}</span>
                  <span className={log.status === 'SUCCESS' ? 'text-green-400' : log.status === 'FAILED' ? 'text-red-400' : 'text-blue-400'}>{log.status}</span>
                </div>
                <div className="font-mono break-all text-[#ecf0f1]">{log.payload}</div>
                <div className="text-xs text-gray-400 mt-1">{log.message ?? '-'} {log.endpointUrl ? `| ${log.endpointUrl}` : ''}</div>
              </div>
            ))
          )}
        </div>
      </section>
    </div>
  );
}

function MetricCard({ label, value, small = false }: { label: string; value: string; small?: boolean }) {
  return (
    <div className="bg-[#2d2d2d] p-4 border-l-2 border-[#f39c12] rounded shadow-sm">
      <p className="text-[10px] text-gray-500 uppercase">{label}</p>
      <p className={`font-bold mt-1 text-[#ecf0f1] ${small ? 'text-sm break-all' : 'text-xl'}`}>{value}</p>
    </div>
  );
}

function PayloadBlock({ title, payload }: { title: string; payload: string }) {
  return (
    <div className="mt-4">
      <div className="text-xs text-gray-400 mb-2">{title}</div>
      <pre className="bg-[#1a1a1a] border border-[#3a3a3a] rounded p-3 text-xs text-[#ecf0f1] whitespace-pre-wrap break-all">{payload}</pre>
    </div>
  );
}
