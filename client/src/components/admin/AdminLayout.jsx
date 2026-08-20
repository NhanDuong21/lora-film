import { useState, useCallback, useEffect, useMemo } from 'react';
import { 
  Menu,
  CheckCircle,
  AlertCircle,
  AlertTriangle,
  LogOut
} from 'lucide-react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { X } from 'lucide-react';

import { useAuth } from '@/contexts/AuthContext';
import AdminSidebar from './AdminSidebar';
import Breadcrumbs from '@/components/common/Breadcrumbs';
import apiClient from '@/services/apiClient';
import scoreAdminService from '@/features/score/admin/services/scoreAdminService';
import { getDashboard as getCustomerDashboard } from '@/features/internal-staff/admin/services/userAdminService';

export default function AdminLayout({ onBackHome }) {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  // Derive activeTab from pathname
  const activeTab = (() => {
    const path = location.pathname;
    if (path.endsWith('/me')) return 'my-account';
    if (path.endsWith('/movie-operations')) return 'movie-operations';
    if (path.endsWith('/movies') || path.includes('/movies/')) return 'movies';
    if (path.endsWith('/genres')) return 'genres';
    if (path.includes('/showtimes')) return 'showtimes';
    if (path.includes('/pricing')) return 'pricing';
    if (path === '/admin/showtime-schedules/create') return 'auto-schedule-create';
    if (path.includes('/showtime-schedules')) return 'auto-schedule-history';
    if (path.includes('/promotions')) return 'promotions';
    if (path.endsWith('/cinemas') || path.includes('/cinemas')) return 'clusters';
    if (path.endsWith('/rooms') || path.includes('/rooms')) return 'rooms';
    if (path.endsWith('/seat-types') || path.includes('/seat-types')) return 'seat-types';
    if (path.endsWith('/analytics')) return 'analytics';
    if (path === '/admin/accounting' || path.startsWith('/admin/accounting/')) return 'accounting';
    if (path.endsWith('/settlements')) return 'settlements';
    if (path.endsWith('/cash-control')) return 'cash-control';
    if (path.endsWith('/accounting-periods')) return 'accounting-periods';
    if (path.endsWith('/accounting-audit')) return 'accounting-audit';
    if (path.endsWith('/finance')) return 'finance';
    if (path.endsWith('/payments') || path.includes('/payments/')) return 'payments';
    if (path.endsWith('/bookings') || path.includes('/bookings/')) return 'bookings';
    if (path.endsWith('/concessions')) return 'concessions';
    if (path.endsWith('/concession-sales')) return 'concession-sales';
    if (path.endsWith('/members')) return 'customers';
    if (path.endsWith('/scores/dashboard')) return 'scores-dashboard';
    if (path.endsWith('/scores/tiers')) return 'scores-tiers';
    if (path.endsWith('/scores/viewer')) return 'customers';
    if (path.endsWith('/scores/adjustments')) return 'customers';
    if (path.endsWith('/scores/reconciliation')) return 'scores-reconciliation';
    if (path.endsWith('/scores/audit-logs')) return 'scores-reconciliation';
    if (path.endsWith('/staff')) return 'staff';
    if (path.endsWith('/hr')) return 'hr';
    if (path.endsWith('/approvals')) return 'approvals';
    if (path.endsWith('/workforce')) return 'workforce';
    if (path.endsWith('/payroll')) return 'payroll';
    if (path.endsWith('/accounts')) return 'accounts';
    if (path.endsWith('/roles')) return 'roles';
    if (path.endsWith('/permissions')) return 'permissions';
    if (path.endsWith('/user-audits')) return 'audits';
    if (path.endsWith('/audits')) return 'audits';
    if (path === '/admin/notifications') return 'notification-dashboard';
    if (path.includes('/notification-attention')) return 'notification-attention';
    if (path.includes('/notification-templates')) return 'notification-templates';
    if (path.includes('/notification-operations')) return 'notification-operations';
    if (path.includes('/notification-coverage')) return 'notification-coverage';
    if (path.endsWith('/departments')) return 'departments';
    if (path.endsWith('/positions')) return 'positions';
    if (path.endsWith('/organization')) return 'organization';
    return 'dashboard';
  })();

  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [attentionCount, setAttentionCount] = useState(null);
  const [scoreWarningCount, setScoreWarningCount] = useState(0);
  const [attentionLoaded, setAttentionLoaded] = useState(false);

  useEffect(() => {
    let active = true;
    Promise.allSettled([
      apiClient.get('/api/audits', { params: { attentionOnly: true, page: 0, size: 1 } }),
      apiClient.get('/api/admin/user-audits', { params: { attentionOnly: true, page: 0, size: 1 } }),
      scoreAdminService.getDashboardStats(),
      getCustomerDashboard(),
    ]).then(responses => {
      if (!active) return;
      const auditTotal = responses.slice(0, 2).reduce((sum, response) => response.status === 'fulfilled'
        ? sum + Number(response.value?.data?.data?.totalElements || 0)
        : sum, 0);
      const scoreStats = responses[2].status === 'fulfilled' ? responses[2].value : null;
      const customerStats = responses[3].status === 'fulfilled' ? responses[3].value : null;
      const scoreTotal = Number(scoreStats?.totalMembers ?? 0);
      const customerTotal = Number(customerStats?.totalCustomers ?? 0);
      const reconTotal = Number(scoreStats?.lastReconciliationTotalUsers ?? 0);
      const reconFinishedAt = scoreStats?.lastReconciliationFinishedAt || scoreStats?.lastReconciliationTime;
      const reconIsStale = !reconFinishedAt
        || (Date.now() - new Date(reconFinishedAt).getTime()) > 24 * 60 * 60 * 1000;
      const scoreWarnings = [
        scoreStats && customerStats && scoreTotal !== customerTotal,
        scoreStats && reconTotal < scoreTotal,
        scoreStats && Number(scoreStats.pendingReconciliationMismatches ?? 0) > 0,
        scoreStats && reconIsStale,
      ].filter(Boolean).length;
      setScoreWarningCount(scoreWarnings);
      setAttentionCount(auditTotal + scoreWarnings);
      setAttentionLoaded(responses.every(response => response.status === 'fulfilled'));
    });
    return () => { active = false; };
  }, [location.pathname]);

  const handleLogout = async () => {
    await logout();
    localStorage.removeItem('lora_session');
    navigate('/', { replace: true });
    if (onBackHome) onBackHome();
  };

  // Toast System
  const [toast, setToast] = useState({ message: '', type: 'success', visible: false });
  const triggerToast = useCallback((message, type = 'success') => {
    setToast({ message, type, visible: true });
    setTimeout(() => {
      setToast(prev => ({ ...prev, visible: false }));
    }, 3000);
  }, []);

  // Confirm Modal System
  const [confirmModal, setConfirmModal] = useState({
    visible: false,
    title: 'Xác nhận thao tác',
    message: '',
    confirmLabel: 'Đồng ý',
    cancelLabel: 'Quay lại',
    tone: 'warning',
    resolve: null,
  });
  const triggerConfirm = useCallback((input) => {
    const options = typeof input === 'string' ? { message: input } : (input || {});
    return new Promise((resolve) => {
      setConfirmModal({
        visible: true,
        title: options.title || 'Xác nhận thao tác',
        message: options.message || '',
        confirmLabel: options.confirmLabel || 'Đồng ý',
        cancelLabel: options.cancelLabel || 'Quay lại',
        tone: options.tone || 'warning',
        resolve,
      });
    });
  }, []);

  const handleConfirmAccept = () => {
    if (confirmModal.resolve) confirmModal.resolve(true);
    setConfirmModal(current => ({ ...current, visible: false, resolve: null }));
  };

  const handleConfirmCancel = () => {
    if (confirmModal.resolve) confirmModal.resolve(false);
    setConfirmModal(current => ({ ...current, visible: false, resolve: null }));
  };

  // Alert Modal System
  const [alertModal, setAlertModal] = useState({ visible: false, message: '' });
  const triggerAlert = useCallback((message) => {
    setAlertModal({ visible: true, message });
  }, []);

  const handleAlertClose = () => {
    setAlertModal({ visible: false, message: '' });
  };

  const [promptModal, setPromptModal] = useState({
    visible: false,
    title: '',
    message: '',
    label: '',
    placeholder: '',
    value: '',
    required: true,
    confirmLabel: 'Xác nhận',
    resolve: null,
  });
  const triggerPrompt = useCallback((input) => {
    const options = typeof input === 'string' ? { title: input } : (input || {});
    return new Promise(resolve => {
      setPromptModal({
        visible: true,
        title: options.title || 'Nhập thông tin',
        message: options.message || '',
        label: options.label || 'Nội dung',
        placeholder: options.placeholder || '',
        value: options.defaultValue || '',
        required: options.required !== false,
        confirmLabel: options.confirmLabel || 'Xác nhận',
        resolve,
      });
    });
  }, []);

  const handlePromptClose = (accepted) => {
    const value = promptModal.value.trim();
    if (accepted && promptModal.required && !value) return;
    promptModal.resolve?.(accepted ? value : null);
    setPromptModal(current => ({ ...current, visible: false, resolve: null }));
  };

  const outletContext = useMemo(
    () => ({ triggerToast, triggerConfirm, triggerAlert, triggerPrompt }),
    [triggerToast, triggerConfirm, triggerAlert, triggerPrompt],
  );

  return (
    <div className="w-full h-screen overflow-clip bg-zinc-950 flex font-sans relative">
      
      {/* Toast Notification */}
      {toast.visible && (
        <div className="fixed bottom-6 right-6 z-50 flex items-center gap-3 bg-zinc-900 border border-zinc-800 rounded-2xl py-4 px-5 shadow-2xl animate-slide-in">
          {toast.type === 'success' ? (
            <CheckCircle className="w-5 h-5 text-emerald-500" />
          ) : (
            <AlertCircle className="w-5 h-5 text-red-500" />
          )}
          <span className="text-xs font-bold text-zinc-200">{toast.message}</span>
        </div>
      )}

      {/* Confirm Modal */}
      {confirmModal.visible && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
          <div role="dialog" aria-modal="true" aria-labelledby="admin-confirm-title" className="bg-zinc-900 border border-zinc-700 rounded-3xl p-6 max-w-md w-full shadow-2xl animate-fade-in-up">
            <div className="mb-4 flex items-start gap-3">
              <span className={`rounded-xl p-2.5 ${
                confirmModal.tone === 'danger'
                  ? 'bg-red-500/10 text-red-400'
                  : 'bg-amber-500/10 text-amber-400'
              }`}>
                <AlertTriangle className="h-5 w-5" />
              </span>
              <div>
                <h3 id="admin-confirm-title" className="text-lg font-black text-zinc-100">{confirmModal.title}</h3>
                <p className="mt-1 text-sm leading-6 text-zinc-400">{confirmModal.message}</p>
              </div>
            </div>
            <div className="flex gap-3 justify-end">
              <button 
                onClick={handleConfirmCancel}
                className="px-4 py-2.5 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 rounded-xl text-sm font-bold transition-colors"
              >
                {confirmModal.cancelLabel}
              </button>
              <button 
                onClick={handleConfirmAccept}
                className={`px-4 py-2.5 text-white rounded-xl text-sm font-black shadow-lg transition-all ${
                  confirmModal.tone === 'danger'
                    ? 'bg-red-500 hover:bg-red-400 shadow-red-500/20'
                    : 'bg-brand-orange hover:bg-orange-500 shadow-brand-orange/20'
                }`}
              >
                {confirmModal.confirmLabel}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Alert Modal */}
      {alertModal.visible && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6 max-w-sm w-full shadow-2xl animate-fade-in-up">
            <div className="flex justify-between items-center mb-2">
              <h3 className="text-lg font-bold text-amber-500 flex items-center gap-2">
                <AlertCircle className="w-5 h-5" /> Thông báo
              </h3>
              <button onClick={handleAlertClose} className="text-zinc-500 hover:text-zinc-300">
                <X className="w-5 h-5" />
              </button>
            </div>
            <p className="text-sm text-zinc-400 mb-6">{alertModal.message}</p>
            <div className="flex justify-end">
              <button 
                onClick={handleAlertClose}
                className="px-6 py-2 bg-zinc-800 hover:bg-zinc-700 text-zinc-200 rounded-xl text-xs font-bold uppercase transition-colors"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

      {promptModal.visible && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
          <div role="dialog" aria-modal="true" aria-labelledby="admin-prompt-title" className="w-full max-w-md rounded-3xl border border-zinc-700 bg-zinc-900 p-6 shadow-2xl animate-fade-in-up">
            <h3 id="admin-prompt-title" className="text-xl font-black text-zinc-100">{promptModal.title}</h3>
            {promptModal.message && <p className="mt-2 text-sm leading-6 text-zinc-400">{promptModal.message}</p>}
            <label className="mt-5 block text-sm font-bold text-zinc-300">
              {promptModal.label}
              <textarea
                autoFocus
                value={promptModal.value}
                onChange={event => setPromptModal(current => ({ ...current, value: event.target.value }))}
                placeholder={promptModal.placeholder}
                rows={3}
                className="mt-2 w-full resize-none rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm font-normal text-zinc-100 outline-none placeholder:text-zinc-600 focus:border-brand-orange"
              />
            </label>
            {promptModal.required && !promptModal.value.trim() && (
              <p className="mt-2 text-xs text-amber-300">Vui lòng nhập nội dung trước khi xác nhận.</p>
            )}
            <div className="mt-6 flex justify-end gap-3">
              <button type="button" onClick={() => handlePromptClose(false)} className="rounded-xl bg-zinc-800 px-4 py-2.5 text-sm font-bold text-zinc-300 hover:bg-zinc-700">
                Quay lại
              </button>
              <button
                type="button"
                onClick={() => handlePromptClose(true)}
                disabled={promptModal.required && !promptModal.value.trim()}
                className="rounded-xl bg-brand-orange px-4 py-2.5 text-sm font-black text-white hover:bg-orange-500 disabled:cursor-not-allowed disabled:opacity-40"
              >
                {promptModal.confirmLabel}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Fixed Sidebar Column */}
      <div className={`shrink-0 h-full fixed lg:static z-30 transition-transform duration-300 ${
        sidebarOpen ? 'translate-x-0' : '-translate-x-full'
      } lg:translate-x-0`}>
        <AdminSidebar 
          activeTab={activeTab} 
          setActiveTab={() => {
            setSidebarOpen(false); // Close on mobile navigation
          }} 
          user={user} 
          onBackHome={onBackHome} 
          handleLogout={handleLogout} 
        />
      </div>

      {/* Mobile Sidebar overlay backdrop */}
      {sidebarOpen && (
        <div 
          onClick={() => setSidebarOpen(false)}
          className="fixed inset-0 bg-black/60 z-20 lg:hidden"
        />
      )}

      {/* Right Column Workspace (Fluid layout) */}
      <div className="min-w-0 flex-1 min-h-0 h-full flex flex-col overflow-clip">
        
        {/* Sticky top Navigation Bar */}
        <header className="w-full h-[72px] bg-zinc-950/80 backdrop-blur-xl border-b border-zinc-800 flex items-center justify-between px-4 sm:px-8 shrink-0 z-20">
          <div className="flex items-center gap-4">
            <button 
              onClick={() => setSidebarOpen(true)}
              aria-label="Mở menu quản trị"
              aria-expanded={sidebarOpen}
              className="lg:hidden p-2 -ml-2 text-zinc-400 hover:text-white transition-colors"
            >
              <Menu className="w-5 h-5" />
            </button>
            <Breadcrumbs />
          </div>
          <div className="flex items-center gap-4">
            <button type="button" onClick={() => navigate(scoreWarningCount > 0 ? '/admin/scores/reconciliation?view=control' : '/admin/audits?tab=attention')} className="hidden rounded-full border border-brand-orange/20 bg-brand-orange/10 px-3 py-1.5 text-[10px] font-bold uppercase tracking-widest text-brand-orange hover:bg-brand-orange/15 md:block">
              {!attentionLoaded || attentionCount === null ? 'Chưa tải trạng thái cảnh báo' : attentionCount === 0 ? 'Hệ thống đang hoạt động' : `${attentionCount} tín hiệu toàn hệ thống cần kiểm tra`}
            </button>
            <div className="h-6 w-px bg-zinc-800 hidden md:block"></div>
            <button className="text-zinc-400 hover:text-white transition-colors p-2" title="Thông báo" aria-label="Thông báo">
              <div className="relative">
                <AlertCircle className="w-5 h-5" />
                <span className="absolute -top-1 -right-1 w-2.5 h-2.5 bg-brand-orange rounded-full border-2 border-zinc-950"></span>
              </div>
            </button>
            <div className="h-6 w-px bg-zinc-800 hidden md:block"></div>
            <button onClick={handleLogout} className="text-zinc-400 hover:text-red-500 transition-colors p-2 flex items-center gap-2" title="Đăng xuất">
              <LogOut className="w-5 h-5" />
              <span className="hidden md:block text-sm font-bold">Đăng xuất</span>
            </button>
          </div>
        </header>

        {/* Dynamic View Body Content */}
        <main className="min-h-0 min-w-0 flex-1 overflow-x-hidden overflow-y-auto p-4 sm:p-6 space-y-6">
          <Outlet context={outletContext} />
        </main>
      </div>

    </div>
  );
}
