import { useCallback, useEffect, useMemo, useState } from 'react';
import { AlertTriangle, ArrowRight, CheckCircle2, GitBranch, RefreshCw, ShieldAlert } from 'lucide-react';
import { Link } from 'react-router-dom';
import { notificationAdminService } from '../services/notificationAdminService';
import { ErrorState, LoadingState, PageHeading, StatusPill, formatDateTime, formatNumber, shortSha } from '../components/NotificationAdminUi';

export default function NotificationCoveragePage() {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const load = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            setData(await notificationAdminService.dashboard({ hours: 24, includeTest: false }));
        } catch (requestError) {
            setError(requestError?.message || 'Không thể kiểm tra cấu hình và độ phủ template.');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        const timer = setTimeout(load, 0);
        return () => clearTimeout(timer);
    }, [load]);

    const items = useMemo(() => [...(data?.coverage?.items || [])]
        .sort((left, right) => {
            const order = { BLOCKED: 0, WARNING: 1, READY: 2 };
            return order[left.readiness] - order[right.readiness]
                || left.templateKey.localeCompare(right.templateKey);
        }), [data]);

    if (loading) return <LoadingState label="Đang đối chiếu contract với nguồn template…" />;
    if (error) return <ErrorState message={error} onRetry={load} />;

    const registry = data?.templateRegistry || {};
    const coverage = data?.coverage || {};
    return (
        <div className="mx-auto max-w-[1500px] space-y-6 pb-10">
            <PageHeading
                eyebrow="Technical admin"
                title="Cấu hình và độ phủ"
                description="Đối chiếu cấu hình runtime hiệu lực với các template mà auth, booking và promotion đang yêu cầu."
                actions={<button type="button" onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-4 py-2.5 text-xs font-black text-white"><RefreshCw className="h-4 w-4" /> Kiểm tra lại</button>}
            />

            <div className="grid gap-4 lg:grid-cols-[0.8fr_1.2fr]">
                <section className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5">
                    <div className="flex items-center gap-3"><div className="rounded-2xl bg-orange-400/10 p-3 text-orange-300"><GitBranch className="h-5 w-5" /></div><div><p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Nguồn mẫu thông báo</p><h2 className="mt-1 text-lg font-black text-white">{registry.repository || 'Chưa xác định'}</h2></div></div>
                    <dl className="mt-6 space-y-4 text-sm">
                        <Row label="Trạng thái" value={<StatusPill value={registry.available ? 'READY' : 'BLOCKED'} />} />
                        <Row label="Remote hiệu lực" value={registry.remoteUri || '—'} />
                        <Row label="Nhánh theo dõi" value={registry.branch || 'main'} mono />
                        <Row label="Remote HEAD" value={shortSha(registry.remoteHeadCommit)} mono />
                        <Row label="Revision đang hoạt động" value={shortSha(registry.headCommit)} mono />
                        <Row label="Đồng bộ gần nhất" value={formatDateTime(registry.lastSyncedAt)} last />
                    </dl>
                </section>

                <section className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5">
                    <div className="flex items-start justify-between gap-4"><div><p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Độ phủ producer contract</p><h2 className="mt-1 text-lg font-black text-white">{formatNumber(coverage.readyRequirements)}/{formatNumber(coverage.totalRequirements)} yêu cầu sẵn sàng</h2></div><StatusPill value={Number(coverage.blockedRequirements) > 0 ? 'BLOCKED' : 'READY'} /></div>
                    <div className="mt-6 grid gap-3 sm:grid-cols-3">
                        <Metric label="Sẵn sàng" value={coverage.readyRequirements} tone="text-emerald-300" />
                        <Metric label="Cảnh báo" value={coverage.warningRequirements} tone="text-amber-300" />
                        <Metric label="Bị chặn" value={coverage.blockedRequirements} tone="text-red-300" />
                    </div>
                    <p className="mt-4 text-xs leading-5 text-zinc-500">Coverage được tính từ contract nằm trong notification-service và tập template active của revision đang chạy; file trong <code>_archive</code> không được tính.</p>
                </section>
            </div>

            <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/50">
                <div className="grid gap-3 border-b border-zinc-800 px-5 py-4 text-[10px] font-black uppercase tracking-widest text-zinc-500 lg:grid-cols-[1fr_0.8fr_0.8fr_0.55fr_1.2fr]">
                    <span>Template contract</span><span>Dịch vụ / sự kiện</span><span>Kênh / locale</span><span>Readiness</span><span>Kết quả kiểm tra</span>
                </div>
                {items.map(item => (
                    <article key={`${item.templateKey}-${item.locale}`} className={`grid gap-4 border-b border-zinc-800/70 px-5 py-5 last:border-0 lg:grid-cols-[1fr_0.8fr_0.8fr_0.55fr_1.2fr] lg:items-center ${item.readiness === 'BLOCKED' ? 'bg-red-500/[0.035]' : ''}`}>
                        <div><p className="text-sm font-black text-white">{item.displayName}</p><p className="mt-1 font-mono text-[11px] text-orange-300">{item.templateKey}</p></div>
                        <div><p className="text-xs font-bold text-zinc-300">{item.sourceService}</p><p className="mt-1 text-[10px] text-zinc-600">{item.eventTypes?.join(', ')}</p></div>
                        <div><p className="text-xs font-bold text-zinc-300">{item.channels?.join(' · ')}</p><p className="mt-1 font-mono text-[10px] text-zinc-600">{item.locale}</p></div>
                        <StatusPill value={item.readiness} />
                        <div className="flex items-start gap-3">
                            {item.readiness === 'READY' ? <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-300" /> : <ShieldAlert className="mt-0.5 h-4 w-4 shrink-0 text-red-300" />}
                            <div><p className="text-xs leading-5 text-zinc-300">{item.message}</p>{item.readiness === 'BLOCKED' && <p className="mt-2 text-[10px] font-bold text-red-200">Khuyến nghị: khôi phục template active hoặc cập nhật contract producer có chủ đích.</p>}{item.activeRevision && <p className="mt-1 font-mono text-[10px] text-zinc-600">Template revision {shortSha(item.activeRevision)}</p>}</div>
                        </div>
                    </article>
                ))}
            </section>

            {Number(coverage.blockedRequirements) > 0 && (
                <div className="flex flex-col gap-3 rounded-2xl border border-amber-400/20 bg-amber-400/5 p-4 sm:flex-row sm:items-center sm:justify-between"><div className="flex items-start gap-3"><AlertTriangle className="mt-0.5 h-5 w-5 text-amber-300" /><p className="text-sm leading-6 text-amber-100">Hệ thống vẫn giữ revision đang chạy, nhưng các contract bị thiếu sẽ tạo delivery lỗi và xuất hiện trong “Cần xử lý”.</p></div><Link to="/admin/notification-operations?tab=dead-letters" className="inline-flex shrink-0 items-center gap-2 text-xs font-black text-orange-300">Mở cần xử lý <ArrowRight className="h-4 w-4" /></Link></div>
            )}
        </div>
    );
}

function Row({ label, value, mono, last }) {
    return <div className={`flex items-start justify-between gap-5 ${last ? '' : 'border-b border-zinc-800 pb-4'}`}><dt className="shrink-0 text-zinc-500">{label}</dt><dd className={`max-w-[70%] break-all text-right text-xs text-zinc-300 ${mono ? 'font-mono text-orange-300' : 'font-bold'}`}>{value}</dd></div>;
}
function Metric({ label, value, tone }) {
    return <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4"><p className={`text-2xl font-black ${tone}`}>{formatNumber(value)}</p><p className="mt-1 text-[10px] font-black uppercase tracking-wider text-zinc-600">{label}</p></div>;
}
