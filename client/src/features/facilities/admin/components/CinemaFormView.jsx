import { useState } from 'react';
import { ArrowLeft, Info, MapPin, Save } from 'lucide-react';
import CinemaLocationForm from './CinemaLocationForm';

export default function CinemaFormView({ onCancel, onSubmit, triggerToast }) {
  const [formData, setFormData] = useState({
    name: '',
    city: '',
    district: '',
    address: '',
    latitude: 10.7741,
    longitude: 106.6934,
    timezone: 'Asia/Ho_Chi_Minh',
  });
  const [addressSearch, setAddressSearch] = useState('');
  const [formErrors, setFormErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validate = () => {
    const errors = {};
    if (!formData.name.trim()) errors.name = 'Vui lòng nhập tên cụm rạp';
    if (!formData.city.trim()) errors.city = 'Vui lòng nhập tỉnh hoặc thành phố';
    if (!formData.district.trim()) errors.district = 'Vui lòng nhập quận hoặc huyện';
    if (!formData.address.trim()) errors.address = 'Vui lòng nhập địa chỉ';
    if (!formData.timezone.trim()) errors.timezone = 'Vui lòng xác nhận múi giờ';
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const submit = async event => {
    event.preventDefault();
    if (!validate()) {
      triggerToast?.('Hãy hoàn thiện dữ liệu tối thiểu để tạo bản nháp.', 'error');
      return;
    }
    setIsSubmitting(true);
    try {
      await onSubmit(formData);
    } catch (error) {
      triggerToast?.(error.message || 'Không thể tạo bản nháp cụm rạp.', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form
      onSubmit={submit}
      className="min-h-screen flex-1 overflow-auto bg-zinc-950 p-6 pb-28 text-zinc-100 md:p-8"
    >
      <header className="mb-6 flex items-center gap-4 border-b border-zinc-900 pb-5">
        <button
          type="button"
          onClick={onCancel}
          className="flex items-center gap-2 rounded-xl border border-zinc-800 bg-zinc-900 px-3 py-2 text-xs font-bold text-zinc-300 hover:bg-zinc-800"
        >
          <ArrowLeft className="h-4 w-4" />
          Danh sách cụm rạp
        </button>
        <div>
          <h1 className="text-2xl font-black uppercase tracking-wider">Tạo bản nháp cụm rạp</h1>
          <p className="mt-1 text-xs text-zinc-500">
            Chỉ tạo hồ sơ tối thiểu; giờ hoạt động, hình ảnh và phòng chiếu được hoàn thiện tại Trung tâm thiết lập.
          </p>
        </div>
      </header>

      <div className="mx-auto max-w-5xl space-y-6">
        <div className="flex items-start gap-3 rounded-2xl border border-sky-500/20 bg-sky-500/5 p-4 text-xs leading-5 text-sky-200">
          <Info className="mt-0.5 h-4 w-4 shrink-0" />
          Bản nháp chưa được đưa vào vận hành, chưa công khai cho khách và không tự mở bán bất kỳ suất chiếu nào.
        </div>

        <section className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-6">
          <div className="mb-4 flex items-center gap-2 border-b border-zinc-800 pb-3">
            <MapPin className="h-4 w-4 text-orange-500" />
            <h2 className="text-sm font-bold uppercase tracking-wider text-white">Hồ sơ tối thiểu</h2>
          </div>
          <label className="flex flex-col gap-1.5">
            <span className="text-[10px] font-black uppercase tracking-wider text-zinc-500">
              Tên cụm rạp <span className="text-rose-500">*</span>
            </span>
            <input
              type="text"
              value={formData.name}
              onChange={event => setFormData({ ...formData, name: event.target.value })}
              placeholder="Ví dụ: LoraFilm Sense City Cần Thơ"
              className={`rounded-xl border bg-zinc-950 px-3.5 py-3 text-sm text-zinc-100 outline-none ${
                formErrors.name ? 'border-red-500' : 'border-zinc-800 focus:border-orange-500/40'
              }`}
            />
            {formErrors.name && <span className="text-[10px] text-red-500">{formErrors.name}</span>}
          </label>
        </section>

        <CinemaLocationForm
          formData={formData}
          setFormData={setFormData}
          formErrors={formErrors}
          addressSearch={addressSearch}
          setAddressSearch={setAddressSearch}
        />

        <section className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-6">
          <label className="flex flex-col gap-1.5">
            <span className="text-[10px] font-black uppercase tracking-wider text-zinc-500">
              Múi giờ vận hành <span className="text-rose-500">*</span>
            </span>
            <input
              type="text"
              value={formData.timezone}
              onChange={event => setFormData({ ...formData, timezone: event.target.value })}
              placeholder="Asia/Ho_Chi_Minh"
              className={`rounded-xl border bg-zinc-950 px-3.5 py-3 text-sm text-zinc-100 outline-none ${
                formErrors.timezone ? 'border-red-500' : 'border-zinc-800 focus:border-orange-500/40'
              }`}
            />
            <span className="text-[10px] leading-4 text-zinc-500">
              Dùng tên IANA; dữ liệu này quyết định ngày phục vụ và giờ suất chiếu.
            </span>
          </label>
        </section>
      </div>

      <div className="fixed bottom-0 left-0 right-0 z-30 flex items-center justify-between border-t border-zinc-900 bg-zinc-950/95 px-6 py-4 backdrop-blur lg:pl-80">
        <button
          type="button"
          onClick={onCancel}
          className="rounded-xl border border-zinc-800 px-5 py-3 text-xs font-bold text-zinc-300 hover:bg-zinc-900"
        >
          Hủy
        </button>
        <button
          type="submit"
          disabled={isSubmitting}
          className="flex items-center gap-2 rounded-xl bg-orange-500 px-6 py-3 text-xs font-black uppercase text-zinc-950 disabled:opacity-50"
        >
          <Save className="h-4 w-4" />
          {isSubmitting ? 'Đang tạo...' : 'Tạo bản nháp và tiếp tục thiết lập'}
        </button>
      </div>
    </form>
  );
}
