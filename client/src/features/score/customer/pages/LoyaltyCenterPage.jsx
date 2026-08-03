import useCustomerScore from '@/features/score/customer/hooks/useCustomerScore';
import MembershipCard from '@/features/score/customer/components/MembershipCard';
import TierProgressBar from '@/features/score/customer/components/TierProgressBar';
import ScoreHistoryTable from '@/features/score/customer/components/ScoreHistoryTable';
import OutstandingBadge from '@/features/score/customer/components/OutstandingBadge';
import ExpiringPointsSection from '@/features/score/customer/components/ExpiringPointsSection';
import TierHistoryTimeline from '@/features/score/customer/components/TierHistoryTimeline';
import { Award, RefreshCw, AlertTriangle } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useState } from 'react';

export default function LoyaltyCenterPage() {
  const { user } = useAuth();
  const [historyFilters, setHistoryFilters] = useState({});
  const {
    scoreData,
    history,
    expiringPoints,
    tierHistory,
    isLoading,
    isHistoryLoading,
    isExpiringLoading,
    isTierHistoryLoading,
    error,
    refreshScore,
    fetchHistory
  } = useCustomerScore();

  const handlePageChange = (newPage) => {
    fetchHistory({ page: newPage, size: 10, ...historyFilters });
  };

  const handleFilterChange = (filters) => {
    setHistoryFilters(filters);
    fetchHistory({ page: 0, size: 10, ...filters });
  };

  return (
    <div className="min-h-screen bg-zinc-950 text-white py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto space-y-10">
        {/* Header */}
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 pb-8">
          <div className="space-y-3">
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-brand-orange/10 border border-brand-orange/20 text-brand-orange">
              <Award className="h-4 w-4" />
              <span className="text-[10px] font-black uppercase tracking-widest">Vietnam Tours Loyalty</span>
            </div>
            <h1 className="text-3xl md:text-4xl font-black tracking-tight text-white">
              Khách hàng Thành viên
            </h1>
            <p className="text-sm text-zinc-400 max-w-lg leading-relaxed">
              Quản lý hạng thẻ, điểm thưởng tích lũy và tra cứu chi tiết lịch sử giao dịch của bạn tại LoraFilm.
            </p>
          </div>
          <button
            onClick={() => refreshScore()}
            disabled={isLoading}
            className="group flex items-center gap-2 px-5 py-2.5 rounded-xl bg-zinc-900/50 hover:bg-zinc-800 backdrop-blur-sm border border-zinc-800/80 hover:border-zinc-700 text-xs font-bold text-zinc-300 transition-all disabled:opacity-50 w-fit"
          >
            <RefreshCw className={`h-4 w-4 transition-transform group-hover:rotate-180 ${isLoading ? 'animate-spin' : ''}`} />
            <span>Làm mới dữ liệu</span>
          </button>
        </div>

        {/* Error Alert */}
        {error && (
          <div className="rounded-2xl bg-red-500/10 border border-red-500/20 p-4 flex items-center gap-3 text-red-400 text-sm font-medium">
            <AlertTriangle className="h-5 w-5 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Outstanding Badge Banner (if in debt) */}
        {scoreData?.outstandingPoints > 0 && (
          <OutstandingBadge outstandingPoints={scoreData.outstandingPoints} variant="banner" />
        )}

        {/* Loading skeleton or Content */}
        {isLoading && !scoreData ? (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 animate-pulse">
            <div className="lg:col-span-7 h-72 rounded-3xl bg-zinc-900 border border-zinc-800" />
            <div className="lg:col-span-5 h-72 rounded-3xl bg-zinc-900 border border-zinc-800" />
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch">
            {/* Left: Membership Card */}
            <div className="lg:col-span-7 flex flex-col">
              <MembershipCard scoreData={scoreData} user={user} />
            </div>

            {/* Right: Tier Progress Bar */}
            <div className="lg:col-span-5 flex flex-col justify-center">
              <TierProgressBar scoreData={scoreData} />
            </div>
          </div>
        )}

        {/* Phase 3: Expiring Points & Tier History Section */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          <div className="lg:col-span-6 flex flex-col">
            <ExpiringPointsSection expiringPoints={expiringPoints} isLoading={isExpiringLoading} />
          </div>
          <div className="lg:col-span-6 flex flex-col">
            <TierHistoryTimeline tierHistory={tierHistory} isLoading={isTierHistoryLoading} />
          </div>
        </div>

        {/* Transaction History Table */}
        <div>
          <ScoreHistoryTable
            history={history}
            isLoading={isHistoryLoading}
            onPageChange={handlePageChange}
            onFilterChange={handleFilterChange}
          />
        </div>

      </div>
    </div>
  );
}

