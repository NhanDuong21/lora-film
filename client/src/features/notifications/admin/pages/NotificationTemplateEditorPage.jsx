import { useCallback, useEffect, useMemo, useState } from 'react';
import {
    AlertTriangle, CheckCircle2, ChevronLeft, Code2, FileJson2, GitCommit,
    History, Maximize2, Monitor, Save, Send, Smartphone, UploadCloud, Users,
} from 'lucide-react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { notificationAdminService } from '../services/notificationAdminService';
import { ErrorState, LoadingState, PageHeading, StatusPill, TechnicalDetails, formatDateTime, shortSha } from '../components/NotificationAdminUi';
import { categoryBusinessName, channelBusinessName, localeBusinessName, notificationBusinessName, serviceBusinessName } from '../utils/notificationBusinessPresentation';

const stringify = value => JSON.stringify(value || {}, null, 2);
const parseJson = value => {
    try { return JSON.parse(value || '{}'); } catch { return {}; }
};
const descriptions = {
    ACCOUNT_LOCKED: 'Thông báo cho người dùng khi tài khoản bị khóa vì lý do bảo mật.',
    REGISTER_OTP: 'Gửi mã OTP để hoàn tất đăng ký tài khoản.',
    FORGOT_PASSWORD_OTP: 'Gửi mã OTP để đặt lại mật khẩu.',
    CHANGE_EMAIL_OTP: 'Gửi mã OTP xác nhận thay đổi địa chỉ email.',
    BOOKING_CONFIRMED: 'Xác nhận thông tin đặt vé và hướng dẫn sử dụng vé.',
    VOUCHER_GRANTED: 'Thông báo voucher mới được cấp cho thành viên.',
};
const sourcePathFrom = document => String(document.description || '').startsWith('Git email template: ')
    ? String(document.description).replace('Git email template: ', '')
    : '';
const contentFrom = document => ({
    displayName: document.displayName || '',
    description: sourcePathFrom(document) ? descriptions[document.templateKey] || `Nội dung thông báo ${document.displayName || document.templateKey}.` : document.description || '',
    category: document.category || 'TRANSACTIONAL', channel: document.channel || 'EMAIL', locale: document.locale || 'vi-VN',
    variablesSchema: document.variablesSchema || {}, sampleData: document.sampleData || {},
    subject: document.subject || '', htmlContent: document.htmlContent || '', textContent: document.textContent || '',
});

export default function NotificationTemplateEditorPage() {
    const { templateKey } = useParams();
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const channel = searchParams.get('channel') || 'EMAIL';
    const locale = searchParams.get('locale') || 'vi-VN';
    const draftId = searchParams.get('draftId');
    const [draftMeta, setDraftMeta] = useState(null);
    const [sourceDocument, setSourceDocument] = useState(null);
    const [form, setForm] = useState(null);
    const [schemaText, setSchemaText] = useState('{}');
    const [sampleText, setSampleText] = useState('{}');
    const [versions, setVersions] = useState([]);
    const [coverageItems, setCoverageItems] = useState([]);
    const [validation, setValidation] = useState(null);
    const [preview, setPreview] = useState(null);
    const [previewMode, setPreviewMode] = useState('desktop');
    const [activeTab, setActiveTab] = useState('content');
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
            const nextContent = contentFrom(document);
            setDraftMeta(draftId ? source : null);
            setSourceDocument(document);
            setForm(nextContent);
            setSchemaText(stringify(document.variablesSchema));
            setSampleText(stringify(document.sampleData));
            const [history, coverage] = await Promise.all([
                notificationAdminService.versions(templateKey, document.channel || channel, document.locale || locale),
                notificationAdminService.coverage(),
            ]);
            setVersions(history || []);
            setCoverageItems((coverage?.items || []).filter(item => item.templateKey === templateKey));
            try {
                const previewResult = draftId
                    ? await notificationAdminService.preview(templateKey, draftId, document.sampleData || {})
                    : await notificationAdminService.previewPublished(templateKey, document.channel, document.locale, null);
                setValidation(previewResult?.validation || null);
                setPreview(previewResult?.rendered || null);
            } catch {
                setPreview(null);
            }
            setDirty(false);
            setConflict(false);
        } catch (requestError) {
            setError(requestError?.message || 'Không thể đọc template từ nguồn Git hiệu lực.');
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

    const parsedContent = () => ({ ...form, variablesSchema: JSON.parse(schemaText), sampleData: JSON.parse(sampleText) });
    const change = (field, value) => { setForm(current => ({ ...current, [field]: value })); setDirty(true); setNotice(''); };
    const changeSample = (key, value) => {
        const next = { ...parseJson(sampleText), [key]: value };
        setSampleText(stringify(next));
        setDirty(true);
    };

    const save = async () => {
        setSaving(true);
        setError('');
        setConflict(false);
        try {
            const content = parsedContent();
            const saved = draftMeta
                ? await notificationAdminService.updateDraft(templateKey, draftMeta.draftId, draftMeta.commitSha, `Update ${templateKey}`, content)
                : await notificationAdminService.createDraft(templateKey, content);
            setDraftMeta(saved);
            setSearchParams({ draftId: saved.draftId, channel: content.channel, locale: content.locale }, { replace: true });
            setDirty(false);
            setNotice(`Đã lưu bản nháp tại revision ${shortSha(saved.commitSha)}.`);
            return saved;
        } catch (requestError) {
            if (requestError?.status === 409 || requestError?.errorCode === 'TEMPLATE_CONFLICT') setConflict(true);
            setError(requestError?.message || 'Không thể lưu bản nháp. Kiểm tra lại JSON và nội dung.');
            return null;
        } finally {
            setSaving(false);
        }
    };

    const validate = async () => {
        const saved = dirty ? await save() : null;
        const currentDraft = saved || draftMeta;
        if (!currentDraft) return;
        try {
            const result = await notificationAdminService.validate(templateKey, currentDraft.draftId);
            setValidation(result);
            setNotice(result.valid ? 'Bản nháp hợp lệ và sẵn sàng gửi thử.' : 'Kiểm tra phát hiện lỗi chặn phát hành.');
        } catch (requestError) { setError(requestError?.message || 'Không thể kiểm tra bản nháp.'); }
    };

    const renderPreview = async () => {
        try {
            let result;
            if (draftMeta) {
                const saved = dirty ? await save() : draftMeta;
                if (!saved) return;
                result = await notificationAdminService.preview(templateKey, saved.draftId, parseJson(sampleText));
            } else {
                result = await notificationAdminService.previewPublished(templateKey, form.channel, form.locale, parseJson(sampleText));
            }
            setValidation(result.validation);
            setPreview(result.rendered);
        } catch (requestError) { setError(requestError?.message || 'Không thể render bản xem trước.'); }
    };

    const publish = async () => {
        if (!draftMeta || dirty) { setError('Hãy lưu bản nháp trước khi phát hành.'); return; }
        if (!window.confirm('Phát hành nội dung này cho khách hàng?\n\nHãy xác nhận bạn đã xem trước trên desktop/mobile, kiểm tra dữ liệu mẫu và gửi bản thử. Phiên bản hiện tại vẫn được lưu trong lịch sử để có thể khôi phục.')) return;
        setSaving(true);
        try {
            const result = await notificationAdminService.publish(templateKey, draftMeta.draftId, draftMeta.commitSha);
            setNotice(`Đã phát hành ${result.version} tại ${shortSha(result.commitSha)}.`);
            navigate(`/admin/notification-templates/${templateKey}?channel=${result.channel}&locale=${result.locale}`, { replace: true });
        } catch (requestError) {
            setConflict(requestError?.status === 409);
            setError(requestError?.message || 'Không thể phát hành bản nháp.');
        } finally { setSaving(false); }
    };

    const compare = async () => {
        if (!diffSelection.from || !diffSelection.to) return;
        try { setDiff(await notificationAdminService.diff(templateKey, diffSelection.from, diffSelection.to, form.channel, form.locale)); }
        catch (requestError) { setError(requestError?.message || 'Không thể so sánh phiên bản.'); }
    };
    const rollback = async version => {
        if (!window.confirm(`Tạo commit rollback mới từ ${version}? Lịch sử Git sẽ không bị ghi lại.`)) return;
        try { const result = await notificationAdminService.rollback(templateKey, form.channel, form.locale, version); setNotice(`Đã phát hành rollback dưới phiên bản ${result.version}.`); await load(); }
        catch (requestError) { setError(requestError?.message || 'Không thể rollback phiên bản.'); }
    };
    const testSend = async () => {
        if (!testDestination) return;
        try {
            const recipient = form.channel === 'EMAIL' ? { email: testDestination } : form.channel === 'SMS' ? { phone: testDestination } : { userPublicId: testDestination, webPushSubscription: testDestination };
            const accepted = await notificationAdminService.testSend(templateKey, {
                idempotencyKey: `admin-test-${crypto.randomUUID()}`, sourceService: 'admin-portal', eventType: 'ADMIN_TEST_SEND',
                templateKey, locale: form.locale, category: form.category, priority: 'NORMAL', test: true,
                recipient, channels: [form.channel], payload: parseJson(sampleText),
            });
            setNotice(`Đã tiếp nhận yêu cầu gửi thử ${accepted.publicId}.`);
        } catch (requestError) { setError(requestError?.message || 'Không thể gửi thử.'); }
    };

    const readiness = coverageItems.some(item => item.readiness === 'BLOCKED') ? 'BLOCKED'
        : coverageItems.some(item => item.readiness === 'WARNING') ? 'WARNING' : 'READY';
    const services = [...new Set(coverageItems.map(item => item.sourceService))];
    const variables = useMemo(() => Object.entries(parseJson(schemaText)), [schemaText]);
    const sampleData = useMemo(() => parseJson(sampleText), [sampleText]);
    const previewWidth = previewMode === 'mobile' ? 'max-w-[390px]' : 'max-w-[900px]';

    if (loading) return <LoadingState label="Đang mở không gian template…" />;
    if (!form) return <ErrorState message={error} onRetry={load} />;

    return (
        <div className={`${fullScreen ? 'fixed inset-0 z-50 overflow-y-auto bg-zinc-950 p-4 sm:p-6' : 'mx-auto max-w-[1600px] pb-12'} space-y-6`}>
            <Link to="/admin/notification-templates" className="inline-flex items-center gap-2 text-xs font-bold text-zinc-500 hover:text-white"><ChevronLeft className="h-4 w-4" /> Quay lại danh sách mẫu</Link>
            <PageHeading
                eyebrow={draftMeta ? 'Bản nháp đang chỉnh sửa' : 'Phiên bản khách hàng đang nhận'}
                title={notificationBusinessName(coverageItems?.[0]?.eventTypes?.[0], templateKey)}
                description={`${channelBusinessName(form.channel)} · ${localeBusinessName(form.locale)} · ${categoryBusinessName(form.category)}`}
                actions={<>
                    <button type="button" onClick={() => setFullScreen(value => !value)} className="rounded-xl border border-zinc-700 p-2.5 text-zinc-300 hover:border-zinc-500" aria-label="Bật hoặc tắt toàn màn hình"><Maximize2 className="h-4 w-4" /></button>
                    {!draftMeta && <button type="button" onClick={save} disabled={saving} className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-4 py-2.5 text-xs font-black text-white disabled:opacity-50"><Save className="h-4 w-4" /> Tạo bản nháp từ phiên bản này</button>}
                    {draftMeta && <><button type="button" onClick={save} disabled={saving || !dirty} className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-4 py-2.5 text-xs font-black text-white disabled:opacity-40"><Save className="h-4 w-4" /> Lưu bản nháp</button><button type="button" onClick={validate} className="rounded-xl border border-zinc-700 px-4 py-2.5 text-xs font-black text-zinc-200">Kiểm tra</button><button type="button" onClick={publish} disabled={dirty || saving || validation?.valid === false} className="inline-flex items-center gap-2 rounded-xl bg-orange-500 px-4 py-2.5 text-xs font-black text-white disabled:cursor-not-allowed disabled:opacity-40"><UploadCloud className="h-4 w-4" /> Phát hành</button></>}
                </>}
            />

            {!draftMeta && <div className="rounded-2xl border border-emerald-400/20 bg-emerald-400/5 px-4 py-3"><p className="text-sm font-black text-emerald-100">Đây là phiên bản đang được gửi cho khách hàng.</p><p className="mt-1 text-xs leading-5 text-zinc-300">Muốn thay đổi nội dung, hãy tạo bản nháp mới. Phiên bản hiện tại sẽ tiếp tục hoạt động cho đến khi bản nháp được kiểm tra và phát hành.</p></div>}
            <div className="flex flex-wrap items-center gap-2"><StatusPill value={draftMeta ? 'DRAFT' : 'PUBLISHED'} /><StatusPill value={readiness} />{services.length > 0 && <span className="inline-flex items-center gap-1.5 text-xs font-bold text-zinc-400"><Users className="h-4 w-4" /> Dùng cho {services.map(serviceBusinessName).join(', ')}</span>}<TechnicalDetails><p className="font-mono">{templateKey} · {form.channel} · {form.locale} · revision {shortSha(sourceDocument?.commitSha)}</p></TechnicalDetails></div>
            {dirty && <div className="rounded-xl border border-amber-400/20 bg-amber-400/5 px-4 py-3 text-xs font-bold text-amber-200">Có thay đổi chưa lưu — preview và phát hành vẫn dùng revision bản nháp gần nhất.</div>}
            {conflict && <div className="flex items-start gap-3 rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm text-red-100"><AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" /><span>Một quản trị viên khác đã thay đổi draft hoặc base revision. Không có nội dung nào bị ghi đè; hãy tải lại trước khi hợp nhất.</span></div>}
            {error && <div className="rounded-xl border border-red-400/20 bg-red-400/5 px-4 py-3 text-sm text-red-200">{error}</div>}
            {notice && <div className="flex items-center gap-2 rounded-xl border border-emerald-400/20 bg-emerald-400/5 px-4 py-3 text-sm text-emerald-200"><CheckCircle2 className="h-4 w-4" /> {notice}</div>}

            <nav className="flex flex-wrap gap-1 rounded-2xl border border-zinc-800 bg-zinc-900/60 p-1.5" aria-label="Khu vực template">
                <Tab active={activeTab === 'content'} onClick={() => setActiveTab('content')} icon={Monitor}>Nội dung & xem trước</Tab>
                <Tab active={activeTab === 'variables'} onClick={() => setActiveTab('variables')} icon={FileJson2}>Thông tin được chèn</Tab>
                <Tab active={activeTab === 'versions'} onClick={() => setActiveTab('versions')} icon={History}>Phiên bản</Tab>
                <Tab active={activeTab === 'technical'} onClick={() => setActiveTab('technical')} icon={Code2}>Kỹ thuật</Tab>
            </nav>

            {activeTab === 'content' && (
                <div className="grid gap-5 xl:grid-cols-[0.8fr_1.2fr]">
                    <section className="space-y-5 rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5">
                        <div className="grid gap-4 sm:grid-cols-2"><Field label="Tên hiển thị" value={form.displayName} onChange={value => change('displayName', value)} readOnly={!draftMeta} /><Field label="Nhóm nghiệp vụ" value={categoryBusinessName(form.category)} onChange={() => {}} readOnly /></div>
                        <Field label="Mô tả nghiệp vụ" value={form.description} onChange={value => change('description', value)} readOnly={!draftMeta} />
                        <Field label="Tiêu đề gửi" value={form.subject} onChange={value => change('subject', value)} readOnly={!draftMeta} mono />
                        <div className={`rounded-2xl border p-4 ${validation?.valid === false ? 'border-red-400/20 bg-red-400/5' : 'border-emerald-400/20 bg-emerald-400/5'}`}><div className="flex items-center justify-between"><p className="text-sm font-black text-white">Kiểm tra nội dung</p><StatusPill value={validation?.valid === false ? 'INVALID' : 'VALID'} /></div><p className="mt-2 text-xs leading-5 text-zinc-400">{validation?.valid === false ? `${validation.errors?.length || 0} lỗi cần sửa trước khi phát hành.` : 'Không phát hiện lỗi render trong dữ liệu mẫu hiện tại.'}</p></div>
                        <section className="rounded-2xl border border-violet-400/20 bg-violet-400/5 p-4"><p className="text-xs font-black text-violet-200">Gửi email thử nghiệm</p><div className="mt-3 flex gap-2"><input value={testDestination} onChange={event => setTestDestination(event.target.value)} placeholder={form.channel === 'EMAIL' ? 'Email nhận bản thử' : 'Đích nhận bản thử'} className="min-w-0 flex-1 rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2.5 text-sm text-white outline-none focus:border-orange-400" /><button type="button" onClick={testSend} className="inline-flex items-center gap-2 rounded-xl bg-white px-4 py-2.5 text-xs font-black text-zinc-950"><Send className="h-4 w-4" /> Gửi bản thử</button></div><p className="mt-2 text-xs leading-5 text-zinc-400">Bản thử dùng phiên bản đang phát hành, có tiền tố [THỬ NGHIỆM] và watermark trong nội dung. Dữ liệu này không tính vào KPI vận hành.</p></section>
                    </section>

                    <section className="sticky top-24 h-fit rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5">
                        <div className="flex flex-wrap items-center justify-between gap-3"><div><p className="text-xs font-black uppercase tracking-widest text-zinc-500">Xem trước an toàn</p><h2 className="mt-1 text-lg font-black text-white">Nội dung khách hàng nhận</h2></div><div className="flex rounded-xl border border-zinc-700 bg-zinc-950 p-1"><PreviewButton active={previewMode === 'desktop'} onClick={() => setPreviewMode('desktop')} icon={Monitor}>Desktop</PreviewButton><PreviewButton active={previewMode === 'mobile'} onClick={() => setPreviewMode('mobile')} icon={Smartphone}>Mobile</PreviewButton><PreviewButton active={previewMode === 'text'} onClick={() => setPreviewMode('text')} icon={Code2}>Văn bản</PreviewButton></div></div>
                        <div className="mt-5 min-h-[500px] rounded-2xl bg-zinc-950 p-3">{!preview ? <div className="flex min-h-[470px] flex-col items-center justify-center gap-3 text-center text-sm text-zinc-600"><AlertTriangle className="h-6 w-6" /><span>Chưa render được dữ liệu mẫu. Kiểm tra tab Biến dữ liệu.</span><button type="button" onClick={renderPreview} className="text-xs font-black text-orange-300">Thử render lại</button></div> : previewMode === 'text' ? <pre className="whitespace-pre-wrap rounded-xl bg-white p-5 font-mono text-xs leading-6 text-zinc-900">{preview.textContent}</pre> : <div className={`mx-auto overflow-hidden rounded-xl bg-white transition-all ${previewWidth}`}><iframe title="Bản xem trước thông báo" sandbox="" srcDoc={preview.htmlContent} className="h-[620px] w-full border-0" /></div>}</div>
                    </section>
                </div>
            )}

            {activeTab === 'variables' && (
                <section className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5"><div className="flex flex-wrap items-end justify-between gap-3"><div><p className="text-xs font-black uppercase tracking-widest text-zinc-500">Thông tin cá nhân hóa</p><h2 className="mt-1 text-lg font-black text-white">Thông tin được chèn vào nội dung</h2><p className="mt-2 text-xs text-zinc-400">Trường bắt buộc phải có dữ liệu trước khi hệ thống có thể gửi.</p></div><button type="button" onClick={renderPreview} className="rounded-xl bg-orange-500 px-4 py-2.5 text-xs font-black text-white">Cập nhật xem trước</button></div><div className="mt-5 overflow-hidden rounded-2xl border border-zinc-800"><div className="hidden grid-cols-[1fr_0.5fr_0.5fr_1.4fr] gap-3 border-b border-zinc-800 bg-zinc-950/60 px-4 py-3 text-xs font-bold text-zinc-500 md:grid"><span>Tên trường</span><span>Loại dữ liệu</span><span>Bắt buộc</span><span>Ví dụ hiển thị</span></div>{variables.map(([key, definition]) => <div key={key} className="grid gap-3 border-b border-zinc-800/70 px-4 py-4 last:border-0 md:grid-cols-[1fr_0.5fr_0.5fr_1.4fr] md:items-center"><div><code className="text-xs font-bold text-orange-300">{key}</code><p className="mt-1 text-[10px] text-zinc-500">Dữ liệu do {services.length ? services.map(serviceBusinessName).join(', ') : 'dịch vụ nguồn'} cung cấp</p></div><span className="text-xs text-zinc-400">{definition.type || 'string'}</span><span className="text-xs text-zinc-400">{definition.required ? 'Có' : 'Không'}</span>{draftMeta ? <input value={sampleData[key] ?? ''} onChange={event => changeSample(key, event.target.value)} className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2 text-xs text-white outline-none focus:border-orange-400" /> : <span className="truncate text-xs text-zinc-300">{String(sampleData[key] ?? 'Chưa có dữ liệu mẫu')}</span>}</div>)}</div></section>
            )}

            {activeTab === 'versions' && (
                <section className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5"><div className="flex items-center gap-2"><History className="h-4 w-4 text-orange-400" /><h2 className="text-lg font-black text-white">Lịch sử phiên bản</h2></div><div className="mt-5 grid gap-5 lg:grid-cols-[0.7fr_1.3fr]"><div><div className="grid grid-cols-2 gap-2"><select aria-label="Phiên bản nguồn" value={diffSelection.from} onChange={event => setDiffSelection(current => ({ ...current, from: event.target.value }))} className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2 text-xs text-white"><option value="">Từ phiên bản…</option>{versions.map(item => <option key={item.version}>{item.version}</option>)}</select><select aria-label="Phiên bản đích" value={diffSelection.to} onChange={event => setDiffSelection(current => ({ ...current, to: event.target.value }))} className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2 text-xs text-white"><option value="">Đến phiên bản…</option>{versions.map(item => <option key={item.version}>{item.version}</option>)}</select></div><button type="button" onClick={compare} className="mt-2 w-full rounded-xl border border-zinc-700 py-2 text-xs font-bold text-zinc-300">So sánh phiên bản</button><div className="mt-4 space-y-2">{versions.map(item => <div key={item.version} className="rounded-xl border border-zinc-800 bg-zinc-950/60 p-3"><div className="flex items-center justify-between"><span className="font-mono text-xs font-black text-orange-300">{item.version}</span><button type="button" onClick={() => rollback(item.version)} className="text-[10px] font-bold text-zinc-500 hover:text-white">Rollback</button></div><p className="mt-2 inline-flex items-center gap-1 font-mono text-[10px] text-zinc-500"><GitCommit className="h-3 w-3" /> {shortSha(item.commitSha)}</p><p className="mt-1 text-[10px] text-zinc-600">{item.author} · {formatDateTime(item.committedAt)}</p></div>)}</div></div><div>{diff ? <div className="grid gap-3 xl:grid-cols-2"><DiffPane label={diffSelection.from} document={diff.from} /><DiffPane label={diffSelection.to} document={diff.to} /></div> : <div className="flex min-h-72 items-center justify-center rounded-2xl border border-dashed border-zinc-700 text-sm text-zinc-600">Chọn hai phiên bản để xem thay đổi.</div>}</div></div></section>
            )}

            {activeTab === 'technical' && (
                <section className="space-y-5 rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5"><div className="grid gap-3 sm:grid-cols-3"><TechnicalMeta label="Đường dẫn nguồn" value={sourcePathFrom(sourceDocument) || 'templates/**/manifest.json'} /><TechnicalMeta label="Template revision" value={sourceDocument?.commitSha} /><TechnicalMeta label="Draft based on revision" value={draftMeta?.baseCommitSha || '—'} /></div><Editor label="HTML source" value={form.htmlContent} onChange={value => change('htmlContent', value)} rows={18} readOnly={!draftMeta} /><Editor label="Plain-text source" value={form.textContent} onChange={value => change('textContent', value)} rows={10} readOnly={!draftMeta} /><div className="grid gap-4 lg:grid-cols-2"><Editor label="Variable schema (JSON)" value={schemaText} onChange={value => { setSchemaText(value); setDirty(true); }} rows={13} readOnly={!draftMeta} /><Editor label="Sample data (JSON)" value={sampleText} onChange={value => { setSampleText(value); setDirty(true); }} rows={13} readOnly={!draftMeta} /></div></section>
            )}
        </div>
    );
}

function Tab({ active, onClick, icon: Icon, children }) { return <button type="button" onClick={onClick} className={`inline-flex items-center gap-2 rounded-xl px-3 py-2.5 text-xs font-black ${active ? 'bg-zinc-800 text-white' : 'text-zinc-500 hover:text-zinc-300'}`}><Icon className="h-4 w-4" />{children}</button>; }
function Field({ label, value, onChange, readOnly, mono }) { return <label className="block text-xs font-bold text-zinc-400">{label}<input readOnly={readOnly} value={value} onChange={event => onChange(event.target.value)} className={`mt-2 w-full rounded-xl border border-zinc-700 px-3 py-2.5 text-sm text-white outline-none ${readOnly ? 'cursor-default bg-zinc-950/40 text-zinc-300' : 'bg-zinc-950 focus:border-orange-400'} ${mono ? 'font-mono' : ''}`} /></label>; }
function Editor({ label, value, onChange, rows, readOnly }) { return <label className="block text-xs font-bold text-zinc-400">{label}<textarea readOnly={readOnly} spellCheck="false" value={value} onChange={event => onChange(event.target.value)} rows={rows} className={`mt-2 w-full resize-y rounded-xl border border-zinc-700 px-4 py-3 font-mono text-xs leading-6 text-zinc-200 outline-none ${readOnly ? 'bg-zinc-950/40' : 'bg-zinc-950 focus:border-orange-400'}`} /></label>; }
function PreviewButton({ active, onClick, icon: Icon, children }) { return <button type="button" onClick={onClick} className={`inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-[10px] font-black ${active ? 'bg-zinc-800 text-white' : 'text-zinc-500'}`}><Icon className="h-3.5 w-3.5" />{children}</button>; }
function TechnicalMeta({ label, value }) { return <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4"><p className="text-[9px] font-black uppercase tracking-wider text-zinc-600">{label}</p><p className="mt-2 break-all font-mono text-xs text-orange-300">{value || '—'}</p></div>; }
function DiffPane({ label, document }) { return <div className="min-w-0 rounded-2xl border border-zinc-800 bg-zinc-950 p-4"><p className="font-mono text-xs font-black text-orange-300">{label}</p><pre className="mt-3 max-h-[500px] overflow-auto whitespace-pre-wrap font-mono text-[11px] leading-5 text-zinc-300">{JSON.stringify(document, null, 2)}</pre></div>; }
