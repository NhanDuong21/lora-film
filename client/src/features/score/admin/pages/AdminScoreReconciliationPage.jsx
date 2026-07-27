import { useState, useEffect } from 'react';
import useAdminScore from '../hooks/useAdminScore';
import { ShieldAlert, Play, RefreshCw, CheckCircle, AlertCircle, Search, FileText, Check } from 'lucide-react';

export default function AdminScoreReconciliationPage() {
  const {
    reconciliationRuns,
    reconciliationDetails,
    fetchReconciliationRuns,
    fetchReconciliationDetails,
    runReconciliation,
    isLoadingOperations
  } = useAdminScore();

  const [batchSize, setBatchSize] = useState('500');
  const [remark, setRemark] = useState('');
  const [notification, setNotification] = useState(null);
  const [selectedRunId, setSelectedRunId] = useState(null);
  const [filterStatus, setFilterStatus] = useState('ALL');

  useEffect(() => {
    fetchReconciliationRuns({ page: 0, size: 20 });
  }, [fetchReconciliationRuns]);

  const showNotify = (msg, type = 'success') => {
    setNotification({ msg, type });
    setTimeout(() => setNotification(null), 4000);
  };

  const handleTriggerRecon = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        batchSize: parseInt(batchSize, 10) || 500,
        remark: remark || `Manual recon by Admin at ${new Date().toLocaleTimeString()}`
      };
      const res = await runReconciliation(payload);
      showNotify(`Đã kích hoạt đối soát thành công! Batch code: ${res?.batchCode || res?.data?.batchCode}`);
      setRemark('');
      await fetchReconciliationRuns({ page: 0, size: 20 });
    } catch (err) {
      showNotify(err.response?.data?.message || 'Lỗi khi kích hoạt đối soát', 'error');
    }
  };

  const handleSelectRun = async (runId) => {
    setSelectedRunId(runId);
    await fetchReconciliationDetails({ runId, page: 0, size: 50 });
  };

  return (
    <div className="space-y-8 p-6 max-w-7xl mx-auto">
      {/* Notification Toast */}
      {notification && (
        <div className={`fixed bottom-6 right-6 z-50 flex items-center gap-3 px-5 py-4 rounded-xl border shadow-2xl backdrop-blur-md transition-all animate-in fade-in slide-in-from-bottom-5 ${
          notification.type === 'error'
            ? 'bg-rose-950/90 border-rose-500/40 text-rose-200'
            : 'bg-emerald-950/90 border-emerald-500/40 text-emerald-200'
        }`}>
          {notification.type === 'error' ? (
            <AlertCircle className="w-5 h-5 text-rose-400 shrink-0" />
          ) : (
            <CheckCircle className="w-5 h-5 text-emerald-400 shrink-0" />
          )}
          <span className="text-sm font-bold">{notification.msg}</span>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-zinc-900/60 p-6 rounded-2xl border border-zinc-800/80 backdrop-blur-md">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-white flex items-center gap-2.5">
            <ShieldAlert className="w-7 h-7 text-amber-500 shrink-0" />
            <span>Đối Soát Dữ Liệu Điểm Thưởng (Reconciliation)</span>
          </h1>
          <p className="text-sm text-zinc-400 mt-1">
            Kiểm tra tính toàn vẹn giữa sổ cái (Score History) và số dư hiện tại (User Score).
          </p>
        </div>
        <button
          onClick={() => fetchReconciliationRuns({ page: 0, size: 20 })}
          disabled={isLoadingOperations}
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-zinc-800 hover:bg-zinc-700 font-bold text-xs text-white transition-all border border-zinc-700/60 shadow-sm disabled:opacity-50 shrink-0 cursor-pointer"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isLoadingOperations ? 'animate-spin text-amber-400' : ''}`} />
          <span>Làm mới danh sách</span>
        </button>
      </div>

      {/* Trigger Box */}
      <form onSubmit={handleTriggerRecon} className="bg-zinc-900/80 p-6 rounded-2xl border border-zinc-800 space-y-4">
        <h2 className="text-sm font-black uppercase text-amber-400 tracking-wider flex items-center gap-2">
          <Play className="w-4 h-4" />
          <span>Kích hoạt đợt đối soát mới (Manual Run)</span>
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
          <div>
            <label className="block text-xs font-bold text-zinc-400 uppercase mb-2">Kích thước batch (Tài khoản/đợt)</label>
            <input
              type="number"
              value={batchSize}
              onChange={(e) => setBatchSize(e.target.value)}
              className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white font-mono"
            />
          </div>
          <div>
            <label className="block text-xs font-bold text-zinc-400 uppercase mb-2">Ghi chú (Remark)</label>
            <input
              type="text"
              value={remark}
              onChange={(e) => setRemark(e.target.value)}
              placeholder="Ví dụ: Đối soát định kỳ tháng 7"
              className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white"
            />
          </div>
          <button
            type="submit"
            disabled={isLoadingOperations}
            className="px-6 py-2.5 bg-amber-500 hover:bg-amber-600 text-zinc-950 font-black rounded-xl text-sm transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
          >
            <Play className="w-4 h-4 fill-zinc-950" />
            <span>Chạy Đối Soát Ngay</span>
          </button>
        </div>
      </form>

      {/* Runs Table */}
      <div className="bg-zinc-900/80 rounded-2xl border border-zinc-800 overflow-hidden">
        <div className="p-5 border-b border-zinc-800 font-bold text-sm text-white flex justify-between items-center">
          <span>Danh sách các đợt đối soát (Reconciliation Runs)</span>
          <span className="text-xs font-mono text-zinc-400">
            Click vào đợt để xem chi tiết chênh lệch
          </span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-zinc-800/80 text-[11px] font-bold text-zinc-400 uppercase bg-zinc-950/40">
                <th className="py-3 px-4">ID</th>
                <th className="py-3 px-4">Mã Batch Code</th>
                <th className="py-3 px-4">Thời gian chạy</th>
                <th className="py-3 px-4 text-right">Tổng user</th>
                <th className="py-3 px-4 text-right">Khớp (Matched)</th>
                <th className="py-3 px-4 text-right">Lệch (Mismatched)</th>
                <th className="py-3 px-4">Trạng thái</th>
                <th className="py-3 px-4">Ghi chú</th>
                <th className="py-3 px-4 text-right">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800/60 text-xs text-zinc-300">
              {reconciliationRuns && reconciliationRuns.content?.length > 0 ? (
                reconciliationRuns.content.map((run) => (
                  <tr key={run.id} className={`hover:bg-zinc-800/40 transition-colors cursor-pointer ${selectedRunId === run.id ? 'bg-amber-500/10' : ''}`} onClick={() => handleSelectRun(run.id)}>
                    <td className="py-3 px-4 font-mono text-amber-400 font-bold">#{run.id}</td>
                    <td className="py-3 px-4 font-mono font-bold text-white">{run.batchCode}</td>
                    <td className="py-3 px-4 font-mono text-zinc-400">{run.startedAt?.replace('T', ' ').substring(0, 19)}</td>
                    <td className="py-3 px-4 text-right font-mono font-bold">{run.totalUsers}</td>
                    <td className="py-3 px-4 text-right font-mono font-bold text-emerald-400">{run.matchedUsers}</td>
                    <td className={`py-3 px-4 text-right font-mono font-black ${run.mismatchedUsers > 0 ? 'text-rose-500' : 'text-zinc-500'}`}>
                      {run.mismatchedUsers}
                    </td>
                    <td className="py-3 px-4 font-bold uppercase">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] ${
                        run.status === 'COMPLETED' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-amber-500/10 text-amber-400'
                      }`}>
                        {run.status}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-zinc-400 truncate max-w-xs">{run.remark}</td>
                    <td className="py-3 px-4 text-right">
                      <button
                        onClick={(e) => { e.stopPropagation(); handleSelectRun(run.id); }}
                        className="px-3 py-1.5 bg-zinc-800 hover:bg-zinc-700 text-white rounded-lg text-[11px] font-bold"
                      >
                        Xem chi tiết
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="9" className="py-8 text-center text-zinc-500">Chưa có đợt đối soát nào. Hãy kích hoạt ở trên.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Discrepancy Details Table */}
      {selectedRunId && (
        <div className="bg-zinc-900/80 rounded-2xl border border-zinc-800 overflow-hidden animate-in fade-in">
          <div className="p-5 border-b border-zinc-800 font-bold text-sm text-white flex justify-between items-center">
            <span className="text-amber-400">Chi tiết lệch dữ liệu của đợt đối soát #{selectedRunId}</span>
            <button
              onClick={() => setSelectedRunId(null)}
              className="text-xs text-zinc-400 hover:text-white underline cursor-pointer"
            >
              Đóng chi tiết
            </button>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-zinc-800/80 text-[11px] font-bold text-zinc-400 uppercase bg-zinc-950/40">
                  <th className="py-3 px-4">ID</th>
                  <th className="py-3 px-4">User ID</th>
                  <th className="py-3 px-4 text-right">Số dư Ledger</th>
                  <th className="py-3 px-4 text-right">Số dư Cache</th>
                  <th className="py-3 px-4 text-right">Lệch Balance</th>
                  <th className="py-3 px-4 text-right">Lệch Accumulated</th>
                  <th className="py-3 px-4">Trạng thái</th>
                  <th className="py-3 px-4">Ghi chú</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/60 text-xs text-zinc-300">
                {reconciliationDetails && reconciliationDetails.content?.length > 0 ? (
                  reconciliationDetails.content.map((det) => (
                    <tr key={det.id} className="hover:bg-zinc-800/40 transition-colors">
                      <td className="py-3 px-4 font-mono text-zinc-500">#{det.id}</td>
                      <td className="py-3 px-4 font-mono font-black text-white">#{det.userId}</td>
                      <td className="py-3 px-4 text-right font-mono text-emerald-400">{det.ledgerBalance}</td>
                      <td className="py-3 px-4 text-right font-mono text-amber-400">{det.currentBalance}</td>
                      <td className={`py-3 px-4 text-right font-mono font-black ${det.balanceDifference !== 0 ? 'text-rose-500' : 'text-zinc-500'}`}>
                        {det.balanceDifference}
                      </td>
                      <td className={`py-3 px-4 text-right font-mono font-black ${det.accumulatedDifference !== 0 ? 'text-rose-500' : 'text-zinc-500'}`}>
                        {det.accumulatedDifference}
                      </td>
                      <td className="py-3 px-4 font-bold">
                        <span className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] uppercase font-mono ${
                          det.status === 'RESOLVED' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                        }`}>
                          {det.status}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-zinc-400">{det.remark}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="8" className="py-6 text-center text-zinc-500">
                      Không phát hiện tài khoản nào có chênh lệch dữ liệu trong đợt này! Toàn bộ đều khớp (MATCHED).
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
