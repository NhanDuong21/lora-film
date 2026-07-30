import { useCallback, useEffect, useMemo, useState } from 'react';
import { Check, ExternalLink, Loader2, Save, Search, Tags } from 'lucide-react';
import { Link, useOutletContext } from 'react-router-dom';
import adminGenreService from '@/features/catalog/admin/services/adminGenreService';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import { AsyncState } from '@/components/common/ui/uiKit';
import { parseApiError } from '@/utils/apiErrorHandler';

const getGenreId = genre => genre?.publicId || genre?.id;
const getGenreName = genre => (typeof genre === 'string' ? genre : genre?.name);
const isGenreActive = genre => (genre?.status || 'ACTIVE') === 'ACTIVE';

export default function MovieGenreTab({ movie, onUpdate }) {
  const { triggerToast } = useOutletContext() || {};
  const [genres, setGenres] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  const fetchGenres = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      const response = await adminGenreService.getAllGenres();
      const data = response?.data?.content || response?.data?.data || response?.data || [];
      if (!response?.success || !Array.isArray(data)) {
        throw new Error('Danh sách thể loại chưa đúng định dạng.');
      }
      setGenres(data.filter(genre => getGenreId(genre)));
    } catch (fetchError) {
      setError(parseApiError(fetchError));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchGenres();
  }, [fetchGenres]);

  const originalIds = useMemo(() => (
    (movie?.genres || [])
      .map(name => genres.find(genre => getGenreName(genre) === getGenreName(name)))
      .map(getGenreId)
      .filter(Boolean)
  ), [genres, movie?.genres]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setSelectedIds(originalIds);
  }, [originalIds]);

  const assignableGenres = useMemo(
    () => genres.filter(genre => isGenreActive(genre) || originalIds.includes(getGenreId(genre))),
    [genres, originalIds],
  );

  const filteredGenres = useMemo(() => {
    const normalized = searchTerm.trim().toLocaleLowerCase('vi');
    if (!normalized) return assignableGenres;
    return assignableGenres.filter(genre => getGenreName(genre)?.toLocaleLowerCase('vi').includes(normalized));
  }, [assignableGenres, searchTerm]);

  const inactiveSelectedCount = useMemo(
    () => selectedIds.filter(id => {
      const genre = genres.find(item => getGenreId(item) === id);
      return genre && !isGenreActive(genre);
    }).length,
    [genres, selectedIds],
  );

  const isDirty = selectedIds.length !== originalIds.length
    || selectedIds.some(id => !originalIds.includes(id));

  const toggleGenre = id => {
    setSelectedIds(current => (
      current.includes(id) ? current.filter(item => item !== id) : [...current, id]
    ));
  };

  const handleSave = async () => {
    setIsSaving(true);
    try {
      const response = await adminMovieService.assignGenres(movie.publicId, selectedIds);
      if (!response?.success) throw new Error('Không thể cập nhật thể loại.');
      triggerToast?.('Đã lưu thể loại cho phim.');
      onUpdate?.();
    } catch (saveError) {
      triggerToast?.(parseApiError(saveError), 'error');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <AsyncState isLoading={isLoading} error={error} onRetry={fetchGenres}>
        <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
          <div>
            <h3 className="flex items-center gap-2 text-base font-bold text-white">
              <Tags className="h-4 w-4 text-orange-400" />
              Phim thuộc thể loại nào?
            </h3>
            <p className="mt-1 text-sm text-zinc-500">
              Chọn một hoặc nhiều thể loại để khách hàng dễ tìm thấy phim.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Link
              to="/admin/genres"
              className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 px-3 py-2 text-xs font-bold text-zinc-300 hover:bg-zinc-800"
            >
              Quản lý danh mục
              <ExternalLink className="h-3.5 w-3.5" />
            </Link>
            <button
              type="button"
              onClick={handleSave}
              disabled={isSaving || !isDirty}
              className="inline-flex items-center gap-2 rounded-xl bg-orange-500 px-4 py-2 text-xs font-black text-zinc-950 transition hover:bg-orange-400 disabled:cursor-not-allowed disabled:opacity-40"
            >
              {isSaving ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Save className="h-3.5 w-3.5" />}
              {isSaving ? 'Đang lưu…' : 'Lưu lựa chọn'}
            </button>
          </div>
        </div>

        <div className="rounded-xl border border-zinc-800 bg-zinc-900/35 p-4">
          <p className="text-xs font-bold uppercase tracking-wide text-zinc-500">
            Đã chọn {selectedIds.length} thể loại
          </p>
          <div className="mt-3 flex min-h-10 flex-wrap gap-2">
            {selectedIds.length > 0 ? (
              selectedIds.map(id => {
                const genre = genres.find(item => getGenreId(item) === id);
                const active = isGenreActive(genre);
                return (
                  <span
                    key={id}
                    className={`rounded-lg border px-3 py-1.5 text-xs font-semibold ${
                      active
                        ? 'border-orange-500/30 bg-orange-500/10 text-orange-200'
                        : 'border-zinc-700 bg-zinc-800/70 text-zinc-400'
                    }`}
                  >
                    {getGenreName(genre) || 'Thể loại không xác định'}
                    {!active && ' · Ngừng sử dụng'}
                  </span>
                );
              })
            ) : (
              <span className="text-sm italic text-zinc-600">Chưa chọn thể loại nào.</span>
            )}
          </div>
          {inactiveSelectedCount > 0 && (
            <p className="mt-3 text-xs leading-5 text-amber-300">
              Có {inactiveSelectedCount} thể loại đã ngừng sử dụng. Bạn có thể bỏ chọn, nhưng không thể gắn lại sau khi lưu.
            </p>
          )}
        </div>

        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-600" />
          <input
            type="search"
            value={searchTerm}
            onChange={event => setSearchTerm(event.target.value)}
            placeholder="Tìm thể loại…"
            aria-label="Tìm thể loại"
            className="h-11 w-full rounded-xl border border-zinc-800 bg-zinc-950 pl-10 pr-4 text-sm text-zinc-100 outline-none placeholder:text-zinc-600 focus:border-orange-500/60"
          />
        </div>

        {filteredGenres.length > 0 ? (
          <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {filteredGenres.map(genre => {
              const id = getGenreId(genre);
              const selected = selectedIds.includes(id);
              const active = isGenreActive(genre);
              return (
                <button
                  key={id}
                  type="button"
                  aria-pressed={selected}
                  onClick={() => toggleGenre(id)}
                  className={`flex min-h-12 items-center justify-between gap-3 rounded-xl border px-3 py-2.5 text-left text-sm font-semibold transition ${
                    selected
                      ? active
                        ? 'border-orange-500 bg-orange-500/10 text-orange-200'
                        : 'border-zinc-600 bg-zinc-800/70 text-zinc-300'
                      : 'border-zinc-800 bg-zinc-950/40 text-zinc-400 hover:border-zinc-700 hover:text-zinc-200'
                  }`}
                >
                  <span className="min-w-0">
                    <span className="block truncate">{getGenreName(genre)}</span>
                    {!active && (
                      <span className="mt-0.5 block text-[10px] font-medium text-zinc-500">Đã ngừng sử dụng</span>
                    )}
                  </span>
                  {selected && <Check className="h-4 w-4 shrink-0 text-orange-400" />}
                </button>
              );
            })}
          </div>
        ) : (
          <div className="rounded-xl border border-dashed border-zinc-800 p-8 text-center text-sm text-zinc-600">
            Không tìm thấy thể loại phù hợp.
          </div>
        )}
      </AsyncState>
    </div>
  );
}
