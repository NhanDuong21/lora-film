// TODO: Connect to Gateway API: GET /api/v1/employee/pos
import { AlertCircle } from 'lucide-react';

export default function EmployeePOSView() {
  return (
    <div className="flex-grow flex flex-col space-y-6 relative h-full">
      {/* Title & Search bar */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 border-b border-zinc-800 pb-5">
        <div>
          <h2 className="text-xl font-black text-white uppercase tracking-wider">ĐẶT VÉ TẠI QUẦY (POS)</h2>
          <p className="text-xs text-zinc-500 uppercase tracking-widest mt-1">Lập vé, chọn ghế &amp; xuất bán Combo dịch vụ ăn uống nhanh chóng</p>
        </div>
      </div>

      <div className="flex-1 flex items-center justify-center bg-zinc-900 border border-zinc-800 rounded-2xl min-h-[500px]">
        <div className="flex flex-col items-center justify-center p-12 text-center max-w-xl mx-auto my-12 space-y-4 shadow-2xl">
          <div className="w-12 h-12 rounded-full bg-orange-500/10 flex items-center justify-center text-orange-500">
            <AlertCircle className="w-6 h-6 animate-pulse" />
          </div>
          <h2 className="text-lg font-bold text-zinc-100">Hệ thống đang được cập nhật</h2>
          <p className="text-xs text-zinc-400">
            No real data available yet. This module is waiting for backend API integration.
          </p>
        </div>
      </div>
    </div>
  );
}
