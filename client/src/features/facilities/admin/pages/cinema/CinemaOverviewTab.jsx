import { useState, useEffect } from 'react';
import { Save } from 'lucide-react';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';

export default function CinemaOverviewTab({ cinema, onUpdate }) {
  const [formData, setFormData] = useState({
    name: '',
    city: '',
    district: '',
    address: '',
    latitude: '',
    longitude: '',
    timezone: '',
    hotline: '',
    description: '',
    status: 'ACTIVE'
  });
  
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [addressSuggestions, setAddressSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [isSearchingAddress, setIsSearchingAddress] = useState(false);

  useEffect(() => {
    if (cinema) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setFormData({
        name: cinema.name || '',
        city: cinema.city || '',
        district: cinema.district || '',
        address: cinema.address || '',
        latitude: cinema.latitude || '',
        longitude: cinema.longitude || '',
        timezone: cinema.timezone || '',
        hotline: cinema.hotline || '',
        description: cinema.description || '',
        status: cinema.status || 'ACTIVE'
      });
    }
  }, [cinema]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    
    // Normalize numeric fields
    const payload = {
      ...formData,
      latitude: formData.latitude ? parseFloat(formData.latitude) : null,
      longitude: formData.longitude ? parseFloat(formData.longitude) : null,
    };
    
    await onUpdate(payload);
    setIsSubmitting(false);
  };

  // Basic Address Suggestion Debouncer (Phase 4 Ready)
  useEffect(() => {
    const timer = setTimeout(async () => {
      if (formData.address && formData.address.length > 3 && showSuggestions) {
        setIsSearchingAddress(true);
        try {
          const res = await adminCinemaService.suggestAddress(formData.address);
          if (res?.success && res.data) {
            setAddressSuggestions(res.data);
          }
        } catch (err) {
          console.error("Failed to suggest address", err);
        } finally {
          setIsSearchingAddress(false);
        }
      }
    }, 500);
    return () => clearTimeout(timer);
  }, [formData.address, showSuggestions]);

  const selectAddress = (addr) => {
    setFormData(prev => ({
      ...prev,
      address: addr.displayName,
      city: addr.city || prev.city,
      district: addr.district || prev.district,
      latitude: addr.lat,
      longitude: addr.lon
    }));
    setShowSuggestions(false);
  };

  return (
    <div className="max-w-4xl space-y-6 pb-20">
      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="bg-zinc-900/30 border border-zinc-800 rounded-2xl p-6">
          <h2 className="text-sm font-black text-brand-coral uppercase tracking-wider mb-6">Thông Tin Cơ Bản</h2>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="md:col-span-2">
              <label className="block text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-2">
                Tên Cụm Rạp <span className="text-brand-coral">*</span>
              </label>
              <input
                type="text"
                name="name"
                required
                value={formData.name}
                onChange={handleChange}
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:border-brand-coral outline-none transition-colors"
                placeholder="Ví dụ: LoraFilm Ho Chi Minh"
              />
            </div>

            <div className="md:col-span-2 relative">
              <label className="block text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-2">
                Địa Chỉ Chi Tiết <span className="text-brand-coral">*</span>
              </label>
              <input
                type="text"
                name="address"
                required
                value={formData.address}
                onChange={(e) => {
                  handleChange(e);
                  setShowSuggestions(true);
                }}
                onFocus={() => setShowSuggestions(true)}
                onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:border-brand-coral outline-none transition-colors"
                placeholder="Nhập địa chỉ..."
              />
              {/* Autocomplete Dropdown (Phase 4 Ready) */}
              {showSuggestions && (addressSuggestions.length > 0 || isSearchingAddress) && (
                <div className="absolute z-50 mt-1 w-full bg-zinc-900 border border-zinc-800 rounded-xl shadow-2xl max-h-60 overflow-y-auto">
                  {isSearchingAddress ? (
                    <div className="p-4 text-xs text-zinc-500 text-center">Đang tìm kiếm...</div>
                  ) : (
                    addressSuggestions.map((s, i) => (
                      <div 
                        key={i} 
                        onClick={() => selectAddress(s)}
                        className="p-3 hover:bg-zinc-800 cursor-pointer text-xs text-zinc-300 border-b border-zinc-800 last:border-0"
                      >
                        {s.displayName}
                      </div>
                    ))
                  )}
                </div>
              )}
            </div>

            <div>
              <label className="block text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-2">
                Tỉnh / Thành Phố <span className="text-brand-coral">*</span>
              </label>
              <input
                type="text"
                name="city"
                required
                value={formData.city}
                onChange={handleChange}
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:border-brand-coral outline-none transition-colors"
              />
            </div>

            <div>
              <label className="block text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-2">
                Quận / Huyện
              </label>
              <input
                type="text"
                name="district"
                value={formData.district}
                onChange={handleChange}
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:border-brand-coral outline-none transition-colors"
              />
            </div>

            <div>
              <label className="block text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-2">
                Vĩ Độ (Latitude)
              </label>
              <input
                type="number"
                step="any"
                name="latitude"
                value={formData.latitude}
                onChange={handleChange}
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:border-brand-coral outline-none transition-colors font-mono"
              />
            </div>

            <div>
              <label className="block text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-2">
                Kinh Độ (Longitude)
              </label>
              <input
                type="number"
                step="any"
                name="longitude"
                value={formData.longitude}
                onChange={handleChange}
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:border-brand-coral outline-none transition-colors font-mono"
              />
            </div>
            
            <div>
              <label className="block text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-2">
                Múi Giờ <span className="text-brand-coral">*</span>
              </label>
              <input
                type="text"
                name="timezone"
                required
                value={formData.timezone}
                onChange={handleChange}
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:border-brand-coral outline-none transition-colors font-mono"
                placeholder="Asia/Ho_Chi_Minh"
              />
            </div>

            <div>
              <label className="block text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-2">
                Hotline
              </label>
              <input
                type="text"
                name="hotline"
                value={formData.hotline}
                onChange={handleChange}
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:border-brand-coral outline-none transition-colors"
              />
            </div>

            <div className="md:col-span-2">
              <label className="block text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-2">
                Mô tả
              </label>
              <textarea
                name="description"
                value={formData.description}
                onChange={handleChange}
                rows={4}
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:border-brand-coral outline-none transition-colors"
              />
            </div>
          </div>
        </div>

        <div className="bg-zinc-900/30 border border-zinc-800 rounded-2xl p-6">
          <h2 className="text-sm font-black text-brand-coral uppercase tracking-wider mb-6">Trạng Thái</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-2">
                Trạng Thái Hoạt Động
              </label>
              <select
                name="status"
                value={formData.status}
                onChange={handleChange}
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:border-brand-coral outline-none transition-colors cursor-pointer"
              >
                <option value="ACTIVE">Đang Hoạt Động</option>
                <option value="DRAFT">Bản Nháp (Draft)</option>
                <option value="MAINTENANCE">Đang Bảo Trì</option>
                <option value="INACTIVE">Ngưng Hoạt Động</option>
              </select>
            </div>
          </div>
        </div>

        <div className="flex justify-end pt-4">
          <button
            type="submit"
            disabled={isSubmitting}
            className="flex items-center gap-2 bg-brand-coral hover:bg-opacity-90 text-white px-8 py-3 rounded-xl font-bold uppercase tracking-wider text-xs transition-colors shadow-lg shadow-brand-coral/20 disabled:opacity-50"
          >
            <Save className="w-4 h-4" />
            {isSubmitting ? 'ĐANG LƯU...' : 'LƯU THAY ĐỔI'}
          </button>
        </div>
      </form>
    </div>
  );
}
