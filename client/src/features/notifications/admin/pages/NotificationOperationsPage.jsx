import { useCallback, useEffect, useState } from 'react';
import { AlertOctagon, ChevronLeft, ChevronRight, CircleDot, Clock3, RefreshCw, RotateCcw, Search, TestTube2 } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import { notificationAdminService } from '../services/notificationAdminService';
import { EmptyState, ErrorState, LoadingState, PageHeading, StatusPill, formatDateTime, shortSha } from '../components/NotificationAdminUi';

export default function NotificationOperationsPage() {
    const [searchParams, setSearchParams] = useSearchParams();
    const [page, setPage] = useState(0);
    const [data, setData] = useState(null);
    const [selected, setSelected] = useState(null);
    const [filters, setFilters] = useState({ query: '', sourceService: '', templateKey: '', status: '', test: '' });
    const [tab, setTab] = useState(searchParams.get('tab') === 'dead-letters' ? 'dead-letters' : 'requests');
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
                const params = Object.fromEntries(Object.entries({ page, size: 25, ...filters })
                    .filter(([, value]) => value !== ''));
                if (params.test !== undefined && params.test !== '') params.test = params.test === 'true';
                setData(await notificationAdminService.requests(params));
            }
        } catch (requestError) {
            setError(requestError?.message || 'Không thể tải dữ liệu vận hành thông báo.');
        } finally {
            setLoading(false);
        }
    }, [filters, page, tab]);

    useEffect(() => {
        const timer = setTimeout(load, tab === 'requests' ? 250 : 0);
        return () => clearTimeout(timer);
    }, [load, tab]);

    const changeTab = nextTab => {
        setTab(nextTab);
        setPage(0);
        setSelected(null);
        if (nextTab === 'dead-letters') setSearchParams({ tab: 'dead-letters' });
        else setSearchParams({});
    };

    const open = async publicId => {
        setDetailLoading(true);
        try {
            setSelected(await notificationAdminService.request(publicId));
        } catch (requestError) {
            setError(requestError?.message || 'Không thể tải chi tiết thông báo.');
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
            setError(requestError?.message || 'Không thể gửi lại thông báo.');
        }
    };

    const requests = data?.content || [];
    const currentPage = tab === 'requests' ? data : deadLetters;

    return (
        <div className="mx-auto max-w-[1600px] space-y-6 pb-10">
            <PageHeading
                eyebrow="Theo dõi lượt gửi"
                title="Vận hành thông báo"
                description="Tra cứu yêu cầu theo mã, sự kiện nguồn hoặc mã đối chiếu; mọi lần gửi lại đều được lưu vết."
                actions={<button type="button" onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-4 py-2.5 text-xs font-black text-white"><RefreshCw className="h-4 w-4" /> Làm mới</button>}
            />

            <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/60 p-3">
                <div className="flex rounded-xl bg-zinc-950 p-1">
                    <Tab active={tab === 'requests'} onClick={() => changeTab('requests')} icon={CircleDot}>Lịch sử gửi</Tab>
                    <Tab active={tab === 'dead-letters'} onClick={() => changeTab('dead-letters')} icon={AlertOctagon}>Cần xử lý</Tab>
                </div>
                {tab === 'requests' && (
                    <label className="relative w-full sm:w-96">
                        <Search className="absolute left-3 top-2.5 h-4 w-4 text-zinc-600" />
                        <input aria-label="Tìm yêu cầu thông báo" value={filters.query} onChange={event => { setPage(0); setFilters(current => ({ ...current, query: event.target.value })); }} placeholder="Tìm toàn bộ theo request, event, correlation…" className="w-full rounded-xl border border-zinc-700 bg-zinc-950 py-2 pl-10 pr-3 text-sm text-white outline-none focus:border-orange-400" />
                    </label>
                )}
            </div>

            {tab === 'requests' && (
                <section className="grid gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/40 p-3 sm:grid-cols-2 xl:grid-cols-4">
                    <input aria-label="Lọc theo dịch vụ nguồn" value={filters.sourceService} onChange={event => { setPage(0); setFilters(current => ({ ...current, sourceService: event.target.value })); }} placeholder="Dịch vụ nguồn" className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2.5 text-sm text-white outline-none focus:border-orange-400" />
                    <input aria-label="Lọc theo template" value={filters.templateKey} onChange={event => { setPage(0); setFilters(current => ({ ...current, templateKey: event.target.value.toUpperCase() })); }} placeholder="Template code" className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2.5 font-mono text-sm text-white outline-none focus:border-orange-400" />
                    <select aria-label="Lọc theo trạng thái" value={filters.status} onChange={event => { setPage(0); setFilters(current => ({ ...current, status: event.target.value })); }} className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-200"><option value="">Mọi trạng thái</option>{['ACCEPTED', 'PROCESSING', 'COMPLETED', 'PARTIALLY_FAILED', 'FAILED', 'CANCELLED'].map(value => <option key={value}>{value}</option>)}</select>
                    <select aria-label="Lọc production hoặc test" value={filters.test} onChange={event => { setPage(0); setFilters(current => ({ ...current, test: event.target.value })); }} className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-200"><option value="">Production và test</option><option value="false">Chỉ production</option><option value="true">Chỉ gửi thử</option></select>
                </section>
            )}

            {error && <ErrorState message={error} onRetry={load} />}
            {!error && loading && <LoadingState label="Đang tải dữ liệu vận hành thông báo…" />}
            {!error && !loading && tab === 'requests' && requests.length === 0 && (
                <EmptyState title="Chưa có yêu cầu thông báo" description="Yêu cầu sẽ xuất hiện sau khi hệ thống tiếp nhận sự kiện được hỗ trợ." />
            )}
            {!error && !loading && tab === 'requests' && requests.length > 0 && (
                <div className="grid gap-5 xl:grid-cols-[1fr_0.8fr]">
                    <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/50">
                        <div className="hidden grid-cols-[1.2fr_0.8fr_0.55fr] gap-3 border-b border-zinc-800 bg-zinc-950/40 px-5 py-3 text-[10px] font-black uppercase tracking-wider text-zinc-600 md:grid"><span>Sự kiện / request</span><span>Template / nguồn</span><span className="text-right">Trạng thái</span></div>
                        {requests.map(item => (
                            <button type="button" key={item.publicId} onClick={() => open(item.publicId)} className={`grid w-full gap-3 border-b border-zinc-800/70 px-5 py-4 text-left hover:bg-zinc-800/40 last:border-0 md:grid-cols-[1.2fr_0.8fr_0.55fr] md:items-center ${selected?.publicId === item.publicId ? 'bg-orange-500/5' : ''}`}>
                                <div className="min-w-0">
                                    <div className="flex items-center gap-2"><p className="truncate text-sm font-black text-white">{item.eventType}</p>{item.test && <TestTube2 className="h-3.5 w-3.5 text-violet-300" />}</div>
                                    <p className="mt-1 truncate font-mono text-[10px] text-zinc-500">{item.publicId}</p>
                                </div>
                                <div className="min-w-0">
                                    <p className="truncate text-xs font-bold text-zinc-300">{item.templateKey}</p>
                                    <p className="mt-1 truncate text-[10px] text-zinc-600">{item.sourceService} · <span className="font-mono">{item.correlationId || item.sourceEventId || 'không có mã đối chiếu'}</span></p>
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
                        ? <EmptyState title="Không có thông báo cần xử lý thủ công" description="Các lỗi đã hết lượt thử sẽ được giữ tại đây để gửi lại có kiểm soát." />
                        : deadLetters.content.map(item => (
                            <article key={item.id} className="grid gap-3 border-b border-zinc-800 px-5 py-4 last:border-0 md:grid-cols-[0.8fr_1fr_0.5fr]">
                                <div><p className="text-xs font-black text-red-300">{item.reason}</p><p className="mt-1 font-mono text-[10px] text-zinc-600">delivery #{item.notificationDeliveryId}</p></div>
                                <p className="text-xs leading-5 text-zinc-400">{item.failureMessage || 'Không có chi tiết lỗi từ nhà cung cấp.'}</p>
                                <div className="md:text-right"><p className="text-xs font-bold text-zinc-300">Đã gửi lại {item.reprocessCount} lần</p><p className="mt-1 text-[10px] text-zinc-600">{formatDateTime(item.createdAt)}</p></div>
                            </article>
                        ))}
                </section>
            )}

            {!error && !loading && currentPage && currentPage.totalPages > 1 && (
                <div className="flex items-center justify-between">
                    <p className="text-xs text-zinc-500">Trang {currentPage.number + 1}/{currentPage.totalPages}</p>
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
    if (loading) return <LoadingState label="Đang tải chi tiết lượt gửi…" />;
    if (!data) return <div className="flex min-h-72 items-center justify-center rounded-3xl border border-dashed border-zinc-700 text-sm text-zinc-600">Chọn một yêu cầu để xem các lượt gửi.</div>;
    return (
        <aside className="rounded-3xl border border-zinc-800 bg-zinc-900/70 p-5">
            <div className="flex items-start justify-between gap-3"><div><p className="text-[10px] font-black uppercase tracking-widest text-orange-400">Chi tiết yêu cầu</p><h2 className="mt-2 text-lg font-black text-white">{data.eventType}</h2></div><StatusPill value={data.status} /></div>
            <dl className="mt-5 grid grid-cols-2 gap-3 text-xs">
                <Detail label="Dịch vụ nguồn" value={data.sourceService} />
                <Detail label="Mẫu" value={data.templateKey} />
                <Detail label="Template revision" value={shortSha(data.templateCommitSha)} mono />
                <Detail label="Phiên bản" value={data.templateVersion || '—'} mono />
                <Detail label="Mã đối chiếu" value={data.correlationId || '—'} mono wide />
                <Detail label="Thời điểm tạo" value={formatDateTime(data.createdAt)} wide />
            </dl>
            <div className="mt-6 rounded-2xl border border-zinc-800 bg-zinc-950/50 p-4">
                <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Timeline xử lý</p>
                <Timeline at={data.createdAt} label="Tiếp nhận yêu cầu" detail={`${data.sourceService} · ${data.eventType}`} />
                {data.templateCommitSha && <Timeline at={data.updatedAt} label={`Resolve ${data.templateKey} · ${data.locale}`} detail={`Template revision ${shortSha(data.templateCommitSha)}`} />}
                {data.deliveries.flatMap(delivery => [
                    delivery.sentAt && <Timeline key={`${delivery.publicId}-sent`} at={delivery.sentAt} label={`${delivery.channel}: provider chấp nhận`} detail={`${delivery.provider} · lần thử ${delivery.attemptCount}`} />,
                    delivery.deliveredAt && <Timeline key={`${delivery.publicId}-delivered`} at={delivery.deliveredAt} label={`${delivery.channel}: xác nhận giao`} detail={delivery.providerMessageId || delivery.publicId} />,
                    delivery.failureCode && <Timeline key={`${delivery.publicId}-failed`} at={delivery.nextRetryAt || data.updatedAt} label={`${delivery.channel}: ${delivery.failureCode}`} detail={delivery.failureMessage || 'Không có mô tả lỗi'} error />,
                ]).filter(Boolean)}
            </div>
            <div className="mt-6 space-y-3">
                <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Các lượt gửi</p>
                {data.deliveries.map(delivery => (
                    <div key={delivery.publicId} className="rounded-2xl border border-zinc-800 bg-zinc-950/70 p-4">
                        <div className="flex items-center justify-between gap-3"><p className="text-xs font-black text-white">{delivery.channel} · {delivery.provider}</p><StatusPill value={delivery.status} /></div>
                        <p className="mt-2 font-mono text-[10px] text-zinc-600">{delivery.publicId}</p>
                        {delivery.failureMessage && <p className="mt-3 rounded-lg bg-red-400/5 p-2 text-xs leading-5 text-red-200">{delivery.failureCode}: {delivery.failureMessage}</p>}
                        {delivery.nextRetryAt && <p className="mt-2 inline-flex items-center gap-1.5 text-[10px] text-amber-300"><Clock3 className="h-3.5 w-3.5" /> Tự thử lại {formatDateTime(delivery.nextRetryAt)}</p>}
                        <div className="mt-3 flex items-center justify-between"><span className="text-[10px] text-zinc-500">Lần thử {delivery.attemptCount}</span>{['FAILED', 'DEAD_LETTERED'].includes(delivery.status) && isRetryable(delivery.failureCategory) && <button type="button" onClick={() => onRetry(delivery.publicId)} className="inline-flex items-center gap-1.5 text-[10px] font-black text-orange-300"><RotateCcw className="h-3.5 w-3.5" /> Thử lại ngay</button>}</div>
                        {['FAILED', 'DEAD_LETTERED'].includes(delivery.status) && !isRetryable(delivery.failureCategory) && <p className="mt-2 text-[10px] font-bold text-red-200">Cần sửa template, payload hoặc người nhận trước khi tạo yêu cầu mới.</p>}
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
function Timeline({ at, label, detail, error }) {
    return <div className="relative mt-4 border-l border-zinc-800 pl-4"><span className={`absolute -left-1 top-1 h-2 w-2 rounded-full ${error ? 'bg-red-400' : 'bg-emerald-400'}`} /><p className="text-xs font-bold text-zinc-200">{label}</p><p className="mt-1 text-[10px] text-zinc-600">{formatDateTime(at)} · {detail}</p></div>;
}
const isRetryable = category => !category || ['TRANSIENT', 'RATE_LIMITED', 'AUTHENTICATION_ERROR'].includes(category);
