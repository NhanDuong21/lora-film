import { useEffect, useMemo, useState } from 'react';
import {
  AlertCircle,
  ArrowRight,
  ChevronRight,
  Clapperboard,
  RefreshCw,
  Search,
  UserRound,
  Video,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import PersonPortrait from '@/features/catalog/customer/components/PersonPortrait';
import { getPeople } from '@/features/catalog/customer/services/peopleService';

const AVAILABILITY_OPTIONS = [
  { value: 'ALL', label: 'Tất cả' },
  { value: 'NOW_SHOWING', label: 'Đang chiếu' },
  { value: 'UPCOMING', label: 'Sắp chiếu' },
];

const SORT_OPTIONS = [
  { value: 'POPULAR', label: 'Phổ biến' },
  { value: 'NAME_ASC', label: 'Tên A–Z' },
  { value: 'NEW', label: 'Mới cập nhật' },
];

const roleCopy = {
  ACTOR: {
    eyebrow: 'Gương mặt trên màn ảnh',
    title: 'Diễn viên',
    description: 'Khám phá những diễn viên góp mặt trong các bộ phim thuộc danh mục LoraFilm.',
    Icon: UserRound,
  },
  DIRECTOR: {
    eyebrow: 'Người kể chuyện bằng hình ảnh',
    title: 'Đạo diễn',
    description: 'Gặp gỡ các đạo diễn đứng sau những tác phẩm đang có mặt tại LoraFilm.',
    Icon: Video,
  },
};

function DirectorySkeleton() {
  return (
    <div aria-label="Đang tải danh sách nghệ sĩ" className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
      {Array.from({ length: 10 }).map((_, index) => (
        <div key={index} className="overflow-hidden rounded-2xl border border-white/10 bg-zinc-900/70">
          <div className="aspect-[2/3] animate-pulse bg-zinc-800" />
          <div className="space-y-3 p-4">
            <div className="h-4 w-2/3 animate-pulse rounded bg-zinc-800" />
            <div className="h-3 w-full animate-pulse rounded bg-zinc-800" />
          </div>
        </div>
      ))}
    </div>
  );
}

function PersonCard({ person }) {
  return (
    <Link
      to={`/nghe-si/${encodeURIComponent(person.slug || person.id)}`}
      className="group relative overflow-hidden rounded-2xl border border-white/10 bg-zinc-900/80 shadow-xl shadow-black/15 transition duration-300 hover:-translate-y-1 hover:border-brand-orange/55 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange"
    >
      <PersonPortrait person={person} className="aspect-[2/3] w-full" />
      <div className="absolute inset-0 hidden bg-gradient-to-t from-black via-black/15 to-transparent opacity-0 transition-opacity duration-300 group-hover:opacity-100 sm:block" />
      <div className="absolute inset-x-3 bottom-28 hidden translate-y-2 items-center justify-center gap-2 rounded-xl border border-white/15 bg-black/75 px-3 py-2.5 text-xs font-black text-white opacity-0 backdrop-blur transition duration-300 group-hover:translate-y-0 group-hover:opacity-100 sm:flex">
        Xem hồ sơ <ArrowRight aria-hidden="true" size={14} />
      </div>
      <div className="relative min-h-28 p-4">
        <p className="text-[10px] font-black uppercase tracking-[0.18em] text-brand-orange">
          {person.roles?.join(' · ') || 'Nghệ sĩ'}
        </p>
        <h2 className="mt-1.5 line-clamp-2 text-base font-black leading-snug text-white transition-colors group-hover:text-orange-300">
          {person.name}
        </h2>
        {person.knownFor?.length > 0 && (
          <p className="mt-2 line-clamp-2 text-xs leading-5 text-zinc-500">
            {person.knownFor.join(' · ')}
          </p>
        )}
      </div>
    </Link>
  );
}

export default function PeopleDirectoryPage({ role }) {
  const copy = roleCopy[role] || roleCopy.ACTOR;
  const [searchInput, setSearchInput] = useState('');
  const [query, setQuery] = useState('');
  const [availability, setAvailability] = useState('ALL');
  const [sort, setSort] = useState('POPULAR');
  const [page, setPage] = useState(0);
  const [people, setPeople] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [isLast, setIsLast] = useState(true);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [requestVersion, setRequestVersion] = useState(0);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setQuery(searchInput.trim());
      setPage(0);
    }, 300);
    return () => window.clearTimeout(timer);
  }, [searchInput]);

  useEffect(() => {
    const controller = new AbortController();
    // The request key changed, so the previous result must immediately enter its loading state.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true);
    setError('');
    getPeople({ role, query, availability, sort, page, size: 20, signal: controller.signal })
      .then(result => {
        const nextPeople = result?.content || [];
        setPeople(current => page === 0 ? nextPeople : [...current, ...nextPeople]);
        setTotalElements(result?.totalElements || 0);
        setIsLast(result?.isLast ?? true);
      })
      .catch(requestError => {
        if (requestError?.code !== 'ERR_CANCELED') {
          setError('Danh sách nghệ sĩ hiện chưa tải được. Vui lòng thử lại.');
          if (page === 0) setPeople([]);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [availability, page, query, requestVersion, role, sort]);

  const resultLabel = useMemo(
    () => totalElements === 1 ? '1 nghệ sĩ' : `${totalElements.toLocaleString('vi-VN')} nghệ sĩ`,
    [totalElements],
  );

  const resetFilters = () => {
    setSearchInput('');
    setQuery('');
    setAvailability('ALL');
    setSort('POPULAR');
    setPage(0);
  };

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100">
      <section className="relative min-h-[250px] overflow-hidden border-b border-white/10 bg-zinc-900">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_80%_20%,rgba(255,122,0,0.2),transparent_34%),linear-gradient(120deg,#09090b_0%,#18181b_58%,#1f1308_100%)]" />
        <Clapperboard aria-hidden="true" className="absolute -bottom-20 right-[8%] h-72 w-72 -rotate-12 text-white/[0.025]" />
        <div className="relative mx-auto flex min-h-[250px] max-w-7xl flex-col justify-center px-5 py-10 sm:px-8">
          <nav aria-label="Đường dẫn" className="flex items-center gap-2 text-xs font-bold text-zinc-500">
            <Link to="/" className="hover:text-brand-orange">Trang chủ</Link>
            <ChevronRight aria-hidden="true" size={14} />
            <span className="text-zinc-300">{copy.title}</span>
          </nav>
          <p className="mt-7 text-xs font-black uppercase tracking-[0.25em] text-brand-orange">{copy.eyebrow}</p>
          <div className="mt-2 flex items-center gap-3">
            <copy.Icon aria-hidden="true" className="h-8 w-8 text-brand-orange" />
            <h1 className="text-4xl font-black text-white sm:text-5xl">{copy.title}</h1>
          </div>
          <p className="mt-3 max-w-2xl text-sm leading-6 text-zinc-400 sm:text-base">{copy.description}</p>
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-5 py-9 sm:px-8">
        <div className="rounded-2xl border border-white/10 bg-zinc-900/80 p-4 shadow-2xl shadow-black/20">
          <div className="grid gap-3 lg:grid-cols-[minmax(260px,1fr)_auto_auto_auto] lg:items-end">
            <label className="block">
              <span className="mb-2 block text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Tìm nghệ sĩ</span>
              <span className="relative block">
                <Search aria-hidden="true" className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-500" />
                <input
                  type="search"
                  value={searchInput}
                  onChange={event => setSearchInput(event.target.value)}
                  placeholder={`Tìm ${copy.title.toLowerCase()}...`}
                  className="h-12 w-full rounded-xl border border-zinc-700 bg-zinc-950 pl-11 pr-4 text-sm text-white outline-none placeholder:text-zinc-600 focus:border-brand-orange focus:ring-2 focus:ring-brand-orange/15"
                />
              </span>
            </label>
            <label>
              <span className="mb-2 block text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Tình trạng phim</span>
              <select
                value={availability}
                onChange={event => { setAvailability(event.target.value); setPage(0); }}
                className="h-12 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 text-sm font-bold text-zinc-200 outline-none focus:border-brand-orange lg:w-44"
              >
                {AVAILABILITY_OPTIONS.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
              </select>
            </label>
            <label>
              <span className="mb-2 block text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Sắp xếp</span>
              <select
                value={sort}
                onChange={event => { setSort(event.target.value); setPage(0); }}
                className="h-12 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 text-sm font-bold text-zinc-200 outline-none focus:border-brand-orange lg:w-44"
              >
                {SORT_OPTIONS.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
              </select>
            </label>
            <button
              type="button"
              onClick={resetFilters}
              className="flex h-12 items-center justify-center gap-2 rounded-xl border border-zinc-700 px-4 text-sm font-bold text-zinc-400 transition hover:border-zinc-500 hover:text-white"
            >
              <RefreshCw aria-hidden="true" size={15} /> Đặt lại
            </button>
          </div>
        </div>

        <div className="mb-5 mt-8 flex items-center justify-between gap-4">
          <p className="text-sm font-bold text-zinc-400">{loading && page === 0 ? 'Đang tìm nghệ sĩ...' : resultLabel}</p>
          {query && <p className="truncate text-xs text-zinc-500">Kết quả cho “{query}”</p>}
        </div>

        {loading && page === 0 ? (
          <DirectorySkeleton />
        ) : error && people.length === 0 ? (
          <div className="rounded-3xl border border-red-500/20 bg-red-950/10 px-6 py-16 text-center">
            <AlertCircle className="mx-auto h-10 w-10 text-red-400" />
            <h2 className="mt-4 text-lg font-black text-white">Không thể tải danh sách</h2>
            <p className="mt-2 text-sm text-zinc-400">{error}</p>
            <button type="button" onClick={() => setRequestVersion(value => value + 1)} className="mt-5 rounded-xl bg-brand-orange px-5 py-2.5 text-sm font-black text-white">Thử lại</button>
          </div>
        ) : people.length === 0 ? (
          <div className="rounded-3xl border border-white/10 bg-zinc-900/50 px-6 py-16 text-center">
            <copy.Icon className="mx-auto h-11 w-11 text-zinc-600" />
            <h2 className="mt-4 text-lg font-black text-white">Chưa tìm thấy nghệ sĩ phù hợp</h2>
            <p className="mt-2 text-sm text-zinc-500">Hãy thử tên khác hoặc thay đổi tình trạng phim.</p>
            <button type="button" onClick={resetFilters} className="mt-5 text-sm font-black text-brand-orange hover:text-orange-300">Xóa bộ lọc</button>
          </div>
        ) : (
          <>
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
              {people.map(person => <PersonCard key={person.id} person={person} />)}
            </div>
            {!isLast && (
              <div className="mt-9 text-center">
                <button
                  type="button"
                  disabled={loading}
                  onClick={() => setPage(value => value + 1)}
                  className="inline-flex min-h-12 items-center gap-2 rounded-full border border-brand-orange/60 px-7 text-sm font-black text-brand-orange transition hover:bg-brand-orange hover:text-white disabled:cursor-wait disabled:opacity-50"
                >
                  {loading ? 'Đang tải...' : 'Xem thêm nghệ sĩ'} <ChevronRight aria-hidden="true" size={16} />
                </button>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
}
