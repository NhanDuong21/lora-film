import { useState } from 'react';
import { ArrowUpRight, ArrowDownLeft, RefreshCcw, AlertCircle, FileText, ChevronLeft, ChevronRight, Calendar } from 'lucide-react';

export default function ScoreHistoryTable({ history, isLoading, onPageChange, onFilterChange }) {
  const [activeFilter, setActiveFilter] = useState('ALL');

  const handleFilterClick = (type) => {
    setActiveFilter(type);
    onFilterChange && onFilterChange(type === 'ALL' ? {} : { transactionType: type });
  };

  const getTransactionBadge = (type) => {
    switch (type) {
      case 'EARN':
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2.5 py-1 text-xs font-bold text-emerald-400 border border-emerald-500/20">
            <ArrowUpRight className="h-3.5 w-3.5" />
            Tích điểm
          </span>
        );
      case 'REDEEM':
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/10 px-2.5 py-1 text-xs font-bold text-amber-400 border border-amber-500/20">
            <ArrowDownLeft className="h-3.5 w-3.5" />
            Dùng điểm
          </span>
        );
      case 'REFUND_REDEEM':
      case 'REVOKE_EARN':
      case 'MANUAL_ADJUSTMENT':
      default:
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-blue-500/10 px-2.5 py-1 text-xs font-bold text-blue-400 border border-blue-500/20">
            <RefreshCcw className="h-3.5 w-3.5" />
            {type}
          </span>
        );
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '—';
    try {
      const date = new Date(dateStr);
      return new Intl.DateTimeFormat('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      }).format(date);
    } catch {
      return dateStr;
    }
  };

  const items = history?.content || [];
  const currentPage = history?.number || 0;
  const totalPages = history?.totalPages || 0;

  return (
    <div className="rounded-[2rem] bg-zinc-900/40 backdrop-blur-md border border-zinc-800/50 p-6 shadow-2xl shadow-black/20 space-y-6">
      {/* Header & Filters */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-zinc-800/50 pb-5">
        <div>
          <h3 className="text-lg font-black text-white flex items-center gap-2">
            <FileText className="h-5 w-5 text-brand-orange" />
            Lịch sử giao dịch điểm
          </h3>
          <p className="text-xs text-zinc-400 mt-1">Theo dõi biến động điểm thưởng của bạn theo thời gian thực</p>
        </div>

        <div role="tablist" aria-label="Bộ lọc loại giao dịch" className="flex flex-wrap gap-1 bg-zinc-950/50 p-1.5 rounded-2xl border border-zinc-800/50 shadow-inner">
          {[
            { id: 'ALL', label: 'Tất cả' },
            { id: 'EARN', label: 'Tích điểm' },
            { id: 'REDEEM', label: 'Dùng điểm' },
            { id: 'REFUND_REDEEM', label: 'Hoàn/Thu hồi' }
          ].map((tab) => (
            <button
              key={tab.id}
              role="tab"
              aria-selected={activeFilter === tab.id}
              onClick={() => handleFilterClick(tab.id)}
              className={`px-4 py-1.5 text-[11px] font-bold rounded-xl transition-all ${
                activeFilter === tab.id
                  ? 'bg-zinc-800 text-white shadow-md border border-zinc-700/50'
                  : 'text-zinc-500 hover:text-zinc-300 hover:bg-white/5'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Table Content */}
      {isLoading ? (
        <div className="flex flex-col items-center justify-center py-16 text-zinc-500 gap-3">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-brand-orange border-t-transparent" />
          <span className="text-xs font-medium">Đang tải lịch sử giao dịch...</span>
        </div>
      ) : items.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-center text-zinc-500 space-y-2">
          <AlertCircle className="h-10 w-10 text-zinc-700 mb-2" />
          <p className="text-sm font-bold text-zinc-400">Chưa có giao dịch điểm thưởng nào</p>
          <p className="text-xs text-zinc-600 max-w-sm leading-relaxed">
            Các giao dịch tích điểm từ việc đặt tour hoặc sử dụng điểm thanh toán sẽ được tự động cập nhật tại đây.
          </p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-zinc-800/50 text-[10px] font-black uppercase tracking-widest text-zinc-500">
                <th className="py-4 px-4 font-black">Thời gian</th>
                <th className="py-4 px-4 font-black">Giao dịch</th>
                <th className="py-4 px-4 font-black">Mã đơn / Mã sự kiện</th>
                <th className="py-4 px-4 text-right font-black">Thay đổi</th>
                <th className="py-4 px-4 text-right font-black">Số dư sau</th>
                <th className="py-4 px-4 font-black">Mô tả</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800/30 text-xs text-zinc-300">
              {items.map((item) => {
                const isPositive = (item.pointChange || item.actualPointChange || 0) >= 0;
                const changeVal = item.pointChange ?? item.actualPointChange ?? 0;
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

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-zinc-800/50 pt-5 text-xs">
          <span className="text-zinc-500 font-medium">
            Trang <span className="text-white font-bold">{currentPage + 1}</span> / <span className="text-white font-bold">{totalPages}</span>
          </span>
          <div className="flex items-center gap-2" role="navigation" aria-label="Điều hướng trang">
            <button
              onClick={() => onPageChange && onPageChange(currentPage - 1)}
              disabled={currentPage === 0}
              aria-label="Trang trước"
              className="p-2 rounded-xl bg-zinc-800/50 border border-zinc-700/50 text-zinc-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-zinc-700 transition-colors shadow-sm"
            >
              <ChevronLeft className="h-4 w-4" aria-hidden="true" />
            </button>
            <button
              onClick={() => onPageChange && onPageChange(currentPage + 1)}
              disabled={currentPage >= totalPages - 1}
              aria-label="Trang sau"
              className="p-2 rounded-xl bg-zinc-800/50 border border-zinc-700/50 text-zinc-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-zinc-700 transition-colors shadow-sm"
            >
              <ChevronRight className="h-4 w-4" aria-hidden="true" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
