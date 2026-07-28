import { TrendingUp } from 'lucide-react';

export default function AdminStatCard({
  title,
  value,
  icon: Icon,
  colorClass,
  description,
  subtitle,
  valueClassName = 'text-white',
  showTrend = false
}) {
  return (
    <section className="enterprise-card flex min-h-32 items-center justify-between rounded-2xl border border-zinc-800 bg-zinc-900/50 p-5 transition-colors hover:bg-zinc-900">
      <div className="min-w-0">
        <p className="mb-1 text-xs font-bold uppercase tracking-wider text-zinc-500">{title}</p>
        <p className={`text-3xl font-black ${valueClassName}`}>{value}</p>
        {(description || subtitle) && (
          <p className="mt-2 text-[10px] uppercase leading-relaxed text-zinc-500">
            {description || subtitle}
          </p>
        )}
      </div>
      <div className="flex shrink-0 flex-col items-end gap-3">
        {showTrend && <TrendingUp className="h-4 w-4 text-zinc-600" aria-hidden="true" />}
        <div className={`flex h-12 w-12 items-center justify-center rounded-xl border ${colorClass}`}>
          <Icon className="h-6 w-6" aria-hidden="true" />
        </div>
      </div>
    </section>
  );
}
