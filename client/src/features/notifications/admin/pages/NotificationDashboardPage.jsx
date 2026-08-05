import { useCallback, useEffect, useMemo, useState } from 'react';
import { Activity, AlertOctagon, ArrowRight, BellRing, CheckCheck, Clock3, GitBranch, RefreshCw, Send } from 'lucide-react';
import { Link } from 'react-router-dom';
import { notificationAdminService } from '../services/notificationAdminService';
import { ErrorState, LoadingState, PageHeading, StatusPill, formatNumber, formatPercent, shortSha } from '../components/NotificationAdminUi';

const cards = [
    { key: 'totalRequests', label: 'Yêu cầu thông báo', icon: BellRing, tone: 'text-white' },
    { key: 'totalDeliveries', label: 'Lượt gửi', icon: Send, tone: 'text-sky-300' },
    { key: 'delivered', label: 'Đã gửi thành công', icon: CheckCheck, tone: 'text-emerald-300' },
    { key: 'failed', label: 'Gửi thất bại', icon: AlertOctagon, tone: 'text-red-300' },
    { key: 'pending', label: 'Đang chờ / gửi lại', icon: Clock3, tone: 'text-amber-300' },
    { key: 'deadLetters', label: 'Cần xử lý thủ công', icon: Activity, tone: 'text-violet-300' },
];

export default function NotificationDashboardPage() {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const load = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            setData(await notificationAdminService.dashboard());
        } catch (requestError) {
            setError(requestError?.message || 'Dịch vụ thông báo chưa trả về số liệu vận hành.');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        const timer = setTimeout(load, 0);
        return () => clearTimeout(timer);
    }, [load]);

    const statusRows = useMemo(() => Object.entries(data?.deliveryStatuses || {})
        .sort(([, left], [, right]) => Number(right) - Number(left)), [data]);
    const maximum = Math.max(1, ...statusRows.map(([, value]) => Number(value)));

    if (loading) return <LoadingState label="Đang tải số liệu gửi thông báo…" />;
    if (error) return <ErrorState message={error} onRetry={load} />;

    return (
        <div className="mx-auto max-w-[1500px] space-y-6 pb-10">
            <PageHeading
                eyebrow="Vận hành thông báo"
                title="Trung tâm điều phối thông báo"
                description="Theo dõi yêu cầu, lượt gửi và lỗi xử lý. Nội dung mẫu được quản lý trong kho Git riêng."
                actions={
                    <>
                        <button type="button" onClick={load} className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-4 py-2.5 text-xs font-bold text-zinc-200 hover:border-zinc-500">
                            <RefreshCw className="h-4 w-4" /> Làm mới
                        </button>
                        <Link to="/admin/notification-operations" className="inline-flex items-center gap-2 rounded-xl bg-orange-500 px-4 py-2.5 text-xs font-black text-white hover:bg-orange-400">
                            Xem nhật ký gửi <ArrowRight className="h-4 w-4" />
                        </Link>
                    </>
                }
            />

            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
                {cards.map(({ key, label, icon: Icon, tone }) => (
                    <article key={key} className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-4 shadow-xl shadow-black/10">
                        <div className="flex items-center justify-between">
                            <Icon className={`h-4 w-4 ${tone}`} />
                            <span className="h-1.5 w-1.5 rounded-full bg-zinc-700" />
                        </div>
                        <p className="mt-5 text-2xl font-black text-white">{formatNumber(data?.[key])}</p>
                        <p className="mt-1 text-[11px] font-bold uppercase tracking-wider text-zinc-500">{label}</p>
                    </article>
                ))}
            </div>

            <div className="grid gap-5 xl:grid-cols-[1.35fr_0.65fr]">
                <section className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5 sm:p-6">
                    <div className="flex items-start justify-between gap-4">
                        <div>
                            <p className="text-xs font-black uppercase tracking-widest text-zinc-500">Trạng thái gửi</p>
                            <h2 className="mt-2 text-lg font-black text-white">Phân bố tiến trình</h2>
                        </div>
                        <div className="rounded-2xl border border-emerald-400/20 bg-emerald-400/5 px-4 py-3 text-right">
                            <p className="text-[10px] font-bold uppercase tracking-wider text-emerald-300">Tỷ lệ gửi thành công</p>
                            <p className="mt-1 text-2xl font-black text-white">{formatPercent(data?.deliveryRate)}</p>
                        </div>
                    </div>
                    <div className="mt-7 space-y-4">
                        {statusRows.map(([status, value]) => (
                            <div key={status} className="grid grid-cols-[130px_1fr_55px] items-center gap-3">
                                <StatusPill value={status} />
                                <div className="h-2 overflow-hidden rounded-full bg-zinc-800">
                                    <div className="h-full rounded-full bg-gradient-to-r from-orange-500 to-amber-300" style={{ width: `${Math.max(2, Number(value) / maximum * 100)}%` }} />
                                </div>
                                <span className="text-right font-mono text-xs font-bold text-zinc-300">{formatNumber(value)}</span>
                            </div>
                        ))}
                    </div>
                </section>

                <section className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5 sm:p-6">
                    <div className="flex items-center gap-3">
                        <div className={`rounded-2xl p-3 ${data?.templateRegistry?.available ? 'bg-emerald-400/10 text-emerald-300' : 'bg-red-400/10 text-red-300'}`}>
                            <GitBranch className="h-5 w-5" />
                        </div>
                        <div>
                            <p className="text-xs font-black uppercase tracking-widest text-zinc-500">Nguồn mẫu thông báo</p>
                            <h2 className="mt-1 text-lg font-black text-white">Kho Git nội bộ</h2>
                        </div>
                    </div>
                    <dl className="mt-6 space-y-4 text-sm">
                        <div className="flex items-center justify-between gap-4 border-b border-zinc-800 pb-4">
                            <dt className="text-zinc-500">Trạng thái</dt>
                            <dd><StatusPill value={data?.templateRegistry?.available ? 'AVAILABLE' : 'UNAVAILABLE'} /></dd>
                        </div>
                        <div className="flex items-center justify-between gap-4 border-b border-zinc-800 pb-4">
                            <dt className="text-zinc-500">Công cụ quản lý</dt>
                            <dd className="font-bold text-zinc-200">{data?.templateRegistry?.provider || 'JGit'}</dd>
                        </div>
                        <div className="flex items-center justify-between gap-4 border-b border-zinc-800 pb-4">
                            <dt className="text-zinc-500">Nhánh được bảo vệ</dt>
                            <dd className="font-mono text-xs text-orange-300">{data?.templateRegistry?.branch || 'main'}</dd>
                        </div>
                        <div className="flex items-center justify-between gap-4">
                            <dt className="text-zinc-500">HEAD</dt>
                            <dd className="font-mono text-xs text-zinc-300">{shortSha(data?.templateRegistry?.headCommit)}</dd>
                        </div>
                    </dl>
                    {!data?.templateRegistry?.available && (
                        <p className="mt-5 rounded-xl border border-red-400/20 bg-red-400/5 p-3 text-xs leading-5 text-red-200">
                            {data?.templateRegistry?.message || 'Kho mẫu thông báo hiện không khả dụng.'}
                        </p>
                    )}
                </section>
            </div>
        </div>
    );
}
