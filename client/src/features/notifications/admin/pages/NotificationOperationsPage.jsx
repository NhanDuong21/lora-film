import { useCallback, useEffect, useState } from 'react';
import { AlertTriangle, ArrowRight, ChevronLeft, ChevronRight, Clock3, RefreshCw, RotateCcw, Search, ShieldAlert, Users } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';
import { notificationAdminService } from '../services/notificationAdminService';
import { EmptyState, ErrorState, LoadingState, PageHeading, StatusPill, TechnicalDetails, formatDateTime, formatNumber, shortSha } from '../components/NotificationAdminUi';
import { getNotificationFailurePresentation } from '../utils/notificationFailurePresentation';
import { channelBusinessName, channelOutcomeLines, deliveryOutcome, notificationBusinessName, recipientDisplay, serviceBusinessName } from '../utils/notificationBusinessPresentation';

export default function NotificationOperationsPage({ mode = 'history' }) {
    const [searchParams] = useSearchParams();
    const [page, setPage] = useState(0);
    const [data, setData] = useState(null);
    const [selected, setSelected] = useState(null);
    const [filters, setFilters] = useState({
        query: searchParams.get('query') || '', sourceService: '', templateKey: '',
        status: searchParams.get('status') || '', test: 'false', timeRange: '168', from: '', to: '',
    });
    const [deadLetters, setDeadLetters] = useState(null);
    const [coverage, setCoverage] = useState(null);
    const [activeRevision, setActiveRevision] = useState('');
    const [loading, setLoading] = useState(true);
    const [detailLoading, setDetailLoading] = useState(false);
    const [error, setError] = useState('');
    const [refreshing, setRefreshing] = useState(false);

    const load = useCallback(async ({ forceRefresh = false, background = false } = {}) => {
        if (background) setRefreshing(true);
        else setLoading(true);
        setError('');
        try {
            if (mode === 'attention') {
                const [deadLetterData, coverageData] = await Promise.all([
                    notificationAdminService.deadLetters({ page, size: 25 }),
                    notificationAdminService.coverage({ forceRefresh }),
                ]);
                setDeadLetters(deadLetterData);
                setCoverage(coverageData);
            } else {
                const rangeParams = buildRangeParams(filters);
                const params = Object.fromEntries(Object.entries({ page, size: 25, ...filters, ...rangeParams })
                    .filter(([, value]) => value !== ''));
                delete params.timeRange;
                if (params.test !== undefined && params.test !== '') params.test = params.test === 'true';
                const [requestData, dashboardData] = await Promise.all([
                    notificationAdminService.requests(params),
                    notificationAdminService.dashboard(
                        { hours: 24, includeTest: false },
                        { forceRefresh },
                    ),
                ]);
                setData(requestData);
                setActiveRevision(dashboardData?.templateRegistry?.headCommit || '');
            }
        } catch (requestError) {
            setError(requestError?.message || 'Không thể tải dữ liệu vận hành thông báo.');
        } finally {
            if (background) setRefreshing(false);
            else setLoading(false);
        }
    }, [filters, mode, page]);

    useEffect(() => {
        const hasTextFilter = mode === 'history'
            && [filters.query, filters.sourceService, filters.templateKey].some(Boolean);
        const timer = setTimeout(load, hasTextFilter ? 250 : 0);
        return () => clearTimeout(timer);
    }, [filters.query, filters.sourceService, filters.templateKey, load, mode]);

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

    const retry = async (deliveryPublicId, requestPublicId = selected?.publicId) => {
        try {
            await notificationAdminService.retryDelivery(deliveryPublicId);
            if (requestPublicId) await open(requestPublicId);
            await load();
        } catch (requestError) {
            setError(requestError?.message || 'Không thể gửi lại thông báo.');
        }
    };

    const requests = data?.content || [];
    const currentPage = mode === 'history' ? data : deadLetters;
    const blockedContracts = (coverage?.items || []).filter(item => item.readiness === 'BLOCKED');
    const deadLetterItems = deadLetters?.content || [];

    return (
        <div className="mx-auto max-w-[1600px] space-y-6 pb-10">
            <PageHeading
                eyebrow={mode === 'attention' ? 'Trung tâm hành động' : 'Theo dõi lượt gửi'}
                title={mode === 'attention' ? 'Cần xử lý' : 'Lịch sử gửi'}
                description={mode === 'attention'
                    ? 'Xem ai đang bị ảnh hưởng, hệ thống đã tự làm gì và hành động cần thực hiện tiếp theo.'
                    : 'Tìm một lượt gửi theo người nhận, mã đặt vé, loại thông báo hoặc mã đối chiếu.'}
                actions={<button type="button" onClick={() => load({ forceRefresh: true, background: true })} disabled={refreshing} className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-4 py-2.5 text-xs font-black text-white disabled:opacity-60"><RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} /> {refreshing ? 'Đang cập nhật' : 'Làm mới'}</button>}
            />

            {mode === 'history' && (
                <div className="flex flex-wrap items-center justify-end gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/60 p-3">
                    <label className="relative w-full sm:w-96">
                        <Search className="absolute left-3 top-2.5 h-4 w-4 text-zinc-600" />
                        <input aria-label="Tìm yêu cầu thông báo" value={filters.query} onChange={event => { setPage(0); setFilters(current => ({ ...current, query: event.target.value })); }} placeholder="Loại thông báo, mã đặt vé, mã yêu cầu hoặc đối chiếu…" className="w-full rounded-xl border border-zinc-700 bg-zinc-950 py-2 pl-10 pr-3 text-sm text-white outline-none focus:border-orange-400" />
                    </label>
                </div>
            )}

            {mode === 'history' && (
                <section className="grid gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/40 p-3 sm:grid-cols-2 xl:grid-cols-5">
                    <input aria-label="Lọc theo dịch vụ nguồn" value={filters.sourceService} onChange={event => { setPage(0); setFilters(current => ({ ...current, sourceService: event.target.value })); }} placeholder="Nhóm nghiệp vụ" className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2.5 text-sm text-white outline-none focus:border-orange-400" />
                    <input aria-label="Lọc theo template" value={filters.templateKey} onChange={event => { setPage(0); setFilters(current => ({ ...current, templateKey: event.target.value.toUpperCase() })); }} placeholder="Mã mẫu (nâng cao)" className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2.5 font-mono text-sm text-white outline-none focus:border-orange-400" />
                    <select aria-label="Lọc theo trạng thái" value={filters.status} onChange={event => { setPage(0); setFilters(current => ({ ...current, status: event.target.value })); }} className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-200"><option value="">Mọi trạng thái</option><option value="ACCEPTED">Đã tiếp nhận</option><option value="PROCESSING">Đang xử lý</option><option value="COMPLETED">Đã xử lý</option><option value="PARTIALLY_FAILED">Một số kênh chưa thành công</option><option value="FAILED">Thất bại</option><option value="CANCELLED">Đã hủy</option></select>
                    <select aria-label="Lọc dữ liệu thật hoặc gửi thử" value={filters.test} onChange={event => { setPage(0); setFilters(current => ({ ...current, test: event.target.value })); }} className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-200"><option value="false">Ẩn dữ liệu gửi thử</option><option value="true">Chỉ dữ liệu gửi thử</option><option value="">Hiện tất cả dữ liệu</option></select>
                    <select aria-label="Khoảng thời gian" value={filters.timeRange} onChange={event => { setPage(0); setFilters(current => ({ ...current, timeRange: event.target.value })); }} className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-200"><option value="24">Hôm nay</option><option value="168">7 ngày</option><option value="720">30 ngày</option><option value="custom">Khoảng tùy chọn</option></select>
                    {filters.timeRange === 'custom' && <div className="grid gap-2 sm:col-span-2 xl:col-span-5 sm:grid-cols-2"><label className="text-[10px] font-bold uppercase tracking-wider text-zinc-500">Từ thời điểm<input type="datetime-local" value={filters.from} onChange={event => setFilters(current => ({ ...current, from: event.target.value }))} className="mt-1 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-zinc-200" /></label><label className="text-[10px] font-bold uppercase tracking-wider text-zinc-500">Đến thời điểm<input type="datetime-local" value={filters.to} onChange={event => setFilters(current => ({ ...current, to: event.target.value }))} className="mt-1 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-zinc-200" /></label></div>}
                </section>
            )}

            {mode === 'attention' && (
                <section className="flex flex-wrap gap-2 rounded-2xl border border-zinc-800 bg-zinc-900/50 p-3 text-xs font-black">
                    <span className="rounded-xl bg-zinc-800 px-3 py-2 text-white">Tất cả {blockedContracts.length + Number(deadLetters?.totalElements || 0)}</span>
                    <span className="rounded-xl bg-red-400/10 px-3 py-2 text-red-200">Cấu hình {blockedContracts.length}</span>
                    <span className="rounded-xl bg-amber-400/10 px-3 py-2 text-amber-200">Lượt gửi {Number(deadLetters?.totalElements || 0)}</span>
                </section>
            )}

            {error && <ErrorState message={error} onRetry={load} />}
            {!error && loading && <LoadingState label="Đang tải dữ liệu vận hành thông báo…" />}
            {!error && !loading && mode === 'history' && requests.length === 0 && (
                <EmptyState title="Chưa có yêu cầu thông báo" description="Yêu cầu sẽ xuất hiện sau khi hệ thống tiếp nhận sự kiện được hỗ trợ." />
            )}
            {!error && !loading && mode === 'history' && requests.length > 0 && (
                <div className="grid gap-5 xl:grid-cols-[1fr_0.8fr]">
                    <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/50">
                        <div className="hidden grid-cols-[1.15fr_0.75fr_0.9fr] gap-3 border-b border-zinc-800 bg-zinc-950/40 px-5 py-3 text-xs font-bold text-zinc-400 md:grid"><span>Thông báo / người nhận</span><span>Nhóm nghiệp vụ</span><span className="text-right">Kết quả theo kênh</span></div>
                        {requests.map(item => (
                            <button type="button" key={item.publicId} onClick={() => open(item.publicId)} className={`grid w-full gap-3 border-b border-zinc-800/70 px-5 py-4 text-left hover:bg-zinc-800/40 last:border-0 md:grid-cols-[1.15fr_0.75fr_0.9fr] md:items-center ${selected?.publicId === item.publicId ? 'bg-orange-500/5' : ''}`}>
                                <div className="min-w-0">
                                    <div className="flex items-center gap-2"><p className="truncate text-sm font-black text-white">{notificationBusinessName(item.eventType, item.templateKey)}</p>{item.test && <StatusPill value="TEST" />}</div>
                                    <p className="mt-1 truncate text-xs text-zinc-400">Gửi tới: {recipientDisplay(item.recipient)}</p>
                                    <p className="mt-1 truncate font-mono text-[10px] text-zinc-600">{item.eventType} · {item.publicId}</p>
                                </div>
                                <div className="min-w-0">
                                    <p className="truncate text-xs font-bold text-zinc-300">{serviceBusinessName(item.sourceService)}</p>
                                    <p className="mt-1 truncate text-[10px] text-zinc-500">Mẫu <span className="font-mono">{item.templateKey}</span>{(item.correlationId || item.sourceEventId) && <> · <span className="font-mono">{item.correlationId || item.sourceEventId}</span></>}</p>
                                </div>
                                <RequestResultSummary item={item} />
                            </button>
                        ))}
                    </section>
                    <RequestDetail data={selected} loading={detailLoading} onRetry={retry} activeRevision={activeRevision} />
                </div>
            )}
            {!error && !loading && mode === 'attention' && blockedContracts.length === 0 && deadLetterItems.length === 0 && (
                <EmptyState title="Không có việc cần xử lý" description="Không có lỗi cấu hình hoặc lượt gửi hết khả năng tự phục hồi." />
            )}
            {!error && !loading && mode === 'attention' && (blockedContracts.length > 0 || deadLetterItems.length > 0) && (
                <section className="space-y-3">
                    {blockedContracts.map(item => (
                        <article key={`${item.templateKey}-${item.locale}`} className="rounded-3xl border border-red-400/20 bg-red-400/5 p-5">
                            <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between"><div className="max-w-3xl"><div className="flex flex-wrap items-center gap-2"><ShieldAlert className="h-5 w-5 text-red-300" /><h2 className="text-base font-black text-white">Không thể gửi {notificationBusinessName(item.eventTypes?.[0], item.templateKey)}</h2><StatusPill value="BLOCKED" /></div><p className="mt-3 text-sm text-zinc-200">Nguyên nhân: Luồng này chưa có mẫu thông báo đang hoạt động.</p><p className="mt-2 text-xs leading-5 text-zinc-400">Ảnh hưởng: mọi yêu cầu mới từ {serviceBusinessName(item.sourceService)}. Hệ thống chưa thể tự khắc phục cho đến khi mẫu được tạo và phát hành.</p><p className="mt-3 text-xs font-bold text-orange-200">Đề xuất: tạo mẫu còn thiếu, kiểm tra nội dung rồi phát hành.</p><TechnicalDetails className="mt-4"><p>{item.templateKey} chưa có template đang hoạt động · {item.sourceService} · {item.eventTypes?.join(', ')} · {item.channels?.join(' · ')} · {item.locale}</p></TechnicalDetails></div><div className="flex shrink-0 flex-wrap gap-2"><Link to={`/admin/notification-templates?contract=${item.templateKey}`} className="rounded-xl bg-red-500 px-3 py-2 text-xs font-black text-white">Tạo mẫu còn thiếu</Link><Link to="/admin/notification-coverage" className="inline-flex items-center gap-1.5 rounded-xl border border-zinc-700 px-3 py-2 text-xs font-black text-zinc-200">Xem phạm vi <ArrowRight className="h-3.5 w-3.5" /></Link></div></div>
                        </article>
                    ))}
                    {deadLetterItems.map(item => (
                        <article key={item.id} className="rounded-3xl border border-amber-400/20 bg-zinc-900/60 p-5">
                            <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between"><div className="max-w-3xl"><div className="flex flex-wrap items-center gap-2"><AlertTriangle className="h-5 w-5 text-amber-300" /><h2 className="text-base font-black text-white">Không thể gửi {notificationBusinessName(item.eventType, item.templateKey)}</h2><span className="rounded-full border border-amber-400/20 bg-amber-400/10 px-2.5 py-1 text-[10px] font-black uppercase tracking-wider text-amber-200">Cần xử lý</span></div><div className="mt-3 flex flex-wrap gap-x-5 gap-y-2 text-xs text-zinc-400"><span className="inline-flex items-center gap-1.5"><Clock3 className="h-3.5 w-3.5" /> Bắt đầu {formatDateTime(item.createdAt)}</span><span className="inline-flex items-center gap-1.5"><Users className="h-3.5 w-3.5" /> 1 người nhận bị ảnh hưởng: {recipientDisplay(item.recipient)}</span></div><p className="mt-4 text-sm font-bold text-zinc-100">{getNotificationFailurePresentation(item.reason).title}</p><p className="mt-1 text-xs leading-5 text-zinc-300">{getNotificationFailurePresentation(item.reason).description}</p><p className="mt-3 text-xs text-zinc-400">Hệ thống đã thử gửi {formatNumber(item.attemptCount)} lần và không còn tự thử lại.</p><p className="mt-3 text-xs font-bold text-orange-200">{item.retryAllowed ? 'Đề xuất: thử gửi lại sau khi đã xác nhận nguyên nhân được xử lý.' : item.retryBlockedReason}</p><TechnicalDetails className="mt-4"><dl className="space-y-2"><DetailLine label="Mã kỹ thuật" value={item.reason || 'UNKNOWN_FAILURE'} /><DetailLine label="Dịch vụ nguồn" value={item.sourceService || '—'} /><DetailLine label="Mã yêu cầu" value={item.requestPublicId || '—'} /><DetailLine label="Mã lượt gửi" value={item.deliveryPublicId || `#${item.notificationDeliveryId}`} /></dl></TechnicalDetails></div><div className="flex shrink-0 flex-wrap gap-2">{item.retryAllowed && <button type="button" onClick={() => retry(item.deliveryPublicId, item.requestPublicId)} className="inline-flex items-center gap-2 rounded-xl bg-orange-500 px-3 py-2 text-xs font-black text-white"><RotateCcw className="h-3.5 w-3.5" /> Thử gửi lại</button>}<Link to={`/admin/notification-operations?query=${item.requestPublicId || ''}`} className="inline-flex items-center gap-1.5 rounded-xl border border-zinc-700 px-3 py-2 text-xs font-black text-zinc-200">Xem lượt bị ảnh hưởng <ArrowRight className="h-3.5 w-3.5" /></Link></div></div>
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

function RequestDetail({ data, loading, onRetry, activeRevision }) {
    if (loading) return <LoadingState label="Đang tải chi tiết lượt gửi…" />;
    if (!data) return <div className="flex min-h-72 items-center justify-center rounded-3xl border border-dashed border-zinc-700 text-sm text-zinc-600">Chọn một yêu cầu để xem các lượt gửi.</div>;
    const deliveries = data.deliveries || [];
    const firstAttemptAt = deliveries
        .flatMap(delivery => delivery.attempts || [])
        .map(attempt => attempt.createdAt)
        .filter(Boolean)
        .sort()[0];
    const finalDelivery = deliveries.find(delivery => ['FAILED', 'DEAD_LETTERED'].includes(delivery.status))
        || deliveries.find(delivery => delivery.status === 'DELIVERED')
        || deliveries[deliveries.length - 1];
    const totalAttempts = deliveries.reduce((total, delivery) => total + Number(delivery.attemptCount || 0), 0);
    const durationMs = deliveries.flatMap(delivery => delivery.attempts || [])
        .reduce((total, attempt) => total + Number(attempt.durationMs || 0), 0);
    return (
        <aside className="rounded-3xl border border-zinc-800 bg-zinc-900/70 p-5">
            <div className="flex items-start justify-between gap-3"><div><p className="text-[10px] font-black uppercase tracking-widest text-orange-400">Kết quả thông báo</p><h2 className="mt-2 text-lg font-black text-white">{notificationBusinessName(data.eventType, data.templateKey)}</h2><p className="mt-1 font-mono text-[10px] text-zinc-500">{data.eventType}</p></div><StatusPill value={data.status} /></div>
            <dl className="mt-5 grid grid-cols-2 gap-3 text-xs">
                <Detail label="Người nhận" value={recipientDisplay(data.recipient)} wide />
                <Detail label="Kênh" value={deliveries.map(delivery => channelBusinessName(delivery.channel)).join(' · ') || '—'} />
                <Detail label="Kết quả cuối" value={deliveryOutcome(finalDelivery)} />
                <Detail label="Số lần thử" value={`${formatNumber(totalAttempts)} lần`} />
                <Detail label="Thời gian xử lý" value={durationMs > 0 ? `${(durationMs / 1000).toFixed(1).replace('.', ',')} giây` : 'Chưa có dữ liệu'} />
                <Detail label="Thời điểm tạo" value={formatDateTime(data.createdAt)} wide />
            </dl>
            {finalDelivery?.failureCode && <FailureNotice code={finalDelivery.failureCode} />}
            {finalDelivery && ['FAILED', 'DEAD_LETTERED'].includes(finalDelivery.status) && finalDelivery.retryAllowed === false && <p className="mt-3 rounded-xl border border-amber-400/20 bg-amber-400/5 px-3 py-2 text-xs font-bold leading-5 text-amber-100">{finalDelivery.retryBlockedReason}</p>}
            <TechnicalDetails className="mt-4" summary="Thông tin kỹ thuật của yêu cầu"><dl className="space-y-3"><DetailLine label="Dịch vụ nguồn" value={data.sourceService} /><DetailLine label="Mẫu" value={data.templateKey} /><DetailLine label="Revision tại thời điểm gửi" value={shortSha(data.templateCommitSha)} mono />{data.templateVersion && <DetailLine label="Phiên bản mẫu" value={data.templateVersion} mono />}{data.correlationId && <DetailLine label="Mã đối chiếu" value={data.correlationId} mono />}<DetailLine label="Mã yêu cầu" value={data.publicId} mono /></dl></TechnicalDetails>
            {data.templateCommitSha && activeRevision && data.templateCommitSha !== activeRevision && <p className="mt-3 rounded-xl border border-sky-400/15 bg-sky-400/5 px-3 py-2 text-xs leading-5 text-sky-100">Lượt gửi này sử dụng phiên bản cũ của mẫu. Nội dung đã gửi vẫn được lưu nguyên để đối chiếu.</p>}
            <div className="mt-6 rounded-2xl border border-zinc-800 bg-zinc-950/50 p-4">
                <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Timeline xử lý</p>
                <Timeline at={data.createdAt} label="Tiếp nhận yêu cầu" detail={notificationBusinessName(data.eventType, data.templateKey)} />
                {data.templateCommitSha && <Timeline at={firstAttemptAt || data.updatedAt} label="Chuẩn bị nội dung gửi" detail="Đã lưu nội dung tại thời điểm gửi" />}
                {deliveries.flatMap(delivery => [
                    delivery.sentAt && <Timeline key={`${delivery.publicId}-sent`} at={delivery.sentAt} label={`${channelBusinessName(delivery.channel)}: nhà cung cấp đã nhận`} detail={`Lần thử ${delivery.attemptCount}`} />,
                    delivery.deliveredAt && <Timeline key={`${delivery.publicId}-delivered`} at={delivery.deliveredAt} label={`${channelBusinessName(delivery.channel)}: đã có xác nhận giao`} detail={delivery.providerMessageId || 'Hoàn tất'} />,
                    ...(delivery.attempts || []).filter(attempt => attempt.failureCode).map(attempt => {
                        const failure = getNotificationFailurePresentation(attempt.failureCode);
                        return <Timeline key={`${delivery.publicId}-attempt-${attempt.attemptNumber}`} at={attempt.createdAt} label={`${channelBusinessName(delivery.channel)} · lần thử ${attempt.attemptNumber}: ${failure.title}`} detail={failure.description} error />;
                    }),
                    delivery.failureCode && !delivery.attempts?.length && <Timeline key={`${delivery.publicId}-failed`} at={data.updatedAt} label={`${channelBusinessName(delivery.channel)}: ${getNotificationFailurePresentation(delivery.failureCode).title}`} detail={getNotificationFailurePresentation(delivery.failureCode).description} error />,
                ]).filter(Boolean)}
            </div>
            <div className="mt-6 space-y-3">
                <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Các lượt gửi</p>
                {deliveries.map(delivery => (
                    <div key={delivery.publicId} className="rounded-2xl border border-zinc-800 bg-zinc-950/70 p-4">
                        <div className="flex items-center justify-between gap-3"><div><p className="text-xs font-black text-white">{channelBusinessName(delivery.channel)}</p><p className="mt-1 text-xs text-zinc-400">{deliveryOutcome(delivery)}</p></div><StatusPill value={delivery.status} /></div>
                        {delivery.failureCode && <FailureNotice code={delivery.failureCode} />}
                        {delivery.nextRetryAt && <p className="mt-2 inline-flex items-center gap-1.5 text-[10px] text-amber-300"><Clock3 className="h-3.5 w-3.5" /> Tự thử lại {formatDateTime(delivery.nextRetryAt)}</p>}
                        <div className="mt-3 flex items-center justify-between"><span className="text-xs text-zinc-400">Đã thử {delivery.attemptCount} lần</span>{['FAILED', 'DEAD_LETTERED'].includes(delivery.status) && (delivery.retryAllowed ?? isRetryable(delivery.failureCategory)) && <button type="button" onClick={() => onRetry(delivery.publicId)} className="inline-flex items-center gap-1.5 text-xs font-black text-orange-300"><RotateCcw className="h-3.5 w-3.5" /> Thử gửi lại</button>}</div>
                        {['FAILED', 'DEAD_LETTERED'].includes(delivery.status) && !(delivery.retryAllowed ?? isRetryable(delivery.failureCategory)) && <p className="mt-2 text-xs font-bold leading-5 text-red-200">{delivery.retryBlockedReason || 'Cần sửa người nhận, nội dung mẫu hoặc dữ liệu đầu vào trước khi tạo yêu cầu mới.'}</p>}
                        <TechnicalDetails className="mt-3"><dl className="space-y-2"><DetailLine label="Nhà cung cấp" value={delivery.provider} /><DetailLine label="Mã lượt gửi" value={delivery.publicId} mono />{delivery.renderedSnapshotAvailable && <DetailLine label="Nội dung đã lưu" value={`Revision ${shortSha(delivery.templateCommitSha)}; gửi lại sẽ dùng đúng snapshot này`} />}</dl></TechnicalDetails>
                    </div>
                ))}
            </div>
        </aside>
    );
}
function RequestResultSummary({ item }) {
    const lines = channelOutcomeLines(item.channelOutcomes);
    return <div className="md:text-right"><StatusPill value={item.status} />{lines.length > 0 && <div className="mt-2 space-y-1">{lines.map(line => <p key={`${line.channel}-${line.outcome}`} className="text-[10px] leading-4 text-zinc-400"><span className="font-bold text-zinc-300">{line.channel}:</span> {line.outcome}</p>)}</div>}<p className="mt-1 text-[10px] text-zinc-600">{formatDateTime(item.createdAt)}</p></div>;
}
function Detail({ label, value, mono, wide }) {
    return <div className={`rounded-xl border border-zinc-800 bg-zinc-950/60 p-3 ${wide ? 'col-span-2' : ''}`}><dt className="text-[9px] font-black uppercase tracking-wider text-zinc-600">{label}</dt><dd className={`mt-1 truncate text-zinc-300 ${mono ? 'font-mono' : 'font-bold'}`}>{value}</dd></div>;
}
function DetailLine({ label, value, mono }) {
    return <div className="flex items-start justify-between gap-4"><dt className="shrink-0 text-zinc-500">{label}</dt><dd className={`max-w-[70%] break-all text-right text-zinc-200 ${mono ? 'font-mono' : 'font-bold'}`}>{value}</dd></div>;
}
function Timeline({ at, label, detail, error }) {
    return <div className="relative mt-4 border-l border-zinc-800 pl-4"><span className={`absolute -left-1 top-1 h-2 w-2 rounded-full ${error ? 'bg-red-400' : 'bg-emerald-400'}`} /><p className="text-xs font-bold text-zinc-200">{label}</p><p className="mt-1 text-[10px] text-zinc-600">{formatDateTime(at)} · {detail}</p></div>;
}
function FailureNotice({ code }) {
    const failure = getNotificationFailurePresentation(code);
    return <div className="mt-3 rounded-xl border border-red-400/10 bg-red-400/5 p-3"><p className="text-xs font-black text-red-200">{failure.title}</p><p className="mt-1 text-xs leading-5 text-zinc-300">{failure.description}</p><p className="mt-2 font-mono text-[10px] text-zinc-500">Mã kỹ thuật: {failure.code}</p></div>;
}
const isRetryable = category => !category || ['TRANSIENT', 'RATE_LIMITED', 'AUTHENTICATION_ERROR'].includes(category);
const buildRangeParams = filters => {
    if (filters.timeRange === 'custom') {
        return {
            from: filters.from ? new Date(filters.from).toISOString() : '',
            to: filters.to ? new Date(filters.to).toISOString() : '',
        };
    }
    const hours = Number(filters.timeRange || 168);
    return { from: new Date(Date.now() - hours * 60 * 60 * 1000).toISOString(), to: '' };
};
