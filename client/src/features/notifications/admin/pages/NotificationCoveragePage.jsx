import { useCallback, useEffect, useMemo, useState } from 'react';
import { AlertTriangle, ArrowRight, CheckCircle2, GitBranch, RefreshCw, ShieldAlert } from 'lucide-react';
import { Link } from 'react-router-dom';
import { notificationAdminService } from '../services/notificationAdminService';
import { ErrorState, LoadingState, PageHeading, StatusPill, TechnicalDetails, formatDateTime, formatNumber, shortSha } from '../components/NotificationAdminUi';
import EmailProviderConfigurationPanel from '../components/EmailProviderConfigurationPanel';
import { channelBusinessName, localeBusinessName, notificationBusinessName, serviceBusinessName } from '../utils/notificationBusinessPresentation';

export default function NotificationCoveragePage() {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const load = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            const [dashboard, templates, emailProvider] = await Promise.all([
                notificationAdminService.dashboard({ hours: 24, includeTest: false }),
                notificationAdminService.templates({ archived: false }),
                notificationAdminService.emailProvider(),
            ]);
            setData({ ...dashboard, templates: templates || [], emailProvider });
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
    const declaredTemplateKeys = new Set((coverage.items || []).map(item => item.templateKey));
    const activeTemplateKeys = new Set((data?.templates || []).map(item => item.templateKey));
    const unlinkedTemplateCount = [...activeTemplateKeys]
        .filter(templateKey => !declaredTemplateKeys.has(templateKey)).length;
    return (
        <div className="mx-auto max-w-[1500px] space-y-6 pb-10">
            <PageHeading
                eyebrow="Kết nối và phạm vi"
                title="Cấu hình và độ phủ"
                description="Kiểm tra tài khoản gửi email và bảo đảm mọi luồng thông báo quan trọng đều có mẫu hoạt động."
                actions={<button type="button" onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-4 py-2.5 text-xs font-black text-white"><RefreshCw className="h-4 w-4" /> Kiểm tra lại</button>}
            />

            <EmailProviderConfigurationPanel
                key={`${data?.emailProvider?.source}-${data?.emailProvider?.updatedAt || 'initial'}-${data?.emailProvider?.senderEmail || ''}`}
                configuration={data?.emailProvider}
                onUpdated={emailProvider => setData(current => ({ ...current, emailProvider }))}
            />

            <div className="grid gap-4 lg:grid-cols-[0.95fr_1.05fr]">
                <section className="order-2 rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5">
                    <div className="flex items-center gap-3"><div className="rounded-2xl bg-orange-400/10 p-3 text-orange-300"><GitBranch className="h-5 w-5" /></div><div><p className="text-xs font-bold text-zinc-500">Kho mẫu thông báo</p><h2 className="mt-1 text-lg font-black text-white">{registry.available ? 'Đã đồng bộ và sẵn sàng' : 'Đang gián đoạn'}</h2></div></div>
                    <p className="mt-4 text-sm leading-6 text-zinc-300">Phiên bản đang hoạt động được cập nhật {formatDateTime(registry.lastSyncedAt)}.</p>
                    <TechnicalDetails className="mt-5"><dl className="space-y-4 text-sm"><Row label="Repository" value={registry.repository || '—'} /><Row label="Remote" value={registry.remoteUri || '—'} /><Row label="Nhánh theo dõi" value={registry.branch || 'main'} mono /><Row label="Remote HEAD" value={shortSha(registry.remoteHeadCommit)} mono /><Row label="Revision đang hoạt động" value={shortSha(registry.headCommit)} mono last /></dl></TechnicalDetails>
                </section>

                <section className="order-1 rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5">
                    <div className="flex items-start justify-between gap-4"><div><p className="text-xs font-bold text-zinc-500">Phạm vi thông báo</p><h2 className="mt-1 text-lg font-black text-white">{formatNumber(coverage.readyRequirements)}/{formatNumber(coverage.totalRequirements)} luồng quan trọng đã có mẫu hoạt động</h2></div><StatusPill value={Number(coverage.blockedRequirements) > 0 ? 'BLOCKED' : 'READY'} /></div>
                    <div className="mt-6 grid gap-3 sm:grid-cols-3">
                        <Metric label="Luồng quan trọng" value={coverage.totalRequirements} tone="text-sky-300" />
                        <Metric label="Sẵn sàng" value={coverage.readyRequirements} tone="text-emerald-300" />
                        <Metric label="Bị chặn" value={coverage.blockedRequirements} tone="text-red-300" />
                    </div>
                    <div className="mt-4 rounded-2xl border border-zinc-800 bg-zinc-950/50 p-4"><p className="text-sm font-bold text-zinc-300">{formatNumber(unlinkedTemplateCount)} mẫu khác trong kho, hiện không được sử dụng</p><p className="mt-1 text-xs text-zinc-500">Không ảnh hưởng đến hoạt động gửi và không được tính là lỗi.</p></div>
                </section>
            </div>

            <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/50">
                <div className="grid gap-3 border-b border-zinc-800 px-5 py-4 text-[10px] font-black uppercase tracking-widest text-zinc-500 lg:grid-cols-[1fr_0.8fr_0.8fr_0.55fr_1.2fr]">
                    <span>Luồng thông báo</span><span>Nhóm nghiệp vụ</span><span>Kênh sử dụng</span><span>Trạng thái</span><span>Kết quả kiểm tra</span>
                </div>
                {items.map(item => (
                    <article key={`${item.templateKey}-${item.locale}`} className={`grid gap-4 border-b border-zinc-800/70 px-5 py-5 last:border-0 lg:grid-cols-[1fr_0.8fr_0.8fr_0.55fr_1.2fr] lg:items-center ${item.readiness === 'BLOCKED' ? 'bg-red-500/[0.035]' : ''}`}>
                        <div><p className="text-sm font-black text-white">{notificationBusinessName(item.eventTypes?.[0], item.templateKey)}</p><TechnicalDetails className="mt-2"><p className="font-mono text-orange-300">{item.templateKey}</p><p className="mt-1">{item.eventTypes?.join(', ')}</p></TechnicalDetails></div>
                        <div><p className="text-xs font-bold text-zinc-300">{serviceBusinessName(item.sourceService)}</p></div>
                        <div><p className="text-xs font-bold text-zinc-300">{item.channels?.map(channelBusinessName).join(' · ')}</p><p className="mt-1 text-xs text-zinc-500">{localeBusinessName(item.locale)}</p></div>
                        <StatusPill value={item.readiness} />
                        <div className="flex items-start justify-between gap-3">
                            {item.readiness === 'READY' ? <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-300" /> : <ShieldAlert className="mt-0.5 h-4 w-4 shrink-0 text-red-300" />}
                            <div className="min-w-0 flex-1"><p className="text-xs leading-5 text-zinc-300">{coverageMessage(item)}</p>{item.readiness === 'BLOCKED' && <div className="mt-3 flex flex-wrap gap-3"><Link to={`/admin/notification-templates?contract=${item.templateKey}`} className="text-xs font-black text-red-200 hover:text-white">Tạo mẫu còn thiếu</Link><Link to="/admin/notification-attention" className="text-xs font-black text-orange-300 hover:text-white">Mở trung tâm xử lý</Link></div>}{item.activeRevision && <TechnicalDetails className="mt-2"><p className="font-mono">Revision {shortSha(item.activeRevision)}</p></TechnicalDetails>}</div>
                        </div>
                    </article>
                ))}
            </section>

            {Number(coverage.blockedRequirements) > 0 && (
                <div className="flex flex-col gap-3 rounded-2xl border border-amber-400/20 bg-amber-400/5 p-4 sm:flex-row sm:items-center sm:justify-between"><div className="flex items-start gap-3"><AlertTriangle className="mt-0.5 h-5 w-5 text-amber-300" /><p className="text-sm leading-6 text-amber-100">Hệ thống vẫn giữ revision đang chạy, nhưng yêu cầu tích hợp bị thiếu sẽ tạo lượt gửi lỗi và xuất hiện trong “Cần xử lý”.</p></div><Link to="/admin/notification-attention" className="inline-flex shrink-0 items-center gap-2 text-xs font-black text-orange-300">Mở cần xử lý <ArrowRight className="h-4 w-4" /></Link></div>
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
const coverageMessage = item => item.readiness === 'READY'
    ? 'Yêu cầu đã có template đang hoạt động.'
    : String(item.message || 'Yêu cầu chưa được đáp ứng.')
        .replace('Contract', 'Yêu cầu tích hợp')
        .replace('template active', 'template đang hoạt động');
