import React from 'react';
import useCustomerScore from '@/features/score/customer/hooks/useCustomerScore';
import MembershipCard from '@/features/score/customer/components/MembershipCard';
import TierProgressBar from '@/features/score/customer/components/TierProgressBar';
import ScoreHistoryTable from '@/features/score/customer/components/ScoreHistoryTable';
import ComingSoonLayout from '@/features/score/customer/components/ComingSoonLayout';
import { Award, RefreshCw, AlertTriangle } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';

export default function LoyaltyCenterPage() {
  const { user } = useAuth();
  const {
    scoreData,
    history,
    isLoading,
    isHistoryLoading,
    error,
    refreshScore,
    fetchHistory
  } = useCustomerScore();

  const handlePageChange = (newPage) => {
    fetchHistory({ page: newPage, size: 10 });
  };

  const handleFilterChange = (filters) => {
    fetchHistory({ page: 0, size: 10, ...filters });
  };

  return (
    <div className="min-h-screen bg-zinc-950 text-white py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto space-y-10">
        {/* Header */}
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 border-b border-zinc-800 pb-6">
          <div>
            <div className="flex items-center gap-2 text-brand-orange mb-1">
              <Award className="h-6 w-6 animate-pulse" />
              <span className="text-xs font-black uppercase tracking-widest">Vietnam Tours Loyalty</span>
            </div>
            <h1 className="text-3xl md:text-4xl font-black tracking-tight text-white">
              Trung tâm Khách hàng Thành viên
            </h1>
            <p className="text-sm text-zinc-400 mt-1">
              Quản lý hạng thẻ, điểm thưởng tích lũy và tra cứu chi tiết lịch sử giao dịch.
            </p>
          </div>
          <button
            onClick={() => refreshScore()}
            disabled={isLoading}
            className="flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-zinc-900 hover:bg-zinc-800 border border-zinc-800 text-xs font-bold text-zinc-300 transition-all shadow-md disabled:opacity-50 w-fit"
          >
            <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
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

        {/* Transaction History Table */}
        <div>
          <ScoreHistoryTable
            history={history}
            isLoading={isHistoryLoading}
            onPageChange={handlePageChange}
            onFilterChange={handleFilterChange}
          />
        </div>

        {/* Coming Soon Features */}
        <div className="pt-4">
          <ComingSoonLayout />
        </div>
      </div>
    </div>
  );
}
