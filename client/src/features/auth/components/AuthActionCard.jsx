import { Link } from 'react-router-dom';

export default function AuthActionCard({ title, subtitle, children, embedded = false }) {
  if (embedded) {
    return (
      <section className="rounded-2xl border border-zinc-800 bg-zinc-900/45 p-5 text-white shadow-2xl shadow-black/15 sm:p-7">
        <h2 className="text-lg font-black text-white">{title}</h2>
        {subtitle && <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-400">{subtitle}</p>}
        <div className="mt-6 max-w-2xl">{children}</div>
      </section>
    );
  }

  return (
    <main className="min-h-screen bg-zinc-950 px-6 py-20 text-white">
      <section className="mx-auto max-w-md rounded-2xl border border-zinc-800 bg-zinc-900 p-8 shadow-2xl">
        <Link to="/" className="text-sm text-zinc-400 hover:text-orange-400">← Trang chủ</Link>
        <h1 className="mt-6 text-2xl font-black uppercase tracking-wider">{title}</h1>
        {subtitle && <p className="mt-2 text-sm text-zinc-400">{subtitle}</p>}
        <div className="mt-7">{children}</div>
      </section>
    </main>
  );
}
