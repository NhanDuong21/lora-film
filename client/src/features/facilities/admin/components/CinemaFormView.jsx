// eslint-disable-next-line no-unused-vars
import React, { useState } from 'react';
import { ChevronLeft } from 'lucide-react';
import CinemaBasicInfo from './CinemaBasicInfo';
import CinemaLocationForm from './CinemaLocationForm';
import CinemaOperatingHours from './CinemaOperatingHours';
import CinemaMediaForm from './CinemaMediaForm';

export default function CinemaFormView({ onCancel, onSubmit, triggerToast }) {
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
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Media states
  const [bannerUrl, setBannerUrl] = useState('');
  const [galleryUrls, setGalleryUrls] = useState(['', '', '', '', '']); // Default 5 images
  const [mapImageUrl, setMapImageUrl] = useState('');

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
        bannerUrl,
        galleryUrls: galleryUrls.filter(url => {
          if (!url) return false;
          if (typeof url === 'string') return url.trim().length > 0;
          return true;
        }),
        mapImageUrl
      });
    } catch (err) {
      triggerToast?.(err.message || 'Không thể tạo cụm rạp', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-screen bg-zinc-950 text-zinc-100 gap-6 animate-fade-in">
      
      {/* Form Page Header */}
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
            <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">THÊM MỚI CỤM RẠP</h1>
            <p className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider mt-0.5">Điền các thông tin vị trí và vận hành của cụm rạp</p>
          </div>
        </div>
      </div>

      {/* Main form */}
      <form onSubmit={handleSubmit} className="w-full flex flex-col gap-6 pb-24">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
          
          {/* Left Fields Section (2 Cols) */}
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
              bannerUrl={bannerUrl}
              setBannerUrl={setBannerUrl}
              galleryUrls={galleryUrls}
              setGalleryUrls={setGalleryUrls}
              mapImageUrl={mapImageUrl}
              setMapImageUrl={setMapImageUrl}
            />
          </div>

          {/* Right Sidebar Section (1 Col) */}
          <div className="flex flex-col gap-6">
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
            {isSubmitting ? 'ĐANG TẠO...' : 'TẠO CỤM RẠP'}
          </button>
        </div>
      </form>
    </div>
  );
}
