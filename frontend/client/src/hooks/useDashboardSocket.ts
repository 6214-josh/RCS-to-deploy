import { useEffect, useMemo, useRef, useState } from 'react';
import { fetchLogs, fetchOrders, fetchSummary, wsUrl } from '@/lib/api';
import {
  CommunicationLog,
  DashboardSocketMessage,
  DashboardSummary,
  PickingOrder,
} from '@/types/dashboard';

export function useDashboardSocket() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [orders, setOrders] = useState<PickingOrder[]>([]);
  const [logs, setLogs] = useState<CommunicationLog[]>([]);
  const [connected, setConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      try {
        const [initialSummary, initialOrders, initialLogs] = await Promise.all([
          fetchSummary(),
          fetchOrders(),
          fetchLogs(),
        ]);
        if (cancelled) return;
        setSummary(initialSummary);
        setOrders(initialOrders);
        setLogs(initialLogs);
      } catch (error) {
        console.error(error);
      }
    }

    bootstrap();

    const socket = new WebSocket(wsUrl('/ws/dashboard'));
    wsRef.current = socket;

    socket.onopen = () => setConnected(true);
    socket.onclose = () => setConnected(false);
    socket.onerror = () => setConnected(false);
    socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(String(event.data || '{}')) as DashboardSocketMessage;
        if (payload.type === 'dashboard:init') {
          setSummary(payload.summary);
          setOrders(payload.orders);
          setLogs(payload.logs);
          return;
        }
        if (payload.type === 'dashboard:summary') {
          setSummary(payload.summary);
          return;
        }
        if (payload.type === 'dashboard:order-updated') {
          setSummary(payload.summary);
          setOrders((prev) => mergeOrder(prev, payload.order));
          return;
        }
        if (payload.type === 'dashboard:log') {
          setSummary(payload.summary);
          setLogs((prev) => [payload.log, ...prev.filter((item) => item.id !== payload.log.id)].slice(0, 120));
        }
      } catch (error) {
        console.error('dashboard ws parse error', error);
      }
    };

    return () => {
      cancelled = true;
      socket.close();
      wsRef.current = null;
    };
  }, []);

  const queueOrders = useMemo(
    () => orders.filter((item) => item.commandStatus === 'Pending' || item.commandStatus === 'Processing'),
    [orders],
  );

  return { summary, orders, queueOrders, logs, connected };
}

function mergeOrder(prev: PickingOrder[], next: PickingOrder) {
  const without = prev.filter((item) => item.commandNo !== next.commandNo);
  return [next, ...without].sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1));
}
