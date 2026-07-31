import { useState } from 'react';
import { Edit3, MapPin, Phone, Save, X } from 'lucide-react';
import { CinemaLocationAutocomplete } from '../../components/CinemaLocationAutocomplete';

function toForm(cinema) {
  return {
    name: cinema?.name || '',
    city: cinema?.city || '',
    district: cinema?.district || '',
    address: cinema?.address || '',
    latitude: cinema?.latitude ?? '',
    longitude: cinema?.longitude ?? '',
    timezone: cinema?.timezone || 'Asia/Ho_Chi_Minh',
    hotline: cinema?.hotline || '',
    description: cinema?.description || '',
    openedDate: cinema?.openedDate || null,
    closedDate: cinema?.closedDate || null,
  };
}

export default function CinemaOverviewTab({ cinema, onUpdate }) {
  const [isEditing, setIsEditing] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formData, setFormData] = useState(() => toForm(cinema));

  const cancelEditing = () => {
    setFormData(toForm(cinema));
    setIsEditing(false);
  };

  const selectAddress = (location) => {
    setFormData((current) => ({
      ...current,
      address: location.address || location.label || current.address,
      city: location.city || current.city,
      district: location.district || current.district,
      latitude: location.latitude ?? current.latitude,
      longitude: location.longitude ?? current.longitude,
      timezone: location.timezone || current.timezone || 'Asia/Ho_Chi_Minh',
    }));
  };

  const submit = async (event) => {
    event.preventDefault();
    setIsSubmitting(true);
    const success = await onUpdate({
      ...formData,
      latitude: formData.latitude === '' ? null : Number(formData.latitude),
      longitude: formData.longitude === '' ? null : Number(formData.longitude),
    });
    setIsSubmitting(false);
    if (success) setIsEditing(false);
  };

  if (!isEditing) {
    return (
      <div className="max-w-5xl space-y-6 pb-20">
        <section className="rounded-3xl border border-zinc-800 bg-zinc-900/30 p-6">
          <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
            <div>
              <p className="text-[10px] font-black uppercase tracking-[0.2em] text-zinc-500">
                Thông tin khách hàng nhìn thấy
              </p>
              <h2 className="mt-2 text-xl font-black text-white">{cinema.name}</h2>
              <p className="mt-3 max-w-3xl text-sm leading-6 text-zinc-400">
                {cinema.description || 'Chưa có phần giới thiệu cụm rạp.'}
              </p>
            </div>
            <button
              type="button"
              onClick={() => setIsEditing(true)}
              className="inline-flex items-center justify-center gap-2 rounded-xl bg-orange-500 px-4 py-2.5 text-xs font-black text-white"
            >
              <Edit3 className="h-4 w-4" />
              Chỉnh sửa thông tin
            </button>
          </div>
        </section>

        <section className="grid gap-4 md:grid-cols-2">
          <InfoCard
            icon={MapPin}
            title="Địa chỉ phục vụ"
            value={cinema.address || 'Chưa có địa chỉ'}
            detail={[cinema.district, cinema.city].filter(Boolean).join(', ')}
          />
          <InfoCard
            icon={Phone}
            title="Thông tin liên hệ"
            value={cinema.hotline || 'Chưa có hotline'}
            detail="Số điện thoại hỗ trợ khách hàng tại cụm rạp"
          />
        </section>

        <div className="rounded-2xl border border-sky-500/20 bg-sky-500/5 p-5 text-xs leading-5 text-zinc-400">
          Vị trí bản đồ và múi giờ được hệ thống xác định tự động từ địa chỉ. Các thông số kỹ thuật
          này được ẩn để tránh chỉnh sửa nhầm.
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={submit} className="max-w-5xl space-y-6 pb-20">
      <section className="rounded-3xl border border-orange-500/20 bg-zinc-900/30 p-6">
        <div className="mb-6 flex items-start justify-between gap-4">
          <div>
            <h2 className="text-sm font-black uppercase tracking-wider text-white">
              Chỉnh sửa thông tin & vị trí
            </h2>
            <p className="mt-1 text-xs text-zinc-500">
              Những thay đổi này có thể xuất hiện trên trang khách hàng.
            </p>
          </div>
          <button
            type="button"
            onClick={cancelEditing}
            className="inline-flex items-center gap-2 rounded-xl border border-zinc-800 px-3 py-2 text-xs font-bold text-zinc-300"
          >
            <X className="h-4 w-4" />
            Hủy chỉnh sửa
          </button>
        </div>

        <div className="grid gap-5 md:grid-cols-2">
          <Field className="md:col-span-2" label="Tên cụm rạp">
            <input
              required
              value={formData.name}
              onChange={(event) => setFormData({ ...formData, name: event.target.value })}
              className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm text-white outline-none transition focus:border-orange-500"
            />
          </Field>
          <Field className="md:col-span-2" label="Tìm và xác nhận địa chỉ">
            <CinemaLocationAutocomplete
              id="cinema-address"
              value={formData.address}
              onChange={(value) => setFormData({ ...formData, address: value })}
              onSelect={selectAddress}
              placeholder="Nhập tên đường, phường hoặc địa điểm gần cụm rạp..."
            />
          </Field>
          <Field label="Tỉnh / Thành phố">
            <input
              required
              value={formData.city}
              onChange={(event) => setFormData({ ...formData, city: event.target.value })}
              className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm text-white outline-none transition focus:border-orange-500"
            />
          </Field>
          <Field label="Quận / Huyện">
            <input
              required
              value={formData.district}
              onChange={(event) => setFormData({ ...formData, district: event.target.value })}
              className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm text-white outline-none transition focus:border-orange-500"
            />
          </Field>
          <Field label="Hotline">
            <input
              value={formData.hotline}
              onChange={(event) => setFormData({ ...formData, hotline: event.target.value })}
              className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm text-white outline-none transition focus:border-orange-500"
            />
          </Field>
          <Field className="md:col-span-2" label="Giới thiệu cụm rạp">
            <textarea
              rows={5}
              value={formData.description}
              onChange={(event) => setFormData({ ...formData, description: event.target.value })}
              className="w-full resize-y rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm text-white outline-none transition focus:border-orange-500"
            />
          </Field>
        </div>
      </section>

      <div className="flex justify-end gap-3">
        <button
          type="button"
          onClick={cancelEditing}
          className="rounded-xl border border-zinc-800 px-5 py-3 text-xs font-black text-zinc-300"
        >
          Giữ nguyên thông tin
        </button>
        <button
          type="submit"
          disabled={isSubmitting}
          className="inline-flex items-center gap-2 rounded-xl bg-orange-500 px-6 py-3 text-xs font-black text-white disabled:opacity-50"
        >
          <Save className="h-4 w-4" />
          {isSubmitting ? 'Đang lưu...' : 'Lưu thay đổi'}
        </button>
      </div>
    </form>
  );
}

function Field({ label, children, className = '' }) {
  return (
    <label className={`flex flex-col gap-2 ${className}`}>
      <span className="text-[10px] font-black uppercase tracking-wider text-zinc-500">{label}</span>
      {children}
    </label>
  );
}

function InfoCard({ icon: Icon, title, value, detail }) {
  return (
    <div className="rounded-2xl border border-zinc-800 bg-zinc-900/30 p-5">
      <div className="flex items-center gap-2 text-orange-400">
        <Icon className="h-4 w-4" />
        <p className="text-xs font-black uppercase tracking-wider">{title}</p>
      </div>
      <p className="mt-4 text-sm font-bold text-zinc-100">{value}</p>
      <p className="mt-1 text-xs text-zinc-500">{detail}</p>
    </div>
  );
}
