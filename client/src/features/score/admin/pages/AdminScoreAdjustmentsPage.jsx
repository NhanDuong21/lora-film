import { useState } from 'react';
import useAdminScore from '../hooks/useAdminScore';
import { PlusCircle, MinusCircle, RotateCcw, Search, CheckCircle, AlertCircle, RefreshCw } from 'lucide-react';

export default function AdminScoreAdjustmentsPage() {
  const {
    userScore,
    userHistory,
    isLoadingUserScore,
    errorUserScore,
    fetchUserScore,
    fetchUserHistory,
    adjustScore,
    reverseAdjustment,
    isLoadingOperations
  } = useAdminScore();

  const [searchUserId, setSearchUserId] = useState('');
  const [activeTab, setActiveTab] = useState('adjust'); // 'adjust' | 'reverse'
  const [notification, setNotification] = useState(null);

  // Form State for Adjust
  const [adjType, setAdjType] = useState('ADD');
  const [adjPoints, setAdjPoints] = useState('');
  const [adjReason, setAdjReason] = useState('');
  const [adjRequestId, setAdjRequestId] = useState('');
  const [adjAffectAccumulated, setAdjAffectAccumulated] = useState(false);

  // Form State for Reverse
  const [revHistoryId, setRevHistoryId] = useState('');
  const [revReason, setRevReason] = useState('');
  const [revRequestId, setRevRequestId] = useState('');

  const showNotify = (msg, type = 'success') => {
    setNotification({ msg, type });
    setTimeout(() => setNotification(null), 4000);
  };

  const handleSearch = async (e) => {
    e?.preventDefault();
    if (!searchUserId.trim()) return;
    await fetchUserScore(searchUserId.trim());
    await fetchUserHistory(searchUserId.trim());
  };

  const handleAdjustSubmit = async (e) => {
    e.preventDefault();
    if (!searchUserId || !adjPoints || !adjReason) {
      showNotify('Vui lòng điền đầy đủ User ID, số điểm và lý do', 'error');
      return;
    }
    try {
      const payload = {
        type: adjType,
        points: parseInt(adjPoints, 10),
        reason: adjReason,
        requestId: adjRequestId || `REQ-ADJ-${Date.now()}`,
        affectAccumulatedPoints: adjAffectAccumulated,
        allowNegative: adjAffectAccumulated
      };
      const res = await adjustScore(searchUserId, payload);
      showNotify(`Điều chỉnh thành công! Số dư mới: ${res.currentPoints || res?.data?.currentPoints} điểm.`);
      setAdjPoints('');
      setAdjReason('');
      setAdjRequestId('');
      await fetchUserHistory(searchUserId);
    } catch (err) {
      showNotify(err.response?.data?.message || 'Lỗi khi điều chỉnh điểm', 'error');
    }
  };

  const handleReverseSubmit = async (e) => {
    e.preventDefault();
    if (!searchUserId || !revHistoryId || !revReason) {
      showNotify('Vui lòng điền đầy đủ User ID, History ID và lý do đảo giao dịch', 'error');
      return;
    }
    try {
      const payload = {
        historyId: parseInt(revHistoryId, 10),
        reason: revReason,
        requestId: revRequestId || `REQ-REV-${Date.now()}`
      };
      const res = await reverseAdjustment(searchUserId, payload);
      showNotify(`Đảo giao dịch thành công! Số dư mới: ${res.currentPoints || res?.data?.currentPoints} điểm.`);
      setRevHistoryId('');
      setRevReason('');
      setRevRequestId('');
      await fetchUserHistory(searchUserId);
    } catch (err) {
      showNotify(err.response?.data?.message || 'Lỗi khi đảo giao dịch', 'error');
    }
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
      <div className="bg-zinc-900/40 p-6 rounded-[2rem] border border-zinc-800/50 backdrop-blur-md shadow-xl shadow-black/10">
        <h1 className="text-2xl font-black tracking-tight text-white flex items-center gap-2.5">
          <PlusCircle className="w-7 h-7 text-brand-orange shrink-0" />
          <span>Điều Chỉnh & Đảo Giao Dịch Điểm Thưởng</span>
        </h1>
        <p className="text-[11px] font-medium tracking-wide text-zinc-400 mt-1">
          Cộng/trừ điểm thủ công cho khách hàng hoặc đảo các giao dịch lỗi với tính năng chống lặp (Idempotency).
        </p>
      </div>

      {/* Search Account Bar */}
      <div className="bg-zinc-900/40 backdrop-blur-md p-8 rounded-[2rem] border border-zinc-800/50 shadow-xl shadow-black/10">
        <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-4 items-end">
          <div className="flex-1 w-full">
            <label className="block text-[10px] font-black uppercase tracking-widest text-zinc-500 mb-2">
              ID Khách Hàng (User ID)
            </label>
            <div className="relative">
              <Search className="w-5 h-5 absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" />
              <input
                type="number"
                value={searchUserId}
                onChange={(e) => setSearchUserId(e.target.value)}
                placeholder="Nhập ID khách hàng..."
                className="w-full bg-black/20 border border-zinc-800/80 rounded-2xl pl-12 pr-4 py-3.5 text-xs text-white placeholder-zinc-600 focus:outline-none focus:border-brand-orange/50 transition-colors font-mono font-bold shadow-inner"
              />
            </div>
          </div>
          <button
            type="submit"
            disabled={isLoadingUserScore || !searchUserId.trim()}
            className="w-full sm:w-auto px-8 py-3.5 bg-brand-orange hover:bg-opacity-90 text-zinc-950 font-black rounded-2xl text-[11px] uppercase tracking-widest transition-all disabled:opacity-50 cursor-pointer shrink-0 shadow-xl shadow-brand-orange/20"
          >
            {isLoadingUserScore ? 'Đang tra cứu...' : 'Tra cứu Tài Khoản'}
          </button>
        </form>

        {errorUserScore && (
          <div className="mt-5 p-4 bg-red-950/40 border border-red-500/30 rounded-2xl text-[11px] text-red-400 font-medium tracking-wide flex items-center gap-2 shadow-inner">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{errorUserScore}</span>
          </div>
        )}

        {userScore && (
          <div className="mt-8 pt-8 border-t border-zinc-800/50 grid grid-cols-2 md:grid-cols-4 gap-5">
            <div className="bg-white/5 p-5 rounded-2xl border border-white/10 shadow-inner">
              <div className="text-[9px] font-black tracking-widest text-zinc-500 uppercase">Điểm khả dụng</div>
              <div className="text-2xl font-black text-brand-orange font-mono mt-1 tracking-tighter">{userScore.currentPoints?.toLocaleString('vi-VN')}</div>
            </div>
            <div className="bg-white/5 p-5 rounded-2xl border border-white/10 shadow-inner">
              <div className="text-[9px] font-black tracking-widest text-zinc-500 uppercase">Điểm tích lũy</div>
              <div className="text-2xl font-black text-white font-mono mt-1 tracking-tighter">{userScore.accumulatedPoints?.toLocaleString('vi-VN')}</div>
            </div>
            <div className="bg-white/5 p-5 rounded-2xl border border-white/10 shadow-inner">
              <div className="text-[9px] font-black tracking-widest text-zinc-500 uppercase">Hạng hiện tại</div>
              <div className="text-xl font-black text-emerald-400 mt-1 uppercase tracking-wide truncate">{userScore.currentTier?.tierName || 'SILVER'}</div>
            </div>
            <div className="bg-white/5 p-5 rounded-2xl border border-white/10 shadow-inner">
              <div className="text-[9px] font-black tracking-widest text-zinc-500 uppercase">Trạng thái</div>
              <div className="text-xl font-black text-blue-400 mt-1 uppercase tracking-wide truncate">{userScore.status || 'ACTIVE'}</div>
            </div>
          </div>
        )}
      </div>

      {/* Tabs */}
      <div className="flex border-b border-zinc-800/50 gap-8">
        <button
          onClick={() => setActiveTab('adjust')}
          className={`pb-4 px-2 font-black text-xs uppercase tracking-widest transition-all border-b-2 flex items-center gap-2 ${
            activeTab === 'adjust' ? 'border-brand-orange text-brand-orange' : 'border-transparent text-zinc-500 hover:text-zinc-300'
          }`}
        >
          <PlusCircle className="w-4 h-4" />
          <span>Điều Chỉnh Điểm</span>
        </button>
        <button
          onClick={() => setActiveTab('reverse')}
          className={`pb-4 px-2 font-black text-xs uppercase tracking-widest transition-all border-b-2 flex items-center gap-2 ${
            activeTab === 'reverse' ? 'border-red-500 text-red-400' : 'border-transparent text-zinc-500 hover:text-zinc-300'
          }`}
        >
          <RotateCcw className="w-4 h-4" />
          <span>Đảo Giao Dịch</span>
        </button>
      </div>

      {/* Tab Content */}
      {activeTab === 'adjust' ? (
        <form onSubmit={handleAdjustSubmit} className="bg-zinc-900/40 backdrop-blur-md p-8 rounded-[2rem] border border-zinc-800/50 shadow-xl shadow-black/10 space-y-8">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div>
              <label className="block text-[10px] font-black uppercase tracking-widest text-zinc-500 mb-3">
                Loại điều chỉnh
              </label>
              <div className="grid grid-cols-2 gap-4">
                <button
                  type="button"
                  onClick={() => setAdjType('ADD')}
                  className={`py-4 px-4 rounded-2xl font-black text-[11px] uppercase tracking-widest flex items-center justify-center gap-2 border transition-all shadow-inner ${
                    adjType === 'ADD' ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400 shadow-emerald-500/5' : 'bg-black/20 border-zinc-800/80 text-zinc-500 hover:bg-white/5 hover:text-zinc-300'
                  }`}
                >
                  <PlusCircle className="w-4 h-4" />
                  <span>Cộng Điểm (ADD)</span>
                </button>
                <button
                  type="button"
                  onClick={() => setAdjType('DEDUCT')}
                  className={`py-4 px-4 rounded-2xl font-black text-[11px] uppercase tracking-widest flex items-center justify-center gap-2 border transition-all shadow-inner ${
                    adjType === 'DEDUCT' ? 'bg-red-500/10 border-red-500/30 text-red-400 shadow-red-500/5' : 'bg-black/20 border-zinc-800/80 text-zinc-500 hover:bg-white/5 hover:text-zinc-300'
                  }`}
                >
                  <MinusCircle className="w-4 h-4" />
                  <span>Trừ Điểm (DEDUCT)</span>
                </button>
              </div>
            </div>

            <div>
              <label className="block text-[10px] font-black uppercase tracking-widest text-zinc-500 mb-3">
                Số điểm điều chỉnh <span className="text-red-400">*</span>
              </label>
              <input
                type="number"
                min="1"
                required
                value={adjPoints}
                onChange={(e) => setAdjPoints(e.target.value)}
                placeholder="Ví dụ: 100"
                className="w-full bg-black/20 border border-zinc-800/80 rounded-2xl px-5 py-4 text-sm text-white placeholder-zinc-600 focus:outline-none focus:border-brand-orange/50 transition-colors font-mono font-bold shadow-inner"
              />
            </div>

            <div className="md:col-span-2">
              <label className="block text-[10px] font-black uppercase tracking-widest text-zinc-500 mb-3">
                Lý do điều chỉnh (Nghiệp vụ / Khiếu nại) <span className="text-red-400">*</span>
              </label>
              <input
                type="text"
                required
                value={adjReason}
                onChange={(e) => setAdjReason(e.target.value)}
                placeholder="Ví dụ: Đền bù điểm cho đơn hàng lỗi #ORD-1029"
                className="w-full bg-black/20 border border-zinc-800/80 rounded-2xl px-5 py-4 text-xs text-white placeholder-zinc-600 focus:outline-none focus:border-brand-orange/50 transition-colors shadow-inner tracking-wide"
              />
            </div>

            <div>
              <label className="block text-[10px] font-black uppercase tracking-widest text-zinc-500 mb-3">
                Mã yêu cầu (Request ID - Chống lặp)
              </label>
              <input
                type="text"
                value={adjRequestId}
                onChange={(e) => setAdjRequestId(e.target.value)}
                placeholder="Để trống sẽ tự tạo tự động"
                className="w-full bg-black/20 border border-zinc-800/80 rounded-2xl px-5 py-4 text-xs text-white placeholder-zinc-600 focus:outline-none focus:border-brand-orange/50 transition-colors font-mono shadow-inner"
              />
            </div>

            <div className="flex items-center pt-6">
              <label className="flex items-center gap-3 cursor-pointer group">
                <div className={`w-5 h-5 rounded-md flex items-center justify-center transition-colors border ${
                  adjAffectAccumulated ? 'bg-brand-orange border-brand-orange' : 'bg-black/20 border-zinc-700 group-hover:border-zinc-500'
                }`}>
                  {adjAffectAccumulated && <CheckCircle className="w-3.5 h-3.5 text-zinc-950" />}
                </div>
                <input
                  type="checkbox"
                  checked={adjAffectAccumulated}
                  onChange={(e) => setAdjAffectAccumulated(e.target.checked)}
                  className="hidden"
                />
                <span className="text-[11px] font-bold text-zinc-400 group-hover:text-zinc-300 transition-colors tracking-wide">
                  Đồng thời điều chỉnh Điểm tích lũy <br/><span className="text-zinc-500 font-medium">(Ảnh hưởng thăng/giáng hạng)</span>
                </span>
              </label>
            </div>
          </div>

          <div className="pt-6 border-t border-zinc-800/50 flex justify-end">
            <button
              type="submit"
              disabled={isLoadingOperations || !searchUserId}
              className="px-8 py-3.5 bg-brand-orange hover:bg-opacity-90 text-zinc-950 font-black rounded-2xl text-[11px] uppercase tracking-widest transition-all shadow-xl shadow-brand-orange/20 disabled:opacity-50 cursor-pointer"
            >
              {isLoadingOperations ? 'Đang xử lý...' : 'Xác Nhận Điều Chỉnh Điểm'}
            </button>
          </div>
        </form>
      ) : (
        <form onSubmit={handleReverseSubmit} className="bg-zinc-900/40 backdrop-blur-md p-8 rounded-[2rem] border border-zinc-800/50 shadow-xl shadow-black/10 space-y-8">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div>
              <label className="block text-[10px] font-black uppercase tracking-widest text-zinc-500 mb-3">
                ID Giao dịch cần đảo (History ID) <span className="text-red-400">*</span>
              </label>
              <input
                type="number"
                min="1"
                required
                value={revHistoryId}
                onChange={(e) => setRevHistoryId(e.target.value)}
                placeholder="Ví dụ: 5042"
                className="w-full bg-black/20 border border-zinc-800/80 rounded-2xl px-5 py-4 text-sm text-white placeholder-zinc-600 focus:outline-none focus:border-red-500/50 transition-colors font-mono font-bold shadow-inner"
              />
            </div>

            <div>
              <label className="block text-[10px] font-black uppercase tracking-widest text-zinc-500 mb-3">
                Mã yêu cầu đảo (Request ID - Chống lặp)
              </label>
              <input
                type="text"
                value={revRequestId}
                onChange={(e) => setRevRequestId(e.target.value)}
                placeholder="Để trống sẽ tự tạo tự động"
                className="w-full bg-black/20 border border-zinc-800/80 rounded-2xl px-5 py-4 text-xs text-white placeholder-zinc-600 focus:outline-none focus:border-red-500/50 transition-colors font-mono shadow-inner"
              />
            </div>

            <div className="md:col-span-2">
              <label className="block text-[10px] font-black uppercase tracking-widest text-zinc-500 mb-3">
                Lý do đảo giao dịch <span className="text-red-400">*</span>
              </label>
              <input
                type="text"
                required
                value={revReason}
                onChange={(e) => setRevReason(e.target.value)}
                placeholder="Ví dụ: Nhập sai số điểm thưởng cho khách"
                className="w-full bg-black/20 border border-zinc-800/80 rounded-2xl px-5 py-4 text-xs text-white placeholder-zinc-600 focus:outline-none focus:border-red-500/50 transition-colors shadow-inner tracking-wide"
              />
            </div>
          </div>

          <div className="pt-6 border-t border-zinc-800/50 flex justify-end">
            <button
              type="submit"
              disabled={isLoadingOperations || !searchUserId}
              className="px-8 py-3.5 bg-red-500 hover:bg-red-600 text-white font-black rounded-2xl text-[11px] uppercase tracking-widest transition-all shadow-xl shadow-red-500/20 disabled:opacity-50 cursor-pointer"
            >
              {isLoadingOperations ? 'Đang xử lý...' : 'Xác Nhận Đảo Giao Dịch'}
            </button>
          </div>
        </form>
      )}

      {/* User History Table Preview */}
      {userHistory && userHistory.content && (
        <div className="bg-zinc-900/40 backdrop-blur-md rounded-[2rem] border border-zinc-800/50 shadow-xl shadow-black/10 overflow-hidden mt-8">
          <div className="p-6 border-b border-zinc-800/50 flex justify-between items-center bg-black/10">
            <span className="font-black text-sm text-white tracking-wide">Lịch sử giao dịch điểm của khách hàng <span className="text-brand-orange">#{searchUserId}</span></span>
            <span className="text-[10px] font-black uppercase tracking-widest text-zinc-500 bg-black/20 px-3 py-1.5 rounded-xl border border-zinc-800/80 shadow-inner">Tổng: {userHistory.totalElements || 0} giao dịch</span>
          </div>
          <div className="overflow-x-auto p-4">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-zinc-800/50 text-[10px] font-black tracking-widest text-zinc-500 uppercase">
                  <th className="py-4 px-4">ID</th>
                  <th className="py-4 px-4">Thời gian</th>
                  <th className="py-4 px-4">Loại GD</th>
                  <th className="py-4 px-4 text-right">Biến động</th>
                  <th className="py-4 px-4 text-right">Số dư sau</th>
                  <th className="py-4 px-4">Lý do</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/30 text-xs text-zinc-300">
                {userHistory.content.map((h) => (
                  <tr key={h.id} className="hover:bg-white/5 transition-colors group">
                    <td className="py-4 px-4 font-mono text-brand-orange font-bold tracking-wide">#{h.id}</td>
                    <td className="py-4 px-4 font-mono text-zinc-500 tracking-wide group-hover:text-zinc-400 transition-colors">{h.occurredAt?.replace('T', ' ').substring(0, 19)}</td>
                    <td className="py-4 px-4 font-black uppercase text-white tracking-wider text-[11px]">{h.transactionType}</td>
                    <td className={`py-4 px-4 text-right font-mono font-black tracking-wide ${h.actualPointChange >= 0 ? 'text-emerald-400' : 'text-amber-400'}`}>
                      {h.actualPointChange >= 0 ? `+${h.actualPointChange}` : h.actualPointChange}
                    </td>
                    <td className="py-4 px-4 text-right font-mono font-bold text-white tracking-wide">{h.balanceAfter}</td>
                    <td className="py-4 px-4 text-[11px] font-medium text-zinc-400 max-w-xs truncate tracking-wide" title={h.reason || h.description}>{h.reason || h.description}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
