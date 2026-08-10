import { useCallback, useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { CheckCircle2, Map, MapPin } from 'lucide-react';
import { CinemaLocationAutocomplete } from './CinemaLocationAutocomplete';

const markerIcon = L.icon({
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
});

const getTimezoneByCountryCode = (countryCode, longitude) => {
  const country = countryCode?.toLowerCase();
  const timezoneByCountry = {
    vn: 'Asia/Ho_Chi_Minh',
    th: 'Asia/Bangkok',
    sg: 'Asia/Singapore',
    jp: 'Asia/Tokyo',
    kr: 'Asia/Seoul',
    cn: 'Asia/Shanghai',
  };

  if (timezoneByCountry[country]) return timezoneByCountry[country];
  if (!country && longitude >= 95 && longitude <= 110) return 'Asia/Ho_Chi_Minh';
  return 'Asia/Ho_Chi_Minh';
};

export default function CinemaLocationForm({
  formData,
  setFormData,
  formErrors,
  addressSearch,
  setAddressSearch,
}) {
  const mapContainerRef = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);

  const handleCoordinatesChange = useCallback(async (latitude, longitude) => {
    setFormData((previous) => ({ ...previous, latitude, longitude }));

    try {
      const response = await fetch(
        `https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}&addressdetails=1`,
        { headers: { 'Accept-Language': 'vi-VN,vi;q=0.9' } },
      );
      const data = await response.json();
      const address = data?.address;
      if (!address) return;

      const city = address.city || address.province || address.state || address.town || '';
      const district =
        address.county || address.district || address.suburb || address.city_district || '';
      const displayAddress = data.display_name || '';
      setFormData((previous) => ({
        ...previous,
        latitude,
        longitude,
        city,
        district,
        address: displayAddress,
        timezone: getTimezoneByCountryCode(address.country_code, longitude),
      }));
      setAddressSearch(displayAddress);
    } catch (error) {
      console.warn('Không thể lấy địa chỉ từ vị trí trên bản đồ:', error);
    }
  }, [setAddressSearch, setFormData]);

  useEffect(() => {
    if (!mapContainerRef.current || mapRef.current) return undefined;

    const initialLatitude = Number(formData.latitude) || 10.7741;
    const initialLongitude = Number(formData.longitude) || 106.6934;
    const map = L.map(mapContainerRef.current, {
      zoomControl: true,
      attributionControl: false,
    }).setView([initialLatitude, initialLongitude], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);
    const marker = L.marker([initialLatitude, initialLongitude], {
      draggable: true,
      icon: markerIcon,
    }).addTo(map);

    mapRef.current = map;
    markerRef.current = marker;
    marker.on('dragend', () => {
      const position = marker.getLatLng();
      handleCoordinatesChange(position.lat, position.lng);
    });
    map.on('click', ({ latlng }) => {
      marker.setLatLng(latlng);
      map.panTo(latlng);
      handleCoordinatesChange(latlng.lat, latlng.lng);
    });

    return () => {
      map.remove();
      mapRef.current = null;
      markerRef.current = null;
    };
  }, [formData.latitude, formData.longitude, handleCoordinatesChange]);

  const handleSelectSuggestion = (suggestion) => {
    const latitude = Number.parseFloat(suggestion.latitude);
    const longitude = Number.parseFloat(suggestion.longitude);
    setAddressSearch(suggestion.label);
    setFormData((previous) => ({
      ...previous,
      latitude,
      longitude,
      city: suggestion.city || '',
      district: suggestion.district || '',
      timezone: getTimezoneByCountryCode(suggestion.countryCode, longitude),
      address: suggestion.address || suggestion.label || '',
    }));

    markerRef.current?.setLatLng([latitude, longitude]);
    mapRef.current?.setView([latitude, longitude], 16);
  };

  return (
    <section className="flex flex-col gap-4 rounded-2xl border border-zinc-800 bg-zinc-900/60 p-6">
      <div className="flex items-center gap-2 border-b border-zinc-800 pb-3">
        <MapPin className="h-4 w-4 text-orange-500" />
        <div>
          <h2 className="text-sm font-bold uppercase tracking-wider text-white">
            Địa chỉ và vị trí trên bản đồ
          </h2>
          <p className="mt-1 text-xs text-zinc-500">
            Tìm địa chỉ rồi kéo ghim nếu cần điều chỉnh vị trí chính xác.
          </p>
        </div>
      </div>

      <div className="relative z-50">
        <CinemaLocationAutocomplete
          id="locationSearch"
          value={addressSearch}
          onChange={setAddressSearch}
          onSelect={handleSelectSuggestion}
          placeholder="Nhập tên đường, số nhà hoặc địa điểm gần cụm rạp..."
        />
      </div>

      <div className="flex flex-col gap-2">
        <div className="flex items-center justify-between px-1 text-[10px] font-bold uppercase tracking-wider text-zinc-500">
          <span>Kéo ghim hoặc nhấn trên bản đồ để chọn vị trí</span>
          <span className="flex items-center gap-1">
            <Map className="h-3 w-3 text-orange-500" /> Bản đồ OpenStreetMap
          </span>
        </div>
        <div className="relative z-10 h-72 w-full overflow-hidden rounded-xl border border-zinc-800">
          <div ref={mapContainerRef} className="h-full w-full bg-zinc-900" />
        </div>
        <div className="flex items-start gap-2 rounded-xl border border-emerald-500/20 bg-emerald-500/5 p-3 text-xs text-emerald-300">
          <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />
          <span>
            Tọa độ và múi giờ được hệ thống xác định tự động, quản trị viên không cần nhập
            dữ liệu kỹ thuật này.
          </span>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <label className="flex flex-col gap-1.5">
          <span className="text-[10px] font-black uppercase tracking-wider text-zinc-500">
            Tỉnh / thành phố <span className="text-rose-500">*</span>
          </span>
          <input
            type="text"
            value={formData.city}
            onChange={(event) => setFormData({ ...formData, city: event.target.value })}
            placeholder="Ví dụ: Cần Thơ"
            className={`rounded-xl border bg-zinc-950 px-3.5 py-2.5 text-xs text-zinc-100 outline-none ${
              formErrors.city ? 'border-red-500' : 'border-zinc-800 focus:border-orange-500/40'
            }`}
          />
          {formErrors.city && <span className="text-[10px] text-red-500">{formErrors.city}</span>}
        </label>

        <label className="flex flex-col gap-1.5">
          <span className="text-[10px] font-black uppercase tracking-wider text-zinc-500">
            Quận / huyện <span className="text-rose-500">*</span>
          </span>
          <input
            type="text"
            value={formData.district}
            onChange={(event) => setFormData({ ...formData, district: event.target.value })}
            placeholder="Ví dụ: Ninh Kiều"
            className={`rounded-xl border bg-zinc-950 px-3.5 py-2.5 text-xs text-zinc-100 outline-none ${
              formErrors.district ? 'border-red-500' : 'border-zinc-800 focus:border-orange-500/40'
            }`}
          />
          {formErrors.district && (
            <span className="text-[10px] text-red-500">{formErrors.district}</span>
          )}
        </label>

        <label className="flex flex-col gap-1.5 md:col-span-2">
          <span className="text-[10px] font-black uppercase tracking-wider text-zinc-500">
            Địa chỉ hiển thị cho khách hàng <span className="text-rose-500">*</span>
          </span>
          <input
            type="text"
            value={formData.address}
            onChange={(event) => setFormData({ ...formData, address: event.target.value })}
            placeholder="Ví dụ: 209 đường 30 Tháng 4, phường Xuân Khánh"
            className={`rounded-xl border bg-zinc-950 px-3.5 py-2.5 text-xs text-zinc-100 outline-none ${
              formErrors.address ? 'border-red-500' : 'border-zinc-800 focus:border-orange-500/40'
            }`}
          />
          {formErrors.address && (
            <span className="text-[10px] text-red-500">{formErrors.address}</span>
          )}
        </label>
      </div>
    </section>
  );
}
