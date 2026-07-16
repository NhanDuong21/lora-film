import React from 'react';
import { Info, Phone } from 'lucide-react';

export default function CinemaBasicInfo({ formData, setFormData, formErrors }) {
  return (
    <div className="flex flex-col gap-6">
      {/* Card: Basic Information */}
      <div className="bg-zinc-900/60 border border-zinc-800/80 rounded-2xl p-6 flex flex-col gap-4">
        <div className="flex items-center gap-2 border-b border-zinc-800/80 pb-3">
          <Info className="w-4 h-4 text-orange-500" />
          <h2 className="text-sm font-bold uppercase tracking-wider text-white">Thông Tin Cơ Bản</h2>
        </div>

        <div className="flex flex-col gap-1.5">
          <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Tên cụm rạp <span className="text-rose-500">*</span></label>
          <input
            type="text"
            value={formData.name}
            onChange={e => setFormData({ ...formData, name: e.target.value })}
            placeholder="Ví dụ: LoraFilm Nguyễn Du"
            className={`w-full bg-zinc-950 border ${formErrors.name ? 'border-red-500/80 focus:border-red-500' : 'border-zinc-800 focus:border-orange-500/40'} rounded-xl py-2.5 px-3.5 text-xs text-zinc-100 focus:outline-none transition-colors`}
          />
          {formErrors.name && <span className="text-[10px] text-red-500 font-semibold">{formErrors.name}</span>}
        </div>

        <div className="flex flex-col gap-1.5">
          <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Mô tả cụm rạp</label>
          <textarea
            rows={4}
            value={formData.description}
            onChange={e => setFormData({ ...formData, description: e.target.value })}
            placeholder="Mô tả giới thiệu về rạp chiếu phim..."
            className="w-full bg-zinc-950 border border-zinc-800 focus:border-orange-500/40 rounded-xl py-2.5 px-3.5 text-xs text-zinc-100 focus:outline-none transition-colors resize-none"
          />
        </div>
      </div>

      {/* Card: Contact info */}
      <div className="bg-zinc-900/60 border border-zinc-800/80 rounded-2xl p-6 flex flex-col gap-4">
        <div className="flex items-center gap-2 border-b border-zinc-800/80 pb-3">
          <Phone className="w-4 h-4 text-orange-500" />
          <h2 className="text-sm font-bold uppercase tracking-wider text-white">Liên Hệ</h2>
        </div>

        <div className="flex flex-col gap-1.5">
          <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Số Hotline</label>
          <input
            type="text"
            value={formData.hotline}
            onChange={e => setFormData({ ...formData, hotline: e.target.value })}
            placeholder="Ví dụ: 19001234"
            className="w-full bg-zinc-950 border border-zinc-800 focus:border-orange-500/40 rounded-xl py-2.5 px-3.5 text-xs text-zinc-100 focus:outline-none transition-colors"
          />
        </div>
      </div>
    </div>
  );
}
