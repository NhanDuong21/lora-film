import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Activity, CheckCircle2, Eye, Info, LockKeyhole, Search, ShieldAlert } from 'lucide-react';
import { getAuthAudits, reviewAuthAudit } from '../services/authAdminService';
import { getUserAudits, getUserProfiles, reviewUserAudit } from '../services/userAdminService';
import { AsyncState, Input, Select } from '@/components/common/ui/uiKit';
import {
  ConsolePagination,
  ConsolePanel,
  DetailDrawer,
  DetailGrid,
  MetricStrip,
  OperationsHeader,
} from '../components/OperationsConsole';
import {
  getAuditActionLabel,
  getDeviceLabel,
  getTargetTypeLabel,
  summarizeAuditDetails,
} from '../utils/systemPresentation';

const TARGET_TYPES = ['', 'USER', 'CUSTOMER', 'EMPLOYEE', 'PAYROLL', 'DEPARTMENT', 'POSITION', 'ATTENDANCE', 'WORK_SHIFT', 'LEAVE_REQUEST'];
const TECHNICAL_ACTIONS = new Set(['PII_BACKFILL_COMPLETED', 'PII_RETENTION_SCHEDULED']);
const ADMINISTRATIVE_ACTIONS = new Set([
  'CREATE_EMPLOYEE_INVITATION', 'RESEND_EMPLOYEE_INVITATION',
  'ADMIN_SENT_PASSWORD_RESET', 'ADMIN_REVOKED_ALL_SESSIONS',
  'UPDATE_ACCOUNT_STATUS', 'UPDATE_ACCOUNT_ROLE', 'UPDATE_ACCOUNT_ACCESS_PROFILE',
  'UPDATE_MANAGER_CINEMA_ASSIGNMENTS', 'UPDATE_ROLE', 'DELETE_ROLE',
]);
const PAGE_SIZE = 20;

const formatDate = value => value ? new Date(value).toLocaleString('vi-VN') : '—';
const isNumeric = value => value !== null && value !== undefined && /^\d+$/.test(String(value));

const resultLabel = result => ({ SUCCESS: 'Thành công', FAILED: 'Thất bại', BLOCKED: 'Bị chặn' }[result] || 'Đã hoàn tất');
const severityLabel = severity => ({ NORMAL: 'Bình thường', REVIEW: 'Cần kiểm tra', CRITICAL: 'Nghiêm trọng' }[severity] || 'Bình thường');
const reviewLabel = status => ({ UNREVIEWED: 'Chưa kiểm tra', REVIEWED: 'Đã kiểm tra', RESOLVED: 'Đã giải quyết', NOT_REQUIRED: 'Không cần xử lý' }[status] || 'Không cần xử lý');

function EventBadge({ entry }) {
  const severity = entry.severity || 'NORMAL';
  const classes = severity === 'CRITICAL'
    ? 'border-red-500/25 bg-red-500/10 text-red-300'
    : severity === 'REVIEW'
      ? 'border-amber-500/25 bg-amber-500/10 text-amber-300'
      : entry.result === 'FAILED'
        ? 'border-zinc-500/25 bg-zinc-500/10 text-zinc-300'
        : 'border-emerald-500/20 bg-emerald-500/10 text-emerald-300';
  return <span className={`inline-flex rounded-full border px-2.5 py-1 text-[10px] font-black uppercase tracking-wide ${classes}`}>{severityLabel(severity)}</span>;
}

export default function AdminAuthAuditPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const requestedTab = searchParams.get('tab');
  const activeTab = ['attention', 'security', 'operations'].includes(requestedTab) ? requestedTab : 'attention';
  const [query, setQuery] = useState({ keyword: '', targetType: '', page: 0, size: PAGE_SIZE });
  const [result, setResult] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [state, setState] = useState({ loading: true, error: '' });
  const [selectedEntry, setSelectedEntry] = useState(null);
  const [profiles, setProfiles] = useState(new Map());
  const [reviewNote, setReviewNote] = useState('');
  const [reviewing, setReviewing] = useState(false);

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      let data;
      if (activeTab === 'attention') {
        const [security, operations] = await Promise.all([
          getAuthAudits({ keyword: query.keyword || undefined, attentionOnly: true, page: 0, size: 100 }),
          getUserAudits({ keyword: query.keyword || undefined, targetType: query.targetType || undefined, attentionOnly: true, page: 0, size: 100 }),
        ]);
        const combined = [
          ...(security?.content || []).map(entry => ({ ...entry, domain: 'security' })),
          ...(operations?.content || []).map(entry => ({ ...entry, domain: 'operations' })),
        ].sort((left, right) => new Date(right.createdAt) - new Date(left.createdAt));
        const start = query.page * query.size;
        data = {
          content: combined.slice(start, start + query.size),
          totalElements: combined.length,
          totalPages: Math.ceil(combined.length / query.size),
        };
      } else if (activeTab === 'security') {
        const response = await getAuthAudits({ keyword: query.keyword || undefined, page: query.page, size: query.size });
        data = { ...response, content: (response?.content || []).map(entry => ({ ...entry, domain: 'security' })) };
      } else {
        const response = await getUserAudits({ keyword: query.keyword || undefined, targetType: query.targetType || undefined, page: query.page, size: query.size });
        const visible = (response?.content || []).filter(entry => !TECHNICAL_ACTIONS.has(entry.action));
        data = { ...response, content: visible.map(entry => ({ ...entry, domain: 'operations' })) };
      }

      const accountIds = [...new Set((data.content || []).flatMap(entry => {
        const ids = [entry.accountId, entry.actorAccountId];
        if (['USER', 'CUSTOMER', 'EMPLOYEE'].includes(entry.targetType) && isNumeric(entry.targetId)) ids.push(Number(entry.targetId));
        return ids.filter(Boolean).map(Number);
      }))];
      const profileRows = await getUserProfiles(accountIds).catch(() => []);
      setProfiles(new Map((profileRows || []).map(profile => [Number(profile.accountId), profile])));
      setResult(data || { content: [], totalPages: 0, totalElements: 0 });
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải nhật ký hoạt động.' });
    }
  }, [activeTab, query]);

  useEffect(() => {
    // Remote audit state follows the current operational filters.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const personName = useCallback((accountId, fallback = 'Hệ thống') => {
    if (!accountId) return fallback;
    return profiles.get(Number(accountId))?.fullName || profiles.get(Number(accountId))?.email || `Người dùng ${accountId}`;
  }, [profiles]);

  const sentence = useCallback(entry => {
    const targetId = entry.accountId || (isNumeric(entry.targetId) ? Number(entry.targetId) : null);
    const target = personName(targetId, 'hệ thống');
    const actor = personName(entry.actorAccountId,
      entry.domain === 'operations' ? 'Hệ thống'
        : ADMINISTRATIVE_ACTIONS.has(entry.action) ? 'Quản trị viên' : target);
    const authSentences = {
      LOGIN_SUCCESS: `${target} đã đăng nhập vào hệ thống`,
      OAUTH2_LOGIN_SUCCESS: `${target} đã đăng nhập bằng tài khoản liên kết`,
      LOGIN_FAILED_INVALID_PASSWORD: `Có lần đăng nhập sai mật khẩu vào tài khoản của ${target}`,
      LOGIN_FAILED_INACTIVE_ACCOUNT: `${target} đã thử đăng nhập khi tài khoản chưa được kích hoạt`,
      LOGIN_FAILED_NOT_VERIFIED: `${target} đã thử đăng nhập khi email chưa được xác minh`,
      REFRESH_TOKEN_FAILED: `Phiên đăng nhập của ${target} đã hết hiệu lực và cần đăng nhập lại`,
      LOGOUT_SUCCESS: `${target} đã đăng xuất khỏi hệ thống`,
      LOGOUT_ALL_SUCCESS: `${target} đã đăng xuất khỏi tất cả thiết bị`,
      PASSWORD_CHANGED: `${target} đã đổi mật khẩu`,
      PASSWORD_RESET_SUCCESS: `${target} đã đặt lại mật khẩu`,
      EMPLOYEE_INVITATION_ACCEPTED: `${target} đã kích hoạt tài khoản và tự đặt mật khẩu`,
      CREATE_EMPLOYEE_INVITATION: `${actor} đã gửi lời mời sử dụng hệ thống cho ${target}`,
      RESEND_EMPLOYEE_INVITATION: `${actor} đã gửi lại lời mời kích hoạt cho ${target}`,
      ADMIN_SENT_PASSWORD_RESET: `${actor} đã gửi email đặt lại mật khẩu cho ${target}`,
      ADMIN_REVOKED_ALL_SESSIONS: `${actor} đã đăng xuất ${target} khỏi tất cả thiết bị`,
      UPDATE_ACCOUNT_STATUS: `${actor} đã thay đổi trạng thái truy cập của ${target}`,
      UPDATE_ACCOUNT_ROLE: `${actor} đã thay đổi vai trò hệ thống của ${target}`,
      UPDATE_ACCOUNT_ACCESS_PROFILE: `${actor} đã thay đổi nhóm nghiệp vụ của ${target}`,
      UPDATE_MANAGER_CINEMA_ASSIGNMENTS: `${actor} đã thay đổi phạm vi rạp của ${target}`,
    };
    if (entry.domain === 'security' && authSentences[entry.action]) return authSentences[entry.action];

    const operationSentences = {
      CUSTOMER_PROFILE_CREATED: `Hệ thống đã tạo hồ sơ khách hàng cho ${target} sau khi email được xác minh`,
      USER_PROFILE_CREATED: `Hệ thống đã tạo hồ sơ người dùng cho ${target}`,
      USER_PROFILE_UPDATED: `${actor} đã cập nhật thông tin hồ sơ của ${target}`,
      AVATAR_UPDATED: `${actor} đã cập nhật ảnh đại diện của ${target}`,
      AVATAR_DELETED: `${actor} đã xóa ảnh đại diện của ${target}`,
      EMPLOYEE_CREATED: `${actor} đã tạo hồ sơ nhân sự cho ${target}`,
      EMPLOYEE_UPDATED: `${actor} đã cập nhật hồ sơ nhân sự của ${target}`,
      EMPLOYEE_CINEMA_ASSIGNED: `${actor} đã thay đổi rạp làm việc của ${target}`,
      EMPLOYEE_RESIGNED: `${actor} đã ghi nhận ${target} nghỉ việc`,
      EMPLOYEE_TRANSFERRED: `${actor} đã điều chuyển công việc của ${target}`,
      ATTENDANCE_CORRECTED: `${actor} đã điều chỉnh chấm công`,
      WORK_SHIFT_CREATED: `${actor} đã tạo ca làm việc`,
      WORK_SHIFT_CANCELLED: `${actor} đã hủy ca làm việc`,
      PAYROLL_APPROVED: `${actor} đã duyệt bảng lương`,
      PAYROLL_CANCELLED: `${actor} đã hủy bảng lương`,
    };
    if (operationSentences[entry.action]) return operationSentences[entry.action];
    return `${actor} đã thực hiện: ${getAuditActionLabel(entry.action).toLocaleLowerCase('vi')}`;
  }, [personName]);

  const changeTab = tab => {
    setSelectedEntry(null);
    setReviewNote('');
    setQuery({ keyword: '', targetType: '', page: 0, size: PAGE_SIZE });
    setSearchParams({ tab });
  };

  const openEntry = entry => {
    setSelectedEntry(entry);
    setReviewNote(entry.reviewNote || '');
  };

  const markReviewed = async status => {
    if (!selectedEntry) return;
    setReviewing(true);
    try {
      const payload = { status, note: reviewNote.trim() || null };
      const updated = selectedEntry.domain === 'security'
        ? await reviewAuthAudit(selectedEntry.id, payload)
        : await reviewUserAudit(selectedEntry.id, payload);
      setSelectedEntry({ ...selectedEntry, ...updated });
      await load();
    } finally {
      setReviewing(false);
    }
  };

  const metrics = useMemo(() => {
    const content = result.content || [];
    return [
      { label: 'Nghiêm trọng', value: content.filter(entry => entry.severity === 'CRITICAL').length, hint: 'Cần xử lý ngay', icon: ShieldAlert, tone: 'red' },
      { label: 'Cần kiểm tra', value: activeTab === 'attention' ? result.totalElements : content.filter(entry => entry.severity === 'REVIEW').length, hint: 'Chưa được rà soát', icon: Eye, tone: 'amber' },
      { label: 'Đã kiểm tra', value: content.filter(entry => ['REVIEWED', 'RESOLVED'].includes(entry.reviewStatus)).length, hint: 'Có ghi nhận xử lý', icon: CheckCircle2, tone: 'green' },
      { label: 'Hoạt động đang xem', value: result.totalElements || 0, hint: activeTab === 'security' ? 'Đăng nhập & bảo mật' : 'Trong bộ lọc hiện tại', icon: Activity, tone: 'blue' },
    ];
  }, [activeTab, result]);

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <OperationsHeader
        eyebrow="Hệ thống · Theo dõi và kiểm soát"
        title="Nhật ký hoạt động"
        description="Mỗi dòng cho biết ai đã làm gì, kết quả ra sao và admin có cần xử lý hay không. Dữ liệu kỹ thuật chỉ xuất hiện khi mở chi tiết."
      />

      <div className="flex w-fit max-w-full gap-1 overflow-x-auto rounded-xl border border-white/10 bg-white/[0.025] p-1">
        <button type="button" onClick={() => changeTab('attention')} className={`inline-flex items-center gap-2 whitespace-nowrap rounded-lg px-4 py-2 text-sm font-bold ${activeTab === 'attention' ? 'bg-brand-orange text-black' : 'text-zinc-400 hover:text-white'}`}><ShieldAlert size={16} /> Cần kiểm tra</button>
        <button type="button" onClick={() => changeTab('security')} className={`inline-flex items-center gap-2 whitespace-nowrap rounded-lg px-4 py-2 text-sm font-bold ${activeTab === 'security' ? 'bg-brand-orange text-black' : 'text-zinc-400 hover:text-white'}`}><LockKeyhole size={16} /> Đăng nhập & bảo mật</button>
        <button type="button" onClick={() => changeTab('operations')} className={`inline-flex items-center gap-2 whitespace-nowrap rounded-lg px-4 py-2 text-sm font-bold ${activeTab === 'operations' ? 'bg-brand-orange text-black' : 'text-zinc-400 hover:text-white'}`}><Activity size={16} /> Hoạt động nghiệp vụ</button>
      </div>

      <MetricStrip items={metrics} />

      <ConsolePanel className="p-4">
        <div className={`grid gap-3 ${activeTab === 'security' ? 'md:grid-cols-[minmax(0,1fr)_auto] md:items-center' : 'md:grid-cols-2'}`}>
          <div className="relative"><Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-600" /><Input aria-label="Tìm nhật ký" className="pl-10" placeholder="Tìm theo người, hoạt động hoặc đối tượng…" value={query.keyword} onChange={event => setQuery(value => ({ ...value, keyword: event.target.value, page: 0 }))} /></div>
          {activeTab !== 'security' ? <Select aria-label="Lọc phân hệ" value={query.targetType} onChange={event => setQuery(value => ({ ...value, targetType: event.target.value, page: 0 }))}>{TARGET_TYPES.map(type => <option key={type || 'ALL'} value={type}>{type ? getTargetTypeLabel(type) : 'Tất cả phân hệ'}</option>)}</Select> : <div className="flex items-center gap-2 px-2 text-xs text-zinc-500"><Info size={15} /><span>Phiên hết hạn thông thường được lưu nhưng không tạo cảnh báo.</span></div>}
        </div>
      </ConsolePanel>

      <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!result.content?.length} emptyMessage={activeTab === 'attention' ? 'Không có việc nào cần kiểm tra' : 'Không có hoạt động phù hợp'} emptyDescription={activeTab === 'attention' ? 'Các hoạt động thông thường vẫn được lưu trong hai tab còn lại.' : 'Hãy thay đổi từ khóa hoặc bộ lọc và thử lại.'}>
        <ConsolePanel className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-white/[0.035] text-[10px] uppercase tracking-wider text-zinc-500"><tr><th className="p-4">Thời gian</th><th className="p-4">Điều gì đã xảy ra?</th><th className="p-4">Kết quả</th><th className="p-4">Mức độ</th><th className="p-4">Xử lý</th><th className="p-4 text-right">Chi tiết</th></tr></thead>
              <tbody className="divide-y divide-white/10">{result.content?.map(entry => <tr key={`${entry.domain}-${entry.id}`} className="hover:bg-white/[0.025]">
                <td className="whitespace-nowrap p-4 text-zinc-500">{formatDate(entry.createdAt)}</td>
                <td className="max-w-xl p-4"><p className="font-bold leading-6 text-white">{sentence(entry)}</p>{summarizeAuditDetails(entry.description || entry.details) ? <p className="mt-1 line-clamp-1 text-xs text-zinc-600">{summarizeAuditDetails(entry.description || entry.details)}</p> : null}</td>
                <td className="p-4 font-semibold text-zinc-300">{resultLabel(entry.result)}</td>
                <td className="p-4"><EventBadge entry={entry} /></td>
                <td className="p-4 text-xs font-semibold text-zinc-400">{reviewLabel(entry.reviewStatus)}</td>
                <td className="p-4 text-right"><button type="button" onClick={() => openEntry(entry)} className="inline-flex items-center gap-1.5 rounded-lg border border-white/10 px-3 py-2 text-xs font-bold text-zinc-300 hover:border-brand-orange/40 hover:text-brand-orange"><Eye size={14} /> Xem</button></td>
              </tr>)}</tbody>
            </table>
          </div>
          <ConsolePagination page={query.page} totalPages={result.totalPages} totalElements={result.totalElements} onPage={page => setQuery(value => ({ ...value, page }))} />
        </ConsolePanel>
      </AsyncState>

      <DetailDrawer open={Boolean(selectedEntry)} onClose={() => setSelectedEntry(null)} title="Chi tiết hoạt động" subtitle={selectedEntry ? formatDate(selectedEntry.createdAt) : ''}>
        {selectedEntry ? <div className="space-y-6">
          <div className="rounded-xl border border-brand-orange/20 bg-brand-orange/[0.07] p-4"><p className="text-[10px] font-black uppercase tracking-[0.18em] text-brand-orange">Điều gì đã xảy ra?</p><p className="mt-2 font-bold leading-6 text-white">{sentence(selectedEntry)}</p></div>
          <DetailGrid items={[
            { label: 'Kết quả', value: resultLabel(selectedEntry.result) },
            { label: 'Mức độ', value: severityLabel(selectedEntry.severity) },
            { label: 'Người thực hiện', value: personName(selectedEntry.actorAccountId, selectedEntry.domain === 'operations' ? 'Hệ thống tự động' : ADMINISTRATIVE_ACTIONS.has(selectedEntry.action) ? 'Quản trị viên' : personName(selectedEntry.accountId)) },
            { label: 'Đối tượng bị tác động', value: personName(selectedEntry.accountId || (isNumeric(selectedEntry.targetId) ? Number(selectedEntry.targetId) : null), getTargetTypeLabel(selectedEntry.targetType || selectedEntry.resource)) },
          ]} />
          {summarizeAuditDetails(selectedEntry.description || selectedEntry.details) ? <div><p className="mb-2 text-[10px] font-black uppercase tracking-[0.18em] text-zinc-600">Lý do hoặc ảnh hưởng</p><div className="rounded-xl border border-white/10 bg-white/[0.025] p-4 text-sm leading-6 text-zinc-300">{summarizeAuditDetails(selectedEntry.description || selectedEntry.details)}</div></div> : null}
          {selectedEntry.reviewStatus === 'UNREVIEWED' ? <div className="rounded-xl border border-amber-500/20 bg-amber-500/[0.06] p-4"><p className="font-bold text-amber-200">Trạng thái kiểm tra</p><p className="mt-1 text-xs leading-5 text-zinc-500">Ghi lại kết quả xác minh để admin khác biết sự kiện đã được xử lý.</p><label className="mt-3 block text-xs font-bold text-zinc-400">Ghi chú kiểm tra<Input className="mt-2" value={reviewNote} onChange={event => setReviewNote(event.target.value)} maxLength={500} placeholder="Ví dụ: Đã xác nhận với nhân viên, do nhập nhầm mật khẩu." /></label><div className="mt-3 flex flex-wrap gap-2"><button type="button" disabled={reviewing} onClick={() => markReviewed('REVIEWED')} className="rounded-lg bg-brand-orange px-3 py-2 text-xs font-black text-black disabled:opacity-40">Đánh dấu đã kiểm tra</button><button type="button" disabled={reviewing} onClick={() => markReviewed('RESOLVED')} className="rounded-lg border border-emerald-500/30 px-3 py-2 text-xs font-bold text-emerald-300 disabled:opacity-40">Đã giải quyết</button></div></div> : selectedEntry.reviewStatus !== 'NOT_REQUIRED' ? <div className="rounded-xl border border-emerald-500/20 bg-emerald-500/[0.06] p-4"><p className="font-bold text-emerald-300">{reviewLabel(selectedEntry.reviewStatus)}</p>{selectedEntry.reviewNote ? <p className="mt-2 text-sm leading-6 text-zinc-300">{selectedEntry.reviewNote}</p> : null}<p className="mt-2 text-xs text-zinc-600">{formatDate(selectedEntry.reviewedAt)}</p></div> : null}
          <details className="rounded-xl border border-white/10 p-4"><summary className="cursor-pointer text-xs font-bold text-zinc-500">Xem dữ liệu kỹ thuật</summary><DetailGrid items={[{ label: 'Mã sự kiện', value: selectedEntry.id }, { label: 'Mã hành động', value: selectedEntry.action }, { label: 'Địa chỉ mạng', value: ['0:0:0:0:0:0:0:1', '127.0.0.1', '::1'].includes(selectedEntry.ipAddress) ? 'Máy chủ nội bộ' : selectedEntry.ipAddress || 'Không ghi nhận' }, { label: 'Thiết bị', value: getDeviceLabel(selectedEntry.userAgent) }]} /><pre className="mt-3 overflow-x-auto whitespace-pre-wrap break-words text-[11px] leading-5 text-zinc-600">{JSON.stringify(selectedEntry, null, 2)}</pre></details>
        </div> : null}
      </DetailDrawer>
    </section>
  );
}
