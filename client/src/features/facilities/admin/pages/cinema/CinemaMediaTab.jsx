import { useMemo, useState } from 'react';
import {
  Archive,
  ChevronLeft,
  ChevronRight,
  Edit3,
  GripVertical,
  Image as ImageIcon,
  PlusCircle,
  Star,
} from 'lucide-react';
import { useOutletContext } from 'react-router-dom';
import adminCinemaService from '../../services/adminCinemaService';
import CinemaImageUploader from '../../components/CinemaImageUploader';
import { MEDIA_TYPE_LABELS } from '../../utils/facilityPresentation';

const PAGE_SIZE = 6;

export default function CinemaMediaTab({
  cinema,
  onAdd,
  onUpdate,
  onArchive,
  onReorder,
}) {
  const { triggerToast, triggerConfirm } = useOutletContext() || {};
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [page, setPage] = useState(1);
  const [orderedMedia, setOrderedMedia] = useState(() =>
    [...(cinema?.gallery || [])].sort(
      (left, right) => (left.displayOrder || 0) - (right.displayOrder || 0),
    ),
  );
  const [draggedId, setDraggedId] = useState(null);
  const [isOrderDirty, setIsOrderDirty] = useState(false);
  const [formData, setFormData] = useState(emptyForm());

  const pageCount = Math.max(1, Math.ceil(orderedMedia.length / PAGE_SIZE));
  const safePage = Math.min(page, pageCount);
  const visibleMedia = useMemo(() => {
    const start = (safePage - 1) * PAGE_SIZE;
    return orderedMedia.slice(start, start + PAGE_SIZE);
  }, [orderedMedia, safePage]);

  const openAddForm = () => {
    setFormData({ ...emptyForm(), displayOrder: orderedMedia.length + 1 });
    setEditingId(null);
    setIsFormOpen(true);
  };

  const openEditForm = (media) => {
    setFormData({
      mediaType: media.mediaType,
      url: media.url,
      file: null,
      title: media.title || '',
      displayOrder: media.displayOrder || 0,
      isPrimary: Boolean(media.isPrimary),
      status: media.status || 'ACTIVE',
    });
    setEditingId(media.publicId);
    setIsFormOpen(true);
  };

  const submit = async (event) => {
    event.preventDefault();
    setIsSubmitting(true);
    const payload = { ...formData };
    if (formData.file) {
      try {
        const upload = await adminCinemaService.uploadCinemaMedia(
          formData.file,
          formData.mediaType,
          cinema.publicId,
        );
        payload.url = upload.data?.secureUrl || upload.data || upload.secureUrl;
      } catch {
        triggerToast?.('Không thể tải hình ảnh lên. Vui lòng thử lại.', 'error');
        setIsSubmitting(false);
        return;
      }
    }
    if (!payload.url) {
      triggerToast?.('Vui lòng chọn hình ảnh trước khi lưu.', 'warning');
      setIsSubmitting(false);
      return;
    }
    const success = editingId
      ? await onUpdate(editingId, payload)
      : await onAdd(payload);
    setIsSubmitting(false);
    if (success) setIsFormOpen(false);
  };

  const archive = async (media) => {
    const confirmed = await triggerConfirm?.({
      title: 'Lưu trữ hình ảnh này?',
      message:
        'Hình ảnh sẽ ngừng hiển thị cho khách hàng nhưng vẫn được giữ trong dữ liệu lịch sử.',
      confirmLabel: 'Lưu trữ hình ảnh',
      tone: 'danger',
    });
    if (confirmed) await onArchive(media);
  };

  const dropOn = (targetId) => {
    if (!draggedId || draggedId === targetId) return;
    setOrderedMedia((current) => {
      const next = [...current];
      const from = next.findIndex((item) => item.publicId === draggedId);
      const to = next.findIndex((item) => item.publicId === targetId);
      const [moved] = next.splice(from, 1);
      next.splice(to, 0, moved);
      return next;
    });
    setDraggedId(null);
    setIsOrderDirty(true);
  };

  const saveOrder = async () => {
    const success = await onReorder(orderedMedia);
    if (success) setIsOrderDirty(false);
  };

  return (
    <div className="space-y-6 pb-20">
      <section className="flex flex-col gap-4 rounded-2xl border border-zinc-800 bg-zinc-900/30 p-5 md:flex-row md:items-center md:justify-between">
        <div>
          <h2 className="text-sm font-black uppercase tracking-wider text-white">
            Hình ảnh cụm rạp
          </h2>
          <p className="mt-1 text-xs text-zinc-500">
            Kéo thả các thẻ để đổi thứ tự. Danh sách được chia trang để giữ giao diện mượt.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {isOrderDirty && (
            <button
              type="button"
              onClick={saveOrder}
              className="rounded-xl border border-emerald-500/40 px-4 py-2.5 text-xs font-black text-emerald-300"
            >
              Lưu thứ tự mới
            </button>
          )}
          <button
            type="button"
            onClick={openAddForm}
            className="inline-flex items-center gap-2 rounded-xl bg-orange-500 px-4 py-2.5 text-xs font-black text-white"
          >
            <PlusCircle className="h-4 w-4" />
            Thêm hình ảnh
          </button>
        </div>
      </section>

      {orderedMedia.length === 0 ? (
        <div className="flex flex-col items-center rounded-3xl border-2 border-dashed border-zinc-800 py-16 text-center">
          <ImageIcon className="h-10 w-10 text-zinc-700" />
          <p className="mt-4 text-sm font-bold text-zinc-300">Chưa có hình ảnh</p>
          <p className="mt-2 text-xs text-zinc-500">
            Thêm ít nhất một ảnh chính để hoàn thành hồ sơ cụm rạp.
          </p>
        </div>
      ) : (
        <>
          <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
            {visibleMedia.map((media) => (
              <article
                key={media.publicId}
                draggable
                onDragStart={() => setDraggedId(media.publicId)}
                onDragOver={(event) => event.preventDefault()}
                onDrop={() => dropOn(media.publicId)}
                className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/50"
              >
                <div className="relative aspect-video bg-zinc-950">
                  <img
                    src={media.url}
                    alt={media.title || 'Hình ảnh cụm rạp'}
                    loading="lazy"
                    decoding="async"
                    className="h-full w-full object-cover"
                  />
                  <div className="absolute left-3 top-3 flex gap-2">
                    <span className="rounded-lg bg-zinc-950/85 px-2 py-1 text-[10px] font-bold text-zinc-200">
                      {MEDIA_TYPE_LABELS[media.mediaType] || 'Thư viện'}
                    </span>
                    {media.isPrimary && (
                      <span className="inline-flex items-center gap-1 rounded-lg bg-orange-500 px-2 py-1 text-[10px] font-black text-white">
                        <Star className="h-3 w-3 fill-current" />
                        Ảnh chính
                      </span>
                    )}
                  </div>
                </div>
                <div className="p-4">
                  <div className="flex items-start gap-3">
                    <GripVertical className="mt-0.5 h-5 w-5 cursor-grab text-zinc-600" />
                    <div className="min-w-0 flex-1">
                      <h3 className="truncate text-sm font-bold text-zinc-100">
                        {media.title || 'Hình ảnh chưa đặt tên'}
                      </h3>
                      <p className="mt-1 text-xs text-zinc-500">
                        Vị trí hiển thị {media.displayOrder || 'chưa sắp xếp'}
                      </p>
                    </div>
                  </div>
                  <div className="mt-4 grid grid-cols-2 gap-2">
                    <button
                      type="button"
                      onClick={() => openEditForm(media)}
                      className="inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-700 px-3 py-2 text-xs font-bold text-zinc-200"
                    >
                      <Edit3 className="h-4 w-4" />
                      Chỉnh sửa
                    </button>
                    <button
                      type="button"
                      onClick={() => archive(media)}
                      className="inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-800 px-3 py-2 text-xs font-bold text-zinc-400 hover:border-red-500/40 hover:text-red-300"
                    >
                      <Archive className="h-4 w-4" />
                      Lưu trữ
                    </button>
                  </div>
                </div>
              </article>
            ))}
          </div>

          <div className="flex items-center justify-between rounded-2xl border border-zinc-800 px-4 py-3">
            <p className="text-xs text-zinc-500">
              Trang {safePage}/{pageCount} · {orderedMedia.length} hình ảnh
            </p>
            <div className="flex gap-2">
              <PageButton
                label="Trang trước"
                icon={ChevronLeft}
                disabled={safePage === 1}
                onClick={() => setPage(Math.max(1, safePage - 1))}
              />
              <PageButton
                label="Trang sau"
                icon={ChevronRight}
                disabled={safePage === pageCount}
                onClick={() => setPage(Math.min(pageCount, safePage + 1))}
              />
            </div>
          </div>
        </>
      )}

      {isFormOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-sm">
          <form
            onSubmit={submit}
            className="w-full max-w-lg rounded-3xl border border-zinc-800 bg-zinc-950 p-6 shadow-2xl"
          >
            <h2 className="text-lg font-black text-white">
              {editingId ? 'Chỉnh sửa hình ảnh' : 'Thêm hình ảnh mới'}
            </h2>
            <div className="mt-5">
              <CinemaImageUploader
                label="Tệp hình ảnh"
                description="Ảnh được tải lên khi bạn lưu"
                aspectRatio={16 / 9}
                value={formData.file || formData.url}
                onChange={(file) => setFormData({ ...formData, file })}
              />
            </div>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              {!editingId && (
                <label>
                  <span className="text-[10px] font-black uppercase text-zinc-500">
                    Mục đích sử dụng
                  </span>
                  <select
                    value={formData.mediaType}
                    onChange={(event) =>
                      setFormData({ ...formData, mediaType: event.target.value })
                    }
                    className="mt-2 w-full rounded-xl border border-zinc-800 bg-zinc-900 px-4 py-3 text-sm"
                  >
                    <option value="BANNER">Ảnh bìa</option>
                    <option value="GALLERY">Thư viện</option>
                    <option value="MAP">Sơ đồ rạp</option>
                  </select>
                </label>
              )}
              <label className={editingId ? 'sm:col-span-2' : ''}>
                <span className="text-[10px] font-black uppercase text-zinc-500">
                  Tên mô tả
                </span>
                <input
                  value={formData.title}
                  onChange={(event) => setFormData({ ...formData, title: event.target.value })}
                  placeholder="Ví dụ: Sảnh chờ tầng 2"
                  className="mt-2 w-full rounded-xl border border-zinc-800 bg-zinc-900 px-4 py-3 text-sm outline-none focus:border-orange-500"
                />
              </label>
            </div>
            <label className="mt-4 flex items-center gap-3 text-sm font-bold text-zinc-300">
              <input
                type="checkbox"
                checked={formData.isPrimary}
                onChange={(event) =>
                  setFormData({ ...formData, isPrimary: event.target.checked })
                }
              />
              Dùng làm ảnh đại diện chính
            </label>
            <div className="mt-6 flex gap-3">
              <button
                type="button"
                onClick={() => setIsFormOpen(false)}
                className="flex-1 rounded-xl border border-zinc-800 py-3 text-xs font-black text-zinc-300"
              >
                Quay lại
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="flex-1 rounded-xl bg-orange-500 py-3 text-xs font-black text-white disabled:opacity-50"
              >
                {isSubmitting ? 'Đang lưu...' : 'Lưu hình ảnh'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}

function emptyForm() {
  return {
    mediaType: 'GALLERY',
    url: '',
    file: null,
    title: '',
    displayOrder: 0,
    isPrimary: false,
    status: 'ACTIVE',
  };
}

function PageButton({ label, icon: Icon, ...props }) {
  return (
    <button
      type="button"
      {...props}
      className="inline-flex items-center gap-2 rounded-xl border border-zinc-800 px-3 py-2 text-xs font-bold text-zinc-300 disabled:opacity-30"
    >
      <Icon className="h-4 w-4" />
      <span className="hidden sm:inline">{label}</span>
    </button>
  );
}
