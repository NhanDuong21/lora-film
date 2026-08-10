import { useEffect, useState } from 'react';
import { CheckCircle2, CloudDownload, Film, Loader2, Search, X } from 'lucide-react';
import adminTmdbService from '@/features/catalog/admin/services/adminTmdbService';
import { formatDate } from '@/utils/movieHelpers';

const STATUS_LABELS = {
  DRAFT: 'Chờ hoàn thiện',
  UPCOMING: 'Sắp chiếu',
  NOW_SHOWING: 'Đang chiếu',
  ENDED: 'Đã kết thúc',
  INACTIVE: 'Tạm ngừng',
};

const readError = error => {
  const message = error?.response?.data?.message
    || error?.response?.data?.error
    || error?.message;
  if (!message) return 'Không thể tìm kiếm phim trên nguồn TMDB. Vui lòng thử lại.';
  if (/connection|network|socket|econn|before response|failed to fetch/i.test(message)) {
    return 'Kết nối tới nguồn TMDB đang bị gián đoạn. Vui lòng kiểm tra dịch vụ rồi thử lại.';
  }
  if (/timeout|timed out/i.test(message)) {
    return 'Nguồn TMDB phản hồi quá chậm. Vui lòng thử lại sau.';
  }
  if (/not found|404/i.test(message)) {
    return 'Chức năng tìm phim theo tên chưa sẵn sàng. Vui lòng khởi động lại nguồn TMDB và Movie Service.';
  }
  return message;
};

const releaseYear = date => date?.slice(0, 4) || 'Chưa rõ năm';

export default function TmdbMovieSearchImport() {
  const [query, setQuery] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const [selectedMovie, setSelectedMovie] = useState(null);
  const [searching, setSearching] = useState(false);
  const [importing, setImporting] = useState(false);
  const [message, setMessage] = useState(null);

  useEffect(() => {
    const normalizedQuery = query.trim();
    if (selectedMovie && normalizedQuery === selectedMovie.title) {
      return undefined;
    }
    if (normalizedQuery.length < 2) {
      return undefined;
    }

    const controller = new AbortController();
    const timer = setTimeout(async () => {
      setSearching(true);
      setMessage(null);
      try {
        setSuggestions(await adminTmdbService.searchMovies(normalizedQuery, controller.signal));
      } catch (error) {
        if (error?.code !== 'ERR_CANCELED') {
          setSuggestions([]);
          setMessage({ type: 'error', text: readError(error) });
        }
      } finally {
        if (!controller.signal.aborted) setSearching(false);
      }
    }, 350);

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [query, selectedMovie]);

  const selectMovie = movie => {
    setSelectedMovie(movie);
    setQuery(movie.title);
    setSuggestions([]);
    setSearching(false);
    setMessage(null);
  };

  const clearSelection = () => {
    setSelectedMovie(null);
    setQuery('');
    setSuggestions([]);
    setSearching(false);
    setMessage(null);
  };

  const importSelectedMovie = async () => {
    if (!selectedMovie || selectedMovie.alreadyImported) return;
    setImporting(true);
    setMessage(null);
    try {
      const response = await adminTmdbService.syncMovieById(selectedMovie.tmdbId);
      setSelectedMovie(current => ({ ...current, alreadyImported: true, localMovieStatus: 'DRAFT' }));
      setMessage({
        type: 'success',
        text: response?.data || 'Đã nhập phim vào danh sách Chờ hoàn thiện.',
      });
    } catch (error) {
      setMessage({ type: 'error', text: readError(error) });
    } finally {
      setImporting(false);
    }
  };

  return (
    <section className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4">
      <h3 className="text-sm font-bold text-white">Tìm và nhập một phim từ TMDB</h3>
      <p className="mt-1 text-xs leading-5 text-zinc-500">
        Nhập tên phim, chọn đúng kết quả rồi đưa phim vào danh sách Chờ hoàn thiện.
      </p>

      <div className="relative mt-3">
        <label className="relative block">
          {searching
            ? <Loader2 className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 animate-spin text-orange-300" />
            : <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-600" />}
          <input
            type="search"
            value={query}
            onChange={event => {
              const nextQuery = event.target.value;
              setQuery(nextQuery);
              setSelectedMovie(null);
              setSuggestions([]);
              setSearching(nextQuery.trim().length >= 2);
              setMessage(null);
            }}
            placeholder="Nhập tên phim, ví dụ: Avatar"
            aria-label="Tên phim cần tìm trên TMDB"
            aria-autocomplete="list"
            aria-expanded={suggestions.length > 0}
            className="h-11 w-full rounded-xl border border-zinc-800 bg-zinc-900 pl-10 pr-10 text-sm text-zinc-200 outline-none placeholder:text-zinc-600 focus:border-orange-500"
          />
          {query && (
            <button
              type="button"
              onClick={clearSelection}
              aria-label="Xóa nội dung tìm kiếm"
              className="absolute right-2 top-1/2 -translate-y-1/2 rounded-lg p-1.5 text-zinc-500 hover:bg-zinc-800 hover:text-zinc-200"
            >
              <X className="h-4 w-4" />
            </button>
          )}
        </label>

        {suggestions.length > 0 && (
          <div
            role="listbox"
            aria-label="Kết quả tìm phim từ TMDB"
            className="absolute z-30 mt-2 max-h-96 w-full overflow-y-auto rounded-xl border border-zinc-700 bg-zinc-950 p-1.5 shadow-2xl"
          >
            {suggestions.map(movie => (
              <button
                key={movie.tmdbId}
                type="button"
                role="option"
                aria-selected={selectedMovie?.tmdbId === movie.tmdbId}
                onClick={() => selectMovie(movie)}
                className="flex w-full items-center gap-3 rounded-lg p-2 text-left hover:bg-zinc-900"
              >
                {movie.posterUrl ? (
                  <img src={movie.posterUrl} alt="" className="h-16 w-11 rounded-md object-cover" />
                ) : (
                  <span className="flex h-16 w-11 shrink-0 items-center justify-center rounded-md bg-zinc-800 text-zinc-600">
                    <Film className="h-5 w-5" />
                  </span>
                )}
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-semibold text-zinc-100">
                    {movie.title} <span className="font-normal text-zinc-500">({releaseYear(movie.originalReleaseDate)})</span>
                  </span>
                  {movie.originalTitle && movie.originalTitle !== movie.title && (
                    <span className="mt-0.5 block truncate text-xs text-zinc-500">Tên gốc: {movie.originalTitle}</span>
                  )}
                  <span className="mt-1 block text-[11px] text-zinc-600">Mã TMDB: {movie.tmdbId}</span>
                </span>
                {movie.alreadyImported && (
                  <span className="shrink-0 rounded-full bg-emerald-500/10 px-2 py-1 text-[10px] font-bold text-emerald-300">
                    Đã có trong hệ thống
                  </span>
                )}
              </button>
            ))}
          </div>
        )}

        {!searching && query.trim().length >= 2 && !selectedMovie && suggestions.length === 0 && !message && (
          <p className="mt-2 text-xs text-zinc-600">Không tìm thấy phim phù hợp.</p>
        )}
      </div>

      {selectedMovie && (
        <div className="mt-3 flex flex-col gap-3 rounded-xl border border-orange-500/25 bg-orange-500/[0.05] p-3 sm:flex-row sm:items-center">
          {selectedMovie.posterUrl ? (
            <img src={selectedMovie.posterUrl} alt="" className="h-20 w-14 rounded-lg object-cover" />
          ) : (
            <span className="flex h-20 w-14 shrink-0 items-center justify-center rounded-lg bg-zinc-800 text-zinc-600">
              <Film className="h-6 w-6" />
            </span>
          )}
          <div className="min-w-0 flex-1">
            <p className="font-bold text-white">{selectedMovie.title}</p>
            <p className="mt-1 text-xs text-zinc-500">
              Phát hành gốc: {selectedMovie.originalReleaseDate ? formatDate(selectedMovie.originalReleaseDate) : 'Chưa có thông tin'}
            </p>
            {selectedMovie.alreadyImported && (
              <p className="mt-1 flex items-center gap-1 text-xs text-emerald-300">
                <CheckCircle2 className="h-3.5 w-3.5" />
                Phim đã có trong hệ thống · {STATUS_LABELS[selectedMovie.localMovieStatus] || 'Đã được nhập'}
              </p>
            )}
          </div>
          <button
            type="button"
            onClick={importSelectedMovie}
            disabled={importing || selectedMovie.alreadyImported}
            className="inline-flex h-10 shrink-0 items-center justify-center gap-2 rounded-xl border border-orange-500/40 bg-orange-500/10 px-4 text-xs font-bold text-orange-200 hover:bg-orange-500/20 disabled:cursor-not-allowed disabled:border-zinc-700 disabled:bg-zinc-800 disabled:text-zinc-500"
          >
            {importing ? <Loader2 className="h-4 w-4 animate-spin" /> : <CloudDownload className="h-4 w-4" />}
            {selectedMovie.alreadyImported ? 'Phim đã được nhập' : 'Nhập phim đã chọn'}
          </button>
        </div>
      )}

      {message && (
        <p className={`mt-3 rounded-xl border px-3 py-2 text-xs ${
          message.type === 'error'
            ? 'border-red-500/30 bg-red-500/10 text-red-200'
            : 'border-emerald-500/30 bg-emerald-500/10 text-emerald-200'
        }`}>
          {message.text}
        </p>
      )}
    </section>
  );
}
