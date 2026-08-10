import { useCallback, useEffect, useState } from 'react';
import { AlertTriangle, CheckCircle2, ChevronLeft, Code2, GitCommit, History, Maximize2, Monitor, Save, Send, Smartphone, UploadCloud } from 'lucide-react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { notificationAdminService } from '../services/notificationAdminService';
import { ErrorState, LoadingState, PageHeading, StatusPill, formatDateTime, shortSha } from '../components/NotificationAdminUi';

const stringify = value => JSON.stringify(value || {}, null, 2);
const contentFrom = document => ({
    displayName: document.displayName || '',
    description: document.description || '',
    category: document.category || 'TRANSACTIONAL',
    channel: document.channel || 'EMAIL',
    locale: document.locale || 'vi-VN',
    variablesSchema: document.variablesSchema || {},
    sampleData: document.sampleData || {},
    subject: document.subject || '',
    htmlContent: document.htmlContent || '',
    textContent: document.textContent || '',
});

export default function NotificationTemplateEditorPage() {
    const { templateKey } = useParams();
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const channel = searchParams.get('channel') || 'EMAIL';
    const locale = searchParams.get('locale') || 'vi-VN';
    const draftId = searchParams.get('draftId');
    const [draftMeta, setDraftMeta] = useState(null);
    const [form, setForm] = useState(null);
    const [schemaText, setSchemaText] = useState('{}');
    const [sampleText, setSampleText] = useState('{}');
    const [versions, setVersions] = useState([]);
    const [validation, setValidation] = useState(null);
    const [preview, setPreview] = useState(null);
    const [previewMode, setPreviewMode] = useState('desktop');
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [dirty, setDirty] = useState(false);
    const [error, setError] = useState('');
    const [notice, setNotice] = useState('');
    const [conflict, setConflict] = useState(false);
    const [diff, setDiff] = useState(null);
    const [diffSelection, setDiffSelection] = useState({ from: '', to: '' });
    const [fullScreen, setFullScreen] = useState(false);
    const [testDestination, setTestDestination] = useState('');

    const load = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            const source = draftId
                ? await notificationAdminService.draft(templateKey, draftId)
                : { document: await notificationAdminService.published(templateKey, channel, locale) };
            const document = source.document;
            setDraftMeta(draftId ? source : null);
            setForm(contentFrom(document));
            setSchemaText(stringify(document.variablesSchema));
            setSampleText(stringify(document.sampleData));
            const history = await notificationAdminService.versions(
                templateKey, document.channel || channel, document.locale || locale);
            setVersions(history || []);
            setDirty(false);
            setConflict(false);
        } catch (requestError) {
            setError(requestError?.message || 'The template could not be read from Git.');
        } finally {
            setLoading(false);
        }
    }, [channel, draftId, locale, templateKey]);

    useEffect(() => {
        const timer = setTimeout(load, 0);
        return () => clearTimeout(timer);
    }, [load]);
    useEffect(() => {
        const protect = event => {
            if (!dirty) return;
            event.preventDefault();
            event.returnValue = '';
        };
        window.addEventListener('beforeunload', protect);
        return () => window.removeEventListener('beforeunload', protect);
    }, [dirty]);

    const parsedContent = () => ({
        ...form,
        variablesSchema: JSON.parse(schemaText),
        sampleData: JSON.parse(sampleText),
    });

    const change = (field, value) => {
        setForm(current => ({ ...current, [field]: value }));
        setDirty(true);
        setNotice('');
    };

    const save = async () => {
        setSaving(true);
        setError('');
        setConflict(false);
        try {
            const content = parsedContent();
            const saved = draftMeta
                ? await notificationAdminService.updateDraft(
                    templateKey, draftMeta.draftId, draftMeta.commitSha, `Update ${templateKey}`, content)
                : await notificationAdminService.createDraft(templateKey, content);
            setDraftMeta(saved);
            setSearchParams({ draftId: saved.draftId, channel: content.channel, locale: content.locale }, { replace: true });
            setDirty(false);
            setNotice(`Draft saved at commit ${shortSha(saved.commitSha)}.`);
            return saved;
        } catch (requestError) {
            if (requestError?.status === 409 || requestError?.errorCode === 'TEMPLATE_CONFLICT') {
                setConflict(true);
            }
            setError(requestError?.message || 'Draft could not be saved.');
            return null;
        } finally {
            setSaving(false);
        }
    };

    const validate = async () => {
        const saved = dirty || !draftMeta ? await save() : null;
        const currentDraft = saved || draftMeta || null;
        if (!currentDraft) return;
        try {
            const result = await notificationAdminService.validate(templateKey, currentDraft.draftId);
            setValidation(result);
            setNotice(result.valid ? 'Validation passed.' : 'Validation found blocking errors.');
        } catch (requestError) {
            setError(requestError?.message || 'Validation failed.');
        }
    };

    const renderPreview = async () => {
        if (!draftMeta || dirty) {
            setError('Save the draft before rendering a server-side preview.');
            return;
        }
        try {
            const result = await notificationAdminService.preview(
                templateKey, draftMeta.draftId, JSON.parse(sampleText));
            setValidation(result.validation);
            setPreview(result.rendered);
        } catch (requestError) {
            setError(requestError?.message || 'Preview could not be rendered.');
        }
    };

    const publish = async () => {
        if (!draftMeta || dirty) {
            setError('Save the draft before publishing.');
            return;
        }
        if (!window.confirm('Publish this validated draft to the protected branch?')) return;
        setSaving(true);
        try {
            const result = await notificationAdminService.publish(
                templateKey, draftMeta.draftId, draftMeta.commitSha);
            setNotice(`Published ${result.version} at ${shortSha(result.commitSha)}.`);
            navigate(`/admin/notification-templates/${templateKey}?channel=${result.channel}&locale=${result.locale}`, { replace: true });
        } catch (requestError) {
            setConflict(requestError?.status === 409);
            setError(requestError?.message || 'Publication failed.');
        } finally {
            setSaving(false);
        }
    };

    const compare = async () => {
        if (!diffSelection.from || !diffSelection.to) return;
        try {
            setDiff(await notificationAdminService.diff(
                templateKey, diffSelection.from, diffSelection.to, form.channel, form.locale));
        } catch (requestError) {
            setError(requestError?.message || 'Versions could not be compared.');
        }
    };

    const rollback = async version => {
        if (!window.confirm(`Create a new rollback commit from ${version}? Git history will not be rewritten.`)) return;
        try {
            const result = await notificationAdminService.rollback(
                templateKey, form.channel, form.locale, version);
            setNotice(`Rollback published as ${result.version}.`);
            await load();
        } catch (requestError) {
            setError(requestError?.message || 'Rollback failed.');
        }
    };

    const testSend = async () => {
        if (!testDestination) return;
        try {
            const recipient = form.channel === 'EMAIL'
                ? { email: testDestination }
                : form.channel === 'SMS'
                    ? { phone: testDestination }
                    : { userPublicId: testDestination, webPushSubscription: testDestination };
            const accepted = await notificationAdminService.testSend(templateKey, {
                idempotencyKey: `admin-test-${crypto.randomUUID()}`,
                sourceService: 'admin-portal',
                eventType: 'ADMIN_TEST_SEND',
                templateKey,
                locale: form.locale,
                category: form.category,
                priority: 'NORMAL',
                test: true,
                recipient,
                channels: [form.channel],
                payload: JSON.parse(sampleText),
            });
            setNotice(`Test notification accepted: ${accepted.publicId}`);
        } catch (requestError) {
            setError(requestError?.message || 'Test send failed.');
        }
    };

    const previewWidth = previewMode === 'mobile' ? 'max-w-[390px]' : 'max-w-[900px]';

    if (loading) return <LoadingState label="Opening Git template workspace…" />;
    if (!form) return <ErrorState message={error} onRetry={load} />;

    return (
        <div className={`${fullScreen ? 'fixed inset-0 z-50 overflow-y-auto bg-zinc-950 p-4 sm:p-6' : 'mx-auto max-w-[1600px] pb-12'} space-y-6`}>
            <Link to="/admin/notification-templates" className="inline-flex items-center gap-2 text-xs font-bold text-zinc-500 hover:text-white">
                <ChevronLeft className="h-4 w-4" /> Back to templates
            </Link>
            <PageHeading
                eyebrow={draftMeta ? 'Draft branch workspace' : 'Published template'}
                title={form.displayName || templateKey}
                description={`${templateKey} · ${form.channel} · ${form.locale}`}
                actions={
                    <>
                        <button type="button" onClick={() => setFullScreen(value => !value)} className="rounded-xl border border-zinc-700 p-2.5 text-zinc-300 hover:border-zinc-500" aria-label="Toggle full screen"><Maximize2 className="h-4 w-4" /></button>
                        <button type="button" onClick={save} disabled={saving} className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-4 py-2.5 text-xs font-black text-white disabled:opacity-50"><Save className="h-4 w-4" /> {draftMeta ? 'Save draft' : 'Create draft'}</button>
                        <button type="button" onClick={publish} disabled={!draftMeta || dirty || saving} className="inline-flex items-center gap-2 rounded-xl bg-orange-500 px-4 py-2.5 text-xs font-black text-white disabled:cursor-not-allowed disabled:opacity-40"><UploadCloud className="h-4 w-4" /> Publish</button>
                    </>
                }
            />

            {dirty && <div className="rounded-xl border border-amber-400/20 bg-amber-400/5 px-4 py-3 text-xs font-bold text-amber-200">Unsaved changes — preview and publish use the last committed draft.</div>}
            {conflict && <div className="flex items-start gap-3 rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm text-red-100"><AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" /><span>Another administrator changed this branch or the published base. Reload before deciding how to reconcile; no changes were overwritten.</span></div>}
            {error && <div className="rounded-xl border border-red-400/20 bg-red-400/5 px-4 py-3 text-sm text-red-200">{error}</div>}
            {notice && <div className="flex items-center gap-2 rounded-xl border border-emerald-400/20 bg-emerald-400/5 px-4 py-3 text-sm text-emerald-200"><CheckCircle2 className="h-4 w-4" /> {notice}</div>}

            <div className="grid gap-5 xl:grid-cols-[1.15fr_0.85fr]">
                <section className="space-y-5 rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5">
                    <div className="grid gap-4 md:grid-cols-2">
                        <Field label="Display name" value={form.displayName} onChange={value => change('displayName', value)} />
                        <Field label="Description" value={form.description} onChange={value => change('description', value)} />
                        <SelectField label="Category" value={form.category} options={['TRANSACTIONAL', 'SECURITY', 'MARKETING', 'OPERATIONAL']} onChange={value => change('category', value)} />
                        <div className="grid grid-cols-2 gap-3">
                            <SelectField label="Channel" value={form.channel} options={['EMAIL', 'IN_APP', 'WEB_PUSH', 'SMS']} onChange={value => change('channel', value)} />
                            <Field label="Locale" value={form.locale} onChange={value => change('locale', value)} mono />
                        </div>
                    </div>
                    <Editor label="Subject" value={form.subject} onChange={value => change('subject', value)} rows={3} />
                    <Editor label="HTML content" value={form.htmlContent} onChange={value => change('htmlContent', value)} rows={15} />
                    <Editor label="Plain-text content" value={form.textContent} onChange={value => change('textContent', value)} rows={10} />
                    <div className="grid gap-4 lg:grid-cols-2">
                        <Editor label="Variable schema (JSON)" value={schemaText} onChange={value => { setSchemaText(value); setDirty(true); }} rows={13} />
                        <Editor label="Sample data (JSON)" value={sampleText} onChange={value => { setSampleText(value); setDirty(true); }} rows={13} />
                    </div>
                    <div className="flex flex-wrap gap-2">
                        <button type="button" onClick={validate} className="rounded-xl border border-zinc-700 px-4 py-2.5 text-xs font-black text-zinc-200">Validate draft</button>
                        <button type="button" onClick={renderPreview} className="rounded-xl border border-zinc-700 px-4 py-2.5 text-xs font-black text-zinc-200">Render preview</button>
                    </div>
                    {validation && (
                        <div className={`rounded-2xl border p-4 ${validation.valid ? 'border-emerald-400/20 bg-emerald-400/5' : 'border-red-400/20 bg-red-400/5'}`}>
                            <div className="flex items-center justify-between"><p className="text-sm font-black text-white">Validation result</p><StatusPill value={validation.valid ? 'VALID' : 'INVALID'} /></div>
                            {[...(validation.errors || []), ...(validation.warnings || [])].map(message => <p key={message} className="mt-2 text-xs text-zinc-300">• {message}</p>)}
                        </div>
                    )}
                </section>

                <div className="space-y-5">
                    <section className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5">
                        <div className="flex flex-wrap items-center justify-between gap-3">
                            <div><p className="text-xs font-black uppercase tracking-widest text-zinc-500">Safe preview</p><h2 className="mt-1 text-lg font-black text-white">Rendered output</h2></div>
                            <div className="flex rounded-xl border border-zinc-700 bg-zinc-950 p-1">
                                <PreviewButton active={previewMode === 'desktop'} onClick={() => setPreviewMode('desktop')} icon={Monitor}>Desktop</PreviewButton>
                                <PreviewButton active={previewMode === 'mobile'} onClick={() => setPreviewMode('mobile')} icon={Smartphone}>Mobile</PreviewButton>
                                <PreviewButton active={previewMode === 'text'} onClick={() => setPreviewMode('text')} icon={Code2}>Text</PreviewButton>
                            </div>
                        </div>
                        <div className="mt-5 min-h-[420px] rounded-2xl bg-zinc-950 p-3">
                            {!preview ? <div className="flex min-h-[390px] items-center justify-center text-center text-sm text-zinc-600">Save and render the draft to see a sanitized preview.</div>
                                : previewMode === 'text'
                                    ? <pre className="whitespace-pre-wrap p-4 font-mono text-xs leading-6 text-zinc-200">{preview.textContent}</pre>
                                    : <div className={`mx-auto overflow-hidden rounded-xl bg-white transition-all ${previewWidth}`}><iframe title="Sanitized notification preview" sandbox="" srcDoc={preview.htmlContent} className="h-[560px] w-full border-0" /></div>}
                        </div>
                    </section>

                    <section className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5">
                        <p className="text-xs font-black uppercase tracking-widest text-zinc-500">Test delivery</p>
                        <div className="mt-4 flex gap-2">
                            <input value={testDestination} onChange={event => setTestDestination(event.target.value)} placeholder={form.channel === 'EMAIL' ? 'qa@example.com' : 'Test destination'} className="min-w-0 flex-1 rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2.5 text-sm text-white outline-none focus:border-orange-400" />
                            <button type="button" onClick={testSend} className="inline-flex items-center gap-2 rounded-xl bg-white px-4 py-2.5 text-xs font-black text-zinc-950"><Send className="h-4 w-4" /> Send</button>
                        </div>
                        <p className="mt-2 text-[11px] leading-5 text-zinc-500">Test requests are persisted with <code>is_test=true</code> and use the published version.</p>
                    </section>

                    <section className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5">
                        <div className="flex items-center gap-2"><History className="h-4 w-4 text-orange-400" /><h2 className="text-sm font-black text-white">Version history</h2></div>
                        <div className="mt-4 grid grid-cols-2 gap-2">
                            <select aria-label="From version" value={diffSelection.from} onChange={event => setDiffSelection(current => ({ ...current, from: event.target.value }))} className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2 text-xs text-white"><option value="">From…</option>{versions.map(item => <option key={item.version}>{item.version}</option>)}</select>
                            <select aria-label="To version" value={diffSelection.to} onChange={event => setDiffSelection(current => ({ ...current, to: event.target.value }))} className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2 text-xs text-white"><option value="">To…</option>{versions.map(item => <option key={item.version}>{item.version}</option>)}</select>
                        </div>
                        <button type="button" onClick={compare} className="mt-2 w-full rounded-xl border border-zinc-700 py-2 text-xs font-bold text-zinc-300">Compare versions</button>
                        <div className="mt-4 max-h-80 space-y-2 overflow-y-auto">
                            {versions.map(item => (
                                <div key={item.version} className="rounded-xl border border-zinc-800 bg-zinc-950/60 p-3">
                                    <div className="flex items-center justify-between"><span className="font-mono text-xs font-black text-orange-300">{item.version}</span><button type="button" onClick={() => rollback(item.version)} className="text-[10px] font-bold text-zinc-500 hover:text-white">Rollback</button></div>
                                    <p className="mt-2 inline-flex items-center gap-1 font-mono text-[10px] text-zinc-500"><GitCommit className="h-3 w-3" /> {shortSha(item.commitSha)}</p>
                                    <p className="mt-1 text-[10px] text-zinc-600">{item.author} · {formatDateTime(item.committedAt)}</p>
                                </div>
                            ))}
                        </div>
                    </section>
                </div>
            </div>

            {diff && (
                <section className="rounded-3xl border border-zinc-800 bg-zinc-900 p-5">
                    <h2 className="text-lg font-black text-white">Version diff</h2>
                    <div className="mt-4 grid gap-4 lg:grid-cols-2">
                        <DiffPane label={diffSelection.from} document={diff.from} />
                        <DiffPane label={diffSelection.to} document={diff.to} />
                    </div>
                </section>
            )}
        </div>
    );
}

function Field({ label, value, onChange, mono }) {
    return <label className="text-xs font-bold text-zinc-400">{label}<input value={value} onChange={event => onChange(event.target.value)} className={`mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2.5 text-sm text-white outline-none focus:border-orange-400 ${mono ? 'font-mono' : ''}`} /></label>;
}
function SelectField({ label, value, options, onChange }) {
    return <label className="text-xs font-bold text-zinc-400">{label}<select value={value} onChange={event => onChange(event.target.value)} className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2.5 text-sm text-white">{options.map(option => <option key={option}>{option}</option>)}</select></label>;
}
function Editor({ label, value, onChange, rows }) {
    return <label className="block text-xs font-bold text-zinc-400">{label}<textarea spellCheck="false" value={value} onChange={event => onChange(event.target.value)} rows={rows} className="mt-2 w-full resize-y rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 font-mono text-xs leading-6 text-zinc-200 outline-none focus:border-orange-400" /></label>;
}
function PreviewButton({ active, onClick, icon: Icon, children }) {
    return <button type="button" onClick={onClick} className={`inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-[10px] font-black ${active ? 'bg-zinc-800 text-white' : 'text-zinc-500'}`}><Icon className="h-3.5 w-3.5" />{children}</button>;
}
function DiffPane({ label, document }) {
    return <div className="min-w-0 rounded-2xl border border-zinc-800 bg-zinc-950 p-4"><p className="font-mono text-xs font-black text-orange-300">{label}</p><pre className="mt-3 max-h-[500px] overflow-auto whitespace-pre-wrap font-mono text-[11px] leading-5 text-zinc-300">{JSON.stringify(document, null, 2)}</pre></div>;
}
