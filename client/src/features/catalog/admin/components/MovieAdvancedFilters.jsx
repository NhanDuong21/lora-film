import { useEffect, useMemo, useState } from 'react';
import { RotateCcw, X } from 'lucide-react';
import {
  ADMIN_MOVIE_QUERY_DEFAULTS,
  ADVANCED_FILTER_KEYS,
  countAdvancedFilters,
} from '@/features/catalog/admin/utils/adminMovieQuery';

const FILTER_LABELS = {
  source: { TMDB: 'TMDB', MANUAL: 'Thủ công' },
  healthStatus: { READY: 'Sẵn sàng', WARNING: 'Cần kiểm tra', BLOCKED: 'Bị chặn' },
  hasPrimaryPoster: { true: 'Có poster chính', false: 'Thiếu poster chính' },
  hasActiveVersion: { true: 'Có phiên bản hoạt động', false: 'Thiếu phiên bản hoạt động' },
  hasShowtime: { true: 'Có lịch chiếu', false: 'Chưa có lịch chiếu' },
};

const inputClass = 'w-full rounded-xl border border-zinc-800 bg-[#050506] px-3 py-2.5 text-xs text-zinc-100 outline-none transition-colors focus:border-amber-500/50';

function FilterField({ label, children }) {
  return (
    <label className="space-y-1.5 text-[11px] font-bold uppercase tracking-wider text-zinc-400">
      <span>{label}</span>
      {children}
    </label>
  );
}

export default function MovieAdvancedFilters({ query, genres, isOpen, onApply, onReset, onRemove }) {
  const [draft, setDraft] = useState(query);

  useEffect(() => {
    // Browser history changes replace the filter draft with committed URL state.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setDraft(query);
  }, [query]);

  const genreNames = useMemo(
    () => new Map(genres.map(genre => [genre.publicId, genre.name])),
    [genres]
  );

  const chips = useMemo(() => {
    const items = [];
    ['source', 'healthStatus', 'hasPrimaryPoster', 'hasActiveVersion', 'hasShowtime'].forEach(key => {
      if (query[key]) items.push({ key, label: FILTER_LABELS[key][query[key]] || query[key] });
    });
    if (query.genrePublicId) items.push({ key: 'genrePublicId', label: genreNames.get(query.genrePublicId) || 'Thể loại đã chọn' });
    if (query.country) items.push({ key: 'country', label: `Quốc gia: ${query.country}` });
    if (query.releaseDateFrom || query.releaseDateTo) {
      items.push({ key: 'releaseDateRange', label: `Khai thác: ${query.releaseDateFrom || '…'} → ${query.releaseDateTo || '…'}` });
    }
    if (query.tmdbUpdatedFrom || query.tmdbUpdatedTo) {
      items.push({ key: 'tmdbUpdatedRange', label: `TMDB: ${query.tmdbUpdatedFrom || '…'} → ${query.tmdbUpdatedTo || '…'}` });
    }
    return items;
  }, [genreNames, query]);

  const updateDraft = (key, value) => setDraft(current => ({ ...current, [key]: value }));

  const resetDraft = () => {
    const reset = { ...draft, sort: ADMIN_MOVIE_QUERY_DEFAULTS.sort };
    ADVANCED_FILTER_KEYS.forEach(key => { reset[key] = ''; });
    setDraft(reset);
    onReset();
  };

  return (
    <div className="space-y-3">
      {isOpen && (
        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-4 shadow-xl">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <FilterField label="Nguồn">
              <select className={inputClass} value={draft.source} onChange={event => updateDraft('source', event.target.value)}>
                <option value="">Tất cả nguồn</option>
                <option value="TMDB">TMDB</option>
                <option value="MANUAL">Thủ công</option>
              </select>
            </FilterField>
            <FilterField label="Tình trạng dữ liệu">
              <select className={inputClass} value={draft.healthStatus} onChange={event => updateDraft('healthStatus', event.target.value)}>
                <option value="">Tất cả</option>
                <option value="READY">Sẵn sàng</option>
                <option value="WARNING">Cần kiểm tra</option>
                <option value="BLOCKED">Bị chặn</option>
              </select>
            </FilterField>
            <FilterField label="Poster chính">
              <select className={inputClass} value={draft.hasPrimaryPoster} onChange={event => updateDraft('hasPrimaryPoster', event.target.value)}>
                <option value="">Tất cả</option>
                <option value="true">Có poster chính</option>
                <option value="false">Thiếu poster chính</option>
              </select>
            </FilterField>
            <FilterField label="Phiên bản hoạt động">
              <select className={inputClass} value={draft.hasActiveVersion} onChange={event => updateDraft('hasActiveVersion', event.target.value)}>
                <option value="">Tất cả</option>
                <option value="true">Có phiên bản</option>
                <option value="false">Thiếu phiên bản</option>
              </select>
            </FilterField>
            <FilterField label="Lịch chiếu">
              <select className={inputClass} value={draft.hasShowtime} onChange={event => updateDraft('hasShowtime', event.target.value)}>
                <option value="">Tất cả</option>
                <option value="true">Có lịch chiếu</option>
                <option value="false">Chưa có lịch chiếu</option>
              </select>
            </FilterField>
            <FilterField label="Thể loại">
              <select className={inputClass} value={draft.genrePublicId} onChange={event => updateDraft('genrePublicId', event.target.value)}>
                <option value="">Tất cả thể loại</option>
                {genres.map(genre => (
                  <option key={genre.publicId} value={genre.publicId}>
                    {genre.name}{genre.status === 'INACTIVE' ? ' (Ngừng sử dụng)' : ''}
                  </option>
                ))}
              </select>
            </FilterField>
            <FilterField label="Quốc gia">
              <input className={inputClass} value={draft.country} onChange={event => updateDraft('country', event.target.value)} placeholder="Ví dụ: Vietnam" />
            </FilterField>
            <FilterField label="Sắp xếp">
              <select className={inputClass} value={draft.sort} onChange={event => updateDraft('sort', event.target.value)}>
                <option value="releaseDate,desc">Ngày khai thác mới nhất</option>
                <option value="releaseDate,asc">Ngày khai thác cũ nhất</option>
                <option value="updatedAt,desc">Cập nhật gần nhất</option>
                <option value="tmdbLastUpdated,desc">TMDB cập nhật gần nhất</option>
                <option value="createdAt,desc">Tạo gần nhất</option>
                <option value="title,asc">Tên A–Z</option>
                <option value="title,desc">Tên Z–A</option>
              </select>
            </FilterField>
          </div>

          <div className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-2">
            <fieldset className="rounded-xl border border-zinc-800 p-3">
              <legend className="px-1 text-[11px] font-bold uppercase tracking-wider text-zinc-400">Ngày bắt đầu khai thác</legend>
              <div className="grid grid-cols-2 gap-3">
                <input aria-label="Ngày bắt đầu khai thác từ" type="date" className={inputClass} value={draft.releaseDateFrom} onChange={event => updateDraft('releaseDateFrom', event.target.value)} />
                <input aria-label="Ngày bắt đầu khai thác đến" type="date" className={inputClass} value={draft.releaseDateTo} onChange={event => updateDraft('releaseDateTo', event.target.value)} />
              </div>
            </fieldset>
            <fieldset className="rounded-xl border border-zinc-800 p-3">
              <legend className="px-1 text-[11px] font-bold uppercase tracking-wider text-zinc-400">Ngày cập nhật TMDB</legend>
              <div className="grid grid-cols-2 gap-3">
                <input aria-label="Ngày TMDB cập nhật từ" type="date" className={inputClass} value={draft.tmdbUpdatedFrom} onChange={event => updateDraft('tmdbUpdatedFrom', event.target.value)} />
                <input aria-label="Ngày TMDB cập nhật đến" type="date" className={inputClass} value={draft.tmdbUpdatedTo} onChange={event => updateDraft('tmdbUpdatedTo', event.target.value)} />
              </div>
            </fieldset>
          </div>

          <div className="mt-4 flex flex-wrap justify-end gap-2">
            <button type="button" onClick={resetDraft} className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 px-4 py-2 text-xs font-bold text-zinc-300 hover:bg-zinc-800">
              <RotateCcw className="h-3.5 w-3.5" /> Đặt lại
            </button>
            <button type="button" onClick={() => onApply(draft)} className="rounded-xl bg-amber-500 px-5 py-2 text-xs font-black text-zinc-950 hover:bg-amber-400">
              Áp dụng
            </button>
          </div>
        </div>
      )}

      {chips.length > 0 && (
        <div className="flex flex-wrap items-center gap-2" aria-label={`${countAdvancedFilters(query)} bộ lọc đang áp dụng`}>
          {chips.map(chip => (
            <span key={chip.key} className="inline-flex items-center gap-1.5 rounded-full border border-amber-500/30 bg-amber-500/10 px-3 py-1 text-[11px] font-bold text-amber-300">
              {chip.label}
              <button type="button" onClick={() => onRemove(chip.key)} aria-label={`Xóa bộ lọc ${chip.label}`} className="rounded-full hover:bg-amber-500/20">
                <X className="h-3 w-3" />
              </button>
            </span>
          ))}
          <button type="button" onClick={onReset} className="text-[11px] font-bold text-zinc-400 underline hover:text-zinc-200">Xóa tất cả</button>
        </div>
      )}
    </div>
  );
}
