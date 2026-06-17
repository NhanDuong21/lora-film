// TODO: Connect to Gateway API: GET /api/v1/employee/tickets
import { AlertCircle } from 'lucide-react';

export default function EmployeeCheckInView() {
  return (
    <div className="flex-grow flex flex-col space-y-6 max-w-4xl mx-auto w-full">
      {/* View Header */}
      <div className="border-b border-zinc-800 pb-5">
        <h2 className="text-xl font-black text-white uppercase tracking-wider">GATE CHECK-IN AUDIT NODE</h2>
        <p className="text-xs text-zinc-500 uppercase tracking-widest mt-1">Kiểm soát vé vào cửa phòng chiếu &amp; xác thực tính hợp lệ của khách hàng</p>
      </div>

      {/* Styled center scanner container box with beautiful Empty State */}
      <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl shadow-xl p-8 flex flex-col items-center justify-center space-y-6">
        <div className="flex flex-col items-center justify-center p-12 text-center max-w-xl mx-auto space-y-4 shadow-2xl">
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
