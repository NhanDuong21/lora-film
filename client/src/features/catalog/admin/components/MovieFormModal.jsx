import { useEffect, useState } from 'react';
import {
  Check,
  ChevronDown,
  Film,
  Image,
  Loader2,
  MonitorPlay,
  Tags,
  X,
} from 'lucide-react';
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

const OPTIONAL_FIELDS = ['originalTitle', 'country', 'endDate', 'synopsis'];

const hasOptionalMovieInformation = movie => OPTIONAL_FIELDS.some(field => Boolean(movie?.[field]));

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
  const [showOptional, setShowOptional] = useState(() => hasOptionalMovieInformation(selectedMovie));

  useEffect(() => {
    if (!selectedMovie) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setFormBasic(emptyForm());
      setFormErrors({});
      setShowOptional(false);
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
    setShowOptional(hasOptionalMovieInformation(selectedMovie));
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

    if (OPTIONAL_FIELDS.some(field => errors[field])) {
      setShowOptional(true);
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
        if (OPTIONAL_FIELDS.some(field => errors[field])) {
          setShowOptional(true);
        }
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
        className={`flex max-h-[92vh] w-full flex-col overflow-hidden rounded-2xl border border-zinc-700 bg-zinc-950 shadow-2xl ${
          isEdit ? 'max-w-3xl' : 'max-w-5xl'
        }`}
      >
        <header className="flex shrink-0 items-start justify-between gap-4 border-b border-zinc-800 px-5 py-4 md:px-7">
          <div className="flex items-start gap-3">
            <span className="rounded-xl bg-orange-500/10 p-2.5 text-orange-400">
              <Film className="h-5 w-5" />
            </span>
            <div>
              {!isEdit && (
                <p className="mb-1 text-[10px] font-black uppercase tracking-[0.16em] text-orange-400">
                  Bước 1 · Thông tin cơ bản
                </p>
              )}
              <h1 id="movie-form-title" className="text-lg font-black text-white md:text-xl">
                {isEdit ? 'Sửa thông tin cơ bản' : 'Tạo hồ sơ phim'}
              </h1>
              <p className="mt-1 text-xs leading-5 text-zinc-500">
                {isEdit
                  ? 'Cập nhật thông tin nhận diện và phát hành cơ bản của phim.'
                  : 'Nhập những thông tin cần thiết trước. Bạn sẽ hoàn thiện nội dung ở bước tiếp theo.'}
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
            <div className={`grid gap-5 ${isEdit ? '' : 'lg:grid-cols-[minmax(0,1fr)_16rem]'}`}>
              <div className="space-y-4">
                <div className="rounded-2xl border border-zinc-800 bg-zinc-900/35 p-4 md:p-6">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <h2 className="text-sm font-bold text-white">Thông tin bắt buộc</h2>
                      <p className="mt-1 text-xs text-zinc-500">
                        Cần điền đủ 4 mục để hồ sơ phim hợp lệ.
                      </p>
                    </div>
                    <span className="rounded-full border border-orange-500/20 bg-orange-500/10 px-2.5 py-1 text-[10px] font-bold text-orange-300">
                      4 mục
                    </span>
                  </div>

                  <div className="mt-5 grid gap-4 md:grid-cols-2">
                    <Field label="Tên phim" required error={formErrors.title}>
                      <Input
                        autoFocus
                        aria-label="Tên phim"
                        value={formBasic.title}
                        onChange={event => setFormBasic(current => ({ ...current, title: event.target.value }))}
                        placeholder="Tên hiển thị với khách hàng"
                      />
                    </Field>
                    <Field label="Thời lượng (phút)" required error={formErrors.durationMinutes}>
                      <Input
                        type="number"
                        min="1"
                        aria-label="Thời lượng (phút)"
                        value={formBasic.durationMinutes}
                        onChange={event => setFormBasic(current => ({ ...current, durationMinutes: event.target.value }))}
                        placeholder="Ví dụ: 120"
                      />
                    </Field>
                    <Field label="Phân loại độ tuổi" required error={formErrors.ageRating}>
                      <Select
                        aria-label="Phân loại độ tuổi"
                        value={formBasic.ageRating}
                        onChange={event => setFormBasic(current => ({ ...current, ageRating: event.target.value }))}
                      >
                        {AGE_RATINGS.map(rating => (
                          <option key={rating} value={rating}>{AGE_RATING_LABELS[rating]}</option>
                        ))}
                      </Select>
                    </Field>
                    <Field label="Ngày khởi chiếu" required error={formErrors.showingStartDate}>
                      <Input
                        type="date"
                        aria-label="Ngày khởi chiếu"
                        value={formBasic.showingStartDate}
                        onChange={event => setFormBasic(current => ({ ...current, showingStartDate: event.target.value }))}
                      />
                    </Field>
                  </div>
                </div>

                <div className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/20">
                  <button
                    type="button"
                    aria-expanded={showOptional}
                    aria-controls="movie-optional-fields"
                    onClick={() => setShowOptional(current => !current)}
                    className="flex w-full items-center justify-between gap-4 p-4 text-left transition hover:bg-zinc-900/60 md:px-6"
                  >
                    <span>
                      <span className="flex flex-wrap items-center gap-2">
                        <span className="text-sm font-bold text-zinc-200">Thông tin bổ sung</span>
                        <span className="rounded-full bg-zinc-800 px-2 py-0.5 text-[10px] font-semibold text-zinc-500">
                          Không bắt buộc
                        </span>
                      </span>
                      <span className="mt-1 block text-xs leading-5 text-zinc-500">
                        Tên gốc, quốc gia, ngày ngừng chiếu và tóm tắt nội dung
                      </span>
                    </span>
                    <ChevronDown
                      className={`h-5 w-5 shrink-0 text-zinc-500 transition-transform ${
                        showOptional ? 'rotate-180' : ''
                      }`}
                    />
                  </button>

                  {showOptional && (
                    <div id="movie-optional-fields" className="border-t border-zinc-800 p-4 md:p-6">
                      <div className="grid gap-4 md:grid-cols-2">
                        <Field label="Tên gốc">
                          <Input
                            aria-label="Tên gốc"
                            value={formBasic.originalTitle}
                            onChange={event => setFormBasic(current => ({ ...current, originalTitle: event.target.value }))}
                            placeholder="Tên phim ở ngôn ngữ gốc"
                          />
                        </Field>
                        <Field label="Quốc gia sản xuất">
                          <Input
                            aria-label="Quốc gia sản xuất"
                            value={formBasic.country}
                            onChange={event => setFormBasic(current => ({ ...current, country: event.target.value }))}
                            placeholder="Ví dụ: Việt Nam, Hoa Kỳ"
                          />
                        </Field>
                        <Field label="Ngày ngừng chiếu" error={formErrors.endDate}>
                          <Input
                            type="date"
                            aria-label="Ngày ngừng chiếu"
                            value={formBasic.endDate}
                            onChange={event => setFormBasic(current => ({ ...current, endDate: event.target.value }))}
                          />
                        </Field>
                        <div className="hidden md:block" aria-hidden="true" />
                        <div className="md:col-span-2">
                          <Field label="Tóm tắt nội dung">
                            <Textarea
                              rows={4}
                              aria-label="Tóm tắt nội dung"
                              value={formBasic.synopsis}
                              onChange={event => setFormBasic(current => ({ ...current, synopsis: event.target.value }))}
                              placeholder="Mô tả ngắn gọn nội dung phim để khách hàng dễ lựa chọn"
                            />
                          </Field>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>

              {!isEdit && (
                <aside className="h-fit rounded-2xl border border-sky-500/20 bg-sky-500/[0.04] p-5 lg:sticky lg:top-0">
                  <p className="text-sm font-bold text-sky-100">Sau khi tạo hồ sơ</p>
                  <p className="mt-2 text-xs leading-5 text-zinc-400">
                    Hệ thống sẽ mở trang chi tiết để bạn hoàn thiện dần các nội dung sau.
                  </p>
                  <ol className="mt-5 space-y-4">
                    <li className="flex gap-3">
                      <span className="rounded-lg bg-sky-500/10 p-2 text-sky-300">
                        <Image className="h-4 w-4" />
                      </span>
                      <span>
                        <span className="block text-xs font-bold text-zinc-200">Poster và hình ảnh</span>
                        <span className="mt-0.5 block text-[11px] leading-4 text-zinc-500">Chọn ảnh hiển thị chính</span>
                      </span>
                    </li>
                    <li className="flex gap-3">
                      <span className="rounded-lg bg-sky-500/10 p-2 text-sky-300">
                        <Tags className="h-4 w-4" />
                      </span>
                      <span>
                        <span className="block text-xs font-bold text-zinc-200">Thể loại phim</span>
                        <span className="mt-0.5 block text-[11px] leading-4 text-zinc-500">Giúp khách hàng dễ tìm phim</span>
                      </span>
                    </li>
                    <li className="flex gap-3">
                      <span className="rounded-lg bg-sky-500/10 p-2 text-sky-300">
                        <MonitorPlay className="h-4 w-4" />
                      </span>
                      <span>
                        <span className="block text-xs font-bold text-zinc-200">Bản chiếu</span>
                        <span className="mt-0.5 block text-[11px] leading-4 text-zinc-500">Ví dụ: 2D phụ đề, lồng tiếng</span>
                      </span>
                    </li>
                  </ol>
                  <p className="mt-5 rounded-xl border border-zinc-800 bg-zinc-950/50 p-3 text-[11px] leading-5 text-zinc-500">
                    Bạn không cần nhập tất cả trong một lần. Phim mới được lưu ở trạng thái cần hoàn thiện.
                  </p>
                </aside>
              )}
            </div>
          </div>

          <footer className="flex shrink-0 flex-col gap-3 border-t border-zinc-800 bg-zinc-950 px-5 py-4 sm:flex-row sm:items-center sm:justify-between md:px-7">
            {!isEdit && (
              <p className="hidden text-xs text-zinc-600 sm:block">
                Bước tiếp theo: hoàn thiện hồ sơ phim
              </p>
            )}
            <div className={`flex flex-col-reverse gap-2 sm:flex-row ${isEdit ? 'sm:ml-auto' : ''}`}>
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
                {isSaving ? 'Đang lưu…' : isEdit ? 'Lưu thay đổi' : 'Tạo và tiếp tục'}
              </button>
            </div>
          </footer>
        </form>
      </section>
    </div>
  );
}
