import { useEffect, useMemo, useState } from 'react';
import { Edit3, Info, Search, ShieldCheck } from 'lucide-react';
import { useOutletContext } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { EmptyState, ErrorState, LoadingState } from '@/components/common/ui/uiKit';
import useAdminSeatTypes from '../hooks/useAdminSeatTypes';

const STATUS_PRESENTATION = {
  ACTIVE: {
    label: 'Đang sử dụng',
    description: 'Có thể gán cho ghế trong phòng chiếu.',
    className: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-400',
  },
  INACTIVE: {
    label: 'Tạm ngừng',
    description: 'Không dùng cho cấu hình phòng mới; dữ liệu cũ vẫn được giữ.',
    className: 'border-zinc-700 bg-zinc-800 text-zinc-400',
  },
};

export default function AdminSeatTypePage() {
  const { triggerToast } = useOutletContext() || {};
  const { userRole } = useAuth();
  const {
    seatTypes,
    isLoading,
    error,
    fetchSeatTypes,
    updateSeatType,
  } = useAdminSeatTypes(triggerToast);

  const normalizedRole = String(userRole || '').replace(/^ROLE_/, '');
  const canConfigure = normalizedRole === 'ADMIN';
  const [searchTerm, setSearchTerm] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formData, setFormData] = useState({
    code: '',
    name: '',
    description: '',
    status: 'ACTIVE',
  });

  useEffect(() => {
    fetchSeatTypes();
  }, [fetchSeatTypes]);

  const filteredSeatTypes = useMemo(() => {
    const keyword = searchTerm.trim().toLocaleLowerCase('vi');
    if (!keyword) return seatTypes;
    return seatTypes.filter((seatType) => (
      seatType.name?.toLocaleLowerCase('vi').includes(keyword)
      || seatType.description?.toLocaleLowerCase('vi').includes(keyword)
    ));
  }, [searchTerm, seatTypes]);

  const openEditForm = (seatType) => {
    if (!canConfigure) return;
    setFormData({
      code: seatType.code,
      name: seatType.name,
      description: seatType.description || '',
      status: seatType.status || 'ACTIVE',
    });
    setEditingId(seatType.publicId);
  };

  const closeForm = () => {
    setEditingId(null);
  };

  const handleFormSubmit = async (event) => {
    event.preventDefault();
    if (!canConfigure || !editingId) return;
    setIsSubmitting(true);
    const success = await updateSeatType(editingId, formData);
    setIsSubmitting(false);
    if (success) closeForm();
  };

  return (
    <div className="flex min-h-[400px] flex-1 flex-col space-y-6 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <header className="border-b border-zinc-900 pb-5">
        <h1 className="text-xl font-black uppercase tracking-wide text-zinc-50 md:text-2xl">
          Loại ghế dùng trong rạp
        </h1>
        <p className="mt-1 text-xs text-zinc-400">
          Quản lý tên hiển thị, mô tả và khả năng sử dụng của các nhóm ghế cố định.
        </p>
      </header>

      <section className={`rounded-2xl border p-5 ${
        canConfigure
          ? 'border-emerald-500/20 bg-emerald-500/5'
          : 'border-amber-500/20 bg-amber-500/5'
      }`}>
        <div className="flex gap-3">
          {canConfigure
            ? <ShieldCheck className="h-5 w-5 shrink-0 text-emerald-400" />
            : <Info className="h-5 w-5 shrink-0 text-amber-400" />}
          <div>
            <h2 className="text-sm font-black text-white">
              {canConfigure ? 'Bạn có quyền thay đổi cấu hình loại ghế' : 'Chế độ chỉ xem'}
            </h2>
            <p className="mt-1 text-xs leading-5 text-zinc-400">
              Loại ghế được dùng chung cho toàn hệ thống. Việc tạm ngừng một loại ghế
              không xóa ghế hoặc dữ liệu lịch sử đã có.
              {!canConfigure && ' Chỉ quản trị viên hệ thống mới được thay đổi cấu hình này.'}
            </p>
          </div>
        </div>
      </section>

      <section className="rounded-2xl border border-zinc-900 bg-zinc-900/20 p-4">
        <div className="relative max-w-lg">
          <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-500" />
          <input
            type="search"
            placeholder="Tìm theo tên hoặc mô tả loại ghế..."
            value={searchTerm}
            onChange={(event) => setSearchTerm(event.target.value)}
            className="w-full rounded-xl border border-zinc-800 bg-zinc-950 py-3 pl-11 pr-4 text-sm text-zinc-200 outline-none transition-colors focus:border-brand-orange"
          />
        </div>
      </section>

      <section className="relative overflow-hidden rounded-3xl border border-zinc-900 bg-zinc-900/20">
        {isLoading && <LoadingState message="Đang tải danh sách loại ghế..." />}
        {!isLoading && error && <ErrorState message={error} onRetry={fetchSeatTypes} />}
        {!isLoading && !error && filteredSeatTypes.length === 0 && (
          <EmptyState message="Không tìm thấy loại ghế phù hợp" />
        )}
        {!isLoading && !error && filteredSeatTypes.length > 0 && (
          <div className="divide-y divide-zinc-900">
            {filteredSeatTypes.map((seatType) => {
              const presentation = STATUS_PRESENTATION[seatType.status]
                || STATUS_PRESENTATION.INACTIVE;
              return (
                <article
                  key={seatType.publicId}
                  className="grid gap-4 p-5 transition-colors hover:bg-zinc-900/30 lg:grid-cols-[1fr_2fr_1fr_auto] lg:items-center"
                >
                  <div>
                    <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">
                      Loại ghế
                    </p>
                    <p className="mt-1 text-sm font-black text-white">{seatType.name}</p>
                  </div>
                  <div>
                    <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">
                      Mô tả cho nhân viên
                    </p>
                    <p className="mt-1 text-sm leading-5 text-zinc-400">
                      {seatType.description || 'Chưa có mô tả'}
                    </p>
                  </div>
                  <div>
                    <span className={`inline-flex rounded-lg border px-3 py-1.5 text-[10px] font-black ${presentation.className}`}>
                      {presentation.label}
                    </span>
                    <p className="mt-2 max-w-xs text-[11px] leading-4 text-zinc-500">
                      {presentation.description}
                    </p>
                  </div>
                  {canConfigure && (
                    <button
                      type="button"
                      onClick={() => openEditForm(seatType)}
                      className="inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-700 px-4 py-2.5 text-xs font-bold text-zinc-200 hover:border-brand-orange hover:text-brand-orange"
                    >
                      <Edit3 className="h-4 w-4" />
                      Chỉnh sửa
                    </button>
                  )}
                </article>
              );
            })}
          </div>
        )}
      </section>

      {editingId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-sm">
          <div className="w-full max-w-lg rounded-3xl border border-zinc-800 bg-zinc-950 p-6 shadow-2xl">
            <h2 className="text-lg font-black uppercase text-white">Chỉnh sửa loại ghế</h2>
            <p className="mt-1 text-xs leading-5 text-zinc-500">
              Thay đổi này áp dụng cho cách hiển thị và lựa chọn loại ghế trên toàn hệ thống.
            </p>
            <form onSubmit={handleFormSubmit} className="mt-6 space-y-4">
              <label className="block space-y-2 text-[10px] font-black uppercase tracking-widest text-zinc-500">
                Tên hiển thị
                <input
                  type="text"
                  required
                  value={formData.name}
                  onChange={(event) => setFormData((current) => ({
                    ...current,
                    name: event.target.value,
                  }))}
                  className="w-full rounded-xl border border-zinc-800 bg-zinc-900 px-4 py-3 text-sm font-semibold normal-case text-white outline-none focus:border-brand-orange"
                />
              </label>
              <label className="block space-y-2 text-[10px] font-black uppercase tracking-widest text-zinc-500">
                Mô tả dễ hiểu
                <textarea
                  rows={3}
                  value={formData.description}
                  onChange={(event) => setFormData((current) => ({
                    ...current,
                    description: event.target.value,
                  }))}
                  className="w-full rounded-xl border border-zinc-800 bg-zinc-900 px-4 py-3 text-sm font-semibold normal-case text-white outline-none focus:border-brand-orange"
                />
              </label>
              <label className="block space-y-2 text-[10px] font-black uppercase tracking-widest text-zinc-500">
                Khả năng sử dụng
                <select
                  value={formData.status}
                  onChange={(event) => setFormData((current) => ({
                    ...current,
                    status: event.target.value,
                  }))}
                  className="w-full rounded-xl border border-zinc-800 bg-zinc-900 px-4 py-3 text-sm font-semibold normal-case text-white outline-none focus:border-brand-orange"
                >
                  <option value="ACTIVE">Đang sử dụng</option>
                  <option value="INACTIVE">Tạm ngừng sử dụng</option>
                </select>
              </label>
              <div className="rounded-xl border border-amber-500/20 bg-amber-500/5 p-3 text-xs leading-5 text-amber-100/80">
                Tạm ngừng không xóa dữ liệu cũ. Hãy kiểm tra các phòng đang dùng loại
                ghế này trước khi lưu thay đổi.
              </div>
              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={closeForm}
                  className="flex-1 rounded-xl border border-zinc-700 py-3 text-xs font-bold text-zinc-300"
                >
                  Quay lại
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="flex-1 rounded-xl bg-brand-orange py-3 text-xs font-black text-white disabled:opacity-50"
                >
                  {isSubmitting ? 'Đang lưu...' : 'Lưu thay đổi'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
