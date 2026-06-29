import { Construction } from 'lucide-react';

export default function SystemUpdating() {
  return (
    <div className="w-full min-h-[400px] flex items-center justify-center animate-fade-in">
      <div className="flex flex-col items-center justify-center p-12 text-center bg-zinc-900/40 border border-zinc-800 rounded-2xl max-w-xl mx-auto my-12 space-y-4 shadow-2xl backdrop-blur-md">
        <div className="w-16 h-16 rounded-full bg-brand-orange/10 flex items-center justify-center text-brand-orange border border-brand-orange/20">
          <Construction className="w-8 h-8 animate-pulse" />
        </div>
        <h2 className="text-xl font-black text-zinc-100 tracking-wider uppercase">
          Hệ thống đang được cập nhật
        </h2>
        <p className="text-sm text-zinc-400 max-w-md">
          Tính năng này đang chờ tích hợp API. Vui lòng quay lại sau khi module được hoàn thiện.
        </p>
      </div>
    </div>
  );
}
