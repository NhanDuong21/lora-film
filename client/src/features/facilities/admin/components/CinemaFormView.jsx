import { useMemo, useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  Check,
  CheckCircle2,
  Image as ImageIcon,
  Info,
  MapPin,
  Save,
  Timer,
} from 'lucide-react';
import CinemaBasicInfo from './CinemaBasicInfo';
import CinemaLocationForm from './CinemaLocationForm';
import CinemaOperatingHours from './CinemaOperatingHours';
import CinemaMediaForm from './CinemaMediaForm';

const STEPS = [
  { id: 'information', label: 'Thông tin & vị trí', icon: MapPin },
  { id: 'hours', label: 'Giờ hoạt động', icon: Timer },
  { id: 'media', label: 'Hình ảnh', icon: ImageIcon },
  { id: 'review', label: 'Kiểm tra & tạo', icon: CheckCircle2 },
];

export default function CinemaFormView({ onCancel, onSubmit, triggerToast }) {
  const [currentStep, setCurrentStep] = useState(0);
  const [formData, setFormData] = useState({
    name: '',
    city: '',
    district: '',
    address: '',
    latitude: 10.7741,
    longitude: 106.6934,
    timezone: 'Asia/Ho_Chi_Minh',
    hotline: '',
    description: '',
    status: 'DRAFT',
  });
  const [operatingHours, setOperatingHours] = useState(
    Array.from({ length: 7 }, (_, index) => ({
      dayOfWeek: index + 1,
      openTime: '08:00',
      closeTime: '23:00',
      isClosed: false,
    })),
  );
  const [addressSearch, setAddressSearch] = useState('');
  const [formErrors, setFormErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [bannerUrl, setBannerUrl] = useState('');
  const [galleryUrls, setGalleryUrls] = useState(['', '', '', '', '']);
  const [mapImageUrl, setMapImageUrl] = useState('');

  const activeDays = operatingHours.filter((hours) => !hours.isClosed).length;
  const mediaCount = [bannerUrl, mapImageUrl, ...galleryUrls].filter(Boolean).length;

  const checklist = useMemo(() => [
    { label: 'Tên cụm rạp', complete: Boolean(formData.name.trim()) },
    {
      label: 'Địa chỉ phục vụ khách hàng',
      complete: Boolean(formData.address.trim() && formData.city.trim() && formData.district.trim()),
    },
    { label: 'Có ít nhất một ngày mở cửa', complete: activeDays > 0 },
    { label: 'Hình ảnh nhận diện (có thể bổ sung sau)', complete: mediaCount > 0, optional: true },
  ], [activeDays, formData, mediaCount]);

  const validateInformation = () => {
    const errors = {};
    if (!formData.name.trim()) errors.name = 'Vui lòng nhập tên cụm rạp';
    if (!formData.address.trim()) errors.address = 'Vui lòng nhập địa chỉ';
    if (!formData.city.trim()) errors.city = 'Vui lòng nhập tỉnh hoặc thành phố';
    if (!formData.district.trim()) errors.district = 'Vui lòng nhập quận hoặc huyện';
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleHoursChange = (index, field, value) => {
    setOperatingHours((previous) =>
      previous.map((hours, currentIndex) =>
        currentIndex === index ? { ...hours, [field]: value } : hours,
      ),
    );
  };

  const goNext = () => {
    if (currentStep === 0 && !validateInformation()) {
      triggerToast?.('Vui lòng hoàn thiện thông tin bắt buộc trước khi tiếp tục.', 'error');
      return;
    }
    if (currentStep === 1 && activeDays === 0) {
      triggerToast?.('Cụm rạp cần có ít nhất một ngày mở cửa trong tuần.', 'error');
      return;
    }
    setCurrentStep((step) => Math.min(step + 1, STEPS.length - 1));
  };

  const handleSubmit = async () => {
    if (!validateInformation() || activeDays === 0) {
      triggerToast?.('Thông tin cụm rạp chưa hoàn chỉnh.', 'error');
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit(formData, operatingHours, {
        bannerUrl,
        galleryUrls: galleryUrls.filter(Boolean),
        mapImageUrl,
      });
    } catch (error) {
      triggerToast?.(error.message || 'Không thể tạo cụm rạp.', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex-1 overflow-auto bg-zinc-950 p-6 pb-28 text-zinc-100 md:p-8">
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
          <h1 className="text-2xl font-black uppercase tracking-wider">Thiết lập cụm rạp mới</h1>
          <p className="mt-1 text-xs text-zinc-500">
            Thực hiện từng bước; cụm rạp được tạo ở trạng thái bản nháp để bạn kiểm tra trước khi mở bán.
          </p>
        </div>
      </header>

      <div className="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
        {STEPS.map((step, index) => {
          const Icon = step.icon;
          const active = index === currentStep;
          const complete = index < currentStep;
          return (
            <button
              key={step.id}
              type="button"
              onClick={() => index <= currentStep && setCurrentStep(index)}
              className={`flex items-center gap-3 rounded-2xl border p-4 text-left ${
                active
                  ? 'border-orange-500 bg-orange-500/10 text-white'
                  : complete
                    ? 'border-emerald-500/30 bg-emerald-500/5 text-emerald-300'
                    : 'border-zinc-800 bg-zinc-900/40 text-zinc-500'
              }`}
            >
              <span className="flex h-8 w-8 items-center justify-center rounded-full bg-zinc-950">
                {complete ? <Check className="h-4 w-4" /> : <Icon className="h-4 w-4" />}
              </span>
              <span>
                <span className="block text-[10px] font-black uppercase">Bước {index + 1}</span>
                <span className="text-xs font-bold">{step.label}</span>
              </span>
            </button>
          );
        })}
      </div>

      <div className="mx-auto max-w-6xl">
        {currentStep === 0 && (
          <div className="space-y-6">
            <CinemaBasicInfo formData={formData} setFormData={setFormData} formErrors={formErrors} />
            <CinemaLocationForm
              formData={formData}
              setFormData={setFormData}
              formErrors={formErrors}
              addressSearch={addressSearch}
              setAddressSearch={setAddressSearch}
            />
          </div>
        )}
        {currentStep === 1 && (
          <CinemaOperatingHours
            operatingHours={operatingHours}
            onHoursChange={handleHoursChange}
          />
        )}
        {currentStep === 2 && (
          <div className="space-y-4">
            <div className="flex items-start gap-3 rounded-2xl border border-blue-500/20 bg-blue-500/5 p-4 text-xs text-blue-200">
              <Info className="mt-0.5 h-4 w-4 shrink-0" />
              Hình ảnh chưa bắt buộc ở bước tạo bản nháp. Bạn có thể bổ sung, sắp xếp và chọn ảnh chính trong trung tâm vận hành sau.
            </div>
            <CinemaMediaForm
              bannerUrl={bannerUrl}
              setBannerUrl={setBannerUrl}
              galleryUrls={galleryUrls}
              setGalleryUrls={setGalleryUrls}
              mapImageUrl={mapImageUrl}
              setMapImageUrl={setMapImageUrl}
            />
          </div>
        )}
        {currentStep === 3 && (
          <section className="rounded-2xl border border-zinc-800 bg-zinc-900/50 p-6">
            <h2 className="text-lg font-black uppercase">Kiểm tra trước khi tạo bản nháp</h2>
            <p className="mt-2 text-sm text-zinc-400">
              {formData.name || 'Cụm rạp chưa đặt tên'} · {formData.address || 'Chưa có địa chỉ'}
            </p>
            <div className="mt-6 grid gap-3 md:grid-cols-2">
              {checklist.map((item) => (
                <div
                  key={item.label}
                  className={`flex items-center gap-3 rounded-xl border p-4 ${
                    item.complete
                      ? 'border-emerald-500/20 bg-emerald-500/5 text-emerald-300'
                      : item.optional
                        ? 'border-zinc-800 bg-zinc-950 text-zinc-400'
                        : 'border-amber-500/20 bg-amber-500/5 text-amber-300'
                  }`}
                >
                  <CheckCircle2 className="h-5 w-5 shrink-0" />
                  <span className="text-xs font-bold">{item.label}</span>
                </div>
              ))}
            </div>
            <div className="mt-6 rounded-xl border border-zinc-800 bg-zinc-950 p-4 text-xs text-zinc-400">
              Sau khi tạo, hãy bổ sung phòng chiếu và sơ đồ ghế. Hệ thống chỉ nên đưa cụm rạp vào hoạt động khi checklist vận hành đã hoàn tất.
            </div>
          </section>
        )}
      </div>

      <div className="fixed bottom-0 left-0 right-0 z-30 flex items-center justify-between border-t border-zinc-900 bg-zinc-950/95 px-6 py-4 backdrop-blur lg:pl-80">
        <button
          type="button"
          onClick={() => (currentStep === 0 ? onCancel() : setCurrentStep((step) => step - 1))}
          className="flex items-center gap-2 rounded-xl border border-zinc-800 px-5 py-3 text-xs font-bold text-zinc-300 hover:bg-zinc-900"
        >
          <ArrowLeft className="h-4 w-4" />
          {currentStep === 0 ? 'Hủy thiết lập' : 'Quay lại'}
        </button>
        {currentStep < STEPS.length - 1 ? (
          <button
            type="button"
            onClick={goNext}
            className="flex items-center gap-2 rounded-xl bg-orange-500 px-6 py-3 text-xs font-black uppercase text-zinc-950"
          >
            Tiếp tục <ArrowRight className="h-4 w-4" />
          </button>
        ) : (
          <button
            type="button"
            disabled={isSubmitting}
            onClick={handleSubmit}
            className="flex items-center gap-2 rounded-xl bg-emerald-500 px-6 py-3 text-xs font-black uppercase text-zinc-950 disabled:opacity-50"
          >
            <Save className="h-4 w-4" />
            {isSubmitting ? 'Đang tạo...' : 'Tạo bản nháp cụm rạp'}
          </button>
        )}
      </div>
    </div>
  );
}
