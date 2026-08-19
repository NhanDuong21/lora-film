import { useState, useEffect } from 'react';
import { X, Award, AlertCircle, CheckCircle2 } from 'lucide-react';

export default function TierModal({ isOpen, onClose, onSave, initialData, existingTiers = [] }) {
  const isEditing = !!initialData?.tierCode;

  const [formData, setFormData] = useState({
    tierCode: '',
    tierName: '',
    minAccumulatedPoints: 0,
    earningRate: 0.05,
    priority: 10,
    description: '',
    active: true
  });

  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (initialData && isOpen) {
      // The form is intentionally synchronized when the selected tier changes.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setFormData({
        tierCode: initialData.tierCode || '',
        tierName: initialData.tierName || '',
        minAccumulatedPoints: initialData.minAccumulatedPoints ?? 0,
        earningRate: initialData.earningRate ?? 0.05,
        priority: initialData.priority ?? 10,
        description: initialData.description || '',
        active: initialData.active ?? true
      });
    } else if (!isOpen) {
      // Reset transient form state only after the modal is closed.
      setFormData({
        tierCode: '',
        tierName: '',
        minAccumulatedPoints: 0,
        earningRate: 0.05,
        priority: 10,
        description: '',
        active: true
      });
      setError(null);
    }
  }, [initialData, isOpen]);

  if (!isOpen) return null;

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : (type === 'number' ? Number(value) : value)
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    if (!formData.tierCode.trim() || !formData.tierName.trim()) {
      setError('Mã hạng và tên hạng thẻ không được để trống.');
      return;
    }
    if (formData.minAccumulatedPoints < 0 || formData.earningRate < 0) {
      setError('Điểm tích lũy tối thiểu và tỷ lệ tích phải lớn hơn hoặc bằng 0.');
      return;
    }
    if (formData.earningRate <= 0 || formData.earningRate > 1) {
      setError('Tỷ lệ tích phải lớn hơn 0% và không vượt quá 100%.');
      return;
    }
    if (existingTiers.some(tier => tier.tierCode !== initialData?.tierCode && Number(tier.minAccumulatedPoints) === Number(formData.minAccumulatedPoints))) {
      setError('Mốc điểm hạng này đang thuộc một hạng khác.');
      return;
    }
    try {
      setIsSubmitting(true);
      await onSave(formData);
      onClose();
    } catch (err) {
      if (!err.cancelled) setError(err.response?.data?.message || err.message || 'Có lỗi xảy ra khi lưu hạng thẻ');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 sm:p-8 max-w-lg w-full shadow-2xl space-y-6 relative">
        <button
          onClick={onClose}
          className="absolute top-6 right-6 p-2 rounded-full text-zinc-400 hover:text-white hover:bg-zinc-800 transition-colors"
        >
          <X className="h-5 w-5" />
        </button>

        <div className="flex items-center gap-3 border-b border-zinc-800 pb-4">
          <div className="h-10 w-10 rounded-2xl bg-brand-orange/10 border border-brand-orange/20 flex items-center justify-center text-brand-orange">
            <Award className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-base font-black text-white uppercase tracking-wider">
              {isEditing ? 'Cập nhật hạng thành viên' : 'Thêm hạng thành viên mới'}
            </h3>
            <p className="text-xs text-zinc-400">
              {isEditing ? `Cấu hình thông số cho hạng thẻ ${formData.tierCode}` : 'Thiết lập quy tắc và mốc thăng hạng cho khách hàng'}
            </p>
          </div>
        </div>

        {error && (
          <div className="rounded-2xl bg-red-500/10 border border-red-500/20 p-3.5 flex items-center gap-2.5 text-red-400 text-xs font-bold">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-[11px] font-black uppercase tracking-wider text-zinc-400">Mã hạng (Code)</label>
              <input
                type="text"
                name="tierCode"
                disabled={isEditing}
                value={formData.tierCode}
                onChange={handleChange}
                placeholder="VD: SILVER, GOLD"
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs font-bold text-white focus:outline-none focus:border-brand-orange disabled:opacity-50 disabled:cursor-not-allowed uppercase"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-[11px] font-black uppercase tracking-wider text-zinc-400">Tên hạng hiển thị</label>
              <input
                type="text"
                name="tierName"
                value={formData.tierName}
                onChange={handleChange}
                placeholder="VD: Silver VIP Member"
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs font-bold text-white focus:outline-none focus:border-brand-orange"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-[11px] font-black uppercase tracking-wider text-zinc-400">Điểm tích lũy tối thiểu</label>
              <input
                type="number"
                name="minAccumulatedPoints"
                min="0"
                value={formData.minAccumulatedPoints}
                onChange={handleChange}
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs font-bold text-white focus:outline-none focus:border-brand-orange"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-[11px] font-black uppercase tracking-wider text-zinc-400">Tỷ lệ tích (%)</label>
              <input
                type="number"
                step="0.1"
                min="0.1"
                max="100"
                value={Number((Number(formData.earningRate || 0) * 100).toFixed(2))}
                onChange={event => setFormData(prev => ({ ...prev, earningRate: Number(event.target.value) / 100 }))}
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs font-bold text-white focus:outline-none focus:border-brand-orange"
              />
            </div>
          </div>

          <div className="rounded-xl border border-sky-500/20 bg-sky-500/[0.06] p-3 text-xs leading-5 text-zinc-400">
            <b className="text-sky-300">Công thức:</b> giá trị thanh toán hợp lệ × {(Number(formData.earningRate || 0) * 100).toLocaleString('vi-VN')}% ÷ 1.000đ/điểm, làm tròn xuống. Ví dụ 225.000đ nhận {Math.floor(225000 * Number(formData.earningRate || 0) / 1000).toLocaleString('vi-VN')} điểm.
          </div>

          <div className="space-y-1.5">
            <label className="text-[11px] font-black uppercase tracking-wider text-zinc-400">Mô tả đặc quyền</label>
            <textarea
              name="description"
              rows="3"
              value={formData.description}
              onChange={handleChange}
              placeholder="VD: Tích lũy trọn đời từ 200 điểm. Hoàn 5% giá trị mỗi giao dịch..."
              className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs font-medium text-white focus:outline-none focus:border-brand-orange resize-none"
            />
          </div>

          <div className="flex items-center gap-2 pt-1">
            <input
              type="checkbox"
              id="active"
              name="active"
              checked={formData.active}
              onChange={handleChange}
              className="h-4 w-4 rounded border-zinc-800 bg-zinc-950 text-brand-orange focus:ring-brand-orange"
            />
            <label htmlFor="active" className="text-xs font-bold text-zinc-300 cursor-pointer">
              Kích hoạt hạng thẻ này ngay sau khi lưu
            </label>
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-zinc-800/80">
            <button
              type="button"
              onClick={onClose}
              className="px-5 py-2.5 rounded-xl bg-zinc-800 hover:bg-zinc-700 text-xs font-bold text-zinc-300 transition-colors"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-6 py-2.5 rounded-xl bg-brand-orange hover:bg-opacity-95 text-xs font-black uppercase tracking-wider text-zinc-950 transition-colors shadow-lg disabled:opacity-50 flex items-center gap-2"
            >
              {isSubmitting ? (
                <>
                  <div className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-zinc-950 border-t-transparent" />
                  <span>Đang lưu...</span>
                </>
              ) : (
                <>
                  <CheckCircle2 className="h-4 w-4" />
                  <span>{isEditing ? 'Cập nhật' : 'Tạo mới'}</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
