import { useCallback, useEffect, useMemo, useState } from 'react';
import {
    AlertOctagon, ArrowRight, BellRing, CheckCheck, Clock3,
    GitBranch, RefreshCw, Send, ShieldCheck, Siren,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { notificationAdminService } from '../services/notificationAdminService';
import {
    ErrorState, LoadingState, PageHeading, StatusPill,
    formatDateTime, formatNumber, formatPercent, shortSha,
} from '../components/NotificationAdminUi';

const cards = [
    { key: 'totalRequests', label: 'Yêu cầu thông báo', icon: BellRing, tone: 'text-white' },
    { key: 'totalDeliveries', label: 'Lượt gửi theo kênh', icon: Send, tone: 'text-sky-300' },
    { key: 'accepted', fallback: 'delivered', label: 'Được chấp nhận gửi', icon: CheckCheck, tone: 'text-emerald-300' },
    { key: 'confirmed', label: 'Có xác nhận giao', icon: ShieldCheck, tone: 'text-teal-300' },
    { key: 'failed', label: 'Thất bại', icon: AlertOctagon, tone: 'text-red-300' },
    { key: 'pending', label: 'Đang chờ / thử lại', icon: Clock3, tone: 'text-amber-300' },
];

const barTones = {
    SENT: 'bg-emerald-400', DELIVERED: 'bg-teal-400', FAILED: 'bg-red-400',
    DEAD_LETTERED: 'bg-rose-500', RETRY_SCHEDULED: 'bg-amber-400',
    PROCESSING: 'bg-sky-400', PENDING: 'bg-sky-500', CANCELLED: 'bg-zinc-500',
    SUPPRESSED: 'bg-zinc-600',
};

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

    const statusRows = useMemo(() => Object.entries(data?.deliveryStatuses || {})
        .sort(([, left], [, right]) => Number(right) - Number(left)), [data]);
    const maximum = Math.max(1, ...statusRows.map(([, value]) => Number(value)));
    const blockedItems = (data?.coverage?.items || []).filter(item => item.readiness === 'BLOCKED');

    if (loading) return <LoadingState label="Đang tải số liệu gửi thông báo…" />;
    if (error) return <ErrorState message={error} onRetry={load} />;

    return (
        <div className="mx-auto max-w-[1500px] space-y-6 pb-10">
            <PageHeading
                eyebrow="Vận hành thông báo"
                title="Trung tâm điều phối thông báo"
                description="Ưu tiên cảnh báo cần xử lý, sức khỏe kênh gửi và số liệu vận hành thật trong phạm vi đã chọn."
                actions={
                    <>
                        <button type="button" onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-4 py-2.5 text-xs font-bold text-zinc-200 hover:border-zinc-500">
                            <RefreshCw className="h-4 w-4" /> Làm mới
                        </button>
                        <Link to="/admin/notification-operations" className="inline-flex items-center gap-2 rounded-xl bg-orange-500 px-4 py-2.5 text-xs font-black text-white hover:bg-orange-400">
                            Xem lịch sử gửi <ArrowRight className="h-4 w-4" />
                        </Link>
                    </>
                }
            />

            <section className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/60 p-3">
                <div className="flex flex-wrap gap-2" role="group" aria-label="Phạm vi số liệu">
                    {[24, 168, 720].map(hours => (
                        <button key={hours} type="button" onClick={() => setFilters(current => ({ ...current, hours }))} className={`rounded-xl px-3 py-2 text-xs font-black ${filters.hours === hours ? 'bg-orange-500 text-white' : 'bg-zinc-950 text-zinc-400 hover:text-white'}`}>
                            {hours === 24 ? '24 giờ' : hours === 168 ? '7 ngày' : '30 ngày'}
                        </button>
                    ))}
                </div>
                <label className={`inline-flex cursor-pointer items-center gap-3 rounded-xl border px-3 py-2 text-xs font-bold transition ${filters.includeTest ? 'border-violet-400/30 bg-violet-400/10 text-violet-200' : 'border-zinc-800 bg-zinc-950 text-zinc-400'}`}>
                    <span>Bao gồm lượt gửi thử</span>
                    <input type="checkbox" role="switch" aria-label="Bao gồm lượt gửi thử" checked={filters.includeTest} onChange={event => setFilters(current => ({ ...current, includeTest: event.target.checked }))} className="peer sr-only" />
                    <span aria-hidden="true" className="relative h-5 w-9 rounded-full bg-zinc-700 transition peer-checked:bg-violet-500 after:absolute after:left-0.5 after:top-0.5 after:h-4 after:w-4 after:rounded-full after:bg-white after:transition-transform peer-checked:after:translate-x-4" />
                </label>
            </section>

            {blockedItems.length > 0 && (
                <section className="rounded-3xl border border-red-400/30 bg-gradient-to-r from-red-500/10 to-orange-500/5 p-5">
                    <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                        <div className="flex items-start gap-4">
                            <div className="rounded-2xl bg-red-400/10 p-3 text-red-300"><Siren className="h-6 w-6" /></div>
                            <div>
                                <p className="text-xs font-black uppercase tracking-widest text-red-300">{blockedItems.length} lỗi cấu hình đang ảnh hưởng luồng gửi</p>
                                <h2 className="mt-2 text-lg font-black text-white">{blockedItems[0].templateKey} chưa có template đang hoạt động</h2>
                                <p className="mt-1 text-sm leading-6 text-zinc-400">{blockedItems[0].sourceService} đang phát sự kiện {blockedItems[0].eventTypes?.join(', ')} cho {blockedItems[0].channels?.join(', ')} · {blockedItems[0].locale}.</p>
                            </div>
                        </div>
                        <Link to="/admin/notification-attention" className="inline-flex items-center justify-center gap-2 rounded-xl bg-red-500 px-4 py-2.5 text-xs font-black text-white hover:bg-red-400">
                            Xem và xử lý <ArrowRight className="h-4 w-4" />
                        </Link>
                    </div>
                </section>
            )}

            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                <HealthCard label="Dịch vụ thông báo" value="Hoạt động" badge="Hoạt động" tone="success" detail={`Cập nhật ${formatDateTime(data?.generatedAt)}`} />
                <HealthCard label="Nguồn mẫu" value={data?.templateRegistry?.available ? 'Đã đồng bộ' : 'Không khả dụng'} badge={data?.templateRegistry?.available ? 'Đã đồng bộ' : 'Không khả dụng'} tone={data?.templateRegistry?.available ? 'success' : 'danger'} detail={data?.templateRegistry?.repository || 'Chưa xác định repository'} />
                <HealthCard label="Hàng đợi" value={`${formatNumber(data?.pending)} đang chờ`} badge={Number(data?.pending) > 0 ? 'Cần theo dõi' : 'Bình thường'} tone={Number(data?.pending) > 0 ? 'warning' : 'info'} detail="Bao gồm lượt chờ và đang thử lại" />
                <HealthCard label="Độ phủ yêu cầu tích hợp" value={`${formatNumber(data?.coverage?.readyRequirements)}/${formatNumber(data?.coverage?.totalRequirements)}`} badge={Number(data?.coverage?.blockedRequirements) > 0 ? `Thiếu ${formatNumber(data?.coverage?.blockedRequirements)} yêu cầu` : 'Đủ yêu cầu'} tone={Number(data?.coverage?.blockedRequirements) > 0 ? 'danger' : 'success'} detail={`${formatNumber(data?.coverage?.blockedRequirements)} yêu cầu bị chặn`} />
            </div>

            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
                {cards.map(({ key, fallback, label, icon: Icon, tone }) => (
                    <article key={key} className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-4 shadow-xl shadow-black/10">
                        <Icon className={`h-4 w-4 ${tone}`} />
                        <p className="mt-5 text-2xl font-black text-white">{formatNumber(data?.[key] ?? data?.[fallback])}</p>
                        <p className="mt-1 text-[11px] font-bold uppercase tracking-wider text-zinc-500">{label}</p>
                    </article>
                ))}
            </div>
            <p className="rounded-xl border border-zinc-800 bg-zinc-900/30 px-4 py-3 text-xs leading-5 text-zinc-500">
                Một yêu cầu thông báo có thể tạo nhiều lượt gửi theo kênh và người nhận. Email ở trạng thái “được chấp nhận gửi” chưa đồng nghĩa người dùng đã đọc hoặc nhận vào hộp thư.
            </p>

            <div className="grid gap-5 xl:grid-cols-[1.35fr_0.65fr]">
                <section className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5 sm:p-6">
                    <div className="flex items-start justify-between gap-4">
                        <div><p className="text-xs font-black uppercase tracking-widest text-zinc-500">Trạng thái lượt gửi</p><h2 className="mt-2 text-lg font-black text-white">Phân bố tiến trình</h2></div>
                        <div className="rounded-2xl border border-emerald-400/20 bg-emerald-400/5 px-4 py-3 text-right">
                            <p className="text-[10px] font-bold uppercase tracking-wider text-emerald-300">Tỷ lệ được chấp nhận</p>
                            <p className="mt-1 text-2xl font-black text-white">{formatPercent(data?.deliveryRate)}</p>
                        </div>
                    </div>
                    <div className="mt-7 space-y-4">
                        {statusRows.map(([status, value]) => (
                            <div key={status} className="grid grid-cols-[155px_1fr_55px] items-center gap-3">
                                <StatusPill value={status} />
                                <div className="h-2 overflow-hidden rounded-full bg-zinc-800">
                                    <div className={`h-full rounded-full ${barTones[status] || 'bg-zinc-500'}`} style={{ width: Number(value) === 0 ? '0%' : `${Number(value) / maximum * 100}%` }} />
                                </div>
                                <span className="text-right font-mono text-xs font-bold text-zinc-300">{formatNumber(value)}</span>
                            </div>
                        ))}
                    </div>
                </section>

                <section className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5 sm:p-6">
                    <div className="flex items-center gap-3">
                        <div className={`rounded-2xl p-3 ${data?.templateRegistry?.available ? 'bg-emerald-400/10 text-emerald-300' : 'bg-red-400/10 text-red-300'}`}><GitBranch className="h-5 w-5" /></div>
                        <div><p className="text-xs font-black uppercase tracking-widest text-zinc-500">Nguồn mẫu thông báo</p><h2 className="mt-1 text-lg font-black text-white">Cấu hình runtime hiệu lực</h2></div>
                    </div>
                    <dl className="mt-6 space-y-4 text-sm">
                        <RegistryRow label="Repository đang dùng" value={data?.templateRegistry?.repository || '—'} />
                        <RegistryRow label="Nhánh theo dõi" value={data?.templateRegistry?.branch || 'main'} mono />
                        <RegistryRow label="Đồng bộ gần nhất" value={formatDateTime(data?.templateRegistry?.lastSyncedAt)} />
                        <RegistryRow label="Remote HEAD" value={shortSha(data?.templateRegistry?.remoteHeadCommit)} mono />
                        <RegistryRow label="Revision đang hoạt động" value={shortSha(data?.templateRegistry?.headCommit)} mono last />
                    </dl>
                    <Link to="/admin/notification-coverage" className="mt-5 inline-flex items-center gap-2 text-xs font-black text-orange-300 hover:text-orange-200">Xem cấu hình và độ phủ <ArrowRight className="h-3.5 w-3.5" /></Link>
                </section>
            </div>
        </div>
    );
}

function HealthCard({ label, value, badge, tone, detail }) {
    const badgeTone = {
        success: 'border-emerald-400/20 bg-emerald-400/10 text-emerald-300',
        info: 'border-sky-400/20 bg-sky-400/10 text-sky-300',
        warning: 'border-amber-400/20 bg-amber-400/10 text-amber-300',
        danger: 'border-red-400/20 bg-red-400/10 text-red-300',
    }[tone] || 'border-zinc-700 bg-zinc-800 text-zinc-300';
    return <article className="rounded-2xl border border-zinc-800 bg-zinc-900/55 p-4"><div className="flex items-center justify-between gap-2"><p className="text-[10px] font-black uppercase tracking-wider text-zinc-500">{label}</p><span className={`inline-flex rounded-full border px-2.5 py-1 text-[10px] font-black uppercase tracking-wider ${badgeTone}`}>{badge}</span></div><p className="mt-3 text-sm font-black text-white">{value}</p><p className="mt-1 truncate text-[10px] text-zinc-500">{detail}</p></article>;
}

function RegistryRow({ label, value, mono, last }) {
    return <div className={`flex items-center justify-between gap-4 ${last ? '' : 'border-b border-zinc-800 pb-4'}`}><dt className="text-zinc-500">{label}</dt><dd className={`max-w-[60%] truncate text-right text-zinc-200 ${mono ? 'font-mono text-xs text-orange-300' : 'font-bold'}`}>{value}</dd></div>;
}
