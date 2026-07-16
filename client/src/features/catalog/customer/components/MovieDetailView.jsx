import React, { useState, useEffect } from 'react';
import { ArrowLeft, Pencil, Users, Building2, ChevronLeft, ChevronRight, Clock, Calendar, Globe, Film, Play, Info } from 'lucide-react';
import { LazyImage } from '@/components/common/ui/uiKit';
import { getYoutubeEmbedUrl, getYoutubeId, formatDate, DEFAULT_AVATAR, STATUS_LABELS, STATUS_COLORS } from '@/utils/movieHelpers';

export default function MovieDetailView({ movie, onClose, onOpenEdit }) {
  const [activeBannerIdx, setActiveBannerIdx] = useState(0);
  const [playDetailTrailer, setPlayDetailTrailer] = useState(false);

  const banners = movie.media ? movie.media.filter(m => m.mediaType === 'BANNER').map(m => m.url) : [];
  const trailerMedia = movie.media ? movie.media.find(m => m.mediaType === 'TRAILER') : null;
  const embedTrailerUrl = getYoutubeEmbedUrl(trailerMedia?.url);

  // Auto-switch banner every 10 seconds
  useEffect(() => {
    if (banners.length <= 1) return;

    const timer = setInterval(() => {
      setActiveBannerIdx(prev => (prev === banners.length - 1 ? 0 : prev + 1));
    }, 10000);

    return () => clearInterval(timer);
  }, [banners.length]);

  // Reset trailer playing status when movie details change
  useEffect(() => {
    setPlayDetailTrailer(false);
  }, [movie]);

  return (
    <div className="flex flex-col flex-1 overflow-auto bg-zinc-950 text-white animate-fade-in pb-12">
      {/* Wide Header Backdrop Banner Section with Crossfade Carousel */}
      <div className="relative w-full h-[320px] md:h-[420px] flex-shrink-0 overflow-hidden border-b border-zinc-900 bg-black group">
        {/* Backdrop Images with crossfade */}
        {banners.length > 0 ? (
          banners.map((url, idx) => (
            <img
              key={idx}
              src={url}
              alt=""
              className={`absolute inset-0 w-full h-full object-cover transition-opacity duration-1000 ease-in-out ${idx === activeBannerIdx ? 'opacity-50' : 'opacity-0 pointer-events-none'}`}
            />
          ))
        ) : movie.primaryPoster ? (
          <img
            src={movie.primaryPoster}
            alt=""
            className="absolute inset-0 w-full h-full object-cover opacity-40 blur-sm"
          />
        ) : (
          <div className="absolute inset-0 bg-gradient-to-t from-zinc-950 to-zinc-900" />
        )}

        {/* Gradients */}
        <div className="absolute inset-0 bg-gradient-to-t from-zinc-950 via-zinc-950/60 to-transparent" />
        <div className="absolute inset-0 bg-gradient-to-r from-zinc-950/50 via-transparent to-zinc-950/50" />

        {/* Navigation & Actions Top Bar */}
        <div className="absolute top-0 left-0 right-0 p-6 flex justify-between items-center z-20">
          <button
            onClick={onClose}
            className="flex items-center gap-2 text-xs font-semibold px-4 py-2.5 rounded-xl bg-zinc-950/80 hover:bg-zinc-900 border border-zinc-800 text-zinc-300 hover:text-white transition-all cursor-pointer shadow-lg backdrop-blur-md"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>DANH SÁCH</span>
          </button>
          <button
            onClick={() => onOpenEdit(movie)}
            className="flex items-center gap-2 text-xs font-bold bg-[#ff7a1a] hover:bg-orange-600 text-zinc-950 px-5 py-2.5 rounded-xl transition-all cursor-pointer shadow-lg hover:shadow-[#ff7a1a]/20"
          >
            <Pencil className="w-3.5 h-3.5" />
            <span>CHỈNH SỬA PHIM</span>
          </button>
        </div>

        {/* Hover Arrow buttons for carousel */}
        {banners.length > 1 && (
          <>
            <button
              type="button"
              onClick={() => setActiveBannerIdx(prev => (prev === 0 ? banners.length - 1 : prev - 1))}
              className="absolute left-6 top-1/2 -translate-y-1/2 p-3 rounded-full bg-black/60 border border-zinc-800 text-zinc-300 hover:text-white transition-all duration-300 opacity-0 group-hover:opacity-100 cursor-pointer hover:scale-110 z-20"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
            <button
              type="button"
              onClick={() => setActiveBannerIdx(prev => (prev === banners.length - 1 ? 0 : prev + 1))}
              className="absolute right-6 top-1/2 -translate-y-1/2 p-3 rounded-full bg-black/60 border border-zinc-800 text-zinc-300 hover:text-white transition-all duration-300 opacity-0 group-hover:opacity-100 cursor-pointer hover:scale-110 z-20"
            >
              <ChevronRight className="w-5 h-5" />
            </button>
          </>
        )}
      </div>

      {/* Main Redesigned Layout Details Grid */}
      <div className="px-6 md:px-8 -mt-20 md:-mt-28 relative z-10 space-y-8">
        {/* Top Hero Card Panel */}
        <div className="bg-zinc-900/60 border border-zinc-800/80 backdrop-blur-md rounded-3xl p-6 md:p-8 flex flex-col md:flex-row gap-6 md:gap-8 items-start shadow-2xl">
          {/* Left: Poster */}
          <div className="w-48 md:w-56 h-72 md:h-80 bg-neutral-950 rounded-2xl overflow-hidden shadow-2xl border border-zinc-800 flex-shrink-0 relative group">
            {movie.primaryPoster ? (
              <LazyImage
                src={movie.primaryPoster}
                alt={movie.title}
                containerClassName="absolute inset-0 w-full h-full border-none rounded-none"
                className="transition-transform duration-500 group-hover:scale-105"
              />
            ) : (
              <div className="w-full h-full flex flex-col items-center justify-center text-zinc-700">
                <ImageIcon className="w-10 h-10 mb-2" />
                <span className="text-[9px] uppercase tracking-wider font-bold">Chưa có poster</span>
              </div>
            )}
            {movie.ageRating && (
              <div className="absolute top-4 left-4 bg-[#ff7a1a] text-zinc-950 font-black px-2.5 py-1 rounded-lg text-[10px] shadow-lg border border-[#ff7a1a]/30">
                {movie.ageRating}
              </div>
            )}
          </div>

          {/* Right: Titles, Genres, Synopsis */}
          <div className="flex-grow space-y-4">
            <div className="space-y-1.5">
              <h2 className="text-2xl md:text-3xl lg:text-4xl font-black tracking-tight text-white">{movie.title}</h2>
              {movie.originalTitle && (
                <p className="text-sm text-zinc-400 italic font-medium">
                  Tên gốc: <span className="text-zinc-300 font-bold">{movie.originalTitle}</span>
                </p>
              )}
            </div>

            {/* Genres Pills */}
            {Array.isArray(movie.genres) && movie.genres.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {movie.genres.map(g => (
                  <span
                    key={g}
                    className="bg-zinc-950 border border-zinc-800 text-zinc-300 text-[10px] px-3.5 py-1.5 rounded-full font-bold hover:border-zinc-700 transition-colors uppercase tracking-wider"
                  >
                    {g}
                  </span>
                ))}
              </div>
            )}

            {/* Synopsis */}
            <div className="space-y-1.5">
              <h4 className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">Nội dung tóm tắt</h4>
              <p className="text-zinc-300 text-xs md:text-sm leading-relaxed font-light">{movie.synopsis || 'Chưa có tóm tắt nội dung phim.'}</p>
            </div>
          </div>
        </div>

        {/* Bottom Grid: 2 Columns */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Column 1: Cast & Crew & Production Companies (lg:col-span-2) */}
          <div className="lg:col-span-2 space-y-6">
            {/* Cast & Crew Card */}
            {((movie.directors && movie.directors.length > 0) ||
              (movie.actors && movie.actors.length > 0) ||
              (movie.writers && movie.writers.length > 0) ||
              (movie.producers && movie.producers.length > 0)) && (
                <div className="bg-zinc-900/40 border border-zinc-800/80 p-6 rounded-3xl space-y-6 shadow-xl">
                  <h4 className="text-xs font-black text-white uppercase tracking-wider flex items-center gap-2 border-b border-zinc-800 pb-3">
                    <Users className="w-4.5 h-4.5 text-[#ff7a1a]" />
                    <span>Đoàn làm phim</span>
                  </h4>

                  {/* Directors */}
                  {movie.directors && movie.directors.length > 0 && (
                    <div className="space-y-2.5">
                      <p className="text-[10px] font-black text-zinc-500 uppercase tracking-wider">Đạo diễn</p>
                      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                        {movie.directors.map((d, dIdx) => (
                          <div key={d.publicId || dIdx} className="flex items-center gap-3 bg-zinc-900/60 border border-zinc-800/60 rounded-2xl px-4 py-2.5 hover:border-zinc-700/40 transition-colors">
                            <LazyImage
                              src={d.profileImageUrl || DEFAULT_AVATAR}
                              alt={d.fullName}
                              containerClassName="w-9 h-9 rounded-full border border-zinc-800 flex-shrink-0"
                              className="rounded-full object-cover"
                            />
                            <div className="min-w-0">
                              <p className="text-xs font-bold text-zinc-200 truncate">{d.fullName}</p>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Writers */}
                  {movie.writers && movie.writers.length > 0 && (
                    <div className="space-y-2.5">
                      <p className="text-[10px] font-black text-zinc-500 uppercase tracking-wider">Biên kịch</p>
                      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                        {movie.writers.map((w, wIdx) => (
                          <div key={w.publicId || wIdx} className="flex items-center gap-3 bg-zinc-900/60 border border-zinc-800/60 rounded-2xl px-4 py-2.5 hover:border-zinc-700/40 transition-colors">
                            <LazyImage
                              src={w.profileImageUrl || DEFAULT_AVATAR}
                              alt={w.fullName}
                              containerClassName="w-9 h-9 rounded-full border border-zinc-800 flex-shrink-0"
                              className="rounded-full object-cover"
                            />
                            <div className="min-w-0">
                              <p className="text-xs font-bold text-zinc-200 truncate">{w.fullName}</p>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Producers */}
                  {movie.producers && movie.producers.length > 0 && (
                    <div className="space-y-2.5">
                      <p className="text-[10px] font-black text-zinc-500 uppercase tracking-wider">Nhà sản xuất</p>
                      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                        {movie.producers.map((p, pIdx) => (
                          <div key={p.publicId || pIdx} className="flex items-center gap-3 bg-zinc-900/60 border border-zinc-800/60 rounded-2xl px-4 py-2.5 hover:border-zinc-700/40 transition-colors">
                            <LazyImage
                              src={p.profileImageUrl || DEFAULT_AVATAR}
                              alt={p.fullName}
                              containerClassName="w-9 h-9 rounded-full border border-zinc-800 flex-shrink-0"
                              className="rounded-full object-cover"
                            />
                            <div className="min-w-0">
                              <p className="text-xs font-bold text-zinc-200 truncate">{p.fullName}</p>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Actors */}
                  {movie.actors && movie.actors.length > 0 && (
                    <div className="space-y-2.5">
                      <p className="text-[10px] font-black text-zinc-500 uppercase tracking-wider">Diễn viên chính</p>
                      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                        {movie.actors.map((a, aIdx) => (
                          <div key={a.publicId || aIdx} className="flex items-center gap-3 bg-zinc-900/60 border border-zinc-800/60 rounded-2xl px-4 py-2.5 hover:border-zinc-700/40 transition-colors">
                            <LazyImage
                              src={a.profileImageUrl || DEFAULT_AVATAR}
                              alt={a.fullName}
                              containerClassName="w-9 h-9 rounded-full border border-zinc-800 flex-shrink-0"
                              className="rounded-full object-cover"
                            />
                            <div className="min-w-0">
                              <p className="text-xs font-bold text-zinc-200 truncate">{a.fullName}</p>
                              {a.characterName && <p className="text-[9px] text-zinc-500 truncate mt-0.5">{a.characterName}</p>}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}

            {/* Production Companies Card */}
            {((movie.productionCompanies && movie.productionCompanies.length > 0) ||
              (movie.studios && movie.studios.length > 0)) && (
                <div className="bg-zinc-900/40 border border-zinc-800/80 p-6 rounded-3xl space-y-4 shadow-xl">
                  <h4 className="text-xs font-black text-white uppercase tracking-wider flex items-center gap-2 border-b border-zinc-800 pb-3">
                    <Building2 className="w-4.5 h-4.5 text-[#ff7a1a]" />
                    <span>Hãng sản xuất</span>
                  </h4>
                  <div className="flex flex-wrap gap-3">
                    {(movie.productionCompanies && movie.productionCompanies.length > 0 ? movie.productionCompanies : movie.studios).map((c, cIdx) => (
                      <div key={c.publicId || cIdx} className="flex items-center gap-3 bg-zinc-900/60 border border-zinc-800/60 rounded-2xl px-4 py-2.5 hover:border-zinc-700/40 transition-colors">
                        {c.logoUrl && (
                          <div className="h-6 max-w-[90px] flex-shrink-0">
                            <LazyImage
                              src={c.logoUrl}
                              alt={c.name}
                              containerClassName="h-6 max-w-[90px] border-none rounded-none bg-transparent"
                              className="object-contain filter brightness-95"
                            />
                          </div>
                        )}
                        <span className="text-xs font-semibold text-zinc-200">{c.name}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
          </div>

          {/* Column 2: Technical Info & Release Versions & Trailer (lg:col-span-1) */}
          <div className="space-y-6">
            {/* Technical Metadata Card */}
            <div className="bg-zinc-900/40 border border-zinc-800/80 rounded-3xl p-5 space-y-4 shadow-xl">
              <h3 className="text-[10px] font-black text-zinc-500 uppercase tracking-widest flex items-center gap-2 border-b border-zinc-800 pb-2.5">
                <Info className="w-4 h-4 text-[#ff7a1a]" />
                <span>Thông tin chi tiết</span>
              </h3>

              <div className="space-y-3 text-xs">
                <div className="flex justify-between items-center border-b border-zinc-800/60 pb-2.5">
                  <span className="text-zinc-500 font-bold uppercase tracking-wider flex items-center gap-1.5"><Clock className="w-3.5 h-3.5" /> Thời lượng</span>
                  <span className="text-zinc-200 font-semibold">{movie.durationMinutes ? `${movie.durationMinutes} phút` : 'N/A'}</span>
                </div>

                <div className="flex justify-between items-center border-b border-zinc-800/60 pb-2.5">
                  <span className="text-zinc-500 font-bold uppercase tracking-wider flex items-center gap-1.5"><Calendar className="w-3.5 h-3.5" /> Khởi chiếu</span>
                  <span className="text-zinc-200 font-semibold">{formatDate(movie.releaseDate)}</span>
                </div>

                {movie.endDate && (
                  <div className="flex justify-between items-center border-b border-zinc-800/60 pb-2.5">
                    <span className="text-zinc-500 font-bold uppercase tracking-wider flex items-center gap-1.5"><Calendar className="w-3.5 h-3.5" /> Kết thúc</span>
                    <span className="text-zinc-200 font-semibold">{formatDate(movie.endDate)}</span>
                  </div>
                )}

                <div className="flex justify-between items-center border-b border-zinc-800/60 pb-2.5">
                  <span className="text-zinc-500 font-bold uppercase tracking-wider flex items-center gap-1.5"><Globe className="w-3.5 h-3.5" /> Quốc gia</span>
                  <span className="text-zinc-200 font-semibold">{movie.country || 'N/A'}</span>
                </div>

                <div className="flex justify-between items-center pt-1">
                  <span className="text-zinc-500 font-bold uppercase tracking-wider flex items-center gap-1.5"><Film className="w-3.5 h-3.5" /> Trạng thái</span>
                  <span className={`text-[9px] font-black px-2 py-0.5 rounded border uppercase shadow-sm ${STATUS_COLORS[movie.status] || ''}`}>
                    {STATUS_LABELS[movie.status] || movie.status}
                  </span>
                </div>
              </div>
            </div>

            {/* Movie Screening Versions Card */}
            {movie.versions && movie.versions.length > 0 && (
              <div className="bg-zinc-900/40 border border-zinc-800/80 rounded-3xl p-5 space-y-4 shadow-xl">
                <h3 className="text-[10px] font-black text-zinc-500 uppercase tracking-widest flex items-center gap-2 border-b border-zinc-800 pb-2.5">
                  <Film className="w-4 h-4 text-[#ff7a1a]" />
                  <span>Phiên bản phát hành</span>
                </h3>
                <div className="grid grid-cols-1 gap-2.5">
                  {movie.versions.map((ver, idx) => (
                    <div key={ver.publicId || idx} className="bg-zinc-950 border border-zinc-900 px-3.5 py-2.5 rounded-2xl flex flex-col gap-1.5">
                      <div className="flex justify-between items-center">
                        <span className="text-xs font-bold text-zinc-200">{ver.versionName}</span>
                        <span className="text-[9px] bg-[#ff7a1a]/10 border border-[#ff7a1a]/20 text-[#ff7a1a] px-1.5 py-0.5 rounded font-black">{ver.format}</span>
                      </div>
                      <div className="flex gap-2 text-[9px] text-zinc-400">
                        <span>Âm thanh: <strong className="text-zinc-300">{ver.audioLanguage}</strong></span>
                        <span>•</span>
                        {ver.subtitleLanguage && <span>Phụ đề: <strong className="text-zinc-300">{ver.subtitleLanguage}</strong></span>}
                        {ver.dubLanguage && ver.dubLanguage !== 'NONE' && (
                          <>
                            <span>•</span>
                            <span>Lồng tiếng: <strong className="text-zinc-300">{ver.dubLanguage}</strong></span>
                          </>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Official Trailer Card */}
            {embedTrailerUrl && (
              <div className="bg-zinc-900/40 border border-zinc-800/80 p-5 rounded-3xl space-y-3.5 shadow-xl">
                <h4 className="text-[10px] font-black text-zinc-500 uppercase tracking-widest flex items-center gap-2 border-b border-zinc-800 pb-2.5">
                  <Play className="w-4 h-4 text-[#ff7a1a] animate-pulse" />
                  <span>Trailer phim</span>
                </h4>
                <div className="relative w-full aspect-video rounded-2xl overflow-hidden border border-zinc-800 shadow-2xl bg-black group/trailer-detail">
                  {playDetailTrailer ? (
                    <iframe
                      src={`${embedTrailerUrl}?autoplay=1`}
                      title={`${movie.title} Official Trailer`}
                      className="absolute inset-0 w-full h-full border-none"
                      allow="autoplay; accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                      allowFullScreen
                    />
                  ) : (
                    <div 
                      onClick={() => setPlayDetailTrailer(true)}
                      className="absolute inset-0 cursor-pointer flex items-center justify-center select-none group/play"
                    >
                      <img 
                        src={`https://img.youtube.com/vi/${getYoutubeId(trailerMedia?.url)}/hqdefault.jpg`} 
                        alt={`${movie.title} Trailer preview`} 
                        className="w-full h-full object-cover opacity-70 group-hover/play:opacity-90 transition-opacity duration-300"
                        onError={(e) => {
                          e.target.src = "https://img.youtube.com/vi/" + getYoutubeId(trailerMedia?.url) + "/0.jpg";
                        }}
                      />
                      <div className="absolute inset-0 bg-black/20 group-hover/play:bg-black/10 transition-colors duration-300" />
                      <div className="absolute w-14 h-14 rounded-full bg-[#ff7a1a]/90 group-hover/play:bg-[#ff7a1a] text-zinc-950 flex items-center justify-center shadow-2xl transition-all duration-300 group-hover/play:scale-110">
                        <Play className="w-6 h-6 fill-current ml-1" />
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
