import { useCallback, useEffect, useMemo, useState } from 'react';
import { AlertTriangle, Archive, ArrowRight, FilePlus2, GitCommit, Search, X } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { notificationAdminService } from '../services/notificationAdminService';
import { EmptyState, ErrorState, LoadingState, PageHeading, StatusPill, shortSha } from '../components/NotificationAdminUi';

const blankContent = {
    displayName: '', description: '', category: 'TRANSACTIONAL', channel: 'EMAIL', locale: 'vi-VN',
    variablesSchema: {}, sampleData: {}, subject: '', htmlContent: '', textContent: '',
};

const categoryLabels = {
    TRANSACTIONAL: 'Giao dịch', SECURITY: 'Bảo mật', MARKETING: 'Ưu đãi', OPERATIONAL: 'Vận hành',
};

export default function NotificationTemplateListPage() {
    const navigate = useNavigate();
    const [templates, setTemplates] = useState([]);
    const [coverage, setCoverage] = useState(null);
    const [filters, setFilters] = useState({ query: '', channel: '', locale: '', archived: '' });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [creating, setCreating] = useState(false);
    const [draft, setDraft] = useState({ templateKey: '', content: blankContent });

    const load = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            const params = Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== ''));
            const [templateData, coverageData] = await Promise.all([
                notificationAdminService.templates(params),
                notificationAdminService.coverage(),
            ]);
            setTemplates(templateData || []);
            setCoverage(coverageData || null);
        } catch (requestError) {
            setError(requestError?.message || 'Không thể đọc danh sách mẫu từ nguồn Git hiệu lực.');
        } finally {
            setLoading(false);
        }
    }, [filters]);

    useEffect(() => {
        const timer = setTimeout(load, 250);
        return () => clearTimeout(timer);
    }, [load]);

    const groups = useMemo(() => {
        const byKey = new Map();
        templates.forEach(item => {
            const group = byKey.get(item.templateKey) || {
                templateKey: item.templateKey,
                displayName: item.displayName,
                category: item.category,
                variants: [],
            };
            group.variants.push(item);
            byKey.set(item.templateKey, group);
        });
        return [...byKey.values()].map(group => {
            const contracts = (coverage?.items || []).filter(item => item.templateKey === group.templateKey);
            const readiness = contracts.some(item => item.readiness === 'BLOCKED')
                ? 'BLOCKED'
                : contracts.some(item => item.readiness === 'WARNING') || contracts.length === 0
                    ? 'WARNING'
                    : 'READY';
            return { ...group, contracts, readiness };
        }).sort((left, right) => {
            const order = { BLOCKED: 0, WARNING: 1, READY: 2 };
            return order[left.readiness] - order[right.readiness]
                || left.templateKey.localeCompare(right.templateKey);
        });
    }, [coverage, templates]);
    const locales = useMemo(() => [...new Set(templates.map(item => item.locale))].sort(), [templates]);
    const blocked = (coverage?.items || []).filter(item => item.readiness === 'BLOCKED');

    const createDraft = async event => {
        event.preventDefault();
        setCreating(true);
        setError('');
        try {
            const created = await notificationAdminService.createDraft(
                draft.templateKey.trim().toUpperCase(), draft.content);
            navigate(`/admin/notification-templates/${created.templateKey}?draftId=${created.draftId}`);
        } catch (requestError) {
            setError(requestError?.message || 'Không thể tạo bản nháp kỹ thuật.');
            setCreating('modal');
        }
    };

    return (
        <div className="mx-auto max-w-[1500px] space-y-6 pb-10">
            <PageHeading
                eyebrow="Nội dung thông báo"
                title="Mẫu thông báo"
                description="Mỗi template code được gom thành một mục; channel và locale là các variant bên trong."
                actions={<button type="button" onClick={() => setCreating('modal')} className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-4 py-2.5 text-xs font-black text-zinc-200 hover:border-zinc-500"><FilePlus2 className="h-4 w-4" /> Tạo mẫu kỹ thuật</button>}
            />

            {blocked.length > 0 && (
                <section className="flex flex-col gap-4 rounded-2xl border border-red-400/25 bg-red-400/5 p-4 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex items-start gap-3"><AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-red-300" /><div><p className="text-sm font-black text-white">{blocked.length} contract bắt buộc chưa có template active</p><p className="mt-1 text-xs leading-5 text-zinc-400">{blocked.map(item => `${item.templateKey} · ${item.locale}`).join(', ')}</p></div></div>
                    <Link to="/admin/notification-coverage" className="inline-flex shrink-0 items-center gap-2 text-xs font-black text-red-300">Xem độ phủ <ArrowRight className="h-4 w-4" /></Link>
                </section>
            )}

            <section className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-4">
                <div className="grid gap-3 md:grid-cols-[1fr_180px_160px_180px]">
                    <label className="relative"><Search className="absolute left-3 top-3 h-4 w-4 text-zinc-500" /><input aria-label="Tìm mẫu thông báo" value={filters.query} onChange={event => setFilters(current => ({ ...current, query: event.target.value }))} placeholder="Tìm theo mã hoặc tên hiển thị" className="w-full rounded-xl border border-zinc-700 bg-zinc-950 py-2.5 pl-10 pr-3 text-sm text-white outline-none focus:border-orange-400" /></label>
                    <Filter label="Kênh" value={filters.channel} onChange={value => setFilters(current => ({ ...current, channel: value }))} options={['EMAIL', 'IN_APP', 'WEB_PUSH', 'SMS']} />
                    <Filter label="Ngôn ngữ" value={filters.locale} onChange={value => setFilters(current => ({ ...current, locale: value }))} options={locales} />
                    <select aria-label="Trạng thái lưu trữ" value={filters.archived} onChange={event => setFilters(current => ({ ...current, archived: event.target.value }))} className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-200"><option value="">Mọi trạng thái phát hành</option><option value="false">Đang hoạt động</option><option value="true">Đã lưu trữ</option></select>
                </div>
            </section>

            {error && <ErrorState message={error} onRetry={load} />}
            {!error && loading && <LoadingState label="Đang đọc và nhóm template từ Git…" />}
            {!error && !loading && groups.length === 0 && <EmptyState title="Không có mẫu phù hợp" description="Điều chỉnh bộ lọc hoặc kiểm tra contract đang bị thiếu." />}
            {!error && !loading && groups.length > 0 && (
                <div className="space-y-3">
                    {groups.map(group => {
                        const services = [...new Set(group.contracts.map(item => item.sourceService))];
                        return (
                            <article key={group.templateKey} className="rounded-3xl border border-zinc-800 bg-zinc-900/55 p-5 transition hover:border-zinc-700">
                                <div className="grid gap-5 lg:grid-cols-[0.85fr_1.35fr_0.65fr] lg:items-center">
                                    <div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><h2 className="truncate text-base font-black text-white">{group.displayName}</h2><StatusPill value={group.readiness} /></div><p className="mt-1 font-mono text-[11px] text-orange-300">{group.templateKey}</p><p className="mt-2 text-[10px] font-bold uppercase tracking-wider text-zinc-600">{categoryLabels[group.category] || group.category}</p></div>
                                    <div className="flex flex-wrap gap-2">
                                        {group.variants.map(variant => (
                                            <Link key={`${variant.channel}-${variant.locale}`} to={`/admin/notification-templates/${variant.templateKey}?channel=${variant.channel}&locale=${variant.locale}`} className="group rounded-2xl border border-zinc-800 bg-zinc-950/70 px-3 py-2.5 hover:border-orange-400/40">
                                                <div className="flex items-center gap-2"><span className="text-xs font-black text-zinc-200">{variant.channel.replaceAll('_', ' ')}</span><span className="font-mono text-[10px] text-zinc-500">{variant.locale}</span></div>
                                                <p className="mt-1 inline-flex items-center gap-1 font-mono text-[10px] text-zinc-600"><GitCommit className="h-3 w-3" /> Template revision {shortSha(variant.commitSha)}</p>
                                            </Link>
                                        ))}
                                    </div>
                                    <div className="lg:text-right"><p className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Được sử dụng bởi</p><p className="mt-2 text-xs font-bold text-zinc-300">{services.length ? services.join(', ') : 'Chưa có producer contract'}</p><p className="mt-1 text-[10px] text-zinc-600">{group.variants.length} variant đang hiển thị</p></div>
                                </div>
                            </article>
                        );
                    })}
                </div>
            )}

            {creating && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
                    <form onSubmit={createDraft} className="w-full max-w-xl rounded-3xl border border-zinc-700 bg-zinc-900 p-6 shadow-2xl">
                        <div className="flex items-start justify-between"><div><p className="text-xs font-black uppercase tracking-widest text-orange-400">Technical admin</p><h2 className="mt-2 text-xl font-black text-white">Tạo template mới</h2></div><button type="button" aria-label="Đóng" onClick={() => setCreating(false)} className="rounded-lg p-2 text-zinc-500 hover:bg-zinc-800 hover:text-white"><X className="h-4 w-4" /></button></div>
                        <p className="mt-3 text-xs leading-5 text-zinc-500">Chỉ dùng cho contract mới đã được thống nhất. Với template đang phát hành, hãy mở variant và tạo bản nháp từ phiên bản đó.</p>
                        <div className="mt-6 grid gap-4 sm:grid-cols-2">
                            <label className="sm:col-span-2 text-xs font-bold text-zinc-400">Mã template<input required pattern="[A-Za-z0-9_]{3,100}" value={draft.templateKey} onChange={event => setDraft(current => ({ ...current, templateKey: event.target.value.toUpperCase() }))} placeholder="TEMPLATE_CODE" className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-3 font-mono text-sm text-white outline-none focus:border-orange-400" /></label>
                            <label className="sm:col-span-2 text-xs font-bold text-zinc-400">Tên hiển thị<input required value={draft.content.displayName} onChange={event => setDraft(current => ({ ...current, content: { ...current.content, displayName: event.target.value } }))} className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-3 text-sm text-white outline-none focus:border-orange-400" /></label>
                            <label className="text-xs font-bold text-zinc-400">Kênh<select value={draft.content.channel} onChange={event => setDraft(current => ({ ...current, content: { ...current.content, channel: event.target.value } }))} className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-3 text-sm text-white">{['EMAIL', 'IN_APP', 'WEB_PUSH', 'SMS'].map(value => <option key={value}>{value}</option>)}</select></label>
                            <label className="text-xs font-bold text-zinc-400">Locale<input required pattern="[a-z]{2}-[A-Z]{2}" value={draft.content.locale} onChange={event => setDraft(current => ({ ...current, content: { ...current.content, locale: event.target.value } }))} className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-3 font-mono text-sm text-white" /></label>
                        </div>
                        <p className="mt-4 flex items-center gap-2 text-xs text-zinc-500"><Archive className="h-4 w-4" /> Nội dung được tạo trên draft branch, không tác động production trước khi phát hành.</p>
                        <div className="mt-6 flex justify-end gap-3"><button type="button" onClick={() => setCreating(false)} className="rounded-xl border border-zinc-700 px-4 py-2.5 text-xs font-bold text-zinc-300">Hủy</button><button disabled={creating === true} className="rounded-xl bg-orange-500 px-4 py-2.5 text-xs font-black text-white disabled:opacity-50">{creating === true ? 'Đang tạo…' : 'Tạo bản nháp'}</button></div>
                    </form>
                </div>
            )}
        </div>
    );
}

function Filter({ label, value, onChange, options }) {
    return <select aria-label={label} value={value} onChange={event => onChange(event.target.value)} className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-200"><option value="">Tất cả {label.toLowerCase()}</option>{options.map(option => <option key={option}>{option}</option>)}</select>;
}
