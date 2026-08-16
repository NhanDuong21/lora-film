import { Outlet, useLocation } from 'react-router-dom';
import Header from './Header';
import AuthHeader from './AuthHeader';
import Footer from './Footer';
import ActiveBookingRecoveryBanner from '@/features/booking/customer/components/ActiveBookingRecoveryBanner';
import BookingFlowHeader from '@/features/booking/customer/components/BookingFlowHeader';

const focusedBookingPaths = [
  '/booking',
  '/seat-selection',
  '/bookings/checkout',
  '/bookings/success',
  '/bookings/failed',
  '/payments/return'
];

const authPaths = [
  '/login',
  '/register',
  '/verify-otp',
  '/forgot-password',
  '/reset-password',
  '/oauth2/redirect'
];

export default function MainLayout() {
  const { pathname } = useLocation();
  const isFocusedBookingFlow = focusedBookingPaths.some(path =>
    pathname === path || pathname.startsWith(`${path}/`)
  );
  const isAuthFlow = authPaths.some(path => pathname === path || pathname.startsWith(`${path}/`));

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100 flex flex-col selection:bg-brand-orange selection:text-white">
      {isAuthFlow ? <AuthHeader /> : isFocusedBookingFlow ? <BookingFlowHeader /> : <Header />}
      {!isFocusedBookingFlow && !isAuthFlow && <ActiveBookingRecoveryBanner />}

      <main className={`flex-grow ${isFocusedBookingFlow ? 'pt-16' : 'pt-20'}`}>
        <Outlet />
      </main>

      {!isFocusedBookingFlow && !isAuthFlow && <Footer />}
    </div>
  );
}
