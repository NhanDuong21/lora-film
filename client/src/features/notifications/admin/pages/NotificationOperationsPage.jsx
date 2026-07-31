import { useCallback, useEffect, useMemo, useState } from 'react';
import { AlertOctagon, ChevronLeft, ChevronRight, CircleDot, RefreshCw, RotateCcw, Search, TestTube2 } from 'lucide-react';
import { notificationAdminService } from '../services/notificationAdminService';
import { EmptyState, ErrorState, LoadingState, PageHeading, StatusPill, formatDateTime, shortSha } from '../components/NotificationAdminUi';

export default function NotificationOperationsPage() {
    const [page, setPage] = useState(0);
    const [data, setData] = useState(null);
    const [selected, setSelected] = useState(null);
    const [query, setQuery] = useState('');
    const [tab, setTab] = useState('requests');
    const [deadLetters, setDeadLetters] = useState(null);
    const [loading, setLoading] = useState(true);
    const [detailLoading, setDetailLoading] = useState(false);
    const [error, setError] = useState('');

    const load = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            if (tab === 'dead-letters') {
                setDeadLetters(await notificationAdminService.deadLetters({ page, size: 25 }));
            } else {
                setData(await notificationAdminService.requests({ page, size: 25 }));
            }
        } catch (requestError) {
            setError(requestError?.message || 'Notification operations could not be loaded.');
        } finally {
            setLoading(false);
        }
    }, [page, tab]);

    useEffect(() => {
        const timer = setTimeout(load, 0);
        return () => clearTimeout(timer);
    }, [load]);

    const open = async publicId => {
        setDetailLoading(true);
        try {
            setSelected(await notificationAdminService.request(publicId));
        } catch (requestError) {
            setError(requestError?.message || 'Notification detail could not be loaded.');
        } finally {
            setDetailLoading(false);
        }
    };

    const retry = async deliveryPublicId => {
        try {
            await notificationAdminService.retryDelivery(deliveryPublicId);
            await open(selected.publicId);
            await load();
        } catch (requestError) {
            setError(requestError?.message || 'Delivery could not be reprocessed.');
        }
    };

    const requests = useMemo(() => {
        const content = data?.content || [];
        const needle = query.trim().toLowerCase();
        if (!needle) return content;
        return content.filter(item => [
            item.publicId, item.sourceEventId, item.eventType, item.correlationId,
            item.templateKey, item.status,
        ].some(value => String(value || '').toLowerCase().includes(needle)));
    }, [data, query]);
    const currentPage = tab === 'requests' ? data : deadLetters;

    return (
        <div className="mx-auto max-w-[1600px] space-y-6 pb-10">
            <PageHeading
                eyebrow="Delivery observability"
                title="Notification operations"
                description="Trace requests by public ID, source event, or correlation ID. Reprocessing is explicit and every provider attempt remains auditable."
                actions={<button type="button" onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-4 py-2.5 text-xs font-black text-white"><RefreshCw className="h-4 w-4" /> Refresh</button>}
            />

            <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/60 p-3">
                <div className="flex rounded-xl bg-zinc-950 p-1">
                    <Tab active={tab === 'requests'} onClick={() => { setTab('requests'); setPage(0); }} icon={CircleDot}>Requests</Tab>
                    <Tab active={tab === 'dead-letters'} onClick={() => { setTab('dead-letters'); setPage(0); }} icon={AlertOctagon}>Dead letters</Tab>
                </div>
                {tab === 'requests' && (
                    <label className="relative w-full sm:w-96">
                        <Search className="absolute left-3 top-2.5 h-4 w-4 text-zinc-600" />
                        <input aria-label="Search notification requests" value={query} onChange={event => setQuery(event.target.value)} placeholder="Search this page…" className="w-full rounded-xl border border-zinc-700 bg-zinc-950 py-2 pl-10 pr-3 text-sm text-white outline-none focus:border-orange-400" />
                    </label>
                )}
            </div>

            {error && <ErrorState message={error} onRetry={load} />}
            {!error && loading && <LoadingState label="Loading notification operations…" />}
            {!error && !loading && tab === 'requests' && requests.length === 0 && (
                <EmptyState title="No notification requests" description="Requests will appear here after an internal API call or a supported Kafka domain event." />
            )}
            {!error && !loading && tab === 'requests' && requests.length > 0 && (
                <div className="grid gap-5 xl:grid-cols-[1fr_0.8fr]">
                    <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/50">
                        {requests.map(item => (
                            <button type="button" key={item.publicId} onClick={() => open(item.publicId)} className={`grid w-full gap-3 border-b border-zinc-800/70 px-5 py-4 text-left hover:bg-zinc-800/40 last:border-0 md:grid-cols-[1.2fr_0.8fr_0.55fr] md:items-center ${selected?.publicId === item.publicId ? 'bg-orange-500/5' : ''}`}>
                                <div className="min-w-0">
                                    <div className="flex items-center gap-2"><p className="truncate text-sm font-black text-white">{item.eventType}</p>{item.test && <TestTube2 className="h-3.5 w-3.5 text-violet-300" />}</div>
                                    <p className="mt-1 truncate font-mono text-[10px] text-zinc-500">{item.publicId}</p>
                                </div>
                                <div className="min-w-0">
                                    <p className="truncate text-xs font-bold text-zinc-300">{item.templateKey}</p>
                                    <p className="mt-1 truncate font-mono text-[10px] text-zinc-600">{item.correlationId || item.sourceEventId || 'no correlation'}</p>
                                </div>
                                <div className="md:text-right"><StatusPill value={item.status} /><p className="mt-1 text-[10px] text-zinc-600">{formatDateTime(item.createdAt)}</p></div>
                            </button>
                        ))}
                    </section>
                    <RequestDetail data={selected} loading={detailLoading} onRetry={retry} />
                </div>
            )}
            {!error && !loading && tab === 'dead-letters' && (
                <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/50">
                    {(deadLetters?.content || []).length === 0
                        ? <EmptyState title="Dead-letter queue is empty" description="Exhausted transient failures will be retained here for controlled reprocessing." />
                        : deadLetters.content.map(item => (
                            <article key={item.id} className="grid gap-3 border-b border-zinc-800 px-5 py-4 last:border-0 md:grid-cols-[0.8fr_1fr_0.5fr]">
                                <div><p className="text-xs font-black text-red-300">{item.reason}</p><p className="mt-1 font-mono text-[10px] text-zinc-600">delivery #{item.notificationDeliveryId}</p></div>
                                <p className="text-xs leading-5 text-zinc-400">{item.failureMessage || 'No provider detail supplied.'}</p>
                                <div className="md:text-right"><p className="text-xs font-bold text-zinc-300">{item.reprocessCount} reprocess attempts</p><p className="mt-1 text-[10px] text-zinc-600">{formatDateTime(item.createdAt)}</p></div>
                            </article>
                        ))}
                </section>
            )}

            {!error && !loading && currentPage && currentPage.totalPages > 1 && (
                <div className="flex items-center justify-between">
                    <p className="text-xs text-zinc-500">Page {currentPage.number + 1} of {currentPage.totalPages}</p>
                    <div className="flex gap-2">
                        <button type="button" disabled={currentPage.first} onClick={() => setPage(value => Math.max(0, value - 1))} className="rounded-xl border border-zinc-700 p-2 text-zinc-300 disabled:opacity-30"><ChevronLeft className="h-4 w-4" /></button>
                        <button type="button" disabled={currentPage.last} onClick={() => setPage(value => value + 1)} className="rounded-xl border border-zinc-700 p-2 text-zinc-300 disabled:opacity-30"><ChevronRight className="h-4 w-4" /></button>
                    </div>
                </div>
            )}
        </div>
    );
}

function RequestDetail({ data, loading, onRetry }) {
    if (loading) return <LoadingState label="Loading delivery detail…" />;
    if (!data) return <div className="flex min-h-72 items-center justify-center rounded-3xl border border-dashed border-zinc-700 text-sm text-zinc-600">Select a request to inspect its deliveries.</div>;
    return (
        <aside className="rounded-3xl border border-zinc-800 bg-zinc-900/70 p-5">
            <div className="flex items-start justify-between gap-3"><div><p className="text-[10px] font-black uppercase tracking-widest text-orange-400">Request detail</p><h2 className="mt-2 text-lg font-black text-white">{data.eventType}</h2></div><StatusPill value={data.status} /></div>
            <dl className="mt-5 grid grid-cols-2 gap-3 text-xs">
                <Detail label="Source" value={data.sourceService} />
                <Detail label="Template" value={data.templateKey} />
                <Detail label="Git commit" value={shortSha(data.templateCommitSha)} mono />
                <Detail label="Version" value={data.templateVersion || '—'} mono />
                <Detail label="Correlation" value={data.correlationId || '—'} mono wide />
                <Detail label="Created" value={formatDateTime(data.createdAt)} wide />
            </dl>
            <div className="mt-6 space-y-3">
                <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Deliveries</p>
                {data.deliveries.map(delivery => (
                    <div key={delivery.publicId} className="rounded-2xl border border-zinc-800 bg-zinc-950/70 p-4">
                        <div className="flex items-center justify-between gap-3"><p className="text-xs font-black text-white">{delivery.channel} · {delivery.provider}</p><StatusPill value={delivery.status} /></div>
                        <p className="mt-2 font-mono text-[10px] text-zinc-600">{delivery.publicId}</p>
                        {delivery.failureMessage && <p className="mt-3 rounded-lg bg-red-400/5 p-2 text-xs leading-5 text-red-200">{delivery.failureCode}: {delivery.failureMessage}</p>}
                        <div className="mt-3 flex items-center justify-between"><span className="text-[10px] text-zinc-500">Attempt {delivery.attemptCount}</span>{['FAILED', 'DEAD_LETTERED'].includes(delivery.status) && <button type="button" onClick={() => onRetry(delivery.publicId)} className="inline-flex items-center gap-1.5 text-[10px] font-black text-orange-300"><RotateCcw className="h-3.5 w-3.5" /> Reprocess</button>}</div>
                    </div>
                ))}
            </div>
        </aside>
    );
}
function Tab({ active, onClick, icon: Icon, children }) {
    return <button type="button" onClick={onClick} className={`inline-flex items-center gap-2 rounded-lg px-3 py-2 text-xs font-black ${active ? 'bg-zinc-800 text-white' : 'text-zinc-500'}`}><Icon className="h-3.5 w-3.5" />{children}</button>;
}
function Detail({ label, value, mono, wide }) {
    return <div className={`rounded-xl border border-zinc-800 bg-zinc-950/60 p-3 ${wide ? 'col-span-2' : ''}`}><dt className="text-[9px] font-black uppercase tracking-wider text-zinc-600">{label}</dt><dd className={`mt-1 truncate text-zinc-300 ${mono ? 'font-mono' : 'font-bold'}`}>{value}</dd></div>;
}
