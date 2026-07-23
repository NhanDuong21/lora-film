import { useState, useEffect } from 'react';
import { ArrowLeft, Check, Film, Info } from 'lucide-react';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import { Field, Input, Select, Textarea } from '@/components/common/ui/uiKit';
import { parseApiError } from '@/utils/apiErrorHandler';
import {
  AGE_RATINGS,
  AGE_RATING_LABELS,
  getTodayString,
} from '@/utils/movieHelpers';
import { useNavigate } from 'react-router-dom';

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

// A simplified generic section component
function FormSection({ title, icon, children }) {
  return (
    <div className="bg-[#050506] border border-zinc-800 rounded-2xl p-6">
      <div className="flex items-center justify-between border-b border-zinc-800 pb-4 mb-4">
        <h2 className="text-sm font-black uppercase tracking-widest text-zinc-100 flex items-center gap-2">
          {icon} {title}
        </h2>
      </div>
      {children}
    </div>
  );
}

export default function MovieFormModal({ selectedMovie, triggerToast, onClose, onRefreshList, detailQuery = '' }) {
  const isEdit = !!selectedMovie;
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
    // For edit, just populate basic fields
     
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
    const errs = {};

    if (!formBasic.title.trim()) errs.title = 'Tên phim không được để trống.';
    if (!formBasic.durationMinutes || Number(formBasic.durationMinutes) <= 0)
      errs.durationMinutes = 'Thời lượng phải là số dương.';
    if (!formBasic.ageRating || !AGE_RATINGS.includes(formBasic.ageRating))
      errs.ageRating = `Độ tuổi phải là một trong: ${AGE_RATINGS.join(', ')}.`;

    if (!formBasic.showingStartDate) {
      errs.showingStartDate = 'Ngày khởi chiếu bắt buộc phải chọn.';
    }
    
    if (formBasic.endDate && formBasic.showingStartDate) {
      if (new Date(formBasic.endDate) < new Date(formBasic.showingStartDate)) {
        errs.endDate = 'Ngày kết thúc không thể trước ngày khởi chiếu.';
      }
    }

    setFormErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    setIsSaving(true);
    try {
      const moviePayload = {
        title: formBasic.title?.trim() || '',
        originalTitle: formBasic.originalTitle?.trim() || null,
        durationMinutes: Number(formBasic.durationMinutes),
        ageRating: formBasic.ageRating,
        releaseDate: formBasic.showingStartDate || getTodayString(),
        endDate: formBasic.endDate || null,
        country: formBasic.country?.trim() || null,
        synopsis: formBasic.synopsis?.trim() || null,
      };

      if (selectedMovie) {
        // Edit payload should NEVER contain status (prevent lifecycle bypass)
        await adminMovieService.updateMovie(selectedMovie.publicId, moviePayload);
        triggerToast?.('Cập nhật thông tin cơ bản thành công!');
        await onRefreshList?.();
        onClose();
      } else {
        // Create payload no longer needs status since backend defaults to DRAFT
        const res = await adminMovieService.createMovie(moviePayload);
        const publicId = res?.data?.publicId || res?.publicId;
        if (!publicId) throw new Error('Không nhận được mã phim từ server. Vui lòng kiểm tra lại.');

        triggerToast?.('Tạo phim thành công! Chuyển đến trang chi tiết để thêm các thông tin khác.');
        await onRefreshList?.();
        onClose();
        navigate(`/admin/movies/${publicId}${detailQuery}`);
      }
    } catch (err) {
      console.error("Failed to save movie:", err);
      const d = err?.response?.data || err;
      if (d && d.errorCode === 'VALIDATION_ERROR' && d.data?.fieldErrors) {
        const errs = {};
        d.data.fieldErrors.forEach(errItem => {
          let fieldKey = errItem.field;
          if (fieldKey === 'releaseDate') fieldKey = 'showingStartDate';
          errs[fieldKey] = errItem.message;
        });
        setFormErrors(errs);
        triggerToast?.('Một số thông tin nhập chưa đúng, vui lòng kiểm tra lại.', 'error');
      } else {
        triggerToast?.(parseApiError(err), 'error');
      }
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
      <div className="bg-[#050506] border border-zinc-800 rounded-2xl w-full max-w-4xl max-h-[90vh] overflow-hidden flex flex-col shadow-2xl">
        <div className="flex flex-col flex-1 p-6 md:p-8 overflow-y-auto custom-scrollbar space-y-5 text-zinc-100">
      <div className="flex justify-between items-center border-b border-zinc-800 pb-4 flex-shrink-0">
        <div className="flex items-center gap-3">
          <button type="button" onClick={onClose} className="p-2 text-zinc-400 hover:text-white bg-zinc-900 border border-zinc-800 rounded-xl transition-all cursor-pointer">
            <ArrowLeft className="w-4 h-4" />
          </button>
          <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider">
            {isEdit ? 'CẬP NHẬT THÔNG TIN PHIM' : 'TẠO PHIM THỦ CÔNG'}
          </h1>
        </div>
        <div className="flex items-center gap-3">
          <button type="button" onClick={onClose}
            className="border border-zinc-850 bg-zinc-900/60 hover:bg-zinc-800 text-zinc-305 font-bold py-2 px-5 rounded-xl text-xs transition-colors cursor-pointer">
            Hủy
          </button>
          <button type="button" onClick={handleSave} disabled={isSaving}
            className="bg-[#ff7a1a] hover:opacity-90 text-zinc-950 font-black py-2 px-6 rounded-xl text-xs uppercase tracking-wider transition-all shadow-lg flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50">
            {isSaving ? (
              <div className="w-4 h-4 border-2 border-zinc-950 border-t-transparent rounded-full animate-spin" />
            ) : (
              <><Check className="w-4 h-4" /><span>LƯU LẠI</span></>
            )}
          </button>
        </div>
      </div>

      <form onSubmit={e => e.preventDefault()} className="pb-16 max-w-4xl mx-auto w-full">
        <FormSection icon={<Film className="w-4 h-4 text-[#ff7a1a]" />} title="Thông Tin Cơ Bản">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Field label="Tên phim" required error={formErrors.title}>
              <Input value={formBasic.title} onChange={e => setFormBasic(p => ({ ...p, title: e.target.value }))} />
            </Field>
            <Field label="Tên gốc (Nguyên bản)">
              <Input value={formBasic.originalTitle} onChange={e => setFormBasic(p => ({ ...p, originalTitle: e.target.value }))} />
            </Field>
            <Field label="Thời lượng (phút)" required error={formErrors.durationMinutes}>
              <Input type="number" min="1" value={formBasic.durationMinutes} onChange={e => setFormBasic(p => ({ ...p, durationMinutes: e.target.value }))} />
            </Field>
            <Field label="Quốc gia sản xuất">
              <Input value={formBasic.country} onChange={e => setFormBasic(p => ({ ...p, country: e.target.value }))} placeholder="Vd: United States of America" />
            </Field>
            <Field label="Giới hạn độ tuổi" required error={formErrors.ageRating}>
              <Select value={formBasic.ageRating} onChange={e => setFormBasic(p => ({ ...p, ageRating: e.target.value }))}>
                {AGE_RATINGS.map(r => <option key={r} value={r}>{AGE_RATING_LABELS[r]}</option>)}
              </Select>
            </Field>


          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
            <Field label="Ngày khởi chiếu" required error={formErrors.showingStartDate}>
              <Input type="date" value={formBasic.showingStartDate} onChange={e => setFormBasic(p => ({ ...p, showingStartDate: e.target.value }))} />
            </Field>
            <Field label="Ngày ngừng chiếu" error={formErrors.endDate}>
              <Input type="date" value={formBasic.endDate} onChange={e => setFormBasic(p => ({ ...p, endDate: e.target.value }))} />
            </Field>
          </div>

          <div className="mt-4">
            <Field label="Nội dung tóm tắt">
              <Textarea rows={5} value={formBasic.synopsis} onChange={e => setFormBasic(p => ({ ...p, synopsis: e.target.value }))} />
            </Field>
          </div>

          {!isEdit && (
            <div className="mt-4 flex items-center gap-2 bg-blue-950/30 border border-blue-900/30 rounded-xl p-3 text-[11px] text-blue-300 leading-relaxed">
              <Info className="w-5 h-5 flex-shrink-0 text-blue-400" />
              <span>
                <strong>Lưu ý:</strong> Phim từ TMDB được hệ thống tự động đồng bộ và tạo ở trạng thái Chờ duyệt.
                Chỉ sử dụng biểu mẫu này để tạo thủ công cho các nội dung đặc biệt hoặc phim không có trong nguồn đồng bộ.
                Phim mới tạo sẽ mặc định ở trạng thái Nháp (DRAFT).
              </span>
            </div>
          )}
        </FormSection>
      </form>
        </div>
      </div>
    </div>
  );
}
