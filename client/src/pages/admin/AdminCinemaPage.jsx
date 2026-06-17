import { useState, useMemo } from 'react';
import { PlusCircle, X, Sliders, Grid, Save, Database, Settings } from 'lucide-react';

export default function AdminCinemaView({ 
  theaters, 
  updateTheatersState, 
  triggerToast 
}) {
  const [selectedTheaterId, setSelectedTheaterId] = useState(1);
  const [theaterModalOpen, setTheaterModalOpen] = useState(false);
  const [theaterForm, setTheaterForm] = useState({ name: '', address: '', defaultHallsCount: 3 });

  const [hallModalOpen, setHallModalOpen] = useState(false);
  const [targetTheaterIdForHall, setTargetTheaterIdForHall] = useState(null);
  const [hallForm, setHallForm] = useState({ name: '', capacity: 120, format: '2D Digital' });

  // Seat Map Configurator state
  const [configuringHall, setConfiguringHall] = useState(null);
  const [configuringTheaterId, setConfiguringTheaterId] = useState(null);
  const [rows, setRows] = useState(10);
  const [cols, setCols] = useState(12);
  const [activeBrush, setActiveBrush] = useState('STANDARD');
  const [seatMatrix, setSeatMatrix] = useState([]);
  const [exportedJson, setExportedJson] = useState(null);

  const selectedTheater = useMemo(() => {
    return theaters.find(t => t.id === selectedTheaterId) || theaters[0];
  }, [theaters, selectedTheaterId]);

  // Non-destructive matrix resizing handlers
  const handleRowsChange = (newRows) => {
    setRows(newRows);
    setSeatMatrix(prev => {
      const nextMatrix = [];
      for (let r = 0; r < newRows; r++) {
        const row = [];
        for (let c = 0; c < cols; c++) {
          const oldCell = prev[r]?.[c];
          if (oldCell) {
            row.push(oldCell);
          } else {
            row.push({ type: 'STANDARD', label: '' });
          }
        }
        nextMatrix.push(row);
      }
      return nextMatrix;
    });
  };

  const handleColsChange = (newCols) => {
    setCols(newCols);
    setSeatMatrix(prev => {
      const nextMatrix = [];
      for (let r = 0; r < rows; r++) {
        const row = [];
        for (let c = 0; c < newCols; c++) {
          const oldCell = prev[r]?.[c];
          if (oldCell) {
            row.push(oldCell);
          } else {
            row.push({ type: 'STANDARD', label: '' });
          }
        }
        nextMatrix.push(row);
      }
      return nextMatrix;
    });
  };

  const handleOpenAddTheater = () => {
    setTheaterForm({ name: '', address: '', defaultHallsCount: 3 });
    setTheaterModalOpen(true);
  };

  const handleSaveTheater = (e) => {
    e.preventDefault();
    if (!theaterForm.name || !theaterForm.address) {
      triggerToast('Vui lòng điền đầy đủ tên và địa chỉ cụm rạp!', 'error');
      return;
    }

    const newTheaterId = Date.now();
    const count = parseInt(theaterForm.defaultHallsCount) || 3;
    const halls = [];
    for (let i = 1; i <= count; i++) {
      halls.push({
        id: `${newTheaterId}-${i}`,
        name: `Phòng Chiếu ${i}`,
        capacity: 120,
        format: '2D Digital',
        seatLayout: null
      });
    }

    const newTheater = {
      id: newTheaterId,
      name: theaterForm.name,
      address: theaterForm.address,
      halls
    };

    const updated = [...theaters, newTheater];
    updateTheatersState(updated);
    setSelectedTheaterId(newTheaterId);
    setTheaterModalOpen(false);
    triggerToast('Thêm cụm rạp mới thành công!');
  };

  const handleOpenAddHall = (theaterId) => {
    setTargetTheaterIdForHall(theaterId);
    const matchedT = theaters.find(t => t.id === theaterId);
    setHallForm({ 
      name: `Phòng Chiếu ${matchedT ? matchedT.halls.length + 1 : 1}`, 
      capacity: 120, 
      format: '2D Digital' 
    });
    setHallModalOpen(true);
  };

  const handleSaveHall = (e) => {
    e.preventDefault();
    if (!hallForm.name) {
      triggerToast('Vui lòng nhập tên phòng chiếu!', 'error');
      return;
    }

    const updated = theaters.map(t => {
      if (t.id === targetTheaterIdForHall) {
        return {
          ...t,
          halls: [
            ...t.halls,
            { 
              id: `${t.id}-${t.halls.length + 1}-${Date.now()}`, 
              name: hallForm.name, 
              capacity: parseInt(hallForm.capacity) || 120, 
              format: hallForm.format,
              seatLayout: null
            }
          ]
        };
      }
      return t;
    });

    updateTheatersState(updated);
    setHallModalOpen(false);
    triggerToast('Đã thêm phòng chiếu mới thành công!');
  };

  const handleRenameHall = (theaterId, hallId) => {
    const newName = prompt('Nhập tên phòng chiếu mới:');
    if (!newName) return;

    const updated = theaters.map(t => {
      if (t.id === theaterId) {
        return {
          ...t,
          halls: t.halls.map(h => h.id === hallId ? { ...h, name: newName } : h)
        };
      }
      return t;
    });

    updateTheatersState(updated);
    triggerToast('Đã đổi tên phòng chiếu thành công!');
  };

  const handleDeleteHall = (theaterId, hallId) => {
    if (confirm('Bạn có chắc chắn muốn xóa phòng chiếu này?')) {
      const updated = theaters.map(t => {
        if (t.id === theaterId) {
          return {
            ...t,
            halls: t.halls.filter(h => h.id !== hallId)
          };
        }
        return t;
      });
      updateTheatersState(updated);
      triggerToast('Đã xóa phòng chiếu thành công!');
    }
  };

  // Launch seat layout editor
  const handleOpenLayoutConfig = (theaterId, hall) => {
    setConfiguringTheaterId(theaterId);
    setConfiguringHall(hall);
    if (hall.seatLayout) {
      setRows(hall.seatLayout.rows || 10);
      setCols(hall.seatLayout.cols || 12);
      setSeatMatrix(hall.seatLayout.matrix || []);
    } else {
      setRows(10);
      setCols(12);
      // Pre-populate default standard grid
      const initialMatrix = [];
      for (let r = 0; r < 10; r++) {
        const row = [];
        for (let c = 0; c < 12; c++) {
          row.push({ type: 'STANDARD', label: '' });
        }
        initialMatrix.push(row);
      }
      setSeatMatrix(initialMatrix);
    }
    setExportedJson(null);
  };

  const handleCellPaint = (r, c) => {
    setSeatMatrix(prev => {
      const copy = prev.map(row => row.map(cell => ({ ...cell })));
      copy[r][c].type = activeBrush;
      return copy;
    });
  };

  // Compile JSON map model & update state
  const handleSaveLayout = () => {
    let totalSeatsCount = 0;
    let standardCount = 0;
    let vipCount = 0;
    let sweetboxCount = 0;
    let aisleCount = 0;

    const formattedMatrix = seatMatrix.map((row, rIdx) => {
      const rowName = String.fromCharCode(65 + rIdx); // A, B, C...
      let seatNumInRow = 1;
      return row.map((cell, cIdx) => {
        const isAisle = cell.type === 'AISLE';
        let label = '';
        if (!isAisle) {
          label = `${rowName}${seatNumInRow}`;
          seatNumInRow++;
          totalSeatsCount++;
          if (cell.type === 'STANDARD') standardCount++;
          if (cell.type === 'VIP') vipCount++;
          if (cell.type === 'SWEETBOX') sweetboxCount++;
        } else {
          aisleCount++;
        }

        return {
          rowName,
          colIndex: cIdx + 1,
          type: cell.type,
          label: label
        };
      });
    });

    const layoutPayload = {
      hallId: configuringHall.id,
      hallName: configuringHall.name,
      rows,
      cols,
      summary: {
        totalSeats: totalSeatsCount,
        standard: standardCount,
        vip: vipCount,
        sweetbox: sweetboxCount,
        aisle: aisleCount
      },
      matrix: formattedMatrix
    };

    setExportedJson(JSON.stringify(layoutPayload, null, 2));

    // Update global theater capacity and seat layout
    const updatedTheaters = theaters.map(t => {
      if (t.id === configuringTheaterId) {
        return {
          ...t,
          halls: t.halls.map(h => {
            if (h.id === configuringHall.id) {
              return {
                ...h,
                capacity: totalSeatsCount,
                seatLayout: {
                  rows,
                  cols,
                  matrix: seatMatrix
                }
              };
            }
            return h;
          })
        };
      }
      return t;
    });

    updateTheatersState(updatedTheaters);
    triggerToast(`Đã lưu sơ đồ ghế thành công! Sức chứa mới: ${totalSeatsCount} ghế.`);
  };

  return (
    <div className="flex-1 h-full overflow-y-auto p-6 bg-zinc-950 space-y-6">
      {/* Page Header Titles */}
      <div className="flex justify-between items-center pb-2 border-b border-zinc-800">
        <div>
          <h3 className="text-base font-bold text-zinc-50 uppercase tracking-wide">QUẢN LÝ PHÒNG CHIẾU</h3>
          <p className="text-xs text-zinc-400 mt-1 uppercase tracking-wide">Cấu hình sơ đồ và định dạng thiết bị phòng chiếu hệ thống Lora</p>
        </div>
      </div>

      {/* Dual Panel Split Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column (1/3 Width Cluster Selector) */}
        <div className="lg:col-span-1 bg-zinc-900/40 border border-zinc-900 p-6 rounded-2xl space-y-3 shadow-xl h-fit">
          <div className="flex justify-between items-center pb-2">
            <span className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">
              Danh sách cụm rạp
            </span>
            <button
              onClick={handleOpenAddTheater}
              className="flex items-center gap-1 bg-brand-coral hover:bg-opacity-90 text-white text-[10px] font-black py-1.5 px-3 rounded-lg uppercase tracking-wider transition-all"
            >
              <PlusCircle className="w-3.5 h-3.5" />
              <span>Thêm Rạp Mới</span>
            </button>
          </div>

          <div className="space-y-2 max-h-[500px] overflow-y-auto pr-1">
            {theaters.map(t => (
              <button
                key={t.id}
                onClick={() => setSelectedTheaterId(t.id)}
                className={`w-full text-left p-4 rounded-xl border transition-all ${
                  (selectedTheater && selectedTheater.id === t.id)
                    ? 'bg-brand-coral/10 border-brand-coral text-white' 
                    : 'bg-zinc-900/40 border-zinc-900 text-zinc-400 hover:bg-zinc-900/60'
                }`}
              >
                <h4 className="font-bold text-sm text-zinc-100">{t.name}</h4>
                <p className="text-[11px] text-zinc-400 truncate mt-1">{t.address}</p>
              </button>
            ))}
            {theaters.length === 0 && (
              <div className="text-center py-8 text-zinc-650 text-xs font-semibold uppercase">Chưa có cụm rạp nào</div>
            )}
          </div>
        </div>

        {/* Right Column (2/3 Width Room Controller Spreadsheet) */}
        <div className="lg:col-span-2 bg-zinc-900/40 backdrop-blur-md border border-zinc-900 p-6 rounded-2xl space-y-6 shadow-xl">
          {selectedTheater ? (
            <>
              <div className="flex justify-between items-center border-b border-zinc-900 pb-4">
                <div>
                  <h3 className="font-black text-zinc-100 text-base">{selectedTheater.name}</h3>
                  <p className="text-zinc-400 text-xs mt-0.5">{selectedTheater.address}</p>
                </div>
                <button
                  onClick={() => handleOpenAddHall(selectedTheater.id)}
                  className="flex items-center gap-1.5 bg-zinc-950 hover:bg-zinc-900 border border-zinc-900 text-[10px] font-black px-3.5 py-2 rounded-xl text-white transition-all"
                >
                  <PlusCircle className="w-3.5 h-3.5 text-brand-coral" />
                  <span>THÊM PHÒNG CHIẾU</span>
                </button>
              </div>

              <div className="space-y-3 max-h-[450px] overflow-y-auto pr-1">
                {selectedTheater.halls && selectedTheater.halls.map(h => (
                  <div 
                    key={h.id} 
                    className="bg-zinc-950/40 border border-zinc-900 p-4 rounded-xl flex items-center justify-between gap-4 hover:border-zinc-800 transition-all"
                  >
                    <div>
                      <h4 className="font-bold text-zinc-150 text-sm">{h.name}</h4>
                      <span className="text-[10px] text-zinc-400 block uppercase font-bold mt-1.5">
                        Định dạng: {h.format} | Sức chứa: {h.capacity} ghế
                      </span>
                    </div>
                    <div className="flex gap-2">
                      <button
                        onClick={() => handleOpenLayoutConfig(selectedTheater.id, h)}
                        className="py-1.5 px-3 bg-amber-500/10 hover:bg-amber-500/20 border border-amber-500/30 hover:border-amber-500 text-[10px] font-black text-amber-400 rounded-xl transition-all"
                      >
                        Sơ đồ ghế
                      </button>
                      <button
                        onClick={() => handleRenameHall(selectedTheater.id, h.id)}
                        className="py-1.5 px-3 bg-zinc-900 hover:bg-zinc-850 text-[10px] font-black text-zinc-400 border border-zinc-900 hover:text-white rounded-xl transition-all"
                      >
                        Đổi tên
                      </button>
                      <button
                        onClick={() => handleDeleteHall(selectedTheater.id, h.id)}
                        className="py-1.5 px-3 bg-red-950/20 border border-red-900/30 hover:border-red-900/60 text-[10px] font-black text-red-400 rounded-xl transition-all"
                      >
                        Xóa
                      </button>
                    </div>
                  </div>
                ))}
                {(!selectedTheater.halls || selectedTheater.halls.length === 0) && (
                  <div className="text-center py-12 text-zinc-650 text-xs font-semibold uppercase">Chưa có phòng chiếu nào</div>
                )}
              </div>
            </>
          ) : (
            <div className="text-center py-24 text-zinc-650 text-xs font-bold uppercase tracking-wider">
              Vui lòng chọn một cụm rạp để xem chi tiết
            </div>
          )}
        </div>
      </div>

      {/* Seat Map Configurator Fullscreen Panel / Modal */}
      {configuringHall && (
        <div className="fixed inset-0 bg-zinc-950 z-50 flex flex-col font-sans">
          {/* Top Bar Header */}
          <header className="h-16 bg-zinc-900 border-b border-zinc-800 px-6 flex justify-between items-center select-none shrink-0">
            <div className="flex items-center gap-3">
              <div className="bg-amber-500/10 p-2 rounded-xl border border-amber-500/20">
                <Settings className="w-5 h-5 text-amber-500" />
              </div>
              <div>
                <h2 className="text-sm font-black text-zinc-50 uppercase tracking-wider">
                  Cấu hình sơ đồ ghế: {configuringHall.name}
                </h2>
                <p className="text-[10px] text-zinc-400 font-bold uppercase tracking-wider mt-0.5">
                  Thiết kế cấu trúc ma trận phòng chiếu • {theaters.find(t => t.id === configuringTheaterId)?.name}
                </p>
              </div>
            </div>

            <button 
              onClick={() => setConfiguringHall(null)}
              className="bg-zinc-900 border border-zinc-850 hover:border-zinc-700 hover:text-white text-zinc-400 p-2 rounded-xl transition-all"
            >
              <X className="w-5 h-5" />
            </button>
          </header>

          {/* Editor Workspace Container */}
          <div className="flex-1 flex overflow-hidden">
            {/* Left Control Bar */}
            <aside className="w-80 bg-zinc-900 border-r border-zinc-800 p-5 flex flex-col justify-between overflow-y-auto shrink-0 select-none">
              <div className="space-y-6">
                {/* Grid Resizer */}
                <div className="space-y-4">
                  <div className="flex items-center gap-2 border-b border-zinc-800 pb-2">
                    <Sliders className="w-4 h-4 text-amber-500" />
                    <h3 className="font-bold text-xs text-white uppercase tracking-wider">Kích thước lưới</h3>
                  </div>

                  {/* Rows Slider */}
                  <div className="space-y-2">
                    <div className="flex justify-between text-xs font-bold text-zinc-400">
                      <span>Số hàng (Rows)</span>
                      <span className="text-amber-500 font-black">{rows}</span>
                    </div>
                    <input 
                      type="range"
                      min="4"
                      max="20"
                      value={rows}
                      onChange={(e) => handleRowsChange(parseInt(e.target.value))}
                      className="w-full h-1 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-amber-500"
                    />
                  </div>

                  {/* Columns Slider */}
                  <div className="space-y-2">
                    <div className="flex justify-between text-xs font-bold text-zinc-400">
                      <span>Số cột (Cols)</span>
                      <span className="text-amber-500 font-black">{cols}</span>
                    </div>
                    <input 
                      type="range"
                      min="4"
                      max="20"
                      value={cols}
                      onChange={(e) => handleColsChange(parseInt(e.target.value))}
                      className="w-full h-1 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-amber-500"
                    />
                  </div>
                </div>

                {/* Brush Selectors */}
                <div className="space-y-4">
                  <div className="flex items-center gap-2 border-b border-zinc-800 pb-2">
                    <Grid className="w-4 h-4 text-amber-500" />
                    <h3 className="font-bold text-xs text-white uppercase tracking-wider">Công cụ vẽ loại ghế</h3>
                  </div>

                  <div className="flex flex-col gap-2">
                    {/* Standard Brush */}
                    <button
                      onClick={() => setActiveBrush('STANDARD')}
                      className={`flex items-center justify-between p-3 rounded-xl border text-left transition-all ${
                        activeBrush === 'STANDARD'
                          ? 'bg-zinc-950 border-amber-500 ring-2 ring-offset-2 ring-offset-zinc-900 ring-amber-500 text-white'
                          : 'bg-zinc-900 border-zinc-800 text-zinc-400 hover:bg-zinc-950'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-4 h-4 rounded-full bg-purple-600"></div>
                        <span className="text-xs font-black uppercase tracking-wider">Ghế Standard</span>
                      </div>
                    </button>

                    {/* VIP Brush */}
                    <button
                      onClick={() => setActiveBrush('VIP')}
                      className={`flex items-center justify-between p-3 rounded-xl border text-left transition-all ${
                        activeBrush === 'VIP'
                          ? 'bg-zinc-950 border-amber-500 ring-2 ring-offset-2 ring-offset-zinc-900 ring-amber-500 text-white'
                          : 'bg-zinc-900 border-zinc-800 text-zinc-400 hover:bg-zinc-950'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-4 h-4 rounded-full bg-red-500"></div>
                        <span className="text-xs font-black uppercase tracking-wider">Ghế VIP</span>
                      </div>
                    </button>

                    {/* Sweetbox Brush */}
                    <button
                      onClick={() => setActiveBrush('SWEETBOX')}
                      className={`flex items-center justify-between p-3 rounded-xl border text-left transition-all ${
                        activeBrush === 'SWEETBOX'
                          ? 'bg-zinc-950 border-amber-500 ring-2 ring-offset-2 ring-offset-zinc-900 ring-amber-500 text-white'
                          : 'bg-zinc-900 border-zinc-800 text-zinc-400 hover:bg-zinc-950'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-4 h-4 rounded-full bg-amber-400"></div>
                        <span className="text-xs font-black uppercase tracking-wider">Ghế Sweetbox / Đôi</span>
                      </div>
                    </button>

                    {/* Aisle/Walkway Brush */}
                    <button
                      onClick={() => setActiveBrush('AISLE')}
                      className={`flex items-center justify-between p-3 rounded-xl border text-left transition-all ${
                        activeBrush === 'AISLE'
                          ? 'bg-zinc-950 border-amber-500 ring-2 ring-offset-2 ring-offset-zinc-900 ring-amber-500 text-white'
                          : 'bg-zinc-900 border-zinc-800 text-zinc-400 hover:bg-zinc-950'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-4 h-4 rounded bg-zinc-800 border border-dashed border-zinc-700"></div>
                        <span className="text-xs font-black uppercase tracking-wider">Lối đi / Trống</span>
                      </div>
                    </button>
                  </div>
                </div>

                {/* Summary Statistics */}
                <div className="bg-zinc-950/50 rounded-2xl p-4 border border-zinc-850 space-y-3">
                  <span className="text-[10px] font-black text-zinc-500 uppercase tracking-widest block">Thống kê sức chứa</span>
                  
                  <div className="space-y-1.5 text-xs">
                    <div className="flex justify-between">
                      <span className="text-zinc-450">Standard (Purple):</span>
                      <span className="text-white font-bold">{seatMatrix.flat().filter(c => c.type === 'STANDARD').length}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-zinc-450">VIP (Red):</span>
                      <span className="text-white font-bold">{seatMatrix.flat().filter(c => c.type === 'VIP').length}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-zinc-450">Sweetbox (Amber):</span>
                      <span className="text-white font-bold">{seatMatrix.flat().filter(c => c.type === 'SWEETBOX').length}</span>
                    </div>
                    <div className="flex justify-between border-t border-zinc-900 pt-1.5 mt-1.5 font-bold">
                      <span className="text-zinc-300">Tổng ghế hoạt động:</span>
                      <span className="text-amber-500">{seatMatrix.flat().filter(c => c.type !== 'AISLE').length} ghế</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Layout Export Preview */}
              <div className="mt-4 pt-4 border-t border-zinc-800 space-y-4">
                <button
                  onClick={handleSaveLayout}
                  className="w-full bg-emerald-500 hover:bg-emerald-600 text-black font-black py-3 rounded-xl text-xs uppercase tracking-wider transition-all shadow-lg flex items-center justify-center gap-2"
                >
                  <Save className="w-4 h-4" />
                  <span>Lưu Bố Cục Sơ Đồ</span>
                </button>
              </div>
            </aside>

            {/* Right Interactive Seating Matrix Canvas */}
            <main className="flex-grow bg-zinc-950 p-8 flex flex-col items-center justify-start overflow-auto">
              
              {/* Screen Badge */}
              <div className="bg-zinc-800 text-zinc-200 font-bold px-10 py-2.5 rounded-lg text-xs uppercase tracking-widest border border-zinc-700/50 mb-10 select-none shadow-xl shadow-black/30">
                MÀN HÌNH CHIẾU
              </div>

              {/* Visual Seat Matrix Configuration Box */}
              <div className="bg-zinc-900/30 border border-zinc-850 p-8 rounded-3xl max-w-full">
                
                {/* Column Headers Numerical Indexes */}
                <div className="flex mb-3 select-none">
                  {/* Empty spacer corner column for Row Letters */}
                  <div className="w-8 shrink-0"></div>
                  {/* Columns loop */}
                  <div 
                    className="grid gap-2 text-center text-[10px] font-black text-zinc-500"
                    style={{ 
                      gridTemplateColumns: `repeat(${cols}, minmax(36px, 1fr))`,
                      width: `${cols * 44}px`
                    }}
                  >
                    {Array.from({ length: cols }).map((_, idx) => (
                      <div key={idx} className="w-9">{idx + 1}</div>
                    ))}
                  </div>
                </div>

                {/* Rows & Cells */}
                <div className="space-y-2">
                  {seatMatrix.map((row, rIdx) => {
                    const rowLetter = String.fromCharCode(65 + rIdx); // A, B, C...
                    return (
                      <div key={rIdx} className="flex items-center">
                        
                        {/* Row letter left anchor */}
                        <div className="w-8 text-[11px] font-black text-zinc-400 select-none uppercase">
                          {rowLetter}
                        </div>

                        {/* Interactive seat buttons row container */}
                        <div 
                          className="grid gap-2"
                          style={{ 
                            gridTemplateColumns: `repeat(${cols}, minmax(36px, 1fr))`,
                            width: `${cols * 44}px`
                          }}
                        >
                          {row.map((cell, cIdx) => {
                            let cellBg;
                            let labelColor = 'text-white/60';

                            if (cell.type === 'STANDARD') {
                              cellBg = 'bg-purple-600 hover:bg-purple-500 text-white border border-purple-700';
                            } else if (cell.type === 'VIP') {
                              cellBg = 'bg-red-500 hover:bg-red-400 text-white border border-red-650';
                            } else if (cell.type === 'SWEETBOX') {
                              cellBg = 'bg-amber-400 hover:bg-amber-300 text-black font-extrabold border border-amber-500';
                              labelColor = 'text-black/80';
                            } else {
                              // AISLE/WALKWAY
                              cellBg = 'bg-zinc-950 border border-dashed border-zinc-800 text-transparent hover:bg-zinc-900 hover:border-zinc-700 cursor-pointer';
                            }

                            return (
                              <button
                                key={cIdx}
                                onClick={() => handleCellPaint(rIdx, cIdx)}
                                className={`w-9 h-9 rounded-lg flex items-center justify-center text-[10px] font-black uppercase tracking-tighter transition-all ${cellBg}`}
                                title={`Hàng ${rowLetter} - Cột ${cIdx + 1} (${cell.type})`}
                              >
                                {cell.type !== 'AISLE' && (
                                  <span className={labelColor}>
                                    {rowLetter}{cIdx + 1}
                                  </span>
                                )}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* JSON preview drawer section */}
              {exportedJson && (
                <div className="w-full max-w-4xl mt-12 bg-zinc-900 border border-zinc-800 rounded-2xl p-5 space-y-3 font-mono shadow-2xl">
                  <div className="flex justify-between items-center border-b border-zinc-800 pb-2">
                    <div className="flex items-center gap-2 text-xs font-bold text-zinc-300 uppercase tracking-wider">
                      <Database className="w-4 h-4 text-emerald-500" />
                      <span>Spring Boot Microservice REST API Payload Preview</span>
                    </div>
                    <button 
                      onClick={() => setExportedJson(null)}
                      className="text-zinc-500 hover:text-zinc-300 text-xs"
                    >
                      Ẩn Preview
                    </button>
                  </div>
                  <pre className="text-[10px] leading-relaxed text-zinc-400 overflow-x-auto max-h-48 p-4 bg-zinc-950 rounded-xl border border-zinc-900 scrollbar-thin">
                    {exportedJson}
                  </pre>
                </div>
              )}

            </main>
          </div>
        </div>
      )}

      {/* Modal Add Theater */}
      {theaterModalOpen && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <form onSubmit={handleSaveTheater} className="bg-zinc-950 border border-zinc-900 rounded-2xl p-6 md:p-8 max-w-md w-full space-y-4 shadow-2xl">
            <div className="flex justify-between items-center border-b border-zinc-900 pb-3">
              <h3 className="text-base font-black text-zinc-100 uppercase tracking-wider">THÊM CỤM RẠP MỚI</h3>
              <button type="button" onClick={() => setTheaterModalOpen(false)} className="text-zinc-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-1">
              <label className="text-zinc-500 text-[10px] font-black uppercase block">Tên cụm rạp</label>
              <input
                type="text"
                value={theaterForm.name}
                onChange={(e) => setTheaterForm({ ...theaterForm, name: e.target.value })}
                placeholder="Ví dụ: Lora Nguyễn Du"
                className="w-full bg-zinc-900/40 border border-zinc-900 rounded-xl py-2 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral"
                required
              />
            </div>

            <div className="space-y-1">
              <label className="text-zinc-500 text-[10px] font-black uppercase block">Địa chỉ</label>
              <input
                type="text"
                value={theaterForm.address}
                onChange={(e) => setTheaterForm({ ...theaterForm, address: e.target.value })}
                placeholder="Ví dụ: 116 Nguyễn Du, Quận 1, TP. HCM"
                className="w-full bg-zinc-900/40 border border-zinc-900 rounded-xl py-2 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral"
                required
              />
            </div>

            <div className="space-y-1">
              <label className="text-zinc-500 text-[10px] font-black uppercase block">Số lượng phòng chiếu mặc định</label>
              <input
                type="number"
                min="1"
                max="10"
                value={theaterForm.defaultHallsCount}
                onChange={(e) => setTheaterForm({ ...theaterForm, defaultHallsCount: parseInt(e.target.value) || 1 })}
                className="w-full bg-zinc-900/40 border border-zinc-900 rounded-xl py-2 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral"
                required
              />
            </div>

            <button
              type="submit"
              className="w-full bg-brand-coral hover:bg-opacity-90 text-white font-black py-3 rounded-xl text-xs uppercase tracking-wider transition-all"
            >
              LƯU THÔNG TIN
            </button>
          </form>
        </div>
      )}

      {/* Modal Add Hall */}
      {hallModalOpen && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <form onSubmit={handleSaveHall} className="bg-zinc-950 border border-zinc-900 rounded-2xl p-6 md:p-8 max-w-md w-full space-y-4 shadow-2xl">
            <div className="flex justify-between items-center border-b border-zinc-900 pb-3">
              <h3 className="text-base font-black text-zinc-100 uppercase tracking-wider">THÊM PHÒNG CHIẾU MỚI</h3>
              <button type="button" onClick={() => setHallModalOpen(false)} className="text-zinc-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-1">
              <label className="text-zinc-500 text-[10px] font-black uppercase block">Tên phòng chiếu</label>
              <input
                type="text"
                value={hallForm.name}
                onChange={(e) => setHallForm({ ...hallForm, name: e.target.value })}
                placeholder="Ví dụ: Phòng Chiếu 5"
                className="w-full bg-zinc-900/40 border border-zinc-900 rounded-xl py-2 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral"
                required
              />
            </div>

            <div className="space-y-1">
              <label className="text-zinc-500 text-[10px] font-black uppercase block">Sức chứa (Ghế)</label>
              <input
                type="number"
                min="10"
                max="300"
                value={hallForm.capacity}
                onChange={(e) => setHallForm({ ...hallForm, capacity: parseInt(e.target.value) || 120 })}
                className="w-full bg-zinc-900/40 border border-zinc-900 rounded-xl py-2 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral"
                required
              />
            </div>

            <div className="space-y-1">
              <label className="text-zinc-500 text-[10px] font-black uppercase block">Định dạng chiếu</label>
              <select
                value={hallForm.format}
                onChange={(e) => setHallForm({ ...hallForm, format: e.target.value })}
                className="w-full bg-zinc-900/40 border border-zinc-900 rounded-xl py-2 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral"
              >
                <option value="2D Digital">2D Digital</option>
                <option value="3D Digital">3D Digital</option>
                <option value="IMAX 3D">IMAX 3D</option>
                <option value="Gold Class">Gold Class</option>
              </select>
            </div>

            <button
              type="submit"
              className="w-full bg-brand-coral hover:bg-opacity-90 text-white font-black py-3 rounded-xl text-xs uppercase tracking-wider transition-all"
            >
              LƯU THÔNG TIN
            </button>
          </form>
        </div>
      )}
    </div>
  );
}

