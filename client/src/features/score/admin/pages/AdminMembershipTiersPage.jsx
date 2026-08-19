import { useEffect, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import useAdminScore from '../hooks/useAdminScore';
import TierModal from '../components/TierModal';
import { Award, Plus, Edit2, CheckCircle, XCircle, AlertCircle, RefreshCw } from 'lucide-react';

export default function AdminMembershipTiersPage() {
  const outlet = useOutletContext();
  const confirm = outlet?.triggerConfirm || (async () => false);
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
    const thresholdConflict = tiers.some(tier => tier.tierCode !== selectedTier?.tierCode && Number(tier.minAccumulatedPoints) === Number(formData.minAccumulatedPoints));
    if (thresholdConflict) throw new Error('Mốc điểm hạng này đã được sử dụng. Mỗi hạng phải có một mốc riêng.');
    const changedThreshold = selectedTier && Number(selectedTier.minAccumulatedPoints) !== Number(formData.minAccumulatedPoints);
    const changedRate = selectedTier && Number(selectedTier.earningRate) !== Number(formData.earningRate);
    const accepted = await confirm({
      title: selectedTier ? 'Xác nhận thay đổi chính sách hạng' : 'Xác nhận tạo chính sách hạng',
      message: selectedTier
        ? `${selectedTier.userCount || 0} tài khoản đang ở hạng ${selectedTier.tierName}. ${changedThreshold ? 'Thay đổi mốc có thể yêu cầu tính lại hạng. ' : ''}${changedRate ? 'Tỷ lệ mới chỉ áp dụng cho lần tích điểm phát sinh sau khi lưu. ' : ''}Giao dịch cũ giữ snapshot cũ.`
        : `Tạo hạng ${formData.tierName} từ ${Number(formData.minAccumulatedPoints).toLocaleString('vi-VN')} điểm hạng với tỷ lệ ${Number(formData.earningRate * 100).toLocaleString('vi-VN')}%.`,
      confirmLabel: selectedTier ? 'Lưu chính sách' : 'Tạo hạng',
    });
    if (!accepted) throw Object.assign(new Error('CANCELLED'), { cancelled: true });
    if (selectedTier) {
      await updateTier(selectedTier.tierCode, formData);
      showNotify(`Đã cập nhật hạng thẻ ${formData.tierCode} thành công!`);
    } else {
      await createTier(formData);
      showNotify(`Đã tạo hạng thẻ mới ${formData.tierCode} thành công!`);
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
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-zinc-900/40 backdrop-blur-md border border-zinc-800/50 p-6 rounded-[2rem] shadow-2xl shadow-black/20">
        <div>
          <div className="flex items-center gap-2 text-brand-orange mb-1.5">
            <Award className="h-5 w-5" />
            <span className="text-[10px] font-black uppercase tracking-widest">Hệ thống Loyalty</span>
          </div>
          <h1 className="text-2xl font-black text-white tracking-tight">Chính sách hạng thành viên</h1>
          <p className="text-[11px] text-zinc-400 mt-1 font-medium tracking-wide">Mốc điểm hạng và tỷ lệ tích áp dụng cho giao dịch mới; lịch sử giữ snapshot tại thời điểm phát sinh.</p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={fetchTiers}
            disabled={isLoadingTiers}
            className="p-3.5 rounded-2xl bg-white/5 hover:bg-white/10 border border-white/10 text-zinc-300 transition-colors disabled:opacity-50 shadow-inner"
            title="Làm mới"
          >
            <RefreshCw className={`h-4 w-4 ${isLoadingTiers ? 'animate-spin' : ''}`} />
          </button>
          <button
            onClick={handleOpenCreate}
            className="flex items-center gap-2 px-6 py-3.5 rounded-2xl bg-brand-orange hover:bg-opacity-90 text-zinc-950 font-black text-[11px] uppercase tracking-widest transition-all shadow-xl shadow-brand-orange/20 cursor-pointer"
          >
            <Plus className="h-4 w-4" />
            <span>Thêm hạng thẻ mới</span>
          </button>
        </div>
      </div>

      {/* Error */}
      {errorTiers && (
        <div className="rounded-3xl bg-red-950/40 border border-red-500/30 p-6 flex items-center gap-4 text-red-400 backdrop-blur-md shadow-xl">
          <AlertCircle className="h-6 w-6 shrink-0" />
          <div>
            <h4 className="text-sm font-black tracking-wide">Không thể tải dữ liệu hạng thẻ</h4>
            <p className="text-[11px] mt-1 text-red-400/80 font-medium">{errorTiers}</p>
          </div>
        </div>
      )}

      {/* Tiers List */}
      <div className="bg-zinc-900/40 backdrop-blur-md border border-zinc-800/50 rounded-[2rem] p-8 shadow-xl shadow-black/10 overflow-hidden">
        {isLoadingTiers && tiers.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-zinc-500 gap-4">
            <div className="h-8 w-8 animate-spin rounded-full border-2 border-brand-orange border-t-transparent" />
            <span className="text-xs font-medium tracking-wide">Đang tải danh sách hạng thành viên...</span>
          </div>
        ) : tiers.length === 0 ? (
          <div className="text-center py-20 text-zinc-500">
            <Award className="h-12 w-12 mx-auto text-zinc-700 mb-3" />
            <p className="text-sm font-black tracking-wide">Chưa cấu hình hạng thành viên nào</p>
            <p className="text-[11px] font-medium text-zinc-500 mt-1.5 max-w-sm mx-auto">Nhấn nút "Thêm hạng thẻ mới" để khởi tạo các hạng Bạc, Vàng, Kim Cương.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-zinc-800/50 text-[10px] font-black uppercase tracking-widest text-zinc-500">
                  <th className="py-4 px-4 font-black">Mã hạng (Code)</th>
                  <th className="py-4 px-4 font-black">Tên hạng hiển thị</th>
                  <th className="py-4 px-4 text-right font-black">Điểm tối thiểu</th>
                  <th className="py-4 px-4 text-right font-black">Tỷ lệ tích</th>
                  <th className="py-4 px-4 font-black">Trạng thái</th>
                  <th className="py-4 px-4 font-black">Mô tả</th>
                  <th className="py-4 px-4 text-right font-black">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/30 text-xs text-zinc-300">
                {tiers.map((tier) => (
                  <tr key={tier.tierCode} className="hover:bg-white/5 transition-colors group">
                    <td className="py-4 px-4 font-mono font-black text-brand-orange uppercase whitespace-nowrap tracking-wide">
                      {tier.tierCode}
                    </td>
                    <td className="py-4 px-4 font-black text-white whitespace-nowrap tracking-wide">
                      {tier.tierName}
                    </td>
                    <td className="py-4 px-4 text-right font-black text-white whitespace-nowrap tracking-wider">
                      {(tier.minAccumulatedPoints || 0).toLocaleString('vi-VN')} <span className="text-[9px] text-zinc-500 uppercase tracking-widest ml-0.5">pts</span>
                    </td>
                    <td className="py-4 px-4 text-right font-black text-emerald-400 whitespace-nowrap tracking-wide">
                      {Math.round((tier.earningRate || 0.05) * 100)}%
                      <span className="mt-1 block text-[9px] font-medium text-zinc-600">÷ 1.000đ/điểm</span>
                    </td>
                    <td className="py-4 px-4 whitespace-nowrap">
                      {tier.active !== false ? (
                        <span className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-500/10 px-3 py-1 text-[9px] font-black uppercase tracking-widest text-emerald-400 border border-emerald-500/20 shadow-inner">
                          <CheckCircle className="h-3 w-3" />
                          Hoạt động
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1.5 rounded-xl bg-zinc-800/50 px-3 py-1 text-[9px] font-black uppercase tracking-widest text-zinc-400 border border-zinc-700/50 shadow-inner">
                          <XCircle className="h-3 w-3" />
                          Tạm khóa
                        </span>
                      )}
                    </td>
                    <td className="py-4 px-4 text-[11px] text-zinc-400 font-medium max-w-[12rem] truncate" title={tier.description}>
                      {tier.description || '—'}
                    </td>
                    <td className="py-4 px-4 text-right whitespace-nowrap">
                      <button
                        onClick={() => handleOpenEdit(tier)}
                        className="p-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-zinc-400 hover:text-white transition-colors border border-white/10 shadow-sm"
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
        existingTiers={tiers}
      />
    </div>
  );
}
