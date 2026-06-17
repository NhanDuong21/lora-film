import { useState, useMemo } from 'react';
import { useData } from '../../contexts/DataContext';
import { 
  Building2, 
  Search, 
  MapPin, 
  Film, 
  Layers
} from 'lucide-react';

export default function EmployeeScheduleView() {
  const { showtimes, movies, theaters } = useData();
  const [selectedTheaterFilter, setSelectedTheaterFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // 1. Data Completeness Filter: eliminate rows that map unresolvable database objects
  const verifiedSchedules = useMemo(() => {
    return showtimes.map(st => {
      const movie = movies.find(m => String(m.id) === String(st.movieId));
      const theater = theaters.find(t => String(t.id) === String(st.theaterId));
      const hall = theater?.halls?.find(h => String(h.id) === String(st.hallId));

      return {
        showtime: st,
        movie,
        theater,
        hall
      };
    }).filter(item => {
      // Must resolve all metadata attributes, excluding any incomplete records
      return item.movie && item.theater && item.hall;
    });
  }, [showtimes, movies, theaters]);

  // Apply filters
  const filteredSchedules = useMemo(() => {
    return verifiedSchedules.filter(item => {
      const matchTheater = selectedTheaterFilter === 'ALL' || String(item.theater.id) === selectedTheaterFilter;
      const matchSearch = item.movie.title.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          item.hall.name.toLowerCase().includes(searchQuery.toLowerCase());
      return matchTheater && matchSearch;
    });
  }, [verifiedSchedules, selectedTheaterFilter, searchQuery]);

  // Helper to calculate status capsule
  const getSessionStatus = (timeStr) => {
    try {
      const [hours, minutes] = timeStr.split(':').map(Number);
      const now = new Date();
      
      const showtimeTime = new Date();
      showtimeTime.setHours(hours, minutes, 0);

      const diffMs = now - showtimeTime;
      const diffMins = diffMs / 60000;

      // Session is considered in progress if started in the last 2 hours
      if (diffMins >= 0 && diffMins <= 120) {
        return 'IN_PROGRESS';
      }
      return 'WAITING';
    } catch (e) {
      return 'WAITING';
    }
  };

  return (
    <div className="flex-grow flex flex-col space-y-6 h-full">
      {/* Header section */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 border-b border-zinc-800 pb-5">
        <div>
          <h2 className="text-xl font-black text-white uppercase tracking-wider">LỊCH CHIẾU VÀ PHÂN BỔ PHÒNG</h2>
          <p className="text-xs text-zinc-500 uppercase tracking-widest mt-1">Giám sát các phòng chiếu phim thời gian thực của rạp</p>
        </div>

        {/* Filters and searches row */}
        <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
          <div className="flex items-center gap-2 bg-zinc-900 border border-zinc-800 rounded-xl px-3 py-1.5 text-xs text-zinc-300">
            <Building2 className="w-3.5 h-3.5 text-zinc-500" />
            <select
              value={selectedTheaterFilter}
              onChange={(e) => setSelectedTheaterFilter(e.target.value)}
              className="bg-transparent border-none outline-none font-bold text-xs text-zinc-300 cursor-pointer"
            >
              <option value="ALL">TẤT CẢ CỤM RẠP</option>
              {theaters.map(t => (
                <option key={t.id} value={String(t.id)}>{t.name.toUpperCase()}</option>
              ))}
            </select>
          </div>

          <div className="flex items-center gap-2.5 bg-zinc-900 border border-zinc-800 px-3.5 py-1.5 rounded-xl w-full md:w-60">
            <Search className="w-3.5 h-3.5 text-zinc-500 shrink-0" />
            <input
              type="text"
              placeholder="Lọc phòng hoặc tên phim..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="bg-transparent border-none outline-none text-xs w-full text-white placeholder-zinc-650"
            />
          </div>
        </div>
      </div>

      {/* Main Ledger Sheet Container */}
      <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl shadow-xl overflow-hidden flex flex-col">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-zinc-400">
            <thead className="bg-zinc-950 text-zinc-500 text-[10px] font-black uppercase tracking-wider border-b border-zinc-800 select-none">
              <tr>
                <th className="py-4 px-6">
                  <span className="flex items-center gap-1.5">
                    <Building2 className="w-3.5 h-3.5" />
                    CỤM RẠP
                  </span>
                </th>
                <th className="py-4 px-6">
                  <span className="flex items-center gap-1.5">
                    <Layers className="w-3.5 h-3.5" />
                    PHÒNG CHIẾU
                  </span>
                </th>
                <th className="py-4 px-6">
                  <span className="flex items-center gap-1.5">
                    <Film className="w-3.5 h-3.5" />
                    PHIM
                  </span>
                </th>
                <th className="py-4 px-6">ĐỊNH DẠNG</th>
                <th className="py-4 px-6">KHUNG GIỜ CHIẾU</th>
                <th className="py-4 px-6 text-center">TRẠNG THÁI</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800/50">
              {filteredSchedules.map((item, idx) => {
                const status = getSessionStatus(item.showtime.time);
                
                return (
                  <tr key={idx} className="hover:bg-zinc-950/20 transition-colors">
                    {/* Cụm Rạp Column */}
                    <td className="py-4 px-6 font-bold text-white whitespace-nowrap">
                      {item.theater.name}
                    </td>

                    {/* Phòng Chiếu Column */}
                    <td className="py-4 px-6 text-zinc-300 font-medium whitespace-nowrap">
                      {item.hall.name}
                    </td>

                    {/* Phim Column */}
                    <td className="py-4 px-6 font-bold text-zinc-100 max-w-xs truncate" title={item.movie.title}>
                      {item.movie.title}
                    </td>

                    {/* Định Dạng Column */}
                    <td className="py-4 px-6">
                      <span className="text-amber-500 font-extrabold text-[10px] tracking-wide bg-amber-500/5 border border-amber-500/10 px-2 py-0.5 rounded font-mono">
                        {item.hall.format || '2D DIGITAL'}
                      </span>
                    </td>

                    {/* Khung Giờ Column */}
                    <td className="py-4 px-6 font-mono font-bold text-zinc-300 whitespace-nowrap">
                      <span className="text-amber-400">{item.showtime.time}</span>
                      <span className="text-zinc-600 mx-1.5">|</span>
                      <span>{item.showtime.date}</span>
                    </td>

                    {/* Trạng Thái Badge Column */}
                    <td className="py-4 px-6 text-center whitespace-nowrap">
                      {status === 'IN_PROGRESS' ? (
                        <span className="inline-block bg-amber-500/10 text-amber-400 border border-amber-500/20 text-[10px] font-black uppercase rounded px-2.5 py-1 select-none">
                          ĐANG CHIẾU
                        </span>
                      ) : (
                        <span className="inline-block bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-[10px] font-black uppercase rounded px-2.5 py-1 select-none">
                          ĐANG MỞ CHỜ KHÁCH
                        </span>
                      )}
                    </td>
                  </tr>
                );
              })}

              {filteredSchedules.length === 0 && (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-zinc-600 italic">
                    Không tìm thấy dữ liệu phân bổ lịch chiếu phù hợp.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Footer info counts */}
        <div className="bg-zinc-950/60 border-t border-zinc-800 p-4 flex justify-between items-center text-[10px] font-bold uppercase tracking-wider text-zinc-500 select-none">
          <span>Tổng số phòng phân bổ hợp lệ: {filteredSchedules.length}</span>
          <span className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-500 inline-block animate-pulse" />
            <span>Màn hình trạng thái tự động cập nhật</span>
          </span>
        </div>
      </div>
    </div>
  );
}
