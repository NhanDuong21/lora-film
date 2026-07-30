import { useState } from 'react';
import { CalendarDays, Clock3, Globe2, Pencil, Shield, Tag, Text, Type } from 'lucide-react';
import { useOutletContext } from 'react-router-dom';
import { getStatusConfig } from '@/features/catalog/admin/config/movieStatusConfig';
import { formatDate, AGE_RATING_LABELS } from '@/utils/movieHelpers';
import MovieFormModal from '../../components/MovieFormModal';

function InfoItem({ icon: Icon, label, value, emptyText = 'Chưa có thông tin' }) {
  return (
    <div className="flex items-start gap-3">
      <Icon className="mt-0.5 h-4 w-4 shrink-0 text-zinc-600" />
      <div className="min-w-0">
        <p className="text-xs text-zinc-500">{label}</p>
        <p className={`mt-1 break-words text-sm ${value ? 'text-zinc-100' : 'italic text-zinc-600'}`}>
          {value || emptyText}
        </p>
      </div>
    </div>
  );
}

export default function MovieOverviewTab({ movie, onUpdate }) {
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const { triggerToast } = useOutletContext() || {};

  if (!movie) return null;

  const status = getStatusConfig(movie.status);

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
        <div>
          <h3 className="text-base font-bold text-white">Thông tin cơ bản</h3>
          <p className="mt-1 text-sm text-zinc-500">
            Đây là thông tin khách hàng nhìn thấy khi tìm và chọn phim.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setIsEditModalOpen(true)}
          className="inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-700 bg-zinc-800 px-4 py-2.5 text-sm font-bold text-zinc-200 transition hover:bg-zinc-700"
        >
          <Pencil className="h-4 w-4" />
          Sửa thông tin
        </button>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <section className="rounded-2xl border border-zinc-800 bg-zinc-900/35 p-5">
          <h4 className="text-sm font-bold text-orange-300">Nhận diện phim</h4>
          <div className="mt-5 grid gap-5 sm:grid-cols-2">
            <InfoItem icon={Type} label="Tên hiển thị" value={movie.title} />
            <InfoItem icon={Text} label="Tên gốc" value={movie.originalTitle} />
            <InfoItem icon={Globe2} label="Quốc gia sản xuất" value={movie.country} />
            <InfoItem
              icon={Tag}
              label="Đường dẫn nội bộ"
              value={movie.slug || movie.activeSlug}
              emptyText="Hệ thống sẽ tự tạo"
            />
          </div>
        </section>

        <section className="rounded-2xl border border-zinc-800 bg-zinc-900/35 p-5">
          <h4 className="text-sm font-bold text-orange-300">Lịch và phân loại</h4>
          <div className="mt-5 grid gap-5 sm:grid-cols-2">
            <InfoItem
              icon={CalendarDays}
              label="Ngày khởi chiếu"
              value={movie.releaseDate ? formatDate(movie.releaseDate) : ''}
            />
            <InfoItem
              icon={CalendarDays}
              label="Ngày ngừng chiếu"
              value={movie.endDate ? formatDate(movie.endDate) : ''}
            />
            <InfoItem
              icon={Clock3}
              label="Thời lượng"
              value={movie.durationMinutes ? `${movie.durationMinutes} phút` : ''}
            />
            <InfoItem
              icon={Shield}
              label="Phân loại độ tuổi"
              value={movie.ageRating ? (AGE_RATING_LABELS[movie.ageRating] || movie.ageRating) : ''}
            />
          </div>
          <div className="mt-5 border-t border-zinc-800 pt-4">
            <p className="text-xs text-zinc-500">Trạng thái phục vụ</p>
            <span className={`mt-2 inline-flex rounded-md border px-2.5 py-1 text-xs font-black uppercase tracking-wide ${status.colorClass}`}>
              {status.label}
            </span>
          </div>
        </section>
      </div>

      <section className="rounded-2xl border border-zinc-800 bg-zinc-900/35 p-5">
        <h4 className="text-sm font-bold text-orange-300">Tóm tắt nội dung</h4>
        {movie.synopsis ? (
          <p className="mt-4 whitespace-pre-wrap text-sm leading-7 text-zinc-300">{movie.synopsis}</p>
        ) : (
          <p className="mt-4 rounded-xl border border-dashed border-zinc-700 bg-zinc-950/50 p-4 text-sm italic text-zinc-600">
            Chưa có tóm tắt. Nên bổ sung để khách hàng hiểu nhanh nội dung phim.
          </p>
        )}
      </section>

      <details className="rounded-xl border border-zinc-800 bg-zinc-950/50">
        <summary className="cursor-pointer list-none px-4 py-3 text-xs font-semibold text-zinc-500 hover:text-zinc-300">
          Thông tin kỹ thuật
        </summary>
        <div className="border-t border-zinc-800 px-4 py-3 text-xs text-zinc-600">
          <span className="mr-2">Slug:</span>
          <code>{movie.slug || movie.activeSlug || 'Chưa có'}</code>
        </div>
      </details>

      {isEditModalOpen && (
        <MovieFormModal
          selectedMovie={movie}
          triggerToast={triggerToast}
          onClose={() => setIsEditModalOpen(false)}
          onRefreshList={() => {
            setIsEditModalOpen(false);
            onUpdate?.();
          }}
        />
      )}
    </div>
  );
}
