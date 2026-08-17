/* eslint-disable react-refresh/only-export-components */
import { AlertTriangle, Inbox, LoaderCircle } from 'lucide-react';

export function PageHeading({ eyebrow, title, description, actions }) {
    return (
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
                <p className="text-[11px] font-bold uppercase tracking-[0.24em] text-orange-400">{eyebrow}</p>
                <h1 className="mt-2 text-2xl font-black tracking-tight text-white sm:text-3xl">{title}</h1>
                <p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-400">{description}</p>
            </div>
            {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
        </div>
    );
}

export function LoadingState({ label = 'Đang tải dữ liệu thông báo…' }) {
    return (
        <div className="flex min-h-64 flex-col items-center justify-center rounded-3xl border border-zinc-800 bg-zinc-900/40 text-zinc-400">
            <LoaderCircle className="mb-3 h-6 w-6 animate-spin text-orange-400" />
            <p className="text-sm font-semibold">{label}</p>
        </div>
    );
}

export function ErrorState({ message, onRetry }) {
    return (
        <div className="flex min-h-64 flex-col items-center justify-center rounded-3xl border border-red-500/20 bg-red-500/5 px-6 text-center">
            <AlertTriangle className="mb-3 h-7 w-7 text-red-400" />
            <h2 className="text-base font-bold text-white">Không thể tải dữ liệu thông báo</h2>
            <p className="mt-2 max-w-xl text-sm text-zinc-400">{message}</p>
            {onRetry && (
                <button type="button" onClick={onRetry} className="mt-5 rounded-xl bg-white px-4 py-2 text-xs font-bold text-zinc-950">
                    Thử lại
                </button>
            )}
        </div>
    );
}

export function EmptyState({ title, description }) {
    return (
        <div className="flex min-h-56 flex-col items-center justify-center rounded-3xl border border-dashed border-zinc-700 bg-zinc-900/30 px-6 text-center">
            <Inbox className="mb-3 h-7 w-7 text-zinc-600" />
            <h2 className="text-sm font-bold text-white">{title}</h2>
            <p className="mt-2 max-w-lg text-sm text-zinc-500">{description}</p>
        </div>
    );
}

const statusStyles = {
    DELIVERED: 'border-emerald-400/20 bg-emerald-400/10 text-emerald-300',
    SENT: 'border-emerald-400/20 bg-emerald-400/10 text-emerald-300',
    COMPLETED: 'border-emerald-400/20 bg-emerald-400/10 text-emerald-300',
    READY: 'border-emerald-400/20 bg-emerald-400/10 text-emerald-300',
    VALID: 'border-emerald-400/20 bg-emerald-400/10 text-emerald-300',
    PUBLISHED: 'border-emerald-400/20 bg-emerald-400/10 text-emerald-300',
    FAILED: 'border-red-400/20 bg-red-400/10 text-red-300',
    DEAD_LETTERED: 'border-red-400/20 bg-red-400/10 text-red-300',
    BLOCKED: 'border-red-400/20 bg-red-400/10 text-red-300',
    INVALID: 'border-red-400/20 bg-red-400/10 text-red-300',
    PARTIALLY_FAILED: 'border-amber-400/20 bg-amber-400/10 text-amber-300',
    WARNING: 'border-amber-400/20 bg-amber-400/10 text-amber-300',
    RETRY_SCHEDULED: 'border-amber-400/20 bg-amber-400/10 text-amber-300',
    PROCESSING: 'border-sky-400/20 bg-sky-400/10 text-sky-300',
    PENDING: 'border-sky-400/20 bg-sky-400/10 text-sky-300',
    ACCEPTED: 'border-sky-400/20 bg-sky-400/10 text-sky-300',
    DRAFT: 'border-violet-400/20 bg-violet-400/10 text-violet-300',
    ARCHIVED: 'border-zinc-500/30 bg-zinc-500/10 text-zinc-400',
    UNLINKED: 'border-zinc-600 bg-zinc-800/70 text-zinc-300',
    SYNCED: 'border-emerald-400/20 bg-emerald-400/10 text-emerald-300',
    HEALTHY: 'border-sky-400/20 bg-sky-400/10 text-sky-300',
    TEST: 'border-violet-400/20 bg-violet-400/10 text-violet-300',
    SUPPRESSED: 'border-zinc-500/30 bg-zinc-500/10 text-zinc-400',
    CANCELLED: 'border-zinc-500/30 bg-zinc-500/10 text-zinc-400',
};

const statusLabels = {
    AVAILABLE: 'Sẵn sàng',
    UNAVAILABLE: 'Không khả dụng',
    DELIVERED: 'Đã xác nhận giao',
    SENT: 'Đã được chấp nhận gửi',
    ACCEPTED: 'Đã tiếp nhận',
    COMPLETED: 'Hoàn tất',
    PARTIALLY_FAILED: 'Hoàn tất một phần',
    PUBLISHED: 'Đã phát hành',
    FAILED: 'Thất bại',
    DEAD_LETTERED: 'Cần can thiệp',
    RETRY_SCHEDULED: 'Đang thử lại',
    PROCESSING: 'Đang xử lý',
    PENDING: 'Đang chờ',
    DRAFT: 'Bản nháp',
    ARCHIVED: 'Đã lưu trữ',
    UNLINKED: 'Chưa liên kết',
    SYNCED: 'Đã đồng bộ',
    HEALTHY: 'Bình thường',
    TEST: 'Gửi thử',
    SUPPRESSED: 'Bị chặn theo chính sách',
    CANCELLED: 'Đã hủy',
    READY: 'Sẵn sàng',
    WARNING: 'Có cảnh báo',
    BLOCKED: 'Bị chặn',
    VALID: 'Hợp lệ',
    INVALID: 'Không hợp lệ',
};

export function StatusPill({ value }) {
    const normalized = String(value || 'UNKNOWN').toUpperCase();
    return (
        <span className={`inline-flex rounded-full border px-2.5 py-1 text-[10px] font-black uppercase tracking-wider ${
            statusStyles[normalized] || 'border-zinc-700 bg-zinc-800 text-zinc-300'
        }`}>
            {statusLabels[normalized] || normalized.replaceAll('_', ' ')}
        </span>
    );
}

export const formatNumber = value => new Intl.NumberFormat('vi-VN').format(Number(value || 0));
export const formatPercent = value => `${Number(value || 0).toFixed(1)}%`;
export const formatDateTime = value => value
    ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
    : '—';
export const shortSha = value => value ? String(value).slice(0, 10) : '—';
