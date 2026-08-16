import { Film, UserRound } from 'lucide-react';

const tmdbPortraitUrl = (imageUrl, size = 'h632') => {
  if (!imageUrl || typeof imageUrl !== 'string') return '';
  try {
    const parsed = new URL(imageUrl);
    if (parsed.hostname !== 'image.tmdb.org') return imageUrl;
    parsed.pathname = parsed.pathname.replace(/\/t\/p\/(?:original|[wh]\d+)\//, `/t/p/${size}/`);
    return parsed.toString();
  } catch {
    return imageUrl;
  }
};

export default function PersonPortrait({ person, className = '', eager = false }) {
  const initial = (person?.name || person?.originalName || '?').trim().charAt(0).toUpperCase();
  return (
    <div
      className={`relative isolate grid overflow-hidden bg-gradient-to-br from-orange-950 via-zinc-900 to-black ${className}`}
    >
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_24%,rgba(255,122,0,0.24),transparent_42%)]" />
      <Film aria-hidden="true" className="absolute -bottom-6 -right-6 h-28 w-28 rotate-12 text-white/[0.04]" />
      <div className="relative m-auto grid h-20 w-20 place-items-center rounded-full border border-brand-orange/30 bg-black/35 text-4xl font-black text-brand-orange shadow-2xl shadow-orange-950/30">
        {initial || <UserRound aria-hidden="true" />}
      </div>
      {person?.profileImageUrl && (
        <img
          src={tmdbPortraitUrl(person.profileImageUrl)}
          srcSet={`${tmdbPortraitUrl(person.profileImageUrl, 'h632')} 1x, ${tmdbPortraitUrl(person.profileImageUrl, 'original')} 2x`}
          alt={`Ảnh chân dung ${person.name}`}
          loading={eager ? 'eager' : 'lazy'}
          fetchPriority={eager ? 'high' : 'auto'}
          decoding="async"
          className="absolute inset-0 h-full w-full object-cover"
          onError={event => event.currentTarget.remove()}
        />
      )}
      <div className="pointer-events-none absolute inset-x-0 bottom-0 h-1/3 bg-gradient-to-t from-black/60 to-transparent" />
    </div>
  );
}
