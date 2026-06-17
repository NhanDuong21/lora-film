// TODO: Connect to Gateway API: GET /api/v1/employee/schedules
import { AlertCircle } from 'lucide-react';

export default function EmployeeScheduleView() {
  return (
    <div className="flex-grow flex flex-col space-y-6 h-full">
      {/* Header section */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 border-b border-zinc-800 pb-5">
        <div>
          <h2 className="text-xl font-black text-white uppercase tracking-wider">LỊCH CHIẾU VÀ PHÂN BỔ PHÒNG</h2>
          <p className="text-xs text-zinc-500 uppercase tracking-widest mt-1">Giám sát các phòng chiếu phim thời gian thực của rạp</p>
        </div>
      </div>

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
