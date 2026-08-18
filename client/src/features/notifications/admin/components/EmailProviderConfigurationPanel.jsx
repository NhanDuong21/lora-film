import { useState } from 'react';
import {
    AlertCircle,
    CheckCircle2,
    Eye,
    EyeOff,
    Loader2,
    Mail,
    PlugZap,
    Save,
    ShieldCheck,
} from 'lucide-react';
import { notificationAdminService } from '../services/notificationAdminService';
import { formatDateTime } from './NotificationAdminUi';

const errorMessages = {
    SMTP_AUTHENTICATION_FAILED: 'Email hoặc App Password không hợp lệ. Gmail đã từ chối đăng nhập SMTP.',
    SMTP_CONNECTION_FAILED: 'Không thể kết nối máy chủ SMTP. Vui lòng kiểm tra mạng và thử lại.',
    SMTP_CONFIGURATION_TEST_FAILED: 'Không thể xác nhận cấu hình SMTP. Kiểm tra email, App Password và trạng thái tài khoản gửi.',
    VALIDATION_ERROR: 'Thông tin cấu hình chưa hợp lệ. Vui lòng kiểm tra lại các trường bên dưới.',
    VALIDATION_FAILED: 'Thông tin cấu hình chưa hợp lệ. Vui lòng kiểm tra lại các trường bên dưới.',
};

export default function EmailProviderConfigurationPanel({ configuration, onUpdated }) {
    const [senderEmail, setSenderEmail] = useState(configuration?.senderEmail || '');
    const [fromName, setFromName] = useState(configuration?.fromName || 'LoraFilm');
    const [appPassword, setAppPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [workingAction, setWorkingAction] = useState('');
    const [feedback, setFeedback] = useState(null);

    const command = () => ({
        senderEmail: senderEmail.trim(),
        appPassword: appPassword.replace(/\s/g, ''),
        fromName: fromName.trim(),
    });

    const validate = () => {
        if (!senderEmail.trim()) return 'Vui lòng nhập email dùng để gửi thông báo.';
        if (!/^\S+@\S+\.\S+$/.test(senderEmail.trim())) return 'Email người gửi không đúng định dạng.';
        if (!fromName.trim()) return 'Vui lòng nhập tên người gửi.';
        if (command().appPassword.length < 8) return 'Vui lòng nhập App Password hợp lệ của tài khoản email mới.';
        return '';
    };

    const testConnection = async () => {
        const validationMessage = validate();
        if (validationMessage) {
            setFeedback({ type: 'error', message: validationMessage });
            return;
        }
        setWorkingAction('test');
        setFeedback(null);
        try {
            const result = await notificationAdminService.testEmailProvider(command());
            setFeedback({
                type: 'success',
                message: result?.message || 'Kết nối SMTP hợp lệ. Bạn có thể lưu cấu hình này.',
            });
        } catch (requestError) {
            setFeedback({ type: 'error', message: resolveError(requestError) });
        } finally {
            setWorkingAction('');
        }
    };

    const save = async event => {
        event.preventDefault();
        const validationMessage = validate();
        if (validationMessage) {
            setFeedback({ type: 'error', message: validationMessage });
            return;
        }
        setWorkingAction('save');
        setFeedback(null);
        try {
            const updated = await notificationAdminService.updateEmailProvider(command());
            setAppPassword('');
            setShowPassword(false);
            setFeedback({
                type: 'success',
                message: 'Đã kiểm tra và chuyển sang tài khoản gửi mới. Các email tiếp theo dùng cấu hình này ngay, không cần khởi động lại dịch vụ.',
            });
            onUpdated?.(updated);
        } catch (requestError) {
            setFeedback({ type: 'error', message: resolveError(requestError) });
        } finally {
            setWorkingAction('');
        }
    };

    const sourceLabel = configuration?.source === 'ADMIN'
        ? 'Quản trị viên cấu hình'
        : 'Biến môi trường máy chủ';
    const connected = configuration?.connectionStatus === 'CONNECTED';

    return (
        <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/60">
            <div className="grid lg:grid-cols-[0.8fr_1.2fr]">
                <div className="border-b border-zinc-800 p-5 lg:border-b-0 lg:border-r">
                    <div className="flex items-center gap-3">
                        <div className="rounded-2xl bg-sky-400/10 p-3 text-sky-300"><Mail className="h-5 w-5" /></div>
                        <div>
                            <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Nhà cung cấp email</p>
                            <h2 className="mt-1 text-lg font-black text-white">Tài khoản gửi SMTP</h2>
                        </div>
                    </div>

                    <dl className="mt-6 space-y-4 text-sm">
                        <StatusRow label="Đang dùng" value={configuration?.senderEmailMasked || 'Chưa cấu hình'} mono />
                        <StatusRow label="Nguồn cấu hình" value={sourceLabel} />
                        <StatusRow label="Máy chủ" value={`${configuration?.smtpHost || 'smtp.gmail.com'}:${configuration?.smtpPort || 587}`} mono />
                        <StatusRow
                            label="Kết nối gần nhất"
                            value={connected ? 'Đã xác thực' : (configuration?.connectionStatus || 'Chưa kiểm tra')}
                            success={connected}
                        />
                        <StatusRow label="Thời điểm kiểm tra" value={formatDateTime(configuration?.lastTestedAt)} last />
                    </dl>

                    <div className="mt-5 flex gap-3 rounded-2xl border border-emerald-400/15 bg-emerald-400/5 p-4">
                        <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-emerald-300" />
                        <p className="text-xs leading-5 text-zinc-400">App Password được mã hóa tại backend, không ghi log và không bao giờ được trả ngược về trình duyệt.</p>
                    </div>
                </div>

                <form onSubmit={save} autoComplete="off" className="p-5">
                    <div className="flex items-start justify-between gap-4">
                        <div>
                            <p className="text-[10px] font-black uppercase tracking-widest text-orange-300">Thay tài khoản gửi</p>
                            <h3 className="mt-1 text-base font-black text-white">Nhập email và App Password mới</h3>
                            <p className="mt-2 max-w-2xl text-xs leading-5 text-zinc-500">Hệ thống sẽ đăng nhập thử vào SMTP trước. Nếu bị từ chối, cấu hình đang chạy được giữ nguyên.</p>
                        </div>
                    </div>

                    <div className="mt-5 grid gap-4 sm:grid-cols-2">
                        <Field label="Email người gửi" htmlFor="smtp-sender-email">
                            <input
                                id="smtp-sender-email"
                                type="email"
                                value={senderEmail}
                                onChange={event => setSenderEmail(event.target.value)}
                                placeholder="notifications@example.com"
                                autoComplete="off"
                                spellCheck="false"
                                className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm text-white outline-none transition focus:border-orange-400"
                            />
                        </Field>
                        <Field label="Tên hiển thị" htmlFor="smtp-from-name">
                            <input
                                id="smtp-from-name"
                                value={fromName}
                                onChange={event => setFromName(event.target.value)}
                                placeholder="LoraFilm"
                                autoComplete="off"
                                className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm text-white outline-none transition focus:border-orange-400"
                            />
                        </Field>
                    </div>

                    <Field label="App Password mới" htmlFor="smtp-app-password" className="mt-4">
                        <div className="relative">
                            <input
                                id="smtp-app-password"
                                type={showPassword ? 'text' : 'password'}
                                value={appPassword}
                                onChange={event => setAppPassword(event.target.value)}
                                placeholder="Mật khẩu ứng dụng của tài khoản email"
                                autoComplete="new-password"
                                spellCheck="false"
                                className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 pr-12 text-sm text-white outline-none transition focus:border-orange-400"
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword(value => !value)}
                                aria-label={showPassword ? 'Ẩn App Password' : 'Hiện App Password'}
                                className="absolute inset-y-0 right-0 px-4 text-zinc-500 hover:text-white"
                            >
                                {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                            </button>
                        </div>
                        <p className="mt-2 text-[11px] leading-5 text-zinc-600">Với Gmail, hãy dùng App Password 16 ký tự; không nhập mật khẩu đăng nhập Google thông thường.</p>
                    </Field>

                    {feedback && (
                        <div role="status" className={`mt-4 flex items-start gap-3 rounded-xl border p-3 text-xs leading-5 ${feedback.type === 'success' ? 'border-emerald-400/20 bg-emerald-400/5 text-emerald-100' : 'border-red-400/20 bg-red-400/5 text-red-100'}`}>
                            {feedback.type === 'success' ? <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-300" /> : <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-red-300" />}
                            <span>{feedback.message}</span>
                        </div>
                    )}

                    <div className="mt-5 flex flex-wrap justify-end gap-3">
                        <button
                            type="button"
                            onClick={testConnection}
                            disabled={Boolean(workingAction)}
                            className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-2.5 text-xs font-black text-white disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            {workingAction === 'test' ? <Loader2 className="h-4 w-4 animate-spin" /> : <PlugZap className="h-4 w-4" />}
                            Kiểm tra kết nối
                        </button>
                        <button
                            type="submit"
                            disabled={Boolean(workingAction)}
                            className="inline-flex items-center gap-2 rounded-xl bg-orange-500 px-4 py-2.5 text-xs font-black text-white transition hover:bg-orange-400 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            {workingAction === 'save' ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                            Kiểm tra và lưu
                        </button>
                    </div>
                </form>
            </div>
        </section>
    );
}

function Field({ label, htmlFor, className = '', children }) {
    return <div className={className}><label htmlFor={htmlFor} className="mb-2 block text-[10px] font-black uppercase tracking-wider text-zinc-500">{label}</label>{children}</div>;
}

function StatusRow({ label, value, mono, success, last }) {
    return <div className={`flex items-start justify-between gap-5 ${last ? '' : 'border-b border-zinc-800 pb-4'}`}><dt className="shrink-0 text-zinc-500">{label}</dt><dd className={`max-w-[65%] break-all text-right text-xs font-bold ${success ? 'text-emerald-300' : 'text-zinc-300'} ${mono ? 'font-mono text-sky-300' : ''}`}>{value || '—'}</dd></div>;
}

function resolveError(error) {
    const errorCode = error?.errorCode || error?.code || error?.response?.data?.errorCode;
    return errorMessages[errorCode]
        || error?.message
        || 'Không thể cập nhật tài khoản gửi email. Cấu hình cũ vẫn đang được giữ nguyên.';
}
