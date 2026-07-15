import { useState, useEffect, useCallback, useRef } from 'react';
import {
  Search, Pencil, Trash2, Plus, LayoutList, Image as ImageIcon, X, Check,
  Eye, Trash, Calendar, Clock, Globe, Film, Play, ArrowLeft, Users, Building2,
  ChevronDown, AlertCircle, Info, ChevronLeft, ChevronRight, GripVertical
} from 'lucide-react';
import adminMovieService from '../../services/adminMovieService';
import adminGenreService from '../../services/adminGenreService';
import SkeletonTable from '../../components/common/SkeletonTable';
import { useOutletContext } from 'react-router-dom';

// ─── Helpers ──────────────────────────────────────────────────────────────────

const DEFAULT_AVATAR = 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png';
const getTodayString = () => new Date().toISOString().split('T')[0];

const AGE_RATINGS = ['P', 'K', 'T13', 'T16', 'T18'];

const AGE_RATING_LABELS = {
  P: 'P – Phổ thông (Mọi lứa tuổi)',
  K: 'K – Trẻ em (cần giám hộ)',
  T13: 'T13 – Từ 13 tuổi',
  T16: 'T16 – Từ 16 tuổi',
  T18: 'T18 – Từ 18 tuổi',
};

const STATUS_LABELS = {
  DRAFT: 'Nháp',
  UPCOMING: 'Sắp chiếu',
  NOW_SHOWING: 'Đang chiếu',
  ENDED: 'Ngừng chiếu',
  INACTIVE: 'Khóa',
};

const STATUS_COLORS = {
  NOW_SHOWING: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
  UPCOMING: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  ENDED: 'bg-neutral-500/10 text-neutral-400 border-neutral-500/20',
  DRAFT: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  INACTIVE: 'bg-red-500/10 text-red-400 border-red-500/20',
};

const mapTmdbCert = (cert) => {
  if (!cert) return 'P';
  const c = cert.toUpperCase();
  if (['G', 'P', 'ALL', 'PG', 'K', '12'].includes(c)) return 'T13';
  if (['C13', 'T13', 'PG-13'].includes(c)) return 'T13';
  if (['C16', 'T16', '16'].includes(c)) return 'T16';
  if (['R', 'NC-17', 'C18', 'T18', '18'].includes(c)) return 'T18';
  return 'P';
};

const FORMAT_MAP_TO_API = {
  '2D': 'TWO_D',
  '3D': 'THREE_D',
  'IMAX': 'IMAX',
  '4DX': 'FOUR_DX',
  'SCREENX': 'SCREENX'
};

const FORMAT_MAP_FROM_API = {
  'TWO_D': '2D',
  'THREE_D': '3D',
  'IMAX': 'IMAX',
  'FOUR_DX': '4DX',
  'SCREENX': 'SCREENX'
};

const extractTrailerUrl = (bundle) => {
  if (!bundle) return '';
  if (bundle.videos?.primaryTrailer?.url) return bundle.videos.primaryTrailer.url;

  const vidTrailers = Array.isArray(bundle.videos?.trailers) ? bundle.videos.trailers : [];
  const firstVidTrailer = vidTrailers.find(t => t && t.url);
  if (firstVidTrailer) return firstVidTrailer.url;

  const vidTeasers = Array.isArray(bundle.videos?.teasers) ? bundle.videos.teasers : [];
  const firstVidTeaser = vidTeasers.find(t => t && t.url);
  if (firstVidTeaser) return firstVidTeaser.url;

  if (bundle.media?.primaryTrailer?.url) return bundle.media.primaryTrailer.url;

  const mediaTrailers = Array.isArray(bundle.media?.trailers) ? bundle.media.trailers : [];
  const firstMediaTrailer = mediaTrailers.find(t => t && t.url);
  if (firstMediaTrailer) return firstMediaTrailer.url;

  const trailers = Array.isArray(bundle.trailers) ? bundle.trailers : [];
  const youtubeTrailer = trailers.find(t => t && t.site?.toLowerCase() === 'youtube' && t.key);
  if (youtubeTrailer) return `https://www.youtube.com/watch?v=${youtubeTrailer.key}`;

  const yTrailer = vidTrailers.find(t => t && t.site?.toLowerCase() === 'youtube' && t.key);
  if (yTrailer) return `https://www.youtube.com/watch?v=${yTrailer.key}`;

  const yTeaser = vidTeasers.find(t => t && t.site?.toLowerCase() === 'youtube' && t.key);
  if (yTeaser) return `https://www.youtube.com/watch?v=${yTeaser.key}`;

  return '';
};

const extractPosterUrl = (bundle) => {
  if (!bundle) return '';
  const mv = bundle.movie || {};
  const posters = Array.isArray(bundle.media?.posters) ? bundle.media.posters : [];
  const val = bundle.media?.primaryPoster?.url
    || mv.posterUrl
    || mv.poster?.url
    || mv.poster
    || (posters[0]?.url)
    || '';
  return typeof val === 'object' && val !== null ? (val.url || '') : String(val || '');
};

const extractBackdropUrl = (bundle) => {
  if (!bundle) return '';
  const mv = bundle.movie || {};
  const backdrops = Array.isArray(bundle.media?.backdrops) ? bundle.media.backdrops : [];
  const val = bundle.media?.primaryBackdrop?.url
    || mv.backdropUrl
    || mv.backdrop?.url
    || mv.backdrop
    || (backdrops[0]?.url)
    || '';
  return typeof val === 'object' && val !== null ? (val.url || '') : String(val || '');
};

const extractRuntime = (bundle) => {
  if (!bundle) return 0;
  const mv = bundle.movie || {};
  return mv.runtimeMinutes || mv.runtime || mv.durationMinutes || mv.duration || 0;
};

const extractCountry = (bundle) => {
  if (!bundle) return '';
  const mv = bundle.movie || {};
  const countries = mv.countries || mv.productionCountries || [];
  if (Array.isArray(countries) && countries.length > 0) {
    return countries[0]?.name || countries[0]?.countryName || '';
  }
  return mv.country || mv.productionCountry || '';
};

const getYoutubeEmbedUrl = (url) => {
  if (!url) return '';
  try {
    if (url.includes('youtube.com/watch')) {
      const k = new URLSearchParams(new URL(url).search).get('v');
      return k ? `https://www.youtube.com/embed/${k}` : '';
    }
    if (url.includes('youtu.be/')) return `https://www.youtube.com/embed/${url.split('youtu.be/')[1]?.split('?')[0]}`;
    if (url.includes('youtube.com/embed/')) return url.split('?')[0];
  } catch { return ''; }
  return '';
};

const getYoutubeId = (url) => {
  if (!url) return '';
  try {
    if (url.includes('youtube.com/watch')) {
      return new URLSearchParams(new URL(url).search).get('v') || '';
    }
    if (url.includes('youtu.be/')) return url.split('youtu.be/')[1]?.split('?')[0] || '';
    if (url.includes('youtube.com/embed/')) {
      const parts = url.split('youtube.com/embed/');
      if (parts[1]) return parts[1].split('?')[0] || '';
    }
  } catch { return ''; }
  return '';
};

const LazyImage = ({ src, alt, className = '', containerClassName = '', ...props }) => {
  const [visible, setVisible] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    if (!src) return;
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        setVisible(true);
        observer.disconnect();
      }
    }, { rootMargin: '100px' });

    if (ref.current) {
      observer.observe(ref.current);
    }

    return () => observer.disconnect();
  }, [src]);

  return (
    <div ref={ref} className={`relative bg-zinc-900 overflow-hidden ${containerClassName}`}>
      {visible && src ? (
        <img
          src={src}
          alt={alt}
          className={`w-full h-full object-cover transition-opacity duration-300 opacity-100 ${className}`}
          loading="lazy"
          {...props}
        />
      ) : (
        <div className="w-full h-full flex items-center justify-center text-zinc-800 animate-pulse bg-zinc-900">
          <ImageIcon className="w-5 h-5" />
        </div>
      )}
    </div>
  );
};

const formatDate = (d) => {
  if (!d) return 'N/A';
  const dt = new Date(d);
  return isNaN(dt) ? d : dt.toLocaleDateString('vi-VN');
};

const parseApiError = (err) => {
  const d = err?.response?.data || err;
  if (!d) return err?.message || 'Lỗi không xác định.';

  if (d.errorCode === 'INVALID_ENUM_VALUE') {
    const field = d.data?.field || d.field || 'không xác định';
    const val = d.data?.rejectedValue ?? d.data?.value ?? '';
    const allowed = (d.data?.allowedValues || []).join(', ');
    return `Giá trị "${val}" không hợp lệ cho trường "${field}". Giá trị hợp lệ: ${allowed}.`;
  }
  if (d.errorCode === 'VALIDATION_ERROR' && d.data?.fieldErrors) {
    return d.data.fieldErrors.map(e => `"${e.field}": ${e.message}`).join('\n');
  }
  if (d.message) return d.message;
  return err?.message || 'Lỗi không xác định.';
};

// ─── Sub-component: Input / Select / Textarea helpers ─────────────────────────

const Field = ({ label, required, error, children }) => (
  <div className="space-y-1">
    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">
      {label}{required && <span className="text-brand-orange ml-0.5">*</span>}
    </label>
    {children}
    {error && <p className="text-red-400 text-[10px] mt-0.5">{error}</p>}
  </div>
);

const Input = ({ className = '', ...props }) => (
  <input
    className={`w-full bg-brand-dark border border-zinc-800 rounded-xl py-2.5 px-3 text-xs focus:outline-none focus:border-brand-orange/40 transition-colors text-zinc-100 ${className}`}
    {...props}
  />
);

const Select = ({ children, className = '', ...props }) => (
  <select
    className={`w-full bg-brand-dark border border-zinc-800 rounded-xl py-2.5 px-3 text-xs focus:outline-none focus:border-brand-orange/40 transition-colors text-zinc-100 outline-none ${className}`}
    {...props}
  >
    {children}
  </select>
);

const Textarea = ({ className = '', ...props }) => (
  <textarea
    className={`w-full bg-brand-dark border border-zinc-800 rounded-xl py-2.5 px-3 text-xs focus:outline-none focus:border-brand-orange/40 transition-colors text-zinc-100 leading-relaxed ${className}`}
    {...props}
  />
);

// ─── Empty form state ──────────────────────────────────────────────────────────

const emptyForm = () => ({
  title: '',
  originalTitle: '',
  durationMinutes: '',
  ageRating: 'P',
  showingStartDate: '',
  endDate: '',
  country: '',
  synopsis: '',
  tmdbReleaseDate: '',
  originalLanguage: '',
  status: 'DRAFT',
});

// ─── Main Component ────────────────────────────────────────────────────────────

export default function AdminMoviePage() {
  const { triggerToast } = useOutletContext() || {};

  // List state
  const [movies, setMovies] = useState([]);
  const [genresList, setGenresList] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [statusFilter, setStatusFilter] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // View state
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [selectedMovie, setSelectedMovie] = useState(null);
  const [isSaving, setIsSaving] = useState(false);
  const [formErrors, setFormErrors] = useState({});
  const [activeBannerIdx, setActiveBannerIdx] = useState(0);
  const [activeFormTab, setActiveFormTab] = useState('basic'); // 'basic' | 'crew' | 'media'
  const [draggedIdx, setDraggedIdx] = useState(null);
  const [draggedType, setDraggedType] = useState(null); // 'directors' | 'writers' | 'producers' | 'cast' | 'studios'

  // Auto-switch banner every 10 seconds
  useEffect(() => {
    if (!isDetailOpen || !selectedMovie) return;
    const banners = selectedMovie.media ? selectedMovie.media.filter(m => m.mediaType === 'BANNER').map(m => m.url) : [];
    if (banners.length <= 1) return;

    const timer = setInterval(() => {
      setActiveBannerIdx(prev => (prev === banners.length - 1 ? 0 : prev + 1));
    }, 10000);

    return () => clearInterval(timer);
  }, [isDetailOpen, selectedMovie, activeBannerIdx]);

  // TMDB autocomplete
  const [tmdbSearch, setTmdbSearch] = useState('');
  const [tmdbSuggestions, setTmdbSuggestions] = useState([]);
  const [isTmdbSearching, setIsTmdbSearching] = useState(false);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [isTmdbLoading, setIsTmdbLoading] = useState(false);

  // TMDB Latest Movies Recommendation Carousel
  const [tmdbLatestMovies, setTmdbLatestMovies] = useState([]);
  const [latestMoviesPageIndex, setLatestMoviesPageIndex] = useState(0);
  const [isLatestMoviesLoading, setIsLatestMoviesLoading] = useState(false);
  const [slideOffset, setSlideOffset] = useState(-14.2857);
  const [isTransitioning, setIsTransitioning] = useState(true);
  const [slideLock, setSlideLock] = useState(false);
  // Form field state
  const [formBasic, setFormBasic] = useState(emptyForm());
  const [selectedGenres, setSelectedGenres] = useState([]); // [{publicId, name}]
  // tmdbGenres: enriched objects from bundle — [{tmdbId, name, existsInDb, dbPublicId|null}]
  const [tmdbGenres, setTmdbGenres] = useState([]);
  const [availableBackdrops, setAvailableBackdrops] = useState([]);
  const [backdropImportCount, setBackdropImportCount] = useState(0);

  // Media
  const [posterUrl, setPosterUrl] = useState('');
  const [bannerUrls, setBannerUrls] = useState(['']); // Array supports multiple banners
  const [trailerUrl, setTrailerUrl] = useState('');

  const [playTrailer, setPlayTrailer] = useState(false);
  const [playDetailTrailer, setPlayDetailTrailer] = useState(false);

  // Reset playTrailer when trailerUrl changes
  useEffect(() => {
    setPlayTrailer(false);
  }, [trailerUrl]);

  // Reset playDetailTrailer when selectedMovie changes
  useEffect(() => {
    setPlayDetailTrailer(false);
  }, [selectedMovie]);

  // Extra TMDB-imported data (display only — not sent to backend yet)
  const [cast, setCast] = useState([]);  // [{name, character, profileImageUrl}]
  const [directors, setDirectors] = useState([]);
  const [writers, setWriters] = useState([]);
  const [producers, setProducers] = useState([]);
  const [studios, setStudios] = useState([]);  // [{name, logoUrl}]

  // Versions
  const [versions, setVersions] = useState([]);
  const [origVersions, setOrigVersions] = useState([]);
  const [origMedia, setOrigMedia] = useState({ poster: null, banners: [], trailer: null });

  // ─── Fetch genres list ───────────────────────────────────────────────────────
  const fetchGenres = useCallback(async () => {
    try {
      const data = await adminGenreService.getAllGenres();
      let list = data?.data?.content || data?.data || data?.content || data || [];
      if (!Array.isArray(list)) list = [];
      setGenresList(list);
    } catch { /* ignore */ }
  }, []);

  useEffect(() => { fetchGenres(); }, [fetchGenres]);

  // Fetch latest TMDB movies for recommendation
  useEffect(() => {
    const fetchLatestMovies = async () => {
      setIsLatestMoviesLoading(true);
      try {
        const res = await adminMovieService.getLatestTop20(20);
        if (res?.success && Array.isArray(res.data)) {
          setTmdbLatestMovies(res.data);
        }
      } catch (err) {
        console.error("Failed to fetch TMDB latest movies:", err);
      } finally {
        setIsLatestMoviesLoading(false);
      }
    };
    fetchLatestMovies();
  }, []);

  // ─── Fetch movies ────────────────────────────────────────────────────────────
  const fetchMovies = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await adminMovieService.getMovies({
        page: currentPage,
        size: pageSize,
        search: searchTerm || undefined,
        status: statusFilter || 'ALL',
      });
      const content = data?.data?.data || data?.data?.content || data?.content || data?.data || data || [];
      const totalEls = data?.data?.totalElements ?? data?.totalElements ?? (Array.isArray(content) ? content.length : 0);
      const totalPgs = data?.data?.totalPages ?? data?.totalPages ?? Math.ceil(totalEls / pageSize);
      setMovies(Array.isArray(content) ? content : []);
      setTotalElements(totalEls);
      setTotalPages(totalPgs);
    } catch {
      triggerToast?.('Lỗi khi tải danh sách phim', 'error');
    } finally {
      setIsLoading(false);
    }
  }, [currentPage, pageSize, searchTerm, statusFilter, triggerToast]);

  useEffect(() => {
    const t = setTimeout(fetchMovies, 300);
    return () => clearTimeout(t);
  }, [fetchMovies]);

  // ─── TMDB autocomplete ───────────────────────────────────────────────────────
  useEffect(() => {
    if (!tmdbSearch.trim()) { setTmdbSuggestions([]); return; }

    const abortController = new AbortController();

    const t = setTimeout(async () => {
      setIsTmdbSearching(true);
      try {
        const res = await adminMovieService.searchTmdbSuggestions(tmdbSearch, abortController.signal);
        setTmdbSuggestions(Array.isArray(res?.data) ? res.data : []);
      } catch (err) {
        if (err?.name !== 'CanceledError' && err?.message !== 'canceled') {
          setTmdbSuggestions([]);
        }
      } finally {
        setIsTmdbSearching(false);
      }
    }, 400);

    return () => {
      clearTimeout(t);
      abortController.abort();
    };
  }, [tmdbSearch]);

  // ─── Select a TMDB suggestion → auto-fill form ───────────────────────────────
  const handleSelectTmdb = async (tmdbId) => {
    setShowSuggestions(false);
    setTmdbSearch('');
    setIsTmdbLoading(true);
    try {
      const res = await adminMovieService.getTmdbMovieBundle(tmdbId);
      if (!res?.success || !res?.data) {
        triggerToast?.('Không lấy được dữ liệu từ TMDB', 'error');
        return;
      }
      const bundle = res.data;
      const mv = bundle.movie || {};

      // Check if movie already exists in local DB
      const searchRes = await adminMovieService.getMovies({ search: mv.title, status: 'ALL' });
      const existingList = searchRes?.data?.data || searchRes?.data || searchRes?.content || [];
      const isDuplicate = Array.isArray(existingList) && existingList.some(
        m => m.title && m.title.toLowerCase().trim() === mv.title.toLowerCase().trim() && m.activeSlug
      );

      if (isDuplicate) {
        triggerToast?.('Bộ phim này đã tồn tại trong hệ thống.', 'error');
        return;
      }

      // Fetch backdrops from TMDB API
      let backdrops = [];
      try {
        const imagesRes = await adminMovieService.getTmdbMovieImages(tmdbId);
        backdrops = Array.isArray(imagesRes?.data?.backdrops) ? imagesRes.data.backdrops : [];
      } catch (err) {
        console.warn('Failed to fetch TMDB movie backdrops:', err);
      }
      setAvailableBackdrops(backdrops);
      setBackdropImportCount(backdrops.length);

      // ── Basic fields ──
      const runtime = extractRuntime(bundle);
      const certification = bundle.releaseInfo?.preferredRelease?.certification || '';
      setFormBasic({
        title: mv.title || '',
        originalTitle: mv.originalTitle || '',
        durationMinutes: runtime ? String(runtime) : '',
        ageRating: mapTmdbCert(certification),
        showingStartDate: mv.releaseDate || '', // Initialized with TMDB release date, editable by admin
        endDate: '',
        country: extractCountry(bundle),
        synopsis: mv.overview || '',
        tmdbReleaseDate: mv.releaseDate || '',   // original TMDB release date
        originalLanguage: (mv.originalLanguage || '').toUpperCase(),
        status: 'DRAFT',
      });

      // ── Media: use robust extraction ──
      const posterSrc = extractPosterUrl(bundle);
      const backdropSrc = extractBackdropUrl(bundle);
      const trailerSrc = extractTrailerUrl(bundle);
      setPosterUrl(posterSrc);

      if (backdrops.length > 0) {
        setBannerUrls(backdrops.map(b => b.url || ''));
      } else {
        setBannerUrls(backdropSrc ? [backdropSrc] : ['']);
      }
      setTrailerUrl(trailerSrc);

      // ── Genres: compute existsInDb flag against current genresList ──
      const rawGenres = Array.isArray(bundle.genres) ? bundle.genres : [];
      const enrichedGenres = rawGenres.map(tg => {
        const found = genresList.find(g => g.name && tg.name && g.name.toLowerCase() === tg.name.toLowerCase());
        return {
          tmdbId: tg.tmdbId || tg.tmdbGenreId,
          name: tg.name,
          existsInDb: !!found,
          dbPublicId: found?.publicId || null,
        };
      });
      setTmdbGenres(enrichedGenres);
      // Pre-select genres that already exist in the DB
      const matched = enrichedGenres
        .filter(tg => tg.existsInDb)
        .map(tg => ({ publicId: tg.dbPublicId, name: tg.name }));
      setSelectedGenres(matched);

      // ── Credits ──
      const cred = bundle.credits || {};
      setCast(Array.isArray(cred.mainCast) ? cred.mainCast.slice(0, 12) : []);
      setDirectors(Array.isArray(cred.directors) ? cred.directors : []);
      setWriters(Array.isArray(cred.writers) ? cred.writers : []);
      setProducers(Array.isArray(cred.producers) ? cred.producers : []);

      // ── Production companies ──
      setStudios(Array.isArray(mv.productionCompanies) ? mv.productionCompanies : []);

      // ── Suggest a default screening version based on original language ──
      const audioLang = (mv.originalLanguage || 'EN').toUpperCase();
      setVersions([{
        versionName: `2D Vietsub`,
        format: '2D',
        audioLanguage: audioLang,
        subtitleLanguage: 'VI',
        dubLanguage: 'NONE',
        status: 'ACTIVE',
      }]);

      triggerToast?.(`Đã import dữ liệu: ${mv.title || ''} (${runtime} phút)`);
    } catch (err) {
      triggerToast?.(parseApiError(err), 'error');
    } finally {
      setIsTmdbLoading(false);
    }
  };

  // ─── Open Add form ────────────────────────────────────────────────────────────
  const handleOpenAdd = () => {
    setSelectedMovie(null);
    setFormBasic(emptyForm());
    setSelectedGenres([]);
    setTmdbGenres([]);
    setPosterUrl('');
    setBannerUrls(['']);
    setTrailerUrl('');
    setCast([]);
    setDirectors([]);
    setWriters([]);
    setProducers([]);
    setStudios([]);
    setVersions([]);
    setOrigVersions([]);
    setOrigMedia({ poster: null, banners: [], trailer: null });
    setFormErrors({});
    setAvailableBackdrops([]);
    setBackdropImportCount(0);
    setActiveFormTab('basic');
    setIsFormOpen(true);
  };

  // ─── TMDB latest movies carousel slide handlers ─────────────────────────────
  const slideRight = () => {
    if (slideLock || tmdbLatestMovies.length === 0) return;
    setSlideLock(true);
    setSlideOffset(-28.5714); // slide track left (show right neighbor)
    setTimeout(() => {
      setLatestMoviesPageIndex(prev => (prev + 1) % tmdbLatestMovies.length);
      setIsTransitioning(false);
      setSlideOffset(-14.2857);
      setTimeout(() => {
        setIsTransitioning(true);
        setSlideLock(false);
      }, 30);
    }, 300);
  };

  const slideLeft = () => {
    if (slideLock || tmdbLatestMovies.length === 0) return;
    setSlideLock(true);
    setSlideOffset(0); // slide track right (show left neighbor)
    setTimeout(() => {
      setLatestMoviesPageIndex(prev => (prev - 1 + tmdbLatestMovies.length) % tmdbLatestMovies.length);
      setIsTransitioning(false);
      setSlideOffset(-14.2857);
      setTimeout(() => {
        setIsTransitioning(true);
        setSlideLock(false);
      }, 30);
    }, 300);
  };

  const handleCleanForm = () => {
    setFormBasic(emptyForm());
    setSelectedGenres([]);
    setTmdbGenres([]);
    setPosterUrl('');
    setBannerUrls(['']);
    setTrailerUrl('');
    setCast([]);
    setDirectors([]);
    setWriters([]);
    setProducers([]);
    setStudios([]);
    setVersions([]);
    setOrigVersions([]);
    setOrigMedia({ poster: null, banners: [], trailer: null });
    setFormErrors({});
    setAvailableBackdrops([]);
    setBackdropImportCount(0);
    triggerToast?.('Đã dọn sạch thông tin biểu mẫu phim!', 'info');
  };

  // ─── Open Edit form ───────────────────────────────────────────────────────────
  const handleOpenEdit = async (movie) => {
    setIsLoading(true);
    try {
      const [detailRes, mediaRes, verRes] = await Promise.all([
        adminMovieService.getMovieById(movie.publicId),
        adminMovieService.getMovieMedia(movie.publicId),
        adminMovieService.getMovieVersions(movie.publicId),
      ]);

      if (!detailRes?.success || !detailRes?.data) {
        triggerToast?.('Không lấy được chi tiết phim', 'error');
        return;
      }
      const fullMovie = detailRes.data;
      setSelectedMovie(fullMovie);

      const mediaList = mediaRes?.data || [];
      const versionList = verRes?.data || [];

      const poster = mediaList.find(m => m.mediaType === 'POSTER' && m.isPrimary) || mediaList.find(m => m.mediaType === 'POSTER') || null;
      const banners = mediaList.filter(m => m.mediaType === 'BANNER');
      const trailer = mediaList.find(m => m.mediaType === 'TRAILER') || null;

      setOrigMedia({ poster, banners, trailer });
      setPosterUrl(poster?.url || '');
      setBannerUrls(banners.length ? banners.map(b => b.url) : ['']);
      setTrailerUrl(trailer?.url || '');

      setVersions(versionList.map(v => ({
        ...v,
        format: FORMAT_MAP_FROM_API[v.format] || v.format
      })));
      setOrigVersions(versionList.map(v => ({
        ...v,
        format: FORMAT_MAP_FROM_API[v.format] || v.format
      })));

      const movieGenreIds = [];
      if (Array.isArray(movie.genres)) {
        movie.genres.forEach(gName => {
          const match = genresList.find(g => g.name?.toLowerCase() === gName.toLowerCase());
          if (match) movieGenreIds.push({ publicId: match.publicId, name: match.name });
        });
      }
      setSelectedGenres(movieGenreIds);
      setTmdbGenres([]);

      setCast(fullMovie.actors ? fullMovie.actors.map(a => ({
        name: a.fullName,
        character: a.characterName,
        profileUrl: a.profileImageUrl || ''
      })) : []);
      setDirectors(fullMovie.directors ? fullMovie.directors.map(d => ({
        name: d.fullName
      })) : []);
      setWriters(fullMovie.writers ? fullMovie.writers.map(w => ({
        name: w.fullName
      })) : []);
      setProducers(fullMovie.producers ? fullMovie.producers.map(p => ({
        name: p.fullName
      })) : []);
      setStudios(fullMovie.productionCompanies ? fullMovie.productionCompanies.map(s => ({
        name: s.name,
        logoUrl: s.logoUrl || ''
      })) : []);

      setFormBasic({
        title: movie.title || '',
        originalTitle: movie.originalTitle || '',
        durationMinutes: movie.durationMinutes || '',
        ageRating: movie.ageRating || 'P',
        showingStartDate: movie.releaseDate || '',
        endDate: movie.endDate || '',
        country: movie.country || '',
        synopsis: movie.synopsis || '',
        tmdbReleaseDate: '',
        originalLanguage: '',
        status: movie.status || 'UPCOMING',
      });
      setAvailableBackdrops([]);
      setBackdropImportCount(0);
      setFormErrors({});
      setActiveFormTab('basic');
      setIsFormOpen(true);
    } catch (err) {
      triggerToast?.(parseApiError(err), 'error');
    } finally {
      setIsLoading(false);
    }
  };

  // ─── Open Detail view ─────────────────────────────────────────────────────────
  const handleOpenDetail = async (movie) => {
    setIsLoading(true);
    setActiveBannerIdx(0);
    try {
      const res = await adminMovieService.getMovieById(movie.publicId);
      if (res?.success && res?.data) {
        setSelectedMovie(res.data);
        setIsDetailOpen(true);
      } else {
        triggerToast?.('Không lấy được chi tiết phim', 'error');
      }
    } catch (err) {
      triggerToast?.(parseApiError(err), 'error');
    } finally {
      setIsLoading(false);
    }
  };

  // ─── Versions helpers ─────────────────────────────────────────────────────────
  const addVersion = () => setVersions(v => [...v, {
    versionName: '', format: '2D', audioLanguage: 'EN', subtitleLanguage: 'VI', dubLanguage: 'NONE', status: 'ACTIVE',
  }]);
  const updateVersion = (i, field, val) => setVersions(v => v.map((ver, idx) => idx === i ? { ...ver, [field]: val } : ver));
  const removeVersion = (i) => setVersions(v => v.filter((_, idx) => idx !== i));

  // ─── Banner helpers ───────────────────────────────────────────────────────────
  const addBanner = () => setBannerUrls(b => [...b, '']);
  const updateBanner = (i, val) => setBannerUrls(b => b.map((url, idx) => idx === i ? val : url));
  const removeBanner = (i) => setBannerUrls(b => b.filter((_, idx) => idx !== i));

  // ─── Genre helpers ────────────────────────────────────────────────────────────
  const toggleGenre = (g) => {
    setSelectedGenres(prev => {
      const exists = prev.some(s => s.publicId === g.publicId);
      return exists ? prev.filter(s => s.publicId !== g.publicId) : [...prev, g];
    });
  };

  // ─── Validate form ────────────────────────────────────────────────────────────
  const validateForm = () => {
    const isEdit = !!selectedMovie;
    const errs = {};
    const todayStr = getTodayString();
    const todayDate = new Date(todayStr);

    if (!formBasic.title.trim()) errs.title = 'Tên phim không được để trống.';
    if (!formBasic.durationMinutes || Number(formBasic.durationMinutes) <= 0)
      errs.durationMinutes = 'Thời lượng phải là số dương.';
    if (!formBasic.ageRating || !AGE_RATINGS.includes(formBasic.ageRating))
      errs.ageRating = `Độ tuổi phải là một trong: ${AGE_RATINGS.join(', ')}.`;

    // Dates validation
    if (!formBasic.showingStartDate) {
      errs.showingStartDate = 'Ngày khởi chiếu bắt buộc phải chọn.';
    }
    if (formBasic.endDate && new Date(formBasic.endDate) < new Date(formBasic.showingStartDate)) {
      errs.endDate = 'Ngày kết thúc không thể trước ngày khởi chiếu.';
    }

    // Status transition constraints (both Frontend & Backend sync)
    const status = formBasic.status || 'DRAFT';
    const startD = formBasic.showingStartDate ? new Date(formBasic.showingStartDate) : null;
    const endD = formBasic.endDate ? new Date(formBasic.endDate) : null;

    if (status === 'UPCOMING') {
      if (startD && startD <= todayDate) {
        errs.showingStartDate = 'Trạng thái Sắp chiếu yêu cầu ngày khởi chiếu ở tương lai (sau hôm nay).';
      }
    } else if (status === 'NOW_SHOWING') {
      if (startD && startD > todayDate) {
        errs.showingStartDate = 'Trạng thái Đang chiếu yêu cầu ngày khởi chiếu ở quá khứ hoặc hôm nay.';
      }
      if (endD && endD < todayDate) {
        errs.endDate = 'Trạng thái Đang chiếu yêu cầu ngày kết thúc ở tương lai hoặc hôm nay.';
      }
      if (selectedGenres.length === 0 && tmdbGenres.length === 0) {
        errs.genres = 'Phim phải có ít nhất 1 thể loại khi ở trạng thái Đang chiếu.';
      }
    } else if (status === 'ENDED') {
      if (!formBasic.endDate) {
        errs.endDate = 'Trạng thái Ngừng chiếu bắt buộc phải chọn ngày kết thúc.';
      } else if (endD && endD >= todayDate) {
        errs.endDate = 'Trạng thái Ngừng chiếu yêu cầu ngày kết thúc ở quá khứ (trước hôm nay).';
      }
    }

    setFormErrors(errs);
    return Object.keys(errs).length === 0;
  };

  // ─── Genre auto-create + assign ───────────────────────────────────────────────
  const resolveAndAssignGenres = async (moviePublicId) => {
    // Collect IDs of genres that already exist and are selected
    let finalIds = selectedGenres
      .filter(g => g.publicId)
      .map(g => g.publicId);

    // For TMDB genres that do NOT exist in the DB yet, create them first.
    // tmdbGenres items: {tmdbId, name, existsInDb, dbPublicId}
    const toCreate = tmdbGenres.filter(tg => !tg.existsInDb);
    for (const tg of toCreate) {
      // Skip if admin manually removed this genre (not in selectedGenres AND it was existsInDb=false)
      // We always create-and-assign all TMDB "new" genres unless admin explicitly unchecked them.
      // Since admin sees them as "will be auto-created", they're always included unless they
      // actively navigate away. (Future: add per-genre remove toggle).
      try {
        const created = await adminMovieService.ensureGenreExists(tg.name);
        if (created?.publicId) {
          // Newly created
          if (!finalIds.includes(created.publicId)) finalIds.push(created.publicId);
        } else {
          // 409 duplicate — genre was created concurrently. Fetch fresh list to find it.
          const freshRes = await adminGenreService.getAllGenres();
          const freshList = freshRes?.data?.content || freshRes?.data || freshRes?.content || freshRes || [];
          const found = Array.isArray(freshList)
            ? freshList.find(g => g.name?.toLowerCase() === tg.name?.toLowerCase())
            : null;
          if (found?.publicId && !finalIds.includes(found.publicId)) {
            finalIds.push(found.publicId);
            setGenresList(prev =>
              prev.some(g => g.publicId === found.publicId) ? prev : [...prev, found]
            );
          }
        }
      } catch { /* skip on error */ }
    }

    const uniqueGenreIds = [...new Set(finalIds)];
    if (uniqueGenreIds.length > 0) {
      await adminMovieService.assignGenres(moviePublicId, uniqueGenreIds);
    }
  };

  const resolveAndAssignCreditsAndCompanies = async (moviePublicId) => {
    try {
      const creditRequests = [];

      for (let i = 0; i < cast.length; i++) {
        const c = cast[i];
        if (c.name?.trim()) {
          const person = await adminMovieService.ensurePersonExists(c.name, c.profileUrl);
          if (person?.publicId) {
            creditRequests.push({
              personPublicId: person.publicId,
              roleType: 'MAIN_ACTOR',
              characterName: c.character || '',
              displayOrder: i
            });
          }
        }
      }

      for (let i = 0; i < directors.length; i++) {
        const d = directors[i];
        if (d.name?.trim()) {
          const person = await adminMovieService.ensurePersonExists(d.name, '');
          if (person?.publicId) {
            creditRequests.push({
              personPublicId: person.publicId,
              roleType: 'DIRECTOR',
              characterName: '',
              displayOrder: i
            });
          }
        }
      }

      for (let i = 0; i < writers.length; i++) {
        const w = writers[i];
        if (w.name?.trim()) {
          const person = await adminMovieService.ensurePersonExists(w.name, '');
          if (person?.publicId) {
            creditRequests.push({
              personPublicId: person.publicId,
              roleType: 'WRITER',
              characterName: '',
              displayOrder: i
            });
          }
        }
      }

      for (let i = 0; i < producers.length; i++) {
        const p = producers[i];
        if (p.name?.trim()) {
          const person = await adminMovieService.ensurePersonExists(p.name, '');
          if (person?.publicId) {
            creditRequests.push({
              personPublicId: person.publicId,
              roleType: 'PRODUCER',
              characterName: '',
              displayOrder: i
            });
          }
        }
      }

      // De-duplicate credits by person, role, and character name
      const seenCredits = new Set();
      const uniqueCreditRequests = [];
      for (const req of creditRequests) {
        const key = `${req.personPublicId}_${req.roleType}_${(req.characterName || '').trim().toLowerCase()}`;
        if (!seenCredits.has(key)) {
          seenCredits.add(key);
          uniqueCreditRequests.push(req);
        }
      }

      await adminMovieService.assignCredits(moviePublicId, uniqueCreditRequests);

      const companyRequests = [];
      for (const s of studios) {
        if (s.name?.trim()) {
          const company = await adminMovieService.ensureProductionCompanyExists(s.name, s.logoUrl);
          if (company?.publicId) {
            companyRequests.push({
              companyPublicId: company.publicId,
              role: 'PRODUCTION'
            });
          }
        }
      }

      // De-duplicate production companies by company and role
      const seenCompanies = new Set();
      const uniqueCompanyRequests = [];
      for (const req of companyRequests) {
        const key = `${req.companyPublicId}_${req.role}`;
        if (!seenCompanies.has(key)) {
          seenCompanies.add(key);
          uniqueCompanyRequests.push(req);
        }
      }

      await adminMovieService.assignProductionCompanies(moviePublicId, uniqueCompanyRequests);
    } catch (err) {
      console.error("Failed to assign credits/companies:", err);
      throw err;
    }
  };

  // ─── Save (Create / Update) ───────────────────────────────────────────────────
  const handleSave = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    setIsSaving(true);
    try {
      // Build backend-safe request — TYPES MATTER:
      const moviePayload = {
        title: formBasic.title?.trim() || '',
        originalTitle: formBasic.originalTitle?.trim() || null,
        durationMinutes: Number(formBasic.durationMinutes), // Must be Integer, NOT string
        ageRating: formBasic.ageRating,               // Must match enum exactly: P|K|T13|T16|T18
        releaseDate: formBasic.showingStartDate || getTodayString(), // Backend field name = releaseDate
        endDate: formBasic.endDate || null,
        country: formBasic.country?.trim() || null,
        synopsis: formBasic.synopsis?.trim() || null,
        status: formBasic.status || 'UPCOMING',
      };

      if (selectedMovie) {
        // ── Update ──────────────────────────────────────────────────────────────
        const publicId = selectedMovie.publicId;
        await adminMovieService.updateMovie(publicId, moviePayload);
        await resolveAndAssignGenres(publicId);
        await resolveAndAssignCreditsAndCompanies(publicId);

        // Media diffs
        await reconcileMedia(publicId, origMedia);

        // Version diffs
        const versionsToDelete = origVersions.filter(ov => !versions.some(v => v.publicId === ov.publicId));
        for (const ov of versionsToDelete) await adminMovieService.deleteMovieVersion(ov.publicId);
        for (const v of versions) {
          if (v.publicId) {
            const orig = origVersions.find(ov => ov.publicId === v.publicId);
            if (orig && JSON.stringify(orig) !== JSON.stringify(v)) {
              await adminMovieService.updateMovieVersion(v.publicId, buildVersionPayload(v));
            }
          } else {
            await adminMovieService.createMovieVersion(publicId, buildVersionPayload(v));
          }
        }

        triggerToast?.('Cập nhật phim thành công!');
      } else {
        // ── Create ──────────────────────────────────────────────────────────────
        const res = await adminMovieService.createMovie(moviePayload);
        const publicId = res?.data?.publicId || res?.publicId;
        if (!publicId) throw new Error('Không nhận được mã phim từ server. Vui lòng kiểm tra lại.');

        await resolveAndAssignGenres(publicId);
        await resolveAndAssignCreditsAndCompanies(publicId);
        await createAllMedia(publicId);
        for (const v of versions) {
          await adminMovieService.createMovieVersion(publicId, buildVersionPayload(v));
        }

        triggerToast?.('Thêm phim mới thành công!');
      }

      setIsFormOpen(false);
      fetchMovies();
    } catch (err) {
      console.error("Failed to save movie:", err);
      const d = err?.response?.data || err;
      if (d && d.errorCode === 'VALIDATION_ERROR' && d.data?.fieldErrors) {
        const errs = {};
        d.data.fieldErrors.forEach(e => {
          let fieldKey = e.field;
          if (fieldKey === 'releaseDate') fieldKey = 'showingStartDate';
          errs[fieldKey] = e.message;
        });
        setFormErrors(errs);
        triggerToast?.('Một số thông tin nhập chưa đúng, vui lòng kiểm tra lại.', 'error');
      } else {
        const msg = parseApiError(err);
        triggerToast?.(msg, 'error');
      }
    } finally {
      setIsSaving(false);
    }
  };

  const buildVersionPayload = (v) => ({
    versionName: v.versionName,
    format: FORMAT_MAP_TO_API[v.format] || v.format,
    audioLanguage: v.audioLanguage || null,
    subtitleLanguage: v.subtitleLanguage || null,
    dubLanguage: v.dubLanguage || null,
    status: v.status || 'ACTIVE',
  });

  const createAllMedia = async (publicId) => {
    if (posterUrl?.trim()) {
      await adminMovieService.createMovieMedia(publicId, { mediaType: 'POSTER', url: posterUrl, title: 'Poster', isPrimary: true, displayOrder: 0, status: 'ACTIVE' });
    }
    for (let i = 0; i < bannerUrls.length; i++) {
      const b = bannerUrls[i];
      if (b?.trim()) {
        await adminMovieService.createMovieMedia(publicId, { mediaType: 'BANNER', url: b, title: `Banner ${i + 1}`, isPrimary: i === 0, displayOrder: i, status: 'ACTIVE' });
      }
    }
    if (trailerUrl?.trim()) {
      await adminMovieService.createMovieMedia(publicId, { mediaType: 'TRAILER', url: trailerUrl, title: 'Trailer', isPrimary: false, displayOrder: 0, status: 'ACTIVE' });
    }
  };

  const reconcileMedia = async (publicId, orig) => {
    // Poster
    if (posterUrl?.trim()) {
      if (orig.poster) {
        if (orig.poster.url !== posterUrl) await adminMovieService.updateMovieMedia(orig.poster.publicId, { mediaType: 'POSTER', url: posterUrl, title: 'Poster', isPrimary: true, displayOrder: 0, status: 'ACTIVE' });
      } else {
        await adminMovieService.createMovieMedia(publicId, { mediaType: 'POSTER', url: posterUrl, title: 'Poster', isPrimary: true, displayOrder: 0, status: 'ACTIVE' });
      }
    } else if (orig.poster) {
      await adminMovieService.deleteMovieMedia(orig.poster.publicId);
    }

    // Banners: simple approach — delete all old, recreate
    for (const ob of orig.banners) await adminMovieService.deleteMovieMedia(ob.publicId);
    for (let i = 0; i < bannerUrls.length; i++) {
      const b = bannerUrls[i];
      if (b?.trim()) await adminMovieService.createMovieMedia(publicId, { mediaType: 'BANNER', url: b, title: `Banner ${i + 1}`, isPrimary: i === 0, displayOrder: i, status: 'ACTIVE' });
    }

    // Trailer
    if (trailerUrl?.trim()) {
      if (orig.trailer) {
        if (orig.trailer.url !== trailerUrl) await adminMovieService.updateMovieMedia(orig.trailer.publicId, { mediaType: 'TRAILER', url: trailerUrl, title: 'Trailer', isPrimary: false, displayOrder: 0, status: 'ACTIVE' });
      } else {
        await adminMovieService.createMovieMedia(publicId, { mediaType: 'TRAILER', url: trailerUrl, title: 'Trailer', isPrimary: false, displayOrder: 0, status: 'ACTIVE' });
      }
    } else if (orig.trailer) {
      await adminMovieService.deleteMovieMedia(orig.trailer.publicId);
    }
  };

  const handleDelete = async (publicId, title) => {
    if (!confirm(`Bạn có chắc chắn muốn xóa phim "${title}"?`)) return;
    try {
      await adminMovieService.deleteMovie(publicId);
      triggerToast?.('Đã xóa phim thành công!');
      fetchMovies();
    } catch (err) {
      triggerToast?.(parseApiError(err), 'error');
    }
  };

  const handleDragStart = (idx, type) => {
    setDraggedIdx(idx);
    setDraggedType(type);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
  };

  const handleDragEnter = (idx, type, items, setItems) => {
    if (draggedType !== type || draggedIdx === null || draggedIdx === idx) return;
    const newItems = [...items];
    const draggedItem = newItems[draggedIdx];
    newItems.splice(draggedIdx, 1);
    newItems.splice(idx, 0, draggedItem);
    setDraggedIdx(idx);
    setItems(newItems);
  };

  const handleDragEnd = () => {
    setDraggedIdx(null);
    setDraggedType(null);
  };

  if (isDetailOpen && selectedMovie) {
    const mv = selectedMovie;
    const banners = mv.media ? mv.media.filter(m => m.mediaType === 'BANNER').map(m => m.url) : [];
    const trailerMedia = mv.media ? mv.media.find(m => m.mediaType === 'TRAILER') : null;
    const embedTrailerUrl = getYoutubeEmbedUrl(trailerMedia?.url);

    return (
      <div className="flex flex-col flex-1 overflow-auto bg-zinc-950 text-white animate-fade-in pb-12">
        {/* Wide Header Backdrop Banner Section with Crossfade Carousel */}
        <div className="relative w-full h-[320px] md:h-[420px] flex-shrink-0 overflow-hidden border-b border-zinc-900 bg-black group">
          {/* Backdrop Images with crossfade */}
          {banners.length > 0 ? (
            banners.map((url, idx) => (
              <img
                key={idx}
                src={url}
                alt=""
                className={`absolute inset-0 w-full h-full object-cover transition-opacity duration-1000 ease-in-out ${idx === activeBannerIdx ? 'opacity-50' : 'opacity-0 pointer-events-none'}`}
              />
            ))
          ) : mv.primaryPoster ? (
            <img
              src={mv.primaryPoster}
              alt=""
              className="absolute inset-0 w-full h-full object-cover opacity-40 blur-sm"
            />
          ) : (
            <div className="absolute inset-0 bg-gradient-to-t from-zinc-950 to-zinc-900" />
          )}

          {/* Gradients */}
          <div className="absolute inset-0 bg-gradient-to-t from-zinc-950 via-zinc-950/60 to-transparent" />
          <div className="absolute inset-0 bg-gradient-to-r from-zinc-950/50 via-transparent to-zinc-950/50" />

          {/* Navigation & Actions Top Bar */}
          <div className="absolute top-0 left-0 right-0 p-6 flex justify-between items-center z-20">
            <button
              onClick={() => setIsDetailOpen(false)}
              className="flex items-center gap-2 text-xs font-semibold px-4 py-2.5 rounded-xl bg-zinc-950/80 hover:bg-zinc-900 border border-zinc-800 text-zinc-300 hover:text-white transition-all cursor-pointer shadow-lg backdrop-blur-md"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>DANH SÁCH</span>
            </button>
            <button
              onClick={() => { setIsDetailOpen(false); handleOpenEdit(mv); }}
              className="flex items-center gap-2 text-xs font-bold bg-brand-orange hover:bg-orange-600 text-zinc-950 px-5 py-2.5 rounded-xl transition-all cursor-pointer shadow-lg hover:shadow-brand-orange/20"
            >
              <Pencil className="w-3.5 h-3.5" />
              <span>CHỬA SỬA PHIM</span>
            </button>
          </div>

          {/* Hover Arrow buttons for carousel */}
          {banners.length > 1 && (
            <>
              <button
                type="button"
                onClick={() => setActiveBannerIdx(prev => (prev === 0 ? banners.length - 1 : prev - 1))}
                className="absolute left-6 top-1/2 -translate-y-1/2 p-3 rounded-full bg-black/60 border border-zinc-800 text-zinc-300 hover:text-white transition-all duration-300 opacity-0 group-hover:opacity-100 cursor-pointer hover:scale-110 z-20"
              >
                <ChevronLeft className="w-5 h-5" />
              </button>
              <button
                type="button"
                onClick={() => setActiveBannerIdx(prev => (prev === banners.length - 1 ? 0 : prev + 1))}
                className="absolute right-6 top-1/2 -translate-y-1/2 p-3 rounded-full bg-black/60 border border-zinc-800 text-zinc-300 hover:text-white transition-all duration-300 opacity-0 group-hover:opacity-100 cursor-pointer hover:scale-110 z-20"
              >
                <ChevronRight className="w-5 h-5" />
              </button>

              {/* Pagination indicators */}
            </>
          )}
        </div>

        {/* Main Redesigned Layout Details Grid */}
        <div className="px-6 md:px-8 -mt-20 md:-mt-28 relative z-10 space-y-8">

          {/* Top Hero Card Panel */}
          <div className="bg-zinc-900/60 border border-zinc-800/80 backdrop-blur-md rounded-3xl p-6 md:p-8 flex flex-col md:flex-row gap-6 md:gap-8 items-start shadow-2xl">
            {/* Left: Poster */}
            <div className="w-48 md:w-56 h-72 md:h-80 bg-neutral-950 rounded-2xl overflow-hidden shadow-2xl border border-zinc-800 flex-shrink-0 relative group">
              {mv.primaryPoster ? (
                <LazyImage
                  src={mv.primaryPoster}
                  alt={mv.title}
                  containerClassName="absolute inset-0 w-full h-full border-none rounded-none"
                  className="transition-transform duration-500 group-hover:scale-105"
                />
              ) : (
                <div className="w-full h-full flex flex-col items-center justify-center text-zinc-700">
                  <ImageIcon className="w-10 h-10 mb-2" />
                  <span className="text-[9px] uppercase tracking-wider font-bold">Chưa có poster</span>
                </div>
              )}
              {mv.ageRating && (
                <div className="absolute top-4 left-4 bg-brand-orange text-zinc-950 font-black px-2.5 py-1 rounded-lg text-[10px] shadow-lg border border-brand-orange/30">
                  {mv.ageRating}
                </div>
              )}
            </div>

            {/* Right: Titles, Genres, Synopsis */}
            <div className="flex-grow space-y-4">
              <div className="space-y-1.5">
                <h2 className="text-2xl md:text-3xl lg:text-4xl font-black tracking-tight text-white">{mv.title}</h2>
                {mv.originalTitle && (
                  <p className="text-sm text-zinc-400 italic font-medium">
                    Tên gốc: <span className="text-zinc-300 font-bold">{mv.originalTitle}</span>
                  </p>
                )}
              </div>

              {/* Genres Pills */}
              {Array.isArray(mv.genres) && mv.genres.length > 0 && (
                <div className="flex flex-wrap gap-2">
                  {mv.genres.map(g => (
                    <span
                      key={g}
                      className="bg-zinc-950 border border-zinc-800 text-zinc-300 text-[10px] px-3.5 py-1.5 rounded-full font-bold hover:border-zinc-700 transition-colors uppercase tracking-wider"
                    >
                      {g}
                    </span>
                  ))}
                </div>
              )}

              {/* Synopsis */}
              <div className="space-y-1.5">
                <h4 className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">Nội dung tóm tắt</h4>
                <p className="text-zinc-300 text-xs md:text-sm leading-relaxed font-light">{mv.synopsis || 'Chưa có tóm tắt nội dung phim.'}</p>
              </div>
            </div>
          </div>

          {/* Bottom Grid: 2 Columns */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">

            {/* Column 1: Cast & Crew & Production Companies (lg:col-span-2) */}
            <div className="lg:col-span-2 space-y-6">

              {/* Cast & Crew Card */}
              {((mv.directors && mv.directors.length > 0) ||
                (mv.actors && mv.actors.length > 0) ||
                (mv.writers && mv.writers.length > 0) ||
                (mv.producers && mv.producers.length > 0)) && (
                  <div className="bg-brand-gray/25 border border-zinc-900 p-6 rounded-3xl space-y-6 shadow-xl">
                    <h4 className="text-xs font-black text-white uppercase tracking-wider flex items-center gap-2 border-b border-zinc-800 pb-3">
                      <Users className="w-4.5 h-4.5 text-brand-orange" />
                      <span>Đoàn làm phim</span>
                    </h4>

                    {/* Directors */}
                    {mv.directors && mv.directors.length > 0 && (
                      <div className="space-y-2.5">
                        <p className="text-[10px] font-black text-zinc-500 uppercase tracking-wider">Đạo diễn</p>
                        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                          {mv.directors.map((d, dIdx) => (
                            <div key={d.publicId || dIdx} className="flex items-center gap-3 bg-zinc-900/60 border border-zinc-900/80 rounded-2xl px-4 py-2.5 hover:border-zinc-800 transition-colors">
                              <LazyImage
                                src={d.profileImageUrl || DEFAULT_AVATAR}
                                alt={d.fullName}
                                containerClassName="w-9 h-9 rounded-full border border-zinc-800 flex-shrink-0"
                                className="rounded-full object-cover"
                                onError={e => { e.target.src = DEFAULT_AVATAR; }}
                              />
                              <div className="min-w-0">
                                <p className="text-xs font-bold text-zinc-200 truncate">{d.fullName}</p>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Writers */}
                    {mv.writers && mv.writers.length > 0 && (
                      <div className="space-y-2.5">
                        <p className="text-[10px] font-black text-zinc-500 uppercase tracking-wider">Biên kịch</p>
                        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                          {mv.writers.map((w, wIdx) => (
                            <div key={w.publicId || wIdx} className="flex items-center gap-3 bg-zinc-900/60 border border-zinc-900/80 rounded-2xl px-4 py-2.5 hover:border-zinc-800 transition-colors">
                              <LazyImage
                                src={w.profileImageUrl || DEFAULT_AVATAR}
                                alt={w.fullName}
                                containerClassName="w-9 h-9 rounded-full border border-zinc-800 flex-shrink-0"
                                className="rounded-full object-cover"
                                onError={e => { e.target.src = DEFAULT_AVATAR; }}
                              />
                              <div className="min-w-0">
                                <p className="text-xs font-bold text-zinc-200 truncate">{w.fullName}</p>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Producers */}
                    {mv.producers && mv.producers.length > 0 && (
                      <div className="space-y-2.5">
                        <p className="text-[10px] font-black text-zinc-500 uppercase tracking-wider">Nhà sản xuất</p>
                        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                          {mv.producers.map((p, pIdx) => (
                            <div key={p.publicId || pIdx} className="flex items-center gap-3 bg-zinc-900/60 border border-zinc-900/80 rounded-2xl px-4 py-2.5 hover:border-zinc-800 transition-colors">
                              <LazyImage
                                src={p.profileImageUrl || DEFAULT_AVATAR}
                                alt={p.fullName}
                                containerClassName="w-9 h-9 rounded-full border border-zinc-800 flex-shrink-0"
                                className="rounded-full object-cover"
                                onError={e => { e.target.src = DEFAULT_AVATAR; }}
                              />
                              <div className="min-w-0">
                                <p className="text-xs font-bold text-zinc-200 truncate">{p.fullName}</p>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Actors */}
                    {mv.actors && mv.actors.length > 0 && (
                      <div className="space-y-2.5">
                        <p className="text-[10px] font-black text-zinc-500 uppercase tracking-wider">Diễn viên chính</p>
                        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                          {mv.actors.map((a, aIdx) => (
                            <div key={a.publicId || aIdx} className="flex items-center gap-3 bg-zinc-900/60 border border-zinc-900/80 rounded-2xl px-4 py-2.5 hover:border-zinc-800 transition-colors">
                              <LazyImage
                                src={a.profileImageUrl || DEFAULT_AVATAR}
                                alt={a.fullName}
                                containerClassName="w-9 h-9 rounded-full border border-zinc-800 flex-shrink-0"
                                className="rounded-full object-cover"
                                onError={e => { e.target.src = DEFAULT_AVATAR; }}
                              />
                              <div className="min-w-0">
                                <p className="text-xs font-bold text-zinc-200 truncate">{a.fullName}</p>
                                {a.characterName && <p className="text-[9px] text-zinc-500 truncate mt-0.5">{a.characterName}</p>}
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )}

              {/* Production Companies Card */}
              {((mv.productionCompanies && mv.productionCompanies.length > 0) ||
                (mv.studios && mv.studios.length > 0)) && (
                  <div className="bg-brand-gray/25 border border-zinc-900 p-6 rounded-3xl space-y-4 shadow-xl">
                    <h4 className="text-xs font-black text-white uppercase tracking-wider flex items-center gap-2 border-b border-zinc-800 pb-3">
                      <Building2 className="w-4.5 h-4.5 text-brand-orange" />
                      <span>Hãng sản xuất</span>
                    </h4>
                    <div className="flex flex-wrap gap-3">
                      {(mv.productionCompanies && mv.productionCompanies.length > 0 ? mv.productionCompanies : mv.studios).map((c, cIdx) => (
                        <div key={c.publicId || cIdx} className="flex items-center gap-3 bg-zinc-900/60 border border-zinc-900/80 rounded-2xl px-4 py-2.5 hover:border-zinc-800 transition-colors">
                          {c.logoUrl && (
                            <div className="h-6 max-w-[90px] flex-shrink-0">
                              <LazyImage
                                src={c.logoUrl}
                                alt={c.name}
                                containerClassName="h-6 max-w-[90px] border-none rounded-none bg-transparent"
                                className="object-contain filter brightness-95"
                                onError={e => { e.target.style.display = 'none'; }}
                              />
                            </div>
                          )}
                          <span className="text-xs font-semibold text-zinc-200">{c.name}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
            </div>

            {/* Column 2: Technical Info & Release Versions & Trailer (lg:col-span-1) */}
            <div className="space-y-6">

              {/* Technical Metadata Card */}
              <div className="bg-brand-gray/25 border border-zinc-900 rounded-3xl p-5 space-y-4 shadow-xl">
                <h3 className="text-[10px] font-black text-zinc-500 uppercase tracking-widest flex items-center gap-2 border-b border-zinc-800/85 pb-2.5">
                  <Info className="w-4 h-4 text-brand-orange" />
                  <span>Thông tin chi tiết</span>
                </h3>

                <div className="space-y-3 text-xs">
                  <div className="flex justify-between items-center border-b border-zinc-900/60 pb-2.5">
                    <span className="text-zinc-500 font-bold uppercase tracking-wider flex items-center gap-1.5"><Clock className="w-3.5 h-3.5" /> Thời lượng</span>
                    <span className="text-zinc-200 font-semibold">{mv.durationMinutes ? `${mv.durationMinutes} phút` : 'N/A'}</span>
                  </div>

                  <div className="flex justify-between items-center border-b border-zinc-900/60 pb-2.5">
                    <span className="text-zinc-500 font-bold uppercase tracking-wider flex items-center gap-1.5"><Calendar className="w-3.5 h-3.5" /> Khởi chiếu</span>
                    <span className="text-zinc-200 font-semibold">{formatDate(mv.releaseDate)}</span>
                  </div>

                  {mv.endDate && (
                    <div className="flex justify-between items-center border-b border-zinc-900/60 pb-2.5">
                      <span className="text-zinc-500 font-bold uppercase tracking-wider flex items-center gap-1.5"><Calendar className="w-3.5 h-3.5" /> Kết thúc</span>
                      <span className="text-zinc-200 font-semibold">{formatDate(mv.endDate)}</span>
                    </div>
                  )}

                  <div className="flex justify-between items-center border-b border-zinc-900/60 pb-2.5">
                    <span className="text-zinc-500 font-bold uppercase tracking-wider flex items-center gap-1.5"><Globe className="w-3.5 h-3.5" /> Quốc gia</span>
                    <span className="text-zinc-200 font-semibold">{mv.country || 'N/A'}</span>
                  </div>

                  <div className="flex justify-between items-center pt-1">
                    <span className="text-zinc-500 font-bold uppercase tracking-wider flex items-center gap-1.5"><Film className="w-3.5 h-3.5" /> Trạng thái</span>
                    <span className={`text-[9px] font-black px-2 py-0.5 rounded border uppercase shadow-sm ${STATUS_COLORS[mv.status] || ''}`}>
                      {STATUS_LABELS[mv.status] || mv.status}
                    </span>
                  </div>
                </div>
              </div>

              {/* Movie Screening Versions Card */}
              {mv.versions && mv.versions.length > 0 && (
                <div className="bg-brand-gray/25 border border-zinc-900 rounded-3xl p-5 space-y-4 shadow-xl">
                  <h3 className="text-[10px] font-black text-zinc-500 uppercase tracking-widest flex items-center gap-2 border-b border-zinc-800/85 pb-2.5">
                    <LayoutList className="w-4 h-4 text-brand-orange" />
                    <span>Phiên bản phát hành</span>
                  </h3>
                  <div className="grid grid-cols-1 gap-2.5">
                    {mv.versions.map((ver, idx) => (
                      <div key={ver.publicId || idx} className="bg-zinc-900/60 border border-zinc-900 px-3.5 py-2.5 rounded-2xl flex flex-col gap-1.5">
                        <div className="flex justify-between items-center">
                          <span className="text-xs font-bold text-zinc-200">{ver.versionName}</span>
                          <span className="text-[9px] bg-brand-orange/10 border border-brand-orange/20 text-brand-orange px-1.5 py-0.5 rounded font-black">{ver.format}</span>
                        </div>
                        <div className="flex gap-2 text-[9px] text-zinc-400">
                          <span>Âm thanh: <strong className="text-zinc-300">{ver.audioLanguage}</strong></span>
                          <span>•</span>
                          {ver.subtitleLanguage && <span>Phụ đề: <strong className="text-zinc-300">{ver.subtitleLanguage}</strong></span>}
                          {ver.dubLanguage && ver.dubLanguage !== 'NONE' && (
                            <>
                              <span>•</span>
                              <span>Lồng tiếng: <strong className="text-zinc-300">{ver.dubLanguage}</strong></span>
                            </>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Official Trailer Card */}
              {embedTrailerUrl && (
                <div className="bg-brand-gray/25 border border-zinc-900 p-5 rounded-3xl space-y-3.5 shadow-xl">
                  <h4 className="text-[10px] font-black text-zinc-500 uppercase tracking-widest flex items-center gap-2 border-b border-zinc-800/85 pb-2.5">
                    <Play className="w-4 h-4 text-brand-orange animate-pulse" />
                    <span>Trailer phim</span>
                  </h4>
                  <div className="relative w-full aspect-video rounded-2xl overflow-hidden border border-zinc-900 shadow-2xl bg-black group/trailer-detail">
                    {playDetailTrailer ? (
                      <iframe
                        src={`${embedTrailerUrl}?autoplay=1`}
                        title={`${mv.title} Official Trailer`}
                        className="absolute inset-0 w-full h-full border-none"
                        allow="autoplay; accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                        allowFullScreen
                      />
                    ) : (
                      <div 
                        onClick={() => setPlayDetailTrailer(true)}
                        className="absolute inset-0 cursor-pointer flex items-center justify-center select-none group/play"
                      >
                        <img 
                          src={`https://img.youtube.com/vi/${getYoutubeId(trailerMedia?.url)}/hqdefault.jpg`} 
                          alt={`${mv.title} Trailer preview`} 
                          className="w-full h-full object-cover opacity-70 group-hover/play:opacity-90 transition-opacity duration-300"
                          onError={(e) => {
                            e.target.src = "https://img.youtube.com/vi/" + getYoutubeId(trailerMedia?.url) + "/0.jpg";
                          }}
                        />
                        <div className="absolute inset-0 bg-black/20 group-hover/play:bg-black/10 transition-colors duration-300" />
                        <div className="absolute w-14 h-14 rounded-full bg-brand-orange/90 group-hover/play:bg-brand-orange text-zinc-950 flex items-center justify-center shadow-2xl transition-all duration-300 group-hover/play:scale-110">
                          <Play className="w-6 h-6 fill-current ml-1" />
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>

          </div>
        </div>
      </div>
    );
  }

  if (isFormOpen) {
    const isEdit = !!selectedMovie;
    return (
      <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto bg-zinc-950 text-zinc-100 space-y-5 animate-fade-in">
        {/* Header */}
        <div className="flex justify-between items-center border-b border-zinc-800 pb-4 flex-shrink-0">
          <div className="flex items-center gap-3">
            <button type="button" onClick={() => setIsFormOpen(false)} className="p-2 text-zinc-400 hover:text-white bg-brand-gray border border-zinc-800/80 rounded-xl transition-all cursor-pointer">
              <ArrowLeft className="w-4 h-4" />
            </button>
            <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider">
              {isEdit ? 'CẬP NHẬT PHIM' : 'THÊM PHIM MỚI'}
            </h1>
          </div>
          <div className="flex items-center gap-3">
            <button type="button" onClick={() => setIsFormOpen(false)}
              className="border border-zinc-800 bg-brand-gray hover:opacity-90 text-zinc-300 font-bold py-2 px-5 rounded-xl text-xs transition-colors cursor-pointer">
              Hủy
            </button>
            {!isEdit && (
              <button type="button" onClick={handleCleanForm}
                className="border border-zinc-805 bg-zinc-900/60 hover:bg-zinc-800 text-zinc-300 font-bold py-2 px-5 rounded-xl text-xs transition-colors cursor-pointer">
                Dọn sạch
              </button>
            )}
            <button type="button" onClick={handleSave} disabled={isSaving}
              className="bg-brand-orange hover:opacity-90 text-zinc-950 font-black py-2 px-6 rounded-xl text-xs uppercase tracking-wider transition-all shadow-lg flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50">
              {isSaving ? (
                <div className="w-4 h-4 border-2 border-zinc-950 border-t-transparent rounded-full animate-spin" />
              ) : (
                <><Check className="w-4 h-4" /><span>LƯU LẠI</span></>
              )}
            </button>
          </div>
        </div>

        {/* TMDB Latest Movies Carousel (only on Add) */}
        {!isEdit && (isLatestMoviesLoading || tmdbLatestMovies.length > 0) && (
          <div className="relative w-full bg-brand-gray/60 border border-zinc-805 p-5 rounded-2xl shadow-lg flex-shrink-0 group overflow-hidden">
            <label className="text-brand-orange text-[10px] font-black uppercase tracking-widest block mb-4">
              GỢI Ý PHIM MỚI TỪ TMDB
            </label>
            <div className="relative px-6">
              {isLatestMoviesLoading ? (
                <div className="grid grid-cols-5 gap-3">
                  {Array.from({ length: 5 }).map((_, idx) => (
                    <div key={idx} className="flex flex-col items-center gap-2 animate-pulse w-full">
                      <div className="w-full aspect-[2/3] bg-zinc-900 border border-zinc-850 rounded-xl" />
                      <div className="h-3 bg-zinc-900 rounded w-3/4 mt-1" />
                      <div className="h-3 bg-zinc-900 rounded w-1/2" />
                    </div>
                  ))}
                </div>
              ) : (
                <>
                  {/* Left arrow */}
                  <button
                    type="button"
                    onClick={slideLeft}
                    className="absolute left-0 top-[40%] -translate-y-1/2 z-10 bg-brand-dark/95 hover:bg-brand-orange hover:text-zinc-950 border border-zinc-800 text-zinc-300 p-2 rounded-full opacity-0 group-hover:opacity-100 -translate-x-3 group-hover:translate-x-0 transition-all duration-300 cursor-pointer shadow-xl"
                  >
                    <ChevronLeft className="w-4 h-4" />
                  </button>

                  {/* Movie Cards Viewport */}
                  <div className="overflow-hidden">
                    {/* Sliding Flex Track */}
                    <div
                      className={`flex w-[140%] -ml-[20%] ${isTransitioning ? 'transition-transform duration-300 ease-in-out' : ''}`}
                      style={{ transform: `translateX(${slideOffset}%)` }}
                    >
                      {Array.from({ length: 7 }).map((_, i) => {
                        const offset = i - 1;
                        const m = tmdbLatestMovies[(latestMoviesPageIndex + offset + tmdbLatestMovies.length) % tmdbLatestMovies.length];
                        if (!m) return null;
                        const posterUrl = m.posterPath ? `https://image.tmdb.org/t/p/w185${m.posterPath}` : null;
                        return (
                          <div key={`${m.tmdbId}-${i}`} className="w-[14.2857%] flex-shrink-0 px-1.5">
                            <button
                              type="button"
                              onClick={() => handleSelectTmdb(m.tmdbId)}
                              className="flex flex-col items-center gap-2 group/item cursor-pointer text-center bg-transparent border-none p-0 m-0 w-full outline-none focus:outline-none"
                            >
                              <div className="w-full aspect-[2/3] bg-zinc-900 border border-zinc-800 group-hover/item:border-brand-orange/50 rounded-xl overflow-hidden shadow-md group-hover/item:shadow-brand-orange/15 group-hover/item:scale-105 transition-all duration-300 relative">
                                {posterUrl ? (
                                  <LazyImage
                                    src={posterUrl}
                                    alt={m.title}
                                    containerClassName="absolute inset-0 w-full h-full border-none rounded-none"
                                  />
                                ) : (
                                  <div className="w-full h-full flex items-center justify-center text-zinc-700">
                                    <ImageIcon className="w-5 h-5" />
                                  </div>
                                )}
                              </div>
                              <span className="text-[10px] font-bold text-zinc-300 group-hover/item:text-brand-orange line-clamp-2 leading-tight transition-colors w-full h-8 flex items-start justify-center">
                                {m.title}
                              </span>
                            </button>
                          </div>
                        );
                      })}
                    </div>
                  </div>

                  {/* Right arrow */}
                  <button
                    type="button"
                    onClick={slideRight}
                    className="absolute right-0 top-[40%] -translate-y-1/2 z-10 bg-brand-dark/95 hover:bg-brand-orange hover:text-zinc-950 border border-zinc-800 text-zinc-300 p-2 rounded-full opacity-0 group-hover:opacity-100 translate-x-3 group-hover:translate-x-0 transition-all duration-300 cursor-pointer shadow-xl"
                  >
                    <ChevronRight className="w-4 h-4" />
                  </button>
                </>
              )}
            </div>
          </div>
        )}

        {/* TMDB Import (only on Add) */}
        {!isEdit && (
          <div className="relative w-full bg-brand-gray/60 border border-zinc-800 p-5 rounded-2xl space-y-2 shadow-lg flex-shrink-0">
            <label className="text-brand-orange text-[10px] font-black uppercase tracking-widest block">
              TÌM KIẾM & IMPORT DỮ LIỆU TỪ TMDB
            </label>
            <div className="relative">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500 pointer-events-none" />
              <input
                type="text"
                value={tmdbSearch}
                onChange={e => { setTmdbSearch(e.target.value); setShowSuggestions(true); }}
                onFocus={() => setShowSuggestions(true)}
                onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
                placeholder="Nhập tên phim để tự động điền dữ liệu (vd: Inception, Avengers...)"
                className="w-full bg-brand-dark border border-zinc-800 focus:border-brand-orange/50 rounded-xl py-3 pl-10 pr-4 text-xs transition-all text-zinc-100 placeholder-zinc-500 outline-none"
              />
              {(isTmdbSearching || isTmdbLoading) && (
                <div className="absolute right-3.5 top-3.5">
                  <div className="w-4 h-4 border-2 border-brand-orange border-t-transparent rounded-full animate-spin" />
                </div>
              )}
            </div>
            {showSuggestions && tmdbSuggestions.length > 0 && (
              <div className="absolute left-5 right-5 top-full mt-1 bg-zinc-900 border border-zinc-800 rounded-xl shadow-2xl z-50 overflow-hidden divide-y divide-zinc-800 max-h-64 overflow-y-auto">
                {tmdbSuggestions.map(s => (
                  <button key={s.tmdbId} type="button" onMouseDown={() => handleSelectTmdb(s.tmdbId)}
                    className="w-full flex items-center gap-3.5 p-3 hover:bg-zinc-800 transition-colors text-left">
                    <div className="w-8 h-12 bg-neutral-900 rounded flex-shrink-0 overflow-hidden">
                      {s.posterThumbnailUrl ? (
                        <LazyImage
                          src={s.posterThumbnailUrl}
                          alt={s.title}
                          containerClassName="w-full h-full border-none rounded-none"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-zinc-700"><ImageIcon className="w-4 h-4" /></div>
                      )}
                    </div>
                    <div>
                      <p className="text-xs font-bold text-zinc-200">{s.title}</p>
                      <p className="text-[10px] text-zinc-500 mt-0.5">{s.releaseYear || ''}</p>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Tab Selector */}
        <div className="flex border-b border-zinc-800 flex-shrink-0">
          {[
            { id: 'basic', label: 'Thông tin cơ bản' },
            { id: 'crew', label: 'Nhân sự & Hãng phim' },
            { id: 'media', label: 'Hình ảnh & Trailer' }
          ].map(tab => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setActiveFormTab(tab.id)}
              className={`py-3 px-6 text-xs md:text-sm font-bold border-b-2 transition-all cursor-pointer uppercase tracking-wider ${activeFormTab === tab.id
                ? 'border-brand-orange text-brand-orange bg-brand-orange/5 font-black'
                : 'border-transparent text-zinc-400 hover:text-zinc-200'
                }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Form Body */}
        <form onSubmit={e => e.preventDefault()} className="pb-16">

          {/* TAB 1: Thông tin cơ bản */}
          {activeFormTab === 'basic' && (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 animate-fade-in">
              <div className="lg:col-span-2 space-y-6">

                {/* Basic Info */}
                <FormSection icon={<Film className="w-4 h-4 text-brand-orange" />} title="Thông Tin Cơ Bản">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <Field label="Tên phim" required error={formErrors.title}>
                      <Input value={formBasic.title} onChange={e => setFormBasic(p => ({ ...p, title: e.target.value }))} />
                    </Field>
                    <Field label="Tên gốc (Nguyên bản)">
                      <Input value={formBasic.originalTitle} onChange={e => setFormBasic(p => ({ ...p, originalTitle: e.target.value }))} />
                    </Field>
                    <Field label="Thời lượng (phút)" required error={formErrors.durationMinutes}>
                      <Input type="number" min="1" value={formBasic.durationMinutes} onChange={e => setFormBasic(p => ({ ...p, durationMinutes: e.target.value }))} />
                    </Field>
                    <Field label="Quốc gia sản xuất">
                      <Input value={formBasic.country} onChange={e => setFormBasic(p => ({ ...p, country: e.target.value }))} placeholder="Vd: United States of America" />
                    </Field>
                    <Field label="Giới hạn độ tuổi" required error={formErrors.ageRating}>
                      <Select value={formBasic.ageRating} onChange={e => setFormBasic(p => ({ ...p, ageRating: e.target.value }))}>
                        {AGE_RATINGS.map(r => <option key={r} value={r}>{AGE_RATING_LABELS[r]}</option>)}
                      </Select>
                    </Field>

                    {isEdit && (
                      <>
                        <Field label="Trạng thái">
                          <Select value={formBasic.status} onChange={e => setFormBasic(p => ({ ...p, status: e.target.value }))}>
                            {Object.entries(STATUS_LABELS).map(([k, v]) => (
                              <option key={k} value={k}>{v}</option>
                            ))}
                          </Select>
                        </Field>
                        <div className="col-span-1 md:col-span-2 bg-zinc-900/40 border border-zinc-800/80 rounded-2xl p-4 mt-2 space-y-2">
                          <p className="text-xs font-bold text-zinc-300 flex items-center gap-1.5">
                            <Info className="w-4 h-4 text-brand-orange" />
                            <span>Quy định thiết lập trạng thái phim:</span>
                          </p>
                          <ul className="text-[11px] text-zinc-400 space-y-1.5 list-disc pl-4 leading-relaxed">
                            <li><strong>Nháp (DRAFT) / Ngừng hoạt động (INACTIVE):</strong> Dùng khi phim đang được biên tập hoặc tạm ẩn. Không ràng buộc ngày chiếu.</li>
                            <li><strong>Sắp chiếu (UPCOMING):</strong> Ngày khởi chiếu phải ở <strong>tương lai</strong> (sau hôm nay). Phải gán ít nhất 1 thể loại.</li>
                            <li><strong>Đang chiếu (NOW_SHOWING):</strong> Ngày khởi chiếu ở <strong>quá khứ hoặc hôm nay</strong> và Ngày ngừng chiếu (nếu có) phải ở <strong>tương lai hoặc hôm nay</strong>. Phải có ít nhất 1 thể loại.</li>
                            <li><strong>Đã chiếu xong (ENDED):</strong> Bắt buộc phải chọn Ngày ngừng chiếu và ngày này phải ở <strong>quá khứ</strong> (trước hôm nay).</li>
                          </ul>
                        </div>
                      </>
                    )}
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-2">
                    <Field label="Ngày khởi chiếu (tại rạp)" required error={formErrors.showingStartDate}>
                      <Input type="date" value={formBasic.showingStartDate} onChange={e => setFormBasic(p => ({ ...p, showingStartDate: e.target.value }))} />
                    </Field>
                    <Field label="Ngày ngừng chiếu" error={formErrors.endDate}>
                      <Input type="date" value={formBasic.endDate} onChange={e => setFormBasic(p => ({ ...p, endDate: e.target.value }))} />
                    </Field>
                  </div>

                  <Field label="Nội dung tóm tắt">
                    <Textarea rows={5} value={formBasic.synopsis} onChange={e => setFormBasic(p => ({ ...p, synopsis: e.target.value }))} />
                  </Field>

                  {!isEdit && (
                    <div className="flex items-center gap-2 bg-blue-950/30 border border-blue-800/30 rounded-xl p-3 text-[11px] text-blue-300">
                      <Info className="w-4 h-4 flex-shrink-0 text-blue-400" />
                      <span>Phim mới tạo sẽ có trạng thái hoạt động (ACTIVE) mặc định. Dùng tính năng cập nhật để thay đổi trạng thái sau.</span>
                    </div>
                  )}
                </FormSection>

                {/* Genres */}
                <FormSection icon={<LayoutList className="w-4 h-4 text-brand-orange" />} title="Thể Loại Phim">
                  {formErrors.genres && (
                    <div className="text-red-400 text-xs mb-4 bg-red-950/20 border border-red-800/30 rounded-xl p-2.5 flex items-center gap-2">
                      <span className="w-1.5 h-1.5 rounded-full bg-red-500 animate-pulse" />
                      <span>{formErrors.genres}</span>
                    </div>
                  )}

                  {tmdbGenres.length > 0 && (
                    <div className="space-y-2 mb-4">
                      <p className="text-[10px] font-black text-zinc-500 uppercase tracking-wider flex items-center gap-1.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-brand-orange inline-block" />
                        Thể loại từ TMDB
                      </p>
                      <div className="flex flex-wrap gap-2">
                        {tmdbGenres.map(tg => (
                          <div
                            key={tg.tmdbId ?? tg.name}
                            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl border text-xs font-semibold select-none ${tg.existsInDb
                              ? 'bg-emerald-500/10 border-emerald-500/25 text-emerald-400'
                              : 'bg-amber-500/10  border-amber-500/25  text-amber-300'
                              }`}
                          >
                            {tg.existsInDb ? <Check className="w-3 h-3 flex-shrink-0" /> : <Plus className="w-3 h-3 flex-shrink-0" />}
                            <span>{tg.name}</span>
                            {!tg.existsInDb && (
                              <span className="text-[9px] font-black bg-amber-400/20 text-amber-200 px-1 py-0.5 rounded uppercase ml-0.5">Tự tạo</span>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {genresList.length > 0 && (
                    <div className="space-y-2">
                      {tmdbGenres.length > 0 && <p className="text-[10px] font-black text-zinc-500 uppercase tracking-wider">Chọn thêm thể loại</p>}
                      <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                        {genresList.map(g => {
                          const checked = selectedGenres.some(s => s.publicId === g.publicId);
                          const fromTmdb = tmdbGenres.some(tg => tg.existsInDb && tg.dbPublicId === g.publicId);
                          return (
                            <label key={g.publicId}
                              className={`flex items-center gap-2 p-2.5 border rounded-xl cursor-pointer transition-all select-none text-xs ${checked && fromTmdb
                                ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300'
                                : checked
                                  ? 'border-brand-orange/30 bg-brand-orange/10 text-brand-orange'
                                  : 'border-zinc-800 bg-brand-dark text-zinc-400 hover:text-zinc-200'
                                }`}>
                              <input type="checkbox" checked={checked} onChange={() => toggleGenre(g)} className="hidden" />
                              {checked ? <Check className="w-3.5 h-3.5 flex-shrink-0" /> : <div className="w-3.5 h-3.5 border border-zinc-700 rounded-sm flex-shrink-0" />}
                              <span className="truncate">{g.name}</span>
                              {fromTmdb && checked && <span className="text-[9px] text-emerald-500 ml-auto flex-shrink-0">TMDB</span>}
                            </label>
                          );
                        })}
                      </div>
                    </div>
                  )}
                </FormSection>
              </div>

              {/* Versions Column */}
              <div className="space-y-6">
                <FormSection icon={<Film className="w-4 h-4 text-brand-orange" />} title="Phiên Bản Chiếu"
                  headerAction={
                    <button type="button" onClick={addVersion} className="text-[10px] font-black text-brand-orange hover:opacity-80 uppercase flex items-center gap-1">
                      <Plus className="w-3.5 h-3.5" />THÊM PHIÊN BẢN
                    </button>
                  }>
                  {versions.length === 0 ? (
                    <p className="text-zinc-500 text-xs py-4 text-center italic">Chưa có phiên bản nào. Click "THÊM PHIÊN BẢN" để cấu hình.</p>
                  ) : (
                    versions.map((ver, vIdx) => (
                      <div key={vIdx} className="relative p-4 bg-brand-dark border border-zinc-800 rounded-xl mb-3 last:mb-0">
                        <button type="button" onClick={() => removeVersion(vIdx)} className="absolute top-3 right-3 text-zinc-600 hover:text-red-400 transition-colors">
                          <Trash className="w-3.5 h-3.5" />
                        </button>
                        <div className="grid grid-cols-1 gap-2.5 pr-6">
                          {[
                            ['Tên phiên bản', 'versionName', 'text', { placeholder: 'Vd: 2D Vietsub' }],
                            ['Định dạng', 'format', 'select', { opts: ['2D', '3D', 'IMAX', '4DX', 'SCREENX'] }],
                            ['Ngôn ngữ thoại', 'audioLanguage', 'select', { opts: ['VI', 'EN', 'JA', 'KO', 'TH', 'ZH', 'FR', 'ES'] }],
                            ['Phụ đề', 'subtitleLanguage', 'select', { opts: ['VI', 'EN', 'NONE'] }],
                            ['Lồng tiếng', 'dubLanguage', 'select', { opts: ['NONE', 'VI', 'EN'] }],
                            isEdit && ['Tình trạng (Condition)', 'status', 'select', { opts: ['ACTIVE', 'INACTIVE'] }],
                          ].filter(Boolean).map(([lbl, field, type, opts]) => (
                            <div key={field} className="space-y-1">
                              <p className="text-[9px] font-black uppercase text-zinc-500">{lbl}</p>
                              {type === 'select' ? (
                                <select value={ver[field] || ''} onChange={e => updateVersion(vIdx, field, e.target.value)}
                                  className="w-full bg-zinc-900 border border-zinc-800 rounded-lg py-1.5 px-2 text-xs text-zinc-100 outline-none">
                                  {Array.from(new Set([...opts.opts, ver[field]].filter(Boolean))).map(o => (
                                    <option key={o} value={o}>{o === 'NONE' ? 'NONE (Không)' : o}</option>
                                  ))}
                                </select>
                              ) : (
                                <input type="text" value={ver[field] || ''} onChange={e => updateVersion(vIdx, field, e.target.value)}
                                  placeholder={opts.placeholder || ''}
                                  className="w-full bg-zinc-900 border border-zinc-800 rounded-lg py-1.5 px-2 text-xs text-zinc-100 focus:outline-none focus:border-brand-orange/30" />
                              )}
                            </div>
                          ))}
                        </div>
                      </div>
                    ))
                  )}
                </FormSection>
              </div>
            </div>
          )}

          {/* TAB 2: Nhân sự & Hãng phim (CRUD & Drag-and-drop) */}
          {activeFormTab === 'crew' && (
            <div className="grid grid-cols-1 gap-6 animate-fade-in max-w-4xl mx-auto">

              {/* Production Companies (Hãng phim) */}
              <FormSection icon={<Building2 className="w-4 h-4 text-brand-orange" />} title="Hãng Sản Xuất"
                headerAction={
                  <button type="button" onClick={() => setStudios(p => [...p, { name: '', logoUrl: '' }])}
                    className="text-[10px] font-black text-brand-orange hover:opacity-80 flex items-center gap-0.5">
                    <Plus className="w-3.5 h-3.5" />Thêm Hãng Phim
                  </button>
                }
              >
                {studios.length === 0 ? (
                  <p className="text-zinc-500 text-xs py-4 text-center italic">Chưa cấu hình hãng sản xuất nào cho phim này.</p>
                ) : (
                  <div className="space-y-2">
                    {studios.map((s, idx) => (
                      <div
                        key={idx}
                        draggable
                        onDragStart={() => handleDragStart(idx, 'studios')}
                        onDragOver={handleDragOver}
                        onDragEnter={() => handleDragEnter(idx, 'studios', studios, setStudios)}
                        onDragEnd={handleDragEnd}
                        className={`flex items-center gap-3 bg-zinc-900 border border-zinc-800 p-2.5 px-4 rounded-xl transition-all duration-200 ${draggedType === 'studios' && draggedIdx === idx ? 'opacity-40 scale-95 border-brand-orange/40 bg-neutral-950' : ''
                          }`}
                      >
                        <div className="cursor-grab text-zinc-650 hover:text-zinc-400">
                          <GripVertical className="w-4 h-4" />
                        </div>
                        <div className="w-8 h-6 bg-zinc-950 border border-zinc-800 rounded flex items-center justify-center overflow-hidden shrink-0">
                          {s.logoUrl ? (
                            <LazyImage
                              src={s.logoUrl}
                              alt=""
                              containerClassName="w-full h-full border-none rounded-none bg-transparent"
                              className="object-contain"
                              onError={e => { e.target.style.display = 'none'; }}
                            />
                          ) : (
                            <Building2 className="w-3.5 h-3.5 text-zinc-700" />
                          )}
                        </div>
                        <div className="flex-grow grid grid-cols-1 sm:grid-cols-2 gap-2">
                          <input
                            type="text"
                            value={s.name || ''}
                            onChange={e => setStudios(prev => prev.map((item, i) => i === idx ? { ...item, name: e.target.value } : item))}
                            placeholder="Tên hãng sản xuất (Bắt buộc)"
                            className="bg-brand-dark border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-brand-orange/30 min-w-0"
                          />
                          <input
                            type="text"
                            value={s.logoUrl || ''}
                            onChange={e => setStudios(prev => prev.map((item, i) => i === idx ? { ...item, logoUrl: e.target.value } : item))}
                            placeholder="URL logo (Tùy chọn)"
                            className="bg-brand-dark border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-brand-orange/30 min-w-0"
                          />
                        </div>
                        <button
                          type="button"
                          onClick={() => setStudios(prev => prev.filter((_, i) => i !== idx))}
                          className="p-1.5 text-zinc-500 hover:text-red-400 transition-colors"
                        >
                          <X className="w-4 h-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </FormSection>

              {/* Directors (Đạo diễn) */}
              <FormSection icon={<Users className="w-4 h-4 text-brand-orange" />} title="Đạo diễn"
                headerAction={
                  <button type="button" onClick={() => setDirectors(p => [...p, { name: '', profileUrl: '' }])}
                    className="text-[10px] font-black text-brand-orange hover:opacity-80 flex items-center gap-0.5">
                    <Plus className="w-3.5 h-3.5" />Thêm Đạo Diễn
                  </button>
                }
              >
                {directors.length === 0 ? (
                  <p className="text-zinc-500 text-xs py-4 text-center italic">Chưa có đạo diễn nào. Nhấp "Thêm Đạo Diễn" để bắt đầu.</p>
                ) : (
                  <div className="space-y-2">
                    {directors.map((d, idx) => (
                      <div
                        key={idx}
                        draggable
                        onDragStart={() => handleDragStart(idx, 'directors')}
                        onDragOver={handleDragOver}
                        onDragEnter={() => handleDragEnter(idx, 'directors', directors, setDirectors)}
                        onDragEnd={handleDragEnd}
                        className={`flex items-center gap-3 bg-zinc-900 border border-zinc-800 p-2.5 px-4 rounded-xl transition-all duration-200 ${draggedType === 'directors' && draggedIdx === idx ? 'opacity-40 scale-95 border-brand-orange/40 bg-neutral-950' : ''
                          }`}
                      >
                        <div className="cursor-grab text-zinc-650 hover:text-zinc-400">
                          <GripVertical className="w-4 h-4" />
                        </div>
                        <LazyImage
                          src={d.profileUrl || d.profileImageUrl || DEFAULT_AVATAR}
                          alt=""
                          containerClassName="w-7 h-7 rounded-full border border-zinc-800 shrink-0"
                          className="rounded-full object-cover"
                          onError={e => { e.target.src = DEFAULT_AVATAR; }}
                        />
                        <div className="flex-grow grid grid-cols-1 sm:grid-cols-2 gap-2">
                          <input
                            type="text"
                            value={d.name || ''}
                            onChange={e => setDirectors(prev => prev.map((item, i) => i === idx ? { ...item, name: e.target.value } : item))}
                            placeholder="Tên đạo diễn (Bắt buộc)"
                            className="bg-brand-dark border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-brand-orange/30 min-w-0"
                          />
                          <input
                            type="text"
                            value={d.profileUrl || d.profileImageUrl || ''}
                            onChange={e => setDirectors(prev => prev.map((item, i) => i === idx ? { ...item, profileUrl: e.target.value, profileImageUrl: e.target.value } : item))}
                            placeholder="URL ảnh đại diện (Tùy chọn)"
                            className="bg-brand-dark border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-brand-orange/30 min-w-0"
                          />
                        </div>
                        <button
                          type="button"
                          onClick={() => setDirectors(prev => prev.filter((_, i) => i !== idx))}
                          className="p-1.5 text-zinc-500 hover:text-red-400 transition-colors"
                        >
                          <X className="w-4 h-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </FormSection>

              {/* Actors / Cast (Diễn viên) */}
              <FormSection icon={<Users className="w-4 h-4 text-brand-orange" />} title="Diễn Viên Chính"
                headerAction={
                  <button type="button" onClick={() => setCast(p => [...p, { name: '', character: '', profileUrl: '' }])}
                    className="text-[10px] font-black text-brand-orange hover:opacity-80 flex items-center gap-0.5">
                    <Plus className="w-3.5 h-3.5" />Thêm Diễn Viên
                  </button>
                }
              >
                {cast.length === 0 ? (
                  <p className="text-zinc-500 text-xs py-4 text-center italic">Chưa có diễn viên nào. Nhấp "Thêm Diễn Viên" để bắt đầu.</p>
                ) : (
                  <div className="space-y-2">
                    {cast.map((c, idx) => (
                      <div
                        key={idx}
                        draggable
                        onDragStart={() => handleDragStart(idx, 'cast')}
                        onDragOver={handleDragOver}
                        onDragEnter={() => handleDragEnter(idx, 'cast', cast, setCast)}
                        onDragEnd={handleDragEnd}
                        className={`flex items-center gap-3 bg-zinc-900 border border-zinc-800 p-2.5 px-4 rounded-xl transition-all duration-200 ${draggedType === 'cast' && draggedIdx === idx ? 'opacity-40 scale-95 border-brand-orange/40 bg-neutral-950' : ''
                          }`}
                      >
                        <div className="cursor-grab text-zinc-650 hover:text-zinc-400">
                          <GripVertical className="w-4 h-4" />
                        </div>
                        <LazyImage
                          src={c.profileUrl || c.profileImageUrl || DEFAULT_AVATAR}
                          alt=""
                          containerClassName="w-7 h-7 rounded-full border border-zinc-800 shrink-0"
                          className="rounded-full object-cover"
                          onError={e => { e.target.src = DEFAULT_AVATAR; }}
                        />
                        <div className="flex-grow grid grid-cols-1 sm:grid-cols-3 gap-2">
                          <input
                            type="text"
                            value={c.name || ''}
                            onChange={e => setCast(prev => prev.map((item, i) => i === idx ? { ...item, name: e.target.value } : item))}
                            placeholder="Tên diễn viên (Bắt buộc)"
                            className="bg-brand-dark border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-brand-orange/30 min-w-0"
                          />
                          <input
                            type="text"
                            value={c.character || ''}
                            onChange={e => setCast(prev => prev.map((item, i) => i === idx ? { ...item, character: e.target.value } : item))}
                            placeholder="Vai diễn (Nhân vật)"
                            className="bg-brand-dark border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-brand-orange/30 min-w-0"
                          />
                          <input
                            type="text"
                            value={c.profileUrl || c.profileImageUrl || ''}
                            onChange={e => setCast(prev => prev.map((item, i) => i === idx ? { ...item, profileUrl: e.target.value, profileImageUrl: e.target.value } : item))}
                            placeholder="URL hình ảnh (Tùy chọn)"
                            className="bg-brand-dark border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-brand-orange/30 min-w-0"
                          />
                        </div>
                        <button
                          type="button"
                          onClick={() => setCast(prev => prev.filter((_, i) => i !== idx))}
                          className="p-1.5 text-zinc-500 hover:text-red-400 transition-colors"
                        >
                          <X className="w-4 h-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </FormSection>

              {/* Writers (Biên kịch) */}
              <FormSection icon={<Users className="w-4 h-4 text-brand-orange" />} title="Biên Kịch"
                headerAction={
                  <button type="button" onClick={() => setWriters(p => [...p, { name: '', profileUrl: '' }])}
                    className="text-[10px] font-black text-brand-orange hover:opacity-80 flex items-center gap-0.5">
                    <Plus className="w-3.5 h-3.5" />Thêm Biên Kịch
                  </button>
                }
              >
                {writers.length === 0 ? (
                  <p className="text-zinc-500 text-xs py-4 text-center italic">Chưa có biên kịch nào. Nhấp "Thêm Biên Kịch" để bắt đầu.</p>
                ) : (
                  <div className="space-y-2">
                    {writers.map((w, idx) => (
                      <div
                        key={idx}
                        draggable
                        onDragStart={() => handleDragStart(idx, 'writers')}
                        onDragOver={handleDragOver}
                        onDragEnter={() => handleDragEnter(idx, 'writers', writers, setWriters)}
                        onDragEnd={handleDragEnd}
                        className={`flex items-center gap-3 bg-zinc-900 border border-zinc-800 p-2.5 px-4 rounded-xl transition-all duration-200 ${draggedType === 'writers' && draggedIdx === idx ? 'opacity-40 scale-95 border-brand-orange/40 bg-neutral-950' : ''
                          }`}
                      >
                        <div className="cursor-grab text-zinc-650 hover:text-zinc-400">
                          <GripVertical className="w-4 h-4" />
                        </div>
                        <LazyImage
                          src={w.profileUrl || w.profileImageUrl || DEFAULT_AVATAR}
                          alt=""
                          containerClassName="w-7 h-7 rounded-full border border-zinc-800 shrink-0"
                          className="rounded-full object-cover"
                          onError={e => { e.target.src = DEFAULT_AVATAR; }}
                        />
                        <div className="flex-grow grid grid-cols-1 sm:grid-cols-2 gap-2">
                          <input
                            type="text"
                            value={w.name || ''}
                            onChange={e => setWriters(prev => prev.map((item, i) => i === idx ? { ...item, name: e.target.value } : item))}
                            placeholder="Tên biên kịch (Bắt buộc)"
                            className="bg-brand-dark border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-brand-orange/30 min-w-0"
                          />
                          <input
                            type="text"
                            value={w.profileUrl || w.profileImageUrl || ''}
                            onChange={e => setWriters(prev => prev.map((item, i) => i === idx ? { ...item, profileUrl: e.target.value, profileImageUrl: e.target.value } : item))}
                            placeholder="URL ảnh đại diện (Tùy chọn)"
                            className="bg-brand-dark border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-brand-orange/30 min-w-0"
                          />
                        </div>
                        <button
                          type="button"
                          onClick={() => setWriters(prev => prev.filter((_, i) => i !== idx))}
                          className="p-1.5 text-zinc-500 hover:text-red-400 transition-colors"
                        >
                          <X className="w-4 h-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </FormSection>

              {/* Producers (Nhà sản xuất) */}
              <FormSection icon={<Users className="w-4 h-4 text-brand-orange" />} title="Nhà Sản Xuất"
                headerAction={
                  <button type="button" onClick={() => setProducers(p => [...p, { name: '', profileUrl: '' }])}
                    className="text-[10px] font-black text-brand-orange hover:opacity-80 flex items-center gap-0.5">
                    <Plus className="w-3.5 h-3.5" />Thêm Nhà Sản Xuất
                  </button>
                }
              >
                {producers.length === 0 ? (
                  <p className="text-zinc-500 text-xs py-4 text-center italic">Chưa có nhà sản xuất nào. Nhấp "Thêm Nhà Sản Xuất" để bắt đầu.</p>
                ) : (
                  <div className="space-y-2">
                    {producers.map((p, idx) => (
                      <div
                        key={idx}
                        draggable
                        onDragStart={() => handleDragStart(idx, 'producers')}
                        onDragOver={handleDragOver}
                        onDragEnter={() => handleDragEnter(idx, 'producers', producers, setProducers)}
                        onDragEnd={handleDragEnd}
                        className={`flex items-center gap-3 bg-zinc-900 border border-zinc-800 p-2.5 px-4 rounded-xl transition-all duration-200 ${draggedType === 'producers' && draggedIdx === idx ? 'opacity-40 scale-95 border-brand-orange/40 bg-neutral-950' : ''
                          }`}
                      >
                        <div className="cursor-grab text-zinc-650 hover:text-zinc-400">
                          <GripVertical className="w-4 h-4" />
                        </div>
                        <LazyImage
                          src={p.profileUrl || p.profileImageUrl || DEFAULT_AVATAR}
                          alt=""
                          containerClassName="w-7 h-7 rounded-full border border-zinc-800 shrink-0"
                          className="rounded-full object-cover"
                          onError={e => { e.target.src = DEFAULT_AVATAR; }}
                        />
                        <div className="flex-grow grid grid-cols-1 sm:grid-cols-2 gap-2">
                          <input
                            type="text"
                            value={p.name || ''}
                            onChange={e => setProducers(prev => prev.map((item, i) => i === idx ? { ...item, name: e.target.value } : item))}
                            placeholder="Tên nhà sản xuất (Bắt buộc)"
                            className="bg-brand-dark border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-brand-orange/30 min-w-0"
                          />
                          <input
                            type="text"
                            value={p.profileUrl || p.profileImageUrl || ''}
                            onChange={e => setProducers(prev => prev.map((item, i) => i === idx ? { ...item, profileUrl: e.target.value, profileImageUrl: e.target.value } : item))}
                            placeholder="URL ảnh đại diện (Tùy chọn)"
                            className="bg-brand-dark border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-brand-orange/30 min-w-0"
                          />
                        </div>
                        <button
                          type="button"
                          onClick={() => setProducers(prev => prev.filter((_, i) => i !== idx))}
                          className="p-1.5 text-zinc-500 hover:text-red-400 transition-colors"
                        >
                          <X className="w-4 h-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </FormSection>
            </div>
          )}

          {/* TAB 3: Media & Trailer (Poster, Banners, Trailer) */}
          {activeFormTab === 'media' && (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 animate-fade-in">
              {/* Poster Section — NOT draggable, URL-only edit */}
              <div className="space-y-6">
                <div className="bg-brand-gray/60 border border-zinc-800/50 rounded-2xl p-5 space-y-5" draggable={false}>
                  <h3 className="text-xs font-black text-white uppercase tracking-wider border-b border-zinc-800 pb-2 flex items-center gap-1.5">
                    <ImageIcon className="w-4.5 h-4.5 text-brand-orange" />
                    <span>Poster phim</span>
                    <span className="ml-auto text-[9px] font-bold text-zinc-600 normal-case tracking-normal border border-zinc-800 bg-zinc-900 rounded px-1.5 py-0.5">
                      Chỉ đổi qua URL
                    </span>
                  </h3>
                  <div className="space-y-2">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Poster URL</label>
                    <input type="text" value={posterUrl} onChange={e => setPosterUrl(e.target.value)}
                      placeholder="https://..."
                      className="w-full bg-brand-dark border border-zinc-800 rounded-xl py-2 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-orange/40" />
                    <div
                      draggable={false}
                      className="w-full h-72 bg-neutral-900 border border-zinc-800/80 rounded-xl overflow-hidden flex items-center justify-center select-none"
                    >
                      {posterUrl?.trim() ? (
                        <LazyImage
                          src={posterUrl}
                          alt="Poster preview"
                          containerClassName="w-full h-full border-none rounded-none"
                          className="object-contain"
                        />
                      ) : (
                        <div className="text-zinc-650 flex flex-col items-center gap-1"><ImageIcon className="w-7 h-7" /><span className="text-[10px]">Chưa có Poster</span></div>
                      )}
                    </div>
                  </div>
                </div>
              </div>

              {/* Banners & Backdrops Config (Clean and scalable) */}
              <div className="lg:col-span-2 space-y-6">
                <div className="bg-brand-gray/60 border border-zinc-800/50 rounded-2xl p-5 space-y-5">
                  <div className="flex justify-between items-center border-b border-zinc-800 pb-2">
                    <h3 className="text-xs font-black text-white uppercase tracking-wider flex items-center gap-1.5">
                      <ImageIcon className="w-4.5 h-4.5 text-brand-orange" />
                      <span>Backdrop & Banner phim</span>
                    </h3>
                    <button type="button" onClick={addBanner} className="text-[10px] font-black text-brand-orange hover:opacity-80 flex items-center gap-0.5">
                      <Plus className="w-3.5 h-3.5" />Thêm Banner
                    </button>
                  </div>

                  {/* Backdrop Import from TMDB (if applicable) */}
                  {availableBackdrops.length > 0 && (
                    <div className="bg-zinc-900 border border-zinc-850 p-4 rounded-xl space-y-2">
                      <label className="text-zinc-400 text-[10px] font-black uppercase tracking-wider block">
                        Số lượng Banner/Backdrop import nhanh (TMDB: {availableBackdrops.length})
                      </label>
                      <input
                        type="number"
                        min="0"
                        max={availableBackdrops.length}
                        value={backdropImportCount}
                        onChange={e => {
                          const count = Math.max(0, Math.min(availableBackdrops.length, Number(e.target.value)));
                          setBackdropImportCount(count);
                          setBannerUrls(availableBackdrops.slice(0, count).map(b => b.url || ''));
                        }}
                        className="w-24 bg-brand-dark border border-zinc-800 rounded-lg py-1 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-orange/40 outline-none"
                      />
                    </div>
                  )}

                  {/* Banner List Inputs & Image Previews — with Drag-and-Drop reordering */}
                  <div className="space-y-3">
                    {bannerUrls.map((b, bIdx) => (
                      <div
                        key={bIdx}
                        draggable
                        onDragStart={() => handleDragStart(bIdx, 'banners')}
                        onDragOver={handleDragOver}
                        onDragEnter={() => handleDragEnter(bIdx, 'banners', bannerUrls, setBannerUrls)}
                        onDragEnd={handleDragEnd}
                        className={`flex gap-3 bg-zinc-900 border rounded-xl p-3 transition-all duration-200 ${
                          draggedType === 'banners' && draggedIdx === bIdx
                            ? 'opacity-40 scale-[0.98] border-brand-orange/40 bg-neutral-950'
                            : 'border-zinc-800/80'
                        }`}
                      >
                        {/* Drag handle */}
                        <div className="flex flex-col items-center justify-center gap-1 cursor-grab text-zinc-600 hover:text-zinc-400 pt-1 shrink-0">
                          <GripVertical className="w-4 h-4" />
                          {bIdx === 0 && (
                            <span className="text-[8px] font-black text-brand-orange uppercase tracking-wider">Chính</span>
                          )}
                        </div>

                        {/* Preview thumbnail */}
                        <div className="w-28 h-16 bg-zinc-950 border border-zinc-800 rounded-lg overflow-hidden flex-shrink-0 flex items-center justify-center">
                          {b?.trim() ? (
                            <LazyImage
                              src={b}
                              alt={`Banner ${bIdx + 1}`}
                              containerClassName="w-full h-full border-none rounded-none"
                            />
                          ) : (
                            <div className="text-zinc-700 flex flex-col items-center gap-0.5">
                              <ImageIcon className="w-4 h-4" />
                              <span className="text-[9px]">Banner {bIdx + 1}</span>
                            </div>
                          )}
                        </div>

                        {/* URL input + remove */}
                        <div className="flex-1 min-w-0 flex flex-col justify-center gap-1.5">
                          <div className="flex items-center gap-1 text-[9px] font-black text-zinc-500 uppercase tracking-wider">
                            <span>Banner #{bIdx + 1}</span>
                            {bIdx === 0 && <span className="text-brand-orange">(Banner chính — hiển thị trước)</span>}
                          </div>
                          <div className="flex gap-2">
                            <input
                              type="text"
                              value={b}
                              onChange={e => updateBanner(bIdx, e.target.value)}
                              placeholder="URL hình ảnh Banner (https://...)"
                              className="flex-1 bg-brand-dark border border-zinc-800 rounded-lg py-1.5 px-2.5 text-xs text-zinc-100 focus:outline-none focus:border-brand-orange/30 min-w-0 font-mono"
                            />
                            {bannerUrls.length > 1 && (
                              <button type="button" onClick={() => removeBanner(bIdx)} className="p-1.5 text-zinc-600 hover:text-red-400 transition-colors shrink-0">
                                <X className="w-3.5 h-3.5" />
                              </button>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Trailer Section */}
                <div className="bg-brand-gray/60 border border-zinc-800/50 rounded-2xl p-5 space-y-5">
                  <h3 className="text-xs font-black text-white uppercase tracking-wider border-b border-zinc-800 pb-2 flex items-center gap-1.5">
                    <Play className="w-4.5 h-4.5 text-brand-orange" />
                    <span>Trailer chính thức</span>
                  </h3>
                  <div className="space-y-2">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">YouTube Trailer URL</label>
                    <input type="text" value={trailerUrl} onChange={e => setTrailerUrl(e.target.value)}
                      placeholder="https://youtube.com/watch?v=..."
                      className="w-full bg-brand-dark border border-zinc-800 rounded-xl py-2 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-orange/40" />
                    {getYoutubeEmbedUrl(trailerUrl) ? (
                      <div className="w-full aspect-video rounded-xl overflow-hidden border border-zinc-800 relative bg-black group/trailer">
                        {playTrailer ? (
                          <iframe src={`${getYoutubeEmbedUrl(trailerUrl)}?autoplay=1`} title="Trailer" className="w-full h-full border-none" allow="autoplay; encrypted-media" allowFullScreen />
                        ) : (
                          <div 
                            onClick={() => setPlayTrailer(true)}
                            className="w-full h-full cursor-pointer relative flex items-center justify-center select-none group/play"
                          >
                            <LazyImage
                              src={`https://img.youtube.com/vi/${getYoutubeId(trailerUrl)}/hqdefault.jpg`} 
                              alt="Trailer preview" 
                              containerClassName="w-full h-full border-none rounded-none"
                              className="opacity-70 group-hover/play:opacity-90 transition-opacity duration-300"
                              onError={(e) => {
                                e.target.src = "https://img.youtube.com/vi/" + getYoutubeId(trailerUrl) + "/0.jpg";
                              }}
                            />
                            <div className="absolute inset-0 bg-black/20 group-hover/play:bg-black/10 transition-colors duration-300" />
                            <div className="absolute w-14 h-14 rounded-full bg-brand-orange/90 group-hover/play:bg-brand-orange text-zinc-950 flex items-center justify-center shadow-2xl transition-all duration-300 group-hover/play:scale-110">
                              <Play className="w-6 h-6 fill-current ml-1" />
                            </div>
                          </div>
                        )}
                      </div>
                    ) : (
                      <div className="w-full aspect-video bg-neutral-900 border border-zinc-800/80 rounded-xl flex flex-col items-center justify-center text-zinc-650 gap-1.5">
                        <Play className="w-7 h-7 animate-pulse" /><span className="text-[10px]">Chưa cấu hình Trailer hoặc đường dẫn không hợp lệ</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          )}

        </form>
      </div>
    );
  }

  // ═══════════════════════════════════════════════════════════════════════════════
  // RENDER: Movie List
  // ═══════════════════════════════════════════════════════════════════════════════
  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto bg-zinc-950 text-white space-y-6 animate-fade-in" data-testid="admin-movie-page">
      <div className="border-b border-zinc-800 pb-4">
        <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider">DANH SÁCH BỘ PHIM</h1>
      </div>

      {/* Filters */}
      <div className="flex flex-col lg:flex-row gap-4 justify-between items-center bg-brand-gray/60 border border-zinc-800/50 p-4 rounded-2xl">
        <div className="relative w-full lg:w-80">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500 pointer-events-none" />
          <input type="text" value={searchTerm} onChange={e => { setSearchTerm(e.target.value); setCurrentPage(0); }}
            placeholder="Tìm kiếm tên phim..."
            className="w-full bg-brand-dark border border-zinc-800 text-zinc-100 placeholder-zinc-500 focus:border-brand-orange/40 rounded-xl py-2.5 pl-9 pr-4 text-xs outline-none transition-colors" />
        </div>
        <div className="flex flex-col sm:flex-row items-center gap-3 w-full lg:w-auto">
          <select value={statusFilter} onChange={e => { setStatusFilter(e.target.value); setCurrentPage(0); }}
            className="w-full sm:w-48 bg-brand-dark border border-zinc-800 text-zinc-100 focus:border-brand-orange/40 rounded-xl py-2.5 px-4 text-xs outline-none cursor-pointer">
            <option value="">Tất cả trạng thái</option>
            {Object.entries(STATUS_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
          <select value={pageSize} onChange={e => { setPageSize(Number(e.target.value)); setCurrentPage(0); }}
            className="w-full sm:w-32 bg-brand-dark border border-zinc-800 text-zinc-100 focus:border-brand-orange/40 rounded-xl py-2.5 px-4 text-xs outline-none cursor-pointer">
            {[5, 10, 20, 50].map(n => <option key={n} value={n}>{n}/trang</option>)}
          </select>
          <button type="button" onClick={handleOpenAdd}
            className="bg-brand-orange hover:opacity-90 text-zinc-950 font-black px-5 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-all shadow-lg flex items-center gap-2 cursor-pointer w-full sm:w-auto justify-center">
            <Plus className="w-4 h-4" />THÊM PHIM
          </button>
        </div>
      </div>

      {/* Table */}
      {isLoading ? (
        <SkeletonTable rows={pageSize} columns={7} />
      ) : (
        <div className="bg-neutral-950 border border-neutral-800 rounded-2xl overflow-hidden shadow-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse whitespace-nowrap">
              <thead>
                <tr className="bg-neutral-900/50 border-b border-neutral-800 text-[10px] font-black text-neutral-400 uppercase tracking-wider">
                  <th className="py-4 px-5 w-12 text-center">STT</th>
                  <th className="py-4 px-5 w-16 text-center">POSTER</th>
                  <th className="py-4 px-5">TÊN PHIM</th>
                  <th className="py-4 px-5 w-32 text-center">THỜI LƯỢNG</th>
                  <th className="py-4 px-5 w-36 text-center">KHỞI CHIẾU</th>
                  <th className="py-4 px-5 w-32 text-center">TRẠNG THÁI</th>
                  <th className="py-4 px-5 w-28 text-right">THAO TÁC</th>
                </tr>
              </thead>
              <tbody>
                {movies.length === 0
                  ? (
                    <tr>
                      <td colSpan={7} className="py-16 text-center text-neutral-500">
                        <div className="flex flex-col items-center gap-2">
                          <LayoutList className="w-10 h-10 text-neutral-700" />
                          <span className="text-sm">Không tìm thấy phim nào.</span>
                        </div>
                      </td>
                    </tr>
                  )
                  : movies.map((movie, idx) => (
                    <tr key={movie.publicId || idx} className="border-b border-neutral-800/50 hover:bg-neutral-900/50 transition-colors">
                      <td className="py-4 px-5 text-center">
                        <span className="text-xs font-black text-neutral-500">{(currentPage * pageSize + idx + 1).toString().padStart(2, '0')}</span>
                      </td>
                      <td className="py-4 px-5">
                        <div className="w-9 h-13 bg-neutral-800 rounded overflow-hidden mx-auto">
                          {movie.primaryPoster ? (
                            <LazyImage
                              src={movie.primaryPoster}
                              alt={movie.title}
                              containerClassName="w-full h-full border-none rounded-none bg-transparent"
                              className="object-cover"
                              onError={e => { e.target.style.display = 'none'; }}
                            />
                          ) : (
                            <div className="w-full h-full flex items-center justify-center text-neutral-700"><ImageIcon className="w-4 h-4" /></div>
                          )}
                        </div>
                      </td>
                      <td className="py-4 px-5">
                        <button type="button" onClick={() => handleOpenDetail(movie)}
                          className="text-sm font-bold text-zinc-200 hover:text-amber-400 transition-colors text-left truncate max-w-[220px] block cursor-pointer">
                          {movie.title}
                        </button>
                        {movie.ageRating && <span className="text-[10px] font-bold text-neutral-500 uppercase">{movie.ageRating}</span>}
                      </td>
                      <td className="py-4 px-5 text-center">
                        <span className="text-xs text-zinc-300">{movie.durationMinutes ? `${movie.durationMinutes} phút` : 'N/A'}</span>
                      </td>
                      <td className="py-4 px-5 text-center">
                        <span className="text-xs text-zinc-300">{formatDate(movie.releaseDate)}</span>
                      </td>
                      <td className="py-4 px-5 text-center">
                        {movie.status && (
                          <span className={`text-[10px] font-black px-2.5 py-1 rounded-md border uppercase tracking-wider ${STATUS_COLORS[movie.status] || 'text-zinc-400 border-zinc-700'}`}>
                            {STATUS_LABELS[movie.status] || movie.status}
                          </span>
                        )}
                      </td>
                      <td className="py-4 px-5 text-right">
                        <div className="flex justify-end gap-1.5">
                          <button type="button" onClick={() => handleOpenEdit(movie)}
                            className="p-2 text-neutral-400 hover:text-amber-500 hover:bg-amber-500/10 rounded-lg transition-all cursor-pointer" title="Sửa">
                            <Pencil className="w-4 h-4" />
                          </button>
                          <button type="button"
                            disabled={movie.status !== 'ENDED' && movie.status !== 'DRAFT'}
                            onClick={() => handleDelete(movie.publicId, movie.title)}
                            className="p-2 text-neutral-400 hover:text-red-500 hover:bg-red-500/10 disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-neutral-400 disabled:cursor-not-allowed rounded-lg transition-all cursor-pointer"
                            title={movie.status === 'ENDED' || movie.status === 'DRAFT' ? "Xóa" : "Chỉ có thể xóa phim ở trạng thái Nháp hoặc Ngừng chiếu"}
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                }
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalElements > 0 && (
            <div className="flex flex-col sm:flex-row items-center justify-between p-4 border-t border-neutral-800 bg-neutral-900/30 gap-4">
              <span className="text-xs text-neutral-400">
                Hiển thị {currentPage * pageSize + 1}–{Math.min((currentPage + 1) * pageSize, totalElements)} / {totalElements} phim
              </span>
              <div className="flex items-center gap-1">
                <button disabled={currentPage === 0} onClick={() => setCurrentPage(p => p - 1)}
                  className="px-3 py-1.5 text-xs font-semibold rounded-lg border border-neutral-800 bg-neutral-900 text-neutral-400 hover:text-white hover:bg-neutral-800 disabled:opacity-40 disabled:cursor-not-allowed transition-colors">
                  Trước
                </button>
                <div className="flex items-center gap-1 px-1">
                  {Array.from({ length: Math.min(totalPages, 7) }, (_, i) => {
                    let page;
                    if (totalPages <= 7) page = i;
                    else if (currentPage < 4) page = i;
                    else if (currentPage > totalPages - 5) page = totalPages - 7 + i;
                    else page = currentPage - 3 + i;
                    return (
                      <button key={page} onClick={() => setCurrentPage(page)}
                        className={`w-7 h-7 flex items-center justify-center text-xs font-bold rounded-lg transition-colors ${currentPage === page ? 'bg-amber-500 text-black' : 'text-neutral-400 hover:bg-neutral-800 hover:text-white'
                          }`}>
                        {page + 1}
                      </button>
                    );
                  })}
                </div>
                <button disabled={currentPage >= totalPages - 1 || totalPages === 0} onClick={() => setCurrentPage(p => p + 1)}
                  className="px-3 py-1.5 text-xs font-semibold rounded-lg border border-neutral-800 bg-neutral-900 text-neutral-400 hover:text-white hover:bg-neutral-800 disabled:opacity-40 disabled:cursor-not-allowed transition-colors">
                  Sau
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

// ─── FormSection helper component ─────────────────────────────────────────────
function FormSection({ icon, title, children, headerAction }) {
  return (
    <div className="bg-brand-gray/60 border border-zinc-800/50 rounded-2xl p-5 space-y-4">
      <div className="flex justify-between items-center border-b border-zinc-800 pb-2">
        <h3 className="text-xs font-black text-white uppercase tracking-wider flex items-center gap-2">
          {icon}<span>{title}</span>
        </h3>
        {headerAction}
      </div>
      {children}
    </div>
  );
}
