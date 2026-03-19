import { useState, useEffect } from 'react';

/**
 * DB Status View - Database Status Monitoring
 * Design: Industrial dark theme with orange accents
 * Features: Database connection status, record count, sync information
 */

export default function DbStatusView() {
  const [totalRecords, setTotalRecords] = useState(0);
  const [lastSync, setLastSync] = useState('');
  const [loading, setLoading] = useState(false);

  const fetchDbStatus = async () => {
    setLoading(true);
    try {
      // 實際 API 呼叫: const response = await fetch('/api/mgr/db-status');
      // const data = await response.json();
      // setTotalRecords(data.userCount);
      // setLastSync(new Date().toLocaleString());

      // 模擬資料
      setTotalRecords(Math.floor(Math.random() * 1000));
      setLastSync(new Date().toLocaleString());
    } catch (error) {
      console.error('Failed to fetch DB status', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDbStatus();
    // 每 5 秒刷新一次
    const interval = setInterval(fetchDbStatus, 5000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="max-w-2xl space-y-6">
      <div className="bg-[#2d2d2d] p-6 rounded border border-[#3f3f3f]">
        <h4 className="text-[#f39c12] font-bold uppercase mb-4">Database Connection Status</h4>
        <div className="space-y-4">
          <div className="flex justify-between border-b border-[#3f3f3f] pb-2">
            <span className="text-gray-400">Database Type</span>
            <span className="font-mono text-[#ecf0f1]">PostgreSQL 15.0</span>
          </div>
          <div className="flex justify-between border-b border-[#3f3f3f] pb-2">
            <span className="text-gray-400">Connection Status</span>
            <span className="text-green-400 font-bold">ACTIVE</span>
          </div>
          <div className="flex justify-between border-b border-[#3f3f3f] pb-2">
            <span className="text-gray-400">Total Records</span>
            <span className="font-mono text-[#ecf0f1]">{loading ? 'Loading...' : totalRecords}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-400">Last Sync</span>
            <span className="font-mono text-xs text-[#ecf0f1]">{lastSync}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
