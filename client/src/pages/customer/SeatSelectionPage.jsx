import { useState, useMemo, useEffect } from 'react';
import { ArrowLeft, CheckCircle, Info, ShoppingBag, Clock, CreditCard, Smartphone, AlertTriangle, Calendar, ShieldAlert } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { useData } from '../../contexts/DataContext';

const ROWS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'];

const SEAT_PRICES = {
  standard: 80000,
  vip: 110000,
  couple: 220000
};

const CONCESSIONS = [
  {
    id: 'combo-solo',
    name: 'Combo Solo',
    description: '1 Bắp ngọt lớn + 1 Soda (L)',
    price: 75000,
    image: 'https://images.unsplash.com/photo-1578849278619-e73505e9610f?w=300&auto=format&fit=crop&q=80'
  },
  {
    id: 'combo-couple',
    name: 'Combo Couple',
    description: '1 Bắp ngọt lớn + 2 Soda (L)',
    price: 105000,
    image: 'https://images.unsplash.com/photo-1585647347483-22b66260dfff?w=300&auto=format&fit=crop&q=80'
  },
  {
    id: 'combo-party',
    name: 'Combo Party Lora',
    description: '2 Bắp lớn + 3 Soda (L) + 1 Snack khoai tây',
    price: 165000,
    image: 'https://images.unsplash.com/photo-1610483178766-82046db3e477?w=300&auto=format&fit=crop&q=80'
  }
];

export default function SeatSelectionView({ bookingData, onBack, onRequireLogin, onUpdateBookingData }) {
  const { isAuthenticated, user } = useAuth();
  const { movies, showtimes, cinemas } = useData();
  
  // Support restoring selected seats after dynamic redirection authentication
  const [selectedSeats, setSelectedSeats] = useState(bookingData.selectedSeats || []);
  const [selectedConcessions, setSelectedConcessions] = useState({});
  const [toastMessage, setToastMessage] = useState('');
  const [showSuccessModal, setShowSuccessModal] = useState(false);
  const [currentStep, setCurrentStep] = useState(1);
  const [timerSeconds, setTimerSeconds] = useState(600);
  const [timerActive, setTimerActive] = useState(false);
  const [showTimeoutModal, setShowTimeoutModal] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState('domestic');
  const [showGapModal, setShowGapModal] = useState(false);

  const { movieTitle, date, cinema, time, format, movieId } = bookingData;

  // Intercept GUEST immediately if they try to access seats
  useEffect(() => {
    if (!isAuthenticated) {
      if (onRequireLogin) {
        onRequireLogin({
          bookingData: {
            ...bookingData,
            selectedSeats
          }
        });
      }
    }
  }, [isAuthenticated, onRequireLogin, bookingData, selectedSeats]);

  // Timer Countdown Effect
  useEffect(() => {
    if (!timerActive) return;

    const interval = setInterval(() => {
      setTimerSeconds((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          setShowTimeoutModal(true);
          setTimerActive(false);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [timerActive]);

  const handleTimeoutOk = () => {
    setShowTimeoutModal(false);
    setSelectedSeats([]);
    setSelectedConcessions({});
    setTimerSeconds(600);
    setTimerActive(false);
    localStorage.removeItem('lora_pending_booking');
    onBack(); // route back to home
  };

  const checkSingleSeatGap = () => {
    for (const row of ROWS) {
      const rowSelected = selectedSeats.filter(seat => seat.startsWith(row));
      if (rowSelected.length === 0) continue;

      // 🟢 EXCLUSION GUARD: If the row contains couple seats (e.g., type === 'COUPLE' or row === 'J'), skip checking!
      if (row === 'J') {
        continue; // Safely bypass this row, couple seats cannot create invalid single seat gaps
      }

      const seatCount = 12; // Since row J is excluded, other rows always have 12 seats
      const rowSeats = [];
      for (let col = 1; col <= seatCount; col++) {
        const seatId = `${row}${col}`;
        let status = 'AVAILABLE';
        if (selectedSeats.includes(seatId)) {
          status = 'SELECTED';
        } else if (occupiedSeats.has(seatId)) {
          status = 'BOOKED';
        }
        rowSeats.push({ label: seatId, status });
      }

      for (let i = 0; i < rowSeats.length; i++) {
        if (rowSeats[i].status === 'AVAILABLE') {
          // Rule A: Check Left-Side Isolation
          if (i + 1 < rowSeats.length && rowSeats[i + 1].status === 'SELECTED') {
            const leftStatus = (i - 1 < 0) ? 'WALL' : rowSeats[i - 1].status;
            if (leftStatus === 'WALL' || leftStatus === 'BOOKED') {
              return true;
            }
          }

          // Rule B: Check Right-Side Isolation
          if (i - 1 >= 0 && rowSeats[i - 1].status === 'SELECTED') {
            const rightStatus = (i + 1 >= rowSeats.length) ? 'WALL' : rowSeats[i + 1].status;
            if (rightStatus === 'WALL' || rightStatus === 'BOOKED') {
              return true;
            }
          }

          // Rule C: Check Mid-Row Double Splitting
          if (i - 1 >= 0 && i + 1 < rowSeats.length) {
            const leftStatus = rowSeats[i - 1].status;
            const rightStatus = rowSeats[i + 1].status;
            if ((leftStatus === 'SELECTED' || leftStatus === 'BOOKED') &&
                (rightStatus === 'SELECTED' || rightStatus === 'BOOKED') &&
                (leftStatus === 'SELECTED' || rightStatus === 'SELECTED')) {
              return true;
            }
          }
        }
      }
    }
    return false;
  };

  const handleProceedToConcessions = () => {
    if (selectedSeats.length === 0) return;
    if (checkSingleSeatGap()) {
      setShowGapModal(true);
      return;
    }
    setTimerActive(true);
    setCurrentStep(2);
  };

  const handleProceedToPayment = () => {
    setCurrentStep(3);
  };

  const handleHeaderBack = () => {
    if (currentStep === 1) {
      onBack();
    } else if (currentStep === 2) {
      setCurrentStep(1);
      setTimerActive(false);
      setTimerSeconds(600);
    } else if (currentStep === 3) {
      setCurrentStep(2);
    }
  };

  const movie = useMemo(() => {
    return movies.find((m) => String(m.id) === String(movieId)) || {
      title: movieTitle,
      posterUrl: 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=600&auto=format&fit=crop&q=80',
      duration: '120 phút',
      ageRating: 'P'
    };
  }, [movies, movieId, movieTitle]);

  const showtimesList = useMemo(() => {
    const foundCinema = cinemas.find(c => c.name === cinema);
    if (!foundCinema) {
      return ["09:30", "13:15", "16:45", "19:30", "22:15"];
    }
    const matching = showtimes.filter(st => 
      String(st.movieId) === String(movieId) && 
      String(st.cinemaId) === String(foundCinema.id)
    );
    if (matching.length === 0) {
      return ["09:30", "13:15", "16:45", "19:30", "22:15"];
    }
    return [...new Set(matching.map(st => st.time))].sort();
  }, [showtimes, cinemas, movieId, cinema]);

  const hallLabel = useMemo(() => {
    if (format.toLowerCase().includes('imax')) return 'IMAX Theater';
    const hash = (cinema + time).split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    return `Phòng Chiếu ${hash % 5 + 1}`;
  }, [cinema, time, format]);

  // Determine occupied seats based on a deterministic hash so they are stable per showtime
  const occupiedSeats = useMemo(() => {
    const occupied = new Set();
    const hashSeed = (cinema + time + date).split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    
    ROWS.forEach((row) => {
      const seatCount = row === 'J' ? 6 : 12;
      for (let col = 1; col <= seatCount; col++) {
        const label = `${row}${col}`;
        // Pseudo-random assignment using math hash (approx 15-20% occupied)
        const val = (row.charCodeAt(0) * 11 + col * 17 + hashSeed) % 6;
        if (val === 0) {
          occupied.add(label);
        }
      }
    });
    return occupied;
  }, [cinema, time, date]);

  // Handle seat click
  const handleSeatClick = (label) => {
    if (occupiedSeats.has(label)) return;

    if (selectedSeats.includes(label)) {
      setSelectedSeats(selectedSeats.filter((seat) => seat !== label));
      setToastMessage('');
    } else {
      if (selectedSeats.length >= 8) {
        showToast('Bạn chỉ được chọn tối đa 8 ghế cho mỗi giao dịch!');
        return;
      }
      setSelectedSeats([...selectedSeats, label]);
      setToastMessage('');
    }
  };

  // Helper to show custom warning toasts
  const showToast = (message) => {
    setToastMessage(message);
    setTimeout(() => {
      setToastMessage('');
    }, 4000);
  };

  const getSeatType = (row) => {
    if (row === 'J') return 'couple';
    if (['F', 'G', 'H', 'I'].includes(row)) return 'vip';
    return 'standard';
  };

  const handleIncreaseConcession = (id) => {
    setSelectedConcessions(prev => ({
      ...prev,
      [id]: (prev[id] || 0) + 1
    }));
  };

  const handleDecreaseConcession = (id) => {
    setSelectedConcessions(prev => {
      const nextVal = (prev[id] || 0) - 1;
      const nextState = { ...prev };
      if (nextVal <= 0) {
        delete nextState[id];
      } else {
        nextState[id] = nextVal;
      }
      return nextState;
    });
  };

  const totalSeatsPrice = useMemo(() => {
    return selectedSeats.reduce((sum, seat) => {
      const row = seat.charAt(0);
      const type = getSeatType(row);
      return sum + SEAT_PRICES[type];
    }, 0);
  }, [selectedSeats]);

  const totalConcessionsPrice = useMemo(() => {
    return Object.entries(selectedConcessions).reduce((sum, [id, qty]) => {
      const item = CONCESSIONS.find(c => c.id === id);
      return sum + (item ? item.price * qty : 0);
    }, 0);
  }, [selectedConcessions]);

  const totalAmount = useMemo(() => {
    return totalSeatsPrice + totalConcessionsPrice;
  }, [totalSeatsPrice, totalConcessionsPrice]);

  // Format currency in VND
  const formatCurrency = (val) => {
    return val.toLocaleString('vi-VN') + 'đ';
  };

  const formatTime = (secs) => {
    const m = Math.floor(secs / 60).toString().padStart(2, '0');
    const s = (secs % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  };

  const handleCheckoutSubmit = () => {
    if (selectedSeats.length === 0) return;
    if (checkSingleSeatGap()) {
      setShowGapModal(true);
      return;
    }

    if (!isAuthenticated) {
      if (onRequireLogin) {
        onRequireLogin({
          bookingData: {
            ...bookingData,
            selectedSeats
          }
        });
      }
      return;
    }

    const concessionItems = Object.entries(selectedConcessions).map(([id, qty]) => {
      const combo = CONCESSIONS.find(c => c.id === id);
      return {
        id,
        name: combo?.name || '',
        quantity: qty,
        price: combo?.price || 0
      };
    });

    // Generate new ticket object
    const newTicket = {
      id: `TKT-${Math.floor(1000 + Math.random() * 9000)}-${Math.floor(1000 + Math.random() * 9000)}`,
      customerName: user?.fullName || 'Khách Vãng Lai',
      customerEmail: user?.email || 'customer@gmail.com',
      movieTitle: movieTitle,
      theaterName: cinema,
      time: time,
      date: date,
      seats: selectedSeats,
      concessions: concessionItems,
      totalAmount: totalAmount,
      status: 'CHUA_KIEM_TRA',
      paymentMethod: paymentMethod === 'lorapay' ? 'Ví điện tử LoraPay' : 'Thẻ nội địa ATM',
      timestamp: new Date().toISOString()
    };

    // Save to localStorage
    const savedTickets = localStorage.getItem('lora_tickets');
    const ticketList = savedTickets ? JSON.parse(savedTickets) : [];
    ticketList.push(newTicket);
    localStorage.setItem('lora_tickets', JSON.stringify(ticketList));

    // Reset timer
    setTimerActive(false);

    setShowSuccessModal(true);
  };

  return (
    <div className="bg-zinc-950 text-zinc-100 min-h-screen py-10 px-4 md:px-12">
      {/* Toast Warning Popup */}
      {toastMessage && (
        <div className="fixed top-24 left-1/2 -translate-x-1/2 z-50 bg-red-600 text-white font-bold py-3 px-6 rounded-xl shadow-2xl flex items-center gap-2 border border-red-500 text-sm">
          <Info className="w-4 h-4 shrink-0" />
          <span>{toastMessage}</span>
        </div>
      )}

      <div className="max-w-7xl mx-auto w-full">
        {/* Floating countdown banner */}
        {timerActive && (
          <div className="border border-amber-500/20 bg-amber-955/30 rounded-2xl p-4 flex items-center justify-between gap-4 animate-pulse mb-6 text-amber-500">
            <div className="flex items-center gap-3">
              <Clock className="w-5 h-5 shrink-0 text-amber-500" />
              <span className="text-xs md:text-sm font-semibold">
                Ghế của bạn đang được giữ, vui lòng hoàn tất đặt vé trong
              </span>
            </div>
            <span className="text-sm md:text-base font-black bg-zinc-950 px-3 py-1.5 rounded-xl border border-amber-500/20 text-amber-500">
              {formatTime(timerSeconds)}
            </span>
          </div>
        )}

        {/* Main Grid Wrapper */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
          
          {/* LEFT MAIN PANEL (Funnel Flow Steps Container) */}
          <div className="lg:col-span-2 space-y-8">
            
            {/* Header Strip with Back link */}
            <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <button
                onClick={handleHeaderBack}
                className="flex items-center gap-2 text-zinc-400 hover:text-brand-coral transition-colors text-sm font-semibold self-start sm:self-auto"
              >
                <ArrowLeft className="w-4 h-4" />
                <span>{currentStep === 1 ? 'Quay lại phim' : 'Quay lại bước trước'}</span>
              </button>
              
              <div className="flex flex-wrap items-center gap-x-6 gap-y-1 text-xs text-zinc-400">
                <div>
                  <span className="text-zinc-600 font-bold mr-1">Rạp:</span>
                  <span className="text-zinc-200 font-semibold">{cinema}</span>
                </div>
                <div>
                  <span className="text-zinc-600 font-bold mr-1">Suất chiếu:</span>
                  <span className="text-brand-coral font-black">{time}</span>
                </div>
                <div>
                  <span className="text-zinc-600 font-bold mr-1">Ngày:</span>
                  <span className="text-zinc-200 font-semibold">{date}</span>
                </div>
              </div>
            </div>

            {/* Step Indicator Bar */}
            <div className="flex items-center justify-between max-w-md mx-auto mb-8 bg-zinc-900 border border-zinc-800 rounded-2xl p-3">
              <div className="flex items-center gap-2">
                <div className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-black ${
                  currentStep >= 1 ? 'bg-brand-coral text-white' : 'bg-zinc-800 text-zinc-500'
                }`}>
                  1
                </div>
                <span className={`text-[11px] font-bold ${currentStep >= 1 ? 'text-white' : 'text-zinc-500'}`}>Chọn Ghế</span>
              </div>
              
              <div className="h-[2px] w-8 bg-zinc-800 flex-grow mx-3"></div>

              <div className="flex items-center gap-2">
                <div className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-black ${
                  currentStep >= 2 ? 'bg-brand-coral text-white' : 'bg-zinc-800 text-zinc-500'
                }`}>
                  2
                </div>
                <span className={`text-[11px] font-bold ${currentStep >= 2 ? 'text-white' : 'text-zinc-500'}`}>Bắp Nước</span>
              </div>

              <div className="h-[2px] w-8 bg-zinc-800 flex-grow mx-3"></div>

              <div className="flex items-center gap-2">
                <div className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-black ${
                  currentStep >= 3 ? 'bg-brand-coral text-white' : 'bg-zinc-800 text-zinc-500'
                }`}>
                  3
                </div>
                <span className={`text-[11px] font-bold ${currentStep >= 3 ? 'text-white' : 'text-zinc-500'}`}>Thanh Toán</span>
              </div>
            </div>

            {/* Step 1: Seat selection grid & Legend Map */}
            {currentStep === 1 && (
              <div className="bg-zinc-900/40 border border-zinc-800/80 rounded-3xl p-6 md:p-8 flex flex-col justify-center relative overflow-hidden">
                
                {/* QUICK SHOWTIME CHANGER WIDGET */}
                <div className="bg-zinc-950/80 border border-zinc-800 rounded-2xl p-4 mb-8 flex flex-col sm:flex-row items-center justify-between gap-4">
                  <div className="flex items-center gap-2 text-xs font-bold text-zinc-400">
                    <Calendar className="w-4 h-4 text-brand-coral shrink-0" />
                    <span>Đổi suất chiếu nhanh:</span>
                  </div>
                  <div className="flex flex-wrap gap-2 justify-center">
                    {showtimesList.map((t) => {
                      const isActive = t === time;
                      return (
                        <button
                          key={t}
                          type="button"
                          onClick={() => {
                            if (t === time) return;
                            setSelectedSeats([]);
                            if (onUpdateBookingData) {
                              onUpdateBookingData({ time: t });
                            }
                          }}
                          className={`px-3.5 py-1.5 rounded-xl text-xs font-bold border transition-all duration-300 ${
                            isActive
                              ? 'bg-brand-coral border-brand-coral text-white shadow-md shadow-brand-coral/25'
                              : 'bg-zinc-900 border-zinc-800 text-zinc-400 hover:text-zinc-200 hover:border-zinc-700'
                          }`}
                        >
                          {t}
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* The Silver Screen Curve */}
                <div className="w-full max-w-lg mx-auto mb-16 text-center">
                  <div className="h-1.5 bg-gradient-to-r from-transparent via-brand-coral to-transparent shadow-[0_0_20px_rgba(216,129,116,0.9)] rounded-full mb-2"></div>
                  <span className="text-zinc-500 text-[10px] tracking-[0.4em] font-black uppercase">MÀN HÌNH CHÍNH / MAIN SCREEN</span>
                </div>

                {/* Architectural Seating Grid */}
                <div className="w-full overflow-x-auto py-4 scrollbar-thin scrollbar-thumb-zinc-800">
                  <div className="min-w-[620px] max-w-2xl mx-auto px-4">
                    <div className="space-y-3">
                      {ROWS.map((row) => {
                        const isCoupleRow = row === 'J';
                        const seatCount = isCoupleRow ? 6 : 12;

                        return (
                          <div key={row} className="flex items-center gap-3">
                            {/* Row Label Left */}
                            <span className="w-4 text-center font-black text-xs text-zinc-500">{row}</span>

                            {/* Seats Row Grid */}
                            <div className="flex-grow grid grid-cols-12 gap-2">
                              {Array.from({ length: seatCount }).map((_, idx) => {
                                const colNumber = idx + 1;
                                const label = `${row}${colNumber}`;
                                const isOccupied = occupiedSeats.has(label);
                                const isSelected = selectedSeats.includes(label);
                                
                                let seatClass;
                                let textLabel = label;

                                if (isOccupied) {
                                  seatClass = 'bg-zinc-950 border border-zinc-900 text-zinc-700 line-through opacity-30 cursor-not-allowed pointer-events-none';
                                } else if (isSelected) {
                                  seatClass = 'bg-emerald-500 border-emerald-400 text-black font-extrabold shadow-lg shadow-emerald-500/20';
                                } else {
                                  // Type styling
                                  if (isCoupleRow) {
                                    seatClass = 'bg-rose-600/10 border border-rose-500/40 hover:bg-rose-500/30 text-rose-400';
                                  } else if (['F', 'G', 'H', 'I'].includes(row)) {
                                    seatClass = 'bg-amber-600/10 border border-amber-500/40 hover:bg-amber-500/30 text-amber-400';
                                  } else {
                                    seatClass = 'bg-zinc-800 border border-zinc-700 hover:border-amber-500 text-zinc-400';
                                  }
                                }

                                // Couple seat occupies double width span
                                const colSpan = isCoupleRow ? 'col-span-2' : 'col-span-1';

                                if (isCoupleRow) {
                                  textLabel = `J${colNumber * 2 - 1}-J${colNumber * 2}`;
                                }

                                return (
                                  <button
                                    key={label}
                                    disabled={isOccupied}
                                    onClick={() => handleSeatClick(label)}
                                    className={`${colSpan} aspect-square md:aspect-auto md:h-10 rounded-lg flex items-center justify-center text-[10px] md:text-xs font-semibold tracking-tighter transition-all duration-200 select-none ${seatClass}`}
                                    aria-label={`Select seat ${textLabel}`}
                                  >
                                    {textLabel}
                                  </button>
                                );
                              })}
                            </div>

                            {/* Row Label Right */}
                            <span className="w-4 text-center font-black text-xs text-zinc-500">{row}</span>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </div>

                {/* Legend Map */}
                <div className="flex flex-wrap justify-center gap-6 mt-12 text-xs text-zinc-400 border-t border-zinc-800 pt-6">
                  <div className="flex items-center gap-2">
                    <div className="w-4 h-4 rounded bg-zinc-800 border border-zinc-700"></div>
                    <span>Ghế thường (80k)</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-4 h-4 rounded bg-amber-600/10 border border-amber-500/40"></div>
                    <span>Ghế VIP (110k)</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-6 h-4 rounded bg-rose-600/10 border border-rose-500/40"></div>
                    <span>Ghế Đôi (220k)</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-4 h-4 rounded bg-emerald-500 border border-emerald-400"></div>
                    <span>Đang chọn</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-4 h-4 rounded bg-zinc-950 border border-zinc-900 line-through opacity-30 flex items-center justify-center text-[8px]">X</div>
                    <span>Đã đặt</span>
                  </div>
                </div>
              </div>
            )}

            {/* Step 2: Concession & combo snacks selection */}
            {currentStep === 2 && (
              <div className="bg-zinc-900/40 border border-zinc-800/80 rounded-3xl p-6 md:p-8 space-y-6">
                <div className="flex items-center gap-3">
                  <ShoppingBag className="w-5 h-5 text-brand-coral" />
                  <div>
                    <h3 className="text-base font-black text-white uppercase tracking-wider">MUA KÈM BẮP NƯỚC</h3>
                    <p className="text-zinc-500 text-xs mt-0.5">Ưu đãi giảm giá combo lớn khi mua trực tuyến</p>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  {CONCESSIONS.map((combo) => {
                    const qty = selectedConcessions[combo.id] || 0;
                    return (
                      <div key={combo.id} className="bg-zinc-900 border border-zinc-800 rounded-2xl overflow-hidden flex flex-col justify-between hover:border-zinc-700 transition-colors">
                        <div className="aspect-video bg-zinc-950 relative overflow-hidden">
                          <img 
                            src={combo.image} 
                            alt={combo.name} 
                            className="w-full h-full object-cover hover:scale-105 transition-transform duration-300"
                          />
                        </div>
                        <div className="p-4 flex-grow flex flex-col justify-between space-y-4">
                          <div>
                            <h4 className="font-bold text-white text-xs tracking-wide">{combo.name}</h4>
                            <p className="text-[10px] text-zinc-500 mt-1 line-clamp-2 leading-relaxed">{combo.description}</p>
                          </div>
                          <div className="flex items-center justify-between pt-2">
                            <span className="text-xs font-black text-brand-yellow">{formatCurrency(combo.price)}</span>
                            <div className="flex items-center gap-2 bg-zinc-950 border border-zinc-800 rounded-lg p-1 select-none">
                              <button
                                type="button"
                                onClick={() => handleDecreaseConcession(combo.id)}
                                className="w-5 h-5 flex items-center justify-center text-zinc-400 hover:text-white rounded hover:bg-zinc-800 transition-colors font-extrabold text-xs"
                              >
                                -
                              </button>
                              <span className="text-xs font-black text-white w-3 text-center">{qty}</span>
                              <button
                                type="button"
                                onClick={() => handleIncreaseConcession(combo.id)}
                                className="w-5 h-5 flex items-center justify-center text-zinc-400 hover:text-white rounded hover:bg-zinc-800 transition-colors font-extrabold text-xs"
                              >
                                +
                              </button>
                            </div>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Step 3: Mock Payment Summary Gateway */}
            {currentStep === 3 && (
              <div className="bg-zinc-900/40 border border-zinc-800/80 rounded-3xl p-6 md:p-8 space-y-6">
                <h3 className="text-base font-black text-white uppercase tracking-wider">PHƯƠNG THỨC THANH TOÁN</h3>
                <p className="text-zinc-500 text-xs mt-0.5">Vui lòng chọn một phương thức thanh toán giả định bên dưới</p>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {/* LoraPay Option */}
                  <button
                    type="button"
                    onClick={() => setPaymentMethod('lorapay')}
                    className={`flex items-center gap-4 p-5 rounded-2xl border text-left transition-all duration-300 ${
                      paymentMethod === 'lorapay'
                        ? 'bg-brand-coral/10 border-brand-coral text-white shadow-lg shadow-brand-coral/10'
                        : 'bg-zinc-950 border-zinc-800 text-zinc-400 hover:border-zinc-700'
                    }`}
                  >
                    <Smartphone className={`w-8 h-8 shrink-0 ${paymentMethod === 'lorapay' ? 'text-brand-coral' : 'text-zinc-500'}`} />
                    <div>
                      <h4 className="font-extrabold text-sm text-white">Ví điện tử LoraPay</h4>
                      <p className="text-[10px] text-zinc-500 mt-0.5">Thanh toán nhanh qua ứng dụng di động</p>
                    </div>
                  </button>

                  {/* Domestic Card Option */}
                  <button
                    type="button"
                    onClick={() => setPaymentMethod('domestic')}
                    className={`flex items-center gap-4 p-5 rounded-2xl border text-left transition-all duration-300 ${
                      paymentMethod === 'domestic'
                        ? 'bg-brand-coral/10 border-brand-coral text-white shadow-lg shadow-brand-coral/10'
                        : 'bg-zinc-950 border-zinc-800 text-zinc-400 hover:border-zinc-700'
                    }`}
                  >
                    <CreditCard className={`w-8 h-8 shrink-0 ${paymentMethod === 'domestic' ? 'text-brand-coral' : 'text-zinc-500'}`} />
                    <div>
                      <h4 className="font-extrabold text-sm text-white">Thẻ nội địa</h4>
                      <p className="text-[10px] text-zinc-500 mt-0.5">ATM / Internet Banking</p>
                    </div>
                  </button>
                </div>

                {/* Final Invoice Summary details */}
                <div className="border-t border-zinc-800 pt-6 space-y-4">
                  <h4 className="font-bold text-xs text-white uppercase tracking-wider">HÓA ĐƠN CHI TIẾT</h4>
                  <div className="bg-zinc-950/60 border border-zinc-800/80 rounded-2xl p-4 space-y-2 text-xs">
                    <div className="flex justify-between text-zinc-400">
                      <span>Giá vé ({selectedSeats.length} ghế)</span>
                      <span className="text-white font-bold">{formatCurrency(totalSeatsPrice)}</span>
                    </div>
                    {totalConcessionsPrice > 0 && (
                      <div className="flex justify-between text-zinc-400">
                        <span>Bắp nước combo</span>
                        <span className="text-white font-bold">{formatCurrency(totalConcessionsPrice)}</span>
                      </div>
                    )}
                    <div className="flex justify-between text-zinc-400 pt-2 border-t border-zinc-800">
                      <span className="font-extrabold text-white">Tổng cộng</span>
                      <span className="text-brand-coral font-black text-sm">{formatCurrency(totalAmount)}</span>
                    </div>
                  </div>
                </div>

                <div className="border-t border-zinc-800 pt-6 space-y-2">
                  <h4 className="font-bold text-xs text-white uppercase tracking-wider">ĐIỀU KHOẢN GIAO DỊCH</h4>
                  <p className="text-[10px] text-zinc-500 leading-relaxed">
                    Bằng việc nhấn nút "Xác Nhận Đặt Vé", bạn đồng ý với các điều khoản thanh toán giả định của hệ thống LoraFilm. Vé đã mua không thể thay đổi hoặc hoàn tiền.
                  </p>
                </div>
              </div>
            )}

          </div>

          {/* RIGHT SIDEBAR PANEL (Sticky Booking Summary Ledger) */}
          <div className="lg:col-span-1 lg:sticky lg:top-24 bg-zinc-900 border border-zinc-800 rounded-3xl p-6 space-y-6 shadow-2xl">
            
            {/* Movie Metadata Block */}
            <div className="flex gap-4 items-start pb-6 border-b border-zinc-800">
              <div className="w-16 aspect-[2/3] rounded-xl overflow-hidden bg-zinc-950 border border-zinc-800 shrink-0">
                <img 
                  src={movie.posterUrl || movie.image} 
                  alt={movie.title} 
                  className="w-full h-full object-cover" 
                  onError={(e) => {
                    e.target.onerror = null;
                    e.target.src = 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=600&auto=format&fit=crop&q=80';
                  }}
                />
              </div>
              <div className="space-y-1.5 flex-grow">
                <span className="inline-block text-[9px] bg-brand-coral/15 text-brand-coral border border-brand-coral/20 px-2 py-0.5 rounded font-black uppercase tracking-wider">
                  {format}
                </span>
                <h3 className="text-sm font-black text-white line-clamp-2 mt-1 leading-snug">{movie.title}</h3>
                <div className="flex items-center gap-1.5 text-[10px] text-zinc-500 font-semibold">
                  <span>{movie.duration}</span>
                  <span>•</span>
                  <span className="text-brand-yellow">{movie.ageRating}</span>
                </div>
              </div>
            </div>

            {/* Showtime Ticket Details */}
            <div className="space-y-3 py-2 text-xs border-b border-zinc-800">
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Cụm rạp</span>
                <span className="text-white font-bold text-right">{cinema}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Phòng chiếu</span>
                <span className="text-zinc-200 font-bold text-right">{hallLabel}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500 font-medium">Suất chiếu</span>
                <span className="text-brand-coral font-black text-right">{time} | {date}</span>
              </div>
            </div>

            {/* Live Receipt Itemization Table */}
            <div className="py-2 border-b border-zinc-800 space-y-4">
              <span className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Giao dịch chi tiết</span>
              
              {/* Seats list */}
              {selectedSeats.length > 0 ? (
                <div className="space-y-2">
                  {selectedSeats.map(seat => {
                    const row = seat.charAt(0);
                    const type = getSeatType(row);
                    const price = SEAT_PRICES[type];
                    
                    const col = parseInt(seat.substring(1));
                    const displayLabel = row === 'J'
                      ? `J${col * 2 - 1}-J${col * 2} (Đôi)`
                      : `${seat} (${type === 'vip' ? 'VIP' : 'Thường'})`;

                    return (
                      <div key={seat} className="flex justify-between text-xs">
                        <span className="text-zinc-300">Ghế {displayLabel}</span>
                        <span className="text-white font-bold">{formatCurrency(price)}</span>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <div className="text-xs text-zinc-600 italic py-1">Chưa chọn ghế ngồi</div>
              )}

              {/* Concessions list */}
              {Object.keys(selectedConcessions).length > 0 && (
                <div className="space-y-2 pt-3 border-t border-zinc-800">
                  {Object.entries(selectedConcessions).map(([id, qty]) => {
                    const combo = CONCESSIONS.find(c => c.id === id);
                    if (!combo) return null;
                    return (
                      <div key={id} className="flex justify-between text-xs">
                        <span className="text-zinc-300">{combo.name} x{qty}</span>
                        <span className="text-white font-bold">{formatCurrency(combo.price * qty)}</span>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Dynamic Calculation Summary Block */}
            <div className="flex justify-between items-center py-4 px-4 bg-zinc-950/60 rounded-2xl border border-zinc-850 shadow-inner">
              <div>
                <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Tổng thanh toán</span>
                <span className="text-[9px] text-zinc-600 block">(Bao gồm VAT)</span>
              </div>
              <span className="text-lg md:text-xl font-black text-brand-coral">{formatCurrency(totalAmount)}</span>
            </div>

            {/* Step-conditional Funnel Action Button */}
            <button
              disabled={selectedSeats.length === 0}
              onClick={() => {
                if (currentStep === 1) {
                  handleProceedToConcessions();
                } else if (currentStep === 2) {
                  handleProceedToPayment();
                } else if (currentStep === 3) {
                  handleCheckoutSubmit();
                }
              }}
              className={`w-full py-4 rounded-2xl font-black uppercase text-xs tracking-wider shadow-lg transition-all duration-300 transform ${
                selectedSeats.length > 0
                  ? 'bg-brand-coral hover:bg-opacity-95 hover:scale-[1.02] text-white shadow-brand-coral/25'
                  : 'bg-zinc-800 text-zinc-500 cursor-not-allowed border border-zinc-700/50'
              }`}
            >
              {currentStep === 1 && 'Tiếp Tục'}
              {currentStep === 2 && 'Tiếp Tục Thanh Toán'}
              {currentStep === 3 && 'Xác Nhận Đặt Vé'}
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
            
            <h3 className="text-xl font-black text-white uppercase tracking-wide mb-2">ĐẶT VÉ THÀNH CÔNG</h3>
            <p className="text-zinc-400 text-xs mb-6 leading-relaxed">
              Cảm ơn bạn đã lựa chọn LoraFilm. Dưới đây là thông tin chi tiết vé của bạn:
            </p>

            <div className="bg-zinc-950/60 border border-zinc-800/80 rounded-2xl p-4 text-left space-y-2.5 text-xs mb-6">
              <div className="flex justify-between">
                <span className="text-zinc-500">Phim:</span>
                <span className="text-white font-extrabold text-right ml-4">{movieTitle}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500">Định dạng:</span>
                <span className="text-brand-yellow font-extrabold">{format}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500">Rạp:</span>
                <span className="text-zinc-200 font-semibold">{cinema}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500">Suất chiếu:</span>
                <span className="text-white font-extrabold">{time} | {date}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500">Ghế chọn:</span>
                <span className="text-emerald-400 font-extrabold text-right ml-4">
                  {selectedSeats.map(seat => {
                    const row = seat.charAt(0);
                    if (row === 'J') {
                      const col = parseInt(seat.substring(1));
                      return `J${col * 2 - 1}-J${col * 2}`;
                    }
                    return seat;
                  }).join(', ')}
                </span>
              </div>

              {/* Popcorn snack sub-table */}
              {Object.keys(selectedConcessions).length > 0 && (
                <div className="border-t border-zinc-850 pt-2.5 mt-2.5 space-y-1.5">
                  <span className="text-zinc-500 font-bold block">Bắp nước kèm theo:</span>
                  {Object.entries(selectedConcessions).map(([id, qty]) => {
                    const combo = CONCESSIONS.find(c => c.id === id);
                    return (
                      <div key={id} className="flex justify-between">
                        <span className="text-zinc-400">{combo?.name}</span>
                        <span className="text-white font-bold">x{qty}</span>
                      </div>
                    );
                  })}
                </div>
              )}

              {/* Payment Method */}
              <div className="flex justify-between border-t border-zinc-850 pt-2.5 mt-2.5">
                <span className="text-zinc-500">Thanh toán:</span>
                <span className="text-zinc-200 font-semibold">
                  {paymentMethod === 'lorapay' ? 'Ví LoraPay' : 'Thẻ ATM nội địa'}
                </span>
              </div>

              <div className="border-t border-zinc-800/80 pt-2.5 mt-2.5 flex justify-between items-center">
                <span className="text-zinc-500 font-bold">Tổng thanh toán:</span>
                <span className="text-brand-coral font-black text-sm">{formatCurrency(totalAmount)}</span>
              </div>
            </div>

            <button
              onClick={() => {
                setShowSuccessModal(false);
                onBack(); // Go back home
              }}
              className="w-full bg-brand-coral hover:bg-opacity-95 text-white font-black py-4 rounded-xl shadow-lg transition-colors duration-300 uppercase tracking-wider text-xs"
            >
              Quay lại Trang Chủ
            </button>
          </div>
        </div>
      )}

      {/* Timeout Expiry Modal */}
      {showTimeoutModal && (
        <div className="fixed inset-0 bg-black/90 backdrop-blur-md flex items-center justify-center z-50 p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-8 max-w-md w-full shadow-2xl text-center relative overflow-hidden animate-in zoom-in duration-300">
            <AlertTriangle className="w-16 h-16 text-red-500 mx-auto mb-4" />
            
            <h3 className="text-xl font-black text-white uppercase tracking-wide mb-2">HẾT THỜI GIAN GIỮ GHẾ</h3>
            <p className="text-zinc-400 text-xs mb-6 leading-relaxed">
              Hết thời gian giữ ghế! Suất chiếu của bạn đã bị hủy tự động để giải phóng chỗ.
            </p>

            <button
              onClick={handleTimeoutOk}
              className="w-full bg-brand-coral hover:bg-opacity-95 text-white font-black py-4 rounded-xl shadow-lg transition-colors duration-300 uppercase tracking-wider text-xs"
            >
              Đồng ý (OK)
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
                Việc chọn vị trí ghế của bạn không được để trống 1 ghế ở bên trái, giữa hoặc bên phải trên cùng hàng ghế mà bạn vừa chọn. Vui lòng chọn lại vị trí khác.
              </p>
              <button
                onClick={() => setShowGapModal(false)}
                className="w-full bg-gradient-to-r from-orange-400 to-amber-500 text-zinc-950 font-black py-2.5 rounded-xl text-xs uppercase tracking-widest mt-4 cursor-pointer text-center block"
              >
                ĐÃ HIỂU &amp; CHỌN LẠI
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
