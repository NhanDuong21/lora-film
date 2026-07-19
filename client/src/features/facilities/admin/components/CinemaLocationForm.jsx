// eslint-disable-next-line no-unused-vars
import React, { useState, useEffect, useRef, useCallback } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { MapPin, Map } from 'lucide-react';
import { CinemaLocationAutocomplete } from './CinemaLocationAutocomplete';

const markerIcon = L.icon({
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41]
});

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
  const mapContainerRef = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);

  // Handle coordinates click or drag map actions
  const handleCoordinatesChange = useCallback(async (lat, lon) => {
    setFormData(prev => ({
      ...prev,
      latitude: lat,
      longitude: lon
    }));

    try {
      const response = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}&addressdetails=1`, {
        headers: {
          'Accept-Language': 'vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7'
        }
      });
      const data = await response.json();
      
      if (data && data.address) {
        const addressObj = data.address;
        const city = addressObj.city || addressObj.province || addressObj.state || addressObj.municipality || addressObj.town || '';
        const district = addressObj.county || addressObj.district || addressObj.suburb || addressObj.city_district || '';
        const addressText = data.display_name || '';
        const countryCode = addressObj.country_code ? addressObj.country_code.toUpperCase() : null;

        setFormData(prev => ({
          ...prev,
          city: city,
          district: district,
          address: addressText,
          timezone: getTimezoneByCountryCode(countryCode, lon)
        }));
        setAddressSearch(addressText);
      } else {
        setAddressSearch(`Tọa độ ${lat.toFixed(6)}, ${lon.toFixed(6)}`);
      }
    } catch (error) {
      console.error('Reverse geocode failed:', error);
      setAddressSearch(`Tọa độ ${lat.toFixed(6)}, ${lon.toFixed(6)}`);
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
    setAddressSearch(s.label);

    const lat = parseFloat(s.latitude);
    const lon = parseFloat(s.longitude);

    setFormData(prev => ({
      ...prev,
      latitude: lat,
      longitude: lon,
      city: s.city || '',
      district: s.district || '',
      timezone: getTimezoneByCountryCode(s.countryCode, lon),
      address: s.address || s.label || ''
    }));

    if (mapRef.current && markerRef.current) {
      markerRef.current.setLatLng([lat, lon]);
      mapRef.current.setView([lat, lon], 16);
    }
  };

  return (
    <div className="bg-zinc-900/60 border border-zinc-800/80 rounded-2xl p-6 flex flex-col gap-4">
      <div className="flex items-center gap-2 border-b border-zinc-800/80 pb-3">
        <MapPin className="w-4 h-4 text-orange-500" />
        <h2 className="text-sm font-bold uppercase tracking-wider text-white">Vị Trí & Bản Đồ Địa Lý</h2>
      </div>

      {/* Autocomplete Search Input */}
      <div className="relative z-50">
        <CinemaLocationAutocomplete
          id="locationSearch"
          value={addressSearch}
          onChange={setAddressSearch}
          onSelect={handleSelectSuggestion}
          placeholder="Tìm kiếm vị trí cụ thể (nhập địa chỉ để định vị tự động)..."
        />
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
