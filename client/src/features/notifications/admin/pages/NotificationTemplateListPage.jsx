import { useCallback, useEffect, useMemo, useState } from 'react';
import { Archive, ArrowRight, FilePlus2, GitCommit, Search, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { notificationAdminService } from '../services/notificationAdminService';
import { EmptyState, ErrorState, LoadingState, PageHeading, StatusPill, formatDateTime, shortSha } from '../components/NotificationAdminUi';

const blankContent = {
    displayName: '',
    description: '',
    category: 'TRANSACTIONAL',
    channel: 'EMAIL',
    locale: 'vi-VN',
    variablesSchema: {},
    sampleData: {},
    subject: '',
    htmlContent: '',
    textContent: '',
};

export default function NotificationTemplateListPage() {
    const navigate = useNavigate();
    const [templates, setTemplates] = useState([]);
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
            setTemplates(await notificationAdminService.templates(params) || []);
        } catch (requestError) {
            setError(requestError?.message || 'Templates could not be read from the Git registry.');
        } finally {
            setLoading(false);
        }
    }, [filters]);

    useEffect(() => {
        const timer = setTimeout(load, 250);
        return () => clearTimeout(timer);
    }, [load]);

    const locales = useMemo(() => [...new Set(templates.map(item => item.locale))].sort(), [templates]);

    const createDraft = async event => {
        event.preventDefault();
        setCreating(true);
        setError('');
        try {
            const created = await notificationAdminService.createDraft(
                draft.templateKey.trim().toUpperCase(), draft.content);
            navigate(`/admin/notification-templates/${created.templateKey}?draftId=${created.draftId}`);
        } catch (requestError) {
            setError(requestError?.message || 'Draft could not be created.');
            setCreating(false);
        }
    };

    return (
        <div className="mx-auto max-w-[1500px] space-y-6 pb-10">
            <PageHeading
                eyebrow="Git-backed content"
                title="Notification templates"
                description="Published content is loaded from the protected Git branch. Draft branches use commit-SHA concurrency checks, and no content is persisted in the notification database."
                actions={
                    <button type="button" onClick={() => setCreating('modal')} className="inline-flex items-center gap-2 rounded-xl bg-orange-500 px-4 py-2.5 text-xs font-black text-white hover:bg-orange-400">
                        <FilePlus2 className="h-4 w-4" /> Create draft
                    </button>
                }
            />

            <section className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-4">
                <div className="grid gap-3 md:grid-cols-[1fr_180px_160px_160px]">
                    <label className="relative">
                        <Search className="absolute left-3 top-3 h-4 w-4 text-zinc-500" />
                        <input aria-label="Search templates" value={filters.query} onChange={event => setFilters(current => ({ ...current, query: event.target.value }))} placeholder="Search key or display name" className="w-full rounded-xl border border-zinc-700 bg-zinc-950 py-2.5 pl-10 pr-3 text-sm text-white outline-none focus:border-orange-400" />
                    </label>
                    <select aria-label="Channel filter" value={filters.channel} onChange={event => setFilters(current => ({ ...current, channel: event.target.value }))} className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-200">
                        <option value="">All channels</option>
                        {['EMAIL', 'IN_APP', 'WEB_PUSH', 'SMS'].map(value => <option key={value}>{value}</option>)}
                    </select>
                    <select aria-label="Locale filter" value={filters.locale} onChange={event => setFilters(current => ({ ...current, locale: event.target.value }))} className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-200">
                        <option value="">All locales</option>
                        {locales.map(value => <option key={value}>{value}</option>)}
                    </select>
                    <select aria-label="Archive filter" value={filters.archived} onChange={event => setFilters(current => ({ ...current, archived: event.target.value }))} className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-200">
                        <option value="">Published + archived</option>
                        <option value="false">Published only</option>
                        <option value="true">Archived only</option>
                    </select>
                </div>
            </section>

            {error && <ErrorState message={error} onRetry={load} />}
            {!error && loading && <LoadingState label="Reading template manifests from Git…" />}
            {!error && !loading && templates.length === 0 && (
                <EmptyState title="No templates match this view" description="Adjust the filters or create the first draft. Template bodies will be committed only to the external registry." />
            )}
            {!error && !loading && templates.length > 0 && (
                <div className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/50">
                    <div className="hidden grid-cols-[1.4fr_0.65fr_0.55fr_0.45fr_0.7fr_44px] gap-4 border-b border-zinc-800 px-5 py-3 text-[10px] font-black uppercase tracking-wider text-zinc-500 lg:grid">
                        <span>Template</span><span>Channel</span><span>Locale</span><span>Status</span><span>Git revision</span><span />
                    </div>
                    {templates.map(item => (
                        <button key={`${item.templateKey}-${item.channel}-${item.locale}`} type="button" onClick={() => navigate(`/admin/notification-templates/${item.templateKey}?channel=${item.channel}&locale=${item.locale}`)} className="grid w-full gap-3 border-b border-zinc-800/70 px-5 py-5 text-left transition hover:bg-zinc-800/40 last:border-0 lg:grid-cols-[1.4fr_0.65fr_0.55fr_0.45fr_0.7fr_44px] lg:items-center lg:gap-4">
                            <div className="min-w-0">
                                <p className="truncate text-sm font-black text-white">{item.displayName}</p>
                                <p className="mt-1 truncate font-mono text-[11px] text-orange-300">{item.templateKey}</p>
                            </div>
                            <span className="text-xs font-bold text-zinc-300">{item.channel.replaceAll('_', ' ')}</span>
                            <span className="font-mono text-xs text-zinc-400">{item.locale}</span>
                            <StatusPill value={item.status} />
                            <div>
                                <p className="inline-flex items-center gap-1.5 font-mono text-xs text-zinc-300"><GitCommit className="h-3.5 w-3.5" /> {shortSha(item.commitSha)}</p>
                                <p className="mt-1 text-[10px] text-zinc-600">{item.publishedVersion || 'untagged'} · {formatDateTime(item.committedAt)}</p>
                            </div>
                            <ArrowRight className="h-4 w-4 text-zinc-600" />
                        </button>
                    ))}
                </div>
            )}

            {creating && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
                    <form onSubmit={createDraft} className="w-full max-w-xl rounded-3xl border border-zinc-700 bg-zinc-900 p-6 shadow-2xl">
                        <div className="flex items-start justify-between">
                            <div>
                                <p className="text-xs font-black uppercase tracking-widest text-orange-400">New Git branch</p>
                                <h2 className="mt-2 text-xl font-black text-white">Create template draft</h2>
                            </div>
                            <button type="button" aria-label="Close" onClick={() => setCreating(false)} className="rounded-lg p-2 text-zinc-500 hover:bg-zinc-800 hover:text-white"><X className="h-4 w-4" /></button>
                        </div>
                        <div className="mt-6 grid gap-4 sm:grid-cols-2">
                            <label className="sm:col-span-2 text-xs font-bold text-zinc-400">Template key
                                <input required pattern="[A-Za-z0-9_]{3,100}" value={draft.templateKey} onChange={event => setDraft(current => ({ ...current, templateKey: event.target.value.toUpperCase() }))} placeholder="TICKET_PURCHASED" className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-3 font-mono text-sm text-white outline-none focus:border-orange-400" />
                            </label>
                            <label className="sm:col-span-2 text-xs font-bold text-zinc-400">Display name
                                <input required value={draft.content.displayName} onChange={event => setDraft(current => ({ ...current, content: { ...current.content, displayName: event.target.value } }))} className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-3 text-sm text-white outline-none focus:border-orange-400" />
                            </label>
                            <label className="text-xs font-bold text-zinc-400">Channel
                                <select value={draft.content.channel} onChange={event => setDraft(current => ({ ...current, content: { ...current.content, channel: event.target.value } }))} className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-3 text-sm text-white">
                                    {['EMAIL', 'IN_APP', 'WEB_PUSH', 'SMS'].map(value => <option key={value}>{value}</option>)}
                                </select>
                            </label>
                            <label className="text-xs font-bold text-zinc-400">Locale
                                <input required pattern="[a-z]{2}-[A-Z]{2}" value={draft.content.locale} onChange={event => setDraft(current => ({ ...current, content: { ...current.content, locale: event.target.value } }))} className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-3 font-mono text-sm text-white" />
                            </label>
                        </div>
                        <p className="mt-4 flex items-center gap-2 text-xs text-zinc-500"><Archive className="h-4 w-4" /> Content starts empty; nothing is copied into the application bundle.</p>
                        <div className="mt-6 flex justify-end gap-3">
                            <button type="button" onClick={() => setCreating(false)} className="rounded-xl border border-zinc-700 px-4 py-2.5 text-xs font-bold text-zinc-300">Cancel</button>
                            <button disabled={creating === true} className="rounded-xl bg-orange-500 px-4 py-2.5 text-xs font-black text-white disabled:opacity-50">{creating === true ? 'Creating…' : 'Create draft'}</button>
                        </div>
                    </form>
                </div>
            )}
        </div>
    );
}
