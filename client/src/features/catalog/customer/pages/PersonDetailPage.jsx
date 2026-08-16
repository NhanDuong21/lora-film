import { useEffect, useState } from 'react';
import {
  AlertCircle,
  ArrowLeft,
  ArrowRight,
  CalendarDays,
  Clapperboard,
  MapPin,
  RefreshCw,
  Ticket,
} from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import PersonPortrait from '@/features/catalog/customer/components/PersonPortrait';
import { getPerson } from '@/features/catalog/customer/services/peopleService';

const formatDate = value => {
  if (!value) return null;
  const date = new Date(`${value}T00:00:00`);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(date);
};

function PosterFallback({ title }) {
  return (
    <div className="absolute inset-0 grid place-items-center bg-gradient-to-br from-orange-950 via-zinc-900 to-black p-4 text-center">
      <Clapperboard aria-hidden="true" className="h-9 w-9 text-brand-orange/70" />
      <span className="mt-2 line-clamp-3 text-xs font-black text-zinc-300">{title}</span>
    </div>
  );
}

function MovieCreditCard({ movie, primaryAction }) {
  const identifier = movie.slug || movie.id;
  const detailPath = `/movies/${encodeURIComponent(identifier)}`;
  const actionPath = primaryAction === 'showtimes' ? `${detailPath}#showtimes` : detailPath;
  return (
    <article className="group overflow-hidden rounded-2xl border border-white/10 bg-zinc-900/80 transition hover:border-brand-orange/45">
      <Link to={detailPath} className="relative block aspect-[2/3] overflow-hidden bg-zinc-950 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand-orange">
        <PosterFallback title={movie.title} />
        {movie.posterUrl && (
          <img
            src={movie.posterUrl}
            alt={`Poster ${movie.title}`}
            loading="lazy"
            className="absolute inset-0 h-full w-full object-cover transition duration-500 group-hover:scale-105"
            onError={event => event.currentTarget.remove()}
          />
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent" />
        {movie.characterName && (
          <p className="absolute inset-x-3 bottom-3 line-clamp-2 text-xs font-bold text-zinc-200">
            Vai {movie.characterName}
          </p>
        )}
      </Link>
      <div className="p-4">
        <p className="text-[10px] font-black uppercase tracking-wider text-brand-orange">{movie.role}</p>
        <Link to={detailPath} className="mt-1.5 block line-clamp-2 min-h-11 text-sm font-black leading-5 text-white hover:text-orange-300">
          {movie.title}
        </Link>
        {movie.releaseDate && <p className="mt-1 text-xs text-zinc-500">{formatDate(movie.releaseDate)}</p>}
        <Link
          to={actionPath}
          className="mt-4 flex min-h-10 items-center justify-center gap-2 rounded-xl border border-white/10 bg-zinc-950 px-3 text-xs font-black text-zinc-200 transition hover:border-brand-orange hover:text-brand-orange focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange"
        >
          {primaryAction === 'showtimes' ? <Ticket aria-hidden="true" size={14} /> : <ArrowRight aria-hidden="true" size={14} />}
          {primaryAction === 'showtimes' ? 'Xem suất chiếu' : 'Xem chi tiết'}
        </Link>
      </div>
    </article>
  );
}

function MovieSection({ id, eyebrow, title, description, movies, primaryAction }) {
  if (!movies?.length) return null;
  return (
    <section id={id} className="scroll-mt-24 border-t border-white/10 py-10 first:border-t-0">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs font-black uppercase tracking-[0.22em] text-brand-orange">{eyebrow}</p>
          <h2 className="mt-2 text-2xl font-black text-white sm:text-3xl">{title}</h2>
          {description && <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-500">{description}</p>}
        </div>
        <span className="rounded-full border border-white/10 bg-zinc-900 px-3 py-1.5 text-xs font-bold text-zinc-400">
          {movies.length} phim
        </span>
      </div>
      <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
        {movies.map(movie => <MovieCreditCard key={movie.id} movie={movie} primaryAction={primaryAction} />)}
      </div>
    </section>
  );
}

function DetailSkeleton() {
  return (
    <div aria-label="Đang tải hồ sơ nghệ sĩ" className="mx-auto max-w-7xl animate-pulse px-5 py-10 sm:px-8">
      <div className="grid gap-8 md:grid-cols-[280px_1fr]">
        <div className="aspect-[2/3] rounded-3xl bg-zinc-800" />
        <div className="space-y-5 pt-6">
          <div className="h-5 w-28 rounded bg-zinc-800" />
          <div className="h-12 w-2/3 rounded bg-zinc-800" />
          <div className="h-4 w-1/3 rounded bg-zinc-800" />
          <div className="h-24 w-full rounded bg-zinc-800" />
        </div>
      </div>
    </div>
  );
}

export default function PersonDetailPage() {
  const { personSlug } = useParams();
  const [person, setPerson] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [requestVersion, setRequestVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    // The route changed, so the previous profile must immediately enter its loading state.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true);
    setError('');
    getPerson(personSlug, { signal: controller.signal })
      .then(setPerson)
      .catch(requestError => {
        if (requestError?.code !== 'ERR_CANCELED') {
          setError(requestError?.status === 404
            ? 'Không tìm thấy nghệ sĩ hoặc nghệ sĩ chưa có phim trong danh mục LoraFilm.'
            : 'Hồ sơ nghệ sĩ hiện chưa tải được. Vui lòng thử lại.');
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [personSlug, requestVersion]);

  if (loading) return <DetailSkeleton />;
  if (error || !person) {
    return (
      <div className="mx-auto flex min-h-[60vh] max-w-2xl flex-col items-center justify-center px-5 text-center">
        <AlertCircle className="h-12 w-12 text-red-400" />
        <h1 className="mt-5 text-2xl font-black text-white">Không thể mở hồ sơ nghệ sĩ</h1>
        <p className="mt-3 text-sm leading-6 text-zinc-400">{error}</p>
        <div className="mt-6 flex flex-wrap justify-center gap-3">
          <button type="button" onClick={() => setRequestVersion(value => value + 1)} className="flex min-h-11 items-center gap-2 rounded-xl bg-brand-orange px-5 text-sm font-black text-white">
            <RefreshCw size={15} /> Thử lại
          </button>
          <Link to="/dien-vien" className="flex min-h-11 items-center gap-2 rounded-xl border border-white/15 px-5 text-sm font-black text-zinc-200">
            <ArrowLeft size={15} /> Về danh sách
          </Link>
        </div>
      </div>
    );
  }

  const returnPath = person.roles?.includes('Đạo diễn') ? '/dao-dien' : '/dien-vien';
  const returnLabel = person.roles?.includes('Đạo diễn') ? 'Danh sách đạo diễn' : 'Danh sách diễn viên';

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100">
      <section className="relative overflow-hidden border-b border-white/10 bg-zinc-900">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_78%_18%,rgba(255,122,0,0.18),transparent_36%),linear-gradient(125deg,#09090b,#18181b_58%,#211306)]" />
        <div className="relative mx-auto max-w-7xl px-5 py-9 sm:px-8">
          <Link to={returnPath} className="inline-flex items-center gap-2 text-xs font-black text-zinc-400 hover:text-brand-orange">
            <ArrowLeft aria-hidden="true" size={15} /> {returnLabel}
          </Link>
          <div className="mt-7 grid items-start gap-8 md:grid-cols-[280px_minmax(0,1fr)] lg:gap-12">
            <PersonPortrait person={person} eager className="aspect-[2/3] w-full max-w-[280px] rounded-3xl border border-white/10 shadow-2xl shadow-black/50" />
            <div className="py-2 md:py-6">
              <p className="text-xs font-black uppercase tracking-[0.24em] text-brand-orange">
                {person.roles?.join(' · ') || 'Nghệ sĩ điện ảnh'}
              </p>
              <h1 className="mt-3 text-4xl font-black leading-tight text-white sm:text-5xl lg:text-6xl">{person.name}</h1>
              {person.originalName && <p className="mt-2 text-base font-semibold text-zinc-500">{person.originalName}</p>}

              {(person.birthDate || person.placeOfBirth) && (
                <dl className="mt-7 flex flex-wrap gap-3">
                  {person.birthDate && (
                    <div className="flex items-center gap-2 rounded-full border border-white/10 bg-black/25 px-4 py-2 text-sm text-zinc-300">
                      <CalendarDays aria-hidden="true" size={16} className="text-brand-orange" />
                      <dt className="sr-only">Ngày sinh</dt><dd>{formatDate(person.birthDate)}</dd>
                    </div>
                  )}
                  {person.placeOfBirth && (
                    <div className="flex items-center gap-2 rounded-full border border-white/10 bg-black/25 px-4 py-2 text-sm text-zinc-300">
                      <MapPin aria-hidden="true" size={16} className="text-brand-orange" />
                      <dt className="sr-only">Nơi sinh</dt><dd>{person.placeOfBirth}</dd>
                    </div>
                  )}
                </dl>
              )}

              {person.biography && (
                <div className="mt-8 max-w-3xl">
                  <h2 className="text-sm font-black uppercase tracking-[0.18em] text-white">Tiểu sử</h2>
                  <p className="mt-3 whitespace-pre-line text-sm leading-7 text-zinc-400 sm:text-base">{person.biography}</p>
                </div>
              )}
            </div>
          </div>
        </div>
      </section>

      <div className="mx-auto max-w-7xl px-5 sm:px-8">
        <MovieSection
          id="phim-dang-co"
          eyebrow="Ưu tiên đặt vé"
          title="Đang có tại LoraFilm"
          description="Các phim đang mở bán vé và có thể chọn suất chiếu ngay."
          movies={person.availableMovies}
          primaryAction="showtimes"
        />
        <MovieSection
          id="phim-sap-chieu"
          eyebrow="Sắp ra mắt"
          title="Phim sắp chiếu"
          description="Những tác phẩm sắp có mặt trên màn ảnh LoraFilm."
          movies={person.upcomingMovies}
          primaryAction="detail"
        />
        <MovieSection
          id="tac-pham-khac"
          eyebrow="Danh mục LoraFilm"
          title="Tác phẩm khác"
          movies={person.otherCredits}
          primaryAction="detail"
        />
      </div>
    </div>
  );
}
