import { useEffect, useRef, useState } from 'react';
import { Search, UserRound, WalletCards } from 'lucide-react';
import { getCustomers } from '@/features/internal-staff/admin/services/userAdminService';
import scoreAdminService from '../services/scoreAdminService';

const n = value => Number(value ?? 0).toLocaleString('vi-VN');

export default function CustomerScoreSearch({ initialQuery = '', onSelect, autoFocus = false }) {
  const [query, setQuery] = useState(initialQuery);
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const requestRef = useRef(0);
  const suppressNextSearchRef = useRef(Boolean(initialQuery));

  useEffect(() => {
    if (suppressNextSearchRef.current) {
      suppressNextSearchRef.current = false;
      setResults([]);
      setOpen(false);
      return undefined;
    }
    const keyword = query.trim();
    if (keyword.length < 2) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setResults([]);
      setOpen(/^\d+$/.test(keyword));
      return undefined;
    }
    const requestId = ++requestRef.current;
    const timer = window.setTimeout(async () => {
      setLoading(true);
      try {
        const response = await getCustomers({ page: 0, size: 6, keyword });
        const customers = response?.content || [];
        const enriched = await Promise.all(customers.map(async customer => {
          if (!customer.accountId) return { ...customer, score: null };
          try {
            const score = await scoreAdminService.getScoreByAccount(customer.accountId);
            return { ...customer, score };
          } catch {
            return { ...customer, score: null };
          }
        }));
        if (requestId === requestRef.current) {
          setResults(enriched);
          setOpen(true);
        }
      } finally {
        if (requestId === requestRef.current) setLoading(false);
      }
    }, 250);
    return () => window.clearTimeout(timer);
  }, [query]);

  const choose = customer => {
    const accountId = customer.accountId || customer.id;
    if (!accountId) return;
    suppressNextSearchRef.current = true;
    requestRef.current += 1;
    setQuery(customer.fullName || customer.customerCode || `Tài khoản điểm ${accountId}`);
    setOpen(false);
    onSelect?.({ ...customer, accountId: String(accountId) });
  };

  const directAccount = /^\d+$/.test(query.trim())
    && !results.some(item => String(item.accountId) === query.trim());

  return (
    <div className="relative">
      <label className="relative block">
        <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-600" size={18} />
        <input
          autoFocus={autoFocus}
          value={query}
          onChange={event => { setQuery(event.target.value); setOpen(true); }}
          onFocus={() => setOpen(true)}
          aria-label="Tìm khách hàng hoặc tài khoản điểm"
          placeholder="Tên, email, số điện thoại, mã khách hoặc ID tài khoản"
          className="h-12 w-full rounded-xl border border-white/10 bg-black/30 pl-11 pr-24 text-sm text-white outline-none placeholder:text-zinc-600 focus:border-brand-orange"
        />
        <span className="absolute right-3 top-1/2 -translate-y-1/2 text-[10px] font-bold text-zinc-600">{loading ? 'ĐANG TÌM…' : '2 KÝ TỰ HOẶC ID'}</span>
      </label>

      {open && (query.trim().length >= 2 || /^\d+$/.test(query.trim())) ? (
        <div className="absolute z-30 mt-2 max-h-80 w-full overflow-y-auto rounded-2xl border border-white/10 bg-zinc-950 p-2 shadow-2xl">
          {results.map(customer => (
            <button key={customer.id || customer.accountId} type="button" onClick={() => choose(customer)} className="flex w-full items-center gap-3 rounded-xl p-3 text-left hover:bg-white/5">
              <span className="rounded-xl bg-white/5 p-2 text-zinc-500"><UserRound size={17} /></span>
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm font-black text-white">{customer.fullName}</span>
                <span className="mt-1 block truncate text-[11px] text-zinc-500">{customer.customerCode || 'Chưa có mã khách hàng'} · {customer.email || customer.phoneNumber || 'Chưa có liên hệ'}</span>
              </span>
              <span className="shrink-0 text-right text-[11px] text-zinc-500">
                {customer.score ? <><b className="block text-emerald-400">{n(customer.score.availablePoints)} điểm</b>{customer.score.currentTier?.tierName || customer.score.currentTier?.tierCode}</> : 'Chưa có tài khoản điểm'}
              </span>
            </button>
          ))}
          {directAccount ? (
            <button type="button" onClick={() => choose({ accountId: query.trim(), fullName: `Tài khoản điểm ${query.trim()}` })} className="flex w-full items-center gap-3 rounded-xl p-3 text-left hover:bg-white/5">
              <span className="rounded-xl bg-brand-orange/10 p-2 text-brand-orange"><WalletCards size={17} /></span>
              <span><b className="block text-sm text-white">Mở trực tiếp tài khoản #{query.trim()}</b><span className="mt-1 block text-[11px] text-zinc-500">Dùng khi tài khoản điểm chưa liên kết hồ sơ khách hàng.</span></span>
            </button>
          ) : null}
          {!loading && !results.length && !directAccount ? <p className="p-5 text-center text-xs text-zinc-500">Không tìm thấy khách hàng phù hợp.</p> : null}
        </div>
      ) : null}
    </div>
  );
}
