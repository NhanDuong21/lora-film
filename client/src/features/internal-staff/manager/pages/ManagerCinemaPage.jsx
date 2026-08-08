import { useEffect, useMemo, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { Building2, CheckCircle2, Clock3, Info, MapPin, Phone, Save } from 'lucide-react';
import CinemaOperatingHours from '@/features/facilities/admin/components/CinemaOperatingHours';
import managerCinemaService from '../services/managerCinemaService';

const defaultHours = () => Array.from({ length: 7 }, (_, index) => ({
  dayOfWeek: index + 1,
  openTime: '08:00',
  closeTime: '23:00',
  isClosed: false,
}));

const normalizeHours = operatingHours => {
  const byDay = new Map((operatingHours || []).map(item => [Number(item.dayOfWeek), item]));
  return defaultHours().map(fallback => ({ ...fallback, ...(byDay.get(fallback.dayOfWeek) || {}) }));
};

export default function ManagerCinemaPage() {
  const { selectedCinema, selectedCinemaId, cinemaState, reloadCinemas } = useOutletContext();
  const [hours, setHours] = useState(defaultHours);
  const [savedHours, setSavedHours] = useState(defaultHours);
  const [saveState, setSaveState] = useState({ saving: false, error: '', success: '' });

  useEffect(() => {
    const next = normalizeHours(selectedCinema?.operatingHours);
    /* eslint-disable react-hooks/set-state-in-effect */
    setHours(next);
    setSavedHours(next);
    setSaveState({ saving: false, error: '', success: '' });
    /* eslint-enable react-hooks/set-state-in-effect */
  }, [selectedCinema]);

  const changed = useMemo(() => JSON.stringify(hours) !== JSON.stringify(savedHours), [hours, savedHours]);
  const updateHour = (index, field, value) => setHours(current => current.map((item, itemIndex) => itemIndex === index ? { ...item, [field]: value } : item));

  const save = async () => {
    if (!selectedCinemaId || !changed) return;
    setSaveState({ saving: true, error: '', success: '' });
    try {
      await managerCinemaService.updateOperatingHours(selectedCinemaId, hours.map(item => ({
        dayOfWeek: item.dayOfWeek,
        openTime: item.isClosed ? null : item.openTime,
        closeTime: item.isClosed ? null : item.closeTime,
        isClosed: item.isClosed,
      })));
      setSavedHours(hours);
      setSaveState({ saving: false, error: '', success: 'Đã cập nhật giờ mở cửa của rạp.' });
      await reloadCinemas();
    } catch (error) {
      setSaveState({ saving: false, error: error?.message || 'Không thể lưu giờ mở cửa.', success: '' });
    }
  };

  if (cinemaState.loading) return <p className="py-24 text-center text-sm font-bold text-zinc-500">Đang tải thông tin rạp…</p>;
  if (!selectedCinema) return <div className="rounded-2xl border border-amber-500/20 bg-amber-500/10 p-8 text-center"><h1 className="text-xl font-black">Chưa được phân công rạp</h1><p className="mt-2 text-sm text-amber-100/70">Bạn chỉ có thể cập nhật rạp do Quản trị viên chỉ định.</p></div>;

  return (
    <div className="space-y-6">
      <header><p className="text-xs font-black uppercase tracking-[0.2em] text-brand-orange">Rạp được phân công</p><h1 className="mt-2 text-3xl font-black">Thông tin vận hành</h1><p className="mt-2 text-sm text-zinc-500">Kiểm tra địa chỉ, liên hệ và cập nhật giờ mở cửa của rạp đang phụ trách.</p></header>

      <section className="grid gap-4 lg:grid-cols-[1.35fr_1fr]">
        <article className="rounded-2xl border border-white/10 bg-white/[0.025] p-6"><div className="flex items-start gap-4"><span className="grid h-12 w-12 shrink-0 place-items-center rounded-xl bg-brand-orange/10 text-brand-orange"><Building2 size={24} /></span><div><h2 className="text-xl font-black">{selectedCinema.name}</h2><span className="mt-2 inline-flex items-center gap-1.5 rounded-full border border-emerald-500/20 bg-emerald-500/10 px-2.5 py-1 text-xs font-bold text-emerald-300"><CheckCircle2 size={13} /> {selectedCinema.status === 'ACTIVE' ? 'Đang hoạt động' : selectedCinema.status}</span></div></div><dl className="mt-6 grid gap-4 text-sm"><div className="flex items-start gap-3"><MapPin size={17} className="mt-0.5 shrink-0 text-zinc-600" /><div><dt className="text-xs font-bold text-zinc-600">Địa chỉ</dt><dd className="mt-1 text-zinc-300">{selectedCinema.address || 'Chưa cập nhật'}{selectedCinema.city ? `, ${selectedCinema.city}` : ''}</dd></div></div><div className="flex items-start gap-3"><Phone size={17} className="mt-0.5 shrink-0 text-zinc-600" /><div><dt className="text-xs font-bold text-zinc-600">Đường dây nóng</dt><dd className="mt-1 text-zinc-300">{selectedCinema.hotline || 'Chưa cập nhật'}</dd></div></div></dl></article>
        <article className="rounded-2xl border border-sky-500/20 bg-sky-500/[0.06] p-6"><div className="flex items-center gap-2 text-sky-300"><Info size={18} /><h2 className="font-black">Bạn được phép làm gì?</h2></div><ul className="mt-4 space-y-3 text-sm leading-6 text-sky-100/70"><li>• Xem lịch chiếu và phòng chiếu của rạp này.</li><li>• Cập nhật giờ mở cửa phục vụ vận hành.</li><li>• Không xem hoặc thay đổi dữ liệu của rạp khác.</li></ul></article>
      </section>

      <CinemaOperatingHours operatingHours={hours} onHoursChange={updateHour} />

      <div className="sticky bottom-4 flex flex-col gap-3 rounded-2xl border border-white/10 bg-[#111114]/95 p-4 shadow-2xl backdrop-blur md:flex-row md:items-center md:justify-between"><div className="flex items-center gap-2 text-sm"><Clock3 size={17} className={changed ? 'text-amber-400' : 'text-emerald-400'} /><span className={changed ? 'text-amber-200' : 'text-zinc-500'}>{changed ? 'Có thay đổi giờ mở cửa chưa được lưu.' : 'Giờ mở cửa đã được cập nhật.'}</span>{saveState.error ? <span className="text-red-300">{saveState.error}</span> : null}{saveState.success ? <span className="text-emerald-300">{saveState.success}</span> : null}</div><button type="button" disabled={!changed || saveState.saving} onClick={save} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-orange px-5 text-sm font-black text-black hover:bg-orange-500 disabled:opacity-40"><Save size={17} /> {saveState.saving ? 'Đang lưu…' : 'Lưu giờ mở cửa'}</button></div>
    </div>
  );
}
