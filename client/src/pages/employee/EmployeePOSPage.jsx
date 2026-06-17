import { useState, useMemo } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useData } from '../../contexts/DataContext';
import { 
  Search, 
  Plus, 
  Minus, 
  X, 
  Check, 
  ShoppingCart,
  Printer,
  Trash2,
  Lock,
  Unlock,
  FileX
} from 'lucide-react';

export default function EmployeePOSView() {
  const { user } = useAuth();
  const { 
    movies, 
    showtimes, 
    theaters, 
    concessions, 
    tickets, 
    setTickets 
  } = useData();

  // Search state
  const [searchQuery, setSearchQuery] = useState('');

  // Selection states
  const [selectedMovie, setSelectedMovie] = useState(null);
  const [selectedShowtime, setSelectedShowtime] = useState(null);
  const [selectedTheater, setSelectedTheater] = useState(null);
  const [selectedHall, setSelectedHall] = useState(null);
  const [selectedSeats, setSelectedSeats] = useState([]);
  
  // Cart state for concessions
  const [cart, setCart] = useState({}); // { concessionId: quantity }

  // UI States
  const [showSeatModal, setShowSeatModal] = useState(false);
  const [showUpsell, setShowUpsell] = useState(false);
  const [showInvoiceModal, setShowInvoiceModal] = useState(false);
  const [createdTicket, setCreatedTicket] = useState(null);

  // Active Sales History Ledger
  const [recentTransactions, setRecentTransactions] = useState([]);
  const [showtimeBookedSeats, setShowtimeBookedSeats] = useState({});

  // Override Modal States
  const [showOverrideModal, setShowOverrideModal] = useState(false);
  const [transactionToVoid, setTransactionToVoid] = useState(null);
  const [overrideEmail, setOverrideEmail] = useState('');
  const [overridePassword, setOverridePassword] = useState('');
  const [overrideError, setOverrideError] = useState('');
  
  // Toast notifications
  const [toast, setToast] = useState({ show: false, message: '', type: 'success' });

  // Filter movies currently showing
  const filteredMovies = useMemo(() => {
    return movies.filter(m => 
      m.title.toLowerCase().includes(searchQuery.toLowerCase()) &&
      m.status === 'NOW_SHOWING'
    );
  }, [movies, searchQuery]);

  // Handle selecting a movie showtime
  const handleSelectShowtime = (movie, st) => {
    const theater = theaters.find(t => String(t.id) === String(st.theaterId));
    const hall = theater?.halls?.find(h => String(h.id) === String(st.hallId));
    
    setSelectedMovie(movie);
    setSelectedShowtime(st);
    setSelectedTheater(theater);
    setSelectedHall(hall);
    setSelectedSeats([]);
    setShowSeatModal(true);
  };

  // Generate seat grid (A-F, 1-10)
  const seatRows = ['A', 'B', 'C', 'D', 'E', 'F'];
  const seatCols = Array.from({ length: 10 }, (_, i) => i + 1);

  // Check if seat is booked (mock index rule or matches existing ticket)
  const isSeatBooked = (row, col) => {
    if (!selectedShowtime) return false;
    const seatId = `${row}${col}`;
    
    // Check dynamic booked list for current showtime
    const currentShowtimeBooked = showtimeBookedSeats[selectedShowtime.id] || [];
    if (currentShowtimeBooked.includes(seatId)) return true;

    // Generate static mock booked seats based on seat index
    const hash = seatId.charCodeAt(0) + seatId.charCodeAt(1);
    return hash % 6 === 0;
  };

  const handleToggleSeat = (seatCode) => {
    if (selectedSeats.includes(seatCode)) {
      setSelectedSeats(selectedSeats.filter(s => s !== seatCode));
    } else {
      setSelectedSeats([...selectedSeats, seatCode]);
    }
  };

  const handleConfirmSeats = () => {
    if (selectedSeats.length === 0) return;
    setShowSeatModal(false);
    // Launch concession upsell drawer immediately
    setShowUpsell(true);
  };

  // Concessions update
  const updateConcessionQty = (id, delta) => {
    setCart(prev => {
      const current = prev[id] || 0;
      const next = current + delta;
      if (next <= 0) {
        const copy = { ...prev };
        delete copy[id];
        return copy;
      }
      return { ...prev, [id]: next };
    });
  };

  // Calculations
  const ticketPrice = 90000; // Standard price
  const ticketsTotal = selectedSeats.length * ticketPrice;

  const concessionsTotal = useMemo(() => {
    return Object.entries(cart).reduce((sum, [id, qty]) => {
      const item = concessions.find(c => String(c.id) === String(id));
      return sum + (item ? item.price * qty : 0);
    }, 0);
  }, [cart, concessions]);

  const grandTotal = ticketsTotal + concessionsTotal;

  // Checkout submit
  const handleCheckout = () => {
    if (!selectedMovie || !selectedShowtime || selectedSeats.length === 0) return;

    // Create new transaction block
    const ticketId = `TKT-${Math.floor(1000 + Math.random() * 9000)}-${Math.floor(1000 + Math.random() * 9000)}`;
    const newTicket = {
      id: ticketId,
      movieId: selectedMovie.id,
      movieTitle: selectedMovie.title,
      showtimeId: selectedShowtime.id,
      time: selectedShowtime.time,
      date: selectedShowtime.date,
      theaterId: selectedTheater.id,
      theaterName: selectedTheater.name,
      hallId: selectedHall.id,
      hallName: selectedHall.name,
      seats: selectedSeats,
      amount: grandTotal,
      status: 'DA_KIEM_TRA', // checked in
      concessions: Object.entries(cart).map(([id, qty]) => {
        const item = concessions.find(c => String(c.id) === String(id));
        return {
          id: item.id,
          name: item.name,
          qty,
          price: item.price
        };
      }),
      createdAt: new Date().toISOString()
    };

    // Save to shared database store
    const freshTicketsStr = localStorage.getItem('lora_tickets');
    const freshTickets = freshTicketsStr ? JSON.parse(freshTicketsStr) : tickets;
    const updatedTickets = [newTicket, ...freshTickets];
    
    setTickets(updatedTickets);
    localStorage.setItem('lora_tickets', JSON.stringify(updatedTickets));

    // Update local shifts transaction log
    const ledgerEntry = {
      id: ticketId,
      movieTitle: selectedMovie.title,
      seats: [...selectedSeats],
      showtimeId: selectedShowtime.id,
      amount: grandTotal,
      status: 'SUCCESS',
      createdAt: new Date().toISOString()
    };
    setRecentTransactions(prev => [ledgerEntry, ...prev]);

    // Also update dynamic booked seats
    setShowtimeBookedSeats(prev => {
      const current = prev[selectedShowtime.id] || [];
      return {
        ...prev,
        [selectedShowtime.id]: [...current, ...selectedSeats]
      };
    });

    // Show success dialog
    setCreatedTicket(newTicket);
    setShowInvoiceModal(true);

    // Reset local selection states
    setSelectedMovie(null);
    setSelectedShowtime(null);
    setSelectedTheater(null);
    setSelectedHall(null);
    setSelectedSeats([]);
    setCart({});
    setShowUpsell(false);
  };

  const executeVoid = (txn) => {
    // 1. Flip invoice status in recentTransactions
    setRecentTransactions(prev => 
      prev.map(t => t.id === txn.id ? { ...t, status: 'VOIDED' } : t)
    );

    // 2. Release the seats back to available in showtimeBookedSeats
    setShowtimeBookedSeats(prev => {
      const current = prev[txn.showtimeId] || [];
      const updated = current.filter(seat => !txn.seats.includes(seat));
      return {
        ...prev,
        [txn.showtimeId]: updated
      };
    });

    // 3. Update global tickets list
    const freshTicketsStr = localStorage.getItem('lora_tickets');
    const freshTickets = freshTicketsStr ? JSON.parse(freshTicketsStr) : tickets;
    const updatedTickets = freshTickets.map(t => 
      t.id === txn.id ? { ...t, status: 'VOIDED', amount: 0 } : t
    );
    setTickets(updatedTickets);
    localStorage.setItem('lora_tickets', JSON.stringify(updatedTickets));

    // Show success toast
    setToast({
      show: true,
      message: "Giám sát đã duyệt hủy hóa đơn thành công",
      type: "success"
    });
    setTimeout(() => setToast(prev => ({ ...prev, show: false })), 4050);
  };

  const handleRequestVoid = (txn) => {
    if (user?.role === 'ROLE_SUPERVISOR') {
      // Direct Supervisor Behavior
      executeVoid(txn);
    } else {
      // Standard Staff Behavior -> Show authentication override modal
      setTransactionToVoid(txn);
      setOverrideEmail('');
      setOverridePassword('');
      setOverrideError('');
      setShowOverrideModal(true);
    }
  };

  const handleConfirmOverride = (e) => {
    e.preventDefault();
    if (overrideEmail === 'supervisor@lorafilm.com' && overridePassword === 'password123') {
      executeVoid(transactionToVoid);
      setShowOverrideModal(false);
      setTransactionToVoid(null);
    } else {
      setOverrideError("Sai tài khoản Giám sát. Từ chối cấp quyền truy cập.");
    }
  };

  const handleVoid = () => {
    setSelectedMovie(null);
    setSelectedShowtime(null);
    setSelectedTheater(null);
    setSelectedHall(null);
    setSelectedSeats([]);
    setCart({});
    alert("Giao dịch đã được hủy khẩn cấp bởi Giám sát ca!");
  };

  return (
    <div className="flex-grow flex flex-col space-y-6 relative h-full">
      {/* Title & Search bar */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 border-b border-zinc-800 pb-5">
        <div>
          <h2 className="text-xl font-black text-white uppercase tracking-wider">ĐẶT VÉ TẠI QUẦY (POS)</h2>
          <p className="text-xs text-zinc-500 uppercase tracking-widest mt-1">Lập vé, chọn ghế &amp; xuất bán Combo dịch vụ ăn uống nhanh chóng</p>
        </div>

        <div className="flex items-center gap-3 w-full md:w-80 bg-zinc-900 border border-zinc-800 px-4 py-2 rounded-xl">
          <Search className="w-4 h-4 text-zinc-500 shrink-0" />
          <input
            type="text"
            placeholder="Tìm nhanh tên phim..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="bg-transparent border-none outline-none text-xs w-full text-white placeholder-zinc-650"
          />
        </div>
      </div>

      <div className="flex-1 grid grid-cols-1 xl:grid-cols-3 gap-6 overflow-hidden min-h-[500px]">
        {/* Left Core Area: Movies & Showtimes */}
        <div className="xl:col-span-2 overflow-y-auto space-y-4 pr-2 max-h-[calc(100vh-180px)]">
          <h3 className="text-xs font-black uppercase text-zinc-400 tracking-wider">DANH SÁCH PHIM ĐANG CHIẾU</h3>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {filteredMovies.map(movie => {
              const movieShowtimes = showtimes.filter(st => String(st.movieId) === String(movie.id));
              
              return (
                <div 
                  key={movie.id}
                  className="bg-zinc-900/60 border border-zinc-800 rounded-2xl shadow-xl p-4 flex gap-4 hover:border-zinc-700 transition-all duration-300"
                >
                  <img
                    src={movie.imageUrl || movie.posterUrl}
                    alt={movie.title}
                    className="w-20 h-28 object-cover rounded-xl border border-zinc-800 shrink-0 bg-zinc-950"
                    onError={(e) => {
                      e.target.onerror = null;
                      e.target.src = 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=600&auto=format&fit=crop&q=80';
                    }}
                  />
                  <div className="flex flex-col justify-between flex-1 min-w-0">
                    <div>
                      <h4 className="font-bold text-white text-sm truncate leading-snug" title={movie.title}>
                        {movie.title}
                      </h4>
                      <p className="text-zinc-500 text-[10px] uppercase font-bold mt-0.5">
                        {movie.duration} Phút | Phân loại: {movie.rating || 'T16'}
                      </p>
                    </div>

                    <div className="space-y-1">
                      <p className="text-[9px] text-zinc-600 font-bold uppercase tracking-wider">Khung giờ xếp lịch:</p>
                      <div className="flex flex-wrap gap-1.5 max-h-[64px] overflow-y-auto pr-1">
                        {movieShowtimes.length > 0 ? (
                          movieShowtimes.map(st => {
                            const t = theaters.find(th => String(th.id) === String(st.theaterId));
                            const h = t?.halls?.find(hall => String(hall.id) === String(st.hallId));
                            return (
                              <button
                                key={st.id}
                                onClick={() => handleSelectShowtime(movie, st)}
                                className="bg-zinc-950 hover:bg-amber-500 hover:text-black border border-zinc-800 text-[9px] font-mono font-bold py-1 px-2 rounded-lg text-zinc-300 transition-all select-none"
                              >
                                {st.time} ({h?.name || 'Hall'})
                              </button>
                            );
                          })
                        ) : (
                          <span className="text-[9px] text-zinc-700 italic">Không có suất chiếu hôm nay</span>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}

            {filteredMovies.length === 0 && (
              <div className="col-span-full py-12 text-center text-zinc-600 text-xs italic">
                Không tìm thấy phim nào đang chiếu.
              </div>
            )}
          </div>

          {/* Shift Transaction Ledger */}
          <div className="border-t border-zinc-800 pt-8 mt-6 space-y-4">
            <div className="flex items-center gap-2">
              <span className="w-1.5 h-6 bg-amber-500 rounded-full" />
              <h3 className="text-sm font-black uppercase text-white tracking-wider">LỊCH SỬ GIAO DỊCH TRONG CA</h3>
            </div>

            {recentTransactions.length > 0 ? (
              <div className="overflow-x-auto border border-zinc-800 rounded-2xl bg-zinc-900/40">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-zinc-800 bg-zinc-950 text-[10px] text-zinc-400 font-bold uppercase tracking-wider">
                      <th className="py-3 px-4">Mã hóa đơn</th>
                      <th className="py-3 px-4">Tên Phim</th>
                      <th className="py-3 px-4 font-mono text-center">Ghế</th>
                      <th className="py-3 px-4 text-center">Tổng tiền</th>
                      <th className="py-3 px-4 text-center">Trạng thái</th>
                      <th className="py-3 px-4 text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-850 text-xs">
                    {recentTransactions.map((txn) => (
                      <tr key={txn.id} className="hover:bg-zinc-900/30 transition-colors">
                        <td className="py-3 px-4 font-mono text-zinc-400 font-bold">{txn.id}</td>
                        <td className="py-3 px-4 text-white font-bold max-w-[150px] truncate">{txn.movieTitle}</td>
                        <td className="py-3 px-4 font-mono text-amber-500 font-bold text-center">{txn.seats.join(', ')}</td>
                        <td className="py-3 px-4 font-mono text-emerald-400 font-bold text-center">{txn.amount.toLocaleString('vi-VN')} đ</td>
                        <td className="py-3 px-4 text-center">
                          {txn.status === 'SUCCESS' ? (
                            <span className="inline-flex items-center gap-1 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-[9px] font-bold px-2 py-0.5 rounded uppercase">
                              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                              SUCCESS
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 bg-red-500/10 text-red-400 border border-red-500/20 text-[9px] font-bold px-2 py-0.5 rounded uppercase">
                              <span className="w-1.5 h-1.5 rounded-full bg-red-400" />
                              VOIDED
                            </span>
                          )}
                        </td>
                        <td className="py-3 px-4 text-right">
                          {txn.status === 'SUCCESS' && (
                            <button
                              onClick={() => handleRequestVoid(txn)}
                              className="inline-flex items-center gap-1.5 bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/20 hover:border-red-500/40 py-1.5 px-3 rounded-lg text-[10px] font-bold uppercase tracking-wider transition-all"
                            >
                              <FileX className="w-3 h-3" />
                              <span>Hủy vé</span>
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="py-8 text-center text-zinc-650 text-xs italic border border-zinc-800 border-dashed rounded-2xl">
                Chưa có giao dịch nào được thực hiện trong ca làm việc này.
              </div>
            )}
          </div>
        </div>

        {/* Right Checkout Order Summary Container */}
        <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6 flex flex-col justify-between h-fit space-y-6 sticky top-0 shadow-2xl">
          <div className="space-y-4">
            <h3 className="text-xs font-black uppercase text-zinc-400 tracking-wider border-b border-zinc-800 pb-3">
              HÓA ĐƠN CHI TIẾT TẠM TÍNH
            </h3>

            {selectedMovie && selectedShowtime ? (
              <div className="space-y-3.5 text-xs">
                <div className="bg-zinc-950/40 border border-zinc-800/60 rounded-xl p-4 space-y-2">
                  <div className="font-bold text-white text-sm">{selectedMovie.title}</div>
                  <div className="text-zinc-400 text-[11px] font-mono">
                    {selectedTheater?.name} - {selectedHall?.name} ({selectedHall?.format})
                  </div>
                  <div className="text-amber-500 text-[11px] font-mono font-bold">
                    {selectedShowtime.time} | {selectedShowtime.date}
                  </div>
                </div>

                {/* Seat tags */}
                {selectedSeats.length > 0 && (
                  <div className="flex flex-wrap gap-1 items-center">
                    <span className="text-zinc-500 font-bold mr-1">Ghế chọn:</span>
                    {selectedSeats.map(seat => (
                      <span 
                        key={seat}
                        className="bg-amber-500/10 text-amber-500 border border-amber-500/20 px-2 py-0.5 rounded font-mono font-bold text-[10px]"
                      >
                        {seat}
                      </span>
                    ))}
                  </div>
                )}

                {/* Concessions list inside summary */}
                {Object.keys(cart).length > 0 && (
                  <div className="border-t border-zinc-850 pt-3 space-y-2">
                    <div className="text-zinc-500 font-bold uppercase text-[10px]">Combo bắp nước bán kèm:</div>
                    <div className="space-y-1 max-h-[120px] overflow-y-auto pr-1">
                      {Object.entries(cart).map(([id, qty]) => {
                        const item = concessions.find(c => String(c.id) === String(id));
                        if (!item) return null;
                        return (
                          <div key={id} className="flex justify-between text-[11px] text-zinc-300 font-mono">
                            <span>{item.name} x{qty}</span>
                            <span>{(item.price * qty).toLocaleString('vi-VN')} đ</span>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <div className="py-12 text-center text-zinc-650 text-xs italic">
                Vui lòng chọn phim &amp; khung giờ để bắt đầu giao dịch
              </div>
            )}
          </div>

          <div className="border-t border-zinc-800 pt-4 space-y-4">
            <div className="flex justify-between items-center">
              <span className="text-zinc-500 text-xs font-bold uppercase">Tổng cộng:</span>
              <span className="text-emerald-400 font-mono text-xl font-bold">
                {grandTotal.toLocaleString('vi-VN')} đ
              </span>
            </div>

            {user?.role === 'ROLE_SUPERVISOR' ? (
              <button
                type="button"
                onClick={handleVoid}
                className="w-full border border-red-500/30 bg-red-500/10 text-red-400 font-bold py-2 rounded-xl text-xs uppercase tracking-wider mb-2 transition-colors hover:bg-red-500/20 flex items-center justify-center gap-2"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>HỦY VÉ KHẨN CẤP (VOID)</span>
              </button>
            ) : user?.role === 'ROLE_STAFF' ? (
              <div className="text-[10px] text-zinc-500 italic text-center mb-2 font-medium">
                Yêu cầu thẻ Giám sát để thực hiện hoàn/hủy vé
              </div>
            ) : null}

            <button
              onClick={handleCheckout}
              disabled={!selectedShowtime || selectedSeats.length === 0}
              className={`w-full py-3.5 rounded-xl font-black uppercase text-xs tracking-wider transition-all select-none ${
                selectedShowtime && selectedSeats.length > 0
                  ? 'bg-amber-500 hover:bg-amber-600 text-black shadow-lg shadow-amber-500/10'
                  : 'bg-zinc-850 text-zinc-600 cursor-not-allowed border border-zinc-800'
              }`}
            >
              XUẤT VÉ &amp; IN HÓA ĐƠN
            </button>
          </div>
        </div>
      </div>

      {/* ➌ Interactive Seat Selection popup canvas modal */}
      {showSeatModal && (
        <div className="fixed inset-0 bg-black/90 backdrop-blur-md flex items-center justify-center z-50 p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 max-w-2xl w-full flex flex-col shadow-2xl relative">
            <button
              onClick={() => setShowSeatModal(false)}
              className="absolute top-4 right-4 p-1 bg-zinc-950 border border-zinc-800 text-zinc-500 hover:text-white rounded-lg transition-colors"
            >
              <X className="w-4 h-4" />
            </button>

            <div className="border-b border-zinc-800 pb-3 mb-6">
              <h3 className="text-base font-black text-white uppercase tracking-wider">CHỌN CHỖ NGỒI PHÒNG CHIẾU</h3>
              <p className="text-[11px] text-zinc-400 font-mono mt-1">
                {selectedMovie?.title} | {selectedTheater?.name} - {selectedHall?.name} ({selectedHall?.format})
              </p>
            </div>

            {/* Screen layout banner */}
            <div className="w-full flex flex-col items-center mb-8">
              <div className="w-4/5 h-1.5 bg-amber-500/80 rounded shadow-[0_4px_20px_rgba(245,158,11,0.5)] mb-2" />
              <span className="text-[9px] text-zinc-500 font-black uppercase tracking-widest">MÀN HÌNH CHÍNH DIỆN</span>
            </div>

            {/* Grid of Seats */}
            <div className="flex-1 overflow-x-auto py-2 flex justify-center mb-6">
              <div className="grid gap-2 select-none" style={{ gridTemplateColumns: `repeat(${seatCols.length + 1}, minmax(0, 1fr))` }}>
                {/* Empty corner helper */}
                <div className="w-8 h-8 flex items-center justify-center text-[10px] text-zinc-600 font-bold uppercase shrink-0" />
                
                {/* Cols header labels */}
                {seatCols.map(col => (
                  <div key={`col-${col}`} className="w-8 h-8 flex items-center justify-center text-[9px] text-zinc-500 font-mono font-bold shrink-0">
                    {col}
                  </div>
                ))}

                {seatRows.map(row => {
                  const cells = [];
                  // Row label
                  cells.push(
                    <div key={`row-${row}`} className="w-8 h-8 flex items-center justify-center text-[10px] text-zinc-400 font-bold uppercase shrink-0">
                      {row}
                    </div>
                  );

                  seatCols.forEach(col => {
                    const seatCode = `${row}${col}`;
                    const booked = isSeatBooked(row, col);
                    const selected = selectedSeats.includes(seatCode);

                    cells.push(
                      <button
                        key={seatCode}
                        onClick={() => !booked && handleToggleSeat(seatCode)}
                        disabled={booked}
                        className={`w-8 h-8 rounded-lg flex items-center justify-center text-[10px] font-mono font-bold transition-all shrink-0 ${
                          booked 
                            ? 'bg-zinc-950 border border-zinc-900 text-zinc-800 cursor-not-allowed'
                            : selected
                              ? 'bg-amber-500 text-black font-black border border-amber-600'
                              : 'bg-zinc-900/60 border border-zinc-800 text-zinc-400 hover:border-zinc-600'
                        }`}
                      >
                        {seatCode}
                      </button>
                    );
                  });

                  return cells;
                })}
              </div>
            </div>

            {/* Seat statuses guide legend */}
            <div className="flex justify-center gap-6 text-[10px] font-bold uppercase tracking-wider mb-6 border-t border-zinc-800/60 pt-4">
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded bg-zinc-900 border border-zinc-800 inline-block" />
                <span className="text-zinc-500">Còn trống</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded bg-amber-500 inline-block" />
                <span className="text-amber-400">Đang chọn</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded bg-zinc-950 border border-zinc-900 inline-block" />
                <span className="text-zinc-700">Đã bán</span>
              </div>
            </div>

            <div className="flex justify-between items-center gap-4">
              <div className="text-xs">
                <span className="text-zinc-500">Đã chọn:</span>{' '}
                <span className="font-mono font-bold text-amber-500">
                  {selectedSeats.length > 0 ? selectedSeats.join(', ') : 'Chưa chọn'}
                </span>
              </div>
              <button
                onClick={handleConfirmSeats}
                disabled={selectedSeats.length === 0}
                className={`px-6 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wide select-none ${
                  selectedSeats.length > 0
                    ? 'bg-amber-500 hover:bg-amber-600 text-black'
                    : 'bg-zinc-800 text-zinc-500 cursor-not-allowed border border-zinc-850'
                }`}
              >
                XÁC NHẬN CHỖ NGỒI
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ➍ Concession Upsell Drawer Interceptor Panel */}
      {showUpsell && (
        <div className="fixed inset-0 bg-black/60 z-40 flex justify-end">
          {/* Backdrop cancel trigger click */}
          <div className="flex-grow" onClick={() => setShowUpsell(false)} />
          
          <div className="w-full sm:w-[450px] bg-zinc-900 border-l border-zinc-800 shadow-2xl h-full p-6 flex flex-col justify-between overflow-y-auto animate-slide-in">
            <div>
              <div className="flex items-center justify-between border-b border-zinc-800 pb-3 mb-6">
                <div>
                  <h3 className="text-base font-black text-white uppercase tracking-wider">BÁN KÈM BẮP NƯỚC</h3>
                  <p className="text-[10px] text-zinc-500 uppercase tracking-widest mt-0.5">Tăng doanh số suất ăn nhẹ đi kèm vé</p>
                </div>
                <button
                  onClick={() => setShowUpsell(false)}
                  className="p-1.5 bg-zinc-950 border border-zinc-800 text-zinc-500 hover:text-white rounded-lg"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              {/* Items listing cards */}
              <div className="space-y-4">
                {concessions.map(item => {
                  const qty = cart[item.id] || 0;
                  return (
                    <div 
                      key={item.id}
                      className="bg-zinc-950 border border-zinc-800/80 p-4 rounded-xl flex items-center justify-between gap-4 hover:border-zinc-700 transition-colors"
                    >
                      <div className="min-w-0 flex-1">
                        <h4 className="font-bold text-white text-xs truncate">{item.name}</h4>
                        <p className="text-zinc-500 text-[10px] mt-0.5 truncate">{item.details}</p>
                        <p className="text-amber-500 font-mono font-bold text-xs mt-1.5">
                          {item.price.toLocaleString('vi-VN')} đ
                        </p>
                      </div>

                      <div className="flex items-center gap-3 shrink-0 select-none">
                        <button
                          onClick={() => updateConcessionQty(item.id, -1)}
                          className="w-7 h-7 bg-zinc-900 border border-zinc-800 rounded-lg flex items-center justify-center text-zinc-400 hover:text-white hover:border-zinc-750 transition-colors"
                        >
                          <Minus className="w-3.5 h-3.5" />
                        </button>
                        <span className="text-xs font-mono font-bold w-4 text-center text-zinc-200">{qty}</span>
                        <button
                          onClick={() => updateConcessionQty(item.id, 1)}
                          className="w-7 h-7 bg-zinc-900 border border-zinc-800 rounded-lg flex items-center justify-center text-zinc-400 hover:text-white hover:border-zinc-750 transition-colors"
                        >
                          <Plus className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            <div className="border-t border-zinc-800 pt-6 mt-6 space-y-4">
              <div className="flex justify-between items-center">
                <span className="text-xs text-zinc-500 uppercase font-black">Cộng dồn dịch vụ:</span>
                <span className="text-sm font-mono font-bold text-amber-500">
                  {concessionsTotal.toLocaleString('vi-VN')} đ
                </span>
              </div>
              <button
                onClick={() => setShowUpsell(false)}
                className="w-full bg-amber-500 hover:bg-amber-600 text-black font-black py-3.5 rounded-xl text-xs uppercase tracking-wider select-none shadow-lg shadow-amber-500/10 flex items-center justify-center gap-2"
              >
                <Check className="w-4 h-4" />
                <span>ÁP DỤNG &amp; TIẾP TỤC</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Invoice Ticket Success modal */}
      {showInvoiceModal && createdTicket && (
        <div className="fixed inset-0 bg-black/95 backdrop-blur-md flex items-center justify-center z-50 p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-8 max-w-md w-full shadow-2xl relative text-center">
            <div className="w-16 h-16 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 flex items-center justify-center mx-auto mb-4">
              <Check className="w-8 h-8" />
            </div>

            <h3 className="text-lg font-black text-white uppercase tracking-wider mb-1">XUẤT VÉ THÀNH CÔNG</h3>
            <p className="text-xs text-zinc-500">Giao dịch đã được ghi nhận vào sổ cái lịch sử hệ thống</p>

            <div className="bg-zinc-950 border border-zinc-850/80 rounded-2xl p-5 my-6 text-left space-y-3.5">
              <div className="flex justify-between items-center border-b border-zinc-850 pb-2.5">
                <span className="text-[10px] text-zinc-500 font-bold uppercase">Mã Hoá Đơn</span>
                <span className="text-xs font-mono font-bold text-amber-500">{createdTicket.id}</span>
              </div>

              <div className="space-y-1.5 text-xs text-zinc-350">
                <div><span className="text-zinc-500">Phim:</span> <span className="text-white font-bold">{createdTicket.movieTitle}</span></div>
                <div><span className="text-zinc-500">Suất chiếu:</span> <span className="text-white font-mono">{createdTicket.time} | {createdTicket.date}</span></div>
                <div><span className="text-zinc-500">Rạp:</span> <span className="text-white">{createdTicket.theaterName} - {createdTicket.hallName}</span></div>
                <div><span className="text-zinc-500">Ghế ngồi:</span> <span className="text-emerald-400 font-mono font-bold">{createdTicket.seats.join(', ')}</span></div>
              </div>

              {createdTicket.concessions?.length > 0 && (
                <div className="border-t border-zinc-850 pt-2.5 space-y-1 text-xs">
                  <div className="text-zinc-500 text-[10px] uppercase font-bold">Dịch vụ đi kèm:</div>
                  {createdTicket.concessions.map((item, idx) => (
                    <div key={idx} className="flex justify-between text-[11px] text-zinc-400 font-mono">
                      <span>{item.name} x{item.qty}</span>
                      <span>{(item.price * item.qty).toLocaleString('vi-VN')} đ</span>
                    </div>
                  ))}
                </div>
              )}

              <div className="border-t border-zinc-850 pt-2.5 mt-2 flex justify-between text-xs font-black">
                <span className="text-zinc-400 uppercase">Tổng cộng:</span>
                <span className="text-emerald-400 text-sm font-mono">{createdTicket.amount.toLocaleString('vi-VN')} đ</span>
              </div>
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => setShowInvoiceModal(false)}
                className="flex-1 bg-zinc-950 hover:bg-zinc-850 border border-zinc-850 text-zinc-300 font-bold py-3.5 rounded-xl text-xs uppercase tracking-wider transition-colors"
              >
                Đóng
              </button>
              <button
                onClick={() => {
                  alert(`Đang in hoá đơn ${createdTicket.id}...`);
                  setShowInvoiceModal(false);
                }}
                className="flex-1 bg-amber-500 hover:bg-amber-600 text-black font-black py-3.5 rounded-xl text-xs uppercase tracking-wider flex items-center justify-center gap-2 select-none shadow-lg shadow-amber-500/10"
              >
                <Printer className="w-3.5 h-3.5" />
                <span>IN HOÁ ĐƠN</span>
              </button>
            </div>
          </div>
        </div>
      )}
      {/* Toast Notification */}
      {toast.show && (
        <div className={`fixed bottom-6 right-6 z-50 flex items-center gap-3 px-5 py-3.5 rounded-2xl border shadow-2xl animate-slide-in ${
          toast.type === 'success' 
            ? 'bg-zinc-900 border-emerald-500/30 text-emerald-400' 
            : 'bg-zinc-900 border-red-500/30 text-red-400'
        }`}>
          {toast.type === 'success' ? <Check className="w-5 h-5 shrink-0" /> : <X className="w-5 h-5 shrink-0" />}
          <span className="text-xs font-bold uppercase tracking-wider">{toast.message}</span>
        </div>
      )}

      {/* Supervisor Pin-Code Override Modal */}
      {showOverrideModal && transactionToVoid && (
        <div className="fixed inset-0 bg-black/85 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-zinc-900 border border-zinc-800 p-6 rounded-2xl max-w-sm w-full shadow-2xl z-50 fixed inset-0 m-auto h-fit animate-zoom-in">
            <div className="text-center space-y-4">
              <div className="w-12 h-12 rounded-full bg-amber-500/10 border border-amber-500/20 text-amber-500 flex items-center justify-center mx-auto">
                <Lock className="w-6 h-6" />
              </div>
              
              <div>
                <h3 className="text-sm font-black text-white uppercase tracking-wider">YÊU CẦU XÁC THỰC GIÁM SÁT</h3>
                <p className="text-[11px] text-zinc-400 mt-2 leading-relaxed">
                  Nhân viên không có quyền hủy hóa đơn. Vui lòng yêu cầu Giám sát ca duyệt phê duyệt giao dịch này.
                </p>
              </div>

              {overrideError && (
                <div className="p-3 bg-red-500/10 border border-red-500/20 text-red-400 rounded-xl text-[10px] font-bold text-center">
                  {overrideError}
                </div>
              )}

              <form onSubmit={handleConfirmOverride} className="space-y-3.5 text-left">
                <div>
                  <label className="text-[10px] text-zinc-500 uppercase font-bold tracking-wider block mb-1">Tài khoản Giám sát</label>
                  <div className="flex items-center gap-2 bg-zinc-950 border border-zinc-850 px-3 py-2 rounded-xl">
                    <span className="text-zinc-500 text-xs">@</span>
                    <input
                      type="email"
                      required
                      placeholder="supervisor@lorafilm.com"
                      value={overrideEmail}
                      onChange={(e) => setOverrideEmail(e.target.value)}
                      className="bg-transparent border-none outline-none text-xs w-full text-white placeholder-zinc-700"
                    />
                  </div>
                </div>

                <div>
                  <label className="text-[10px] text-zinc-500 uppercase font-bold tracking-wider block mb-1">Mật khẩu xác nhận</label>
                  <div className="flex items-center gap-2 bg-zinc-950 border border-zinc-850 px-3 py-2 rounded-xl">
                    <Lock className="w-3.5 h-3.5 text-zinc-650 shrink-0" />
                    <input
                      type="password"
                      required
                      placeholder="••••••••"
                      value={overridePassword}
                      onChange={(e) => setOverridePassword(e.target.value)}
                      className="bg-transparent border-none outline-none text-xs w-full text-white placeholder-zinc-700"
                    />
                  </div>
                </div>

                <div className="flex gap-2.5 pt-2">
                  <button
                    type="button"
                    onClick={() => {
                      setShowOverrideModal(false);
                      setTransactionToVoid(null);
                    }}
                    className="flex-1 bg-zinc-950 hover:bg-zinc-850 border border-zinc-850 text-zinc-400 font-bold py-2.5 rounded-xl text-xs uppercase tracking-wider transition-colors"
                  >
                    Hủy
                  </button>
                  <button
                    type="submit"
                    className="flex-1 bg-amber-500 hover:bg-amber-600 text-black font-black py-2.5 rounded-xl text-xs uppercase tracking-wider flex items-center justify-center gap-1.5 transition-colors shadow-lg shadow-amber-500/10"
                  >
                    <Unlock className="w-3.5 h-3.5" />
                    <span>XÁC NHẬN HỦY VÉ</span>
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
