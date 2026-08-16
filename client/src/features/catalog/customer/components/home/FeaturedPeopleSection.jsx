import { useCallback, useEffect, useMemo, useState } from 'react';
import { ArrowRight, Sparkles } from 'lucide-react';
import { Link } from 'react-router-dom';
import PersonPortrait from '@/features/catalog/customer/components/PersonPortrait';
import { getPeople } from '@/features/catalog/customer/services/peopleService';

const interleave = (actors, directors) => {
  const result = [];
  const length = Math.max(actors.length, directors.length);
  for (let index = 0; index < length; index += 1) {
    if (actors[index]) result.push(actors[index]);
    if (directors[index]) result.push(directors[index]);
  }
  return result.slice(0, 6);
};

export default function FeaturedPeopleSection() {
  const [people, setPeople] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [actors, directors] = await Promise.all([
        getPeople({ role: 'ACTOR', availability: 'NOW_SHOWING', sort: 'POPULAR', page: 0, size: 3 }),
        getPeople({ role: 'DIRECTOR', availability: 'NOW_SHOWING', sort: 'POPULAR', page: 0, size: 3 }),
      ]);
      setPeople(interleave(actors?.content || [], directors?.content || []));
    } catch {
      setPeople([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const cards = useMemo(() => people.slice(0, 6), [people]);
  if (!loading && cards.length === 0) return null;

  return (
    <section className="w-full bg-zinc-950 py-16 text-zinc-100">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <header className="flex flex-wrap items-end justify-between gap-4 border-b border-zinc-800 pb-5">
          <div>
            <div className="flex items-center gap-2 text-[11px] font-black uppercase tracking-[0.22em] text-brand-orange">
              <Sparkles aria-hidden="true" className="h-4 w-4" /> Nhân vật điện ảnh
            </div>
            <h2 className="mt-2 text-xl font-black uppercase text-white md:text-2xl">Gương mặt nổi bật tại LoraFilm</h2>
            <p className="mt-2 max-w-2xl text-sm text-zinc-500">Diễn viên và đạo diễn trong những phim đang mở bán vé.</p>
          </div>
          <Link to="/dien-vien" className="flex items-center gap-2 text-sm font-bold text-zinc-400 transition hover:text-brand-orange">
            Khám phá nghệ sĩ <ArrowRight aria-hidden="true" className="h-4 w-4" />
          </Link>
        </header>

        {loading ? (
          <div aria-label="Đang tải nghệ sĩ nổi bật" className="mt-8 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
            {Array.from({ length: 6 }).map((_, index) => <div key={index} className="aspect-[2/3] animate-pulse rounded-2xl bg-zinc-900" />)}
          </div>
        ) : (
          <div className="mt-8 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
            {cards.map(person => (
              <Link
                key={person.id}
                to={`/nghe-si/${encodeURIComponent(person.slug || person.id)}`}
                className="group overflow-hidden rounded-2xl border border-white/10 bg-zinc-900 transition hover:-translate-y-1 hover:border-brand-orange/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange"
              >
                <PersonPortrait person={person} className="aspect-[2/3] w-full" />
                <div className="p-3.5">
                  <p className="text-[9px] font-black uppercase tracking-wider text-brand-orange">{person.roles?.[0]}</p>
                  <h3 className="mt-1 line-clamp-2 text-sm font-black leading-5 text-white group-hover:text-orange-300">{person.name}</h3>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
