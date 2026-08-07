import { X, ChevronLeft, ChevronRight } from 'lucide-react';

const METRIC_TONES = {
  blue: 'border-blue-500/20 bg-blue-500/10 text-blue-400',
  green: 'border-emerald-500/20 bg-emerald-500/10 text-emerald-400',
  amber: 'border-amber-500/20 bg-amber-500/10 text-amber-400',
  orange: 'border-orange-500/20 bg-orange-500/10 text-orange-400',
  purple: 'border-purple-500/20 bg-purple-500/10 text-purple-400',
  red: 'border-red-500/20 bg-red-500/10 text-red-400'
};

export function OperationsHeader({ eyebrow, title, description, actions }) {
  return (
    <header className="flex flex-col gap-5 border-b border-white/10 pb-7 xl:flex-row xl:items-end xl:justify-between">
      <div className="max-w-3xl">
        <p className="mb-2 text-[11px] font-black uppercase tracking-[0.28em] text-brand-orange">{eyebrow}</p>
        <h1 className="text-3xl font-black tracking-tight text-white md:text-4xl">{title}</h1>
        <p className="mt-3 max-w-2xl text-sm leading-6 text-zinc-400">{description}</p>
      </div>
      {actions ? <div className="flex flex-wrap gap-2">{actions}</div> : null}
    </header>
  );
}

export function MetricStrip({ items = [] }) {
  const desktopColumns = items.length === 3 ? 'xl:grid-cols-3' : 'xl:grid-cols-4';
  return (
    <div className={`grid overflow-hidden rounded-2xl border border-white/10 bg-white/[0.025] sm:grid-cols-2 ${desktopColumns}`}>
      {items.map(item => {
        const Icon = item.icon;
        return (
          <div key={item.label} className="flex min-h-28 items-center justify-between border-b border-white/10 p-5 last:border-b-0 sm:[&:nth-child(odd)]:border-r xl:border-b-0 xl:border-r xl:last:border-r-0">
            <div>
              <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">{item.label}</p>
              <p className="mt-2 text-2xl font-black text-white">{item.value}</p>
              {item.hint ? <p className="mt-1 text-xs text-zinc-500">{item.hint}</p> : null}
            </div>
            {Icon ? <span className={`rounded-xl border p-3 ${METRIC_TONES[item.tone] || item.tone || 'border-white/10 bg-white/5 text-zinc-300'}`}><Icon size={20} /></span> : null}
          </div>
        );
      })}
    </div>
  );
}

export function ConsolePanel({ children, className = '' }) {
  return <section className={`rounded-2xl border border-white/10 bg-[#0b0b0e] ${className}`}>{children}</section>;
}

export function DetailDrawer({ open, onClose, title, subtitle, children, footer }) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/70 backdrop-blur-sm" role="presentation" onMouseDown={event => event.target === event.currentTarget && onClose()}>
      <aside role="dialog" aria-modal="true" aria-label={title} className="flex h-full w-full max-w-xl flex-col border-l border-white/10 bg-[#09090b] shadow-2xl">
        <header className="flex items-start justify-between border-b border-white/10 px-6 py-5">
          <div>
            <h2 className="text-xl font-black text-white">{title}</h2>
            {subtitle ? <p className="mt-1 text-sm text-zinc-500">{subtitle}</p> : null}
          </div>
          <button type="button" aria-label="Đóng" onClick={onClose} className="rounded-lg p-2 text-zinc-500 hover:bg-white/5 hover:text-white"><X size={20} /></button>
        </header>
        <div className="flex-1 overflow-y-auto p-6">{children}</div>
        {footer ? <footer className="border-t border-white/10 p-5">{footer}</footer> : null}
      </aside>
    </div>
  );
}

export function ActionModal({ open, onClose, title, description, children, onSubmit, submitLabel, submitting = false, tone = 'orange' }) {
  if (!open) return null;
  const submitTone = tone === 'danger' ? 'bg-red-500 hover:bg-red-400 text-white' : 'bg-brand-orange hover:bg-orange-500 text-black';
  return (
    <div className="fixed inset-0 z-[60] grid place-items-center bg-black/75 p-4 backdrop-blur-sm">
      <form onSubmit={onSubmit} className="w-full max-w-lg rounded-2xl border border-white/10 bg-[#0b0b0e] p-6 shadow-2xl">
        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-xl font-black text-white">{title}</h2>
            {description ? <p className="mt-2 text-sm leading-6 text-zinc-400">{description}</p> : null}
          </div>
          <button type="button" aria-label="Đóng" onClick={onClose} className="rounded-lg p-2 text-zinc-500 hover:bg-white/5 hover:text-white"><X size={19} /></button>
        </div>
        <div className="mt-6 space-y-4">{children}</div>
        <div className="mt-7 flex justify-end gap-3 border-t border-white/10 pt-5">
          <button type="button" onClick={onClose} className="rounded-xl border border-white/10 px-4 py-2.5 text-sm font-bold text-zinc-300 hover:bg-white/5">Hủy</button>
          <button type="submit" disabled={submitting} className={`rounded-xl px-4 py-2.5 text-sm font-black disabled:opacity-50 ${submitTone}`}>{submitting ? 'Đang xử lý…' : submitLabel}</button>
        </div>
      </form>
    </div>
  );
}

export function DetailGrid({ items = [] }) {
  return (
    <dl className="grid grid-cols-1 gap-px overflow-hidden rounded-xl border border-white/10 bg-white/10 sm:grid-cols-2">
      {items.map(item => (
        <div key={item.label} className="bg-[#0f0f12] p-4">
          <dt className="text-[10px] font-black uppercase tracking-[0.15em] text-zinc-600">{item.label}</dt>
          <dd className="mt-1.5 break-words text-sm font-semibold text-zinc-200">{item.value ?? '—'}</dd>
        </div>
      ))}
    </dl>
  );
}

export function ConsolePagination({ page = 0, totalPages = 0, totalElements = 0, onPage }) {
  if (totalPages <= 1) return null;
  return (
    <div className="flex items-center justify-between border-t border-white/10 px-5 py-4">
      <span className="text-xs font-semibold text-zinc-500">{totalElements} bản ghi · Trang {page + 1}/{totalPages}</span>
      <div className="flex gap-2">
        <button type="button" aria-label="Trang trước" disabled={page <= 0} onClick={() => onPage(page - 1)} className="rounded-lg border border-white/10 p-2 text-zinc-300 hover:bg-white/5 disabled:opacity-30"><ChevronLeft size={17} /></button>
        <button type="button" aria-label="Trang tiếp" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)} className="rounded-lg border border-white/10 p-2 text-zinc-300 hover:bg-white/5 disabled:opacity-30"><ChevronRight size={17} /></button>
      </div>
    </div>
  );
}
