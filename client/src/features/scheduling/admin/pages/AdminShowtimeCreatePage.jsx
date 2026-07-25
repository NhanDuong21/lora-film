// React is removed
import { useOutletContext, useNavigate } from 'react-router-dom';
import { ArrowLeft, Save, Calendar, Clock, Loader2, MapPin, Film, Info } from 'lucide-react';
import useShowtimeForm from '@/features/scheduling/admin/hooks/useShowtimeForm';
import SearchableSelect from '@/components/common/SearchableSelect';
import { getShowtimeStatusPresentation } from '@/features/scheduling/admin/utils/schedulingPresentation';

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

  const cinemaOptions = cinemas.map(c => ({
    value: c.publicId,
    label: c.name,
    subtitle: c.address
  }));

  const auditoriumOptions = auditoriums.map(a => ({
    value: a.publicId,
    label: a.name,
    subtitle: `${a.screenType} - ${a.soundType} • ${a.capacity} ghế`
  }));

  const movieOptions = movies
    .filter(m => m.status !== 'DRAFT')
    .map(m => {
      return {
        value: m.publicId,
        label: m.title,
        subtitle: `${m.durationMinutes} phút • ${m.releaseDate || 'N/A'}`,
        badge: m.status?.replace('_', ' '),
        badgeColor: 'text-blue-400 border-blue-500/20 bg-blue-500/10',
        disabled: false
      };
    });

  const versionOptions = versions.map(v => {
    const isInactive = v.status !== 'ACTIVE';
    return {
      value: v.publicId,
      label: v.versionName,
      subtitle: isInactive ? 'NGỪNG HOẠT ĐỘNG' : `${v.format} - ${v.audioLanguage}`,
      disabled: isInactive
    };
  });

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
              <div className="z-20 relative">
                <label className="block text-xs font-semibold text-zinc-400 mb-1">Cụm rạp *</label>
                <SearchableSelect
                  options={cinemaOptions}
                  value={selectedCinemaId}
                  onChange={setSelectedCinemaId}
                  placeholder="Chọn cụm rạp..."
                  disabled={isLoadingCinemas}
                  error={errors.cinemaId}
                />
                {errors.cinemaId && <p className="text-red-500 text-xs mt-1">{errors.cinemaId}</p>}
              </div>

              <div className="z-20 relative">
                <label className="block text-xs font-semibold text-zinc-400 mb-1">Phòng chiếu *</label>
                <SearchableSelect
                  options={auditoriumOptions}
                  value={selectedAuditoriumId}
                  onChange={setSelectedAuditoriumId}
                  placeholder={!selectedCinemaId ? 'Vui lòng chọn cụm rạp trước' : 'Chọn phòng chiếu...'}
                  disabled={!selectedCinemaId || auditoriums.length === 0}
                  error={errors.auditoriumId}
                />
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
              <div className="z-10 relative">
                <label className="block text-xs font-semibold text-zinc-400 mb-1">Phim *</label>
                <SearchableSelect
                  options={movieOptions}
                  value={selectedMovieId}
                  onChange={setSelectedMovieId}
                  placeholder="Chọn phim..."
                  disabled={isLoadingMovies}
                  error={errors.movieId}
                />
                {errors.movieId && <p className="text-red-500 text-xs mt-1">{errors.movieId}</p>}
              </div>

              <div className="z-10 relative">
                <label className="block text-xs font-semibold text-zinc-400 mb-1">Định dạng *</label>
                <SearchableSelect
                  options={versionOptions}
                  value={selectedVersionId}
                  onChange={setSelectedVersionId}
                  placeholder={!selectedMovieId ? 'Vui lòng chọn phim trước' : 'Chọn định dạng...'}
                  disabled={!selectedMovieId || versions.length === 0}
                  error={errors.versionId}
                />
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
                  {getShowtimeStatusPresentation('DRAFT').label}
                </span>
                <p className="text-[10px] text-zinc-500 mt-2">Suất chiếu được tạo ở trạng thái Bản nháp. Cần chuyển sang Đang mở bán để bắt đầu nhận đặt vé.</p>
              </div>

            </div>
          </div>
        </div>

      </div>
    </div>
  );
};

export default AdminShowtimeCreatePage;
