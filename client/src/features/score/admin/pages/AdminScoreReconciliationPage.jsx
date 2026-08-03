import { useState, useEffect } from 'react';
import useAdminScore from '../hooks/useAdminScore';
import { ShieldAlert, Play, RefreshCw, CheckCircle, AlertCircle } from 'lucide-react';

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
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-zinc-900/40 backdrop-blur-md border border-zinc-800/50 p-6 rounded-[2rem] shadow-2xl shadow-black/20">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-white flex items-center gap-2.5">
            <ShieldAlert className="w-7 h-7 text-brand-orange shrink-0" />
            <span>Đối Soát Dữ Liệu Điểm Thưởng (Reconciliation)</span>
          </h1>
          <p className="text-[11px] font-medium tracking-wide text-zinc-400 mt-1">
            Kiểm tra tính toàn vẹn giữa sổ cái (Score History) và số dư hiện tại (User Score).
          </p>
        </div>
        <button
          onClick={() => fetchReconciliationRuns({ page: 0, size: 20 })}
          disabled={isLoadingOperations}
          className="inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-2xl bg-white/5 hover:bg-white/10 border border-white/10 font-black text-[11px] uppercase tracking-widest text-zinc-300 transition-all shadow-inner disabled:opacity-50 shrink-0 cursor-pointer"
        >
          <RefreshCw className={`w-4 h-4 ${isLoadingOperations ? 'animate-spin text-brand-orange' : ''}`} />
          <span>Làm mới danh sách</span>
        </button>
      </div>

      {/* Trigger Box */}
      <form onSubmit={handleTriggerRecon} className="bg-zinc-900/40 backdrop-blur-md p-8 rounded-[2rem] border border-zinc-800/50 shadow-xl shadow-black/10 space-y-6">
        <h2 className="text-[11px] font-black uppercase text-brand-orange tracking-widest flex items-center gap-2">
          <Play className="w-4 h-4" />
          <span>Kích hoạt đợt đối soát mới (Manual Run)</span>
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-end">
          <div>
            <label className="block text-[10px] font-black uppercase tracking-widest text-zinc-500 mb-2">Kích thước batch (Tài khoản/đợt)</label>
            <input
              type="number"
              value={batchSize}
              onChange={(e) => setBatchSize(e.target.value)}
              className="w-full bg-black/20 border border-zinc-800/80 rounded-2xl px-5 py-3.5 text-xs text-white font-mono placeholder-zinc-600 focus:outline-none focus:border-brand-orange/50 transition-colors shadow-inner"
            />
          </div>
          <div>
            <label className="block text-[10px] font-black uppercase tracking-widest text-zinc-500 mb-2">Ghi chú (Remark)</label>
            <input
              type="text"
              value={remark}
              onChange={(e) => setRemark(e.target.value)}
              placeholder="Ví dụ: Đối soát định kỳ tháng 7"
              className="w-full bg-black/20 border border-zinc-800/80 rounded-2xl px-5 py-3.5 text-xs text-white placeholder-zinc-600 focus:outline-none focus:border-brand-orange/50 transition-colors shadow-inner tracking-wide"
            />
          </div>
          <button
            type="submit"
            disabled={isLoadingOperations}
            className="px-8 py-3.5 bg-brand-orange hover:bg-opacity-90 text-zinc-950 font-black rounded-2xl text-[11px] uppercase tracking-widest transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50 shadow-xl shadow-brand-orange/20"
          >
            <Play className="w-4 h-4 fill-zinc-950" />
            <span>Chạy Đối Soát Ngay</span>
          </button>
        </div>
      </form>

      {/* Runs Table */}
      <div className="bg-zinc-900/40 backdrop-blur-md rounded-[2rem] border border-zinc-800/50 shadow-xl shadow-black/10 overflow-hidden">
        <div className="p-6 border-b border-zinc-800/50 font-black text-sm text-white flex justify-between items-center bg-black/10">
          <span className="tracking-wide">Danh sách các đợt đối soát (Reconciliation Runs)</span>
          <span className="text-[10px] font-black tracking-widest text-zinc-500 uppercase">
            Click vào đợt để xem chi tiết chênh lệch
          </span>
        </div>
        <div className="overflow-x-auto p-4">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-zinc-800/50 text-[10px] font-black tracking-widest text-zinc-500 uppercase">
                <th className="py-4 px-4">ID</th>
                <th className="py-4 px-4">Mã Batch Code</th>
                <th className="py-4 px-4">Thời gian chạy</th>
                <th className="py-4 px-4 text-right">Tổng user</th>
                <th className="py-4 px-4 text-right">Khớp (Matched)</th>
                <th className="py-4 px-4 text-right">Lệch (Mismatched)</th>
                <th className="py-4 px-4">Trạng thái</th>
                <th className="py-4 px-4">Ghi chú</th>
                <th className="py-4 px-4 text-right">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800/30 text-xs text-zinc-300">
              {reconciliationRuns && reconciliationRuns.content?.length > 0 ? (
                reconciliationRuns.content.map((run) => (
                  <tr key={run.id} className={`hover:bg-white/5 transition-colors cursor-pointer group ${selectedRunId === run.id ? 'bg-brand-orange/10 border-brand-orange/20' : ''}`} onClick={() => handleSelectRun(run.id)}>
                    <td className="py-4 px-4 font-mono text-brand-orange font-bold tracking-wide">#{run.id}</td>
                    <td className="py-4 px-4 font-mono font-black text-white tracking-wide">{run.batchCode}</td>
                    <td className="py-4 px-4 font-mono text-zinc-500 group-hover:text-zinc-400 tracking-wide transition-colors">{run.startedAt?.replace('T', ' ').substring(0, 19)}</td>
                    <td className="py-4 px-4 text-right font-mono font-black">{run.totalUsers}</td>
                    <td className="py-4 px-4 text-right font-mono font-black text-emerald-400">{run.matchedUsers}</td>
                    <td className={`py-4 px-4 text-right font-mono font-black ${run.mismatchedUsers > 0 ? 'text-red-500' : 'text-zinc-500'}`}>
                      {run.mismatchedUsers}
                    </td>
                    <td className="py-4 px-4 font-black uppercase">
                      <span className={`inline-flex items-center px-3 py-1 rounded-xl text-[9px] tracking-widest shadow-inner ${
                        run.status === 'COMPLETED' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-brand-orange/10 text-brand-orange border border-brand-orange/20'
                      }`}>
                        {run.status}
                      </span>
                    </td>
                    <td className="py-4 px-4 text-[11px] font-medium text-zinc-400 truncate max-w-xs">{run.remark}</td>
                    <td className="py-4 px-4 text-right">
                      <button
                        onClick={(e) => { e.stopPropagation(); handleSelectRun(run.id); }}
                        className="px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/10 text-white rounded-xl text-[10px] uppercase tracking-widest font-black transition-colors shadow-inner"
                      >
                        Xem chi tiết
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="9" className="py-16 text-center text-zinc-500 font-black tracking-wide">Chưa có đợt đối soát nào. Hãy kích hoạt ở trên.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Discrepancy Details Table */}
      {selectedRunId && (
        <div className="bg-zinc-900/40 backdrop-blur-md rounded-[2rem] border border-zinc-800/50 shadow-xl shadow-black/10 overflow-hidden animate-in fade-in zoom-in-95 duration-200">
          <div className="p-6 border-b border-zinc-800/50 font-black text-sm text-white flex justify-between items-center bg-black/10">
            <span className="text-brand-orange tracking-wide">Chi tiết lệch dữ liệu của đợt đối soát <span className="font-mono">#{selectedRunId}</span></span>
            <button
              onClick={() => setSelectedRunId(null)}
              className="text-[10px] font-black uppercase tracking-widest text-zinc-500 hover:text-white transition-colors cursor-pointer bg-white/5 hover:bg-white/10 px-4 py-2 rounded-xl border border-white/10 shadow-inner"
            >
              Đóng chi tiết
            </button>
          </div>
          <div className="overflow-x-auto p-4">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-zinc-800/50 text-[10px] font-black tracking-widest text-zinc-500 uppercase">
                  <th className="py-4 px-4">ID</th>
                  <th className="py-4 px-4">User ID</th>
                  <th className="py-4 px-4 text-right">Số dư Ledger</th>
                  <th className="py-4 px-4 text-right">Số dư Cache</th>
                  <th className="py-4 px-4 text-right">Lệch Balance</th>
                  <th className="py-4 px-4 text-right">Lệch Accumulated</th>
                  <th className="py-4 px-4">Trạng thái</th>
                  <th className="py-4 px-4">Ghi chú</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/30 text-xs text-zinc-300">
                {reconciliationDetails && reconciliationDetails.content?.length > 0 ? (
                  reconciliationDetails.content.map((det) => (
                    <tr key={det.id} className="hover:bg-white/5 transition-colors group">
                      <td className="py-4 px-4 font-mono text-zinc-500 tracking-wide">#{det.id}</td>
                      <td className="py-4 px-4 font-mono font-black text-white tracking-wide">#{det.userId}</td>
                      <td className="py-4 px-4 text-right font-mono text-emerald-400 tracking-wide">{det.ledgerBalance}</td>
                      <td className="py-4 px-4 text-right font-mono text-brand-orange tracking-wide">{det.currentBalance}</td>
                      <td className={`py-4 px-4 text-right font-mono font-black tracking-wide ${det.balanceDifference !== 0 ? 'text-red-500' : 'text-zinc-500'}`}>
                        {det.balanceDifference}
                      </td>
                      <td className={`py-4 px-4 text-right font-mono font-black tracking-wide ${det.accumulatedDifference !== 0 ? 'text-red-500' : 'text-zinc-500'}`}>
                        {det.accumulatedDifference}
                      </td>
                      <td className="py-4 px-4 font-black">
                        <span className={`inline-flex items-center px-3 py-1 rounded-xl text-[9px] uppercase font-mono tracking-widest shadow-inner ${
                          det.status === 'RESOLVED' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-red-500/10 text-red-400 border border-red-500/20'
                        }`}>
                          {det.status}
                        </span>
                      </td>
                      <td className="py-4 px-4 text-[11px] font-medium text-zinc-400 tracking-wide">{det.remark}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="8" className="py-16 text-center text-zinc-500 font-black tracking-wide">
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
