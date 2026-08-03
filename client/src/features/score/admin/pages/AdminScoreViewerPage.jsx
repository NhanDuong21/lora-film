import { useState } from 'react';
import useAdminScore from '../hooks/useAdminScore';
import ExpiringPointsSection from '@/features/score/customer/components/ExpiringPointsSection';
import TierHistoryTimeline from '@/features/score/customer/components/TierHistoryTimeline';
import { Search, Award, User, TrendingUp, AlertCircle, FileText, Calendar, ArrowUpRight, ArrowDownLeft, RefreshCcw, ChevronLeft, ChevronRight, Info } from 'lucide-react';

export default function AdminScoreViewerPage() {
  const {
    userScore,
    userHistory,
    expiringPoints,
    tierHistory,
    isLoadingUserScore,
    errorUserScore,
    fetchUserScore,
    fetchUserHistory,
    fetchUserExpiringPoints,
    fetchUserTierHistory
  } = useAdminScore();


  const [searchId, setSearchId] = useState('');
  const [activeTabFilter, setActiveTabFilter] = useState('ALL');

  const handleSearch = async (e) => {
    e?.preventDefault();
    if (!searchId.trim()) return;
    try {
      await fetchUserScore(searchId.trim());
      await fetchUserHistory(searchId.trim(), { page: 0, size: 10 });
      await fetchUserExpiringPoints(searchId.trim());
      await fetchUserTierHistory(searchId.trim());
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
      case 'EARN_BY_BOOKING':
        return <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2.5 py-1 text-xs font-bold text-emerald-400 border border-emerald-500/20"><ArrowUpRight className="h-3.5 w-3.5" />Tích điểm</span>;
      case 'REDEEM':
      case 'REDEEM_FOR_BOOKING':
        return <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/10 px-2.5 py-1 text-xs font-bold text-amber-400 border border-amber-500/20"><ArrowDownLeft className="h-3.5 w-3.5" />Dùng điểm</span>;
      case 'REFUND_REDEEM':
        return <span className="inline-flex items-center gap-1 rounded-full bg-cyan-500/10 px-2.5 py-1 text-xs font-bold text-cyan-400 border border-cyan-500/20"><RefreshCcw className="h-3.5 w-3.5" />Hoàn điểm</span>;
      case 'REVOKE_EARN':
      case 'REVOKE_EARN_BY_REFUND':
        return <span className="inline-flex items-center gap-1 rounded-full bg-red-500/10 px-2.5 py-1 text-xs font-bold text-red-400 border border-red-500/20"><AlertCircle className="h-3.5 w-3.5" />Thu hồi</span>;
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
      <div className="bg-zinc-900/40 backdrop-blur-md border border-zinc-800/50 p-8 rounded-[2rem] shadow-2xl shadow-black/20 space-y-5">
        <div>
          <div className="flex items-center gap-2 text-brand-orange mb-1.5">
            <User className="h-5 w-5" />
            <span className="text-[10px] font-black uppercase tracking-widest">Tra cứu điểm thành viên</span>
          </div>
          <h1 className="text-2xl font-black text-white tracking-tight">Tra cứu & Lịch sử Điểm thưởng Khách hàng</h1>
          <p className="text-[11px] text-zinc-400 font-medium tracking-wide mt-1">
            Tìm kiếm thông tin điểm khả dụng, hạng thành viên và kiểm tra chi tiết các giao dịch của khách hàng
          </p>
        </div>

        {/* Search form */}
        <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-3 pt-2">
          <div className="relative flex-grow">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-zinc-500" />
            <input
              type="text"
              value={searchId}
              onChange={(e) => setSearchId(e.target.value)}
              placeholder="Nhập Account ID (hoặc UUID tài khoản khách hàng)..."
              className="w-full bg-black/20 border border-zinc-800/80 focus:border-brand-orange/50 rounded-2xl py-3.5 pl-12 pr-4 text-xs font-bold text-white focus:outline-none transition-colors shadow-inner"
            />
          </div>
          <button
            type="submit"
            disabled={isLoadingUserScore || !searchId.trim()}
            className="px-8 py-3.5 rounded-2xl bg-brand-orange hover:bg-opacity-90 text-zinc-950 font-black text-[11px] uppercase tracking-widest transition-all shadow-xl shadow-brand-orange/20 disabled:opacity-50 shrink-0 flex items-center justify-center gap-2 cursor-pointer"
          >
            <Search className="h-4 w-4" />
            <span>Tra cứu ngay</span>
          </button>
        </form>
      </div>

      {/* Error state */}
      {errorUserScore && (
        <div className="rounded-3xl bg-red-950/40 border border-red-500/30 p-6 flex items-center gap-4 text-red-400 backdrop-blur-md shadow-xl">
          <AlertCircle className="h-6 w-6 shrink-0" />
          <div>
            <h4 className="text-sm font-black tracking-wide">Không tìm thấy thông tin</h4>
            <p className="text-[11px] mt-1 text-red-400/80 font-medium">{errorUserScore}</p>
          </div>
        </div>
      )}

      {/* Loading */}
      {isLoadingUserScore && (
        <div className="flex flex-col items-center justify-center py-20 text-zinc-500 gap-4 bg-zinc-900/40 backdrop-blur-md rounded-[2rem] border border-zinc-800/50 shadow-inner">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-brand-orange border-t-transparent" />
          <span className="text-xs font-medium tracking-wide">Đang tra cứu hồ sơ điểm...</span>
        </div>
      )}

      {/* Score Summary Display */}
      {userScore && !isLoadingUserScore && (
        <div className="space-y-6 animate-in fade-in duration-300">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="rounded-[2rem] bg-zinc-900/40 backdrop-blur-md border border-zinc-800/50 p-8 shadow-xl shadow-black/10 flex flex-col justify-between">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Điểm khả dụng</span>
                <div className="w-10 h-10 rounded-2xl bg-white/5 flex items-center justify-center shadow-inner border border-white/10">
                  <Award className="h-5 w-5 text-brand-orange" />
                </div>
              </div>
              <div className="mt-6 flex items-baseline gap-2">
                <span className="text-4xl md:text-5xl font-black text-white tracking-tighter">
                  {(userScore.currentPoints ?? 0).toLocaleString('vi-VN')}
                </span>
                <span className="text-[10px] font-bold text-zinc-500 uppercase tracking-widest pb-1.5">PTS</span>
              </div>
              <div className="mt-5 pt-5 border-t border-zinc-800/50 text-[11px] text-zinc-500 flex items-center justify-between font-medium tracking-wide">
                <span>Account ID</span>
                <span className="font-mono font-bold text-zinc-300">{userScore.accountId}</span>
              </div>
            </div>

            <div className="rounded-[2rem] bg-zinc-900/40 backdrop-blur-md border border-zinc-800/50 p-8 shadow-xl shadow-black/10 flex flex-col justify-between">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Tích lũy trọn đời</span>
                <div className="w-10 h-10 rounded-2xl bg-white/5 flex items-center justify-center shadow-inner border border-white/10">
                  <TrendingUp className="h-5 w-5 text-emerald-400" />
                </div>
              </div>
              <div className="mt-6 flex items-baseline gap-2">
                <span className="text-4xl md:text-5xl font-black text-white tracking-tighter">
                  {(userScore.accumulatedPoints ?? 0).toLocaleString('vi-VN')}
                </span>
                <span className="text-[10px] font-bold text-zinc-500 uppercase tracking-widest pb-1.5">PTS</span>
              </div>
              <div className="mt-5 pt-5 border-t border-zinc-800/50 text-[11px] text-zinc-500 flex flex-wrap items-center justify-between gap-2 font-medium tracking-wide">
                <div className="flex items-center gap-1.5">
                  <span>Tạm giữ</span>
                  <span className="font-black text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded-md">{(userScore.heldPoints ?? 0).toLocaleString('vi-VN')}</span>
                </div>
                {(userScore.outstandingPoints > 0) && (
                  <div className="flex items-center gap-1.5">
                    <span>Nợ</span>
                    <span className="font-black text-red-400 bg-red-500/10 px-2 py-0.5 rounded-md">{(userScore.outstandingPoints ?? 0).toLocaleString('vi-VN')}</span>
                  </div>
                )}
              </div>
            </div>

            <div className="rounded-[2rem] bg-gradient-to-br from-amber-500/10 via-zinc-900/40 to-zinc-950/40 backdrop-blur-md border border-amber-500/20 p-8 shadow-xl shadow-amber-900/10 flex flex-col justify-between relative overflow-hidden">
              <div className="absolute -top-10 -right-10 w-40 h-40 bg-amber-500/10 rounded-full filter blur-3xl pointer-events-none" />
              <div className="flex items-center justify-between relative z-10">
                <span className="text-[10px] font-black uppercase tracking-widest text-amber-500/80">Hạng thẻ</span>
                <span className="px-3 py-1.5 rounded-xl bg-amber-500/10 border border-amber-500/20 text-[9px] font-black uppercase text-amber-400 shadow-inner">
                  {userScore.currentTier?.tierCode || 'SILVER'}
                </span>
              </div>
              <div className="mt-6 relative z-10">
                <span className="text-3xl font-black text-amber-400 tracking-tighter block mb-1">
                  {userScore.currentTier?.tierName || 'Silver Member'}
                </span>
                <span className="text-[11px] text-zinc-400 font-medium tracking-wide block">
                  Hoàn điểm <strong className="text-amber-300">{Math.round((userScore.currentTier?.earningRate || 0.05) * 100)}%</strong> giá trị giao dịch
                </span>
              </div>
              <div className="mt-5 pt-5 border-t border-zinc-800/50 text-[11px] text-zinc-500 flex items-center justify-between font-medium tracking-wide relative z-10">
                <span>Quyền hạn</span>
                <span className="font-black text-emerald-400">Thành viên chính thức</span>
              </div>
            </div>
          </div>

          {/* Phase 3 Note & Sections */}
          <div className="rounded-2xl bg-blue-500/10 border border-blue-500/20 p-4 flex items-start gap-3 text-blue-300 text-xs leading-relaxed">
            <Info className="h-5 w-5 shrink-0 text-blue-400 mt-0.5" />
            <div>
              <span className="font-bold block mb-0.5">Quyền hạn quản lý điểm Phase 3 (Refund, Expiration & Tier)</span>
              Ở Giai đoạn 3, hệ thống đã hoàn thiện toàn bộ vòng đời điểm thưởng: tích điểm, dùng điểm, hoàn điểm (Refund), thu hồi điểm (Revoke), theo dõi hạn sử dụng theo nguyên tắc FIFO (Expiration) và lịch sử thăng giáng hạng (Tier History - Append Only).
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
            <div className="lg:col-span-6 flex flex-col">
              <ExpiringPointsSection expiringPoints={expiringPoints} isLoading={isLoadingUserScore} />
            </div>
            <div className="lg:col-span-6 flex flex-col">
              <TierHistoryTimeline tierHistory={tierHistory} isLoading={isLoadingUserScore} />
            </div>
          </div>


          {/* History Table */}
          <div className="rounded-[2rem] bg-zinc-900/40 backdrop-blur-md border border-zinc-800/50 p-8 shadow-xl shadow-black/10 space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-zinc-800/50 pb-5">
              <div>
                <h3 className="text-base font-black text-white flex items-center gap-2">
                  <FileText className="h-5 w-5 text-brand-orange" />
                  Lịch sử giao dịch điểm của tài khoản
                </h3>
              </div>

              <div className="flex flex-wrap gap-1 bg-zinc-950/50 p-1.5 rounded-2xl border border-zinc-800/50 shadow-inner">
                {[
                  { id: 'ALL', label: 'Tất cả' },
                  { id: 'EARN', label: 'Tích điểm' },
                  { id: 'REDEEM', label: 'Dùng điểm' },
                  { id: 'REFUND_REDEEM', label: 'Hoàn điểm' },
                  { id: 'REVOKE_EARN', label: 'Thu hồi' }
                ].map((tab) => (

                  <button
                    key={tab.id}
                    onClick={() => handleFilterClick(tab.id)}
                    className={`px-4 py-1.5 text-[11px] font-bold rounded-xl transition-all ${activeTabFilter === tab.id ? 'bg-zinc-800 text-white shadow-md border border-zinc-700/50' : 'text-zinc-500 hover:text-zinc-300 hover:bg-white/5'
                      }`}
                  >
                    {tab.label}
                  </button>
                ))}
              </div>
            </div>

            {historyItems.length === 0 ? (
              <div className="text-center py-16 text-zinc-500">
                <p className="text-sm font-black tracking-wide">Khách hàng chưa có giao dịch điểm nào</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-zinc-800/50 text-[10px] font-black uppercase tracking-widest text-zinc-500">
                      <th className="py-4 px-4">Thời gian</th>
                      <th className="py-4 px-4">Loại giao dịch</th>
                      <th className="py-4 px-4">Mã đơn / Sự kiện</th>
                      <th className="py-4 px-4 text-right">Thay đổi</th>
                      <th className="py-4 px-4 text-right">Số dư sau</th>
                      <th className="py-4 px-4">Mô tả</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-800/30 text-xs text-zinc-300">
                    {historyItems.map((item) => {
                      const changeVal = item.pointChange ?? item.actualPointChange ?? 0;
                      const isPositive = changeVal >= 0;
                      return (
                        <tr key={item.historyId || item.id} className="hover:bg-white/5 transition-colors group">
                          <td className="py-4 px-4 whitespace-nowrap text-zinc-400 font-medium flex items-center gap-2">
                            <Calendar className="h-3.5 w-3.5 text-zinc-600 group-hover:text-brand-orange transition-colors" />
                            {formatDate(item.occurredAt || item.createdAt)}
                          </td>
                          <td className="py-4 px-4 whitespace-nowrap">
                            {getTransactionBadge(item.transactionType)}
                          </td>
                          <td className="py-4 px-4 whitespace-nowrap font-mono text-zinc-500 font-bold tracking-wide">
                            {item.bookingId ? `#BK${item.bookingId}` : (item.eventId || '—')}
                          </td>
                          <td className={`py-4 px-4 whitespace-nowrap text-right font-black text-sm tracking-wide ${isPositive ? 'text-emerald-400' : 'text-amber-400'}`}>
                            {isPositive ? `+${changeVal.toLocaleString('vi-VN')}` : changeVal.toLocaleString('vi-VN')}
                          </td>
                          <td className="py-4 px-4 whitespace-nowrap text-right font-bold text-white tracking-wide">
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
              <div className="flex items-center justify-between border-t border-zinc-800/50 pt-5 text-xs">
                <span className="text-zinc-500 font-medium">
                  Trang <span className="text-white font-bold">{currentPage + 1}</span> / <span className="text-white font-bold">{totalPages}</span>
                </span>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={currentPage === 0}
                    className="p-2 rounded-xl bg-zinc-800/50 border border-zinc-700/50 text-zinc-300 disabled:opacity-40 hover:bg-zinc-700 transition-colors shadow-sm"
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </button>
                  <button
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage >= totalPages - 1}
                    className="p-2 rounded-xl bg-zinc-800/50 border border-zinc-700/50 text-zinc-300 disabled:opacity-40 hover:bg-zinc-700 transition-colors shadow-sm"
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
