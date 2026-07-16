import { useState, useEffect, useMemo, useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
 
 
// eslint-disable-next-line no-unused-vars
import { ArrowLeft, CheckCircle, Info, Clock, AlertTriangle, ShieldAlert, Film, ShieldCheck } from 'lucide-react';
import { getShowtimeDetail, getSeatLayout } from '@/features/catalog/customer/services/movieService';

export default function SeatSelectionPage() {
  const location = useLocation();
  const navigate = useNavigate();

  // Extract showtimeId from query params
  const showtimeId = useMemo(() => {
    const params = new URLSearchParams(location.search);
    return params.get('showtimeId');
  }, [location.search]);

  const [showtime, setShowtime] = useState(null);
  const [layout, setLayout] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const [selectedSeats, setSelectedSeats] = useState([]);
  const [toastMessage, setToastMessage] = useState('');
  const [showSuccessModal, setShowSuccessModal] = useState(false);
  const [showGapModal, setShowGapModal] = useState(false);

  const fetchLayoutData = useCallback(async () => {
    if (!showtimeId) {
      setError("Thiếu thông tin suất chiếu.");
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const showtimeData = await getShowtimeDetail(showtimeId);
      const layoutData = await getSeatLayout(showtimeId);
      setShowtime(showtimeData);
      setLayout(layoutData);
    } catch (err) {
      setError(err.message || "Không thể tải sơ đồ ghế ngồi.");
    } finally {
      setLoading(false);
    }
  }, [showtimeId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchLayoutData();
  }, [fetchLayoutData]);

  // Group seats by row
  const seatsByRow = useMemo(() => {
    if (!layout || !layout.seats) return {};
    const grouped = {};
    layout.seats.forEach(seat => {
      const row = seat.rowLabel;
      if (!grouped[row]) {
        grouped[row] = [];
      }
      grouped[row].push(seat);
    });

    // Sort seats in each row by column position
    Object.keys(grouped).forEach(row => {
      grouped[row].sort((a, b) => a.positionColumn - b.positionColumn);
    });

    return grouped;
  }, [layout]);

  // Handle seat click selection
  const handleSeatClick = (seat) => {
    if (seat.blockedForShowtime || seat.status === 'MAINTENANCE') return;

    const isSelected = selectedSeats.some(s => s.publicId === seat.publicId);
    if (isSelected) {
      setSelectedSeats(selectedSeats.filter(s => s.publicId !== seat.publicId));
      setToastMessage('');
    } else {
      if (selectedSeats.length >= 8) {
        showToast('Bạn chỉ được chọn tối đa 8 ghế cho mỗi giao dịch!');
        return;
      }
      setSelectedSeats([...selectedSeats, seat]);
      setToastMessage('');
    }
  };

  const showToast = (message) => {
    setToastMessage(message);
    setTimeout(() => setToastMessage(''), 4000);
  };

  // Check for single seat gaps in rows
  const checkSingleSeatGap = () => {
    const rows = Object.keys(seatsByRow);
    for (const row of rows) {
      const rowSeats = seatsByRow[row];
      // Get statuses including selection state
      const seatsWithSelection = rowSeats.map(seat => {
        let seatStatus = 'AVAILABLE';
        if (seat.blockedForShowtime) {
          seatStatus = 'BOOKED';
        } else if (seat.status === 'MAINTENANCE') {
          seatStatus = 'MAINTENANCE';
        } else if (selectedSeats.some(s => s.publicId === seat.publicId)) {
          seatStatus = 'SELECTED';
        }
        return { ...seat, seatStatus };
      });

      // Scan row for isolated empty seats
      for (let i = 0; i < seatsWithSelection.length; i++) {
        if (seatsWithSelection[i].seatStatus === 'AVAILABLE') {
          // Check if isolated by SELECTED or BOOKED seats on both sides
          const leftIsBlocked = i === 0 || ['SELECTED', 'BOOKED', 'MAINTENANCE'].includes(seatsWithSelection[i - 1].seatStatus);
          const rightIsBlocked = i === seatsWithSelection.length - 1 || ['SELECTED', 'BOOKED', 'MAINTENANCE'].includes(seatsWithSelection[i + 1].seatStatus);
          
          // Gap exists if both sides are blocked and at least one is SELECTED (meaning user created the gap)
          if (leftIsBlocked && rightIsBlocked) {
            const leftIsSelected = i > 0 && seatsWithSelection[i - 1].seatStatus === 'SELECTED';
            const rightIsSelected = i < seatsWithSelection.length - 1 && seatsWithSelection[i + 1].seatStatus === 'SELECTED';
            if (leftIsSelected || rightIsSelected) {
              return true;
            }
          }
        }
      }
    }
    return false;
  };

  const handleCheckoutSubmit = () => {
    if (selectedSeats.length === 0) return;
    if (checkSingleSeatGap()) {
      setShowGapModal(true);
      return;
    }
    setShowSuccessModal(true);
  };

  const handleBack = () => {
    if (showtime && showtime.movie) {
      navigate(`/movie/${showtime.movie.slug}`);
    } else {
      navigate('/movies');
    }
  };

  const formatCurrency = (val) => {
    return val.toLocaleString('vi-VN') + 'đ';
  };

  const totalAmount = useMemo(() => {
    return selectedSeats.reduce((sum, seat) => sum + (seat.price || 0), 0);
  }, [selectedSeats]);

  // eslint-disable-next-line no-unused-vars
  const currencyLabel = useMemo(() => {
    if (selectedSeats.length > 0) return selectedSeats[0].currency || 'VND';
    if (layout?.seats?.length > 0) return layout.seats[0].currency || 'VND';
    return 'VND';
  }, [selectedSeats, layout]);

  // Format startTime to friendly date/time
  const formattedDateTime = useMemo(() => {
    if (!showtime || !showtime.startTime) return '';
    const dateObj = new Date(showtime.startTime);
    const dateStr = dateObj.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
    const timeStr = dateObj.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false });
    return { date: dateStr, time: timeStr };
  }, [showtime]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-[#050506] text-white">
        <div className="flex flex-col items-center gap-4 animate-pulse">
          <div className="w-12 h-12 border-4 border-brand-coral border-t-transparent rounded-full animate-spin"></div>
          <p className="text-sm font-semibold tracking-wider text-zinc-400">Đang tải sơ đồ ghế ngồi...</p>
        </div>
      </div>
    );
  }

  if (error || !showtime || !layout) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen bg-brand-dark px-4 text-center">
        <div className="w-16 h-16 rounded-full bg-red-500/10 flex items-center justify-center text-red-500 mb-6">
          <AlertTriangle className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-bold text-zinc-100 mb-2">Không thể tải thông tin phòng chiếu</h2>
        <p className="text-sm text-zinc-400 max-w-md mb-8">
          {error || "Đường dẫn không hợp lệ hoặc suất chiếu này đã kết thúc."}
        </p>
        <button
          onClick={() => navigate('/movies')}
          className="bg-brand-coral hover:bg-opacity-90 text-white font-bold px-6 py-3 rounded-full transition-all text-xs uppercase tracking-wider"
        >
          Quay lại danh sách phim
        </button>
      </div>
    );
  }

  return (
    <div className="bg-zinc-950 text-zinc-100 min-h-screen pt-32 pb-16 px-4 md:px-12 selection:bg-brand-coral selection:text-zinc-950 font-sans font-medium">
      {/* Toast Warning Popup */}
      {toastMessage && (
        <div className="fixed top-24 left-1/2 -translate-x-1/2 z-50 bg-red-600 text-white font-bold py-3 px-6 rounded-xl shadow-2xl flex items-center gap-2 border border-red-500 text-sm">
          <Info className="w-4 h-4 shrink-0" />
          <span>{toastMessage}</span>
        </div>
      )}

      <div className="max-w-7xl mx-auto w-full">
        {/* Main Grid Wrapper */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
          
          {/* LEFT MAIN PANEL: Seat Selection Grid */}
          <div className="lg:col-span-2 space-y-8">
            
            {/* Header Strip */}
            <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <button
                onClick={handleBack}
                className="flex items-center gap-2 text-zinc-400 hover:text-brand-coral transition-colors text-xs font-bold self-start sm:self-auto"
              >
                <ArrowLeft className="w-4 h-4" />
                <span>Quay lại phim</span>
              </button>
              
              <div className="flex flex-wrap items-center gap-x-6 gap-y-1 text-xs text-zinc-400">
                <div>
                  <span className="text-zinc-600 font-bold mr-1">Rạp:</span>
                  <span className="text-zinc-200 font-semibold">{showtime.cinema?.name}</span>
                </div>
                <div>
                  <span className="text-zinc-600 font-bold mr-1">Phòng:</span>
                  <span className="text-zinc-200 font-semibold">{showtime.auditorium?.name}</span>
                </div>
                <div>
                  <span className="text-zinc-600 font-bold mr-1">Suất chiếu:</span>
                  <span className="text-brand-coral font-black">{formattedDateTime.time}</span>
                </div>
                <div>
                  <span className="text-zinc-600 font-bold mr-1">Ngày:</span>
                  <span className="text-zinc-200 font-semibold">{formattedDateTime.date}</span>
                </div>
              </div>
            </div>

            {/* Screen layout & Seats */}
            <div className="bg-zinc-900/40 border border-zinc-800/80 rounded-3xl p-6 md:p-8 flex flex-col justify-center relative overflow-hidden">
              
              {/* Screen Indicator */}
              <div className="w-full max-w-lg mx-auto mb-16 text-center">
                <div className="h-1.5 bg-gradient-to-r from-transparent via-brand-coral to-transparent shadow-[0_0_20px_rgba(216,129,116,0.9)] rounded-full mb-2"></div>
                <span className="text-zinc-500 text-[10px] tracking-[0.4em] font-black uppercase">MÀN HÌNH CHÍNH / MAIN SCREEN</span>
              </div>

              {/* Seating Grid */}
              <div className="w-full overflow-x-auto py-4 scrollbar-thin scrollbar-thumb-zinc-800">
                <div className="min-w-[640px] max-w-2xl mx-auto px-4">
                  <div className="space-y-3">
                    {Object.keys(seatsByRow).map((row) => {
                      const rowSeats = seatsByRow[row];
                      const isCoupleRow = rowSeats.some(s => s.seatType === 'COUPLE');

                      return (
                        <div key={row} className="flex items-center gap-3">
                          {/* Row Label Left */}
                          <span className="w-6 text-center font-black text-xs text-zinc-500">{row}</span>

                          {/* Seats Grid Row */}
                          <div className="flex-grow grid grid-cols-12 gap-2">
                            {rowSeats.map((seat) => {
                              const isOccupied = seat.blockedForShowtime;
                              const isMaintenance = seat.status === 'MAINTENANCE';
                              const isSelected = selectedSeats.some(s => s.publicId === seat.publicId);
                              
                              // eslint-disable-next-line no-useless-assignment
                              let seatClass = '';
                              if (isMaintenance) {
                                seatClass = 'bg-zinc-900 border border-red-500/20 text-red-500/40 cursor-not-allowed opacity-50 relative';
                              } else if (isOccupied) {
                                seatClass = 'bg-zinc-950 border border-zinc-900 text-zinc-700 line-through opacity-30 cursor-not-allowed pointer-events-none';
                              } else if (isSelected) {
                                seatClass = 'bg-emerald-500 border-emerald-400 text-black font-extrabold shadow-lg shadow-emerald-500/20';
                              } else {
                                // Dynamic seat types styling
                                switch (seat.seatType) {
                                  case 'COUPLE':
                                    seatClass = 'bg-rose-600/10 border border-rose-500/40 hover:bg-rose-500/30 text-rose-400';
                                    break;
                                  case 'VIP':
                                    seatClass = 'bg-amber-600/10 border border-amber-500/40 hover:bg-amber-500/30 text-amber-400';
                                    break;
                                  default: // STANDARD
                                    seatClass = 'bg-zinc-800 border border-zinc-700 hover:border-amber-500 text-zinc-400';
                                }
                              }

                              const colSpan = isCoupleRow ? 'col-span-2' : 'col-span-1';

                              return (
                                <button
                                  key={seat.publicId}
                                  disabled={isOccupied || isMaintenance}
                                  onClick={() => handleSeatClick(seat)}
                                  className={`${colSpan} h-9 rounded-lg flex items-center justify-center text-[10px] md:text-xs font-semibold tracking-tighter transition-all duration-200 select-none ${seatClass}`}
                                  title={`Ghế ${seat.seatCode} - ${seat.seatType} - ${formatCurrency(seat.price)}`}
                                >
                                  {isMaintenance ? '🛠️' : seat.seatCode}
                                </button>
                              );
                            })}
                          </div>

                          {/* Row Label Right */}
                          <span className="w-6 text-center font-black text-xs text-zinc-500">{row}</span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>

              {/* Legend map */}
              <div className="flex flex-wrap justify-center gap-6 mt-12 text-xs text-zinc-400 border-t border-zinc-800 pt-6">
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded bg-zinc-800 border border-zinc-700"></div>
                  <span>Ghế thường</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded bg-amber-600/10 border border-amber-500/40"></div>
                  <span>Ghế VIP</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-6 h-4 rounded bg-rose-600/10 border border-rose-500/40"></div>
                  <span>Ghế Đôi</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded bg-emerald-500 border border-emerald-400"></div>
                  <span>Đang chọn</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded bg-zinc-950 border border-zinc-900 line-through opacity-30 flex items-center justify-center text-[8px]">X</div>
                  <span>Đã đặt</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded bg-zinc-900 border border-red-500/20 text-center text-[8px] text-red-500/40">🛠️</div>
                  <span>Bảo trì</span>
                </div>
              </div>
            </div>
          </div>

          {/* RIGHT STICKY PANEL: Booking Details */}
          <div className="lg:col-span-1 sticky top-24 bg-zinc-900 border border-zinc-800 rounded-3xl p-6 space-y-6 shadow-2xl">
            
            {/* Movie Poster & Meta */}
            <div className="flex gap-4 items-start pb-6 border-b border-zinc-800">
              <div className="w-16 aspect-[2/3] rounded-xl overflow-hidden bg-zinc-950 border border-zinc-800 shrink-0">
                <img 
                  src={showtime.movie?.posterUrl} 
                  alt={showtime.movie?.title} 
                  className="w-full h-full object-cover" 
                  onError={(e) => {
                    e.target.onerror = null;
                    e.target.src = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' fill='%2318181b'><rect width='100%' height='100%'/></svg>";
                  }}
                />
              </div>
              <div className="space-y-1.5 flex-grow">
                <span className="inline-block text-[9px] bg-brand-coral/15 text-brand-coral border border-brand-coral/20 px-2 py-0.5 rounded font-black uppercase tracking-wider">
                  {showtime.movieVersion?.versionName || showtime.movieVersion?.format || '2D Digital'}
                </span>
                <h3 className="text-sm font-black text-white line-clamp-2 mt-1 leading-snug">{showtime.movie?.title}</h3>
                <div className="flex items-center gap-1.5 text-[10px] text-zinc-500 font-semibold">
                  <span>{showtime.movie?.durationMinutes} phút</span>
                  <span>•</span>
                  <span className="text-brand-yellow font-black border border-brand-yellow/30 px-1 py-0.2 rounded text-[8px]">{showtime.movie?.ageRating}</span>
                </div>
              </div>
            </div>

            {/* Showtime Info */}
            <div className="space-y-3 py-2 text-xs border-b border-zinc-800">
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Cụm rạp</span>
                <span className="text-white font-bold text-right">{showtime.cinema?.name}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Phòng chiếu</span>
                <span className="text-zinc-200 font-bold text-right">{showtime.auditorium?.name}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Suất chiếu</span>
                <span className="text-brand-coral font-black text-right">{formattedDateTime.time} | {formattedDateTime.date}</span>
              </div>
            </div>

            {/* Selected Seats and cost breakdown */}
            <div className="py-2 border-b border-zinc-800 space-y-4">
              <span className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Ghế đã chọn</span>
              
              {selectedSeats.length > 0 ? (
                <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                  {selectedSeats.map(seat => (
                    <div key={seat.publicId} className="flex justify-between text-xs">
                      <span className="text-zinc-300">Ghế {seat.seatCode} ({seat.seatType})</span>
                      <span className="text-white font-bold">{formatCurrency(seat.price)}</span>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-xs text-zinc-650 italic py-1">Chưa chọn ghế ngồi</div>
              )}
            </div>

            {/* Amount Summary */}
            <div className="flex justify-between items-center py-4 px-4 bg-zinc-950/60 rounded-2xl border border-zinc-850 shadow-inner">
              <div>
                <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Tổng tiền</span>
                <span className="text-[8px] text-zinc-600 block">(Bao gồm VAT)</span>
              </div>
              <span className="text-lg md:text-xl font-black text-brand-coral">{formatCurrency(totalAmount)}</span>
            </div>

            {/* Action button */}
            <button
              disabled={selectedSeats.length === 0}
              onClick={handleCheckoutSubmit}
              className={`w-full py-4 rounded-2xl font-black uppercase text-xs tracking-wider shadow-lg transition-all duration-300 transform ${
                selectedSeats.length > 0
                  ? 'bg-brand-coral hover:bg-opacity-95 hover:scale-[1.02] text-white shadow-brand-coral/25'
                  : 'bg-zinc-850 text-zinc-550 border border-zinc-800 cursor-not-allowed'
              }`}
            >
              Tiếp tục đặt vé
            </button>
          </div>
        </div>
      </div>

      {/* Success Confirmation Modal */}
      {showSuccessModal && (
        <div className="fixed inset-0 bg-black/90 backdrop-blur-md flex items-center justify-center z-50 p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-8 max-w-md w-full shadow-2xl text-center relative overflow-hidden animate-in zoom-in duration-300">
            <div className="absolute -top-24 -left-24 w-48 h-48 bg-brand-coral/10 rounded-full filter blur-3xl pointer-events-none"></div>

            <CheckCircle className="w-16 h-16 text-emerald-500 mx-auto mb-4" />
            
            <h3 className="text-xl font-black text-white uppercase tracking-wide mb-2">CHỌN GHẾ THÀNH CÔNG</h3>
            <p className="text-zinc-400 text-xs mb-6 leading-relaxed">
              Bạn đã chọn các vị trí ghế ngồi thành công cho suất chiếu này:
            </p>

            <div className="bg-zinc-950/60 border border-zinc-800/80 rounded-2xl p-4 text-left space-y-2.5 text-xs mb-6">
              <div className="flex justify-between">
                <span className="text-zinc-500">Phim:</span>
                <span className="text-white font-extrabold text-right ml-4">{showtime.movie?.title}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500">Rạp:</span>
                <span className="text-zinc-200 font-semibold">{showtime.cinema?.name}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500">Phòng chiếu:</span>
                <span className="text-zinc-200 font-semibold">{showtime.auditorium?.name}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500">Suất chiếu:</span>
                <span className="text-white font-extrabold">{formattedDateTime.time} | {formattedDateTime.date}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500">Vị trí ghế:</span>
                <span className="text-emerald-400 font-extrabold text-right ml-4">
                  {selectedSeats.map(s => s.seatCode).join(', ')}
                </span>
              </div>
              <div className="flex justify-between border-t border-zinc-800 pt-2.5 mt-2.5 items-center">
                <span className="text-zinc-500 font-bold">Tổng giá trị:</span>
                <span className="text-brand-coral font-black text-sm">{formatCurrency(totalAmount)}</span>
              </div>
            </div>

            <button
              onClick={() => navigate('/')}
              className="w-full bg-brand-coral hover:bg-opacity-95 text-white font-black py-4 rounded-xl shadow-lg transition-colors duration-300 uppercase tracking-wider text-xs"
            >
              Quay lại Trang Chủ
            </button>
          </div>
        </div>
      )}

      {/* Anti-Single-Seat Gap Warning Modal */}
      {showGapModal && (
        <div className="fixed inset-0 bg-black/90 backdrop-blur-md flex items-center justify-center z-50 p-4">
          <div className="bg-zinc-900 border border-zinc-800 p-6 rounded-2xl max-w-sm w-full fixed inset-0 m-auto h-fit z-50 shadow-2xl animate-zoom-in">
            <div className="text-center">
              <h3 className="text-amber-500 font-black text-sm uppercase tracking-wider mb-2 flex items-center justify-center gap-2">
                <ShieldAlert className="w-5 h-5 text-amber-500 shrink-0" />
                <span>Thông báo vị trí ghế</span>
              </h3>
              <p className="text-xs text-zinc-300 font-medium leading-relaxed mt-3">
                Cách chọn vị trí ghế của bạn không được để trống 1 ghế ở bên trái, bên phải hoặc ở giữa hàng ghế. Vui lòng chọn lại ghế liền kề nhau để không tạo khoảng trống đơn lẻ!
              </p>
              <button
                onClick={() => setShowGapModal(false)}
                className="w-full bg-gradient-to-r from-orange-400 to-amber-500 text-zinc-950 font-black py-2.5 rounded-xl text-xs uppercase tracking-widest mt-4 cursor-pointer text-center block"
              >
                Đồng ý & Chọn lại
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
