// eslint-disable-next-line no-unused-vars
import React, { useState, useEffect, useRef, useCallback } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { MapPin, Search, Navigation, Map } from 'lucide-react';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';

const markerIcon = L.icon({
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41]
});

const LOCAL_SUGGESTIONS_FALLBACK = [
  { label: 'Quận Ninh Kiều, Cần Thơ', city: 'Cần Thơ', district: 'Ninh Kiều', latitude: 10.0371124, longitude: 105.7882201, timezone: 'Asia/Ho_Chi_Minh' },
  { label: 'Bến Ninh Kiều, Cần Thơ', city: 'Cần Thơ', district: 'Ninh Kiều', latitude: 10.0336585, longitude: 105.7877239, timezone: 'Asia/Ho_Chi_Minh' },
  { label: 'Đại học Cần Thơ, Xuân Khánh', city: 'Cần Thơ', district: 'Ninh Kiều', latitude: 10.0299337, longitude: 105.7684824, timezone: 'Asia/Ho_Chi_Minh' },
  { label: 'Nhà hát Lớn Hà Nội, Tràng Tiền', city: 'Hà Nội', district: 'Hoàn Kiếm', latitude: 21.0243452, longitude: 105.8572138, timezone: 'Asia/Ho_Chi_Minh' },
  { label: 'Hồ Hoàn Kiếm, Hàng Trống', city: 'Hà Nội', district: 'Hoàn Kiếm', latitude: 21.0285110, longitude: 105.8542240, timezone: 'Asia/Ho_Chi_Minh' },
  { label: 'Chợ Bến Thành, Quận 1', city: 'Hồ Chí Minh', district: 'Quận 1', latitude: 10.7719183, longitude: 106.6982631, timezone: 'Asia/Ho_Chi_Minh' },
  { label: 'Nhà thờ Đức Bà, Bến Nghé, Quận 1', city: 'Hồ Chí Minh', district: 'Quận 1', latitude: 10.7797746, longitude: 106.6990479, timezone: 'Asia/Ho_Chi_Minh' },
  { label: 'Cầu Rồng, Hải Châu, Đà Nẵng', city: 'Đà Nẵng', district: 'Hải Châu', latitude: 16.0611598, longitude: 108.2272895, timezone: 'Asia/Ho_Chi_Minh' }
];

const getTimezoneByCountryCode = (countryCode, lon) => {
  const cc = countryCode?.toLowerCase();
  if (!cc) {
    if (lon >= 95 && lon <= 110) return 'Asia/Ho_Chi_Minh';
  }
  const tzMap = {
    vn: 'Asia/Ho_Chi_Minh',
    th: 'Asia/Bangkok',
    sg: 'Asia/Singapore',
    jp: 'Asia/Tokyo',
    kr: 'Asia/Seoul',
    cn: 'Asia/Shanghai',
    gb: 'Europe/London',
    fr: 'Europe/Paris',
    de: 'Europe/Berlin',
    ru: 'Europe/Moscow',
    au: 'Australia/Sydney',
    in: 'Asia/Kolkata',
    br: 'America/Sao_Paulo',
    ca: 'America/Toronto',
    mx: 'America/Mexico_City'
  };
  
  if (tzMap[cc]) return tzMap[cc];
  
  if (cc === 'us') {
    if (lon < -114) return 'America/Los_Angeles';
    if (lon < -102) return 'America/Denver';
    if (lon < -88) return 'America/Chicago';
    return 'America/New_York';
  }
  
  const offset = Math.round(lon / 15);
  return `UTC${offset >= 0 ? '+' : ''}${offset}`;
};

export default function CinemaLocationForm({ formData, setFormData, formErrors, addressSearch, setAddressSearch }) {
  const [suggestions, setSuggestions] = useState([]);
  const mapContainerRef = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);

  // Autocomplete debouncing
  useEffect(() => {
    if (!addressSearch.trim()) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setSuggestions([]);
      return;
    }

    const timer = setTimeout(async () => {
      // 1. Try backend API first
      try {
        const res = await adminCinemaService.suggestAddress(addressSearch);
        if (res?.success && Array.isArray(res?.data)) {
          setSuggestions(res.data);
          return;
        } else if (Array.isArray(res)) {
          setSuggestions(res);
          return;
        }
      // eslint-disable-next-line no-unused-vars
      } catch (err) {
        // ignore
      }

      // 2. Try Nominatim public API
      try {
        const url = `https://nominatim.openstreetmap.org/search?format=jsonv2&q=${encodeURIComponent(addressSearch)}&accept-language=vi,en&addressdetails=1&limit=5`;
        const response = await fetch(url);
        if (response.ok) {
          const data = await response.json();
          if (Array.isArray(data)) {
            const mapped = data.map(item => {
              const nameParts = item.display_name.split(',').map(s => s.trim());
              const city = item.address?.city || item.address?.town || item.address?.state || nameParts[nameParts.length - 2] || '';
              const district = item.address?.district || item.address?.suburb || item.address?.county || item.address?.city_district || nameParts[nameParts.length - 3] || '';
              return {
                label: item.display_name,
                latitude: parseFloat(item.lat),
                longitude: parseFloat(item.lon),
                city,
                district,
                countryCode: item.address?.country_code || ''
              };
            });
            setSuggestions(mapped);
            return;
          }
        }
      // eslint-disable-next-line no-unused-vars
      } catch (err) {
        // ignore
      }

      // 3. Try Local fallback list
      const query = addressSearch.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
      const matched = LOCAL_SUGGESTIONS_FALLBACK.filter(item => {
        const labelNorm = item.label.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
        const cityNorm = item.city.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
        return labelNorm.includes(query) || cityNorm.includes(query);
      });
      setSuggestions(matched);
    }, 300);

    return () => clearTimeout(timer);
  }, [addressSearch]);

  // Handle coordinates click or drag map actions
  const handleCoordinatesChange = useCallback(async (lat, lon) => {
    setFormData(prev => ({
      ...prev,
      latitude: parseFloat(lat.toFixed(7)),
      longitude: parseFloat(lon.toFixed(7))
    }));

    let reverseData = null;

    // 1. Try backend reverse geocoding API first
    try {
      const res = await adminCinemaService.reverseGeocode(lat, lon);
      if (res?.success && res?.data) {
        reverseData = res.data;
      } else if (res && (res.city || res.label)) {
        reverseData = res;
      }
    // eslint-disable-next-line no-unused-vars
    } catch (err) {
      // ignore
    }

    // 2. Try Nominatim public API
    if (!reverseData) {
      try {
        const url = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lon}&accept-language=vi,en`;
        const response = await fetch(url);
        if (response.ok) {
          const data = await response.json();
          if (data && data.address) {
            const addr = data.address;
            const city = addr.city || addr.town || addr.village || addr.state || '';
            const district = addr.district || addr.suburb || addr.county || addr.city_district || '';
            
            const label = data.display_name || '';
            reverseData = {
              label,
              city,
              district,
              countryCode: addr.country_code || '',
              timezone: getTimezoneByCountryCode(addr.country_code, lon)
            };
          }
        }
      // eslint-disable-next-line no-unused-vars
      } catch (err) {
        // ignore
      }
    }

    // 3. Apply reverse geocode results or use fallback mapping
    if (reverseData) {
      const tz = reverseData.timezone || getTimezoneByCountryCode(reverseData.countryCode, lon);
      setFormData(prev => ({
        ...prev,
        address: reverseData.label || reverseData.address || '',
        city: reverseData.city || '',
        district: reverseData.district || '',
        timezone: tz
      }));
      setAddressSearch(reverseData.label || reverseData.address || '');
    } else {
      console.warn('Reverse geocode failed, finding closest local region fallback');
      let closest = null;
      let minDistance = Infinity;
      for (const item of LOCAL_SUGGESTIONS_FALLBACK) {
        const d = Math.pow(item.latitude - lat, 2) + Math.pow(item.longitude - lon, 2);
        if (d < minDistance) {
          minDistance = d;
          closest = item;
        }
      }
      
      if (closest && minDistance < 5.0) {
        const isVeryClose = minDistance < 0.00001;
        const fallbackAddress = isVeryClose 
          ? closest.label 
          : `Tọa độ ${lat.toFixed(6)}, ${lon.toFixed(6)}, ${closest.district}, ${closest.city}`;

        setFormData(prev => ({
          ...prev,
          city: closest.city,
          district: closest.district,
          timezone: closest.timezone,
          address: fallbackAddress
        }));
        setAddressSearch(isVeryClose ? closest.label : `Tọa độ ${lat.toFixed(6)}, ${lon.toFixed(6)}`);
      } else {
        const fallbackAddress = `Tọa độ ${lat.toFixed(6)}, ${lon.toFixed(6)}, Quận 1, Hồ Chí Minh`;
        setFormData(prev => ({
          ...prev,
          city: 'Hồ Chí Minh',
          district: 'Quận 1',
          timezone: 'Asia/Ho_Chi_Minh',
          address: fallbackAddress
        }));
        setAddressSearch(`Tọa độ ${lat.toFixed(6)}, ${lon.toFixed(6)}`);
      }
    }
  }, [setFormData, setAddressSearch]);

  // Initialize Map
  useEffect(() => {
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
      // eslint-disable-next-line no-unused-vars
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

    return () => {
      if (mapRef.current) {
        mapRef.current.remove();
        mapRef.current = null;
        markerRef.current = null;
      }
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [handleCoordinatesChange]);

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

    let reverseData = null;

    // 1. Try backend reverse geocoding API first
    try {
      const res = await adminCinemaService.reverseGeocode(lat, lon);
      if (res?.success && res?.data) {
        reverseData = res.data;
      } else if (res && (res.city || res.label)) {
        reverseData = res;
      }
    // eslint-disable-next-line no-unused-vars
    } catch (err) {
      // ignore
    }

    // 2. Try Nominatim public API
    if (!reverseData) {
      try {
        const url = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lon}&accept-language=vi,en`;
        const response = await fetch(url);
        if (response.ok) {
          const data = await response.json();
          if (data && data.address) {
            const addr = data.address;
            const city = addr.city || addr.town || addr.village || addr.state || '';
            const district = addr.district || addr.suburb || addr.county || addr.city_district || '';
            reverseData = {
              label: data.display_name,
              city,
              district,
              countryCode: addr.country_code || '',
              timezone: getTimezoneByCountryCode(addr.country_code, lon)
            };
          }
        }
      // eslint-disable-next-line no-unused-vars
      } catch (err) {
        // ignore
      }
    }

    // 3. Apply reverse geocode results or use suggestion defaults
    if (reverseData) {
      const tz = reverseData.timezone || getTimezoneByCountryCode(reverseData.countryCode, lon);
      setFormData(prev => ({
        ...prev,
        city: reverseData.city || s.city || prev.city || '',
        district: reverseData.district || prev.district || '',
        timezone: tz,
        address: reverseData.label || reverseData.address || s.label || prev.address
      }));
    } else {
      setFormData(prev => ({
        ...prev,
        city: s.city || prev.city || '',
        district: s.district || prev.district || '',
        timezone: getTimezoneByCountryCode(s.countryCode, lon),
        address: s.label || prev.address
      }));
    }
  };

  return (
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
            {suggestions.slice(0, 5).map((s, idx) => (
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
  );
}
