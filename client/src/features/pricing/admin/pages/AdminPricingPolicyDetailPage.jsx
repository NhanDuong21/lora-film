import { useCallback, useEffect, useState } from 'react';
import { ArrowLeft, ChevronLeft, ChevronRight, Copy, Edit, Power, PowerOff, RefreshCw } from 'lucide-react';
import { useNavigate, useOutletContext, useParams, useSearchParams } from 'react-router-dom';
import adminPricingService from '../services/adminPricingService';
import {
  getConflictPresentation,
  getDayTypeLabel,
  getPricingReasonPresentation,
  getRuleScopePresentation,
  getTimeBandLabel,
} from '../utils/pricingPresentation';

const money = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value || 0);
const formatDate = value => {
  if (!value) return 'Không giới hạn';
  const [year, month, day] = String(value).slice(0, 10).split('-').map(Number);
  if (!year || !month || !day) return value;
  return new Intl.DateTimeFormat('vi-VN').format(new Date(Date.UTC(year, month - 1, day)));
};
const formatDateTime = value => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
  : 'Chưa có';
const POLICY_STATUS_LABELS = {
  DRAFT: 'Đang soạn',
  ACTIVE: 'Đang áp dụng',
  INACTIVE: 'Đã ngừng',
};

export default function AdminPricingPolicyDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { triggerToast, triggerConfirm, triggerPrompt } = useOutletContext() || {};
  const requestedReturnTo = searchParams.get('returnTo') || '';
  const returnTo = requestedReturnTo.startsWith('/admin/') ? requestedReturnTo : '';
  const [policy, setPolicy] = useState(null);
  const [usage, setUsage] = useState(null);
  const [usagePage, setUsagePage] = useState(0);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      const [policyResponse, usageResponse] = await Promise.all([
        adminPricingService.getPolicy(id),
        adminPricingService.getUsage(id, { page: usagePage, size: 20 }),
      ]);
      setPolicy(policyResponse?.data);
      setUsage(usageResponse?.data);
    } catch (error) {
      triggerToast?.(error.response?.data?.message || 'Không thể tải bảng giá', 'error');
    }
  }, [id, triggerToast, usagePage]);

  useEffect(() => {
    load();
  }, [load]);

  const act = async action => {
    if (!policy || busy) return;
    setBusy(true);
    try {
      let response;
      if (action === 'activate') {
        const accepted = await triggerConfirm?.({
          title: 'Kích hoạt bảng giá này?',
          message: 'Bảng giá sẽ được dùng để tính giá cho các suất chiếu mới. Nội dung sẽ được khóa để tránh thay đổi ngoài ý muốn.',
          confirmLabel: 'Kích hoạt bảng giá',
        });
        if (accepted === false) return;
        response = await adminPricingService.activatePolicy(id, policy.version);
      } else if (action === 'deactivate') {
        const reason = await triggerPrompt?.({
          title: 'Ngừng dùng bảng giá',
          message: 'Các suất chiếu đã chốt giá không bị thay đổi. Lý do sẽ được lưu để người vận hành khác hiểu quyết định này.',
          label: 'Lý do ngừng dùng',
          placeholder: 'Ví dụ: Thay bằng bảng giá mùa hè',
          confirmLabel: 'Ngừng dùng',
        });
        if (!reason?.trim()) return;
        response = await adminPricingService.deactivatePolicy(id, policy.version, reason.trim());
      } else {
        const name = await triggerPrompt?.({
          title: 'Tạo bản sao để chỉnh sửa',
          message: 'Bảng giá đang dùng sẽ được giữ nguyên. Bạn sẽ chỉnh sửa trên một bản đang soạn mới.',
          label: 'Tên bảng giá mới',
          defaultValue: `${policy.name} - Bản mới`,
          confirmLabel: 'Tạo bản đang soạn',
        });
        if (!name?.trim()) return;
        response = await adminPricingService.copyPolicy(id, policy.version, name.trim());
        const returnQuery = returnTo ? `?returnTo=${encodeURIComponent(returnTo)}` : '';
        navigate(`/admin/pricing/${response.data.publicId}/edit${returnQuery}`);
        return;
      }
      setPolicy(response.data);
      triggerToast?.('Đã cập nhật bảng giá', 'success');
      load();
    } catch (error) {
      if (error?.errorCode === 'PRICE_POLICY_OVERLAP' && Array.isArray(error?.data)) {
        setPolicy(current => ({ ...current, conflicts: error.data }));
      }
      triggerToast?.(
        (error?.errorCode?.startsWith('PRIC') || error?.errorCode?.startsWith('PRICE_')
          ? getPricingReasonPresentation(error.errorCode).label
          : null)
          || error?.message
          || 'Không thể cập nhật bảng giá',
        'error',
      );
    } finally {
      setBusy(false);
    }
  };

  if (!policy) return <div className="p-12 text-center text-zinc-500">Đang tải bảng giá…</div>;

  return (
    <div className="space-y-6 bg-zinc-950 text-white">
      {returnTo && (
        <section className={`flex flex-col gap-3 rounded-2xl border p-4 text-sm sm:flex-row sm:items-center sm:justify-between ${policy.storedStatus === 'ACTIVE' ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-100' : 'border-blue-500/30 bg-blue-500/10 text-blue-100'}`} role="status">
          <div><p className="font-black">{policy.storedStatus === 'ACTIVE' ? 'Bảng giá đã sẵn sàng' : 'Còn một bước: kích hoạt bảng giá'}</p><p className="mt-1 opacity-80">{policy.storedStatus === 'ACTIVE' ? 'Quay lại lịch đang kiểm tra; hệ thống sẽ tự kiểm tra giá lại cho toàn bộ suất.' : 'Bảng giá đang soạn chưa được dùng. Hãy kiểm tra rồi bấm “Kích hoạt bảng giá”.'}</p></div>
          <button type="button" onClick={() => navigate(returnTo)} disabled={policy.storedStatus !== 'ACTIVE'} className="inline-flex min-h-10 shrink-0 items-center gap-2 rounded-xl border border-current/30 px-4 font-black disabled:cursor-not-allowed disabled:opacity-40"><ArrowLeft className="h-4 w-4" />Quay lại lịch</button>
        </section>
      )}
      <div className="flex flex-col gap-4 border-b border-zinc-800 pb-5 md:flex-row md:items-center md:justify-between">
        <div className="flex items-center gap-3">
          <button type="button" onClick={() => navigate('/admin/pricing')} className="rounded-xl p-2 text-zinc-400 hover:bg-zinc-800"><ArrowLeft className="h-5 w-5" /></button>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-black">{policy.name}</h1>
              <span className="rounded-full border border-amber-500/30 bg-amber-500/10 px-2.5 py-1 text-xs font-black text-amber-300">
                {POLICY_STATUS_LABELS[policy.displayStatus] || POLICY_STATUS_LABELS[policy.storedStatus] || 'Chưa xác định'}
              </span>
            </div>
            <p className="mt-1 text-xs text-zinc-500">Bảng giá cho {policy.cinemaName}</p>
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" onClick={load} className="rounded-xl border border-zinc-700 p-2.5 text-zinc-400"><RefreshCw className="h-4 w-4" /></button>
          {policy.storedStatus === 'DRAFT' && <button type="button" onClick={() => navigate(`/admin/pricing/${id}/edit${returnTo ? `?returnTo=${encodeURIComponent(returnTo)}` : ''}`)} className="flex items-center gap-2 rounded-xl border border-zinc-700 px-3 py-2 text-sm font-bold"><Edit className="h-4 w-4" /> Sửa</button>}
          {policy.storedStatus === 'DRAFT' && <button disabled={busy || policy.conflicts?.length > 0} type="button" onClick={() => act('activate')} className="flex items-center gap-2 rounded-xl bg-emerald-500 px-3 py-2 text-sm font-black text-zinc-950 disabled:opacity-40"><Power className="h-4 w-4" /> Kích hoạt bảng giá</button>}
          {policy.storedStatus === 'ACTIVE' && <button disabled={busy} type="button" onClick={() => act('deactivate')} className="flex items-center gap-2 rounded-xl bg-red-500 px-3 py-2 text-sm font-black text-white"><PowerOff className="h-4 w-4" /> Ngừng dùng</button>}
          <button disabled={busy} type="button" onClick={() => act('copy')} className="flex items-center gap-2 rounded-xl border border-zinc-700 px-3 py-2 text-sm font-bold"><Copy className="h-4 w-4" /> Tạo phiên bản mới</button>
        </div>
      </div>

      {policy.storedStatus === 'ACTIVE' && (
        <section className="rounded-2xl border border-blue-500/30 bg-blue-500/10 p-4 text-sm text-blue-100">
          Bảng giá đang áp dụng không thể sửa trực tiếp để giá vé đã bán không bị thay đổi. Hãy tạo một phiên bản mới nếu cần điều chỉnh.
        </section>
      )}

      <div className="grid gap-5 lg:grid-cols-3">
        <section className="rounded-2xl border border-zinc-800 bg-zinc-900/40 p-5 lg:col-span-2">
          <h2 className="font-black">Bảng giá áp dụng ở đâu và khi nào?</h2>
          <dl className="mt-4 grid gap-4 text-sm md:grid-cols-2">
            <div><dt className="text-xs text-zinc-500">Rạp</dt><dd className="mt-1 font-bold">{policy.cinemaName}</dd></div>
            <div><dt className="text-xs text-zinc-500">Thời gian áp dụng</dt><dd className="mt-1">{formatDate(policy.effectiveFrom)} – {formatDate(policy.effectiveTo)}</dd></div>
            <div><dt className="text-xs text-zinc-500">Được kích hoạt lúc</dt><dd className="mt-1">{policy.activatedAt ? formatDateTime(policy.activatedAt) : 'Chưa kích hoạt'}</dd></div>
            {policy.deactivatedAt && <div><dt className="text-xs text-zinc-500">Đã ngừng lúc</dt><dd className="mt-1">{formatDateTime(policy.deactivatedAt)} {policy.deactivationReason ? `· ${policy.deactivationReason}` : ''}</dd></div>}
          </dl>
        </section>
        <section className="rounded-2xl border border-zinc-800 bg-zinc-900/40 p-5">
          <h2 className="font-black">Mức sử dụng</h2>
          <p className="mt-5 text-3xl font-black text-amber-400">{usage?.snapshotShowtimeCount ?? 0}</p>
          <p className="text-xs text-zinc-500">Suất chiếu đã chốt giá từ bảng này</p>
          <p className="mt-5 text-3xl font-black text-zinc-200">{usage?.futureDraftShowtimeCount ?? 0}</p>
          <p className="text-xs text-zinc-500">Suất chiếu đang soạn có thể cần cập nhật giá</p>
        </section>
      </div>

      {policy.conflicts?.length > 0 && (
        <section className="rounded-2xl border border-red-500/30 bg-red-500/10 p-5">
          <h2 className="font-black text-red-300">Có mức giá bị trùng cần xử lý trước khi kích hoạt</h2>
          {policy.conflicts.map(item => {
            const presentation = getConflictPresentation(item);
            return (
              <div key={`${item.firstRuleId}-${item.secondRuleId}`} className="mt-3 rounded-xl border border-red-500/20 bg-zinc-950/30 p-3 text-xs text-red-100">
                <p className="font-black">{presentation.title}</p>
                <p className="mt-1">{presentation.facts}</p>
                <p className="mt-2 text-red-200">{presentation.guidance}</p>
                <details className="mt-2 text-zinc-400">
                  <summary className="cursor-pointer">Chi tiết kỹ thuật</summary>
                  <p className="mt-1 font-mono">{presentation.technical.reasonCode} · {presentation.technical.ruleIds.join(' ↔ ')}</p>
                </details>
              </div>
            );
          })}
        </section>
      )}

      <details className="rounded-2xl border border-zinc-800 bg-zinc-900/20 p-4 text-xs text-zinc-500">
        <summary className="cursor-pointer font-bold text-zinc-400">Thông tin kỹ thuật</summary>
        <p className="mt-3 break-all font-mono">Mã bảng giá: {policy.publicId}</p>
        <p className="mt-1 font-mono">Phiên bản: {policy.version} · Trạng thái lưu: {policy.storedStatus} · Tiền tệ: {policy.currency} · Ưu tiên: {policy.priority}</p>
      </details>

      <section className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/30">
        <div className="border-b border-zinc-800 p-5"><h2 className="font-black">Các mức giá</h2></div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-zinc-950 text-xs uppercase text-zinc-500"><tr><th className="p-4">Loại ghế</th><th className="p-4">Phạm vi</th><th className="p-4">Ngày / giờ</th><th className="p-4 text-right">Giá</th></tr></thead>
            <tbody className="divide-y divide-zinc-800">
              {policy.rules.map(rule => {
                const scope = getRuleScopePresentation(rule);
                return (
                <tr key={rule.publicId}>
                  <td className="p-4 font-bold">{rule.seatTypeName} <span className="ml-1 text-xs text-zinc-500">{rule.seatTypeCode}</span></td>
                  <td className="p-4 text-zinc-400">{scope.label}{scope.detail ? ` · ${scope.detail}` : ''}</td>
                  <td className="p-4 text-zinc-400">{getDayTypeLabel(rule.dayType)} · {getTimeBandLabel(rule.timeBandStart, rule.timeBandEnd)}</td>
                  <td className="p-4 text-right font-black text-emerald-400">{money(rule.price)}</td>
                </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </section>

      {usage?.affectedFutureShowtimes?.length > 0 && (
        <section className="rounded-2xl border border-zinc-800 bg-zinc-900/30 p-5">
          <div className="flex items-center justify-between">
            <h2 className="font-black">Suất chiếu đang soạn trong tương lai</h2>
            {usage.affectedTotalPages > 1 && (
              <div className="flex items-center gap-2 text-xs text-zinc-400">
                <button type="button" disabled={usage.affectedPage === 0} onClick={() => setUsagePage(page => Math.max(0, page - 1))} className="rounded-lg border border-zinc-700 p-1.5 disabled:opacity-30"><ChevronLeft className="h-4 w-4" /></button>
                <span>{usage.affectedPage + 1}/{usage.affectedTotalPages}</span>
                <button type="button" disabled={usage.affectedLast} onClick={() => setUsagePage(page => page + 1)} className="rounded-lg border border-zinc-700 p-1.5 disabled:opacity-30"><ChevronRight className="h-4 w-4" /></button>
              </div>
            )}
          </div>
          <div className="mt-3 divide-y divide-zinc-800">
            {usage.affectedFutureShowtimes.map(item => (
              <button key={item.showtimeId} type="button" onClick={() => navigate(`/admin/showtimes/${item.showtimeId}/pricing`)} className="flex w-full justify-between py-3 text-left text-sm hover:text-amber-300">
                <span>{item.auditoriumName}</span><span className="font-mono text-zinc-500">{item.startTime}</span>
              </button>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
