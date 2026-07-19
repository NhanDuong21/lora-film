import React from 'react';
import { useOutletContext, useNavigate } from 'react-router-dom';
import { ArrowLeft, Save, Calendar, Clock, Loader2, MapPin, Film, Info } from 'lucide-react';
import useShowtimeForm from '@/features/scheduling/admin/hooks/useShowtimeForm';

const AdminShowtimeCreatePage = () => {
  const { triggerToast } = useOutletContext() || {};
  const navigate = useNavigate();

  const handleSuccess = (showtimePublicId) => {
    navigate(`/admin/showtimes/${showtimePublicId}`);
  };

  const {
    cinemas, auditoriums, movies, versions,
    selectedCinemaId, setSelectedCinemaId,
    selectedAuditoriumId, setSelectedAuditoriumId,
    selectedMovieId, setSelectedMovieId,
    selectedVersionId, setSelectedVersionId,
    startTime, setStartTime,
    isLoadingCinemas, isLoadingMovies, isSubmitting,
    errors,
    selectedCinema, selectedAuditorium, selectedMovie, selectedVersion,
    expectedEndTime, cleaningCompleteTime,
    handleSubmit
  } = useShowtimeForm({ triggerToast, onSuccess: handleSuccess });

  const formatTimeInfo = (dateObj) => {
    if (!dateObj) return '—';
    return dateObj.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' });
  };

  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-[400px] bg-zinc-950 text-white space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between border-b border-zinc-800 pb-4 gap-4">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/admin/showtimes')}
            className="p-2 hover:bg-zinc-800 rounded-xl transition-colors text-zinc-400 hover:text-white"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">TẠO SUẤT CHIẾU MỚI</h1>
            <p className="text-zinc-500 text-sm mt-1">Lên lịch thủ công một suất chiếu cụ thể</p>
          </div>
        </div>
        <button
          onClick={handleSubmit}
          disabled={isSubmitting}
          className="bg-brand-orange hover:bg-opacity-90 text-zinc-950 font-black px-6 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-all duration-300 shadow-lg shadow-brand-orange/10 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
          <span>TẠO SUẤT CHIẾU</span>
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Form Column */}
        <div className="lg:col-span-2 space-y-6">
          
          {/* Location Section */}
          <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-6 space-y-4">
            <h2 className="text-sm font-black uppercase tracking-wider text-zinc-300 flex items-center gap-2 border-b border-zinc-800 pb-3">
              <MapPin className="w-4 h-4 text-brand-orange" /> Địa điểm
            </h2>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-zinc-400 mb-1">Cụm rạp *</label>
                <select
                  value={selectedCinemaId}
                  onChange={(e) => setSelectedCinemaId(e.target.value)}
                  disabled={isLoadingCinemas}
                  className={`w-full bg-zinc-950 border ${errors.cinemaId ? 'border-red-500' : 'border-zinc-800'} text-zinc-200 focus:border-brand-orange/40 rounded-xl py-2.5 px-3.5 text-sm transition-colors focus:outline-none`}
                >
                  <option value="">Chọn cụm rạp</option>
                  {cinemas.map(c => (
                    <option key={c.publicId} value={c.publicId}>{c.name}</option>
                  ))}
                </select>
                {errors.cinemaId && <p className="text-red-500 text-xs mt-1">{errors.cinemaId}</p>}
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-400 mb-1">Phòng chiếu *</label>
                <select
                  value={selectedAuditoriumId}
                  onChange={(e) => setSelectedAuditoriumId(e.target.value)}
                  disabled={!selectedCinemaId || auditoriums.length === 0}
                  className={`w-full bg-zinc-950 border ${errors.auditoriumId ? 'border-red-500' : 'border-zinc-800'} text-zinc-200 focus:border-brand-orange/40 rounded-xl py-2.5 px-3.5 text-sm transition-colors focus:outline-none`}
                >
                  <option value="">Chọn phòng chiếu</option>
                  {auditoriums.map(a => (
                    <option key={a.publicId} value={a.publicId}>{a.name} ({a.screenType} - {a.soundType})</option>
                  ))}
                </select>
                {errors.auditoriumId && <p className="text-red-500 text-xs mt-1">{errors.auditoriumId}</p>}
                {selectedCinemaId && auditoriums.length === 0 && (
                  <p className="text-zinc-500 text-xs mt-1">Không có phòng chiếu active.</p>
                )}
              </div>
            </div>
          </div>

          {/* Movie Section */}
          <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-6 space-y-4">
            <h2 className="text-sm font-black uppercase tracking-wider text-zinc-300 flex items-center gap-2 border-b border-zinc-800 pb-3">
              <Film className="w-4 h-4 text-brand-orange" /> Phim & Định dạng
            </h2>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-zinc-400 mb-1">Phim *</label>
                <select
                  value={selectedMovieId}
                  onChange={(e) => setSelectedMovieId(e.target.value)}
                  disabled={isLoadingMovies}
                  className={`w-full bg-zinc-950 border ${errors.movieId ? 'border-red-500' : 'border-zinc-800'} text-zinc-200 focus:border-brand-orange/40 rounded-xl py-2.5 px-3.5 text-sm transition-colors focus:outline-none`}
                >
                  <option value="">Chọn phim</option>
                  {movies.map(m => (
                    <option key={m.publicId} value={m.publicId}>{m.title}</option>
                  ))}
                </select>
                {errors.movieId && <p className="text-red-500 text-xs mt-1">{errors.movieId}</p>}
              </div>

              <div>
                <label className="block text-xs font-semibold text-zinc-400 mb-1">Định dạng *</label>
                <select
                  value={selectedVersionId}
                  onChange={(e) => setSelectedVersionId(e.target.value)}
                  disabled={!selectedMovieId || versions.length === 0}
                  className={`w-full bg-zinc-950 border ${errors.versionId ? 'border-red-500' : 'border-zinc-800'} text-zinc-200 focus:border-brand-orange/40 rounded-xl py-2.5 px-3.5 text-sm transition-colors focus:outline-none`}
                >
                  <option value="">Chọn định dạng</option>
                  {versions.map(v => (
                    <option key={v.publicId} value={v.publicId}>{v.versionName} ({v.format} - {v.audioLanguage})</option>
                  ))}
                </select>
                {errors.versionId && <p className="text-red-500 text-xs mt-1">{errors.versionId}</p>}
              </div>
            </div>
          </div>

          {/* Timing Section */}
          <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-6 space-y-4">
            <h2 className="text-sm font-black uppercase tracking-wider text-zinc-300 flex items-center gap-2 border-b border-zinc-800 pb-3">
              <Calendar className="w-4 h-4 text-brand-orange" /> Thời gian
            </h2>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-zinc-400 mb-1">Giờ bắt đầu *</label>
                <input
                  type="datetime-local"
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                  className={`w-full bg-zinc-950 border ${errors.startTime ? 'border-red-500' : 'border-zinc-800'} text-zinc-200 focus:border-brand-orange/40 rounded-xl py-2.5 px-3.5 text-sm transition-colors focus:outline-none [color-scheme:dark]`}
                />
                {errors.startTime && <p className="text-red-500 text-xs mt-1">{errors.startTime}</p>}
                <p className="text-zinc-600 text-[10px] mt-1.5 flex items-center gap-1">
                  <Info className="w-3 h-3" />
                  Theo múi giờ của cụm rạp: {selectedCinema ? selectedCinema.timezone : 'Asia/Ho_Chi_Minh'}
                </p>
              </div>

              <div>
                {/* Expected values read-only area */}
                <div className="bg-zinc-950/50 border border-zinc-800/50 p-4 rounded-xl space-y-3 h-full">
                  <div>
                    <span className="text-zinc-500 text-[10px] font-bold uppercase block mb-0.5">Giờ kết thúc dự kiến</span>
                    <span className="text-sm text-zinc-300 font-medium">{formatTimeInfo(expectedEndTime)}</span>
                  </div>
                  <div>
                    <span className="text-zinc-500 text-[10px] font-bold uppercase block mb-0.5">Hoàn tất dọn dẹp</span>
                    <span className="text-sm text-amber-400/80 font-medium">{formatTimeInfo(cleaningCompleteTime)}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
        </div>

        {/* Summary Sidebar */}
        <div className="lg:col-span-1">
          <div className="bg-zinc-900/40 border border-zinc-800 rounded-2xl p-6 sticky top-6">
            <h2 className="text-sm font-black uppercase tracking-wider text-zinc-300 mb-4 pb-3 border-b border-zinc-800">TỔNG QUAN</h2>
            
            <div className="space-y-4">
              <div>
                <span className="text-[10px] text-zinc-500 font-bold uppercase block mb-1">Phim</span>
                <p className="text-sm font-medium text-white">{selectedMovie ? selectedMovie.title : '—'}</p>
                {selectedVersion && (
                  <p className="text-xs text-zinc-400 mt-0.5 bg-zinc-800/50 inline-block px-2 py-0.5 rounded text-nowrap truncate max-w-full">
                    {selectedVersion.versionName} • {selectedVersion.format}
                  </p>
                )}
              </div>

              <div>
                <span className="text-[10px] text-zinc-500 font-bold uppercase block mb-1">Địa điểm</span>
                <p className="text-sm font-medium text-white">{selectedCinema ? selectedCinema.name : '—'}</p>
                <p className="text-xs text-zinc-400 mt-0.5">{selectedAuditorium ? selectedAuditorium.name : '—'}</p>
              </div>

              <div>
                <span className="text-[10px] text-zinc-500 font-bold uppercase block mb-1">Thời gian chiếu</span>
                <p className="text-sm font-medium text-white flex items-center gap-1.5">
                  <Clock className="w-3.5 h-3.5 text-brand-orange" />
                  {startTime ? new Date(startTime).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' }) : '—'}
                </p>
                {selectedMovie?.durationMinutes && (
                  <p className="text-xs text-zinc-500 mt-1 pl-5">Thời lượng: {selectedMovie.durationMinutes} phút</p>
                )}
              </div>
              
              <div className="pt-4 mt-4 border-t border-zinc-800">
                <span className="text-[10px] text-zinc-500 font-bold uppercase block mb-1">Trạng thái khởi tạo</span>
                <span className="px-2.5 py-1 text-[10px] font-black border border-zinc-700 bg-zinc-800 text-zinc-300 rounded-full uppercase tracking-wider inline-block">
                  DRAFT
                </span>
                <p className="text-[10px] text-zinc-500 mt-2">Suất chiếu được tạo ở trạng thái nháp. Cần chuyển sang OPEN FOR BOOKING để mở bán.</p>
              </div>

            </div>
          </div>
        </div>

      </div>
    </div>
  );
};

export default AdminShowtimeCreatePage;
