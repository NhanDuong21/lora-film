import { useCallback, useEffect, useMemo, useState } from 'react';
import {
    AlertOctagon, ArrowRight, BellRing, CheckCheck, Clock3,
    GitBranch, RefreshCw, Send, ShieldCheck, Siren,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { notificationAdminService } from '../services/notificationAdminService';
import {
    ErrorState, LoadingState, PageHeading, StatusPill, TechnicalDetails,
    formatDateTime, formatNumber, formatPercent, shortSha,
} from '../components/NotificationAdminUi';
import {
    channelBusinessName, notificationBusinessName, serviceBusinessName,
} from '../utils/notificationBusinessPresentation';

const journeyStages = [
    { key: 'totalRequests', label: 'Yêu cầu thông báo', icon: BellRing },
    { key: 'totalDeliveries', label: 'Lượt gửi theo kênh', icon: Send },
];

const resultStages = [
    { key: 'accepted', fallback: 'delivered', label: 'Nhà cung cấp đã nhận', icon: CheckCheck },
    { key: 'confirmed', label: 'Có xác nhận giao', icon: ShieldCheck },
    { key: 'failed', label: 'Lượt gửi thất bại', icon: AlertOctagon },
];

export default function NotificationDashboardPage() {
    const [data, setData] = useState(null);
    const [filters, setFilters] = useState({ hours: 24, includeTest: false });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const load = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            setData(await notificationAdminService.dashboard(filters));
        } catch (requestError) {
            setError(requestError?.message || 'Dịch vụ thông báo chưa trả về số liệu vận hành.');
        } finally {
            setLoading(false);
        }
    }, [filters]);

    useEffect(() => {
        const timer = setTimeout(load, 0);
        return () => clearTimeout(timer);
    }, [load]);

    const blockedItems = (data?.coverage?.items || []).filter(item => item.readiness === 'BLOCKED');
    const activeIncidents = Number(data?.activeIncidents ?? (Number(data?.deadLetters || 0) + blockedItems.length));
    const channels = useMemo(() => Object.entries(data?.channels || {}), [data]);
    const cta = activeIncidents > 0
        ? { to: '/admin/notification-attention', label: `Xử lý ${formatNumber(activeIncidents)} sự cố` }
        : Number(data?.failed) > 0
            ? { to: '/admin/notification-operations?status=FAILED', label: `Xem ${formatNumber(data.failed)} lượt thất bại` }
            : { to: '/admin/notification-operations', label: 'Xem lịch sử gửi' };

    if (loading) return <LoadingState label="Đang tải số liệu gửi thông báo…" />;
    if (error) return <ErrorState message={error} onRetry={load} />;

    return (
        <div className="mx-auto max-w-[1500px] space-y-6 pb-10">
            <PageHeading
                eyebrow="Vận hành thông báo"
                title="Trung tâm điều phối thông báo"
                description="Biết ngay hệ thống có ổn không, luồng nào bị ảnh hưởng và hành động cần thực hiện."
                actions={<>
                    <button type="button" onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-4 py-2.5 text-xs font-bold text-zinc-200 hover:border-zinc-500"><RefreshCw className="h-4 w-4" /> Làm mới</button>
                    <Link to={cta.to} className={`inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-xs font-black text-white ${activeIncidents > 0 ? 'bg-red-500 hover:bg-red-400' : 'bg-orange-500 hover:bg-orange-400'}`}>{cta.label} <ArrowRight className="h-4 w-4" /></Link>
                </>}
            />

            <section className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/60 p-3">
                <div className="flex flex-wrap gap-2" role="group" aria-label="Phạm vi số liệu">
                    {[24, 168, 720].map(hours => <button key={hours} type="button" onClick={() => setFilters(current => ({ ...current, hours }))} className={`rounded-xl px-3 py-2 text-xs font-black ${filters.hours === hours ? 'bg-orange-500 text-white' : 'bg-zinc-950 text-zinc-400 hover:text-white'}`}>{hours === 24 ? '24 giờ' : hours === 168 ? '7 ngày' : '30 ngày'}</button>)}
                </div>
                <label className={`inline-flex cursor-pointer items-center gap-3 rounded-xl border px-3 py-2 text-xs font-bold transition ${filters.includeTest ? 'border-violet-400/30 bg-violet-400/10 text-violet-200' : 'border-zinc-800 bg-zinc-950 text-zinc-400'}`}>
                    <span>Bao gồm dữ liệu gửi thử</span>
                    <input type="checkbox" role="switch" aria-label="Bao gồm lượt gửi thử" checked={filters.includeTest} onChange={event => setFilters(current => ({ ...current, includeTest: event.target.checked }))} className="peer sr-only" />
                    <span aria-hidden="true" className="relative h-5 w-9 rounded-full bg-zinc-700 transition peer-checked:bg-violet-500 after:absolute after:left-0.5 after:top-0.5 after:h-4 after:w-4 after:rounded-full after:bg-white after:transition-transform peer-checked:after:translate-x-4" />
                </label>
            </section>

            <OperationsConclusion activeIncidents={activeIncidents} failed={Number(data?.failed || 0)} blockedItems={blockedItems} hours={filters.hours} />

            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                <HealthCard label="Dịch vụ thông báo" value="Đang hoạt động" badge="Bình thường" tone="success" detail={`Cập nhật ${formatDateTime(data?.generatedAt)}`} />
                <HealthCard label="Mẫu thông báo" value={data?.templateRegistry?.available ? 'Đã đồng bộ' : 'Không khả dụng'} badge={data?.templateRegistry?.available ? 'Sẵn sàng' : 'Cần xử lý'} tone={data?.templateRegistry?.available ? 'success' : 'danger'} detail={`Cập nhật ${formatDateTime(data?.templateRegistry?.lastSyncedAt)}`} />
                <HealthCard label="Luồng thông báo quan trọng" value={`${formatNumber(data?.coverage?.readyRequirements)}/${formatNumber(data?.coverage?.totalRequirements)} đã có mẫu hoạt động`} badge={Number(data?.coverage?.blockedRequirements) > 0 ? `Thiếu ${formatNumber(data?.coverage?.blockedRequirements)}` : 'Đầy đủ'} tone={Number(data?.coverage?.blockedRequirements) > 0 ? 'danger' : 'success'} detail="Theo phạm vi tích hợp đang khai báo" />
            </div>

            <section className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5 sm:p-6">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div><p className="text-xs font-black uppercase tracking-widest text-zinc-500">Luồng xử lý trong phạm vi đã chọn</p><h2 className="mt-2 text-lg font-black text-white">Từ yêu cầu đến kết quả gửi</h2></div>
                    <div className="rounded-2xl border border-emerald-400/20 bg-emerald-400/5 px-4 py-3 text-right"><p className="text-[10px] font-bold uppercase tracking-wider text-emerald-300">Tỷ lệ nhà cung cấp tiếp nhận</p><p className="mt-1 text-2xl font-black text-white">{formatPercent(data?.deliveryRate)}</p></div>
                </div>
                <div className="mt-6 grid gap-5 xl:grid-cols-[0.72fr_1.28fr]">
                    <div className="grid gap-3 sm:grid-cols-2">
                        {journeyStages.map((stage, index) => <FunnelCard key={stage.key} stage={stage} data={data} arrow={index === 0} />)}
                    </div>
                    <div className="rounded-2xl border border-zinc-800/80 bg-zinc-950/25 p-3">
                        <p className="px-1 text-[10px] font-black uppercase tracking-widest text-zinc-500">Kết quả của {formatNumber(data?.totalDeliveries)} lượt gửi</p>
                        <div className="mt-3 grid gap-3 sm:grid-cols-3">
                            {resultStages.map(stage => <FunnelCard key={stage.key} stage={stage} data={data} />)}
                        </div>
                    </div>
                </div>
                <p className="mt-4 text-xs leading-5 text-zinc-400">Một yêu cầu có thể tạo nhiều lượt gửi theo kênh. “Nhà cung cấp đã nhận” chưa đồng nghĩa khách hàng đã mở hoặc đọc email.</p>
                {Number(data?.pending) > 0 && <p className="mt-3 inline-flex items-center gap-2 text-xs text-amber-200"><Clock3 className="h-4 w-4" /><StatusPill value="RETRY_SCHEDULED" /> {formatNumber(data.pending)} lượt đang chờ hoặc được hệ thống tự thử lại.</p>}
            </section>

            <div className="grid gap-5 xl:grid-cols-[1fr_0.72fr]">
                <section className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5 sm:p-6">
                    <p className="text-xs font-black uppercase tracking-widest text-zinc-500">Sức khỏe theo kênh</p>
                    <h2 className="mt-2 text-lg font-black text-white">Kênh nào đang gặp vấn đề?</h2>
                    {channels.length === 0 ? <p className="mt-5 text-sm text-zinc-500">Chưa có lượt gửi trong phạm vi đã chọn.</p> : <div className="mt-5 grid gap-3 sm:grid-cols-2">{channels.map(([channel, values]) => <article key={channel} className={`rounded-2xl border p-4 ${Number(values.failed) > 0 ? 'border-amber-400/20 bg-amber-400/5' : 'border-zinc-800 bg-zinc-950/60'}`}><div className="flex items-center justify-between gap-3"><p className="font-black text-white">{channelBusinessName(channel)}</p><StatusPill value={Number(values.failed) > 0 ? 'PERIOD_ERRORS' : 'HEALTHY'} /></div><p className="mt-3 text-xs text-zinc-300">{formatNumber(values.total)} lượt · {formatNumber(values.accepted)} được tiếp nhận · <span className={Number(values.failed) > 0 ? 'font-bold text-amber-200' : ''}>{formatNumber(values.failed)} thất bại</span></p>{Number(values.failed) > 0 && activeIncidents === 0 && <p className="mt-2 text-xs font-bold text-emerald-300">Không còn sự cố cần can thiệp</p>}{Number(values.pending) > 0 && <p className="mt-2 text-xs text-amber-300">{formatNumber(values.pending)} lượt đang chờ hoặc thử lại</p>}</article>)}</div>}
                </section>

                <section className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5 sm:p-6">
                    <div className="flex items-center gap-3"><div className={`rounded-2xl p-3 ${data?.templateRegistry?.available ? 'bg-emerald-400/10 text-emerald-300' : 'bg-red-400/10 text-red-300'}`}><GitBranch className="h-5 w-5" /></div><div><p className="text-xs font-black uppercase tracking-widest text-zinc-500">Nguồn nội dung</p><h2 className="mt-1 text-lg font-black text-white">Kho mẫu thông báo {data?.templateRegistry?.available ? 'đã đồng bộ' : 'đang gián đoạn'}</h2></div></div>
                    <p className="mt-4 text-sm leading-6 text-zinc-300">Phiên bản mẫu đang hoạt động được cập nhật {formatDateTime(data?.templateRegistry?.lastSyncedAt)}.</p>
                    <TechnicalDetails className="mt-5"><dl className="space-y-3"><RegistryRow label="Repository" value={data?.templateRegistry?.repository || '—'} /><RegistryRow label="Nhánh" value={data?.templateRegistry?.branch || 'main'} mono /><RegistryRow label="Remote HEAD" value={shortSha(data?.templateRegistry?.remoteHeadCommit)} mono /><RegistryRow label="Revision đang hoạt động" value={shortSha(data?.templateRegistry?.headCommit)} mono /></dl></TechnicalDetails>
                    <Link to="/admin/notification-coverage" className="mt-5 inline-flex items-center gap-2 text-xs font-black text-orange-300 hover:text-orange-200">Xem cấu hình và phạm vi <ArrowRight className="h-3.5 w-3.5" /></Link>
                </section>
            </div>
        </div>
    );
}

function FunnelCard({ stage, data, arrow = false }) {
    const { key, fallback, label, icon: Icon } = stage;
    const failed = key === 'failed' && Number(data?.failed) > 0;
    return <article className={`relative rounded-2xl border p-4 ${failed ? 'border-red-400/25 bg-red-400/5' : 'border-zinc-800 bg-zinc-950/60'}`}><div className="flex items-center justify-between"><Icon className={`h-4 w-4 ${failed ? 'text-red-300' : 'text-orange-300'}`} />{arrow && <ArrowRight className="hidden h-4 w-4 text-zinc-600 sm:block" />}</div><p className="mt-4 text-2xl font-black text-white">{formatNumber(data?.[key] ?? data?.[fallback])}</p><p className="mt-1 text-xs font-bold text-zinc-400">{label}</p></article>;
}

function OperationsConclusion({ activeIncidents, failed, blockedItems, hours }) {
    if (activeIncidents > 0) {
        const first = blockedItems[0];
        return <section className="rounded-3xl border border-red-400/30 bg-gradient-to-r from-red-500/10 to-orange-500/5 p-5"><div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between"><div className="flex items-start gap-4"><div className="rounded-2xl bg-red-400/10 p-3 text-red-300"><Siren className="h-6 w-6" /></div><div><p className="text-xs font-black uppercase tracking-widest text-red-300">{formatNumber(activeIncidents)} sự cố đang cần can thiệp</p><h2 className="mt-2 text-lg font-black text-white">{first ? `${notificationBusinessName(first.eventTypes?.[0], first.templateKey)} chưa sẵn sàng` : 'Có lượt gửi đã hết khả năng tự phục hồi'}</h2><p className="mt-1 text-sm leading-6 text-zinc-300">{first ? `${serviceBusinessName(first.sourceService)} đang bị ảnh hưởng. Hãy mở trung tâm xử lý để xem hành động đề xuất.` : 'Hệ thống không thể tự khôi phục các lượt gửi này.'}</p>{first && <TechnicalDetails className="mt-3"><p>{first.templateKey} chưa có template đang hoạt động · {first.sourceService} · {first.eventTypes?.join(', ')}</p></TechnicalDetails>}</div></div><Link to="/admin/notification-attention" className="inline-flex items-center justify-center gap-2 rounded-xl bg-red-500 px-4 py-2.5 text-xs font-black text-white hover:bg-red-400">Xem và xử lý <ArrowRight className="h-4 w-4" /></Link></div></section>;
    }
    if (failed > 0) return <section className="rounded-2xl border border-amber-400/20 bg-amber-400/5 px-5 py-4"><p className="text-sm font-black text-amber-100">Có {formatNumber(failed)} lượt gửi thất bại trong {hours === 24 ? '24 giờ' : hours === 168 ? '7 ngày' : '30 ngày'}, nhưng không có sự cố tồn đọng.</p><p className="mt-1 text-xs leading-5 text-zinc-300">Các lượt trên là dữ liệu lịch sử; hệ thống không còn yêu cầu admin can thiệp.</p></section>;
    return <section className="rounded-2xl border border-emerald-400/20 bg-emerald-400/5 px-5 py-4"><p className="text-sm font-black text-emerald-100">Hệ thống đang vận hành bình thường.</p><p className="mt-1 text-xs text-zinc-300">Không có lượt gửi thất bại hoặc sự cố cần can thiệp trong phạm vi đã chọn.</p></section>;
}

function HealthCard({ label, value, badge, tone, detail }) {
    const badgeTone = { success: 'border-emerald-400/20 bg-emerald-400/10 text-emerald-300', warning: 'border-amber-400/20 bg-amber-400/10 text-amber-300', danger: 'border-red-400/20 bg-red-400/10 text-red-300' }[tone] || 'border-zinc-700 bg-zinc-800 text-zinc-300';
    return <article className="rounded-2xl border border-zinc-800 bg-zinc-900/55 p-4"><div className="flex items-center justify-between gap-2"><p className="text-xs font-bold text-zinc-400">{label}</p><span className={`inline-flex rounded-full border px-2.5 py-1 text-[10px] font-black uppercase tracking-wider ${badgeTone}`}>{badge}</span></div><p className="mt-3 text-sm font-black text-white">{value}</p><p className="mt-1 text-xs text-zinc-400">{detail}</p></article>;
}

function RegistryRow({ label, value, mono }) {
    return <div className="flex items-start justify-between gap-4"><dt>{label}</dt><dd className={`max-w-[65%] break-all text-right text-zinc-200 ${mono ? 'font-mono text-orange-300' : 'font-bold'}`}>{value}</dd></div>;
}
