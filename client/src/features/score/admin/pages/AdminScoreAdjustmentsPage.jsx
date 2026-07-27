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
      <div className="bg-zinc-900/60 p-6 rounded-2xl border border-zinc-800/80 backdrop-blur-md">
        <h1 className="text-2xl font-black tracking-tight text-white flex items-center gap-2.5">
          <PlusCircle className="w-7 h-7 text-amber-500 shrink-0" />
          <span>Điều Chỉnh & Đảo Giao Dịch Điểm Thưởng</span>
        </h1>
        <p className="text-sm text-zinc-400 mt-1">
          Cộng/trừ điểm thủ công cho khách hàng hoặc đảo các giao dịch lỗi với tính năng chống lặp (Idempotency).
        </p>
      </div>

      {/* Search Account Bar */}
      <div className="bg-zinc-900/80 p-6 rounded-2xl border border-zinc-800">
        <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-4 items-end">
          <div className="flex-1 w-full">
            <label className="block text-xs font-bold uppercase tracking-wider text-zinc-400 mb-2">
              ID Khách Hàng (User ID)
            </label>
            <div className="relative">
              <Search className="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-500" />
              <input
                type="number"
                value={searchUserId}
                onChange={(e) => setSearchUserId(e.target.value)}
                placeholder="Nhập ID khách hàng..."
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl pl-11 pr-4 py-3 text-sm text-white placeholder-zinc-600 focus:outline-none focus:border-amber-500 transition-colors font-mono"
              />
            </div>
          </div>
          <button
            type="submit"
            disabled={isLoadingUserScore || !searchUserId.trim()}
            className="w-full sm:w-auto px-6 py-3 bg-amber-500 hover:bg-amber-600 text-zinc-950 font-black rounded-xl text-sm transition-all disabled:opacity-50 cursor-pointer shrink-0"
          >
            {isLoadingUserScore ? 'Đang tra cứu...' : 'Tra cứu Tài Khoản'}
          </button>
        </form>

        {errorUserScore && (
          <div className="mt-4 p-4 bg-rose-500/10 border border-rose-500/20 rounded-xl text-xs text-rose-400 font-bold flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{errorUserScore}</span>
          </div>
        )}

        {userScore && (
          <div className="mt-6 pt-6 border-t border-zinc-800 grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="bg-zinc-950/60 p-4 rounded-xl border border-zinc-800/80">
              <div className="text-[11px] font-bold text-zinc-500 uppercase">Điểm khả dụng (Current)</div>
              <div className="text-xl font-black text-amber-400 font-mono mt-1">{userScore.currentPoints?.toLocaleString('vi-VN')}</div>
            </div>
            <div className="bg-zinc-950/60 p-4 rounded-xl border border-zinc-800/80">
              <div className="text-[11px] font-bold text-zinc-500 uppercase">Điểm tích lũy (Accumulated)</div>
              <div className="text-xl font-black text-white font-mono mt-1">{userScore.accumulatedPoints?.toLocaleString('vi-VN')}</div>
            </div>
            <div className="bg-zinc-950/60 p-4 rounded-xl border border-zinc-800/80">
              <div className="text-[11px] font-bold text-zinc-500 uppercase">Hạng hiện tại</div>
              <div className="text-xl font-black text-emerald-400 font-mono uppercase mt-1">{userScore.currentTier?.tierName || 'SILVER'}</div>
            </div>
            <div className="bg-zinc-950/60 p-4 rounded-xl border border-zinc-800/80">
              <div className="text-[11px] font-bold text-zinc-500 uppercase">Trạng thái</div>
              <div className="text-xl font-black text-blue-400 font-mono uppercase mt-1">{userScore.status || 'ACTIVE'}</div>
            </div>
          </div>
        )}
      </div>

      {/* Tabs */}
      <div className="flex border-b border-zinc-800 gap-6">
        <button
          onClick={() => setActiveTab('adjust')}
          className={`pb-4 px-2 font-black text-sm transition-all border-b-2 flex items-center gap-2 ${
            activeTab === 'adjust' ? 'border-amber-500 text-amber-400' : 'border-transparent text-zinc-400 hover:text-white'
          }`}
        >
          <PlusCircle className="w-4 h-4" />
          <span>Điều Chỉnh Điểm (Adjust)</span>
        </button>
        <button
          onClick={() => setActiveTab('reverse')}
          className={`pb-4 px-2 font-black text-sm transition-all border-b-2 flex items-center gap-2 ${
            activeTab === 'reverse' ? 'border-rose-500 text-rose-400' : 'border-transparent text-zinc-400 hover:text-white'
          }`}
        >
          <RotateCcw className="w-4 h-4" />
          <span>Đảo Giao Dịch (Reverse)</span>
        </button>
      </div>

      {/* Tab Content */}
      {activeTab === 'adjust' ? (
        <form onSubmit={handleAdjustSubmit} className="bg-zinc-900/80 p-6 rounded-2xl border border-zinc-800 space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-zinc-400 mb-2">
                Loại điều chỉnh
              </label>
              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => setAdjType('ADD')}
                  className={`py-3 px-4 rounded-xl font-bold text-xs flex items-center justify-center gap-2 border transition-all ${
                    adjType === 'ADD' ? 'bg-emerald-500/20 border-emerald-500 text-emerald-400' : 'bg-zinc-950 border-zinc-800 text-zinc-400'
                  }`}
                >
                  <PlusCircle className="w-4 h-4" />
                  <span>Cộng Điểm (ADD)</span>
                </button>
                <button
                  type="button"
                  onClick={() => setAdjType('DEDUCT')}
                  className={`py-3 px-4 rounded-xl font-bold text-xs flex items-center justify-center gap-2 border transition-all ${
                    adjType === 'DEDUCT' ? 'bg-rose-500/20 border-rose-500 text-rose-400' : 'bg-zinc-950 border-zinc-800 text-zinc-400'
                  }`}
                >
                  <MinusCircle className="w-4 h-4" />
                  <span>Trừ Điểm (DEDUCT)</span>
                </button>
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-zinc-400 mb-2">
                Số điểm điều chỉnh <span className="text-rose-400">*</span>
              </label>
              <input
                type="number"
                min="1"
                required
                value={adjPoints}
                onChange={(e) => setAdjPoints(e.target.value)}
                placeholder="Ví dụ: 100"
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-sm text-white placeholder-zinc-600 focus:outline-none focus:border-amber-500 transition-colors font-mono font-bold"
              />
            </div>

            <div className="md:col-span-2">
              <label className="block text-xs font-bold uppercase tracking-wider text-zinc-400 mb-2">
                Lý do điều chỉnh (Nghiệp vụ / Khiếu nại) <span className="text-rose-400">*</span>
              </label>
              <input
                type="text"
                required
                value={adjReason}
                onChange={(e) => setAdjReason(e.target.value)}
                placeholder="Ví dụ: Đền bù điểm cho đơn hàng lỗi #ORD-1029"
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-sm text-white placeholder-zinc-600 focus:outline-none focus:border-amber-500 transition-colors"
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-zinc-400 mb-2">
                Mã yêu cầu (Request ID - Chống lặp)
              </label>
              <input
                type="text"
                value={adjRequestId}
                onChange={(e) => setAdjRequestId(e.target.value)}
                placeholder="Để trống sẽ tự tạo tự động"
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-sm text-white placeholder-zinc-600 focus:outline-none focus:border-amber-500 transition-colors font-mono"
              />
            </div>

            <div className="flex items-center pt-6">
              <label className="flex items-center gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  checked={adjAffectAccumulated}
                  onChange={(e) => setAdjAffectAccumulated(e.target.checked)}
                  className="w-5 h-5 rounded border-zinc-800 bg-zinc-950 text-amber-500 focus:ring-amber-500"
                />
                <span className="text-xs font-bold text-zinc-300">
                  Đồng thời điều chỉnh Điểm tích lũy (Ảnh hưởng thăng/giáng hạng)
                </span>
              </label>
            </div>
          </div>

          <div className="pt-4 flex justify-end">
            <button
              type="submit"
              disabled={isLoadingOperations || !searchUserId}
              className="px-8 py-3.5 bg-emerald-500 hover:bg-emerald-600 text-zinc-950 font-black rounded-xl text-sm transition-all shadow-lg shadow-emerald-500/10 disabled:opacity-50 cursor-pointer"
            >
              {isLoadingOperations ? 'Đang xử lý...' : 'Xác Nhận Điều Chỉnh Điểm'}
            </button>
          </div>
        </form>
      ) : (
        <form onSubmit={handleReverseSubmit} className="bg-zinc-900/80 p-6 rounded-2xl border border-zinc-800 space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-zinc-400 mb-2">
                ID Giao dịch cần đảo (History ID) <span className="text-rose-400">*</span>
              </label>
              <input
                type="number"
                min="1"
                required
                value={revHistoryId}
                onChange={(e) => setRevHistoryId(e.target.value)}
                placeholder="Ví dụ: 5042"
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-sm text-white placeholder-zinc-600 focus:outline-none focus:border-rose-500 transition-colors font-mono font-bold"
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-zinc-400 mb-2">
                Mã yêu cầu đảo (Request ID - Chống lặp)
              </label>
              <input
                type="text"
                value={revRequestId}
                onChange={(e) => setRevRequestId(e.target.value)}
                placeholder="Để trống sẽ tự tạo tự động"
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-sm text-white placeholder-zinc-600 focus:outline-none focus:border-rose-500 transition-colors font-mono"
              />
            </div>

            <div className="md:col-span-2">
              <label className="block text-xs font-bold uppercase tracking-wider text-zinc-400 mb-2">
                Lý do đảo giao dịch <span className="text-rose-400">*</span>
              </label>
              <input
                type="text"
                required
                value={revReason}
                onChange={(e) => setRevReason(e.target.value)}
                placeholder="Ví dụ: Nhập sai số điểm thưởng cho khách"
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-sm text-white placeholder-zinc-600 focus:outline-none focus:border-rose-500 transition-colors"
              />
            </div>
          </div>

          <div className="pt-4 flex justify-end">
            <button
              type="submit"
              disabled={isLoadingOperations || !searchUserId}
              className="px-8 py-3.5 bg-rose-500 hover:bg-rose-600 text-zinc-950 font-black rounded-xl text-sm transition-all shadow-lg shadow-rose-500/10 disabled:opacity-50 cursor-pointer"
            >
              {isLoadingOperations ? 'Đang xử lý...' : 'Xác Nhận Đảo Giao Dịch'}
            </button>
          </div>
        </form>
      )}

      {/* User History Table Preview */}
      {userHistory && userHistory.content && (
        <div className="bg-zinc-900/80 rounded-2xl border border-zinc-800 overflow-hidden">
          <div className="p-5 border-b border-zinc-800 font-bold text-sm text-white flex justify-between items-center">
            <span>Lịch sử giao dịch điểm của khách hàng #{searchUserId}</span>
            <span className="text-xs font-mono text-zinc-400">Tổng: {userHistory.totalElements || 0} giao dịch</span>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-zinc-800/80 text-[11px] font-bold text-zinc-400 uppercase bg-zinc-950/40">
                  <th className="py-3 px-4">ID</th>
                  <th className="py-3 px-4">Thời gian</th>
                  <th className="py-3 px-4">Loại GD</th>
                  <th className="py-3 px-4 text-right">Biến động</th>
                  <th className="py-3 px-4 text-right">Số dư sau</th>
                  <th className="py-3 px-4">Lý do</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/60 text-xs text-zinc-300">
                {userHistory.content.map((h) => (
                  <tr key={h.id} className="hover:bg-zinc-800/40 transition-colors">
                    <td className="py-3 px-4 font-mono text-amber-400 font-bold">#{h.id}</td>
                    <td className="py-3 px-4 font-mono text-zinc-400">{h.occurredAt?.replace('T', ' ').substring(0, 19)}</td>
                    <td className="py-3 px-4 font-bold uppercase text-white">{h.transactionType}</td>
                    <td className={`py-3 px-4 text-right font-mono font-bold ${h.actualPointChange >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
                      {h.actualPointChange >= 0 ? `+${h.actualPointChange}` : h.actualPointChange}
                    </td>
                    <td className="py-3 px-4 text-right font-mono text-white">{h.balanceAfter}</td>
                    <td className="py-3 px-4 text-zinc-400 max-w-xs truncate">{h.reason || h.description}</td>
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
