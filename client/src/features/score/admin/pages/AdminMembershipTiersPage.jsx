import React, { useEffect, useState } from 'react';
import useAdminScore from '../hooks/useAdminScore';
import TierModal from '../components/TierModal';
import { Award, Plus, Edit2, CheckCircle, XCircle, AlertCircle, RefreshCw } from 'lucide-react';

export default function AdminMembershipTiersPage() {
  const {
    tiers,
    isLoadingTiers,
    errorTiers,
    fetchTiers,
    createTier,
    updateTier
  } = useAdminScore();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedTier, setSelectedTier] = useState(null);
  const [notification, setNotification] = useState(null);

  useEffect(() => {
    fetchTiers();
  }, [fetchTiers]);

  const showNotify = (msg, type = 'success') => {
    setNotification({ msg, type });
    setTimeout(() => setNotification(null), 3500);
  };

  const handleOpenCreate = () => {
    setSelectedTier(null);
    setIsModalOpen(true);
  };

  const handleOpenEdit = (tier) => {
    setSelectedTier(tier);
    setIsModalOpen(true);
  };

  const handleSaveTier = async (formData) => {
    try {
      if (selectedTier) {
        await updateTier(selectedTier.tierCode, formData);
        showNotify(`Đã cập nhật hạng thẻ ${formData.tierCode} thành công!`);
      } else {
        await createTier(formData);
        showNotify(`Đã tạo hạng thẻ mới ${formData.tierCode} thành công!`);
      }
    } catch (err) {
      throw err;
    }
  };

  return (
    <div className="space-y-6 text-white">
      {/* Toast */}
      {notification && (
        <div className={`fixed top-24 right-8 z-50 py-3.5 px-6 rounded-2xl shadow-2xl border flex items-center gap-3 transition-all duration-300 ${
          notification.type === 'success' ? 'bg-emerald-950 border-emerald-500/30 text-emerald-400' : 'bg-red-950 border-red-500/30 text-red-400'
        }`}>
          {notification.type === 'success' ? <CheckCircle className="w-5 h-5" /> : <AlertCircle className="w-5 h-5" />}
          <span className="text-xs md:text-sm font-bold">{notification.msg}</span>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-zinc-900 border border-zinc-800 p-6 rounded-3xl shadow-xl">
        <div>
          <div className="flex items-center gap-2 text-brand-orange mb-1">
            <Award className="h-5 w-5" />
            <span className="text-xs font-black uppercase tracking-widest">Hệ thống Loyalty</span>
          </div>
          <h1 className="text-2xl font-black text-white">Quản lý Hạng thẻ Thành viên</h1>
          <p className="text-xs text-zinc-400 mt-1">Thiết lập các mốc thăng hạng và tỷ lệ tích điểm thưởng cho khách hàng</p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={fetchTiers}
            disabled={isLoadingTiers}
            className="p-3 rounded-2xl bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 text-zinc-300 transition-colors disabled:opacity-50"
            title="Làm mới"
          >
            <RefreshCw className={`h-4 w-4 ${isLoadingTiers ? 'animate-spin' : ''}`} />
          </button>
          <button
            onClick={handleOpenCreate}
            className="flex items-center gap-2 px-5 py-3 rounded-2xl bg-brand-orange hover:bg-opacity-95 text-zinc-950 font-black text-xs uppercase tracking-wider transition-all shadow-lg cursor-pointer"
          >
            <Plus className="h-4 w-4" />
            <span>Thêm hạng thẻ mới</span>
          </button>
        </div>
      </div>

      {/* Error */}
      {errorTiers && (
        <div className="rounded-3xl bg-red-500/10 border border-red-500/20 p-6 flex items-center gap-4 text-red-400">
          <AlertCircle className="h-6 w-6 shrink-0" />
          <div>
            <h4 className="text-sm font-bold">Không thể tải dữ liệu hạng thẻ</h4>
            <p className="text-xs mt-0.5 opacity-90">{errorTiers}</p>
          </div>
        </div>
      )}

      {/* Tiers List */}
      <div className="bg-zinc-900/80 border border-zinc-800 rounded-3xl p-6 shadow-xl backdrop-blur-md overflow-hidden">
        {isLoadingTiers && tiers.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-zinc-500 gap-3">
            <div className="h-8 w-8 animate-spin rounded-full border-2 border-brand-orange border-t-transparent" />
            <span className="text-xs font-medium">Đang tải danh sách hạng thành viên...</span>
          </div>
        ) : tiers.length === 0 ? (
          <div className="text-center py-16 text-zinc-500">
            <Award className="h-10 w-10 mx-auto text-zinc-700 mb-2" />
            <p className="text-sm font-bold text-zinc-400">Chưa cấu hình hạng thành viên nào</p>
            <p className="text-xs text-zinc-600 mt-1">Nhấn nút "Thêm hạng thẻ mới" để khởi tạo các hạng Bạc, Vàng, Kim Cương.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-zinc-800 text-[11px] font-black uppercase tracking-wider text-zinc-500">
                  <th className="py-4 px-4">Mã hạng (Code)</th>
                  <th className="py-4 px-4">Tên hạng hiển thị</th>
                  <th className="py-4 px-4 text-right">Điểm tích lũy tối thiểu</th>
                  <th className="py-4 px-4 text-right">Tỷ lệ hoàn điểm</th>
                  <th className="py-4 px-4">Trạng thái</th>
                  <th className="py-4 px-4">Mô tả</th>
                  <th className="py-4 px-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/60 text-xs text-zinc-300">
                {tiers.map((tier) => (
                  <tr key={tier.tierCode} className="hover:bg-zinc-800/40 transition-colors group">
                    <td className="py-4 px-4 font-mono font-black text-amber-400 uppercase whitespace-nowrap">
                      {tier.tierCode}
                    </td>
                    <td className="py-4 px-4 font-bold text-white whitespace-nowrap">
                      {tier.tierName}
                    </td>
                    <td className="py-4 px-4 text-right font-black text-white whitespace-nowrap">
                      {(tier.minAccumulatedPoints || 0).toLocaleString('vi-VN')} điểm
                    </td>
                    <td className="py-4 px-4 text-right font-bold text-emerald-400 whitespace-nowrap">
                      {Math.round((tier.earningRate || 0.05) * 100)}%
                    </td>
                    <td className="py-4 px-4 whitespace-nowrap">
                      {tier.active !== false ? (
                        <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2.5 py-1 text-[11px] font-bold text-emerald-400 border border-emerald-500/20">
                          <CheckCircle className="h-3 w-3" />
                          Đang hoạt động
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 rounded-full bg-zinc-800 px-2.5 py-1 text-[11px] font-bold text-zinc-400 border border-zinc-700">
                          <XCircle className="h-3 w-3" />
                          Tạm khóa
                        </span>
                      )}
                    </td>
                    <td className="py-4 px-4 text-zinc-400 max-w-xs truncate" title={tier.description}>
                      {tier.description || '—'}
                    </td>
                    <td className="py-4 px-4 text-right whitespace-nowrap">
                      <button
                        onClick={() => handleOpenEdit(tier)}
                        className="p-2 rounded-xl bg-zinc-800 hover:bg-zinc-700 text-zinc-300 hover:text-white transition-colors border border-zinc-700"
                        title="Chỉnh sửa hạng thẻ"
                      >
                        <Edit2 className="h-4 w-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal */}
      <TierModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSave={handleSaveTier}
        initialData={selectedTier}
      />
    </div>
  );
}
