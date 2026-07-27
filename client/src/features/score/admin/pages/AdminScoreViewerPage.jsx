import { useState } from 'react';
import useAdminScore from '../hooks/useAdminScore';
import { Search, Award, User, TrendingUp, AlertCircle, FileText, Calendar, ArrowUpRight, ArrowDownLeft, RefreshCcw, ChevronLeft, ChevronRight, Info } from 'lucide-react';

export default function AdminScoreViewerPage() {
  const {
    userScore,
    userHistory,
    isLoadingUserScore,
    errorUserScore,
    fetchUserScore,
    fetchUserHistory
  } = useAdminScore();

  const [searchId, setSearchId] = useState('');
  const [activeTabFilter, setActiveTabFilter] = useState('ALL');

  const handleSearch = async (e) => {
    e?.preventDefault();
    if (!searchId.trim()) return;
    try {
      await fetchUserScore(searchId.trim());
      await fetchUserHistory(searchId.trim(), { page: 0, size: 10 });
    } catch {
      // error handled in hook
    }
  };

  const handlePageChange = (newPage) => {
    if (!searchId.trim()) return;
    const filters = activeTabFilter === 'ALL' ? {} : { transactionType: activeTabFilter };
    fetchUserHistory(searchId.trim(), { page: newPage, size: 10, ...filters });
  };

  const handleFilterClick = (type) => {
    setActiveTabFilter(type);
    if (!searchId.trim()) return;
    const filters = type === 'ALL' ? {} : { transactionType: type };
    fetchUserHistory(searchId.trim(), { page: 0, size: 10, ...filters });
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '—';
    try {
      return new Intl.DateTimeFormat('vi-VN', {
        day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
      }).format(new Date(dateStr));
    } catch {
      return dateStr;
    }
  };

  const getTransactionBadge = (type) => {
    switch (type) {
      case 'EARN':
        return <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2.5 py-1 text-xs font-bold text-emerald-400 border border-emerald-500/20"><ArrowUpRight className="h-3.5 w-3.5" />Tích điểm</span>;
      case 'REDEEM':
        return <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/10 px-2.5 py-1 text-xs font-bold text-amber-400 border border-amber-500/20"><ArrowDownLeft className="h-3.5 w-3.5" />Dùng điểm</span>;
      default:
        return <span className="inline-flex items-center gap-1 rounded-full bg-blue-500/10 px-2.5 py-1 text-xs font-bold text-blue-400 border border-blue-500/20"><RefreshCcw className="h-3.5 w-3.5" />{type}</span>;
    }
  };

  const historyItems = userHistory?.content || [];
  const currentPage = userHistory?.number || 0;
  const totalPages = userHistory?.totalPages || 0;

  return (
    <div className="space-y-8 text-white">
      {/* Header */}
      <div className="bg-zinc-900 border border-zinc-800 p-6 rounded-3xl shadow-xl space-y-4">
        <div>
          <div className="flex items-center gap-2 text-brand-orange mb-1">
            <User className="h-5 w-5" />
            <span className="text-xs font-black uppercase tracking-widest">Tra cứu điểm thành viên</span>
          </div>
          <h1 className="text-2xl font-black text-white">Tra cứu & Lịch sử Điểm thưởng Khách hàng</h1>
          <p className="text-xs text-zinc-400 mt-1">
            Tìm kiếm thông tin điểm khả dụng, hạng thành viên và kiểm tra chi tiết các giao dịch của khách hàng
          </p>
        </div>

        {/* Search form */}
        <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-3 pt-2">
          <div className="relative flex-grow">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-500" />
            <input
              type="text"
              value={searchId}
              onChange={(e) => setSearchId(e.target.value)}
              placeholder="Nhập Account ID (hoặc UUID tài khoản khách hàng)..."
              className="w-full bg-zinc-950 border border-zinc-800 focus:border-brand-orange rounded-2xl py-3.5 pl-11 pr-4 text-xs font-bold text-white focus:outline-none transition-colors"
            />
          </div>
          <button
            type="submit"
            disabled={isLoadingUserScore || !searchId.trim()}
            className="px-6 py-3.5 rounded-2xl bg-brand-orange hover:bg-opacity-95 text-zinc-950 font-black text-xs uppercase tracking-wider transition-all shadow-lg disabled:opacity-50 shrink-0 flex items-center justify-center gap-2 cursor-pointer"
          >
            <Search className="h-4 w-4" />
            <span>Tra cứu ngay</span>
          </button>
        </form>
      </div>

      {/* Error state */}
      {errorUserScore && (
        <div className="rounded-3xl bg-red-500/10 border border-red-500/20 p-6 flex items-center gap-4 text-red-400">
          <AlertCircle className="h-6 w-6 shrink-0" />
          <div>
            <h4 className="text-sm font-bold">Không tìm thấy thông tin</h4>
            <p className="text-xs mt-0.5 opacity-90">{errorUserScore}</p>
          </div>
        </div>
      )}

      {/* Loading */}
      {isLoadingUserScore && (
        <div className="flex flex-col items-center justify-center py-20 text-zinc-500 gap-3 bg-zinc-900/40 rounded-3xl border border-zinc-800">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-brand-orange border-t-transparent" />
          <span className="text-xs font-medium">Đang tra cứu hồ sơ điểm...</span>
        </div>
      )}

      {/* Score Summary Display */}
      {userScore && !isLoadingUserScore && (
        <div className="space-y-6 animate-in fade-in duration-300">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="rounded-3xl bg-gradient-to-br from-zinc-900 via-zinc-900 to-zinc-950 border border-zinc-800 p-6 shadow-xl flex flex-col justify-between">
              <div className="flex items-center justify-between">
                <span className="text-xs font-black uppercase tracking-wider text-zinc-400">Điểm khả dụng hiện tại</span>
                <Award className="h-5 w-5 text-brand-orange" />
              </div>
              <div className="mt-6 flex items-baseline gap-2">
                <span className="text-4xl font-black text-white">
                  {(userScore.currentPoints ?? 0).toLocaleString('vi-VN')}
                </span>
                <span className="text-xs font-bold text-zinc-500 uppercase">Điểm</span>
              </div>
              <div className="mt-4 pt-4 border-t border-zinc-800 text-[11px] text-zinc-400 flex items-center justify-between">
                <span>Account ID:</span>
                <span className="font-mono font-bold text-white">{userScore.accountId}</span>
              </div>
            </div>

            <div className="rounded-3xl bg-gradient-to-br from-zinc-900 via-zinc-900 to-zinc-950 border border-zinc-800 p-6 shadow-xl flex flex-col justify-between">
              <div className="flex items-center justify-between">
                <span className="text-xs font-black uppercase tracking-wider text-zinc-400">Điểm tích lũy trọn đời</span>
                <TrendingUp className="h-5 w-5 text-emerald-400" />
              </div>
              <div className="mt-6 flex items-baseline gap-2">
                <span className="text-4xl font-black text-white">
                  {(userScore.accumulatedPoints ?? 0).toLocaleString('vi-VN')}
                </span>
                <span className="text-xs font-bold text-zinc-500 uppercase">Điểm</span>
              </div>
              <div className="mt-4 pt-4 border-t border-zinc-800 text-[11px] text-zinc-400 flex items-center justify-between">
                <span>Điểm tạm giữ (Held):</span>
                <span className="font-bold text-amber-400">{(userScore.heldPoints ?? 0).toLocaleString('vi-VN')} điểm</span>
              </div>
            </div>

            <div className="rounded-3xl bg-gradient-to-br from-amber-600/20 via-zinc-900 to-zinc-950 border border-amber-500/30 p-6 shadow-xl flex flex-col justify-between relative overflow-hidden">
              <div className="absolute top-0 right-0 w-32 h-32 bg-amber-500/10 rounded-full filter blur-2xl pointer-events-none" />
              <div className="flex items-center justify-between relative z-10">
                <span className="text-xs font-black uppercase tracking-wider text-amber-400">Hạng thẻ hiện tại</span>
                <span className="px-2.5 py-1 rounded-full bg-amber-500/20 border border-amber-400/30 text-[10px] font-black uppercase text-amber-300">
                  {userScore.currentTier?.tierCode || 'SILVER'}
                </span>
              </div>
              <div className="mt-6 relative z-10">
                <span className="text-2xl font-black text-white block">
                  {userScore.currentTier?.tierName || 'Silver Member'}
                </span>
                <span className="text-xs text-zinc-400 font-medium mt-1 block">
                  Hoàn điểm {Math.round((userScore.currentTier?.earningRate || 0.05) * 100)}% giá trị giao dịch
                </span>
              </div>
              <div className="mt-4 pt-4 border-t border-zinc-800/80 text-[11px] text-zinc-400 flex items-center justify-between relative z-10">
                <span>Quyền hạn:</span>
                <span className="font-bold text-emerald-400">Thành viên chính thức</span>
              </div>
            </div>
          </div>

          {/* Phase 2 Note */}
          <div className="rounded-2xl bg-blue-500/10 border border-blue-500/20 p-4 flex items-start gap-3 text-blue-300 text-xs leading-relaxed">
            <Info className="h-5 w-5 shrink-0 text-blue-400 mt-0.5" />
            <div>
              <span className="font-bold block mb-0.5">Quyền hạn quản lý điểm Phase 1</span>
              Ở Giai đoạn 1 (Foundation & Loyalty Center), Admin được cấp quyền tra cứu số dư và lịch sử giao dịch điểm. Các tính năng Điều chỉnh số dư thủ công (Manual Adjustments) và Phục hồi điểm (Revoke/Refund) sẽ được tích hợp trong Giai đoạn 2.
            </div>
          </div>

          {/* History Table */}
          <div className="rounded-3xl bg-zinc-900/80 border border-zinc-800 p-6 shadow-xl space-y-6 backdrop-blur-md">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-zinc-800 pb-5">
              <div>
                <h3 className="text-base font-black text-white flex items-center gap-2">
                  <FileText className="h-5 w-5 text-brand-orange" />
                  Lịch sử giao dịch điểm của tài khoản
                </h3>
              </div>

              <div className="flex flex-wrap gap-1.5 bg-zinc-950 p-1 rounded-xl border border-zinc-800/80">
                {[
                  { id: 'ALL', label: 'Tất cả' },
                  { id: 'EARN', label: 'Tích điểm' },
                  { id: 'REDEEM', label: 'Dùng điểm' },
                  { id: 'REFUND_REDEEM', label: 'Hoàn/Thu hồi' }
                ].map((tab) => (
                  <button
                    key={tab.id}
                    onClick={() => handleFilterClick(tab.id)}
                    className={`px-3 py-1.5 text-xs font-bold rounded-lg transition-all ${
                      activeTabFilter === tab.id ? 'bg-zinc-800 text-white shadow-sm border border-zinc-700/60' : 'text-zinc-400 hover:text-zinc-200'
                    }`}
                  >
                    {tab.label}
                  </button>
                ))}
              </div>
            </div>

            {historyItems.length === 0 ? (
              <div className="text-center py-12 text-zinc-500">
                <p className="text-sm font-bold text-zinc-400">Khách hàng chưa có giao dịch điểm nào</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-zinc-800 text-[11px] font-black uppercase tracking-wider text-zinc-500">
                      <th className="py-3 px-4">Thời gian</th>
                      <th className="py-3 px-4">Loại giao dịch</th>
                      <th className="py-3 px-4">Mã đơn / Sự kiện</th>
                      <th className="py-3 px-4 text-right">Thay đổi</th>
                      <th className="py-3 px-4 text-right">Số dư sau</th>
                      <th className="py-3 px-4">Mô tả</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-800/60 text-xs text-zinc-300">
                    {historyItems.map((item) => {
                      const changeVal = item.pointChange ?? item.actualPointChange ?? 0;
                      const isPositive = changeVal >= 0;
                      return (
                        <tr key={item.historyId || item.id} className="hover:bg-zinc-800/40 transition-colors group">
                          <td className="py-4 px-4 whitespace-nowrap text-zinc-400 font-medium flex items-center gap-2">
                            <Calendar className="h-3.5 w-3.5 text-zinc-600" />
                            {formatDate(item.occurredAt || item.createdAt)}
                          </td>
                          <td className="py-4 px-4 whitespace-nowrap">
                            {getTransactionBadge(item.transactionType)}
                          </td>
                          <td className="py-4 px-4 whitespace-nowrap font-mono text-zinc-400 font-bold">
                            {item.bookingId ? `#BK${item.bookingId}` : (item.eventId || '—')}
                          </td>
                          <td className={`py-4 px-4 whitespace-nowrap text-right font-black text-sm ${isPositive ? 'text-emerald-400' : 'text-amber-400'}`}>
                            {isPositive ? `+${changeVal.toLocaleString('vi-VN')}` : changeVal.toLocaleString('vi-VN')}
                          </td>
                          <td className="py-4 px-4 whitespace-nowrap text-right font-bold text-white">
                            {(item.balanceAfter ?? 0).toLocaleString('vi-VN')}
                          </td>
                          <td className="py-4 px-4 text-zinc-400 max-w-xs truncate" title={item.description}>
                            {item.description || '—'}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}

            {totalPages > 1 && (
              <div className="flex items-center justify-between border-t border-zinc-800 pt-4 text-xs">
                <span className="text-zinc-500 font-medium">
                  Trang <span className="text-white font-bold">{currentPage + 1}</span> / <span className="text-white font-bold">{totalPages}</span>
                </span>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={currentPage === 0}
                    className="p-2 rounded-xl bg-zinc-800 border border-zinc-700 text-zinc-300 disabled:opacity-40"
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </button>
                  <button
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage >= totalPages - 1}
                    className="p-2 rounded-xl bg-zinc-800 border border-zinc-700 text-zinc-300 disabled:opacity-40"
                  >
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
