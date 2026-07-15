import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useOutletContext } from 'react-router-dom';
import { 
  Search, 
  MapPin, 
  Phone, 
  Calendar, 
  FileText, 
  Trash2, 
  Plus, 
  Check, 
  X, 
  Map,
  Layers,
  ArrowLeft,
  Navigation
} from 'lucide-react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import adminCinemaService from '@/features/cinemas-rooms/services/adminCinemaService';
import SkeletonTable from '@/components/common/SkeletonTable';

export default function AdminCinemaPage() {
  const { triggerToast } = useOutletContext() || {};

  // View state: 'list' | 'create'
  const [view, setView] = useState('list');

  // List states
  const [cinemas, setCinemas] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [cityFilter, setCityFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  
  // Pagination
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Form states
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
    openedDate: '',
    closedDate: ''
  });

  const [formErrors, setFormErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Map & Autocomplete states
  const [addressSearch, setAddressSearch] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const mapContainerRef = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);

  // Available cities list for filters (extracted dynamically from loaded cinemas)
  const citiesList = useMemo(() => {
    const cities = cinemas.map(c => c.city).filter(Boolean);
    return [...new Set(cities)];
  }, [cinemas]);

  // Leaflet custom marker pin layout
  const markerIcon = useMemo(() => {
    return L.divIcon({
      className: 'custom-div-icon',
      html: `
        <div class="relative flex items-center justify-center">
          <div class="absolute w-8 h-8 rounded-full bg-orange-500/30 animate-ping"></div>
          <div class="relative w-8 h-8 rounded-full bg-gradient-to-tr from-orange-600 to-amber-400 border border-orange-500 flex items-center justify-center shadow-lg shadow-orange-500/20">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" class="text-zinc-950">
              <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/>
              <circle cx="12" cy="10" r="3"/>
            </svg>
          </div>
        </div>
      `,
      iconSize: [32, 32],
      iconAnchor: [16, 32]
    });
  }, []);

  // Fetch Cinemas List
  const fetchCinemas = useCallback(async () => {
    setIsLoading(true);
    try {
      const params = {
        page: currentPage,
        size: pageSize,
        sort: 'createdAt,desc'
      };
      if (searchTerm.trim()) params.keyword = searchTerm.trim();
      if (cityFilter) params.city = cityFilter;
      if (statusFilter) params.status = statusFilter;

      const res = await adminCinemaService.getCinemas(params);
      if (res?.success && res?.data) {
        setCinemas(res.data.data || []);
        setTotalPages(res.data.totalPages || 0);
        setTotalElements(res.data.totalElements || 0);
      }
    } catch (err) {
      triggerToast?.('Không thể tải danh sách cụm rạp', 'error');
    } finally {
      setIsLoading(false);
    }
  }, [currentPage, pageSize, searchTerm, cityFilter, statusFilter, triggerToast]);

  useEffect(() => {
    if (view === 'list') {
      fetchCinemas();
    }
  }, [fetchCinemas, view]);

  // Handle Search and Filter changes
  const handleSearch = (e) => {
    setSearchTerm(e.target.value);
    setCurrentPage(0);
  };

  const handleCityFilter = (e) => {
    setCityFilter(e.target.value);
    setCurrentPage(0);
  };

  const handleStatusFilter = (e) => {
    setStatusFilter(e.target.value);
    setCurrentPage(0);
  };

  // Autocomplete debouncing
  useEffect(() => {
    if (!addressSearch.trim() || view !== 'create') {
      setSuggestions([]);
      return;
    }

    const timer = setTimeout(async () => {
      try {
        const res = await adminCinemaService.suggestAddress(addressSearch);
        if (res?.success && Array.isArray(res?.data)) {
          setSuggestions(res.data);
        } else if (Array.isArray(res)) {
          setSuggestions(res);
        } else {
          setSuggestions([]);
        }
      } catch (err) {
        console.error('Error fetching suggestions:', err);
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [addressSearch, view]);

  // Handle coordinates click or drag map actions
  const handleCoordinatesChange = useCallback(async (lat, lon) => {
    setFormData(prev => ({
      ...prev,
      latitude: parseFloat(lat.toFixed(7)),
      longitude: parseFloat(lon.toFixed(7))
    }));

    try {
      const res = await adminCinemaService.reverseGeocode(lat, lon);
      if (res?.success && res?.data) {
        const addrData = res.data;
        setFormData(prev => ({
          ...prev,
          address: addrData.label || addrData.address || prev.address,
          city: addrData.city || prev.city || '',
          district: addrData.district || prev.district || '',
          timezone: addrData.timezone || prev.timezone || 'Asia/Ho_Chi_Minh'
        }));
        
        // Temporarily block suggestion search
        setAddressSearch(addrData.label || addrData.address || '');
      }
    } catch (err) {
      console.warn('Reverse geocode failed:', err);
    }
  }, []);

  // Initialize Map
  useEffect(() => {
    if (view !== 'create') {
      // Cleanup map instance on view switch
      if (mapRef.current) {
        mapRef.current.remove();
        mapRef.current = null;
        markerRef.current = null;
      }
      return;
    }

    if (mapContainerRef.current && !mapRef.current) {
      const initialLat = formData.latitude;
      const initialLon = formData.longitude;

      const map = L.map(mapContainerRef.current, {
        zoomControl: true,
        attributionControl: false
      }).setView([initialLat, initialLon], 13);

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);

      const marker = L.marker([initialLat, initialLon], {
        draggable: true,
        icon: markerIcon
      }).addTo(map);

      mapRef.current = map;
      markerRef.current = marker;

      // Event listener: Drag marker end
      marker.on('dragend', (e) => {
        const position = marker.getLatLng();
        handleCoordinatesChange(position.lat, position.lng);
      });

      // Event listener: Click anywhere on map
      map.on('click', (e) => {
        const { lat, lng } = e.latlng;
        marker.setLatLng(e.latlng);
        map.panTo(e.latlng);
        handleCoordinatesChange(lat, lng);
      });
    }
  }, [view, markerIcon, handleCoordinatesChange, formData.latitude, formData.longitude]);

  // Handle selected autocomplete suggestion
  const handleSelectSuggestion = async (s) => {
    setSuggestions([]);
    setAddressSearch(s.label);

    const lat = parseFloat(s.latitude);
    const lon = parseFloat(s.longitude);

    setFormData(prev => ({
      ...prev,
      latitude: lat,
      longitude: lon,
      address: s.label || prev.address,
      city: s.city || prev.city || ''
    }));

    if (mapRef.current && markerRef.current) {
      markerRef.current.setLatLng([lat, lon]);
      mapRef.current.setView([lat, lon], 16);
    }

    // Call reverse geocoding to fill remaining details (e.g. district, timezone)
    try {
      const res = await adminCinemaService.reverseGeocode(lat, lon);
      if (res?.success && res?.data) {
        const addrData = res.data;
        setFormData(prev => ({
          ...prev,
          city: addrData.city || s.city || prev.city || '',
          district: addrData.district || prev.district || '',
          timezone: addrData.timezone || prev.timezone || 'Asia/Ho_Chi_Minh',
          address: addrData.label || addrData.address || s.label || prev.address
        }));
      }
    } catch (err) {
      console.warn('Autocomplete reverse geocode failed:', err);
    }
  };

  // Open Create Mode
  const handleOpenCreate = () => {
    setFormData({
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
      openedDate: '',
      closedDate: ''
    });
    setAddressSearch('');
    setSuggestions([]);
    setFormErrors({});
    setView('create');
  };

  // Soft Delete Cinema
  const handleDeleteCinema = async (publicId, name) => {
    if (confirm(`Bạn có chắc chắn muốn xóa cụm rạp "${name}"?`)) {
      try {
        await adminCinemaService.deleteCinema(publicId);
        triggerToast?.('Đã xóa cụm rạp thành công!');
        fetchCinemas();
      } catch (err) {
        triggerToast?.(err.message || 'Lỗi khi xóa cụm rạp', 'error');
      }
    }
  };

  // Toggle/Update status in list row
  const handleStatusChange = async (publicId, newStatus) => {
    try {
      await adminCinemaService.updateCinemaStatus(publicId, newStatus);
      triggerToast?.('Cập nhật trạng thái cụm rạp thành công!');
      fetchCinemas();
    } catch (err) {
      triggerToast?.('Không thể cập nhật trạng thái', 'error');
    }
  };

  // Form validations
  const validateForm = () => {
    const errors = {};
    if (!formData.name.trim()) errors.name = 'Tên cụm rạp không được để trống';
    if (!formData.address.trim()) errors.address = 'Địa chỉ không được để trống';
    if (!formData.city.trim()) errors.city = 'Thành phố không được để trống';
    if (!formData.district.trim()) errors.district = 'Quận/Huyện không được để trống';
    
    if (formData.openedDate && formData.closedDate) {
      const opened = new Date(formData.openedDate);
      const closed = new Date(formData.closedDate);
      if (closed < opened) {
        errors.closedDate = 'Ngày đóng cửa phải sau hoặc bằng ngày mở cửa';
      }
    }
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
      // Backend create API sets status to DRAFT automatically
      const res = await adminCinemaService.createCinema({
        name: formData.name,
        city: formData.city,
        district: formData.district,
        address: formData.address,
        latitude: formData.latitude,
        longitude: formData.longitude,
        timezone: formData.timezone,
        hotline: formData.hotline || null,
        description: formData.description || null,
        openedDate: formData.openedDate || null,
        closedDate: formData.closedDate || null
      });

      if (res?.success && res?.data) {
        const createdCinema = res.data;
        
        // If user chose a status other than DRAFT, sync it now
        if (formData.status !== 'DRAFT') {
          await adminCinemaService.updateCinemaStatus(createdCinema.publicId, formData.status);
        }

        triggerToast?.('Tạo mới cụm rạp thành công!');
        setView('list');
      }
    } catch (err) {
      triggerToast?.(err.message || 'Lỗi khi tạo mới cụm rạp', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Render Status Badge styling
  const renderStatusBadge = (status) => {
    const config = {
      DRAFT: 'bg-zinc-800 text-zinc-400 border-zinc-700',
      ACTIVE: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
      MAINTENANCE: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
      TEMPORARILY_CLOSED: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
      INACTIVE: 'bg-zinc-800 text-zinc-500 border-zinc-700',
      PERMANENTLY_CLOSED: 'bg-red-500/10 text-red-500 border-red-500/20'
    };

    return (
      <span className={`px-2.5 py-1 text-[10px] font-black border rounded-full uppercase tracking-wider ${config[status] || config.DRAFT}`}>
        {status.replace('_', ' ')}
      </span>
    );
  };

  if (view === 'create') {
    return (
      <div className="w-full min-h-screen bg-zinc-950 text-zinc-100 p-6 md:p-8 animate-fade-in flex flex-col gap-6">
        
        {/* Form Page Header */}
        <div className="flex items-center justify-between border-b border-zinc-900 pb-4">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setView('list')}
              className="p-2.5 text-zinc-400 hover:text-white bg-zinc-900 border border-zinc-800/80 rounded-xl transition-all cursor-pointer"
              title="Quay lại danh sách"
            >
              <ArrowLeft className="w-4 h-4" />
            </button>
            <div>
              <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">THÊM CỤM RẠP MỚI</h1>
              <p className="text-xs text-zinc-500 mt-0.5">Thêm cụm rạp chiếu phim mới và định cấu hình vị trí địa lý</p>
            </div>
          </div>
        </div>

        {/* Create Form Container */}
        <form onSubmit={handleSubmit} className="w-full flex flex-col gap-6 pb-24">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            
            {/* Left and Middle Sections (2 Cols) */}
            <div className="lg:col-span-2 flex flex-col gap-6">
              
              {/* Card: Basic Information */}
              <div className="bg-zinc-900/60 border border-zinc-800/80 rounded-2xl p-6 flex flex-col gap-4">
                <div className="flex items-center gap-2 border-b border-zinc-800/80 pb-3">
                  <FileText className="w-4 h-4 text-orange-500" />
                  <h2 className="text-sm font-bold uppercase tracking-wider text-white">Thông Tin Cơ Bản</h2>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div className="md:col-span-2 flex flex-col gap-1.5">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Tên cụm rạp <span className="text-rose-500">*</span></label>
                    <input
                      type="text"
                      value={formData.name}
                      onChange={e => setFormData({ ...formData, name: e.target.value })}
                      placeholder="Ví dụ: LoraFilm Nguyễn Du"
                      className={`w-full bg-zinc-950 border ${formErrors.name ? 'border-red-500/80 focus:border-red-500' : 'border-zinc-800 focus:border-orange-500/40'} rounded-xl py-2.5 px-3.5 text-xs text-zinc-100 focus:outline-none transition-colors`}
                    />
                    {formErrors.name && <span className="text-[10px] text-red-500 font-semibold">{formErrors.name}</span>}
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Trạng thái hoạt động</label>
                    <select
                      value={formData.status}
                      onChange={e => setFormData({ ...formData, status: e.target.value })}
                      className="w-full bg-zinc-950 border border-zinc-800 focus:border-orange-500/40 rounded-xl py-2.5 px-3 text-xs text-zinc-200 focus:outline-none transition-colors cursor-pointer"
                    >
                      <option value="DRAFT">DRAFT (Bản nháp)</option>
                      <option value="ACTIVE">ACTIVE (Hoạt động)</option>
                      <option value="MAINTENANCE">MAINTENANCE (Bảo trì)</option>
                      <option value="TEMPORARILY_CLOSED">TEMPORARILY CLOSED (Tạm đóng)</option>
                      <option value="INACTIVE">INACTIVE (Ngừng hoạt động)</option>
                      <option value="PERMANENTLY_CLOSED">PERMANENTLY CLOSED (Đóng vĩnh viễn)</option>
                    </select>
                  </div>
                </div>

                <div className="flex flex-col gap-1.5">
                  <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Mô tả cụm rạp</label>
                  <textarea
                    rows={4}
                    value={formData.description}
                    onChange={e => setFormData({ ...formData, description: e.target.value })}
                    placeholder="Mô tả giới thiệu về rạp chiếu phim..."
                    className="w-full bg-zinc-950 border border-zinc-800 focus:border-orange-500/40 rounded-xl py-2.5 px-3.5 text-xs text-zinc-100 focus:outline-none transition-colors resize-none"
                  />
                </div>
              </div>

              {/* Card: Location Information */}
              <div className="bg-zinc-900/60 border border-zinc-800/80 rounded-2xl p-6 flex flex-col gap-4">
                <div className="flex items-center gap-2 border-b border-zinc-800/80 pb-3">
                  <MapPin className="w-4 h-4 text-orange-500" />
                  <h2 className="text-sm font-bold uppercase tracking-wider text-white">Vị Trí & Bản Đồ Địa Lý</h2>
                </div>

                {/* Autocomplete Search Input */}
                <div className="relative">
                  <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-zinc-500">
                    <Search className="w-4 h-4" />
                  </span>
                  <input
                    type="text"
                    value={addressSearch}
                    onChange={e => setAddressSearch(e.target.value)}
                    placeholder="Tìm kiếm vị trí cụ thể (nhập địa chỉ để định vị tự động)..."
                    className="w-full bg-zinc-950 border border-zinc-800 focus:border-orange-500/40 rounded-xl py-2.5 pl-9 pr-4 text-xs text-zinc-100 focus:outline-none transition-colors"
                  />

                  {/* Suggestion drop down */}
                  {suggestions.length > 0 && (
                    <div className="absolute left-0 right-0 top-full mt-1.5 max-h-56 bg-zinc-900 border border-zinc-800 rounded-xl overflow-y-auto z-50 shadow-2xl">
                      {suggestions.map((s, idx) => (
                        <div
                          key={idx}
                          onClick={() => handleSelectSuggestion(s)}
                          className="flex items-start gap-2.5 px-4 py-3 hover:bg-zinc-800/60 text-xs text-zinc-300 hover:text-white border-b border-zinc-800/40 last:border-b-0 cursor-pointer transition-colors"
                        >
                          <Navigation className="w-3.5 h-3.5 mt-0.5 text-orange-500 shrink-0" />
                          <div>
                            <span className="font-bold block">{s.label}</span>
                            {s.city && <span className="text-[10px] text-zinc-500 block mt-0.5">{s.city}, {s.country || 'Việt Nam'}</span>}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Interactive Leaflet Map View */}
                <div className="flex flex-col gap-2">
                  <div className="flex justify-between items-center text-[10px] font-bold text-zinc-500 uppercase tracking-wider px-1">
                    <span>Kéo marker hoặc click để ghim vị trí chính xác</span>
                    <span className="flex items-center gap-1"><Map className="w-3 h-3 text-orange-500" /> Bản đồ OSM</span>
                  </div>
                  <div className="relative w-full h-72 rounded-xl overflow-hidden border border-zinc-800 shadow-inner z-10">
                    <div ref={mapContainerRef} className="w-full h-full" style={{ background: '#18181b' }} />
                  </div>
                </div>

                {/* Geocoded fields */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="flex flex-col gap-1.5">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Kinh độ (Longitude) <span className="text-zinc-600">(Tự động)</span></label>
                    <input
                      type="text"
                      readOnly
                      value={formData.longitude}
                      className="w-full bg-zinc-950 border border-zinc-800/60 rounded-xl py-2.5 px-3.5 text-xs text-zinc-500 focus:outline-none select-all"
                    />
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Vĩ độ (Latitude) <span className="text-zinc-600">(Tự động)</span></label>
                    <input
                      type="text"
                      readOnly
                      value={formData.latitude}
                      className="w-full bg-zinc-950 border border-zinc-800/60 rounded-xl py-2.5 px-3.5 text-xs text-zinc-500 focus:outline-none select-all"
                    />
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Múi giờ (Timezone) <span className="text-zinc-600">(Tự động)</span></label>
                    <input
                      type="text"
                      readOnly
                      value={formData.timezone}
                      className="w-full bg-zinc-950 border border-zinc-800/60 rounded-xl py-2.5 px-3.5 text-xs text-zinc-500 focus:outline-none"
                    />
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Thành phố <span className="text-rose-500">*</span></label>
                    <input
                      type="text"
                      value={formData.city}
                      onChange={e => setFormData({ ...formData, city: e.target.value })}
                      placeholder="Ví dụ: Hồ Chí Minh"
                      className={`w-full bg-zinc-950 border ${formErrors.city ? 'border-red-500/80 focus:border-red-500' : 'border-zinc-800 focus:border-orange-500/40'} rounded-xl py-2.5 px-3.5 text-xs text-zinc-100 focus:outline-none transition-colors`}
                    />
                    {formErrors.city && <span className="text-[10px] text-red-500 font-semibold">{formErrors.city}</span>}
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Quận/Huyện <span className="text-rose-500">*</span></label>
                    <input
                      type="text"
                      value={formData.district}
                      onChange={e => setFormData({ ...formData, district: e.target.value })}
                      placeholder="Ví dụ: Quận 1"
                      className={`w-full bg-zinc-950 border ${formErrors.district ? 'border-red-500/80 focus:border-red-500' : 'border-zinc-800 focus:border-orange-500/40'} rounded-xl py-2.5 px-3.5 text-xs text-zinc-100 focus:outline-none transition-colors`}
                    />
                    {formErrors.district && <span className="text-[10px] text-red-500 font-semibold">{formErrors.district}</span>}
                  </div>

                  <div className="flex flex-col gap-1.5 md:col-span-2">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Địa chỉ chi tiết <span className="text-rose-500">*</span></label>
                    <input
                      type="text"
                      value={formData.address}
                      onChange={e => setFormData({ ...formData, address: e.target.value })}
                      placeholder="Ví dụ: 116 Nguyễn Du, Phường Bến Thành"
                      className={`w-full bg-zinc-950 border ${formErrors.address ? 'border-red-500/80 focus:border-red-500' : 'border-zinc-800 focus:border-orange-500/40'} rounded-xl py-2.5 px-3.5 text-xs text-zinc-100 focus:outline-none transition-colors`}
                    />
                    {formErrors.address && <span className="text-[10px] text-red-500 font-semibold">{formErrors.address}</span>}
                  </div>
                </div>
              </div>

            </div>

            {/* Right Sidebar Section (1 Col) */}
            <div className="flex flex-col gap-6">
              
              {/* Card: Contact info */}
              <div className="bg-zinc-900/60 border border-zinc-800/80 rounded-2xl p-6 flex flex-col gap-4">
                <div className="flex items-center gap-2 border-b border-zinc-800/80 pb-3">
                  <Phone className="w-4 h-4 text-orange-500" />
                  <h2 className="text-sm font-bold uppercase tracking-wider text-white">Liên Hệ</h2>
                </div>

                <div className="flex flex-col gap-1.5">
                  <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Số Hotline</label>
                  <input
                    type="text"
                    value={formData.hotline}
                    onChange={e => setFormData({ ...formData, hotline: e.target.value })}
                    placeholder="Ví dụ: 19001234"
                    className="w-full bg-zinc-950 border border-zinc-800 focus:border-orange-500/40 rounded-xl py-2.5 px-3.5 text-xs text-zinc-100 focus:outline-none transition-colors"
                  />
                </div>
              </div>

              {/* Card: Operating Dates */}
              <div className="bg-zinc-900/60 border border-zinc-800/80 rounded-2xl p-6 flex flex-col gap-4">
                <div className="flex items-center gap-2 border-b border-zinc-800/80 pb-3">
                  <Calendar className="w-4 h-4 text-orange-500" />
                  <h2 className="text-sm font-bold uppercase tracking-wider text-white">Thời Gian Hoạt Động</h2>
                </div>

                <div className="flex flex-col gap-1.5">
                  <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Ngày khai trương (Opened)</label>
                  <input
                    type="date"
                    value={formData.openedDate}
                    onChange={e => setFormData({ ...formData, openedDate: e.target.value })}
                    className="w-full bg-zinc-950 border border-zinc-800 focus:border-orange-500/40 rounded-xl py-2.5 px-3.5 text-xs text-zinc-100 focus:outline-none transition-colors"
                  />
                </div>

                <div className="flex flex-col gap-1.5">
                  <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Ngày đóng cửa (Closed)</label>
                  <input
                    type="date"
                    value={formData.closedDate}
                    onChange={e => setFormData({ ...formData, closedDate: e.target.value })}
                    className={`w-full bg-zinc-950 border ${formErrors.closedDate ? 'border-red-500/80' : 'border-zinc-800'} focus:border-orange-500/40 rounded-xl py-2.5 px-3.5 text-xs text-zinc-100 focus:outline-none transition-colors`}
                  />
                  {formErrors.closedDate && <span className="text-[10px] text-red-500 font-semibold">{formErrors.closedDate}</span>}
                </div>
              </div>

            </div>
          </div>

          {/* Bottom Action Bar */}
          <div className="fixed bottom-0 left-0 right-0 z-30 bg-zinc-950/80 backdrop-blur-md border-t border-zinc-900 py-4 px-6 lg:pl-72 flex justify-between items-center shadow-xl">
            <button
              type="button"
              onClick={() => setView('list')}
              className="flex items-center justify-center gap-2 border border-zinc-850 hover:border-zinc-700 bg-zinc-900 text-zinc-300 font-bold px-6 py-3 rounded-xl text-xs transition-colors cursor-pointer"
            >
              <span>Hủy Bỏ</span>
            </button>

            <button
              type="submit"
              disabled={isSubmitting}
              className="bg-brand-orange hover:bg-opacity-90 disabled:opacity-50 text-zinc-950 font-black px-8 py-3 rounded-xl text-xs transition-all shadow-xl shadow-brand-orange/10 tracking-wider uppercase flex items-center justify-center gap-2 cursor-pointer"
            >
              <Check className="w-4 h-4" />
              <span>{isSubmitting ? 'ĐANG TẠO...' : 'TẠO CỤM RẠP'}</span>
            </button>
          </div>
        </form>
      </div>
    );
  }

  // LIST VIEW RENDERING
  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-[400px] bg-zinc-950 text-white space-y-6 animate-fade-in">
      
      {/* Title Header */}
      <div className="flex flex-col border-b border-zinc-800 pb-4">
        <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">HỆ THỐNG CỤM RẠP</h1>
      </div>

      {/* Filter and search bar */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 bg-zinc-900/60 border border-zinc-850 p-4 rounded-2xl backdrop-blur-md">
        
        {/* Search by Keyword */}
        <div className="relative md:col-span-2">
          <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-zinc-500">
            <Search className="w-4 h-4" />
          </span>
          <input
            type="text"
            value={searchTerm}
            onChange={handleSearch}
            placeholder="Tìm tên rạp, địa chỉ..."
            className="w-full bg-zinc-950 border border-zinc-800 text-zinc-100 placeholder-zinc-500 focus:border-brand-orange/40 focus:ring-0 rounded-xl py-2.5 pl-9 pr-4 text-xs transition-colors"
          />
        </div>

        {/* City Filter */}
        <div>
          <select
            value={cityFilter}
            onChange={handleCityFilter}
            className="w-full bg-zinc-950 border border-zinc-800 text-zinc-300 focus:border-brand-orange/40 rounded-xl py-2.5 px-3.5 text-xs transition-colors cursor-pointer focus:outline-none"
          >
            <option value="">Tất cả thành phố</option>
            {citiesList.map(city => (
              <option key={city} value={city}>{city}</option>
            ))}
          </select>
        </div>

        {/* Status Filter */}
        <div>
          <select
            value={statusFilter}
            onChange={handleStatusFilter}
            className="w-full bg-zinc-950 border border-zinc-800 text-zinc-300 focus:border-brand-orange/40 rounded-xl py-2.5 px-3.5 text-xs transition-colors cursor-pointer focus:outline-none"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="DRAFT">DRAFT</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="MAINTENANCE">MAINTENANCE</option>
            <option value="TEMPORARILY_CLOSED">TEMPORARILY CLOSED</option>
            <option value="INACTIVE">INACTIVE</option>
            <option value="PERMANENTLY_CLOSED">PERMANENTLY CLOSED</option>
          </select>
        </div>
      </div>

      {/* Button add cinema */}
      <div className="flex justify-end">
        <button
          onClick={handleOpenCreate}
          className="bg-brand-orange hover:bg-opacity-90 text-zinc-950 font-black px-5 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-all duration-300 shadow-lg shadow-brand-orange/10 flex items-center gap-2 cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          <span>THÊM CỤM RẠP</span>
        </button>
      </div>

      {/* Data Table */}
      {isLoading ? (
        <SkeletonTable rows={5} columns={6} />
      ) : (
        <div className="bg-zinc-950 border border-zinc-900 rounded-2xl overflow-hidden w-full shadow-2xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse whitespace-nowrap">
              <thead>
                <tr className="bg-zinc-900/40 border-b border-zinc-900 text-[10px] font-black text-zinc-400 uppercase tracking-wider">
                  <th className="py-4 px-6 w-16 text-center">STT</th>
                  <th className="py-4 px-6">TÊN CỤM RẠP</th>
                  <th className="py-4 px-6">ĐỊA CHỈ</th>
                  <th className="py-4 px-6">HOTLINE</th>
                  <th className="py-4 px-6 w-44">TRẠNG THÁI</th>
                  <th className="py-4 px-6 w-24 text-right">THAO TÁC</th>
                </tr>
              </thead>
              <tbody>
                {cinemas.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="py-16 text-center text-zinc-500 text-sm font-semibold">
                      <div className="flex flex-col items-center justify-center gap-2">
                        <MapPin className="w-8 h-8 text-zinc-800" />
                        <span>Không tìm thấy cụm rạp nào.</span>
                      </div>
                    </td>
                  </tr>
                ) : (
                  cinemas.map((cinema, index) => (
                    <tr key={cinema.publicId} className="border-b border-zinc-900/60 hover:bg-zinc-900/30 transition-colors group">
                      <td className="py-4 px-6 text-center">
                        <span className="text-xs font-black text-zinc-500">
                          {((currentPage * pageSize) + index + 1).toString().padStart(2, '0')}
                        </span>
                      </td>
                      <td className="py-4 px-6">
                        <div className="flex flex-col gap-0.5">
                          <span className="text-sm font-bold text-zinc-200 group-hover:text-amber-400 transition-colors">
                            {cinema.name}
                          </span>
                          <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider">
                            {cinema.city} {cinema.district ? `- ${cinema.district}` : ''}
                          </span>
                        </div>
                      </td>
                      <td className="py-4 px-6">
                        <span className="text-xs text-zinc-400 font-medium block max-w-sm overflow-hidden text-ellipsis">
                          {cinema.address}
                        </span>
                      </td>
                      <td className="py-4 px-6">
                        <span className="text-xs font-semibold text-zinc-300">
                          {cinema.hotline || '—'}
                        </span>
                      </td>
                      <td className="py-4 px-6">
                        <div className="flex items-center gap-3">
                          {renderStatusBadge(cinema.status)}
                          
                          {/* Quick change dropdown */}
                          <select
                            value={cinema.status}
                            onChange={(e) => handleStatusChange(cinema.publicId, e.target.value)}
                            className="bg-zinc-900 border border-zinc-800 text-[10px] text-zinc-400 font-black rounded-lg py-1 px-1.5 focus:outline-none focus:border-brand-orange/40 cursor-pointer hover:text-white transition-colors"
                          >
                            <option value="DRAFT">DRAFT</option>
                            <option value="ACTIVE">ACTIVE</option>
                            <option value="MAINTENANCE">MAINTENANCE</option>
                            <option value="TEMPORARILY_CLOSED">TEMPORARILY CLOSED</option>
                            <option value="INACTIVE">INACTIVE</option>
                            <option value="PERMANENTLY_CLOSED">PERMANENTLY CLOSED</option>
                          </select>
                        </div>
                      </td>
                      <td className="py-4 px-6 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => handleDeleteCinema(cinema.publicId, cinema.name)}
                            className="p-2 text-zinc-500 hover:text-red-500 hover:bg-red-500/10 border border-transparent hover:border-red-500/20 rounded-lg transition-all cursor-pointer"
                            title="Xóa cụm rạp"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination Footer */}
          {totalPages > 1 && (
            <div className="flex justify-between items-center px-6 py-4 bg-zinc-900/20 border-t border-zinc-900">
              <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider">
                Hiển thị {cinemas.length} / {totalElements} cụm rạp
              </span>
              <div className="flex gap-2">
                <button
                  disabled={currentPage === 0}
                  onClick={() => setCurrentPage(currentPage - 1)}
                  className="px-3 py-1.5 bg-zinc-900 border border-zinc-800 disabled:opacity-50 text-xs text-zinc-300 rounded-lg hover:text-white transition-colors cursor-pointer"
                >
                  Trước
                </button>
                <span className="px-3 py-1.5 text-xs text-zinc-400 font-bold bg-zinc-950 border border-zinc-900 rounded-lg">
                  {currentPage + 1} / {totalPages}
                </span>
                <button
                  disabled={currentPage === totalPages - 1}
                  onClick={() => setCurrentPage(currentPage + 1)}
                  className="px-3 py-1.5 bg-zinc-900 border border-zinc-800 disabled:opacity-50 text-xs text-zinc-300 rounded-lg hover:text-white transition-colors cursor-pointer"
                >
                  Sau
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
