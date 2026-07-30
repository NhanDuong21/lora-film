import { useEffect, useState } from 'react';
import { Check, Film, Info, Loader2, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import { Field, Input, Select, Textarea } from '@/components/common/ui/uiKit';
import { parseApiError } from '@/utils/apiErrorHandler';
import {
  AGE_RATINGS,
  AGE_RATING_LABELS,
  getTodayString,
} from '@/utils/movieHelpers';

const emptyForm = () => ({
  title: '',
  originalTitle: '',
  durationMinutes: '',
  ageRating: 'P',
  showingStartDate: '',
  endDate: '',
  country: '',
  synopsis: '',
});

export default function MovieFormModal({
  selectedMovie,
  triggerToast,
  onClose,
  onRefreshList,
  detailQuery = '',
}) {
  const isEdit = Boolean(selectedMovie);
  const navigate = useNavigate();
  const [isSaving, setIsSaving] = useState(false);
  const [formErrors, setFormErrors] = useState({});
  const [formBasic, setFormBasic] = useState(emptyForm());

  useEffect(() => {
    if (!selectedMovie) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setFormBasic(emptyForm());
      setFormErrors({});
      return;
    }

    setFormBasic({
      title: selectedMovie.title || '',
      originalTitle: selectedMovie.originalTitle || '',
      durationMinutes: selectedMovie.durationMinutes || '',
      ageRating: selectedMovie.ageRating || 'P',
      showingStartDate: selectedMovie.releaseDate || '',
      endDate: selectedMovie.endDate || '',
      country: selectedMovie.country || '',
      synopsis: selectedMovie.synopsis || '',
    });
  }, [selectedMovie]);

  const validateForm = () => {
    const errors = {};

    if (!formBasic.title.trim()) errors.title = 'Vui lòng nhập tên phim.';
    if (!formBasic.durationMinutes || Number(formBasic.durationMinutes) <= 0) {
      errors.durationMinutes = 'Thời lượng phải lớn hơn 0 phút.';
    }
    if (!formBasic.ageRating || !AGE_RATINGS.includes(formBasic.ageRating)) {
      errors.ageRating = 'Vui lòng chọn phân loại độ tuổi.';
    }
    if (!formBasic.showingStartDate) {
      errors.showingStartDate = 'Vui lòng chọn ngày khởi chiếu.';
    }
    if (
      formBasic.endDate
      && formBasic.showingStartDate
      && new Date(formBasic.endDate) < new Date(formBasic.showingStartDate)
    ) {
      errors.endDate = 'Ngày ngừng chiếu phải sau ngày khởi chiếu.';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSave = async event => {
    event.preventDefault();
    if (!validateForm()) return;

    setIsSaving(true);
    try {
      const moviePayload = {
        title: formBasic.title.trim(),
        originalTitle: formBasic.originalTitle.trim() || null,
        durationMinutes: Number(formBasic.durationMinutes),
        ageRating: formBasic.ageRating,
        releaseDate: formBasic.showingStartDate || getTodayString(),
        endDate: formBasic.endDate || null,
        country: formBasic.country.trim() || null,
        synopsis: formBasic.synopsis.trim() || null,
      };

      if (selectedMovie) {
        await adminMovieService.updateMovie(selectedMovie.publicId, moviePayload);
        triggerToast?.('Đã lưu thông tin phim.');
        await onRefreshList?.();
        onClose();
      } else {
        const response = await adminMovieService.createMovie(moviePayload);
        const publicId = response?.data?.publicId || response?.publicId;
        if (!publicId) {
          throw new Error('Không nhận được mã phim từ hệ thống. Vui lòng thử lại.');
        }

        triggerToast?.('Đã tạo phim. Hãy hoàn thiện các bước còn lại trong hồ sơ phim.');
        await onRefreshList?.();
        onClose();
        navigate(`/admin/movies/${encodeURIComponent(publicId)}${detailQuery}`);
      }
    } catch (error) {
      const payload = error?.response?.data || error;
      if (payload?.errorCode === 'VALIDATION_ERROR' && payload.data?.fieldErrors) {
        const errors = {};
        payload.data.fieldErrors.forEach(item => {
          const field = item.field === 'releaseDate' ? 'showingStartDate' : item.field;
          errors[field] = item.message;
        });
        setFormErrors(errors);
        triggerToast?.('Một số thông tin chưa hợp lệ. Vui lòng kiểm tra các mục được đánh dấu.', 'error');
      } else {
        triggerToast?.(parseApiError(error), 'error');
      }
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-3 backdrop-blur-sm sm:p-6">
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="movie-form-title"
        className="flex max-h-[92vh] w-full max-w-4xl flex-col overflow-hidden rounded-2xl border border-zinc-700 bg-zinc-950 shadow-2xl"
      >
        <header className="flex shrink-0 items-start justify-between gap-4 border-b border-zinc-800 px-5 py-4 md:px-7">
          <div className="flex items-start gap-3">
            <span className="rounded-xl bg-orange-500/10 p-2.5 text-orange-400">
              <Film className="h-5 w-5" />
            </span>
            <div>
              <h1 id="movie-form-title" className="text-lg font-black text-white md:text-xl">
                {isEdit ? 'Sửa thông tin phim' : 'Thêm phim thủ công'}
              </h1>
              <p className="mt-1 text-xs leading-5 text-zinc-500">
                {isEdit
                  ? 'Cập nhật các thông tin cơ bản. Trạng thái phát hành được quản lý trong hồ sơ phim.'
                  : 'Dùng khi phim chưa có trong nguồn nhập tự động. Phim mới sẽ ở trạng thái cần hoàn thiện.'}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng biểu mẫu"
            className="rounded-xl border border-zinc-800 bg-zinc-900 p-2 text-zinc-400 transition hover:text-white"
          >
            <X className="h-4 w-4" />
          </button>
        </header>

        <form onSubmit={handleSave} className="flex min-h-0 flex-1 flex-col">
          <div className="min-h-0 flex-1 overflow-y-auto px-5 py-5 md:px-7">
            {!isEdit && (
              <div className="mb-5 flex items-start gap-3 rounded-xl border border-sky-500/20 bg-sky-500/5 p-3 text-xs leading-5 text-sky-200">
                <Info className="mt-0.5 h-4 w-4 shrink-0 text-sky-400" />
                <span>
                  Sau khi tạo, hệ thống sẽ mở hồ sơ phim để bạn thêm bản chiếu, poster, thể loại và các nội dung liên quan.
                </span>
              </div>
            )}

            <div className="rounded-2xl border border-zinc-800 bg-zinc-900/35 p-4 md:p-6">
              <h2 className="text-sm font-bold text-white">Thông tin cơ bản</h2>
              <p className="mt-1 text-xs text-zinc-500">
                Các mục có dấu <span className="text-orange-400">*</span> là bắt buộc.
              </p>

              <div className="mt-5 grid gap-4 md:grid-cols-2">
                <Field label="Tên phim" required error={formErrors.title}>
                  <Input
                    autoFocus
                    value={formBasic.title}
                    onChange={event => setFormBasic(current => ({ ...current, title: event.target.value }))}
                    placeholder="Tên hiển thị với khách hàng"
                  />
                </Field>
                <Field label="Tên gốc">
                  <Input
                    value={formBasic.originalTitle}
                    onChange={event => setFormBasic(current => ({ ...current, originalTitle: event.target.value }))}
                    placeholder="Tên phim ở ngôn ngữ gốc"
                  />
                </Field>
                <Field label="Thời lượng (phút)" required error={formErrors.durationMinutes}>
                  <Input
                    type="number"
                    min="1"
                    value={formBasic.durationMinutes}
                    onChange={event => setFormBasic(current => ({ ...current, durationMinutes: event.target.value }))}
                    placeholder="Ví dụ: 120"
                  />
                </Field>
                <Field label="Quốc gia sản xuất">
                  <Input
                    value={formBasic.country}
                    onChange={event => setFormBasic(current => ({ ...current, country: event.target.value }))}
                    placeholder="Ví dụ: Việt Nam, Hoa Kỳ"
                  />
                </Field>
                <Field label="Phân loại độ tuổi" required error={formErrors.ageRating}>
                  <Select
                    value={formBasic.ageRating}
                    onChange={event => setFormBasic(current => ({ ...current, ageRating: event.target.value }))}
                  >
                    {AGE_RATINGS.map(rating => (
                      <option key={rating} value={rating}>{AGE_RATING_LABELS[rating]}</option>
                    ))}
                  </Select>
                </Field>
                <div className="hidden md:block" aria-hidden="true" />
                <Field label="Ngày khởi chiếu" required error={formErrors.showingStartDate}>
                  <Input
                    type="date"
                    value={formBasic.showingStartDate}
                    onChange={event => setFormBasic(current => ({ ...current, showingStartDate: event.target.value }))}
                  />
                </Field>
                <Field label="Ngày ngừng chiếu" error={formErrors.endDate}>
                  <Input
                    type="date"
                    value={formBasic.endDate}
                    onChange={event => setFormBasic(current => ({ ...current, endDate: event.target.value }))}
                  />
                </Field>
              </div>

              <div className="mt-4">
                <Field label="Tóm tắt nội dung">
                  <Textarea
                    rows={5}
                    value={formBasic.synopsis}
                    onChange={event => setFormBasic(current => ({ ...current, synopsis: event.target.value }))}
                    placeholder="Mô tả ngắn gọn nội dung phim để khách hàng dễ lựa chọn"
                  />
                </Field>
              </div>
            </div>
          </div>

          <footer className="flex shrink-0 flex-col-reverse gap-2 border-t border-zinc-800 bg-zinc-950 px-5 py-4 sm:flex-row sm:justify-end md:px-7">
            <button
              type="button"
              onClick={onClose}
              disabled={isSaving}
              className="h-11 rounded-xl border border-zinc-700 px-5 text-sm font-bold text-zinc-300 transition hover:bg-zinc-800 disabled:opacity-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSaving}
              className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-orange-500 px-6 text-sm font-black text-zinc-950 transition hover:bg-orange-400 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isSaving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Check className="h-4 w-4" />}
              {isSaving ? 'Đang lưu…' : isEdit ? 'Lưu thay đổi' : 'Tạo phim'}
            </button>
          </footer>
        </form>
      </section>
    </div>
  );
}
