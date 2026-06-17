import { useState } from 'react';
import { useData } from '../../contexts/DataContext';
import { 
  CheckCircle, 
  AlertTriangle, 
  Search, 
  Calendar, 
  MapPin, 
  CreditCard 
} from 'lucide-react';

export default function EmployeeCheckInView() {
  const { tickets, setTickets } = useData();
  const [ticketCode, setTicketCode] = useState('');
  const [auditResult, setAuditResult] = useState(null);

  const handleAuditCheck = (e) => {
    if (e) e.preventDefault();
    if (!ticketCode.trim()) return;

    const code = ticketCode.trim().toUpperCase();

    // Read fresh tickets list from localStorage to avoid stale state issues
    const freshTicketsStr = localStorage.getItem('lora_tickets');
    const freshTickets = freshTicketsStr ? JSON.parse(freshTicketsStr) : tickets;

    const matchedIndex = freshTickets.findIndex(t => t.id.toUpperCase() === code);

    if (matchedIndex !== -1) {
      const ticket = freshTickets[matchedIndex];

      if (ticket.status === 'DA_KIEM_TRA') {
        // Ticket is duplicate (already checked-in)
        setAuditResult({
          success: false,
          code,
          ticket,
          type: 'DUPLICATE',
          message: 'Vé này đã được check-in trước đó! Vui lòng kiểm tra lại để tránh gian lận.'
        });
      } else {
        // Ticket is valid, check it in now
        const updatedTickets = [...freshTickets];
        updatedTickets[matchedIndex] = { ...ticket, status: 'DA_KIEM_TRA' };
        
        setTickets(updatedTickets);
        localStorage.setItem('lora_tickets', JSON.stringify(updatedTickets));

        setAuditResult({
          success: true,
          code,
          ticket: updatedTickets[matchedIndex],
          type: 'VALID',
          message: 'Vé hợp lệ! Đã hoàn tất thủ tục check-in cổng tự động.'
        });
      }
    } else {
      // Non-existent ticket
      setAuditResult({
        success: false,
        code,
        ticket: null,
        type: 'NON_EXISTENT',
        message: 'Mã vé không tồn tại trên hệ thống hoặc chưa được hoàn tất thanh toán.'
      });
    }
  };

  return (
    <div className="flex-grow flex flex-col space-y-6 max-w-4xl mx-auto w-full">
      {/* View Header */}
      <div className="border-b border-zinc-800 pb-5">
        <h2 className="text-xl font-black text-white uppercase tracking-wider">GATE CHECK-IN AUDIT NODE</h2>
        <p className="text-xs text-zinc-500 uppercase tracking-widest mt-1">Kiểm soát vé vào cửa phòng chiếu &amp; xác thực tính hợp lệ của khách hàng</p>
      </div>

      {/* Styled center scanner container box */}
      <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl shadow-xl p-8 flex flex-col items-center justify-center space-y-6">
        <div className="w-16 h-16 rounded-full bg-amber-500/10 border border-amber-500/20 text-amber-500 flex items-center justify-center">
          <Search className="w-8 h-8" />
        </div>

        <div className="text-center max-w-md">
          <h3 className="text-sm font-bold text-white uppercase tracking-wider">QUÉT MÃ SỐ VÉ KHÁCH HÀNG</h3>
          <p className="text-xs text-zinc-500 mt-1">Nhập mã vé dạng chuỗi định danh (TKT-XXXX-XXXX) được cấp trên điện thoại hoặc vé in giấy.</p>
        </div>

        <form onSubmit={handleAuditCheck} className="flex gap-3 w-full max-w-lg">
          <input
            type="text"
            placeholder="Nhập mã vé (Ví dụ: TKT-8492-9582)"
            value={ticketCode}
            onChange={(e) => setTicketCode(e.target.value)}
            className="bg-zinc-950 border border-zinc-800 rounded-xl py-3.5 px-4 text-xs text-white placeholder-zinc-650 focus:outline-none focus:border-amber-500 flex-grow font-mono font-bold"
          />
          <button
            type="submit"
            className="bg-amber-500 hover:bg-amber-600 text-black font-semibold px-6 py-2 rounded-xl text-sm transition-all select-none"
          >
            KIỂM TRA VÉ
          </button>
        </form>
      </div>

      {/* The Reactive Audit Response Box */}
      {auditResult && (
        <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl shadow-xl p-6 space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-zinc-800 pb-4">
            <div>
              <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider block">Mã vé đối soát:</span>
              <span className="text-sm font-mono font-bold text-white">{auditResult.code}</span>
            </div>
            
            {/* Dynamic Gate Pass Status Capsules */}
            <div>
              {auditResult.success ? (
                <span className="bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-xs px-3 py-1 rounded-full uppercase font-bold tracking-wider inline-block">
                  VÉ HỢP LỆ - ĐÃ CHECK-IN
                </span>
              ) : (
                <span className="bg-red-500/10 text-red-400 border border-red-500/20 text-xs px-3 py-1 rounded-full uppercase font-bold tracking-wider inline-block">
                  CẢNH BÁO - VÉ KHÔNG HỢP LỆ
                </span>
              )}
            </div>
          </div>

          {/* Audit error/success description message */}
          <div className={`p-4 rounded-xl border flex gap-3 text-xs ${
            auditResult.success 
              ? 'bg-emerald-950/20 border-emerald-900/40 text-emerald-300'
              : 'bg-red-950/20 border-red-900/40 text-red-300'
          }`}>
            {auditResult.success ? (
              <CheckCircle className="w-5 h-5 text-emerald-500 shrink-0" />
            ) : (
              <AlertTriangle className="w-5 h-5 text-red-500 shrink-0" />
            )}
            <div>
              <p className="font-bold">{auditResult.success ? 'Hợp lệ' : 'Cảnh báo lỗi hệ thống'}</p>
              <p className="mt-0.5 opacity-90">{auditResult.message}</p>
            </div>
          </div>

          {/* Customer & Showtime Parameters details sheet */}
          {auditResult.ticket && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-2">
              <div className="space-y-4">
                <h4 className="text-xs font-black uppercase text-zinc-400 tracking-wider">Thông Tin Suất Chiếu</h4>
                
                <div className="bg-zinc-950/40 border border-zinc-800 rounded-xl p-4 space-y-3">
                  <div className="flex items-start gap-3">
                    <Calendar className="w-4 h-4 text-amber-500 shrink-0 mt-0.5" />
                    <div>
                      <p className="text-[10px] text-zinc-500 font-bold uppercase">Thời gian:</p>
                      <p className="text-xs font-bold text-zinc-200">{auditResult.ticket.time} | {auditResult.ticket.date}</p>
                    </div>
                  </div>

                  <div className="flex items-start gap-3">
                    <MapPin className="w-4 h-4 text-amber-500 shrink-0 mt-0.5" />
                    <div>
                      <p className="text-[10px] text-zinc-500 font-bold uppercase">Cụm Rạp &amp; Phòng Chiếu:</p>
                      <p className="text-xs font-bold text-zinc-200">
                        {auditResult.ticket.theaterName} - {auditResult.ticket.hallName || 'Phong Chiếu 1'}
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              <div className="space-y-4">
                <h4 className="text-xs font-black uppercase text-zinc-400 tracking-wider">Chi Tiết Ghế Ngồi &amp; Thanh Toán</h4>
                
                <div className="bg-zinc-950/40 border border-zinc-800 rounded-xl p-4 space-y-3">
                  <div className="flex items-start gap-3">
                    <CheckCircle className="w-4 h-4 text-amber-500 shrink-0 mt-0.5" />
                    <div>
                      <p className="text-[10px] text-zinc-500 font-bold uppercase">Vị trí ghế ngồi:</p>
                      <p className="text-xs font-mono font-bold text-emerald-400">
                        {auditResult.ticket.seats?.join(', ')}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-start gap-3">
                    <CreditCard className="w-4 h-4 text-amber-500 shrink-0 mt-0.5" />
                    <div>
                      <p className="text-[10px] text-zinc-500 font-bold uppercase">Tổng thanh toán:</p>
                      <p className="text-xs font-mono font-bold text-white">
                        {auditResult.ticket.amount?.toLocaleString('vi-VN')} đ
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              <div className="col-span-full bg-zinc-950 border border-zinc-800/80 rounded-xl p-4">
                <span className="text-[10px] text-zinc-500 font-bold uppercase block">Tên phim chỉ định:</span>
                <span className="text-sm font-bold text-white block mt-0.5">{auditResult.ticket.movieTitle}</span>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
