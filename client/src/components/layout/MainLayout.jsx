import { Outlet, useLocation } from 'react-router-dom';
import Header from './Header';
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

export default function MainLayout() {
  const { pathname } = useLocation();
  const isFocusedBookingFlow = focusedBookingPaths.some(path =>
    pathname === path || pathname.startsWith(`${path}/`)
  );

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100 flex flex-col selection:bg-brand-orange selection:text-white">
      {isFocusedBookingFlow ? <BookingFlowHeader /> : <Header />}
      {!isFocusedBookingFlow && <ActiveBookingRecoveryBanner />}

      <main className={`flex-grow ${isFocusedBookingFlow ? 'pt-16' : 'pt-20'}`}>
        <Outlet />
      </main>

      {!isFocusedBookingFlow && <Footer />}
    </div>
  );
}
