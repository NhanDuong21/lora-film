import React, { useState, useEffect } from 'react';
import { ChevronLeft, ShieldAlert } from 'lucide-react';
import CinemaBasicInfo from './CinemaBasicInfo';
import CinemaLocationForm from './CinemaLocationForm';
import CinemaOperatingHours from './CinemaOperatingHours';
import CinemaMediaForm from './CinemaMediaForm';
import CinemaClosurePeriodsCard from './CinemaClosurePeriodsCard';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';

export default function CinemaEditView({ cinemaPublicId, onCancel, onSubmit, triggerToast }) {
  const [isLoadingDetail, setIsLoadingDetail] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

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
    status: 'DRAFT'
  });

  const [operatingHours, setOperatingHours] = useState([
    { dayOfWeek: 1, openTime: '08:00', closeTime: '23:00', isClosed: false },
    { dayOfWeek: 2, openTime: '08:00', closeTime: '23:00', isClosed: false },
    { dayOfWeek: 3, openTime: '08:00', closeTime: '23:00', isClosed: false },
    { dayOfWeek: 4, openTime: '08:00', closeTime: '23:00', isClosed: false },
    { dayOfWeek: 5, openTime: '08:00', closeTime: '23:00', isClosed: false },
    { dayOfWeek: 6, openTime: '08:00', closeTime: '23:00', isClosed: false },
    { dayOfWeek: 7, openTime: '08:00', closeTime: '23:00', isClosed: false }
  ]);

  const [addressSearch, setAddressSearch] = useState('');
  const [formErrors, setFormErrors] = useState({});

  // Media states
  const [logoUrl, setLogoUrl] = useState('');
  const [bannerUrl, setBannerUrl] = useState('');
  const [galleryUrls, setGalleryUrls] = useState(['', '', '', '', '']);
  const [mapImageUrl, setMapImageUrl] = useState('');

  // Keep references to original media list to handle update/delete/create changes
  const [originalMedia, setOriginalMedia] = useState([]);

  // Emergency closure states
  const [closures, setClosures] = useState([]);
  const [newClosure, setNewClosure] = useState({
    startTime: '',
    endTime: '',
    reason: ''
  });

  // Load details on mount
  useEffect(() => {
    const fetchCinemaData = async () => {
      setIsLoadingDetail(true);
      try {
        const res = await adminCinemaService.getAdminCinemaDetail(cinemaPublicId);
        if (res?.success && res?.data) {
          const d = res.data;
          setFormData({
            name: d.name || '',
            city: d.city || '',
            district: d.district || '',
            address: d.address || '',
            latitude: d.latitude || 10.7741,
            longitude: d.longitude || 106.6934,
            timezone: d.timezone || 'Asia/Ho_Chi_Minh',
            hotline: d.hotline || '',
            description: d.description || '',
            status: d.status || 'DRAFT'
          });
          setAddressSearch(d.address || '');

          if (Array.isArray(d.operatingHours) && d.operatingHours.length > 0) {
            const mappedHours = [1, 2, 3, 4, 5, 6, 7].map(dayNum => {
              const found = d.operatingHours.find(h => h.dayOfWeek === dayNum);
              return {
                dayOfWeek: dayNum,
                openTime: (found && found.openTime) ? found.openTime.substring(0, 5) : '08:00',
                closeTime: (found && found.closeTime) ? found.closeTime.substring(0, 5) : '23:00',
                isClosed: found ? found.isClosed : false
              };
            });
            setOperatingHours(mappedHours);
          }

          if (Array.isArray(d.gallery)) {
            setOriginalMedia(d.gallery);

            const logoItem = d.gallery.find(m => m.mediaType === 'LOGO');
            const bannerItem = d.gallery.find(m => m.mediaType === 'BANNER');
            const mapItem = d.gallery.find(m => m.mediaType === 'MAP');
            const galleryItems = d.gallery.filter(m => m.mediaType === 'GALLERY');

            setLogoUrl(logoItem?.url || '');
            setBannerUrl(bannerItem?.url || '');
            setMapImageUrl(mapItem?.url || '');

            const mappedGallery = galleryItems.map(g => g.url);
            while (mappedGallery.length < 5) {
              mappedGallery.push('');
            }
            setGalleryUrls(mappedGallery);
          }
        }
      } catch (err) {
        triggerToast?.('Không thể tải thông tin chi tiết: ' + err.message, 'error');
      } finally {
        setIsLoadingDetail(false);
      }
    };

    fetchCinemaData();
    fetchClosures();
  }, [cinemaPublicId]);

  // Fetch closure periods
  const fetchClosures = async () => {
    try {
      const res = await adminCinemaService.getClosurePeriods(cinemaPublicId, { page: 0, size: 50 });
      if (res?.success && res?.data?.data) {
        setClosures(res.data.data);
      }
    } catch (err) {
      console.error('Failed to fetch closure periods:', err);
    }
  };

  // Handle operating hour toggle and inputs
  const handleOperatingHoursChange = (idx, field, value) => {
    setOperatingHours(prev => prev.map((oh, i) => {
      if (i !== idx) return oh;
      return { ...oh, [field]: value };
    }));
  };

  // Form validations
  const validateForm = () => {
    const errors = {};
    if (!formData.name.trim()) errors.name = 'Tên cụm rạp không được để trống';
    if (!formData.address.trim()) errors.address = 'Địa chỉ không được để trống';
    if (!formData.city.trim()) errors.city = 'Thành phố không được để trống';
    if (!formData.district.trim()) errors.district = 'Quận/Huyện không được để trống';

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Handle Form Submission
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) {
      triggerToast?.('Vui lòng kiểm tra lại các thông tin nhập vào', 'error');
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit(formData, operatingHours, {
        logoUrl,
        bannerUrl,
        galleryUrls: galleryUrls.filter(url => url && url.trim().length > 0),
        mapImageUrl,
        originalMedia
      });
    } catch (err) {
      triggerToast?.(err.message || 'Không thể cập nhật cụm rạp', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Handle Emergency Closure Submission
  const handleCreateClosure = async (e) => {
    e.preventDefault();

    if (formData.status !== 'ACTIVE') {
      triggerToast?.('Không thể lập lịch đóng cửa khẩn cấp! Cụm rạp phải ở trạng thái Đang hoạt động (ACTIVE) mới có thể thực hiện thao tác này.', 'error');
      return;
    }

    if (!newClosure.startTime || !newClosure.endTime || !newClosure.reason.trim()) {
      triggerToast?.('Vui lòng điền đầy đủ thời gian và lý do ngừng hoạt động', 'error');
      return;
    }

    try {
      let startDate = new Date(newClosure.startTime);
      const now = new Date();

      // If the selected start time is in the past or within 10 seconds of now,
      // adjust it to be exactly 15 seconds in the future to satisfy backend validation buffer.
      if (startDate.getTime() < now.getTime() + 10000) {
        startDate = new Date(now.getTime() + 15000);
      }

      const startInstant = startDate.toISOString();
      const endInstant = new Date(newClosure.endTime).toISOString();
      
      if (new Date(endInstant) <= new Date(startInstant)) {
        triggerToast?.('Thời gian kết thúc phải sau thời gian bắt đầu', 'error');
        return;
      }

      await adminCinemaService.createClosurePeriod(cinemaPublicId, {
        startTime: startInstant,
        endTime: endInstant,
        reason: newClosure.reason
      });

      // Automatically update cinema status to TEMPORARILY_CLOSED locally and in backend
      try {
        await adminCinemaService.updateCinemaStatus(cinemaPublicId, 'TEMPORARILY_CLOSED');
        setFormData(prev => ({
          ...prev,
          status: 'TEMPORARILY_CLOSED'
        }));
      } catch (statusErr) {
        console.error("Failed to auto-update status to TEMPORARILY_CLOSED:", statusErr);
      }

      triggerToast?.('Đã kích hoạt dừng rạp khẩn cấp và chuyển trạng thái rạp sang Tạm thời đóng cửa!');
      setNewClosure({ startTime: '', endTime: '', reason: '' });
      fetchClosures();
    } catch (err) {
      triggerToast?.('Không thể lưu đợt dừng hoạt động: ' + err.message, 'error');
    }
  };

  // Handle Cancel Closure
  const handleCancelClosure = async (closureId) => {
    try {
      await adminCinemaService.cancelClosurePeriod(closureId);

      // Automatically update cinema status back to ACTIVE locally and in backend
      try {
        await adminCinemaService.updateCinemaStatus(cinemaPublicId, 'ACTIVE');
        setFormData(prev => ({
          ...prev,
          status: 'ACTIVE'
        }));
      } catch (statusErr) {
        console.error("Failed to restore status to ACTIVE:", statusErr);
      }

      triggerToast?.('Đã khôi phục hoạt động rạp thành công!');
      fetchClosures();
    } catch (err) {
      triggerToast?.('Không thể khôi phục rạp: ' + err.message, 'error');
    }
  };

  if (isLoadingDetail) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-[#050506] text-white">
        <div className="flex flex-col items-center gap-4">
          <div className="w-12 h-12 border-4 border-orange-500 border-t-transparent rounded-full animate-spin" />
          <p className="text-sm font-semibold tracking-wider text-zinc-400">Đang tải thông tin chi tiết cụm rạp...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-screen bg-zinc-950 text-zinc-100 gap-6 animate-fade-in">
      
      {/* Page Header */}
      <div className="flex items-center justify-between border-b border-zinc-900 pb-4">
        <div className="flex items-center gap-3">
          <button
            onClick={onCancel}
            type="button"
            className="p-2 bg-zinc-900 hover:bg-zinc-800 border border-zinc-800/80 rounded-xl transition-all cursor-pointer text-zinc-400 hover:text-white"
            title="Quay lại"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">CHỈNH SỬA CỤM RẠP</h1>
            <p className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider mt-0.5">Cập nhật thông tin chi tiết, giờ hoạt động và sự cố khẩn cấp</p>
          </div>
        </div>
      </div>

      {/* Main form */}
      <form onSubmit={handleSubmit} className="w-full flex flex-col gap-6 pb-24">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
          
          {/* Left Fields Column (2 Cols) */}
          <div className="lg:col-span-2 flex flex-col gap-6">
            <CinemaBasicInfo
              formData={formData}
              setFormData={setFormData}
              formErrors={formErrors}
            />

            <CinemaLocationForm
              formData={formData}
              setFormData={setFormData}
              formErrors={formErrors}
              addressSearch={addressSearch}
              setAddressSearch={setAddressSearch}
            />

            <CinemaMediaForm
              logoUrl={logoUrl}
              setLogoUrl={setLogoUrl}
              bannerUrl={bannerUrl}
              setBannerUrl={setBannerUrl}
              galleryUrls={galleryUrls}
              setGalleryUrls={setGalleryUrls}
              mapImageUrl={mapImageUrl}
              setMapImageUrl={setMapImageUrl}
            />

            <CinemaClosurePeriodsCard
              closures={closures}
              newClosure={newClosure}
              setNewClosure={setNewClosure}
              onCreateClosure={handleCreateClosure}
              onCancelClosure={handleCancelClosure}
            />
          </div>

          {/* Right Sidebar Column (1 Col) */}
          <div className="flex flex-col gap-6">
            
            {/* Card: Status control */}
            <div className="bg-zinc-900/60 border border-zinc-800/80 rounded-2xl p-6 flex flex-col gap-4">
              <div className="flex items-center gap-2 border-b border-zinc-800/80 pb-3">
                <ShieldAlert className="w-4 h-4 text-orange-500" />
                <h2 className="text-sm font-bold uppercase tracking-wider text-white">Trạng Thái Vận Hành</h2>
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Trạng thái rạp</label>
                <select
                  value={formData.status}
                  onChange={e => setFormData({ ...formData, status: e.target.value })}
                  className="w-full bg-zinc-950 border border-zinc-800 focus:border-orange-500/40 rounded-xl py-2.5 px-3 text-xs text-zinc-100 focus:outline-none transition-colors cursor-pointer"
                >
                  <option value="DRAFT">Nháp / Chờ duyệt (DRAFT)</option>
                  <option value="ACTIVE">Đang Hoạt Động (ACTIVE)</option>
                  <option value="MAINTENANCE">Đang Bảo Trì (MAINTENANCE)</option>
                  <option value="TEMPORARILY_CLOSED">Tạm Thời Đóng Cửa (TEMPORARILY_CLOSED)</option>
                  <option value="INACTIVE">Ngưng Hoạt Động (INACTIVE)</option>
                  <option value="PERMANENTLY_CLOSED">Đóng Cửa Vĩnh Viễn (PERMANENTLY_CLOSED)</option>
                </select>
              </div>
            </div>

            <CinemaOperatingHours
              operatingHours={operatingHours}
              onHoursChange={handleOperatingHoursChange}
            />
          </div>

        </div>

        {/* Floating bottom action bar */}
        <div className="fixed bottom-0 left-0 right-0 z-30 bg-zinc-950/80 backdrop-blur-md border-t border-zinc-900 py-4 px-6 lg:pl-72 flex justify-between items-center shadow-xl">
          <button
            type="button"
            onClick={onCancel}
            className="border border-zinc-800 hover:bg-zinc-900 text-zinc-300 font-bold px-6 py-3 rounded-xl text-xs uppercase tracking-wider transition-colors cursor-pointer"
          >
            Hủy Bỏ
          </button>
          
          <button
            type="submit"
            disabled={isSubmitting}
            className="bg-brand-orange hover:bg-opacity-95 text-zinc-950 font-black px-8 py-3 rounded-xl text-xs uppercase tracking-wider transition-all duration-300 shadow-lg shadow-brand-orange/15 disabled:opacity-50 flex items-center gap-2 cursor-pointer"
          >
            {isSubmitting ? 'ĐANG LƯU...' : 'LƯU THAY ĐỔI'}
          </button>
        </div>
      </form>
    </div>
  );
}
