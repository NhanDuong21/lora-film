import { useCallback, useEffect, useState } from 'react';
import { Bell } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import {
  NOTIFICATIONS_CHANGED_EVENT,
  notificationCustomerService
} from '../services/notificationCustomerService';

const POLL_INTERVAL_MS = 60_000;

export default function CustomerNotificationBell() {
  const navigate = useNavigate();
  const [unreadCount, setUnreadCount] = useState(0);

  const refresh = useCallback(async () => {
    try {
      setUnreadCount(await notificationCustomerService.unreadCount());
    } catch {
      // The header remains usable while the notification service is unavailable.
    }
  }, []);

  useEffect(() => {
    refresh();
    const timer = window.setInterval(refresh, POLL_INTERVAL_MS);
    window.addEventListener(NOTIFICATIONS_CHANGED_EVENT, refresh);
    return () => {
      window.clearInterval(timer);
      window.removeEventListener(NOTIFICATIONS_CHANGED_EVENT, refresh);
    };
  }, [refresh]);

  const label = unreadCount > 0
    ? `Thông báo, ${unreadCount} chưa đọc`
    : 'Thông báo';

  return (
    <button
      type="button"
      aria-label={label}
      onClick={() => navigate('/profile?tab=notifications')}
      className="relative rounded-xl border border-zinc-800/80 bg-zinc-900 p-2 text-zinc-400 transition-all hover:bg-zinc-800 hover:text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange"
    >
      <Bell className="h-4 w-4" />
      {unreadCount > 0 && (
        <span className="absolute -right-1.5 -top-1.5 flex min-h-4 min-w-4 items-center justify-center rounded-full bg-brand-orange px-1 text-[9px] font-black leading-none text-white ring-2 ring-zinc-950">
          {unreadCount > 99 ? '99+' : unreadCount}
        </span>
      )}
    </button>
  );
}
