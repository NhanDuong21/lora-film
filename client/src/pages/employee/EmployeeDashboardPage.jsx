import { useState, useMemo } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { 
  Ticket, 
  Coffee, 
  CheckSquare, 
  Calendar, 
  LogOut, 
  Home, 
  Plus, 
  Minus, 
  Search, 
  CheckCircle,
  AlertCircle
} from 'lucide-react';
import { useData } from '../../contexts/DataContext';

export default function EmployeeDashboardView({ onBackHome, onTicketingSelect }) {
  const { user, logout } = useAuth();
  const [activeTab, setActiveTab] = useState('ticketing');
  const { 
    movies, 
    showtimes, 
    theaters, 
    tickets, setTickets: updateTicketsState, 
    concessions, setConcessions: updateConcessionsState 
  } = useData();

  // Concessions Cart State
  const [cart, setCart] = useState({});
  const [showConcessionsSuccess, setShowConcessionsSuccess] = useState(false);

  // Ticket Validation State
  const [ticketCode, setTicketCode] = useState('');
  const [validationResult, setValidationResult] = useState(null);

  // Search State for Ticketing
  const [searchQuery, setSearchQuery] = useState('');

  // Toast Notification State
  const [toastMessage, setToastMessage] = useState('');
  const [toastType, setToastType] = useState('success');
  const triggerToast = (msg, type = 'success') => {
    setToastMessage(msg);
    setToastType(type);
    setTimeout(() => setToastMessage(''), 3000);
  };

  // Concessions Logic
  const updateCartQty = (id, delta) => {
    setCart((prev) => {
      const currentQty = prev[id] || 0;
      const newQty = currentQty + delta;
      if (newQty <= 0) {
        const copy = { ...prev };
        delete copy[id];
        return copy;
      }
      return { ...prev, [id]: newQty };
    });
  };

  const cartTotal = useMemo(() => {
    return Object.entries(cart).reduce((sum, [itemId, qty]) => {
      const item = concessions.find((i) => i.id === parseInt(itemId));
      return sum + (item ? item.price * qty : 0);
    }, 0);
  }, [cart, concessions]);

  const handleCheckoutConcessions = () => {
    if (Object.keys(cart).length === 0) return;

    // Mutate state to increment salesCount in localStorage concessions catalog
    const updatedConcessions = concessions.map(c => {
      const qtyInCart = cart[c.id];
      if (qtyInCart) {
        return { ...c, salesCount: c.salesCount + qtyInCart };
      }
      return c;
    });

    updateConcessionsState(updatedConcessions);
    setShowConcessionsSuccess(true);
  };

  const handleResetConcessions = () => {
    setCart({});
    setShowConcessionsSuccess(false);
  };

  // Ticket Validation Live Verification
  const handleValidateTicket = (e) => {
    e.preventDefault();
    if (!ticketCode.trim()) return;

    const code = ticketCode.trim().toUpperCase();
    
    // Read fresh list from localStorage to avoid stale state issues
    const freshTicketsStr = localStorage.getItem('lora_tickets');
    const freshTickets = freshTicketsStr ? JSON.parse(freshTicketsStr) : tickets;

    const matchedTicketIndex = freshTickets.findIndex(t => t.id.toUpperCase() === code);

    if (matchedTicketIndex !== -1) {
      const ticket = freshTickets[matchedTicketIndex];
      
      if (ticket.status === 'DA_KIEM_TRA') {
        setValidationResult({
          success: false,
          code,
          message: 'Canh bao: Ve nay da duoc check-in tu truoc do!'
        });
      } else {
        // Mutate status to DA_KIEM_TRA
        const updatedTickets = [...freshTickets];
        updatedTickets[matchedTicketIndex] = { ...ticket, status: 'DA_KIEM_TRA' };
        updateTicketsState(updatedTickets);

        setValidationResult({
          success: true,
          code,
          movie: ticket.movieTitle,
          seats: ticket.seats.join(', '),
          cinema: ticket.theaterName,
          showtime: `${ticket.time} | ${ticket.date}`,
          message: 'Ve hop le! Check-in thanh cong. Cho phep khach hang vao phong chieu.'
        });
        triggerToast('Check-in ve thanh cong!');
      }
    } else {
      setValidationResult({
        success: false,
        code,
        message: 'Ma ve khong ton tai trong he thong hoac chua duoc thanh toan!'
      });
    }
  };

  // Ticketing Search Logic (Now showing movies only)
  const filteredMovies = useMemo(() => {
    return movies.filter((m) => 
      m.title.toLowerCase().includes(searchQuery.toLowerCase()) && 
      m.status === 'NOW_SHOWING'
    );
  }, [movies, searchQuery]);

  const handleLogout = () => {
    logout();
    onBackHome();
  };

  return (
    <div className="bg-zinc-950 text-zinc-100 min-h-screen flex flex-col lg:flex-row relative">
      {/* Toast popup */}
      {toastMessage && (
        <div className={`fixed top-6 right-6 z-50 py-3.5 px-6 rounded-xl shadow-2xl flex items-center gap-2 border font-bold text-sm text-white animate-bounce ${
          toastType === 'success' ? 'bg-emerald-600 border-emerald-500' : 'bg-red-600 border-red-500'
        }`}>
          <AlertCircle className="w-4.5 h-4.5" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Sidebar Operational Staff Panel */}
      <aside className="w-full lg:w-64 bg-zinc-900 border-r border-zinc-800 flex flex-col justify-between shrink-0">
        <div>
          {/* Box Office branding */}
          <div className="p-6 border-b border-zinc-800">
            <span className="text-brand-coral font-black tracking-widest text-lg uppercase block mb-1">
              Lora Film
            </span>
            <div className="flex items-center gap-1.5">
              <span className="text-[10px] uppercase font-black px-2 py-0.5 rounded bg-brand-coral text-white">
                NHÂN VIÊN
              </span>
              <span className="text-[9px] text-zinc-500 uppercase tracking-widest font-bold">
                Quầy Vé & Dịch Vụ
              </span>
            </div>
          </div>

          {/* Employee Navigation Links */}
          <nav className="p-4 space-y-1">
            <button
              onClick={() => setActiveTab('ticketing')}
              className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-bold uppercase transition-all duration-200 ${
                activeTab === 'ticketing'
                  ? 'bg-brand-coral/10 text-brand-coral border-l-4 border-brand-coral'
                  : 'text-zinc-400 hover:bg-zinc-800/50 hover:text-white'
              }`}
            >
              <Ticket className="w-4 h-4" />
              <span>Đặt Vé Tại Quầy</span>
            </button>

            <button
              onClick={() => setActiveTab('concessions')}
              className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-bold uppercase transition-all duration-200 ${
                activeTab === 'concessions'
                  ? 'bg-brand-coral/10 text-brand-coral border-l-4 border-brand-coral'
                  : 'text-zinc-400 hover:bg-zinc-800/50 hover:text-white'
              }`}
            >
              <Coffee className="w-4 h-4" />
              <span>Bán Kèm Bắp Nước</span>
            </button>

            <button
              onClick={() => setActiveTab('validation')}
              className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-bold uppercase transition-all duration-200 ${
                activeTab === 'validation'
                  ? 'bg-brand-coral/10 text-brand-coral border-l-4 border-brand-coral'
                  : 'text-zinc-400 hover:bg-zinc-800/50 hover:text-white'
              }`}
            >
              <CheckSquare className="w-4 h-4" />
              <span>Kiểm Tra Vé</span>
            </button>

            <button
              onClick={() => setActiveTab('schedules')}
              className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-bold uppercase transition-all duration-200 ${
                activeTab === 'schedules'
                  ? 'bg-brand-coral/10 text-brand-coral border-l-4 border-brand-coral'
                  : 'text-zinc-400 hover:bg-zinc-800/50 hover:text-white'
              }`}
            >
              <Calendar className="w-4 h-4" />
              <span>Xem Lịch Chiếu</span>
            </button>
          </nav>
        </div>

        {/* User profile controls footer */}
        <div className="p-4 border-t border-zinc-800 space-y-2 mt-auto">
          <div className="px-4 py-2">
            <p className="text-xs text-zinc-500 font-bold uppercase">Nhân viên</p>
            <p className="text-sm font-bold text-white truncate">{user?.fullName || 'Frontline Staff'}</p>
          </div>

          <button
            onClick={onBackHome}
            className="w-full flex items-center gap-3 px-4 py-2.5 rounded-lg text-xs font-bold text-zinc-400 hover:bg-zinc-800 hover:text-white transition-all"
          >
            <Home className="w-4 h-4" />
            <span>Về Trang Chủ</span>
          </button>

          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-3 px-4 py-2.5 rounded-lg text-xs font-bold text-red-400 hover:bg-red-950/20 hover:text-red-300 transition-all"
          >
            <LogOut className="w-4 h-4" />
            <span>Đăng xuất</span>
          </button>
        </div>
      </aside>

      {/* Content Space */}
      <main className="flex-grow p-6 md:p-10 space-y-8 overflow-y-auto lg:max-h-screen">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-zinc-800/80 pb-6">
          <div>
            <h1 className="text-2xl md:text-3xl font-black text-white tracking-wide uppercase">
              {activeTab === 'ticketing' && 'ĐẶT VÉ TẠI QUẦY'}
              {activeTab === 'concessions' && 'DỊCH VỤ BẮP NƯỚC'}
              {activeTab === 'validation' && 'QUÉT & KIỂM TRA VÉ'}
              {activeTab === 'schedules' && 'LỊCH CHIẾU HÔM NAY'}
            </h1>
            <p className="text-zinc-500 text-xs uppercase tracking-wider mt-1">
              Giao diện tác vụ trực tiếp hỗ trợ khách hàng tại quầy vé
            </p>
          </div>
          <div className="text-right text-xs text-zinc-400">
            Quầy hỗ trợ: <span className="text-emerald-500 font-black">BOX OFFICE 1</span>
          </div>
        </div>

        {/* Tab content bodies */}
        <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 min-h-[400px]">
          
          {/* Ticketing Tab */}
          {activeTab === 'ticketing' && (
            <div className="space-y-6">
              <div className="flex items-center gap-3 max-w-md bg-zinc-950 border border-zinc-850 px-4 py-2.5 rounded-xl">
                <Search className="w-4 h-4 text-zinc-500" />
                <input
                  type="text"
                  placeholder="Tìm nhanh phim đang chiếu..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="bg-transparent border-none outline-none text-xs w-full text-white placeholder-zinc-600 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {filteredMovies.map((movie) => {
                  const movieShowtimes = showtimes.filter(st => st.movieId === movie.id);
                  
                  return (
                    <div key={movie.id} className="bg-zinc-950/60 border border-zinc-800/80 rounded-2xl p-4 flex gap-4">
                      <img
                        src={movie.posterUrl}
                        alt={movie.title}
                        className="w-20 h-28 object-cover rounded-xl border border-zinc-850 shrink-0"
                        onError={(e) => {
                          e.target.onerror = null;
                          e.target.src = 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=600&auto=format&fit=crop&q=80';
                        }}
                      />
                      <div className="flex flex-col justify-between flex-grow">
                        <div>
                          <h3 className="font-black text-white text-sm leading-tight mb-1">{movie.title}</h3>
                          <p className="text-zinc-500 text-[11px]">{movie.genres.join(', ')} | {movie.duration}</p>
                        </div>

                        <div className="flex flex-wrap gap-2 mt-2">
                          {movieShowtimes.length > 0 ? (
                            movieShowtimes.map((st) => {
                              const t = theaters.find(th => th.id === st.theaterId);
                              const h = t?.halls.find(hall => hall.id === st.hallId);
                              
                              return (
                                <button
                                  key={st.id}
                                  onClick={() => onTicketingSelect({
                                    movieId: movie.id,
                                    movieTitle: movie.title,
                                    date: st.date,
                                    cinema: `${t?.name} - ${h?.name}`,
                                    time: st.time,
                                    format: h?.format || '2D DIGITAL'
                                  })}
                                  className="bg-brand-coral hover:bg-opacity-95 text-white font-extrabold text-[9px] py-1.5 px-2.5 rounded uppercase tracking-wider transition-all"
                                >
                                  {st.time} ({h?.name})
                                </button>
                              );
                            })
                          ) : (
                            <span className="text-[10px] text-zinc-600 italic">Khong co suat chieu</span>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Concessions Tab */}
          {activeTab === 'concessions' && (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              {/* Concessions Menu */}
              <div className="lg:col-span-2 space-y-4">
                <h3 className="text-lg font-black text-white mb-2 uppercase tracking-wide">Thực đơn bắp nước</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {concessions.map((item) => (
                    <div key={item.id} className="bg-zinc-950/60 border border-zinc-800 p-4 rounded-2xl flex justify-between items-center">
                      <div>
                        <h4 className="font-bold text-white text-sm">{item.name}</h4>
                        <p className="text-zinc-500 text-xs mt-0.5">{item.details}</p>
                        <p className="text-brand-coral font-black text-sm mt-2">{item.price.toLocaleString('vi-VN')}đ</p>
                      </div>

                      <div className="flex items-center gap-3 shrink-0">
                        <button
                          onClick={() => updateCartQty(item.id, -1)}
                          className="w-7 h-7 bg-zinc-900 border border-zinc-800 rounded-lg flex items-center justify-center text-zinc-400 hover:text-white"
                        >
                          <Minus className="w-3 h-3" />
                        </button>
                        <span className="text-sm font-black w-4 text-center">{cart[item.id] || 0}</span>
                        <button
                          onClick={() => updateCartQty(item.id, 1)}
                          className="w-7 h-7 bg-zinc-900 border border-zinc-800 rounded-lg flex items-center justify-center text-zinc-400 hover:text-white"
                        >
                          <Plus className="w-3 h-3" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Concessions Cart */}
              <div className="bg-zinc-950/80 border border-zinc-800 rounded-2xl p-6 flex flex-col justify-between h-fit space-y-6">
                <div>
                  <h3 className="text-md font-black text-white border-b border-zinc-800 pb-3 mb-4 uppercase tracking-wide">
                    Hoá đơn thanh toán
                  </h3>
                  
                  {Object.keys(cart).length === 0 ? (
                    <p className="text-zinc-600 text-sm italic text-center py-6">Chưa có sản phẩm nào được chọn</p>
                  ) : (
                    <div className="space-y-3 max-h-[220px] overflow-y-auto scrollbar-thin">
                      {Object.entries(cart).map(([itemId, qty]) => {
                        const item = concessions.find((i) => i.id === parseInt(itemId));
                        if (!item) return null;
                        return (
                          <div key={itemId} className="flex justify-between text-xs">
                            <span className="text-zinc-300 font-medium">
                              {item.name} <span className="text-zinc-500">x{qty}</span>
                            </span>
                            <span className="text-white font-bold">
                              {(item.price * qty).toLocaleString('vi-VN')}đ
                            </span>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>

                <div className="border-t border-zinc-850 pt-4 space-y-4">
                  <div className="flex justify-between items-center">
                    <span className="text-zinc-400 text-xs font-bold uppercase">Tổng cộng</span>
                    <span className="text-xl font-black text-brand-coral">
                      {cartTotal.toLocaleString('vi-VN')}đ
                    </span>
                  </div>

                  <button
                    onClick={handleCheckoutConcessions}
                    disabled={Object.keys(cart).length === 0}
                    className={`w-full py-3.5 rounded-xl font-black uppercase text-xs tracking-wider transition-all ${
                      Object.keys(cart).length > 0
                        ? 'bg-brand-coral hover:bg-opacity-95 text-white shadow-lg'
                        : 'bg-zinc-800 text-zinc-500 cursor-not-allowed border border-zinc-705'
                    }`}
                  >
                    Thanh Toán & In Hoá Đơn
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Validation Tab */}
          {activeTab === 'validation' && (
            <div className="max-w-xl mx-auto space-y-8 py-6">
              <form onSubmit={handleValidateTicket} className="space-y-4">
                <div className="space-y-1">
                  <label className="text-zinc-400 text-xs font-black uppercase tracking-wider block">
                    Mã Check-in Vé Phim
                  </label>
                  <div className="flex gap-3">
                    <input
                      type="text"
                      placeholder="Nhập mã vé (VD: TKT-8492-9582)"
                      value={ticketCode}
                      onChange={(e) => setTicketCode(e.target.value)}
                      className="bg-zinc-950 border border-zinc-800 rounded-xl py-3 px-4 text-xs text-white placeholder-zinc-650 focus:outline-none focus:border-brand-coral flex-grow"
                    />
                    <button
                      type="submit"
                      className="bg-brand-coral hover:bg-opacity-95 text-white font-black px-6 rounded-xl text-xs uppercase tracking-wider"
                    >
                      Kiểm Tra
                    </button>
                  </div>
                </div>
              </form>

              {validationResult && (
                <div className={`border rounded-2xl p-6 ${
                  validationResult.success 
                    ? 'bg-emerald-950/20 border-emerald-900/60 text-emerald-200' 
                    : 'bg-red-950/20 border-red-900/60 text-red-200'
                }`}>
                  <div className="flex items-start gap-4">
                    {validationResult.success ? (
                      <CheckCircle className="w-6 h-6 text-emerald-500 shrink-0 mt-0.5" />
                    ) : (
                      <AlertCircle className="w-6 h-6 text-red-500 shrink-0 mt-0.5" />
                    )}

                    <div className="space-y-2 flex-grow">
                      <h4 className="font-black text-white text-base">
                        {validationResult.success ? 'KẾT QUẢ: HỢP LỆ' : 'KẾT QUẢ: KHÔNG HỢP LỆ'}
                      </h4>
                      <p className="text-xs">{validationResult.message}</p>

                      {validationResult.success && (
                        <div className="bg-zinc-950/40 rounded-xl p-4 text-xs space-y-1.5 mt-4 text-zinc-300">
                          <div><span className="text-zinc-500">Mã vé:</span> <span className="text-white font-bold">{validationResult.code}</span></div>
                          <div><span className="text-zinc-500">Phim:</span> <span className="text-white font-bold">{validationResult.movie}</span></div>
                          <div><span className="text-zinc-500">Suất chiếu:</span> <span className="text-brand-coral font-bold">{validationResult.showtime}</span></div>
                          <div><span className="text-zinc-500">Phòng/Ghế:</span> <span className="text-emerald-400 font-bold">{validationResult.cinema} - {validationResult.seats}</span></div>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Schedules Tab */}
          {activeTab === 'schedules' && (
            <div className="space-y-6">
              <h3 className="text-lg font-black text-white mb-4 uppercase tracking-wide">Bảng Phân Bổ Phòng Chiếu</h3>
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm text-zinc-400">
                  <thead className="bg-zinc-950 text-zinc-500 text-xs font-black uppercase tracking-wider border-b border-zinc-800">
                    <tr>
                      <th className="py-3 px-4">Cum rap</th>
                      <th className="py-3 px-4">Phong chieu</th>
                      <th className="py-3 px-4">Phim</th>
                      <th className="py-3 px-4">Định Dạng</th>
                      <th className="py-3 px-4">Khung Giờ</th>
                      <th className="py-3 px-4">Trạng Thái</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-800/60">
                    {showtimes.map((st, idx) => {
                      const movie = movies.find(m => m.id === st.movieId);
                      const t = theaters.find(th => th.id === st.theaterId);
                      const h = t?.halls.find(hall => hall.id === st.hallId);

                      return (
                        <tr key={idx} className="hover:bg-zinc-950/20">
                          <td className="py-3.5 px-4 font-bold text-white">{t?.name || 'Unknown'}</td>
                          <td className="py-3.5 px-4 text-zinc-300">{h?.name || 'Unknown'}</td>
                          <td className="py-3.5 px-4 font-semibold text-zinc-100">{movie?.title || 'Unknown'}</td>
                          <td className="py-3.5 px-4"><span className="text-brand-coral font-black text-[11px]">{h?.format || '2D DIGITAL'}</span></td>
                          <td className="py-3.5 px-4 font-mono font-bold text-brand-yellow">
                            {st.time} | {st.date}
                          </td>
                          <td className="py-3.5 px-4">
                            <span className="px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-[10px] font-black uppercase">
                              Đang mở
                            </span>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}

        </div>
      </main>

      {/* Concessions Checkout Success Modal */}
      {showConcessionsSuccess && (
        <div className="fixed inset-0 bg-black/90 backdrop-blur-md flex items-center justify-center z-50 p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-8 max-w-md w-full shadow-2xl text-center relative overflow-hidden">
            <CheckCircle className="w-16 h-16 text-emerald-500 mx-auto mb-4" />
            <h3 className="text-2xl font-black text-white uppercase tracking-wide mb-2">THANH TOÁN THÀNH CÔNG</h3>
            <p className="text-zinc-400 text-sm mb-6">Hoá đơn dịch vụ bắp nước đã được thanh toán và in thành công.</p>
            
            <div className="bg-zinc-950/60 border border-zinc-800/80 rounded-2xl p-4 text-left space-y-2 text-xs mb-6 text-zinc-300">
              {Object.entries(cart).map(([itemId, qty]) => {
                const item = concessions.find((i) => i.id === parseInt(itemId));
                if (!item) return null;
                return (
                  <div key={itemId} className="flex justify-between">
                    <span>{item.name} x{qty}</span>
                    <span className="text-white">{(item.price * qty).toLocaleString('vi-VN')}đ</span>
                  </div>
                );
              })}
              <div className="border-t border-zinc-850 pt-2.5 mt-2.5 flex justify-between font-bold">
                <span>Tổng hoá đơn:</span>
                <span className="text-brand-coral">{cartTotal.toLocaleString('vi-VN')}đ</span>
              </div>
            </div>

            <button
              onClick={handleResetConcessions}
              className="w-full bg-brand-coral hover:bg-opacity-95 text-white font-black py-4 rounded-xl text-xs uppercase tracking-wider"
            >
              Hoàn Thành
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
