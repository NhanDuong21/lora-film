import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useOutletContext, useSearchParams } from 'react-router-dom';
import {
  AlertTriangle,
  CheckCircle2,
  Info,
  KeyRound,
  LockKeyhole,
  LogOut,
  Mail,
  Search,
  Settings2,
  ShieldCheck,
  ShieldAlert,
  Users,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import {
  getAccount,
  getAccessProfiles,
  getAccounts,
  getAuthAudits,
  getPermissions,
  getRoles,
  revokeAccountSessions,
  sendAccountPasswordReset,
  updateAccessProfile,
  updateAccountAccessProfile,
  updateAccountRole,
  updateAccountStatus,
  updateManagerCinemaAssignments,
} from '../services/authAdminService';
import { getEmployees, getUserProfiles, searchUserProfiles } from '../services/userAdminService';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import {
  ConsolePagination,
  ConsolePanel,
  DetailDrawer,
  DetailGrid,
  MetricStrip,
  OperationsHeader,
} from '../components/OperationsConsole';
import { AsyncState, Input, Select, StatusBadge } from '@/components/common/ui/uiKit';
import useAdminAccess from '../hooks/useAdminAccess';
import {
  SYSTEM_ROLE_ORDER,
  getAuditActionLabel,
  getPermissionGroupKey,
  getPermissionGroupLabel,
  getPermissionLabel,
  getRolePresentation,
  normalizeRoleCode,
} from '../utils/systemPresentation';

const COMMON_EMPLOYEE_PERMISSION_CODES = new Set([
  'EMPLOYEE_DASHBOARD_VIEW',
  'EMPLOYEE_SCHEDULE_VIEW',
  'EMPLOYEE_LEAVE_CREATE',
  'EMPLOYEE_ATTENDANCE_VIEW',
  'EMPLOYEE_ATTENDANCE_UPDATE',
  'EMPLOYEE_PAYROLL_VIEW',
]);
const STATUS_LABELS = {
  ACTIVE: 'Hoạt động',
  INACTIVE: 'Chờ kích hoạt',
  LOCKED: 'Bị khóa bảo mật',
  DELETED: 'Đã thu hồi',
};

const ACCOUNT_CHANGE_ACTIONS = new Set([
  'CREATE_EMPLOYEE_INVITATION',
  'RESEND_EMPLOYEE_INVITATION',
  'ADMIN_SENT_PASSWORD_RESET',
  'ADMIN_REVOKED_ALL_SESSIONS',
  'UPDATE_ACCOUNT_STATUS',
  'UPDATE_ACCOUNT_ROLE',
  'UPDATE_ACCOUNT_ACCESS_PROFILE',
  'UPDATE_MANAGER_CINEMA_ASSIGNMENTS',
  'EMPLOYEE_INVITATION_ACCEPTED',
  'PASSWORD_CHANGED',
  'PASSWORD_RESET_SUCCESS',
]);

const formatDateTime = value => {
  if (!value) return 'Chưa ghi nhận';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Chưa ghi nhận';
  const time = date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false });
  const day = String(date.getDate()).padStart(2, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  return `${time} · ${day}/${month}/${date.getFullYear()}`;
};

const operationalStatus = account => {
  if (account.status === 'INACTIVE' && account.invitationExpiresAt
      && new Date(account.invitationExpiresAt) < new Date()) {
    return { label: 'Lời mời hết hạn', status: 'LOCKED', needsAttention: true };
  }
  return {
    label: STATUS_LABELS[account.status] || 'Cần kiểm tra',
    status: account.status,
    needsAttention: false,
  };
};

const needsAccountAttention = account => {
  if (operationalStatus(account).needsAttention) return true;
  const roleCode = normalizeRoleCode(account.role);
  if (roleCode === 'EMPLOYEE') {
    return !account.accessProfile || !account.person?.cinemaPublicId;
  }
  if (roleCode === 'MANAGER') return !(account.assignedCinemaPublicIds || []).length;
  return false;
};

const detailValues = value => Object.fromEntries(String(value || '')
  .split(',')
  .map(part => part.split('=').map(item => item.trim()))
  .filter(([key, item]) => key && item));

const personCode = account => {
  const roleCode = normalizeRoleCode(account.role);
  if (roleCode === 'CUSTOMER' && account.person?.customerCode) return account.person.customerCode;
  if (['ADMIN', 'MANAGER'].includes(roleCode)) {
    return `${roleCode === 'ADMIN' ? 'QT' : 'QL'}-${String(account.id).padStart(4, '0')}`;
  }
  if (account.person?.employeeCode) return account.person.employeeCode.replace(/^EMP[-_]?/i, 'NV-');
  const prefix = roleCode === 'CUSTOMER' ? 'KH' : 'NV';
  return `${prefix}-${String(account.id).padStart(4, '0')}`;
};

const orderedRoles = roles => SYSTEM_ROLE_ORDER
  .map(code => roles.find(role => normalizeRoleCode(role) === code))
  .filter(Boolean);

const accessPermissionCount = (role, profile) => {
  if (normalizeRoleCode(role) === 'EMPLOYEE') {
    return profile?.permissions?.length || profile?.permissionIds?.length || 0;
  }
  return role?.permissions?.length || role?.permissionIds?.length || 0;
};

const sameIds = (left, right) => {
  const a = [...left].map(Number).sort((x, y) => x - y);
  const b = [...right].map(Number).sort((x, y) => x - y);
  return a.length === b.length && a.every((value, index) => value === b[index]);
};

const sameStrings = (left = [], right = []) => {
  const a = [...left].map(String).sort();
  const b = [...right].map(String).sort();
  return a.length === b.length && a.every((value, index) => value === b[index]);
};

export default function AdminAccountPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const requestedTab = searchParams.get('tab');
  const activeTab = ['access', 'roles', 'permissions'].includes(requestedTab) ? 'access' : 'accounts';
  const accountScope = requestedTab === 'customers' ? 'CUSTOMER' : 'INTERNAL';
  const { user } = useAuth();
  const can = useAdminAccess();
  const canUpdateEmployeeAccess = can('ROLE_UPDATE');
  const [query, setQuery] = useState({
    keyword: '', roleId: '', status: '', cinemaPublicId: '', accessProfileId: '',
    attentionOnly: false, page: 0, size: 10,
  });
  const [accounts, setAccounts] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [roles, setRoles] = useState([]);
  const [accessProfiles, setAccessProfiles] = useState([]);
  const [cinemas, setCinemas] = useState([]);
  const [permissions, setPermissions] = useState([]);
  const [accountState, setAccountState] = useState({ loading: true, error: '' });
  const [accessState, setAccessState] = useState({ loading: true, error: '' });
  const [updatingId, setUpdatingId] = useState(null);
  const [selectedAccount, setSelectedAccount] = useState(null);
  const [drawerTab, setDrawerTab] = useState('overview');
  const [accountHistory, setAccountHistory] = useState([]);
  const [draftRoleId, setDraftRoleId] = useState('');
  const [draftAccessProfileId, setDraftAccessProfileId] = useState('');
  const [draftCinemaPublicIds, setDraftCinemaPublicIds] = useState([]);
  const [accessReason, setAccessReason] = useState('');
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);
  const [knownActiveAdministratorCount, setKnownActiveAdministratorCount] = useState(0);
  const accountLoadSequence = useRef(0);
  const [selectedAccessProfileId, setSelectedAccessProfileId] = useState('');
  const [selectedPermissionIds, setSelectedPermissionIds] = useState([]);
  const [permissionSearch, setPermissionSearch] = useState('');
  const [showTechnicalCodes, setShowTechnicalCodes] = useState(false);
  const [savingPermissions, setSavingPermissions] = useState(false);
  const outlet = useOutletContext();
  const confirmAction = outlet?.triggerConfirm || (() => Promise.resolve(true));
  const notify = outlet?.triggerToast || (() => undefined);

  const loadAccounts = useCallback(async () => {
    const loadSequence = ++accountLoadSequence.current;
    setAccountState({ loading: true, error: '' });
    try {
      if (accountScope === 'CUSTOMER' && !query.keyword.trim()) {
        setAccounts({ content: [], totalPages: 0, totalElements: 0 });
        setAccountState({ loading: false, error: '' });
        return;
      }

      let accountRows;
      if (accountScope === 'CUSTOMER') {
        const profileMatches = await searchUserProfiles(query.keyword.trim(), 50);
        const fetched = await Promise.all((profileMatches || []).map(profile =>
          getAccount(profile.accountId).catch(() => null)));
        accountRows = fetched.filter(account => account
          && normalizeRoleCode(account.role) === 'CUSTOMER'
          && (!query.status || account.status === query.status));
      } else {
        const data = await getAccounts({
          accountScope,
          roleId: query.roleId || undefined,
          status: query.status || undefined,
          page: 0,
          size: 100,
        });
        accountRows = data?.content || [];
        setKnownActiveAdministratorCount(current => Math.max(current, accountRows.filter(account => (
          normalizeRoleCode(account.role) === 'ADMIN'
          && account.status === 'ACTIVE'
          && account.enabled !== false
        )).length));
      }

      const accountIds = accountRows.map(account => account.id);
      const [profiles, employeePage] = await Promise.all([
        getUserProfiles(accountIds).catch(() => []),
        accountScope === 'INTERNAL'
          ? getEmployees({ page: 0, size: 100 }).catch(() => ({ content: [] }))
          : Promise.resolve({ content: [] }),
      ]);
      const profileById = new Map((profiles || []).map(profile => [Number(profile.accountId), profile]));
      const employeeById = new Map((employeePage?.content || []).map(employee => [Number(employee.accountId), employee]));
      let enriched = accountRows.map(account => ({
        ...account,
        person: employeeById.get(Number(account.id)) || profileById.get(Number(account.id)) || null,
      }));
      const keyword = query.keyword.trim().toLocaleLowerCase('vi');
      if (keyword && accountScope === 'INTERNAL') {
        enriched = enriched.filter(account => [
          account.person?.fullName,
          account.person?.employeeCode,
          account.person?.phoneNumber,
          account.email,
        ].filter(Boolean).join(' ').toLocaleLowerCase('vi').includes(keyword));
      }
      if (accountScope === 'INTERNAL' && query.cinemaPublicId) {
        enriched = enriched.filter(account => String(account.person?.cinemaPublicId) === query.cinemaPublicId
          || (account.assignedCinemaPublicIds || []).map(String).includes(query.cinemaPublicId));
      }
      if (accountScope === 'INTERNAL' && query.accessProfileId) {
        enriched = enriched.filter(account => String(account.accessProfile?.id || '') === query.accessProfileId);
      }
      if (accountScope === 'INTERNAL' && query.attentionOnly) {
        enriched = enriched.filter(needsAccountAttention);
      }
      const start = query.page * query.size;
      const totalElements = enriched.length;
      if (loadSequence !== accountLoadSequence.current) return;
      setAccounts({
        content: enriched.slice(start, start + query.size),
        allContent: enriched,
        totalElements,
        totalPages: Math.ceil(totalElements / query.size),
      });
      setAccountState({ loading: false, error: '' });
    } catch (error) {
      if (loadSequence !== accountLoadSequence.current) return;
      setAccountState({ loading: false, error: error?.message || 'Không thể tải danh sách tài khoản.' });
    }
  }, [accountScope, query]);

  const loadAccessConfiguration = useCallback(async (preferredProfileId = '') => {
    setAccessState({ loading: true, error: '' });
    try {
      const [roleData, permissionData, profileData, cinemaResponse] = await Promise.all([
        getRoles(),
        getPermissions(),
        getAccessProfiles(),
        adminCinemaService.getCinemas({ page: 0, size: 100, showDeleted: false, sort: 'name,asc' })
          .catch(() => null),
      ]);
      const nextRoles = roleData || [];
      const nextProfiles = profileData || [];
      setRoles(nextRoles);
      setPermissions(permissionData || []);
      setAccessProfiles(nextProfiles);
      setCinemas(cinemaResponse?.data?.data || []);
      const selectedProfile = nextProfiles.find(profile => String(profile.id) === String(preferredProfileId))
        || nextProfiles.find(profile => profile.code === 'BOX_OFFICE')
        || nextProfiles[0];
      setSelectedAccessProfileId(selectedProfile ? String(selectedProfile.id) : '');
      setSelectedPermissionIds((selectedProfile?.permissions || []).map(permission => permission.id));
      setAccessState({ loading: false, error: '' });
    } catch (error) {
      setAccessState({ loading: false, error: error?.message || 'Không thể tải cấu hình phân quyền.' });
    }
  }, []);

  useEffect(() => {
    // Remote account state is synchronized whenever the operator changes a filter.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadAccounts();
  }, [loadAccounts]);

  useEffect(() => {
    // Roles and permissions are synchronized once for the operational console.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadAccessConfiguration();
  }, [loadAccessConfiguration]);

  const systemRoles = useMemo(() => orderedRoles(roles), [roles]);
  const employeeRole = useMemo(
    () => roles.find(role => normalizeRoleCode(role) === 'EMPLOYEE'),
    [roles],
  );
  const cinemaByPublicId = useMemo(
    () => new Map(cinemas.map(cinema => [String(cinema.publicId), cinema])),
    [cinemas],
  );
  const selectedAccessProfile = useMemo(
    () => accessProfiles.find(profile => String(profile.id) === selectedAccessProfileId),
    [accessProfiles, selectedAccessProfileId],
  );
  const profilePermissionIds = useMemo(
    () => (selectedAccessProfile?.permissions || []).map(permission => permission.id),
    [selectedAccessProfile],
  );
  const permissionChanges = useMemo(() => {
    const current = new Set(profilePermissionIds.map(Number));
    const selected = new Set(selectedPermissionIds.map(Number));
    return {
      added: [...selected].filter(id => !current.has(id)).length,
      removed: [...current].filter(id => !selected.has(id)).length,
    };
  }, [profilePermissionIds, selectedPermissionIds]);

  const permissionGroups = useMemo(() => {
    const keyword = permissionSearch.trim().toLocaleLowerCase('vi');
    const groups = new Map();
    permissions
      .filter(permission => {
        if (COMMON_EMPLOYEE_PERMISSION_CODES.has(permission.code)) return false;
        if (!keyword) return true;
        return `${getPermissionLabel(permission)} ${permission.code} ${permission.module}`
          .toLocaleLowerCase('vi')
          .includes(keyword);
      })
      .forEach(permission => {
        const key = getPermissionGroupKey(permission);
        if (!groups.has(key)) groups.set(key, []);
        groups.get(key).push(permission);
      });
    return [...groups.entries()]
      .map(([key, items]) => ({
        key,
        label: getPermissionGroupLabel(key),
        items: items.sort((a, b) => getPermissionLabel(a).localeCompare(getPermissionLabel(b), 'vi')),
      }))
      .sort((a, b) => a.label.localeCompare(b.label, 'vi'));
  }, [permissionSearch, permissions]);

  const isCurrentAccount = account => {
    if (!account) return false;
    const knownIds = [user?.id, user?.accountId].filter(value => value !== undefined && value !== null).map(Number);
    return knownIds.includes(Number(account.id)) || Boolean(user?.email && user.email === account.email);
  };

  const openAccount = account => {
    setSelectedAccount(account);
    setDrawerTab('overview');
    setAccountHistory([]);
    setDraftRoleId(String(account.role?.id || ''));
    setDraftAccessProfileId(String(account.accessProfile?.id || ''));
    setDraftCinemaPublicIds(account.assignedCinemaPublicIds || []);
    setAccessReason('');
    getAuthAudits({ keyword: String(account.id), page: 0, size: 20 })
      .then(data => setAccountHistory((data?.content || []).filter(entry => (
        accountScope === 'INTERNAL'
          ? ACCOUNT_CHANGE_ACTIONS.has(entry.action)
          : entry.action.includes('LOGIN') || entry.action.includes('LOGOUT') || entry.action.includes('PASSWORD')
      ))))
      .catch(() => setAccountHistory([]));
  };

  const runSecurityAction = async (account, action) => {
    const config = {
      password: {
        title: 'Gửi email đặt lại mật khẩu?',
        message: `${account.email} sẽ nhận mã đặt lại mật khẩu có hiệu lực 15 phút. Admin không thể xem mật khẩu mới.`,
        confirmLabel: 'Gửi email',
        call: () => sendAccountPasswordReset(account.id),
        success: 'Đã gửi email đặt lại mật khẩu.',
      },
      sessions: {
        title: 'Đăng xuất khỏi tất cả thiết bị?',
        message: `${account.email} sẽ phải đăng nhập lại trên mọi thiết bị đang sử dụng.`,
        confirmLabel: 'Đăng xuất tất cả',
        call: () => revokeAccountSessions(account.id),
        success: 'Đã thu hồi toàn bộ phiên đăng nhập.',
      },
    }[action];
    if (!config) return;
    const approved = await confirmAction({ ...config, tone: 'warning' });
    if (!approved) return;
    setUpdatingId(account.id);
    try {
      await config.call();
      notify(config.success);
      await loadAccounts();
    } catch (error) {
      notify(error?.message || 'Không thể thực hiện thao tác bảo mật.', 'error');
    } finally {
      setUpdatingId(null);
    }
  };

  const handleAccountAccessSave = async () => {
    if (!selectedAccount || !draftRoleId) {
      setSelectedAccount(null);
      return;
    }
    const nextRole = roles.find(role => String(role.id) === draftRoleId);
    const nextRoleCode = normalizeRoleCode(nextRole);
    const currentRole = roles.find(role => String(role.id) === String(selectedAccount.role?.id)) || selectedAccount.role;
    const currentRoleCode = normalizeRoleCode(currentRole);
    const nextProfile = accessProfiles.find(profile => String(profile.id) === draftAccessProfileId);
    const currentPermissionCount = accessPermissionCount(currentRole, selectedAccount.accessProfile);
    const nextPermissionCount = accessPermissionCount(nextRole, nextProfile);
    const roleChanged = String(selectedAccount.role?.id || '') !== draftRoleId;
    const profileChanged = String(selectedAccount.accessProfile?.id || '') !== draftAccessProfileId;
    const cinemaAssignmentsChanged = !sameStrings(
      selectedAccount.assignedCinemaPublicIds || [],
      draftCinemaPublicIds,
    );
    if (!roleChanged
      && !(['EMPLOYEE', 'MANAGER'].includes(nextRoleCode) && profileChanged)
      && !(nextRoleCode === 'MANAGER' && cinemaAssignmentsChanged)) {
      setSelectedAccount(null);
      return;
    }
    if (nextRoleCode === 'EMPLOYEE' && !draftAccessProfileId) {
      notify('Vui lòng chọn nhóm nghiệp vụ cho nhân viên.', 'error');
      return;
    }
    if (nextRoleCode === 'MANAGER' && !draftCinemaPublicIds.length) {
      notify('Vui lòng chọn ít nhất một rạp cho tài khoản Quản lý rạp.', 'error');
      return;
    }
    if (isCurrentAccount(selectedAccount) && normalizeRoleCode(nextRole) !== 'ADMIN') {
      notify('Bạn không thể tự thu hồi quyền quản trị của tài khoản đang sử dụng.', 'error');
      return;
    }
    if (currentRoleCode !== 'ADMIN' && nextRoleCode === 'ADMIN' && accessReason.trim().length < 5) {
      notify('Vui lòng nhập lý do khi cấp quyền Quản trị hệ thống.', 'error');
      return;
    }
    const scopeAfter = nextRoleCode === 'MANAGER'
      ? selectedCinemaNames.join(' · ') || 'Chưa chọn rạp'
      : getRolePresentation(nextRole).scope;
    const impactSummary = `Vai trò: ${getRolePresentation(currentRole).label} → ${getRolePresentation(nextRole).label}. `
      + `Phạm vi sau khi lưu: ${scopeAfter}. `
      + `Quyền bị gỡ: ${Math.max(0, currentPermissionCount - nextPermissionCount)}. `
      + `Quyền được thêm: ${Math.max(0, nextPermissionCount - currentPermissionCount)}. `
      + 'Sau khi lưu, tài khoản sẽ bị đăng xuất khỏi tất cả thiết bị để quyền mới có hiệu lực.';
    const approved = await confirmAction({
      title: nextRoleCode === 'ADMIN' && currentRoleCode !== 'ADMIN'
        ? 'Xác nhận cấp quyền Quản trị hệ thống'
        : 'Xác nhận phân quyền tài khoản',
      message: impactSummary,
      confirmLabel: 'Lưu phân quyền',
      tone: 'warning',
    });
    if (!approved) return;
    setUpdatingId(selectedAccount.id);
    try {
      if (roleChanged) {
        await updateAccountRole(selectedAccount.id, Number(draftRoleId), accessReason.trim() || undefined);
      }
      if (nextRoleCode === 'EMPLOYEE' && (roleChanged || profileChanged)) {
        await updateAccountAccessProfile(selectedAccount.id, Number(draftAccessProfileId));
      }
      if (nextRoleCode === 'MANAGER' && (roleChanged || profileChanged)) {
        await updateAccountAccessProfile(
          selectedAccount.id,
          draftAccessProfileId ? Number(draftAccessProfileId) : null,
        );
      }
      if (nextRoleCode === 'MANAGER' && (roleChanged || cinemaAssignmentsChanged)) {
        await updateManagerCinemaAssignments(selectedAccount.id, draftCinemaPublicIds);
      }
      setSelectedAccount(null);
      await loadAccounts();
      notify('Đã cập nhật vai trò và nhóm nghiệp vụ của tài khoản.');
    } catch (error) {
      notify(error?.message || 'Không thể đổi vai trò tài khoản.', 'error');
    } finally {
      setUpdatingId(null);
    }
  };

  const handleStatusChange = async (account, newStatus) => {
    if (newStatus === 'LOCKED' && isCurrentAccount(account)) {
      notify('Bạn không thể tự khóa tài khoản đang sử dụng.', 'error');
      return;
    }
    const approved = await confirmAction({
      title: newStatus === 'LOCKED' ? 'Khóa tài khoản?' : 'Mở lại tài khoản?',
      message: newStatus === 'LOCKED'
        ? `${account.email} sẽ không thể đăng nhập cho đến khi được mở khóa.`
        : `${account.email} sẽ có thể đăng nhập trở lại theo vai trò hiện tại.`,
      confirmLabel: newStatus === 'LOCKED' ? 'Khóa tài khoản' : 'Mở khóa',
      tone: newStatus === 'LOCKED' ? 'danger' : 'warning',
    });
    if (!approved) return;
    setUpdatingId(account.id);
    try {
      await updateAccountStatus(account.id, newStatus);
      setSelectedAccount(null);
      await loadAccounts();
      notify(newStatus === 'LOCKED' ? 'Đã khóa tài khoản.' : 'Đã mở khóa tài khoản.');
    } catch (error) {
      notify(error?.message || 'Không thể đổi trạng thái tài khoản.', 'error');
    } finally {
      setUpdatingId(null);
    }
  };

  const togglePermission = permissionId => {
    setSelectedPermissionIds(current => current.includes(permissionId)
      ? current.filter(id => id !== permissionId)
      : [...current, permissionId]);
  };

  const togglePermissionGroup = items => {
    const itemIds = items.map(item => item.id);
    const allSelected = itemIds.every(id => selectedPermissionIds.includes(id));
    setSelectedPermissionIds(current => allSelected
      ? current.filter(id => !itemIds.includes(id))
      : [...new Set([...current, ...itemIds])]);
  };

  const selectAccessProfile = profile => {
    setSelectedAccessProfileId(String(profile.id));
    setSelectedPermissionIds((profile.permissions || []).map(permission => permission.id));
    setPermissionSearch('');
  };

  const saveAccessProfilePermissions = async () => {
    if (!selectedAccessProfile || sameIds(profilePermissionIds, selectedPermissionIds)) return;
    const approved = await confirmAction({
      title: `Áp dụng quyền cho nhóm “${selectedAccessProfile.name}”?`,
      message: `Thay đổi này sẽ cấp thêm ${permissionChanges.added} quyền và thu hồi ${permissionChanges.removed} quyền của ${selectedAccessProfile.assignedAccountCount || 0} tài khoản đang thuộc nhóm. Phiên đăng nhập của họ sẽ được làm mới.`,
      confirmLabel: 'Áp dụng thay đổi',
      tone: 'warning',
    });
    if (!approved) return;
    setSavingPermissions(true);
    try {
      await updateAccessProfile(selectedAccessProfile.id, {
        permissionIds: selectedPermissionIds,
      });
      await loadAccessConfiguration(selectedAccessProfile.id);
      notify(`Đã cập nhật quyền cho nhóm “${selectedAccessProfile.name}”.`);
    } catch (error) {
      notify(error?.message || 'Không thể cập nhật quyền nhân viên.', 'error');
    } finally {
      setSavingPermissions(false);
    }
  };

  const selectedRole = roles.find(role => String(role.id) === draftRoleId);
  const selectedRoleInfo = getRolePresentation(selectedRole);
  const selectedCurrentRole = roles.find(role => String(role.id) === String(selectedAccount?.role?.id))
    || selectedAccount?.role;
  const selectedNextProfile = accessProfiles.find(profile => String(profile.id) === draftAccessProfileId);
  const selectedCurrentPermissionCount = accessPermissionCount(selectedCurrentRole, selectedAccount?.accessProfile);
  const selectedNextPermissionCount = accessPermissionCount(selectedRole, selectedNextProfile);
  const selectedPermissionImpact = {
    removed: Math.max(0, selectedCurrentPermissionCount - selectedNextPermissionCount),
    added: Math.max(0, selectedNextPermissionCount - selectedCurrentPermissionCount),
  };
  const selectedCinemaNames = draftCinemaPublicIds
    .map(publicId => cinemaByPublicId.get(String(publicId))?.name)
    .filter(Boolean);
  const isAccessChanged = !sameIds(profilePermissionIds, selectedPermissionIds);
  const accountAccessChanged = Boolean(selectedAccount) && (
    String(selectedAccount.role?.id || '') !== draftRoleId
    || (normalizeRoleCode(selectedRole) === 'EMPLOYEE'
      && String(selectedAccount.accessProfile?.id || '') !== draftAccessProfileId)
    || (normalizeRoleCode(selectedRole) === 'MANAGER'
      && !sameStrings(selectedAccount.assignedCinemaPublicIds || [], draftCinemaPublicIds))
  );
  const accountSummary = useMemo(() => {
    const rows = accounts.allContent || accounts.content || [];
    return {
      active: rows.filter(account => account.status === 'ACTIVE').length,
      pending: rows.filter(account => account.status === 'INACTIVE').length,
      attention: rows.filter(needsAccountAttention).length,
      locked: rows.filter(account => account.status === 'LOCKED').length,
    };
  }, [accounts]);
  const allInternalAccounts = accounts.allContent || accounts.content || [];
  const activeAdministratorCount = Math.max(knownActiveAdministratorCount, allInternalAccounts.filter(account => (
    normalizeRoleCode(account.role) === 'ADMIN'
    && account.status === 'ACTIVE'
    && account.enabled !== false
  )).length);
  const selectedIsLastActiveAdministrator = Boolean(selectedAccount)
    && normalizeRoleCode(selectedAccount.role) === 'ADMIN'
    && selectedAccount.status === 'ACTIVE'
    && activeAdministratorCount <= 1;
  const selectedRoleProtectionReason = isCurrentAccount(selectedAccount)
    ? 'Bạn không thể thay đổi vai trò của tài khoản đang sử dụng.'
    : selectedIsLastActiveAdministrator
      ? 'Không thể thay đổi vai trò vì đây là quản trị viên hoạt động cuối cùng.'
      : '';
  const selectedSecurityProtectionReason = isCurrentAccount(selectedAccount)
    ? 'Bạn không thể tạm khóa hoặc đăng xuất tài khoản đang sử dụng khỏi tất cả thiết bị.'
    : selectedIsLastActiveAdministrator
      ? 'Không thể khóa vì đây là quản trị viên hoạt động cuối cùng.'
      : '';

  const applyMetricFilter = (status, attentionOnly = false) => {
    setQuery(value => ({ ...value, status, attentionOnly, page: 0 }));
  };

  const changeAccountScope = scope => {
    accountLoadSequence.current += 1;
    setSelectedAccount(null);
    setAccounts({ content: [], totalPages: 0, totalElements: 0 });
    setQuery(value => ({
      ...value,
      keyword: '', roleId: '', status: '', cinemaPublicId: '', accessProfileId: '',
      attentionOnly: false, page: 0,
    }));
    setSearchParams({ tab: scope === 'CUSTOMER' ? 'customers' : 'accounts' });
  };

  const presentAccountHistory = entry => {
    const values = detailValues(entry.description);
    const actorAccount = allInternalAccounts.find(account => Number(account.id) === Number(entry.actorAccountId));
    const actor = actorAccount?.person?.fullName || actorAccount?.email || 'Quản trị viên';
    const beforeRole = values.before ? getRolePresentation({ code: values.before }).label : null;
    const afterRole = values.after ? getRolePresentation({ code: values.after }).label : null;
    const titles = {
      UPDATE_ACCOUNT_ROLE: `${actor} đã thay đổi vai trò tài khoản`,
      UPDATE_ACCOUNT_ACCESS_PROFILE: `${actor} đã thay đổi nhóm nghiệp vụ`,
      UPDATE_MANAGER_CINEMA_ASSIGNMENTS: `${actor} đã thay đổi phạm vi rạp`,
      UPDATE_ACCOUNT_STATUS: `${actor} đã thay đổi trạng thái truy cập`,
      CREATE_EMPLOYEE_INVITATION: `${actor} đã tạo hồ sơ và gửi lời mời`,
      RESEND_EMPLOYEE_INVITATION: `${actor} đã gửi lại lời mời kích hoạt`,
      ADMIN_SENT_PASSWORD_RESET: `${actor} đã gửi email đặt lại mật khẩu`,
      ADMIN_REVOKED_ALL_SESSIONS: `${actor} đã đăng xuất tài khoản khỏi tất cả thiết bị`,
      EMPLOYEE_INVITATION_ACCEPTED: 'Nhân viên đã chấp nhận lời mời và kích hoạt tài khoản',
      PASSWORD_CHANGED: 'Chủ tài khoản đã đổi mật khẩu',
      PASSWORD_RESET_SUCCESS: 'Chủ tài khoản đã đặt lại mật khẩu',
    };
    let change = '';
    if (entry.action === 'UPDATE_ACCOUNT_ROLE' && beforeRole && afterRole) change = `Trước: ${beforeRole} · Sau: ${afterRole}`;
    else if (entry.action === 'UPDATE_ACCOUNT_STATUS' && values.before && values.after) change = `Trước: ${STATUS_LABELS[values.before] || values.before} · Sau: ${STATUS_LABELS[values.after] || values.after}`;
    else if (values.before || values.after) change = `Trước: ${values.before || 'Chưa thiết lập'} · Sau: ${values.after || 'Chưa thiết lập'}`;
    if (values.reason) change = `${change}${change ? ' · ' : ''}Lý do: ${values.reason}`;
    return { title: titles[entry.action] || getAuditActionLabel(entry.action), change };
  };

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <OperationsHeader
        eyebrow="Hệ thống · Tài khoản và truy cập"
        title="Tài khoản & quyền truy cập"
        description="Theo dõi trạng thái truy cập và kiểm soát đúng phạm vi công việc. Tiếp nhận hoặc gửi lại lời mời được thực hiện tại Hồ sơ nhân viên."
        actions={activeTab === 'accounts' && accountScope === 'INTERNAL' ? (
          <Link to="/admin/staff?onboarding=new" className="inline-flex items-center gap-2 rounded-xl border border-brand-orange/30 bg-brand-orange/10 px-4 py-2.5 text-sm font-black text-brand-orange hover:bg-brand-orange/15">
            <Users size={17} /> Tiếp nhận nhân viên tại Hồ sơ nhân viên
          </Link>
        ) : null}
      />

      <div className="flex w-fit gap-1 rounded-xl border border-white/10 bg-white/[0.025] p-1">
        <button type="button" onClick={() => changeAccountScope('INTERNAL')} className={`rounded-lg px-4 py-2 text-sm font-bold transition-colors ${activeTab === 'accounts' && accountScope === 'INTERNAL' ? 'bg-brand-orange text-black' : 'text-zinc-400 hover:text-white'}`}>
          Tài khoản nội bộ
        </button>
        <button type="button" onClick={() => changeAccountScope('CUSTOMER')} className={`rounded-lg px-4 py-2 text-sm font-bold transition-colors ${activeTab === 'accounts' && accountScope === 'CUSTOMER' ? 'bg-brand-orange text-black' : 'text-zinc-400 hover:text-white'}`}>
          Tra cứu khách hàng
        </button>
        <button type="button" onClick={() => setSearchParams({ tab: 'access' })} className={`rounded-lg px-4 py-2 text-sm font-bold transition-colors ${activeTab === 'access' ? 'bg-brand-orange text-black' : 'text-zinc-400 hover:text-white'}`}>
          Nhóm nghiệp vụ & quyền
        </button>
      </div>

      {activeTab === 'accounts' ? (
        <>
          {accountScope === 'INTERNAL' ? (
            <MetricStrip items={[
              { label: 'Đang hoạt động', value: accountSummary.active, hint: 'Có thể đăng nhập', icon: Users, tone: 'green', onClick: () => applyMetricFilter('ACTIVE') },
              { label: 'Chờ kích hoạt', value: accountSummary.pending, hint: 'Đã gửi lời mời', icon: Mail, tone: 'orange', onClick: () => applyMetricFilter('INACTIVE') },
              { label: 'Cần kiểm tra', value: accountSummary.attention, hint: 'Lời mời hết hạn, thiếu phạm vi hoặc quyền bất thường', icon: ShieldAlert, tone: 'amber', onClick: () => applyMetricFilter('', true) },
              { label: 'Bị khóa bảo mật', value: accountSummary.locked, hint: 'Không thể đăng nhập', icon: LockKeyhole, tone: 'red', onClick: () => applyMetricFilter('LOCKED') },
            ]} />
          ) : (
            <ConsolePanel className="flex items-start gap-3 p-5 text-sm leading-6 text-zinc-400">
              <Info size={18} className="mt-0.5 shrink-0 text-brand-orange" />
              <p>Nhập tên, email hoặc số điện thoại để tìm khách hàng cần hỗ trợ. Hệ thống không tải toàn bộ khách hàng vào khu vực phân quyền nội bộ.</p>
            </ConsolePanel>
          )}

          <div className={`grid gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/50 p-4 ${accountScope === 'INTERNAL' ? 'md:grid-cols-4' : 'md:grid-cols-2'}`}>
            <Input aria-label="Tìm tài khoản" placeholder={accountScope === 'INTERNAL' ? 'Tìm theo tên, email, mã nhân viên hoặc số điện thoại…' : 'Tìm khách hàng theo tên, email hoặc số điện thoại…'} value={query.keyword} onChange={event => setQuery(value => ({ ...value, keyword: event.target.value, page: 0 }))} />
            {accountScope === 'INTERNAL' ? <Select aria-label="Lọc vai trò" value={query.roleId} onChange={event => setQuery(value => ({ ...value, roleId: event.target.value, page: 0 }))}>
              <option value="">Tất cả vai trò</option>
              {systemRoles.filter(role => normalizeRoleCode(role) !== 'CUSTOMER').map(role => <option key={role.id} value={role.id}>{getRolePresentation(role).label}</option>)}
            </Select> : null}
            <Select aria-label="Lọc trạng thái" value={query.status} onChange={event => setQuery(value => ({ ...value, status: event.target.value, page: 0 }))}>
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Hoạt động</option>
              <option value="INACTIVE">Chờ kích hoạt</option>
              <option value="LOCKED">Bị khóa bảo mật</option>
              <option value="DELETED">Đã thu hồi</option>
            </Select>
            {accountScope === 'INTERNAL' ? <button type="button" onClick={() => setShowAdvancedFilters(value => !value)} className={`inline-flex items-center justify-center gap-2 rounded-xl border px-4 text-sm font-bold ${showAdvancedFilters || query.cinemaPublicId || query.accessProfileId || query.attentionOnly ? 'border-brand-orange/50 bg-brand-orange/10 text-brand-orange' : 'border-white/10 text-zinc-300'}`}><Settings2 size={16} /> Bộ lọc</button> : null}
          </div>

          {accountScope === 'INTERNAL' && (showAdvancedFilters || query.cinemaPublicId || query.accessProfileId || query.attentionOnly) ? <ConsolePanel className="grid gap-3 p-4 md:grid-cols-3">
            <Select aria-label="Lọc rạp làm việc" value={query.cinemaPublicId} onChange={event => setQuery(value => ({ ...value, cinemaPublicId: event.target.value, page: 0 }))}><option value="">Tất cả rạp làm việc</option>{cinemas.map(cinema => <option key={cinema.publicId} value={cinema.publicId}>{cinema.name}</option>)}</Select>
            <Select aria-label="Lọc nhóm nghiệp vụ" value={query.accessProfileId} onChange={event => setQuery(value => ({ ...value, accessProfileId: event.target.value, page: 0 }))}><option value="">Tất cả nhóm nghiệp vụ</option>{accessProfiles.map(profile => <option key={profile.id} value={profile.id}>{profile.name}</option>)}</Select>
            <button type="button" onClick={() => setQuery(value => ({ ...value, roleId: '', status: '', cinemaPublicId: '', accessProfileId: '', attentionOnly: false, page: 0 }))} className="rounded-xl border border-white/10 px-4 py-2 text-sm font-bold text-zinc-400 hover:text-white">Xóa bộ lọc</button>
          </ConsolePanel> : null}

          <AsyncState loading={accountState.loading} error={accountState.error} onRetry={loadAccounts} empty={!accounts.content?.length} emptyMessage={accountScope === 'CUSTOMER' && !query.keyword ? 'Tìm khách hàng khi cần hỗ trợ' : 'Không tìm thấy tài khoản'} emptyDescription={accountScope === 'CUSTOMER' && !query.keyword ? 'Nhập tên, email hoặc số điện thoại ở ô tìm kiếm phía trên.' : 'Hãy thay đổi từ khóa hoặc bộ lọc và thử lại.'}>
            <ConsolePanel className="overflow-hidden">
              <div className="overflow-x-auto">
                {accountScope === 'CUSTOMER' ? (
                  <table className="min-w-full text-left text-sm">
                    <thead className="bg-white/[0.035] text-[10px] uppercase tracking-wider text-zinc-500"><tr><th className="p-4">Khách hàng</th><th className="p-4">Liên hệ</th><th className="p-4">Xác minh</th><th className="p-4">Trạng thái tài khoản</th><th className="p-4">Hoạt động gần nhất</th><th className="p-4 text-right">Hỗ trợ</th></tr></thead>
                    <tbody className="divide-y divide-white/10">{accounts.content?.map(account => {
                      const displayName = account.person?.fullName || account.email?.split('@')[0] || 'Chưa cập nhật họ tên';
                      const accessStatus = operationalStatus(account);
                      return <tr key={account.id} className="hover:bg-white/[0.025]">
                        <td className="p-4"><p className="font-bold text-white">{displayName}</p><p className="mt-1 text-[10px] text-zinc-600">{personCode(account)}</p></td>
                        <td className="p-4"><p className="font-semibold text-zinc-300">{account.email}</p><p className="mt-1 text-xs text-zinc-600">{account.person?.phoneNumber || 'Chưa cập nhật số điện thoại'}</p></td>
                        <td className="p-4"><p className={account.status === 'INACTIVE' ? 'font-semibold text-amber-300' : 'font-semibold text-emerald-300'}>{account.status === 'INACTIVE' ? 'Chưa hoàn tất xác minh' : 'Email đã xác minh'}</p></td>
                        <td className="p-4"><StatusBadge status={accessStatus.status} label={accessStatus.label} /></td>
                        <td className="p-4 text-zinc-400">{account.lastLoginAt ? formatDateTime(account.lastLoginAt) : 'Chưa đăng nhập'}</td>
                        <td className="p-4 text-right"><button type="button" onClick={() => openAccount(account)} className="inline-flex items-center gap-1.5 rounded-lg border border-white/10 px-3 py-2 text-xs font-bold text-zinc-300 hover:border-brand-orange/40 hover:text-brand-orange"><Settings2 size={14} /> Mở hồ sơ khách hàng</button></td>
                      </tr>;
                    })}</tbody>
                  </table>
                ) : (
                  <table className="min-w-full text-left text-sm">
                    <thead className="bg-white/[0.035] text-[10px] uppercase tracking-wider text-zinc-500"><tr><th className="p-4">Nhân sự</th><th className="p-4">Công việc</th><th className="p-4">Phạm vi làm việc</th><th className="p-4">Trạng thái truy cập</th><th className="p-4">Hoạt động gần nhất</th><th className="p-4 text-right">Thao tác</th></tr></thead>
                    <tbody className="divide-y divide-white/10">{accounts.content?.map(account => {
                      const role = roles.find(item => item.id === account.role?.id) || account.role;
                      const roleInfo = getRolePresentation(role);
                      const accessStatus = operationalStatus(account);
                      const displayName = account.person?.fullName || account.email?.split('@')[0] || 'Chưa cập nhật họ tên';
                      const cinemaName = cinemaByPublicId.get(String(account.person?.cinemaPublicId))?.name;
                      return <tr key={account.id} className="hover:bg-white/[0.025]">
                        <td className="p-4"><p className="font-bold text-white">{displayName}</p><p className="mt-1 text-[10px] text-zinc-600">{personCode(account)} · {account.email}</p></td>
                        <td className="p-4"><p className="font-semibold text-zinc-200">{normalizeRoleCode(role) === 'EMPLOYEE' ? account.accessProfile?.name || account.person?.positionName || 'Chưa phân nhóm nghiệp vụ' : roleInfo.label}</p><p className="mt-1 text-xs text-zinc-600">{account.person?.positionName && normalizeRoleCode(role) === 'EMPLOYEE' ? account.person.positionName : roleInfo.description}</p></td>
                        <td className="p-4">{normalizeRoleCode(role) === 'EMPLOYEE' ? <><p className={`font-bold ${cinemaName ? 'text-zinc-200' : 'text-amber-300'}`}>{cinemaName || 'Chưa phân công rạp'}</p><p className="mt-1 max-w-xs text-xs text-zinc-600">{account.person?.departmentName || 'Phạm vi theo hồ sơ nhân sự'}</p></> : normalizeRoleCode(role) === 'MANAGER' ? <><p className={`font-bold ${(account.assignedCinemaPublicIds || []).length ? 'text-zinc-200' : 'text-amber-300'}`}>{(account.assignedCinemaPublicIds || []).length ? `${account.assignedCinemaPublicIds.length} rạp được phân công` : 'Chưa phân công rạp'}</p><p className="mt-1 max-w-xs text-xs text-zinc-600">{(account.assignedCinemaPublicIds || []).map(publicId => cinemaByPublicId.get(String(publicId))?.name).filter(Boolean).join(' · ') || 'Cần chọn rạp trước khi bàn giao tài khoản.'}</p></> : <><p className="text-zinc-300">{roleInfo.scope}</p><p className="mt-1 max-w-xs text-xs text-zinc-600">{roleInfo.description}</p></>}</td>
                        <td className="p-4"><StatusBadge status={accessStatus.status} label={accessStatus.label} />{account.status === 'INACTIVE' ? <p className="mt-1.5 text-[10px] text-zinc-600">Hết hạn {formatDateTime(account.invitationExpiresAt)}</p> : null}</td>
                        <td className="p-4"><p className="font-semibold text-zinc-300">{account.lastLoginAt ? formatDateTime(account.lastLoginAt) : 'Chưa đăng nhập'}</p><p className="mt-1 text-xs text-zinc-600">{account.lastLoginAt ? 'Lần đăng nhập gần nhất' : 'Chưa có hoạt động'}</p></td>
                        <td className="p-4 text-right"><button type="button" onClick={() => openAccount(account)} className="inline-flex items-center gap-1.5 rounded-lg border border-white/10 px-3 py-2 text-xs font-bold text-zinc-300 hover:border-brand-orange/40 hover:text-brand-orange"><Settings2 size={14} /> Chi tiết</button></td>
                      </tr>;
                    })}</tbody>
                  </table>
                )}
              </div>
              <ConsolePagination page={query.page} totalPages={accounts.totalPages} totalElements={accounts.totalElements} onPage={page => setQuery(value => ({ ...value, page }))} />
            </ConsolePanel>
          </AsyncState>
        </>
      ) : (
        <AsyncState loading={accessState.loading} error={accessState.error} onRetry={loadAccessConfiguration} empty={!roles.length || !accessProfiles.length} emptyMessage="Chưa có cấu hình nhóm nghiệp vụ">
          <div className="space-y-6">
            <ConsolePanel className="grid gap-px overflow-hidden bg-white/10 lg:grid-cols-3">
              {[
                { step: '01', title: 'Vai trò hệ thống', description: 'Xác định đây là Quản trị, Quản lý rạp, Nhân viên hay Khách hàng.' },
                { step: '02', title: 'Nhóm nghiệp vụ', description: 'Với Nhân viên, chọn công việc như Bán vé, Soát vé hoặc Kế toán.' },
                { step: '03', title: 'Quyền thực hiện', description: 'Hệ thống tự cộng quyền cá nhân và quyền của nhóm nghiệp vụ.' },
              ].map(item => (
                <div key={item.step} className="bg-[#0b0b0e] p-5">
                  <span className="text-[10px] font-black tracking-[0.2em] text-brand-orange">BƯỚC {item.step}</span>
                  <p className="mt-2 font-black text-white">{item.title}</p>
                  <p className="mt-1 text-xs leading-5 text-zinc-500">{item.description}</p>
                </div>
              ))}
            </ConsolePanel>

            <div>
              <div className="mb-3 flex items-center gap-2"><ShieldCheck className="text-brand-orange" size={19} /><h2 className="text-lg font-black text-white">Bốn vai trò cố định của hệ thống</h2></div>
              <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                {systemRoles.map(role => {
                  const code = normalizeRoleCode(role);
                  const info = getRolePresentation(role);
                  return (
                    <article key={role.id} className={`rounded-2xl border p-5 ${code === 'EMPLOYEE' ? 'border-brand-orange/40 bg-brand-orange/[0.06]' : 'border-white/10 bg-white/[0.025]'}`}>
                      <div className="flex items-start justify-between gap-3">
                        <div><p className="text-[10px] font-black tracking-[0.18em] text-zinc-600">{code}</p><h3 className="mt-1 font-black text-white">{info.label}</h3></div>
                        {code === 'EMPLOYEE' ? <span className="rounded-full bg-brand-orange px-2 py-1 text-[9px] font-black text-black">THEO NGHIỆP VỤ</span> : <LockKeyhole size={16} className="text-zinc-600" />}
                      </div>
                      <p className="mt-3 text-xs leading-5 text-zinc-400">{info.description}</p>
                      <p className="mt-3 border-t border-white/10 pt-3 text-[11px] font-semibold text-zinc-500">{info.scope}</p>
                    </article>
                  );
                })}
              </div>
            </div>

            <ConsolePanel className="p-5">
              <div className="flex items-start gap-3">
                <CheckCircle2 size={19} className="mt-0.5 shrink-0 text-emerald-400" />
                <div>
                  <h2 className="font-black text-white">Quyền cá nhân dùng chung cho mọi nhân viên</h2>
                  <p className="mt-1 text-sm leading-6 text-zinc-400">Nhân viên luôn được xem lịch ca, chấm công, gửi đơn nghỉ và xem bảng lương của chính mình. Admin không cần cấu hình lại các quyền an toàn này.</p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {(employeeRole?.permissions || []).map(permission => <span key={permission.id} className="rounded-full border border-emerald-500/20 bg-emerald-500/10 px-3 py-1 text-xs font-semibold text-emerald-200">{getPermissionLabel(permission)}</span>)}
                  </div>
                </div>
              </div>
            </ConsolePanel>

            <div>
              <div className="mb-3 flex flex-col gap-2 md:flex-row md:items-end md:justify-between">
                <div><div className="flex items-center gap-2"><Users size={19} className="text-brand-orange" /><h2 className="text-lg font-black text-white">Chọn nhóm nghiệp vụ cần cấu hình</h2></div><p className="mt-1 text-sm text-zinc-500">Mỗi nhân viên chỉ thuộc một nhóm. Chọn thẻ để xem và chỉnh quyền công việc của cả nhóm.</p></div>
                <span className="text-xs text-zinc-600">
                  {accessProfiles.filter(profile => profile.code !== 'GENERAL_STAFF').length} nhóm công việc
                  {accessProfiles.some(profile => profile.code === 'GENERAL_STAFF') ? ' · 1 nhóm chờ phân loại' : ''}
                </span>
              </div>
              <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                {accessProfiles.map(profile => {
                  const selected = String(profile.id) === selectedAccessProfileId;
                  const unassigned = profile.code === 'GENERAL_STAFF';
                  return (
                    <button key={profile.id} type="button" onClick={() => selectAccessProfile(profile)} className={`rounded-2xl border p-5 text-left transition-colors ${selected ? 'border-brand-orange bg-brand-orange/[0.08]' : unassigned ? 'border-amber-500/20 bg-amber-500/[0.04] hover:border-amber-500/40' : 'border-white/10 bg-white/[0.025] hover:border-white/20'}`}>
                      <span className="flex items-start justify-between gap-3"><span className="font-black text-white">{profile.name}</span>{selected ? <CheckCircle2 size={18} className="shrink-0 text-brand-orange" /> : null}</span>
                      <span className="mt-2 block min-h-10 text-xs leading-5 text-zinc-500">{profile.description}</span>
                      <span className="mt-4 flex items-center justify-between border-t border-white/10 pt-3 text-[11px]"><span className="font-bold text-zinc-400">{profile.permissions?.length || 0} quyền công việc</span><span className={unassigned && profile.assignedAccountCount ? 'font-black text-amber-300' : 'text-zinc-600'}>{profile.assignedAccountCount || 0} tài khoản</span></span>
                    </button>
                  );
                })}
              </div>
            </div>

            {selectedAccessProfile ? (
              <ConsolePanel className="overflow-hidden">
                <header className="flex flex-col gap-4 border-b border-white/10 p-5 xl:flex-row xl:items-start xl:justify-between">
                  <div className="max-w-2xl">
                    <p className="text-[10px] font-black uppercase tracking-[0.18em] text-brand-orange">Đang cấu hình</p>
                    <h2 className="mt-1 text-xl font-black text-white">{selectedAccessProfile.name}</h2>
                    <p className="mt-2 text-sm leading-6 text-zinc-400">{selectedAccessProfile.description}</p>
                  </div>
                  <div className="rounded-xl border border-amber-500/20 bg-amber-500/10 px-4 py-3 text-xs leading-5 text-amber-200"><div className="flex items-start gap-2"><AlertTriangle size={16} className="mt-0.5 shrink-0" /><span>Chỉ chọn quyền cần cho công việc. Thay đổi sẽ áp dụng cho toàn bộ nhân viên trong nhóm.</span></div></div>
                </header>

                <div className="flex flex-col gap-3 border-b border-white/10 p-4 md:flex-row md:items-center md:justify-between">
                  <div className="relative w-full max-w-lg"><Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-600" /><Input aria-label="Tìm quyền công việc" className="pl-10" placeholder="Tìm theo công việc, ví dụ: bán vé, đối soát…" value={permissionSearch} onChange={event => setPermissionSearch(event.target.value)} /></div>
                  <label className="inline-flex items-center gap-2 text-xs text-zinc-500"><input type="checkbox" checked={showTechnicalCodes} onChange={event => setShowTechnicalCodes(event.target.checked)} className="rounded border-zinc-700 bg-zinc-900 text-brand-orange focus:ring-brand-orange" /> Hiện mã kỹ thuật</label>
                </div>

                <div className="grid gap-4 p-5 xl:grid-cols-2">
                  {permissionGroups.map(group => {
                    const selectedCount = group.items.filter(item => selectedPermissionIds.includes(item.id)).length;
                    const allSelected = selectedCount === group.items.length;
                    return (
                      <section key={group.key} className="overflow-hidden rounded-xl border border-white/10 bg-white/[0.02]">
                        <header className="flex items-center justify-between border-b border-white/10 px-4 py-3"><div><h3 className="text-sm font-black text-white">{group.label}</h3><p className="mt-0.5 text-[10px] text-zinc-600">Đã chọn {selectedCount}/{group.items.length}</p></div><button type="button" disabled={!canUpdateEmployeeAccess} onClick={() => togglePermissionGroup(group.items)} className="text-xs font-bold text-brand-orange hover:underline disabled:text-zinc-700">{allSelected ? 'Bỏ chọn nhóm' : 'Chọn cả nhóm'}</button></header>
                        <div className="divide-y divide-white/5">
                          {group.items.map(permission => <label key={permission.id} className="flex cursor-pointer items-start gap-3 px-4 py-3 hover:bg-white/[0.025]"><input type="checkbox" disabled={!canUpdateEmployeeAccess} checked={selectedPermissionIds.includes(permission.id)} onChange={() => togglePermission(permission.id)} className="mt-0.5 rounded border-zinc-700 bg-zinc-900 text-brand-orange focus:ring-brand-orange disabled:opacity-40" /><span className="min-w-0"><span className="block text-sm font-semibold text-zinc-200">{getPermissionLabel(permission)}</span>{showTechnicalCodes ? <span className="mt-1 block font-mono text-[10px] text-zinc-600">{permission.code}</span> : null}</span></label>)}
                        </div>
                      </section>
                    );
                  })}
                </div>

                <footer className="sticky bottom-0 flex flex-col gap-3 border-t border-white/10 bg-[#0b0b0e]/95 p-5 backdrop-blur md:flex-row md:items-center md:justify-between">
                  <div className="flex items-center gap-2 text-sm">{isAccessChanged ? <AlertTriangle size={17} className="text-amber-400" /> : <CheckCircle2 size={17} className="text-emerald-400" />}<span className={isAccessChanged ? 'text-amber-200' : 'text-zinc-500'}>{isAccessChanged ? `Chưa lưu: cấp thêm ${permissionChanges.added}, thu hồi ${permissionChanges.removed} quyền` : `${selectedPermissionIds.length} quyền công việc đang áp dụng`}</span></div>
                  {canUpdateEmployeeAccess ? <button type="button" disabled={!isAccessChanged || savingPermissions} onClick={saveAccessProfilePermissions} className="rounded-xl bg-brand-orange px-5 py-2.5 text-sm font-black text-black hover:bg-orange-500 disabled:cursor-not-allowed disabled:opacity-40">{savingPermissions ? 'Đang áp dụng…' : 'Lưu quyền của nhóm'}</button> : <span className="text-xs text-zinc-600">Bạn chỉ có quyền xem cấu hình này.</span>}
                </footer>
              </ConsolePanel>
            ) : null}
          </div>
        </AsyncState>
      )}

      <DetailDrawer
        open={Boolean(selectedAccount)}
        onClose={() => setSelectedAccount(null)}
        title={selectedAccount?.person?.fullName || selectedAccount?.email?.split('@')[0] || 'Chi tiết tài khoản'}
        subtitle={selectedAccount ? `${personCode(selectedAccount)} · ${selectedAccount.email}` : ''}
        footer={selectedAccount && drawerTab === 'access' && accountScope === 'INTERNAL' ? (
          <div className="flex items-center justify-between gap-3">
            <span className="text-xs text-zinc-500">Sau khi lưu, tài khoản sẽ bị đăng xuất khỏi tất cả thiết bị để quyền mới có hiệu lực.</span>
            <button type="button" disabled={updatingId === selectedAccount.id || selectedAccount.status === 'DELETED' || !accountAccessChanged || Boolean(selectedRoleProtectionReason)} onClick={handleAccountAccessSave} className="rounded-xl bg-brand-orange px-4 py-2.5 text-sm font-black text-black hover:bg-orange-500 disabled:opacity-40">Lưu quyền truy cập</button>
          </div>
        ) : null}
      >
        {selectedAccount ? (
          <div className="space-y-6">
            <div className="flex gap-1 overflow-x-auto rounded-xl border border-white/10 bg-white/[0.025] p-1">
              {(accountScope === 'CUSTOMER'
                ? [['overview', 'Tổng quan'], ['security', 'Bảo mật'], ['history', 'Lịch sử đăng nhập']]
                : [['overview', 'Tổng quan'], ['access', 'Quyền truy cập'], ['security', 'Bảo mật'], ['history', 'Thay đổi tài khoản']]
              ).map(([key, label]) => <button key={key} type="button" onClick={() => setDrawerTab(key)} className={`whitespace-nowrap rounded-lg px-3 py-2 text-xs font-bold ${drawerTab === key ? 'bg-brand-orange text-black' : 'text-zinc-400 hover:text-white'}`}>{label}</button>)}
            </div>

            {drawerTab === 'overview' ? (
              <div className="space-y-4">
                <div className="flex items-center justify-between rounded-xl border border-white/10 bg-white/[0.025] p-4">
                  <div><p className="font-black text-white">{selectedAccount.person?.fullName || 'Chưa cập nhật họ tên'}</p><p className="mt-1 text-xs text-zinc-500">{selectedAccount.email}</p></div>
                  <StatusBadge status={operationalStatus(selectedAccount).status} label={operationalStatus(selectedAccount).label} />
                </div>
                <DetailGrid items={accountScope === 'CUSTOMER' ? [
                  { label: 'Mã khách hàng', value: personCode(selectedAccount) },
                  { label: 'Số điện thoại', value: selectedAccount.person?.phoneNumber || 'Chưa cập nhật' },
                  { label: 'Xác minh email', value: selectedAccount.status === 'INACTIVE' ? 'Chưa hoàn tất xác minh' : 'Đã xác minh' },
                  { label: 'Trạng thái tài khoản', value: operationalStatus(selectedAccount).label },
                  { label: 'Lần đăng nhập gần nhất', value: selectedAccount.lastLoginAt ? formatDateTime(selectedAccount.lastLoginAt) : 'Chưa đăng nhập' },
                ] : [
                  { label: 'Mã nhân sự', value: personCode(selectedAccount) },
                  { label: 'Số điện thoại', value: selectedAccount.person?.phoneNumber || 'Chưa cập nhật' },
                  { label: 'Công việc', value: selectedAccount.accessProfile?.name || getRolePresentation(selectedAccount.role).label },
                  { label: normalizeRoleCode(selectedAccount.role) === 'ADMIN' ? 'Phạm vi truy cập' : 'Rạp làm việc', value: cinemaByPublicId.get(String(selectedAccount.person?.cinemaPublicId))?.name || selectedCinemaNames.join(' · ') || 'Toàn hệ thống' },
                  { label: 'Lần đăng nhập gần nhất', value: selectedAccount.lastLoginAt ? formatDateTime(selectedAccount.lastLoginAt) : 'Chưa đăng nhập' },
                  { label: 'Lời mời', value: normalizeRoleCode(selectedAccount.role) !== 'EMPLOYEE' ? 'Không áp dụng' : selectedAccount.status === 'INACTIVE' ? `Chờ chấp nhận · hết hạn ${formatDateTime(selectedAccount.invitationExpiresAt)}` : 'Đã chấp nhận' },
                ]} />
              </div>
            ) : null}

            {drawerTab === 'access' ? (
              <div className="space-y-5">
                {selectedRoleProtectionReason ? <div className="flex items-start gap-3 rounded-xl border border-amber-500/25 bg-amber-500/10 p-4 text-sm leading-6 text-amber-100"><ShieldAlert size={18} className="mt-0.5 shrink-0" /><p>{selectedRoleProtectionReason}</p></div> : null}
                <label className="block text-xs font-black uppercase tracking-wide text-zinc-500">Loại người dùng<Select className="mt-2" value={draftRoleId} onChange={event => { setDraftRoleId(event.target.value); setAccessReason(''); }} disabled={selectedAccount.status === 'DELETED' || Boolean(selectedRoleProtectionReason)}>{systemRoles.filter(role => normalizeRoleCode(role) !== 'CUSTOMER').map(role => <option key={role.id} value={role.id}>{getRolePresentation(role).label}</option>)}</Select></label>
                {['EMPLOYEE', 'MANAGER'].includes(normalizeRoleCode(selectedRole)) ? (
                  <label className="block text-xs font-black uppercase tracking-wide text-zinc-500">{normalizeRoleCode(selectedRole) === 'MANAGER' ? 'Hồ sơ quyền bổ sung' : 'Nhóm nghiệp vụ'}<Select className="mt-2" value={draftAccessProfileId} onChange={event => setDraftAccessProfileId(event.target.value)}>{normalizeRoleCode(selectedRole) === 'MANAGER' ? <option value="">Không cấp quyền bổ sung</option> : null}{accessProfiles.map(profile => <option key={profile.id} value={profile.id}>{profile.name} · {profile.permissions?.length || 0} quyền công việc</option>)}</Select></label>
                ) : null}
                {normalizeRoleCode(selectedRole) === 'MANAGER' ? <div className="space-y-2"><p className="text-xs font-black uppercase tracking-wide text-zinc-500">Rạp được phân công</p>{cinemas.map(cinema => { const checked = draftCinemaPublicIds.includes(String(cinema.publicId)); return <button key={cinema.publicId} type="button" onClick={() => setDraftCinemaPublicIds(current => checked ? current.filter(id => id !== String(cinema.publicId)) : [...current, String(cinema.publicId)])} className={`flex w-full items-center justify-between rounded-xl border p-3 text-left ${checked ? 'border-brand-orange/50 bg-brand-orange/10' : 'border-white/10'}`}><span className="font-semibold text-zinc-200">{cinema.name}</span>{checked ? <CheckCircle2 size={16} className="text-brand-orange" /> : null}</button>; })}</div> : null}
                {normalizeRoleCode(selectedRole) === 'ADMIN' ? <div className="flex items-start gap-3 rounded-xl border border-amber-500/20 bg-amber-500/10 p-4 text-sm leading-6 text-amber-100"><AlertTriangle size={18} className="mt-0.5 shrink-0" /><p>Quản trị hệ thống có toàn quyền. Thao tác nâng quyền cần được kiểm tra trong nhật ký hoạt động.</p></div> : null}
                {normalizeRoleCode(selectedCurrentRole) !== 'ADMIN' && normalizeRoleCode(selectedRole) === 'ADMIN' ? <label className="block text-xs font-black uppercase tracking-wide text-zinc-500">Lý do cấp quyền quản trị *<textarea className="mt-2 min-h-24 w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm font-normal normal-case leading-6 text-white outline-none focus:border-brand-orange" value={accessReason} onChange={event => setAccessReason(event.target.value)} maxLength={500} placeholder="Nêu rõ nhu cầu công việc và người phê duyệt…" /></label> : null}
                <div className="rounded-xl border border-brand-orange/20 bg-brand-orange/[0.05] p-4">
                  <p className="text-[10px] font-black uppercase tracking-wider text-brand-orange">Thay đổi sắp thực hiện</p>
                  <div className="mt-3 space-y-2 text-sm text-zinc-300"><p><span className="text-zinc-500">Vai trò:</span> {getRolePresentation(selectedCurrentRole).label} → {selectedRoleInfo.label}</p><p><span className="text-zinc-500">Phạm vi:</span> {normalizeRoleCode(selectedRole) === 'MANAGER' ? selectedCinemaNames.join(' · ') || 'Chưa chọn rạp' : selectedRoleInfo.scope}</p><p><span className="text-zinc-500">Quyền bị gỡ:</span> {selectedPermissionImpact.removed}</p><p><span className="text-zinc-500">Quyền được thêm:</span> {selectedPermissionImpact.added}</p></div>
                  <p className="mt-3 border-t border-white/10 pt-3 text-xs leading-5 text-amber-200">Sau khi lưu, tài khoản sẽ bị đăng xuất khỏi tất cả thiết bị để quyền mới có hiệu lực.</p>
                </div>
              </div>
            ) : null}

            {drawerTab === 'security' ? (
              <div className="space-y-3">
                {selectedSecurityProtectionReason ? <div className="flex items-start gap-3 rounded-xl border border-amber-500/25 bg-amber-500/10 p-4 text-sm leading-6 text-amber-100"><ShieldAlert size={18} className="mt-0.5 shrink-0" /><p>{selectedSecurityProtectionReason}</p></div> : null}
                {accountScope === 'INTERNAL' && selectedAccount.status === 'INACTIVE' ? <Link to="/admin/staff" className="flex w-full items-start gap-3 rounded-xl border border-brand-orange/25 bg-brand-orange/10 p-4 text-left"><Mail size={19} className="mt-0.5 text-brand-orange" /><span><strong className="block text-white">Quản lý lời mời tại Hồ sơ nhân viên</strong><span className="mt-1 block text-xs leading-5 text-zinc-400">Mở đúng hồ sơ để gửi lại hoặc hủy tiếp nhận, tránh thao tác sai vòng đời.</span></span></Link> : null}
                {selectedAccount.status === 'ACTIVE' ? <button type="button" disabled={updatingId === selectedAccount.id} onClick={() => runSecurityAction(selectedAccount, 'password')} className="flex w-full items-start gap-3 rounded-xl border border-white/10 p-4 text-left hover:border-white/20"><KeyRound size={19} className="mt-0.5 text-sky-400" /><span><strong className="block text-white">Gửi email đặt lại mật khẩu</strong><span className="mt-1 block text-xs leading-5 text-zinc-500">Chủ tài khoản tự đặt mật khẩu mới; admin không thể xem mật khẩu.</span></span></button> : null}
                {selectedAccount.status === 'ACTIVE' ? <button type="button" disabled={updatingId === selectedAccount.id || Boolean(selectedSecurityProtectionReason)} onClick={() => runSecurityAction(selectedAccount, 'sessions')} className="flex w-full items-start gap-3 rounded-xl border border-white/10 p-4 text-left hover:border-white/20 disabled:cursor-not-allowed disabled:opacity-40"><LogOut size={19} className="mt-0.5 text-amber-400" /><span><strong className="block text-white">Đăng xuất khỏi tất cả thiết bị</strong><span className="mt-1 block text-xs leading-5 text-zinc-500">Thu hồi ngay mọi phiên đăng nhập đang hoạt động.</span></span></button> : null}
                {selectedAccount.status === 'LOCKED' ? <button type="button" onClick={() => handleStatusChange(selectedAccount, 'ACTIVE')} className="w-full rounded-xl border border-emerald-500/30 p-4 text-left font-bold text-emerald-300">Mở khóa tài khoản</button> : selectedAccount.status !== 'DELETED' ? <button type="button" disabled={Boolean(selectedSecurityProtectionReason)} onClick={() => handleStatusChange(selectedAccount, 'LOCKED')} className="w-full rounded-xl border border-red-500/30 p-4 text-left font-bold text-red-300 disabled:cursor-not-allowed disabled:opacity-40">Tạm khóa tài khoản</button> : <p className="rounded-xl border border-white/10 p-4 text-sm text-zinc-500">Quyền truy cập đã được thu hồi. Lịch sử vẫn được giữ nguyên.</p>}
              </div>
            ) : null}

            {drawerTab === 'history' ? (
              <div className="space-y-2">{accountHistory.length ? accountHistory.map(entry => {
                const presentation = presentAccountHistory(entry);
                return <article key={entry.id} className="rounded-xl border border-white/10 p-4"><div className="flex items-start justify-between gap-3"><p className="font-bold text-zinc-200">{accountScope === 'CUSTOMER' ? getAuditActionLabel(entry.action) : presentation.title}</p><span className="whitespace-nowrap text-[10px] text-zinc-600">{formatDateTime(entry.createdAt)}</span></div>{accountScope === 'INTERNAL' && presentation.change ? <p className="mt-2 text-xs leading-5 text-zinc-500">{presentation.change}</p> : null}</article>;
              }) : <p className="rounded-xl border border-white/10 p-4 text-sm text-zinc-500">{accountScope === 'CUSTOMER' ? 'Chưa có lịch sử đăng nhập được ghi nhận.' : 'Chưa có thay đổi tài khoản được ghi nhận.'}</p>}</div>
            ) : null}
          </div>
        ) : null}
      </DetailDrawer>

    </section>
  );
}
