import CustomerBookingHistory from '../components/CustomerBookingHistory';

export default function BookingHistoryPage() {
  return (
    <div className="bg-zinc-950 text-zinc-100 min-h-screen pt-32 pb-16 px-4 md:px-12 selection:bg-brand-orange selection:text-zinc-950 font-sans font-medium">
      <div className="max-w-6xl mx-auto w-full space-y-8">

        {/* Page Header */}
        <div>
          <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">Lịch Sử Đặt Vé</h1>
          <p className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider mt-1">
            Xem lại danh sách vé xem phim và bắp nước bạn đã đặt
          </p>
        </div>

        <CustomerBookingHistory />
      </div>
    </div>
  );
}
