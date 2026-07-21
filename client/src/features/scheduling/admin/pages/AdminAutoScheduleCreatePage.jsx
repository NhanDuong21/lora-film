import { useState } from 'react';
import { useOutletContext, useNavigate } from 'react-router-dom';
import { ArrowLeft, Save, Loader2, MapPin, Settings2, CalendarDays, ChevronRight, AlertCircle, Search } from 'lucide-react';
import useAutoScheduleForm from '@/features/scheduling/admin/hooks/useAutoScheduleForm';
import SearchableSelect from '@/components/common/SearchableSelect';

const AdminAutoScheduleCreatePage = () => {
  const { triggerToast } = useOutletContext() || {};
  const navigate = useNavigate();

  const handleSuccess = (previewPublicId) => {
    navigate(`/admin/showtime-schedules/${previewPublicId}`);
  };

  const {
    cinemas, movies, auditoriums, versionsByMovie,
    selectedCinemaId, setSelectedCinemaId, selectedCinema,
    scheduleFrom, setScheduleFrom,
    scheduleTo, setScheduleTo,
    slotGranularityMinutes, setSlotGranularityMinutes,
    previewTtlMinutes, setPreviewTtlMinutes,
    selectedAuditoriumIds, toggleAuditorium,
    selectedMovieVersionIds, toggleVersion,
    isLoadingCinemas, isLoadingAuditoriums, isLoadingMovies, isSubmitting,
    errors, toggleMovieExpansion,
    handleSubmit
  } = useAutoScheduleForm({ triggerToast, onSuccess: handleSuccess });

  const [expandedMovies, setExpandedMovies] = useState({});
  const [movieSearch, setMovieSearch] = useState('');

  const handleToggleMovie = (movieId, isDisabled) => {
    if (isDisabled) return;
    const isExpanded = !expandedMovies[movieId];
    setExpandedMovies(prev => ({ ...prev, [movieId]: isExpanded }));
    toggleMovieExpansion(movieId, isExpanded);
  };

  const cinemaOptions = cinemas.map(c => ({
    value: c.publicId,
    label: c.name,
    subtitle: c.address
  }));

  const filteredMovies = movies.filter(m => m.title.toLowerCase().includes(movieSearch.toLowerCase()));

  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-[400px] bg-zinc-950 text-white space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between border-b border-zinc-800 pb-4 gap-4">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate(-1)}
            className="p-2 hover:bg-zinc-800 rounded-xl transition-colors text-zinc-400 hover:text-white"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white flex items-center gap-2">
              <Settings2 className="w-6 h-6 text-brand-orange" />
              CẤU HÌNH XẾP LỊCH TỰ ĐỘNG
            </h1>
            <p className="text-zinc-500 text-sm mt-1">Thiết lập tham số để tạo bản xem trước lịch chiếu</p>
          </div>
        </div>
        <button
          onClick={handleSubmit}
          disabled={isSubmitting}
          className="bg-brand-orange hover:bg-opacity-90 text-zinc-950 font-black px-6 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-all duration-300 shadow-lg shadow-brand-orange/10 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
          <span>TẠO BẢN XEM TRƯỚC</span>
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Left Column (Step 1 & 2) */}
        <div className="lg:col-span-1 space-y-6">
          
          {/* Step 1: Scope */}
          <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-5 space-y-4">
            <h2 className="text-sm font-black uppercase tracking-wider text-zinc-300 flex items-center gap-2 border-b border-zinc-800 pb-3">
              <span className="bg-brand-orange text-zinc-950 w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-black">1</span>
              Phạm vi
            </h2>
            
            <div className="space-y-4">
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
                {errors.cinemaId && <p className="text-red-500 text-[10px] mt-1">{errors.cinemaId}</p>}
                {selectedCinema && (
                  <p className="text-zinc-500 text-[10px] mt-1 flex items-center gap-1">
                    <MapPin className="w-3 h-3" /> Múi giờ: {selectedCinema.timezone}
                  </p>
                )}
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-zinc-400 mb-1">Từ ngày *</label>
                  <input
                    type="date"
                    value={scheduleFrom}
                    onChange={(e) => setScheduleFrom(e.target.value)}
                    className={`w-full bg-zinc-950 border ${errors.scheduleFrom ? 'border-red-500' : 'border-zinc-800'} text-zinc-200 focus:border-brand-orange/40 rounded-xl py-2 px-3 text-sm transition-colors focus:outline-none [color-scheme:dark]`}
                  />
                  {errors.scheduleFrom && <p className="text-red-500 text-[10px] mt-1">{errors.scheduleFrom}</p>}
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-400 mb-1">Đến ngày *</label>
                  <input
                    type="date"
                    value={scheduleTo}
                    onChange={(e) => setScheduleTo(e.target.value)}
                    className={`w-full bg-zinc-950 border ${errors.scheduleTo ? 'border-red-500' : 'border-zinc-800'} text-zinc-200 focus:border-brand-orange/40 rounded-xl py-2 px-3 text-sm transition-colors focus:outline-none [color-scheme:dark]`}
                  />
                  {errors.scheduleTo && <p className="text-red-500 text-[10px] mt-1">{errors.scheduleTo}</p>}
                </div>
              </div>
              <p className="text-[10px] text-zinc-500 italic flex items-center gap-1 mt-1">
                <AlertCircle className="w-3 h-3" /> Tối đa 14 ngày mỗi lần tạo bản xem trước.
              </p>

              <div className="grid grid-cols-2 gap-3 mt-4">
                <div>
                  <label className="block text-xs font-semibold text-zinc-400 mb-1 flex items-center gap-1">
                    Khoảng cách thử lịch (phút)
                    <div className="group relative cursor-help">
                      <AlertCircle className="w-3 h-3 text-zinc-500" />
                      <div className="absolute left-1/2 -translate-x-1/2 bottom-full mb-1 hidden group-hover:block w-48 p-2 bg-zinc-800 text-[10px] text-zinc-300 rounded shadow-xl z-50 text-center">
                        Hệ thống sẽ thử các mốc bắt đầu cách nhau khoảng thời gian này (vd: 15 phút).
                      </div>
                    </div>
                  </label>
                  <input
                    type="number"
                    min={5} max={60} step={5}
                    value={slotGranularityMinutes}
                    onChange={(e) => setSlotGranularityMinutes(e.target.value)}
                    className={`w-full bg-zinc-950 border ${errors.slotGranularityMinutes ? 'border-red-500' : 'border-zinc-800'} text-zinc-200 focus:border-brand-orange/40 rounded-xl py-2 px-3 text-sm transition-colors focus:outline-none`}
                  />
                  {errors.slotGranularityMinutes && <p className="text-red-500 text-[10px] mt-1">{errors.slotGranularityMinutes}</p>}
                </div>
                <div>
                  <label className="block text-xs font-semibold text-zinc-400 mb-1">
                    Bản xem trước hết hạn sau (phút)
                  </label>
                  <input
                    type="number"
                    min={5} max={120} step={5}
                    value={previewTtlMinutes}
                    onChange={(e) => setPreviewTtlMinutes(e.target.value)}
                    className={`w-full bg-zinc-950 border ${errors.previewTtlMinutes ? 'border-red-500' : 'border-zinc-800'} text-zinc-200 focus:border-brand-orange/40 rounded-xl py-2 px-3 text-sm transition-colors focus:outline-none`}
                  />
                  {errors.previewTtlMinutes && <p className="text-red-500 text-[10px] mt-1">{errors.previewTtlMinutes}</p>}
                </div>
              </div>
            </div>
          </div>

          {/* Step 2: Auditoriums */}
          <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-5 space-y-4">
            <h2 className="text-sm font-black uppercase tracking-wider text-zinc-300 flex items-center justify-between border-b border-zinc-800 pb-3">
              <span className="flex items-center gap-2">
                <span className="bg-brand-orange text-zinc-950 w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-black">2</span>
                Phòng chiếu
              </span>
              <span className="text-[10px] bg-zinc-800 px-2 py-0.5 rounded text-zinc-400">
                Đã chọn: {selectedAuditoriumIds.length}/{auditoriums.length}
              </span>
            </h2>

            {isLoadingAuditoriums ? (
              <div className="flex justify-center py-4"><Loader2 className="w-5 h-5 animate-spin text-zinc-500" /></div>
            ) : !selectedCinemaId ? (
              <p className="text-zinc-500 text-xs italic text-center py-4">Vui lòng chọn cụm rạp trước</p>
            ) : auditoriums.length === 0 ? (
              <p className="text-zinc-500 text-xs text-center py-4">Không có phòng chiếu active</p>
            ) : (
              <div className="space-y-2 max-h-[300px] overflow-y-auto pr-2 custom-scrollbar">
                {auditoriums.map(aud => {
                  const isActive = aud.status === 'ACTIVE';
                  const isChecked = selectedAuditoriumIds.includes(aud.publicId);
                  
                  return (
                    <label 
                      key={aud.publicId} 
                      className={`flex items-start gap-3 p-3 rounded-xl border transition-all cursor-pointer ${
                        !isActive ? 'opacity-50 grayscale border-zinc-800/50 bg-zinc-950/50' : 
                        isChecked ? 'border-brand-orange/50 bg-brand-orange/5' : 'border-zinc-800/50 hover:bg-zinc-800/30'
                      }`}
                    >
                      <input 
                        type="checkbox"
                        checked={isChecked}
                        disabled={!isActive}
                        onChange={() => toggleAuditorium(aud.publicId)}
                        className="mt-1 flex-shrink-0 w-4 h-4 rounded border-zinc-700 text-brand-orange focus:ring-brand-orange focus:ring-offset-zinc-900 bg-zinc-950"
                      />
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between gap-2">
                          <span className="text-sm font-bold text-white truncate">{aud.name}</span>
                          {!isActive && (
                            <span className="text-[9px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded bg-red-500/20 text-red-400">
                              Bảo trì
                            </span>
                          )}
                        </div>
                        <div className="flex items-center gap-2 mt-1 text-[10px] text-zinc-400">
                          <span className="bg-zinc-800 px-1.5 rounded">{aud.screenType}</span>
                          <span className="bg-zinc-800 px-1.5 rounded">{aud.soundType}</span>
                          <span>• {aud.capacity} ghế</span>
                        </div>
                      </div>
                    </label>
                  );
                })}
                {errors.auditoriums && <p className="text-red-500 text-[10px] mt-2">{errors.auditoriums}</p>}
              </div>
            )}
          </div>
        </div>

        {/* Right Column (Step 3) */}
        <div className="lg:col-span-2 space-y-6">
          
          <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-5 space-y-4 h-full flex flex-col">
            <h2 className="text-sm font-black uppercase tracking-wider text-zinc-300 flex items-center justify-between border-b border-zinc-800 pb-3">
              <span className="flex items-center gap-2">
                <span className="bg-brand-orange text-zinc-950 w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-black">3</span>
                Phim & Định dạng
              </span>
              <span className="text-[10px] bg-zinc-800 px-2 py-0.5 rounded text-zinc-400">
                Đã chọn: {selectedMovieVersionIds.length} định dạng
              </span>
            </h2>

            <div className="relative">
              <Search className="w-4 h-4 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Tìm phim..."
                value={movieSearch}
                onChange={(e) => setMovieSearch(e.target.value)}
                className="w-full bg-zinc-950 border border-zinc-800 text-zinc-200 focus:border-brand-orange/40 rounded-xl py-2 pl-9 pr-3 text-sm transition-colors focus:outline-none"
              />
            </div>

            {errors.versions && <p className="text-red-500 text-[10px] bg-red-500/10 p-2 rounded border border-red-500/20">{errors.versions}</p>}

            <div className="flex-1 overflow-y-auto pr-2 custom-scrollbar space-y-3">
              {isLoadingMovies ? (
                <div className="flex justify-center py-8"><Loader2 className="w-6 h-6 animate-spin text-zinc-500" /></div>
          ) : filteredMovies.length === 0 ? (
                <p className="text-zinc-500 text-sm text-center py-8">Không tìm thấy phim phù hợp.</p>
              ) : (
                filteredMovies.map(movie => {
                  const isExpanded = !!expandedMovies[movie.publicId];
                  const versions = versionsByMovie[movie.publicId] || [];
                  
                  // Count selected versions for this movie
                  const selectedCount = versions.filter(v => selectedMovieVersionIds.includes(v.publicId)).length;
                  
                  const isDisabled = !movie.eligible;
                  const disableReasons = movie.reasons || [];

                  return (
                    <div key={movie.publicId} className={`border border-zinc-800/70 rounded-xl overflow-hidden bg-zinc-950/50 ${isDisabled ? 'opacity-60 grayscale' : ''}`}>
                      
                      {/* Movie Header (Accordion toggle) */}
                      <div 
                        className={`p-3 md:p-4 flex flex-col md:flex-row md:items-center justify-between gap-3 transition-colors ${isDisabled ? 'cursor-not-allowed' : 'cursor-pointer hover:bg-zinc-800/30'} ${isExpanded ? 'bg-zinc-800/20' : ''}`}
                        onClick={() => handleToggleMovie(movie.publicId, isDisabled)}
                      >
                        <div className="flex items-center gap-3 min-w-0">
                          <div className={`w-8 h-8 rounded bg-zinc-800 flex items-center justify-center flex-shrink-0 transition-transform ${isExpanded ? 'rotate-90' : ''}`}>
                            <ChevronRight className="w-4 h-4 text-zinc-400" />
                          </div>
                          <div className="min-w-0">
                            <h3 className="font-bold text-white text-sm truncate flex items-center gap-2 flex-wrap">
                              {movie.title}
                              {isDisabled && disableReasons.length > 0 && (
                                <div className="flex gap-1 flex-wrap items-center">
                                  {disableReasons.map((r, i) => (
                                    <span key={i} title={r.code} className="text-[9px] px-1.5 py-0.5 rounded font-black bg-red-500/10 text-red-400 border border-red-500/20 whitespace-nowrap">
                                      {r.message.toUpperCase()}
                                    </span>
                                  ))}
                                </div>
                              )}
                            </h3>
                            <div className="flex items-center gap-2 mt-1 text-[10px] text-zinc-500">
                              <span className="flex items-center gap-1"><CalendarDays className="w-3 h-3" /> {movie.releaseDate || 'N/A'}</span>
                              <span>• {movie.durationMinutes} phút</span>
                              {selectedCount > 0 && (
                                <span className="bg-brand-orange/20 text-brand-orange font-bold px-1.5 rounded ml-2">
                                  {selectedCount} đã chọn
                                </span>
                              )}
                            </div>
                          </div>
                        </div>
                        <div className="flex items-center">
                          <span className={`text-[10px] px-2 py-0.5 rounded font-black tracking-wider ${
                            movie.status === 'NOW_SHOWING' ? 'bg-green-500/10 text-green-400 border border-green-500/20' : 
                            'bg-blue-500/10 text-blue-400 border border-blue-500/20'
                          }`}>
                            {movie.status.replace('_', ' ')}
                          </span>
                        </div>
                      </div>

                      {/* Versions Content */}
                      {isExpanded && (
                        <div className="border-t border-zinc-800/50 bg-zinc-900/30 p-3 md:p-4">
                          {versions.length === 0 ? (
                            <p className="text-zinc-500 text-xs text-center py-2">Không có định dạng nào</p>
                          ) : (
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                              {versions.map(version => {
                                const isActive = version.status === 'ACTIVE';
                                const isChecked = selectedMovieVersionIds.includes(version.publicId);
                                
                                return (
                                  <label 
                                    key={version.publicId}
                                    className={`flex items-start gap-3 p-3 rounded-xl border transition-all cursor-pointer ${
                                      !isActive ? 'opacity-50 grayscale border-zinc-800/50 bg-zinc-950/50' : 
                                      isChecked ? 'border-brand-orange/50 bg-brand-orange/5' : 'border-zinc-800/50 hover:bg-zinc-800/50 bg-zinc-950/80'
                                    }`}
                                  >
                                    <input 
                                      type="checkbox"
                                      checked={isChecked}
                                      disabled={!isActive}
                                      onChange={() => toggleVersion(version.publicId)}
                                      className="mt-0.5 flex-shrink-0 w-4 h-4 rounded border-zinc-700 text-brand-orange focus:ring-brand-orange focus:ring-offset-zinc-900 bg-zinc-950"
                                    />
                                    <div className="flex-1 min-w-0">
                                      <div className="flex items-center justify-between gap-2">
                                        <span className="text-sm font-bold text-zinc-200 truncate">{version.versionName}</span>
                                        {!isActive && (
                                          <span className="text-[9px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded bg-red-500/20 text-red-400">
                                            Ngừng
                                          </span>
                                        )}
                                      </div>
                                      <div className="flex flex-wrap items-center gap-1.5 mt-1.5 text-[10px] text-zinc-400">
                                        <span className="bg-zinc-800/80 px-1.5 py-0.5 rounded border border-zinc-700/50 font-medium">{version.format}</span>
                                        <span className="bg-zinc-800/80 px-1.5 py-0.5 rounded border border-zinc-700/50 flex items-center gap-1">
                                          <span className="text-zinc-500">Audio:</span> {version.audioLanguage}
                                        </span>
                                        {version.subtitleLanguage && (
                                          <span className="bg-zinc-800/80 px-1.5 py-0.5 rounded border border-zinc-700/50 flex items-center gap-1">
                                            <span className="text-zinc-500">Sub:</span> {version.subtitleLanguage}
                                          </span>
                                        )}
                                      </div>
                                    </div>
                                  </label>
                                );
                              })}
                            </div>
                          )}
                        </div>
                      )}
                      
                    </div>
                  );
                })
              )}
            </div>

            <div className="pt-4 border-t border-zinc-800 mt-2 flex items-start gap-3">
              <AlertCircle className="w-5 h-5 text-amber-500 flex-shrink-0 mt-0.5" />
              <p className="text-[10px] text-zinc-400 leading-relaxed">
                Hệ thống sẽ thử phân bổ <strong className="text-zinc-200">{selectedMovieVersionIds.length} định dạng phim</strong> vào <strong className="text-zinc-200">{selectedAuditoriumIds.length} phòng chiếu</strong> được chọn, trong khoảng thời gian từ <strong className="text-zinc-200">{scheduleFrom || '?'}</strong> đến <strong className="text-zinc-200">{scheduleTo || '?'}</strong>. Quá trình này không ghi trực tiếp vào cơ sở dữ liệu mà tạo ra một <strong>Bản xem trước (Preview)</strong> có hiệu lực trong {previewTtlMinutes} phút.
              </p>
            </div>

          </div>
        </div>

      </div>
    </div>
  );
};

export default AdminAutoScheduleCreatePage;
