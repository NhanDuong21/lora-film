import { useState } from 'react';
import { Save } from 'lucide-react';
import CinemaOperatingHours from '../../components/CinemaOperatingHours';

const normalizeTimeForApi = (value) => {
  if (!value) return null;
  if (value === '24:00' || value === '24:00:00') return '23:59:59';
  return value.length === 5 ? `${value}:00` : value;
};

export default function CinemaOperatingHoursTab({ cinema, onUpdate }) {
  const [hours, setHours] = useState(() => createInitialHours(cinema));
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleChange = (index, field, value) => {
    setHours((previous) =>
      previous.map((item, currentIndex) =>
        currentIndex === index ? { ...item, [field]: value } : item,
      ),
    );
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setIsSubmitting(true);
    await onUpdate(
      hours.map((item) => ({
        dayOfWeek: item.dayOfWeek,
        openTime: item.isClosed ? null : normalizeTimeForApi(item.openTime),
        closeTime: item.isClosed ? null : normalizeTimeForApi(item.closeTime),
        isClosed: item.isClosed,
      })),
    );
    setIsSubmitting(false);
  };

  return (
    <form onSubmit={handleSubmit} className="mx-auto max-w-4xl space-y-6 pb-20">
      <CinemaOperatingHours operatingHours={hours} onHoursChange={handleChange} />
      <div className="flex justify-end">
        <button
          type="submit"
          disabled={isSubmitting}
          className="flex items-center gap-2 rounded-xl bg-orange-500 px-7 py-3 text-xs font-black uppercase text-zinc-950 disabled:opacity-50"
        >
          <Save className="h-4 w-4" />
          {isSubmitting ? 'Đang lưu...' : 'Lưu giờ hoạt động'}
        </button>
      </div>
    </form>
  );
}

function createInitialHours(cinema) {
  const currentHours = cinema?.operatingHours || [];
  return Array.from({ length: 7 }, (_, index) => {
    const dayOfWeek = index + 1;
    const existing = currentHours.find((item) => item.dayOfWeek === dayOfWeek);
    return {
      dayOfWeek,
      openTime: existing?.openTime?.substring(0, 5) || '08:00',
      closeTime: existing?.closeTime?.substring(0, 5) || '23:30',
      isClosed: existing?.isClosed ?? false,
    };
  });
}
