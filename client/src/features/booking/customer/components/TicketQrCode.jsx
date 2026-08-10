import { useState } from 'react';

export default function TicketQrCode({ ticketCode, size = 120, className = '' }) {
  const [unavailable, setUnavailable] = useState(false);

  if (unavailable) {
    return (
      <div
        className={`flex items-center justify-center rounded-lg border border-dashed border-zinc-700 bg-zinc-900 p-2 text-center text-[9px] font-bold text-zinc-400 ${className}`}
        aria-label={`Mã vé dự phòng ${ticketCode}`}
      >
        <span>QR không khả dụng<br /><strong className="break-all text-zinc-200">{ticketCode}</strong></span>
      </div>
    );
  }

  return (
    <img
      src={`https://api.qrserver.com/v1/create-qr-code/?size=${size}x${size}&data=${encodeURIComponent(ticketCode)}`}
      alt={`Mã QR vé ${ticketCode}`}
      className={className}
      onError={() => setUnavailable(true)}
    />
  );
}
