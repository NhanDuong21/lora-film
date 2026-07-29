import { Calendar, Copy, MoonStar } from 'lucide-react';
import { getHoursDescription, isOvernight } from '../utils/facilityPresentation';

const DAY_NAMES = [
  'Thứ Hai',
  'Thứ Ba',
  'Thứ Tư',
  'Thứ Năm',
  'Thứ Sáu',
  'Thứ Bảy',
  'Chủ Nhật',
];

export default function CinemaOperatingHours({ operatingHours, onHoursChange }) {
  const copyMonday = (lastIndex) => {
    const monday = operatingHours[0];
    if (!monday) return;
    for (let index = 1; index <= lastIndex; index += 1) {
      onHoursChange(index, 'isClosed', monday.isClosed);
      onHoursChange(index, 'openTime', monday.openTime);
      onHoursChange(index, 'closeTime', monday.closeTime);
    }
  };

  return (
    <section className="flex flex-col gap-4 rounded-2xl border border-zinc-800 bg-zinc-900/60 p-6">
      <div className="flex items-start gap-2 border-b border-zinc-800 pb-3">
        <Calendar className="mt-0.5 h-4 w-4 text-orange-500" />
        <div>
          <h2 className="text-sm font-bold uppercase tracking-wider text-white">
            Giờ mở cửa hằng tuần
          </h2>
          <p className="mt-1 text-xs text-zinc-500">
            Giờ kết thúc nhỏ hơn giờ mở cửa sẽ được hiểu là kết thúc vào ngày hôm sau.
          </p>
        </div>
      </div>

      <div className="flex flex-wrap justify-end gap-2">
        <button
          type="button"
          onClick={() => copyMonday(4)}
          className="flex items-center gap-2 rounded-lg border border-zinc-800 bg-zinc-950 px-3 py-2 text-[10px] font-bold uppercase tracking-wider text-orange-400 hover:bg-zinc-800"
        >
          <Copy className="h-3.5 w-3.5" />
          Sao chép Thứ Hai cho ngày thường
        </button>
        <button
          type="button"
          onClick={() => copyMonday(6)}
          className="flex items-center gap-2 rounded-lg border border-zinc-800 bg-zinc-950 px-3 py-2 text-[10px] font-bold uppercase tracking-wider text-orange-400 hover:bg-zinc-800"
        >
          <Copy className="h-3.5 w-3.5" />
          Sao chép cho cả tuần
        </button>
      </div>

      <div className="flex flex-col gap-3">
        {DAY_NAMES.map((dayName, index) => {
          const hours = operatingHours[index];
          if (!hours) return null;
          const overnight = !hours.isClosed && isOvernight(hours.openTime, hours.closeTime);

          return (
            <div
              key={dayName}
              className="rounded-xl border border-zinc-900 bg-zinc-950 p-3"
            >
              <div className="flex flex-col justify-between gap-3 md:flex-row md:items-center">
                <label className="flex min-w-40 items-center gap-3">
                  <input
                    type="checkbox"
                    checked={!hours.isClosed}
                    onChange={(event) => onHoursChange(index, 'isClosed', !event.target.checked)}
                    className="h-4 w-4 cursor-pointer rounded border-zinc-800 bg-zinc-900 accent-orange-500"
                  />
                  <span className="text-xs font-bold text-zinc-300">{dayName}</span>
                  <span className={`text-[10px] font-bold ${hours.isClosed ? 'text-zinc-500' : 'text-emerald-400'}`}>
                    {hours.isClosed ? 'Đóng cửa' : 'Mở cửa'}
                  </span>
                </label>

                {!hours.isClosed && (
                  <div className="flex items-center gap-2">
                    <input
                      aria-label={`Giờ mở cửa ${dayName}`}
                      type="time"
                      value={hours.openTime || ''}
                      onChange={(event) => onHoursChange(index, 'openTime', event.target.value)}
                      className="w-28 rounded-lg border border-zinc-800 bg-zinc-900 px-2 py-2 text-center text-xs text-zinc-100 outline-none focus:border-orange-500/50"
                    />
                    <span className="text-xs text-zinc-600">đến</span>
                    <input
                      aria-label={`Giờ đóng cửa ${dayName}`}
                      type="time"
                      value={hours.closeTime || ''}
                      onChange={(event) => onHoursChange(index, 'closeTime', event.target.value)}
                      className="w-28 rounded-lg border border-zinc-800 bg-zinc-900 px-2 py-2 text-center text-xs text-zinc-100 outline-none focus:border-orange-500/50"
                    />
                  </div>
                )}
              </div>

              <div className={`mt-2 flex items-center gap-2 text-[11px] ${overnight ? 'text-amber-300' : 'text-zinc-500'}`}>
                {overnight && <MoonStar className="h-3.5 w-3.5" />}
                <span>{getHoursDescription(hours)}</span>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
