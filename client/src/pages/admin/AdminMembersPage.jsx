// TODO: Connect to Gateway API: GET /api/v1/customers
import { AlertCircle } from 'lucide-react';

export default function AdminMembersView() {
  return (
    <div className="w-full min-h-[400px] flex items-center justify-center">
      <div className="flex flex-col items-center justify-center p-12 text-center bg-zinc-900/40 border border-zinc-800 rounded-2xl max-w-xl mx-auto my-12 space-y-4 shadow-2xl">
        <div className="w-12 h-12 rounded-full bg-orange-500/10 flex items-center justify-center text-orange-500">
          <AlertCircle className="w-6 h-6 animate-pulse" />
        </div>
        <h2 className="text-lg font-bold text-zinc-100">Hệ thống đang được cập nhật</h2>
        <p className="text-xs text-zinc-400">
          No real data available yet. This module is waiting for backend API integration.
        </p>
      </div>
    </div>
  );
}
