import { useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';
import {
  accupickCommands,
  buildAccupickPayload,
  clearLogs,
  createSystemLog,
  loadLogs,
  loadSettings,
  saveSettings,
  sendSocketPayload,
} from '@/lib/integration';
import { connect, disconnect, isConnected as wsIsConnected, sendMessage } from '../services/websocket';

export default function AccupickView() {
  const [settings, setSettings] = useState(loadSettings());
  const [logs, setLogs] = useState(loadLogs());
  const [selectedId, setSelectedId] = useState(accupickCommands[0]?.id ?? '');
  const [isWsConnected, setIsWsConnected] = useState(false);
  const [receivedMessages, setReceivedMessages] = useState<string[]>([]);
  const [sending, setSending] = useState(false);

  const selectedCommand = useMemo(
    () => accupickCommands.find((item) => item.id === selectedId) ?? accupickCommands[0],
    [selectedId],
  );

  useEffect(() => {
    saveSettings(settings);
  }, [settings]);

  useEffect(() => {
    setIsWsConnected(wsIsConnected());
  }, []);

  useEffect(() => {
    if (!settings.accupick.autoEnabled || !selectedCommand) return;
    createSystemLog('accupick', buildAccupickPayload(selectedCommand), 'Auto send to AccuPick started');
    setLogs(loadLogs());

    const timer = window.setInterval(async () => {
      const log = await sendSocketPayload('accupick', settings.accupick.endpointUrl, buildAccupickPayload(selectedCommand));
      setLogs(loadLogs());
      toast(log.status === 'SUCCESS' ? '已自動送出至 AccuPick' : '自動送出至 AccuPick 失敗');
    }, Math.max(settings.accupick.intervalSec, 1) * 1000);

    return () => {
      window.clearInterval(timer);
      createSystemLog('accupick', buildAccupickPayload(selectedCommand), 'Auto send to AccuPick stopped');
      setLogs(loadLogs());
    };
  }, [selectedCommand, settings.accupick.autoEnabled, settings.accupick.endpointUrl, settings.accupick.intervalSec]);

  const connectWebSocket = () => {
    connect(
      settings.accupick.websocketUrl,
      (event) => {
        const message = String(event.data ?? '');
        setReceivedMessages((prev) => [message, ...prev].slice(0, 50));
        createSystemLog('accupick', message, 'WebSocket message received from backend');
        setLogs(loadLogs());
      },
      () => {
        setIsWsConnected(true);
        toast('AccuPick WebSocket 已連線');
      },
      () => {
        setIsWsConnected(false);
        toast('AccuPick WebSocket 已斷線');
      },
    );
  };

  const disconnectWebSocket = () => {
    disconnect();
    setIsWsConnected(false);
  };

  const sendToAccuPick = async () => {
    if (!selectedCommand) return;
    setSending(true);
    const log = await sendSocketPayload('accupick', settings.accupick.endpointUrl, buildAccupickPayload(selectedCommand));
    setLogs(loadLogs());
    setSending(false);
    toast(log.status === 'SUCCESS' ? '已送出 AccuPick Socket' : `AccuPick Socket 送出失敗：${log.message ?? ''}`);
  };

  const pingBackendWs = () => {
    if (!isWsConnected) {
      toast('請先連線 WebSocket');
      return;
    }
    sendMessage(`PING_ACCUPICK:${selectedCommand ? buildAccupickPayload(selectedCommand) : 'PING'}`);
    toast('已透過 WebSocket 通知後端');
  };

  const accupickLogs = logs.filter((item) => item.target === 'accupick');

  return (
    <div className="flex flex-col h-full space-y-6">
      <div className="grid grid-cols-4 gap-4">
        <MetricCard label="AccuPick 命令模板" value={String(accupickCommands.length)} />
        <MetricCard label="Endpoint" value={settings.accupick.endpointUrl} small />
        <MetricCard label="WebSocket" value={isWsConnected ? 'CONNECTED' : 'DISCONNECTED'} />
        <MetricCard label="Auto Send" value={settings.accupick.autoEnabled ? `ON / ${settings.accupick.intervalSec}s` : 'OFF'} />
      </div>

      <div className="grid grid-cols-2 gap-6">
        <section className="bg-[#2d2d2d] p-5 rounded border border-[#3f3f3f]">
          <h3 className="text-lg font-bold text-[#f39c12] mb-4">AccuPick Socket 控制</h3>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs text-gray-400 mb-2">AccuPick API Endpoint</label>
              <input
                value={settings.accupick.endpointUrl}
                onChange={(e) => setSettings((prev) => ({ ...prev, accupick: { ...prev.accupick, endpointUrl: e.target.value } }))}
                className="w-full bg-[#1a1a1a] border border-[#3f3f3f] rounded px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-400 mb-2">AccuPick WebSocket URL</label>
              <input
                value={settings.accupick.websocketUrl}
                onChange={(e) => setSettings((prev) => ({ ...prev, accupick: { ...prev.accupick, websocketUrl: e.target.value } }))}
                className="w-full bg-[#1a1a1a] border border-[#3f3f3f] rounded px-3 py-2 text-sm"
              />
            </div>
          </div>

          <div className="grid grid-cols-[1fr_120px_120px] gap-3 mt-4 items-end">
            <div>
              <label className="block text-xs text-gray-400 mb-2">命令模板</label>
              <select
                value={selectedId}
                onChange={(e) => setSelectedId(e.target.value)}
                className="w-full bg-[#1a1a1a] border border-[#3f3f3f] rounded px-3 py-2 text-sm"
              >
                {accupickCommands.map((item) => (
                  <option key={item.id} value={item.id}>{`${item.sceneCode} | ${item.commandName},${item.carrierId}`}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs text-gray-400 mb-2">間隔(秒)</label>
              <input
                type="number"
                min={1}
                value={settings.accupick.intervalSec}
                onChange={(e) => setSettings((prev) => ({ ...prev, accupick: { ...prev.accupick, intervalSec: Number(e.target.value || 1) } }))}
                className="w-full bg-[#1a1a1a] border border-[#3f3f3f] rounded px-3 py-2 text-sm"
              />
            </div>
            <label className="flex items-center gap-2 text-sm text-[#ecf0f1] pb-2">
              <input
                type="checkbox"
                checked={settings.accupick.autoEnabled}
                onChange={(e) => setSettings((prev) => ({ ...prev, accupick: { ...prev.accupick, autoEnabled: e.target.checked } }))}
              />
              自動送出
            </label>
          </div>

          <div className="mt-4">
            <div className="text-xs text-gray-400 mb-2">AccuPick 原始 Payload</div>
            <pre className="bg-[#1a1a1a] border border-[#3a3a3a] rounded p-3 text-xs text-[#ecf0f1] whitespace-pre-wrap break-all">{selectedCommand ? buildAccupickPayload(selectedCommand) : ''}</pre>
          </div>

          <div className="mt-4 flex flex-wrap gap-3">
            <button onClick={sendToAccuPick} disabled={sending} className="px-4 py-2 bg-[#f39c12] text-[#1a1a1a] rounded font-semibold hover:bg-yellow-500 disabled:opacity-50">手動送出 AccuPick Socket</button>
            <button onClick={connectWebSocket} disabled={isWsConnected} className="px-4 py-2 bg-blue-700 text-white rounded hover:bg-blue-800 disabled:opacity-50">連線 WebSocket</button>
            <button onClick={disconnectWebSocket} disabled={!isWsConnected} className="px-4 py-2 bg-red-700 text-white rounded hover:bg-red-800 disabled:opacity-50">斷線 WebSocket</button>
            <button onClick={pingBackendWs} className="px-4 py-2 bg-[#4b5563] text-white rounded hover:bg-[#6b7280]">通知後端轉送</button>
            <button onClick={() => { clearLogs(); setLogs([]); toast('已清空 WES / AccuPick 通訊紀錄'); }} className="px-4 py-2 bg-[#374151] text-white rounded hover:bg-[#4b5563]">清空紀錄</button>
          </div>
        </section>

        <section className="bg-[#2d2d2d] p-5 rounded border border-[#3f3f3f]">
          <h3 className="text-lg font-bold text-[#f39c12] mb-4">AccuPick 資料對照</h3>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="text-left text-gray-400 border-b border-[#3f3f3f]">
                <tr>
                  <th className="py-2">場景</th>
                  <th>命令</th>
                  <th>Carrier</th>
                  <th>預期回覆</th>
                </tr>
              </thead>
              <tbody>
                {accupickCommands.map((item) => (
                  <tr key={item.id} className="border-b border-[#3a3a3a]">
                    <td className="py-2">{item.sceneCode}</td>
                    <td>{item.commandName}</td>
                    <td>{item.carrierId}</td>
                    <td className="font-mono text-xs">{item.expectedReply}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="mt-5">
            <div className="text-xs text-gray-400 mb-2">WebSocket 接收訊息</div>
            <div className="max-h-[220px] overflow-auto space-y-2">
              {receivedMessages.length === 0 ? (
                <div className="text-sm text-gray-400">目前尚未收到後端 WebSocket 訊息。</div>
              ) : (
                receivedMessages.map((item, index) => (
                  <div key={`${item}-${index}`} className="bg-[#1a1a1a] border border-[#3a3a3a] rounded p-3 text-xs font-mono break-all">{item}</div>
                ))
              )}
            </div>
          </div>
        </section>
      </div>

      <section className="bg-[#2d2d2d] p-5 rounded border border-[#3f3f3f] flex-1 min-h-[260px]">
        <h3 className="text-lg font-bold text-[#f39c12] mb-4">AccuPick 通訊紀錄</h3>
        <div className="space-y-2 max-h-[320px] overflow-auto">
          {accupickLogs.length === 0 ? (
            <div className="text-sm text-gray-400">目前尚無 AccuPick 通訊紀錄。</div>
          ) : (
            accupickLogs.map((log) => (
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
