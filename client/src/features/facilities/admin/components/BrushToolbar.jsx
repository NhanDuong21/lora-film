// eslint-disable-next-line no-unused-vars
import React from 'react';
import { LogOut } from 'lucide-react';

export default function BrushToolbar({ activeBrush, setActiveBrush }) {
  return (
    <div className="bg-zinc-900/60 border border-zinc-900 rounded-2xl p-3 flex flex-wrap items-center gap-2 mb-8 select-none shadow-2xl backdrop-blur-md">
      <div className="text-[10px] font-black text-zinc-500 uppercase tracking-widest px-2.5">
        Cọ vẽ:
      </div>
      
      {/* Standard Brush */}
      <button
        onClick={() => setActiveBrush('STANDARD')}
        className={`flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-bold uppercase transition-all ${
          activeBrush === 'STANDARD'
            ? 'bg-purple-600 text-white border border-purple-500 shadow-lg shadow-purple-600/20'
            : 'bg-zinc-950 border border-zinc-800 text-zinc-400 hover:bg-zinc-900'
        }`}
      >
        <div className="w-3.5 h-3.5 rounded-md bg-purple-600 border border-purple-500"></div>
        <span>Thường</span>
      </button>

      {/* VIP Brush */}
      <button
        onClick={() => setActiveBrush('VIP')}
        className={`flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-bold uppercase transition-all ${
          activeBrush === 'VIP'
            ? 'bg-red-500 text-white border border-red-400 shadow-lg shadow-red-500/20'
            : 'bg-zinc-950 border border-zinc-800 text-zinc-400 hover:bg-zinc-900'
        }`}
      >
        <div className="w-3.5 h-3.5 rounded-md bg-red-500 border border-red-400"></div>
        <span>VIP</span>
      </button>

      {/* Couple Brush */}
      <button
        onClick={() => setActiveBrush('COUPLE')}
        className={`flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-bold uppercase transition-all ${
          activeBrush === 'COUPLE'
            ? 'bg-amber-400 text-black border border-amber-300 shadow-lg shadow-amber-400/20'
            : 'bg-zinc-950 border border-zinc-800 text-zinc-400 hover:bg-zinc-900'
        }`}
      >
        <div className="w-3.5 h-3.5 rounded-md bg-amber-400 border border-amber-300"></div>
        <span>Ghế Đôi</span>
      </button>

      {/* Disabled Brush */}
      <button
        onClick={() => setActiveBrush('DISABLED')}
        className={`flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-bold uppercase transition-all ${
          activeBrush === 'DISABLED'
            ? 'bg-sky-500 text-white border border-sky-400 shadow-lg shadow-sky-500/20'
            : 'bg-zinc-950 border border-zinc-800 text-zinc-400 hover:bg-zinc-900'
        }`}
      >
        <div className="w-3.5 h-3.5 rounded-md bg-sky-500 border border-sky-400"></div>
        <span>Khuyết tật</span>
      </button>

      <div className="h-6 w-[1px] bg-zinc-800 mx-1"></div>

      {/* Aisle Brush */}
      <button
        onClick={() => setActiveBrush('AISLE')}
        className={`flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-bold uppercase transition-all ${
          activeBrush === 'AISLE'
            ? 'bg-zinc-900 border border-zinc-700 text-zinc-300'
            : 'bg-zinc-950 border border-zinc-800 text-zinc-400 hover:bg-zinc-900'
        }`}
      >
        <div className="w-3.5 h-3.5 rounded-md bg-zinc-950 border border-dashed border-zinc-700"></div>
        <span>Lối đi</span>
      </button>

      {/* Exit Door Brush */}
      <button
        onClick={() => setActiveBrush('EXIT')}
        className={`flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-bold uppercase transition-all ${
          activeBrush === 'EXIT'
            ? 'bg-emerald-600 text-white border border-emerald-500 shadow-lg shadow-emerald-600/20'
            : 'bg-zinc-950 border border-zinc-800 text-zinc-400 hover:bg-zinc-900'
        }`}
      >
        <LogOut className="w-3.5 h-3.5 text-emerald-400" />
        <span>Cửa Thoát</span>
      </button>
    </div>
  );
}
