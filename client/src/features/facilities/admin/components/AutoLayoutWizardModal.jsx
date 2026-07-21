import React, { useState, useMemo } from 'react';
import { 
  X, 
  ChevronRight, 
  ChevronLeft, 
  Check, 
  Sparkles, 
  Grid3X3,
  SplitSquareHorizontal,
  Armchair,
  Eye,
  DoorOpen
} from 'lucide-react';
import SeatGridDesigner from './SeatGridDesigner';

export default function AutoLayoutWizardModal({ isOpen, onClose, onApply, currentSkipIO }) {
  const [step, setStep] = useState(1);
  
  // Step 1: Scale
  const [preset, setPreset] = useState('STANDARD'); // SMALL, STANDARD, LARGE, CUSTOM
  const [rows, setRows] = useState(10);
  const [cols, setCols] = useState(12);

  // Step 2: Aisles & Exits
  const [verticalAisle, setVerticalAisle] = useState('CENTER'); // NONE, CENTER, TWO
  const [horizontalAisle, setHorizontalAisle] = useState(true);
  const [exitLeft, setExitLeft] = useState(true);
  const [exitRight, setExitRight] = useState(true);

  // Step 3: Strategy
  const [strategy, setStrategy] = useState('AUTO'); // AUTO, ALL_STANDARD

  // Handle Preset changes
  const handlePresetChange = (p) => {
    setPreset(p);
    if (p === 'SMALL') { setRows(6); setCols(8); }
    else if (p === 'STANDARD') { setRows(10); setCols(12); }
    else if (p === 'LARGE') { setRows(14); setCols(18); }
  };

  const handleNext = () => setStep(s => Math.min(s + 1, 4));
  const handleBack = () => setStep(s => Math.max(s - 1, 1));

  // Generate the preview matrix based on current selections
  const previewMatrix = useMemo(() => {
    if (step !== 4) return [];
    
    let matrix = [];
    for (let r = 0; r < rows; r++) {
      let rowArr = [];
      for (let c = 0; c < cols; c++) {
        rowArr.push({ type: 'STANDARD' });
      }
      matrix.push(rowArr);
    }

    // Apply Vertical Aisle
    if (verticalAisle === 'CENTER' && cols > 0) {
      const mid = Math.floor(cols / 2);
      for (let r = 0; r < rows; r++) matrix[r][mid].type = 'AISLE';
    } else if (verticalAisle === 'TWO' && cols > 3) {
      const q1 = Math.floor(cols / 3);
      const q2 = Math.floor((cols * 2) / 3);
      for (let r = 0; r < rows; r++) {
        matrix[r][q1].type = 'AISLE';
        matrix[r][q2].type = 'AISLE';
      }
    }

    // Apply Horizontal Aisle (typically after row E, index 4)
    if (horizontalAisle && rows > 5) {
      for (let c = 0; c < cols; c++) {
        matrix[4][c].type = 'AISLE';
      }
    }

    // Exits
    if (exitLeft && rows > 0) matrix[0][0].type = 'EXIT';
    if (exitRight && rows > 0 && cols > 1) matrix[0][cols - 1].type = 'EXIT';

    // Auto Seating Zones (only applied to non-aisle, non-exit cells)
    if (strategy === 'AUTO') {
      const standardThreshold = Math.floor(rows * 0.3);
      const coupleThreshold = Math.floor(rows * 0.85);

      for (let r = 0; r < rows; r++) {
        for (let c = 0; c < cols; c++) {
          if (matrix[r][c].type !== 'STANDARD') continue; // Skip aisles/exits

          if (r >= coupleThreshold) {
            matrix[r][c].type = 'COUPLE';
          } else if (r >= standardThreshold) {
            matrix[r][c].type = 'VIP';
          }
        }
      }

      // Wheelchairs (2 seats in front row or right after horizontal aisle)
      let wPlaced = 0;
      const targetRow = horizontalAisle && rows > 5 ? 5 : 0;
      for (let c = 0; c < cols && wPlaced < 2; c++) {
        if (matrix[targetRow][c].type === 'STANDARD') {
          matrix[targetRow][c].type = 'DISABLED';
          wPlaced++;
        }
      }
    }

    return matrix;
  }, [step, rows, cols, verticalAisle, horizontalAisle, exitLeft, exitRight, strategy]);

  // Compute stats for Preview
  const stats = useMemo(() => {
    let standard = 0, vip = 0, couple = 0, disabled = 0;
    previewMatrix.forEach(r => r.forEach(c => {
      if (c.type === 'STANDARD') standard++;
      if (c.type === 'VIP') vip++;
      if (c.type === 'COUPLE') couple++;
      if (c.type === 'DISABLED') disabled++;
    }));
    return { standard, vip, couple, disabled, total: standard + vip + couple + disabled };
  }, [previewMatrix]);

  const handleApply = () => {
    onApply(previewMatrix, rows, cols);
    onClose();
  };

  const steps = [
    { id: 1, title: 'Quy mô phòng', icon: Grid3X3 },
    { id: 2, title: 'Lối đi', icon: SplitSquareHorizontal },
    { id: 3, title: 'Phân vùng ghế', icon: Armchair },
    { id: 4, title: 'Xem trước', icon: Eye }
  ];

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 select-none bg-black/80 backdrop-blur-sm animate-fade-in">
      <div className="bg-zinc-950 border border-zinc-800 rounded-3xl shadow-2xl w-full max-w-5xl flex flex-col overflow-hidden max-h-[90vh]">
        
        {/* Header */}
        <div className="px-6 py-5 border-b border-zinc-800 flex justify-between items-center bg-zinc-900/50">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-brand-orange/20 flex items-center justify-center text-brand-orange">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-black text-white uppercase tracking-wider">Sinh sơ đồ tự động</h2>
              <p className="text-xs text-zinc-400 font-bold mt-0.5">Wizard hỗ trợ khởi tạo nhanh Layout phòng chiếu</p>
            </div>
          </div>
          <button 
            onClick={onClose}
            className="p-2 hover:bg-zinc-800 text-zinc-400 hover:text-white rounded-xl transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="flex flex-1 overflow-hidden">
          
          {/* Sidebar Steps */}
          <div className="w-64 bg-zinc-900 border-r border-zinc-800 p-6 flex flex-col gap-2 shrink-0">
            {steps.map((s, idx) => {
              const isActive = step === s.id;
              const isPast = step > s.id;
              return (
                <div key={s.id} className="relative">
                  <div className={`flex items-center gap-3 p-3 rounded-xl transition-all ${
                    isActive ? 'bg-brand-orange/10 text-brand-orange border border-brand-orange/20' : 
                    isPast ? 'text-zinc-300' : 'text-zinc-600'
                  }`}>
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-black ${
                      isActive ? 'bg-brand-orange text-black' : 
                      isPast ? 'bg-zinc-800 text-zinc-300' : 'bg-zinc-900 text-zinc-700 border border-zinc-800'
                    }`}>
                      {isPast ? <Check className="w-4 h-4" /> : s.id}
                    </div>
                    <span className="text-xs font-bold uppercase tracking-wide">{s.title}</span>
                  </div>
                  {idx < steps.length - 1 && (
                    <div className={`w-0.5 h-4 ml-[22px] my-1 ${isPast ? 'bg-brand-orange/50' : 'bg-zinc-800'}`} />
                  )}
                </div>
              );
            })}
          </div>

          {/* Step Content */}
          <div className="flex-1 overflow-y-auto p-8 bg-zinc-950">
            
            {/* STEP 1 */}
            {step === 1 && (
              <div className="space-y-8 animate-slide-in">
                <div>
                  <h3 className="text-lg font-black text-white uppercase tracking-wider mb-4">Quy mô phòng chiếu</h3>
                  <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                    {['SMALL', 'STANDARD', 'LARGE', 'CUSTOM'].map(p => (
                      <div 
                        key={p} 
                        onClick={() => handlePresetChange(p)}
                        className={`p-4 rounded-2xl border-2 cursor-pointer transition-all ${
                          preset === p ? 'border-brand-orange bg-brand-orange/10' : 'border-zinc-800 bg-zinc-900 hover:border-zinc-600'
                        }`}
                      >
                        <div className="flex justify-between items-center mb-2">
                          <span className={`text-xs font-black uppercase tracking-wider ${preset === p ? 'text-brand-orange' : 'text-zinc-300'}`}>
                            {p === 'SMALL' ? 'Nhỏ' : p === 'STANDARD' ? 'Tiêu chuẩn' : p === 'LARGE' ? 'Lớn' : 'Tùy chỉnh'}
                          </span>
                          {preset === p && <Check className="w-4 h-4 text-brand-orange" />}
                        </div>
                        {p === 'SMALL' && <p className="text-2xl font-bold text-white">6 × 8</p>}
                        {p === 'STANDARD' && <p className="text-2xl font-bold text-white">10 × 12</p>}
                        {p === 'LARGE' && <p className="text-2xl font-bold text-white">14 × 18</p>}
                        {p === 'CUSTOM' && <p className="text-sm font-bold text-zinc-500 mt-2">Tự nhập kích thước</p>}
                      </div>
                    ))}
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-6 p-6 bg-zinc-900 rounded-2xl border border-zinc-800">
                  <div className="space-y-2">
                    <label className="text-xs font-bold text-zinc-400 uppercase tracking-wider">Số hàng ghế (Rows)</label>
                    <input 
                      type="number" min="4" max="20"
                      value={rows}
                      onChange={e => { setRows(Number(e.target.value)); setPreset('CUSTOM'); }}
                      className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-white font-bold focus:outline-none focus:border-brand-orange"
                    />
                  </div>
                  <div className="space-y-2">
                    <label className="text-xs font-bold text-zinc-400 uppercase tracking-wider">Số cột ghế (Cols)</label>
                    <input 
                      type="number" min="4" max="20"
                      value={cols}
                      onChange={e => { setCols(Number(e.target.value)); setPreset('CUSTOM'); }}
                      className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-3 text-white font-bold focus:outline-none focus:border-brand-orange"
                    />
                  </div>
                </div>
              </div>
            )}

            {/* STEP 2 */}
            {step === 2 && (
              <div className="space-y-8 animate-slide-in">
                <div>
                  <h3 className="text-lg font-black text-white uppercase tracking-wider mb-4">Lối đi dọc</h3>
                  <div className="grid grid-cols-3 gap-4">
                    {[
                      { id: 'NONE', label: 'Không có' },
                      { id: 'CENTER', label: 'Một lối đi giữa' },
                      { id: 'TWO', label: 'Hai lối đi' }
                    ].map(a => (
                      <div 
                        key={a.id} 
                        onClick={() => setVerticalAisle(a.id)}
                        className={`p-4 rounded-2xl border-2 cursor-pointer transition-all flex flex-col items-center justify-center gap-2 ${
                          verticalAisle === a.id ? 'border-brand-orange bg-brand-orange/10 text-brand-orange' : 'border-zinc-800 bg-zinc-900 text-zinc-300 hover:border-zinc-600'
                        }`}
                      >
                        <span className="text-xs font-black uppercase tracking-wider">{a.label}</span>
                      </div>
                    ))}
                  </div>
                  <p className="text-[10px] text-zinc-500 font-bold mt-3">* Lối đi sẽ chiếm 1 ô (cell) trên sơ đồ thay vì chỉ tạo khoảng trống.</p>
                </div>

                <div className="h-px w-full bg-zinc-800" />

                <div>
                  <h3 className="text-lg font-black text-white uppercase tracking-wider mb-4">Lối đi ngang & Cửa</h3>
                  <div className="space-y-3">
                    <label className="flex items-center gap-3 p-4 bg-zinc-900 border border-zinc-800 rounded-xl cursor-pointer hover:border-zinc-700 transition-colors">
                      <input 
                        type="checkbox" 
                        checked={horizontalAisle}
                        onChange={e => setHorizontalAisle(e.target.checked)}
                        className="w-5 h-5 accent-brand-orange rounded border-zinc-700 bg-zinc-950"
                      />
                      <span className="text-sm font-bold text-white">Có lối đi ngang sau hàng E (Dòng số 5)</span>
                    </label>

                    <div className="flex gap-4">
                      <label className="flex-1 flex items-center gap-3 p-4 bg-zinc-900 border border-zinc-800 rounded-xl cursor-pointer hover:border-zinc-700 transition-colors">
                        <input 
                          type="checkbox" 
                          checked={exitLeft}
                          onChange={e => setExitLeft(e.target.checked)}
                          className="w-5 h-5 accent-brand-orange rounded border-zinc-700 bg-zinc-950"
                        />
                        <DoorOpen className="w-5 h-5 text-zinc-500" />
                        <span className="text-sm font-bold text-white">Cửa thoát hiểm Góc Trái</span>
                      </label>
                      <label className="flex-1 flex items-center gap-3 p-4 bg-zinc-900 border border-zinc-800 rounded-xl cursor-pointer hover:border-zinc-700 transition-colors">
                        <input 
                          type="checkbox" 
                          checked={exitRight}
                          onChange={e => setExitRight(e.target.checked)}
                          className="w-5 h-5 accent-brand-orange rounded border-zinc-700 bg-zinc-950"
                        />
                        <DoorOpen className="w-5 h-5 text-zinc-500" />
                        <span className="text-sm font-bold text-white">Cửa thoát hiểm Góc Phải</span>
                      </label>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* STEP 3 */}
            {step === 3 && (
              <div className="space-y-8 animate-slide-in">
                <div>
                  <h3 className="text-lg font-black text-white uppercase tracking-wider mb-4">Chiến lược phân vùng ghế</h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div 
                      onClick={() => setStrategy('AUTO')}
                      className={`p-5 rounded-2xl border-2 cursor-pointer transition-all ${
                        strategy === 'AUTO' ? 'border-brand-orange bg-brand-orange/10' : 'border-zinc-800 bg-zinc-900 hover:border-zinc-600'
                      }`}
                    >
                      <div className="flex items-center gap-3 mb-3">
                        <div className={`p-2 rounded-xl ${strategy === 'AUTO' ? 'bg-brand-orange text-black' : 'bg-zinc-800 text-zinc-400'}`}>
                          <Sparkles className="w-5 h-5" />
                        </div>
                        <span className={`text-sm font-black uppercase tracking-wider ${strategy === 'AUTO' ? 'text-brand-orange' : 'text-zinc-300'}`}>Hệ thống tự đề xuất</span>
                      </div>
                      <p className="text-xs text-zinc-400 leading-relaxed font-medium">
                        Áp dụng tỷ lệ tối ưu: ~30% Standard ở các hàng đầu, ~50% VIP ở giữa phòng, và ~20% Couple ở các hàng cuối. Tự động xếp 2 ghế Hỗ trợ.
                      </p>
                    </div>

                    <div 
                      onClick={() => setStrategy('ALL_STANDARD')}
                      className={`p-5 rounded-2xl border-2 cursor-pointer transition-all ${
                        strategy === 'ALL_STANDARD' ? 'border-brand-orange bg-brand-orange/10' : 'border-zinc-800 bg-zinc-900 hover:border-zinc-600'
                      }`}
                    >
                      <div className="flex items-center gap-3 mb-3">
                        <div className={`p-2 rounded-xl ${strategy === 'ALL_STANDARD' ? 'bg-brand-orange text-black' : 'bg-zinc-800 text-zinc-400'}`}>
                          <Grid3X3 className="w-5 h-5" />
                        </div>
                        <span className={`text-sm font-black uppercase tracking-wider ${strategy === 'ALL_STANDARD' ? 'text-brand-orange' : 'text-zinc-300'}`}>Toàn bộ là ghế Thường</span>
                      </div>
                      <p className="text-xs text-zinc-400 leading-relaxed font-medium">
                        Trải đều ghế Standard (Thường) cho toàn bộ sơ đồ. Bạn sẽ tự dùng cọ (Brush) để đổi màu VIP/Couple sau.
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* STEP 4 */}
            {step === 4 && (
              <div className="animate-slide-in flex flex-col h-full">
                <div className="flex justify-between items-end mb-6 shrink-0">
                  <div>
                    <h3 className="text-lg font-black text-white uppercase tracking-wider">Xem trước Sơ đồ</h3>
                    <p className="text-xs text-zinc-400 font-bold mt-1">Kiểm tra lại bố cục trước khi áp dụng vào Editor</p>
                  </div>
                  <div className="flex gap-4">
                    <div className="bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2 text-center">
                      <span className="block text-[10px] text-zinc-500 font-bold uppercase tracking-wider mb-0.5">Tổng ghế</span>
                      <span className="text-lg font-black text-white leading-none">{stats.total}</span>
                    </div>
                    <div className="bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2 text-center">
                      <span className="block text-[10px] text-zinc-500 font-bold uppercase tracking-wider mb-0.5">VIP</span>
                      <span className="text-lg font-black text-rose-500 leading-none">{stats.vip}</span>
                    </div>
                    <div className="bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2 text-center">
                      <span className="block text-[10px] text-zinc-500 font-bold uppercase tracking-wider mb-0.5">Couple</span>
                      <span className="text-lg font-black text-pink-500 leading-none">{stats.couple}</span>
                    </div>
                  </div>
                </div>

                <div className="flex-1 bg-zinc-950 border border-zinc-800 rounded-3xl overflow-auto p-4 custom-scrollbar">
                  <div className="min-w-max mx-auto origin-top" style={{ transform: 'scale(0.85)' }}>
                    <SeatGridDesigner 
                      matrix={previewMatrix} 
                      cols={cols} 
                      rows={rows} 
                      isLayoutEditable={false} 
                      skipIO={currentSkipIO}
                    />
                  </div>
                </div>
              </div>
            )}

          </div>
        </div>

        {/* Footer Actions */}
        <div className="px-6 py-4 border-t border-zinc-800 bg-zinc-900 flex justify-between items-center shrink-0">
          <button
            onClick={handleBack}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-xl font-bold text-xs uppercase tracking-wider transition-all ${
              step === 1 ? 'opacity-0 pointer-events-none' : 'text-zinc-400 hover:text-white hover:bg-zinc-800'
            }`}
          >
            <ChevronLeft className="w-4 h-4" />
            Quay lại
          </button>

          {step < 4 ? (
            <button
              onClick={handleNext}
              className="flex items-center gap-2 bg-brand-orange hover:bg-orange-500 text-black font-black px-6 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-all"
            >
              Tiếp tục
              <ChevronRight className="w-4 h-4" />
            </button>
          ) : (
            <button
              onClick={handleApply}
              className="flex items-center gap-2 bg-emerald-500 hover:bg-emerald-400 text-black font-black px-6 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-all shadow-lg shadow-emerald-500/20"
            >
              <Check className="w-4 h-4" />
              Áp dụng vào Trình thiết kế
            </button>
          )}
        </div>
        
      </div>
    </div>
  );
}
