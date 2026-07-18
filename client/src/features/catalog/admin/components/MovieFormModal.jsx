// eslint-disable-next-line no-unused-vars
import React, { useState, useEffect } from 'react';
// eslint-disable-next-line no-unused-vars
import { ArrowLeft, Check, Plus, Search, ChevronLeft, ChevronRight, Image as ImageIcon, Play, X, Building2, Users, GripVertical, Trash2, Film, Info, LayoutList } from 'lucide-react';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import adminGenreService from '@/features/catalog/admin/services/adminGenreService';
import { LazyImage, Field, Input, Select, Textarea } from '@/components/common/ui/uiKit';
import { parseApiError } from '@/utils/apiErrorHandler';
import {
  DEFAULT_AVATAR,
  AGE_RATINGS,
  AGE_RATING_LABELS,
  STATUS_LABELS,
  // eslint-disable-next-line no-unused-vars
  STATUS_COLORS,
  FORMAT_MAP_TO_API,
  FORMAT_MAP_FROM_API,
  getYoutubeEmbedUrl,
  getYoutubeId,
  getTodayString,
  // eslint-disable-next-line no-unused-vars
  formatDate
} from '@/utils/movieHelpers';

const emptyForm = () => ({
  title: '',
  originalTitle: '',
  durationMinutes: '',
  ageRating: 'P',
  showingStartDate: '',
  endDate: '',
  country: '',
  synopsis: '',
  tmdbReleaseDate: '',
  originalLanguage: '',
  status: 'DRAFT',
});

export default function MovieFormModal({ selectedMovie, genresList, setGenresList, triggerToast, onClose, onRefreshList }) {
  const isEdit = !!selectedMovie;
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [formErrors, setFormErrors] = useState({});
  const [activeFormTab, setActiveFormTab] = useState('basic'); // 'basic' | 'crew' | 'media'

  // Form field states
  const [formBasic, setFormBasic] = useState(emptyForm());
  const [selectedGenres, setSelectedGenres] = useState([]); // [{publicId, name}]
  const [tmdbGenres, setTmdbGenres] = useState([]);
  const [availableBackdrops, setAvailableBackdrops] = useState([]);
  const [backdropImportCount, setBackdropImportCount] = useState(0);

  const [posterUrl, setPosterUrl] = useState('');
  const [bannerUrls, setBannerUrls] = useState(['']); // Array supports multiple banners
  const [trailerUrl, setTrailerUrl] = useState('');

  const [playTrailer, setPlayTrailer] = useState(false);

  // Extra TMDB-imported data (credits & companies)
  const [cast, setCast] = useState([]); // [{name, character, profileUrl}]
  const [directors, setDirectors] = useState([]);
  const [writers, setWriters] = useState([]);
  const [producers, setProducers] = useState([]);
  const [studios, setStudios] = useState([]); // [{name, logoUrl}]

  // Versions & Media state to reconcile on edit
  const [versions, setVersions] = useState([]);
  const [origVersions, setOrigVersions] = useState([]);
  const [origMedia, setOrigMedia] = useState({ poster: null, banners: [], trailer: null });

  // Drag-and-drop state
  const [draggedIdx, setDraggedIdx] = useState(null);
  const [draggedType, setDraggedType] = useState(null); // 'directors' | 'writers' | 'producers' | 'cast' | 'studios' | 'banners'

  // Load edit details or reset form
  useEffect(() => {
    const loadMovieData = async () => {
      if (!selectedMovie) {
        setFormBasic(emptyForm());
        setSelectedGenres([]);
        setTmdbGenres([]);
        setPosterUrl('');
        setBannerUrls(['']);
        setTrailerUrl('');
        setCast([]);
        setDirectors([]);
        setWriters([]);
        setProducers([]);
        setStudios([]);
        setVersions([]);
        setOrigVersions([]);
        setOrigMedia({ poster: null, banners: [], trailer: null });
        setFormErrors({});
        setAvailableBackdrops([]);
        setBackdropImportCount(0);
        return;
      }

      setIsLoading(true);
      try {
        const [detailRes, mediaRes, verRes] = await Promise.all([
          adminMovieService.getMovieById(selectedMovie.publicId),
          adminMovieService.getMovieMedia(selectedMovie.publicId),
          adminMovieService.getMovieVersions(selectedMovie.publicId),
        ]);

        if (!detailRes?.success || !detailRes?.data) {
          triggerToast?.('Không lấy được chi tiết phim', 'error');
          onClose();
          return;
        }
        const fullMovie = detailRes.data;

        const mediaList = mediaRes?.data || [];
        const versionList = verRes?.data || [];

        const poster = mediaList.find(m => m.mediaType === 'POSTER' && m.isPrimary) || mediaList.find(m => m.mediaType === 'POSTER') || null;
        const banners = mediaList.filter(m => m.mediaType === 'BANNER');
        const trailer = mediaList.find(m => m.mediaType === 'TRAILER') || null;

        setOrigMedia({ poster, banners, trailer });
        setPosterUrl(poster?.url || '');
        setBannerUrls(banners.length ? banners.map(b => b.url) : ['']);
        setTrailerUrl(trailer?.url || '');

        setVersions(versionList.map(v => ({
          ...v,
          format: FORMAT_MAP_FROM_API[v.format] || v.format
        })));
        setOrigVersions(versionList.map(v => ({
          ...v,
          format: FORMAT_MAP_FROM_API[v.format] || v.format
        })));

        const movieGenreIds = [];
        if (Array.isArray(selectedMovie.genres)) {
          selectedMovie.genres.forEach(gName => {
            const match = genresList.find(g => g.name?.toLowerCase() === gName.toLowerCase());
            if (match) movieGenreIds.push({ publicId: match.publicId, name: match.name });
          });
        }
        setSelectedGenres(movieGenreIds);
        setTmdbGenres([]);

        setCast(fullMovie.actors ? fullMovie.actors.map(a => ({
          name: a.fullName,
          character: a.characterName,
          profileUrl: a.profileImageUrl || ''
        })) : []);
        setDirectors(fullMovie.directors ? fullMovie.directors.map(d => ({
          name: d.fullName
        })) : []);
        setWriters(fullMovie.writers ? fullMovie.writers.map(w => ({
          name: w.fullName
        })) : []);
        setProducers(fullMovie.producers ? fullMovie.producers.map(p => ({
          name: p.fullName
        })) : []);
        setStudios(fullMovie.productionCompanies ? fullMovie.productionCompanies.map(s => ({
          name: s.name,
          logoUrl: s.logoUrl || ''
        })) : []);

        setFormBasic({
          title: selectedMovie.title || '',
          originalTitle: selectedMovie.originalTitle || '',
          durationMinutes: selectedMovie.durationMinutes || '',
          ageRating: selectedMovie.ageRating || 'P',
          showingStartDate: selectedMovie.releaseDate || '',
          endDate: selectedMovie.endDate || '',
          country: selectedMovie.country || '',
          synopsis: selectedMovie.synopsis || '',
          tmdbReleaseDate: '',
          originalLanguage: '',
          status: selectedMovie.status || 'UPCOMING',
        });
        setAvailableBackdrops([]);
        setBackdropImportCount(0);
        setFormErrors({});
      } catch (err) {
        triggerToast?.(parseApiError(err), 'error');
      } finally {
        setIsLoading(false);
      }
    };

    loadMovieData();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedMovie, genresList, triggerToast]);

  // Reset playTrailer when trailer changes
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setPlayTrailer(false);
  }, [trailerUrl]);

  const handleCleanForm = () => {
    setFormBasic(emptyForm());
    setSelectedGenres([]);
    setTmdbGenres([]);
    setPosterUrl('');
    setBannerUrls(['']);
    setTrailerUrl('');
    setCast([]);
    setDirectors([]);
    setWriters([]);
    setProducers([]);
    setStudios([]);
    setVersions([]);
    setOrigVersions([]);
    setOrigMedia({ poster: null, banners: [], trailer: null });
    setFormErrors({});
    setAvailableBackdrops([]);
    setBackdropImportCount(0);
    triggerToast?.('Đã dọn sạch thông tin biểu mẫu phim!', 'info');
  };

  // Drag and drop handlers
  const handleDragStart = (idx, type) => {
    setDraggedIdx(idx);
    setDraggedType(type);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
  };

  const handleDragEnter = (idx, type, items, setItems) => {
    if (draggedType !== type || draggedIdx === null || draggedIdx === idx) return;
    const newItems = [...items];
    const draggedItem = newItems[draggedIdx];
    newItems.splice(draggedIdx, 1);
    newItems.splice(idx, 0, draggedItem);
    setDraggedIdx(idx);
    setItems(newItems);
  };

  const handleDragEnd = () => {
    setDraggedIdx(null);
    setDraggedType(null);
  };

  // Form Section Helpers
  const addVersion = () => setVersions(v => [...v, {
    versionName: '', format: '2D', audioLanguage: 'EN', subtitleLanguage: 'VI', dubLanguage: 'NONE', status: 'ACTIVE',
  }]);
  const updateVersion = (i, field, val) => setVersions(v => v.map((ver, idx) => idx === i ? { ...ver, [field]: val } : ver));
  const removeVersion = (i) => setVersions(v => v.filter((_, idx) => idx !== i));

  const addBanner = () => setBannerUrls(b => [...b, '']);
  const updateBanner = (i, val) => setBannerUrls(b => b.map((url, idx) => idx === i ? val : url));
  const removeBanner = (i) => setBannerUrls(b => b.filter((_, idx) => idx !== i));

  const toggleGenre = (g) => {
    setSelectedGenres(prev => {
      const exists = prev.some(s => s.publicId === g.publicId);
      return exists ? prev.filter(s => s.publicId !== g.publicId) : [...prev, g];
    });
  };

  // Save implementation
  const validateForm = () => {
    const errs = {};
    const todayStr = getTodayString();
    const todayDate = new Date(todayStr);

    if (!formBasic.title.trim()) errs.title = 'Tên phim không được để trống.';
    if (!formBasic.durationMinutes || Number(formBasic.durationMinutes) <= 0)
      errs.durationMinutes = 'Thời lượng phải là số dương.';
    if (!formBasic.ageRating || !AGE_RATINGS.includes(formBasic.ageRating))
      errs.ageRating = `Độ tuổi phải là một trong: ${AGE_RATINGS.join(', ')}.`;

    if (!formBasic.showingStartDate) {
      errs.showingStartDate = 'Ngày khởi chiếu bắt buộc phải chọn.';
    }
    if (formBasic.endDate && new Date(formBasic.endDate) < new Date(formBasic.showingStartDate)) {
      errs.endDate = 'Ngày kết thúc không thể trước ngày khởi chiếu.';
    }

    const status = formBasic.status || 'DRAFT';
    const startD = formBasic.showingStartDate ? new Date(formBasic.showingStartDate) : null;
    const endD = formBasic.endDate ? new Date(formBasic.endDate) : null;

    if (status === 'UPCOMING') {
      if (startD && startD <= todayDate) {
        errs.showingStartDate = 'Trạng thái Sắp chiếu yêu cầu ngày khởi chiếu ở tương lai (sau hôm nay).';
      }
    } else if (status === 'NOW_SHOWING') {
      if (startD && startD > todayDate) {
        errs.showingStartDate = 'Trạng thái Đang chiếu yêu cầu ngày khởi chiếu ở quá khứ hoặc hôm nay.';
      }
      if (endD && endD < todayDate) {
        errs.endDate = 'Trạng thái Đang chiếu yêu cầu ngày kết thúc ở tương lai hoặc hôm nay.';
      }
      if (selectedGenres.length === 0 && tmdbGenres.length === 0) {
        errs.genres = 'Phim phải có ít nhất 1 thể loại khi ở trạng thái Đang chiếu.';
      }
    } else if (status === 'ENDED') {
      if (!formBasic.endDate) {
        errs.endDate = 'Trạng thái Ngừng chiếu bắt buộc phải chọn ngày kết thúc.';
      } else if (endD && endD >= todayDate) {
        errs.endDate = 'Trạng thái Ngừng chiếu yêu cầu ngày kết thúc ở quá khứ (trước hôm nay).';
      }
    }

    setFormErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const resolveAndAssignGenres = async (moviePublicId) => {
    let finalIds = selectedGenres.filter(g => g.publicId).map(g => g.publicId);

    const toCreate = tmdbGenres.filter(tg => !tg.existsInDb);
    for (const tg of toCreate) {
      try {
        const created = await adminMovieService.ensureGenreExists(tg.name);
        if (created?.publicId) {
          if (!finalIds.includes(created.publicId)) finalIds.push(created.publicId);
        } else {
          const freshRes = await adminGenreService.getAllGenres();
          const freshList = freshRes?.data?.content || freshRes?.data || freshRes?.content || freshRes || [];
          const found = Array.isArray(freshList)
            ? freshList.find(g => g.name?.toLowerCase() === tg.name?.toLowerCase())
            : null;
          if (found?.publicId && !finalIds.includes(found.publicId)) {
            finalIds.push(found.publicId);
            setGenresList(prev =>
              prev.some(g => g.publicId === found.publicId) ? prev : [...prev, found]
            );
          }
        }
      } catch { /* skip */ }
    }

    const uniqueGenreIds = [...new Set(finalIds)];
    if (uniqueGenreIds.length > 0) {
      await adminMovieService.assignGenres(moviePublicId, uniqueGenreIds);
    }
  };

  const resolveAndAssignCreditsAndCompanies = async (moviePublicId) => {
    try {
      const creditRequests = [];

      for (let i = 0; i < cast.length; i++) {
        const c = cast[i];
        if (c.name?.trim()) {
          const person = await adminMovieService.ensurePersonExists(c.name, c.profileUrl);
          if (person?.publicId) {
            creditRequests.push({
              personPublicId: person.publicId,
              roleType: 'MAIN_ACTOR',
              characterName: c.character || '',
              displayOrder: i
            });
          }
        }
      }

      for (let i = 0; i < directors.length; i++) {
        const d = directors[i];
        if (d.name?.trim()) {
          const person = await adminMovieService.ensurePersonExists(d.name, d.profileUrl || '');
          if (person?.publicId) {
            creditRequests.push({
              personPublicId: person.publicId,
              roleType: 'DIRECTOR',
              characterName: '',
              displayOrder: i
            });
          }
        }
      }

      for (let i = 0; i < writers.length; i++) {
        const w = writers[i];
        if (w.name?.trim()) {
          const person = await adminMovieService.ensurePersonExists(w.name, w.profileUrl || '');
          if (person?.publicId) {
            creditRequests.push({
              personPublicId: person.publicId,
              roleType: 'WRITER',
              characterName: '',
              displayOrder: i
            });
          }
        }
      }

      for (let i = 0; i < producers.length; i++) {
        const p = producers[i];
        if (p.name?.trim()) {
          const person = await adminMovieService.ensurePersonExists(p.name, p.profileUrl || '');
          if (person?.publicId) {
            creditRequests.push({
              personPublicId: person.publicId,
              roleType: 'PRODUCER',
              characterName: '',
              displayOrder: i
            });
          }
        }
      }

      const seenCredits = new Set();
      const uniqueCreditRequests = [];
      for (const req of creditRequests) {
        const key = `${req.personPublicId}_${req.roleType}_${(req.characterName || '').trim().toLowerCase()}`;
        if (!seenCredits.has(key)) {
          seenCredits.add(key);
          uniqueCreditRequests.push(req);
        }
      }

      await adminMovieService.assignCredits(moviePublicId, uniqueCreditRequests);

      const companyRequests = [];
      for (const s of studios) {
        if (s.name?.trim()) {
          const company = await adminMovieService.ensureProductionCompanyExists(s.name, s.logoUrl);
          if (company?.publicId) {
            companyRequests.push({
              companyPublicId: company.publicId,
              role: 'PRODUCTION'
            });
          }
        }
      }

      const seenCompanies = new Set();
      const uniqueCompanyRequests = [];
      for (const req of companyRequests) {
        const key = `${req.companyPublicId}_${req.role}`;
        if (!seenCompanies.has(key)) {
          seenCompanies.add(key);
          uniqueCompanyRequests.push(req);
        }
      }

      await adminMovieService.assignProductionCompanies(moviePublicId, uniqueCompanyRequests);
    } catch (err) {
      console.error("Failed to assign credits/companies:", err);
      throw err;
    }
  };

  const createAllMedia = async (publicId) => {
    if (posterUrl?.trim()) {
      await adminMovieService.createMovieMedia(publicId, { mediaType: 'POSTER', url: posterUrl, title: 'Poster', isPrimary: true, displayOrder: 0, status: 'ACTIVE' });
    }
    for (let i = 0; i < bannerUrls.length; i++) {
      const b = bannerUrls[i];
      if (b?.trim()) {
        await adminMovieService.createMovieMedia(publicId, { mediaType: 'BANNER', url: b, title: `Banner ${i + 1}`, isPrimary: i === 0, displayOrder: i, status: 'ACTIVE' });
      }
    }
    if (trailerUrl?.trim()) {
      await adminMovieService.createMovieMedia(publicId, { mediaType: 'TRAILER', url: trailerUrl, title: 'Trailer', isPrimary: false, displayOrder: 0, status: 'ACTIVE' });
    }
  };

  const reconcileMedia = async (publicId, orig) => {
    if (posterUrl?.trim()) {
      if (orig.poster) {
        if (orig.poster.url !== posterUrl) await adminMovieService.updateMovieMedia(orig.poster.publicId, { mediaType: 'POSTER', url: posterUrl, title: 'Poster', isPrimary: true, displayOrder: 0, status: 'ACTIVE' });
      } else {
        await adminMovieService.createMovieMedia(publicId, { mediaType: 'POSTER', url: posterUrl, title: 'Poster', isPrimary: true, displayOrder: 0, status: 'ACTIVE' });
      }
    } else if (orig.poster) {
      await adminMovieService.deleteMovieMedia(orig.poster.publicId);
    }

    for (const ob of orig.banners) await adminMovieService.deleteMovieMedia(ob.publicId);
    for (let i = 0; i < bannerUrls.length; i++) {
      const b = bannerUrls[i];
      if (b?.trim()) await adminMovieService.createMovieMedia(publicId, { mediaType: 'BANNER', url: b, title: `Banner ${i + 1}`, isPrimary: i === 0, displayOrder: i, status: 'ACTIVE' });
    }

    if (trailerUrl?.trim()) {
      if (orig.trailer) {
        if (orig.trailer.url !== trailerUrl) await adminMovieService.updateMovieMedia(orig.trailer.publicId, { mediaType: 'TRAILER', url: trailerUrl, title: 'Trailer', isPrimary: false, displayOrder: 0, status: 'ACTIVE' });
      } else {
        await adminMovieService.createMovieMedia(publicId, { mediaType: 'TRAILER', url: trailerUrl, title: 'Trailer', isPrimary: false, displayOrder: 0, status: 'ACTIVE' });
      }
    } else if (orig.trailer) {
      await adminMovieService.deleteMovieMedia(orig.trailer.publicId);
    }
  };

  const buildVersionPayload = (v) => ({
    versionName: v.versionName,
    format: FORMAT_MAP_TO_API[v.format] || v.format,
    audioLanguage: v.audioLanguage || null,
    subtitleLanguage: v.subtitleLanguage || null,
    dubLanguage: v.dubLanguage || null,
    status: v.status || 'ACTIVE',
  });

  const handleSave = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    setIsSaving(true);
    try {
      const moviePayload = {
        title: formBasic.title?.trim() || '',
        originalTitle: formBasic.originalTitle?.trim() || null,
        durationMinutes: Number(formBasic.durationMinutes),
        ageRating: formBasic.ageRating,
        releaseDate: formBasic.showingStartDate || getTodayString(),
        endDate: formBasic.endDate || null,
        country: formBasic.country?.trim() || null,
        synopsis: formBasic.synopsis?.trim() || null,
        status: formBasic.status || 'UPCOMING',
      };

      if (selectedMovie) {
        const publicId = selectedMovie.publicId;
        await adminMovieService.updateMovie(publicId, moviePayload);
        await resolveAndAssignGenres(publicId);
        await resolveAndAssignCreditsAndCompanies(publicId);
        await reconcileMedia(publicId, origMedia);

        const versionsToDelete = origVersions.filter(ov => !versions.some(v => v.publicId === ov.publicId));
        for (const ov of versionsToDelete) await adminMovieService.deleteMovieVersion(ov.publicId);
        for (const v of versions) {
          if (v.publicId) {
            const orig = origVersions.find(ov => ov.publicId === v.publicId);
            if (orig && JSON.stringify(orig) !== JSON.stringify(v)) {
              await adminMovieService.updateMovieVersion(v.publicId, buildVersionPayload(v));
            }
          } else {
            await adminMovieService.createMovieVersion(publicId, buildVersionPayload(v));
          }
        }
        triggerToast?.('Cập nhật phim thành công!');
      } else {
        const res = await adminMovieService.createMovie(moviePayload);
        const publicId = res?.data?.publicId || res?.publicId;
        if (!publicId) throw new Error('Không nhận được mã phim từ server. Vui lòng kiểm tra lại.');

        await resolveAndAssignGenres(publicId);
        await resolveAndAssignCreditsAndCompanies(publicId);
        await createAllMedia(publicId);
        for (const v of versions) {
          await adminMovieService.createMovieVersion(publicId, buildVersionPayload(v));
        }
        triggerToast?.('Thêm phim mới thành công!');
      }

      onRefreshList();
      onClose();
    } catch (err) {
      console.error("Failed to save movie:", err);
      const d = err?.response?.data || err;
      if (d && d.errorCode === 'VALIDATION_ERROR' && d.data?.fieldErrors) {
        const errs = {};
        d.data.fieldErrors.forEach(e => {
          let fieldKey = e.field;
          if (fieldKey === 'releaseDate') fieldKey = 'showingStartDate';
          errs[fieldKey] = e.message;
        });
        setFormErrors(errs);
        triggerToast?.('Một số thông tin nhập chưa đúng, vui lòng kiểm tra lại.', 'error');
      } else {
        triggerToast?.(parseApiError(err), 'error');
      }
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center p-20 min-h-screen bg-zinc-950 text-white">
        <div className="flex flex-col items-center gap-4">
          <div className="w-12 h-12 border-4 border-[#ff7a1a] border-t-transparent rounded-full animate-spin"></div>
          <p className="text-sm font-semibold tracking-wider text-zinc-400">Đang tải thông tin phim...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto bg-zinc-950 text-zinc-100 space-y-5 animate-fade-in">
      {/* Header */}
      <div className="flex justify-between items-center border-b border-zinc-800 pb-4 flex-shrink-0">
        <div className="flex items-center gap-3">
          <button type="button" onClick={onClose} className="p-2 text-zinc-400 hover:text-white bg-zinc-900 border border-zinc-800 rounded-xl transition-all cursor-pointer">
            <ArrowLeft className="w-4 h-4" />
          </button>
          <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider">
            {isEdit ? 'CẬP NHẬT PHIM' : 'THÊM PHIM MỚI'}
          </h1>
        </div>
        <div className="flex items-center gap-3">
          <button type="button" onClick={onClose}
            className="border border-zinc-850 bg-zinc-900/60 hover:bg-zinc-800 text-zinc-305 font-bold py-2 px-5 rounded-xl text-xs transition-colors cursor-pointer">
            Hủy
          </button>
          {!isEdit && (
            <button type="button" onClick={handleCleanForm}
              className="border border-zinc-850 bg-zinc-900/60 hover:bg-zinc-800 text-zinc-305 font-bold py-2 px-5 rounded-xl text-xs transition-colors cursor-pointer">
              Dọn sạch
            </button>
          )}
          <button type="button" onClick={handleSave} disabled={isSaving}
            className="bg-[#ff7a1a] hover:opacity-90 text-zinc-950 font-black py-2 px-6 rounded-xl text-xs uppercase tracking-wider transition-all shadow-lg flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50">
            {isSaving ? (
              <div className="w-4 h-4 border-2 border-zinc-950 border-t-transparent rounded-full animate-spin" />
            ) : (
              <><Check className="w-4 h-4" /><span>LƯU LẠI</span></>
            )}
          </button>
        </div>
      </div>

{/* TMDB Recommendations Placeholder */}
      {!isEdit && (
        <div className="relative w-full bg-zinc-900/40 border border-zinc-800/40 p-6 rounded-3xl flex-shrink-0 group overflow-hidden">
          <label className="text-[#ff7a1a] text-[10px] font-black uppercase tracking-widest block mb-2">
            ĐỒNG BỘ TMDB
          </label>
          <p className="text-sm text-zinc-400">
            Dữ liệu phim được hệ thống đồng bộ tự động từ TMDB. Chức năng quản lý đồng bộ sẽ được triển khai ở giai đoạn sau.
          </p>
        </div>
      )}

      {/* Tab Selector */}
      <div className="flex border-b border-zinc-800 flex-shrink-0">
        {[
          { id: 'basic', label: 'Thông tin cơ bản' },
          { id: 'crew', label: 'Nhân sự & Hãng phim' },
          { id: 'media', label: 'Hình ảnh & Trailer' }
        ].map(tab => (
          <button
            key={tab.id}
            type="button"
            onClick={() => setActiveFormTab(tab.id)}
            className={`py-3 px-6 text-xs md:text-sm font-bold border-b-2 transition-all cursor-pointer uppercase tracking-wider ${activeFormTab === tab.id
              ? 'border-[#ff7a1a] text-[#ff7a1a] bg-[#ff7a1a]/5 font-black'
              : 'border-transparent text-zinc-400 hover:text-zinc-200'
              }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Form Body */}
      <form onSubmit={e => e.preventDefault()} className="pb-16">
        {/* TAB 1: Basic */}
        {activeFormTab === 'basic' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 animate-fade-in">
            <div className="lg:col-span-2 space-y-6">
              <FormSection icon={<Film className="w-4 h-4 text-[#ff7a1a]" />} title="Thông Tin Cơ Bản">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <Field label="Tên phim" required error={formErrors.title}>
                    <Input value={formBasic.title} onChange={e => setFormBasic(p => ({ ...p, title: e.target.value }))} />
                  </Field>
                  <Field label="Tên gốc (Nguyên bản)">
                    <Input value={formBasic.originalTitle} onChange={e => setFormBasic(p => ({ ...p, originalTitle: e.target.value }))} />
                  </Field>
                  <Field label="Thời lượng (phút)" required error={formErrors.durationMinutes}>
                    <Input type="number" min="1" value={formBasic.durationMinutes} onChange={e => setFormBasic(p => ({ ...p, durationMinutes: e.target.value }))} />
                  </Field>
                  <Field label="Quốc gia sản xuất">
                    <Input value={formBasic.country} onChange={e => setFormBasic(p => ({ ...p, country: e.target.value }))} placeholder="Vd: United States of America" />
                  </Field>
                  <Field label="Giới hạn độ tuổi" required error={formErrors.ageRating}>
                    <Select value={formBasic.ageRating} onChange={e => setFormBasic(p => ({ ...p, ageRating: e.target.value }))}>
                      {AGE_RATINGS.map(r => <option key={r} value={r}>{AGE_RATING_LABELS[r]}</option>)}
                    </Select>
                  </Field>

                  {isEdit && (
                    <>
                      <Field label="Trạng thái">
                        <Select value={formBasic.status} onChange={e => setFormBasic(p => ({ ...p, status: e.target.value }))}>
                          {Object.entries(STATUS_LABELS).map(([k, v]) => (
                            <option key={k} value={k}>{v}</option>
                          ))}
                        </Select>
                      </Field>
                      <div className="col-span-1 md:col-span-2 bg-zinc-900/40 border border-zinc-800 rounded-2xl p-4 mt-2 space-y-2">
                        <p className="text-xs font-bold text-zinc-300 flex items-center gap-1.5">
                          <ImageIcon className="w-4 h-4 text-[#ff7a1a]" />
                          <span>Quy định thiết lập trạng thái phim:</span>
                        </p>
                        <ul className="text-[11px] text-zinc-400 space-y-1.5 list-disc pl-4 leading-relaxed">
                          <li><strong>Nháp (DRAFT) / Khóa (INACTIVE):</strong> Dùng khi phim đang biên tập. Không ràng buộc ngày.</li>
                          <li><strong>Sắp chiếu (UPCOMING):</strong> Ngày khởi chiếu phải ở <strong>tương lai</strong>.</li>
                          <li><strong>Đang chiếu (NOW_SHOWING):</strong> Ngày khởi chiếu ở <strong>quá khứ hoặc hôm nay</strong> và Ngày kết thúc (nếu có) phải ở <strong>tương lai hoặc hôm nay</strong>. Phải có ít nhất 1 thể loại.</li>
                          <li><strong>Ngừng chiếu (ENDED):</strong> Bắt buộc có Ngày kết thúc và ngày này ở <strong>quá khứ</strong>.</li>
                        </ul>
                      </div>
                    </>
                  )}
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-2">
                  <Field label="Ngày khởi chiếu" required error={formErrors.showingStartDate}>
                    <Input type="date" value={formBasic.showingStartDate} onChange={e => setFormBasic(p => ({ ...p, showingStartDate: e.target.value }))} />
                  </Field>
                  <Field label="Ngày ngừng chiếu" error={formErrors.endDate}>
                    <Input type="date" value={formBasic.endDate} onChange={e => setFormBasic(p => ({ ...p, endDate: e.target.value }))} />
                  </Field>
                </div>

                <Field label="Nội dung tóm tắt">
                  <Textarea rows={5} value={formBasic.synopsis} onChange={e => setFormBasic(p => ({ ...p, synopsis: e.target.value }))} />
                </Field>

                {!isEdit && (
                  <div className="flex items-center gap-2 bg-blue-950/30 border border-blue-900/30 rounded-xl p-3 text-[11px] text-blue-300">
                    <Info className="w-4 h-4 flex-shrink-0 text-blue-400" />
                    <span>Phim mới tạo sẽ ở trạng thái Nháp (DRAFT) mặc định.</span>
                  </div>
                )}
              </FormSection>

              {/* Genres */}
              <FormSection icon={<LayoutList className="w-4 h-4 text-[#ff7a1a]" />} title="Thể Loại Phim">
                {formErrors.genres && (
                  <div className="text-red-400 text-xs mb-4 bg-red-950/20 border border-red-800/30 rounded-xl p-2.5 flex items-center gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-red-500 animate-pulse" />
                    <span>{formErrors.genres}</span>
                  </div>
                )}

                {tmdbGenres.length > 0 && (
                  <div className="space-y-2 mb-4">
                    <p className="text-[10px] font-black text-zinc-500 uppercase tracking-wider flex items-center gap-1.5">
                      <span className="w-1.5 h-1.5 rounded-full bg-[#ff7a1a] inline-block" />
                      Thể loại từ TMDB
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {tmdbGenres.map(tg => (
                        <div
                          key={tg.tmdbId ?? tg.name}
                          className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl border text-xs font-semibold select-none ${tg.existsInDb
                            ? 'bg-emerald-500/10 border-emerald-500/25 text-emerald-400'
                            : 'bg-amber-500/10 border-amber-500/25 text-amber-300'
                          }`}
                        >
                          {tg.existsInDb ? <Check className="w-3 h-3 flex-shrink-0" /> : <Plus className="w-3 h-3 flex-shrink-0" />}
                          <span>{tg.name}</span>
                          {!tg.existsInDb && (
                            <span className="text-[9px] font-black bg-amber-400/20 text-amber-200 px-1 py-0.5 rounded uppercase ml-0.5">Tự tạo</span>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {genresList.length > 0 && (
                  <div className="space-y-2">
                    {tmdbGenres.length > 0 && <p className="text-[10px] font-black text-zinc-500 uppercase tracking-wider">Chọn thêm thể loại</p>}
                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                      {genresList.map(g => {
                        const checked = selectedGenres.some(s => s.publicId === g.publicId);
                        const fromTmdb = tmdbGenres.some(tg => tg.existsInDb && tg.dbPublicId === g.publicId);
                        return (
                          <label key={g.publicId}
                            className={`flex items-center gap-2 p-2.5 border rounded-xl cursor-pointer transition-all select-none text-xs ${checked && fromTmdb
                              ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300'
                              : checked
                                ? 'border-[#ff7a1a]/30 bg-[#ff7a1a]/10 text-[#ff7a1a]'
                                : 'border-zinc-800 bg-[#050506] text-zinc-400 hover:text-zinc-200'
                            }`}>
                            <input type="checkbox" checked={checked} onChange={() => toggleGenre(g)} className="hidden" />
                            {checked ? <Check className="w-3.5 h-3.5 flex-shrink-0" /> : <div className="w-3.5 h-3.5 border border-zinc-700 rounded-sm flex-shrink-0" />}
                            <span className="truncate">{g.name}</span>
                            {fromTmdb && checked && <span className="text-[9px] text-emerald-500 ml-auto flex-shrink-0">TMDB</span>}
                          </label>
                        );
                      })}
                    </div>
                  </div>
                )}
              </FormSection>
            </div>

            {/* Versions Column */}
            <div className="space-y-6">
              <FormSection icon={<Film className="w-4 h-4 text-[#ff7a1a]" />} title="Phiên Bản Chiếu"
                headerAction={
                  <button type="button" onClick={addVersion} className="text-[10px] font-black text-[#ff7a1a] hover:opacity-80 uppercase flex items-center gap-1">
                    <Plus className="w-3.5 h-3.5" />THÊM PHIÊN BẢN
                  </button>
                }>
                {versions.length === 0 ? (
                  <p className="text-zinc-500 text-xs py-4 text-center italic">Chưa có phiên bản nào. Click "THÊM PHIÊN BẢN".</p>
                ) : (
                  versions.map((ver, vIdx) => (
                    <div key={vIdx} className="relative p-4 bg-[#050506] border border-zinc-800 rounded-xl mb-3 last:mb-0">
                      <button type="button" onClick={() => removeVersion(vIdx)} className="absolute top-3 right-3 text-zinc-650 hover:text-red-400 transition-colors">
                        <X className="w-3.5 h-3.5" />
                      </button>
                      <div className="grid grid-cols-1 gap-2.5 pr-6">
                        {[
                          ['Tên phiên bản', 'versionName', 'text', { placeholder: 'Vd: 2D Vietsub' }],
                          ['Định dạng', 'format', 'select', { opts: ['2D', '3D', 'IMAX', '4DX', 'SCREENX'] }],
                          ['Ngôn ngữ thoại', 'audioLanguage', 'select', { opts: ['VI', 'EN', 'JA', 'KO', 'TH', 'ZH', 'FR', 'ES'] }],
                          ['Phụ đề', 'subtitleLanguage', 'select', { opts: ['VI', 'EN', 'NONE'] }],
                          ['Lồng tiếng', 'dubLanguage', 'select', { opts: ['NONE', 'VI', 'EN'] }],
                          isEdit && ['Trạng thái', 'status', 'select', { opts: ['ACTIVE', 'INACTIVE'] }],
                        ].filter(Boolean).map(([lbl, field, type, opts]) => (
                          <div key={field} className="space-y-1">
                            <p className="text-[9px] font-black uppercase text-zinc-500">{lbl}</p>
                            {type === 'select' ? (
                              <select value={ver[field] || ''} onChange={e => updateVersion(vIdx, field, e.target.value)}
                                className="w-full bg-zinc-900 border border-zinc-800 rounded-lg py-1.5 px-2 text-xs text-zinc-100 outline-none">
                                {Array.from(new Set([...opts.opts, ver[field]].filter(Boolean))).map(o => (
                                  <option key={o} value={o}>{o === 'NONE' ? 'Không' : o}</option>
                                ))}
                              </select>
                            ) : (
                              <input type="text" value={ver[field] || ''} onChange={e => updateVersion(vIdx, field, e.target.value)}
                                placeholder={opts.placeholder || ''}
                                className="w-full bg-zinc-900 border border-zinc-800 rounded-lg py-1.5 px-2 text-xs text-zinc-100 focus:outline-none focus:border-[#ff7a1a]/30" />
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  ))
                )}
              </FormSection>
            </div>
          </div>
        )}

        {/* TAB 2: Crew */}
        {activeFormTab === 'crew' && (
          <div className="grid grid-cols-1 gap-6 animate-fade-in max-w-4xl mx-auto">
            {/* Studios */}
            <FormSection icon={<Building2 className="w-4 h-4 text-[#ff7a1a]" />} title="Hãng Sản Xuất"
              headerAction={
                <button type="button" onClick={() => setStudios(p => [...p, { name: '', logoUrl: '' }])}
                  className="text-[10px] font-black text-[#ff7a1a] hover:opacity-80 flex items-center gap-0.5">
                  <Plus className="w-3.5 h-3.5" />Thêm Hãng Phim
                </button>
              }
            >
              {studios.length === 0 ? (
                <p className="text-zinc-500 text-xs py-4 text-center italic">Chưa cấu hình hãng sản xuất.</p>
              ) : (
                <div className="space-y-2">
                  {studios.map((s, idx) => (
                    <div
                      key={idx}
                      draggable
                      onDragStart={() => handleDragStart(idx, 'studios')}
                      onDragOver={handleDragOver}
                      onDragEnter={() => handleDragEnter(idx, 'studios', studios, setStudios)}
                      onDragEnd={handleDragEnd}
                      className={`flex items-center gap-3 bg-zinc-900 border border-zinc-800 p-2.5 px-4 rounded-xl transition-all duration-200 ${draggedType === 'studios' && draggedIdx === idx ? 'opacity-40 scale-95 border-[#ff7a1a]/40 bg-neutral-950' : ''}`}
                    >
                      <div className="cursor-grab text-zinc-650 hover:text-zinc-400">
                        <GripVertical className="w-4 h-4" />
                      </div>
                      <div className="w-8 h-6 bg-zinc-950 border border-zinc-800 rounded flex items-center justify-center overflow-hidden shrink-0">
                        {s.logoUrl ? (
                          <LazyImage
                            src={s.logoUrl}
                            alt=""
                            containerClassName="w-full h-full border-none rounded-none bg-transparent"
                            className="object-contain"
                          />
                        ) : (
                          <Building2 className="w-3.5 h-3.5 text-zinc-700" />
                        )}
                      </div>
                      <div className="flex-grow grid grid-cols-1 sm:grid-cols-2 gap-2">
                        <input
                          type="text"
                          value={s.name || ''}
                          onChange={e => setStudios(prev => prev.map((item, i) => i === idx ? { ...item, name: e.target.value } : item))}
                          placeholder="Tên hãng sản xuất (Bắt buộc)"
                          className="bg-[#050506] border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-[#ff7a1a]/30 min-w-0"
                        />
                        <input
                          type="text"
                          value={s.logoUrl || ''}
                          onChange={e => setStudios(prev => prev.map((item, i) => i === idx ? { ...item, logoUrl: e.target.value } : item))}
                          placeholder="URL logo (Tùy chọn)"
                          className="bg-[#050506] border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-[#ff7a1a]/30 min-w-0"
                        />
                      </div>
                      <button
                        type="button"
                        onClick={() => setStudios(prev => prev.filter((_, i) => i !== idx))}
                        className="p-1.5 text-zinc-500 hover:text-red-400 transition-colors"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </FormSection>

            {/* Directors */}
            <FormSection icon={<Users className="w-4 h-4 text-[#ff7a1a]" />} title="Đạo diễn"
              headerAction={
                <button type="button" onClick={() => setDirectors(p => [...p, { name: '', profileUrl: '' }])}
                  className="text-[10px] font-black text-[#ff7a1a] hover:opacity-80 flex items-center gap-0.5">
                  <Plus className="w-3.5 h-3.5" />Thêm Đạo Diễn
                </button>
              }
            >
              {directors.length === 0 ? (
                <p className="text-zinc-500 text-xs py-4 text-center italic">Chưa có đạo diễn. Nhấp "Thêm Đạo Diễn".</p>
              ) : (
                <div className="space-y-2">
                  {directors.map((d, idx) => (
                    <div
                      key={idx}
                      draggable
                      onDragStart={() => handleDragStart(idx, 'directors')}
                      onDragOver={handleDragOver}
                      onDragEnter={() => handleDragEnter(idx, 'directors', directors, setDirectors)}
                      onDragEnd={handleDragEnd}
                      className={`flex items-center gap-3 bg-zinc-900 border border-zinc-800 p-2.5 px-4 rounded-xl transition-all duration-200 ${draggedType === 'directors' && draggedIdx === idx ? 'opacity-40 scale-95 border-[#ff7a1a]/40 bg-neutral-950' : ''}`}
                    >
                      <div className="cursor-grab text-zinc-650 hover:text-zinc-400">
                        <GripVertical className="w-4 h-4" />
                      </div>
                      <LazyImage
                        src={d.profileUrl || d.profileImageUrl || DEFAULT_AVATAR}
                        alt=""
                        containerClassName="w-7 h-7 rounded-full border border-zinc-800 shrink-0"
                        className="rounded-full object-cover"
                      />
                      <div className="flex-grow grid grid-cols-1 sm:grid-cols-2 gap-2">
                        <input
                          type="text"
                          value={d.name || ''}
                          onChange={e => setDirectors(prev => prev.map((item, i) => i === idx ? { ...item, name: e.target.value } : item))}
                          placeholder="Tên đạo diễn (Bắt buộc)"
                          className="bg-[#050506] border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-[#ff7a1a]/30 min-w-0"
                        />
                        <input
                          type="text"
                          value={d.profileUrl || d.profileImageUrl || ''}
                          onChange={e => setDirectors(prev => prev.map((item, i) => i === idx ? { ...item, profileUrl: e.target.value, profileImageUrl: e.target.value } : item))}
                          placeholder="URL ảnh đại diện (Tùy chọn)"
                          className="bg-[#050506] border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-[#ff7a1a]/30 min-w-0"
                        />
                      </div>
                      <button
                        type="button"
                        onClick={() => setDirectors(prev => prev.filter((_, i) => i !== idx))}
                        className="p-1.5 text-zinc-500 hover:text-red-400 transition-colors"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </FormSection>

            {/* Cast */}
            <FormSection icon={<Users className="w-4 h-4 text-[#ff7a1a]" />} title="Diễn Viên Chính"
              headerAction={
                <button type="button" onClick={() => setCast(p => [...p, { name: '', character: '', profileUrl: '' }])}
                  className="text-[10px] font-black text-[#ff7a1a] hover:opacity-80 flex items-center gap-0.5">
                  <Plus className="w-3.5 h-3.5" />Thêm Diễn Viên
                </button>
              }
            >
              {cast.length === 0 ? (
                <p className="text-zinc-500 text-xs py-4 text-center italic">Chưa có diễn viên. Nhấp "Thêm Diễn Viên".</p>
              ) : (
                <div className="space-y-2">
                  {cast.map((c, idx) => (
                    <div
                      key={idx}
                      draggable
                      onDragStart={() => handleDragStart(idx, 'cast')}
                      onDragOver={handleDragOver}
                      onDragEnter={() => handleDragEnter(idx, 'cast', cast, setCast)}
                      onDragEnd={handleDragEnd}
                      className={`flex items-center gap-3 bg-zinc-900 border border-zinc-800 p-2.5 px-4 rounded-xl transition-all duration-200 ${draggedType === 'cast' && draggedIdx === idx ? 'opacity-40 scale-95 border-[#ff7a1a]/40 bg-neutral-950' : ''}`}
                    >
                      <div className="cursor-grab text-zinc-650 hover:text-zinc-400">
                        <GripVertical className="w-4 h-4" />
                      </div>
                      <LazyImage
                        src={c.profileUrl || c.profileImageUrl || DEFAULT_AVATAR}
                        alt=""
                        containerClassName="w-7 h-7 rounded-full border border-zinc-800 shrink-0"
                        className="rounded-full object-cover"
                      />
                      <div className="flex-grow grid grid-cols-1 sm:grid-cols-3 gap-2">
                        <input
                          type="text"
                          value={c.name || ''}
                          onChange={e => setCast(prev => prev.map((item, i) => i === idx ? { ...item, name: e.target.value } : item))}
                          placeholder="Tên diễn viên (Bắt buộc)"
                          className="bg-[#050506] border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-[#ff7a1a]/30 min-w-0"
                        />
                        <input
                          type="text"
                          value={c.character || ''}
                          onChange={e => setCast(prev => prev.map((item, i) => i === idx ? { ...item, character: e.target.value } : item))}
                          placeholder="Vai diễn (Nhân vật)"
                          className="bg-[#050506] border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-[#ff7a1a]/30 min-w-0"
                        />
                        <input
                          type="text"
                          value={c.profileUrl || c.profileImageUrl || ''}
                          onChange={e => setCast(prev => prev.map((item, i) => i === idx ? { ...item, profileUrl: e.target.value, profileImageUrl: e.target.value } : item))}
                          placeholder="URL hình ảnh (Tùy chọn)"
                          className="bg-[#050506] border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-[#ff7a1a]/30 min-w-0"
                        />
                      </div>
                      <button
                        type="button"
                        onClick={() => setCast(prev => prev.filter((_, i) => i !== idx))}
                        className="p-1.5 text-zinc-500 hover:text-red-400 transition-colors"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </FormSection>

            {/* Writers */}
            <FormSection icon={<Users className="w-4 h-4 text-[#ff7a1a]" />} title="Biên Kịch"
              headerAction={
                <button type="button" onClick={() => setWriters(p => [...p, { name: '', profileUrl: '' }])}
                  className="text-[10px] font-black text-[#ff7a1a] hover:opacity-80 flex items-center gap-0.5">
                  <Plus className="w-3.5 h-3.5" />Thêm Biên Kịch
                </button>
              }
            >
              {writers.length === 0 ? (
                <p className="text-zinc-500 text-xs py-4 text-center italic">Chưa có biên kịch. Nhấp "Thêm Biên Kịch".</p>
              ) : (
                <div className="space-y-2">
                  {writers.map((w, idx) => (
                    <div
                      key={idx}
                      draggable
                      onDragStart={() => handleDragStart(idx, 'writers')}
                      onDragOver={handleDragOver}
                      onDragEnter={() => handleDragEnter(idx, 'writers', writers, setWriters)}
                      onDragEnd={handleDragEnd}
                      className={`flex items-center gap-3 bg-zinc-900 border border-zinc-800 p-2.5 px-4 rounded-xl transition-all duration-200 ${draggedType === 'writers' && draggedIdx === idx ? 'opacity-40 scale-95 border-[#ff7a1a]/40 bg-neutral-950' : ''}`}
                    >
                      <div className="cursor-grab text-zinc-650 hover:text-zinc-400">
                        <GripVertical className="w-4 h-4" />
                      </div>
                      <LazyImage
                        src={w.profileUrl || w.profileImageUrl || DEFAULT_AVATAR}
                        alt=""
                        containerClassName="w-7 h-7 rounded-full border border-zinc-800 shrink-0"
                        className="rounded-full object-cover"
                      />
                      <div className="flex-grow grid grid-cols-1 sm:grid-cols-2 gap-2">
                        <input
                          type="text"
                          value={w.name || ''}
                          onChange={e => setWriters(prev => prev.map((item, i) => i === idx ? { ...item, name: e.target.value } : item))}
                          placeholder="Tên biên kịch (Bắt buộc)"
                          className="bg-[#050506] border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-[#ff7a1a]/30 min-w-0"
                        />
                        <input
                          type="text"
                          value={w.profileUrl || w.profileImageUrl || ''}
                          onChange={e => setWriters(prev => prev.map((item, i) => i === idx ? { ...item, profileUrl: e.target.value, profileImageUrl: e.target.value } : item))}
                          placeholder="URL ảnh đại diện (Tùy chọn)"
                          className="bg-[#050506] border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-[#ff7a1a]/30 min-w-0"
                        />
                      </div>
                      <button
                        type="button"
                        onClick={() => setWriters(prev => prev.filter((_, i) => i !== idx))}
                        className="p-1.5 text-zinc-500 hover:text-red-400 transition-colors"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </FormSection>

            {/* Producers */}
            <FormSection icon={<Users className="w-4 h-4 text-[#ff7a1a]" />} title="Nhà Sản Xuất"
              headerAction={
                <button type="button" onClick={() => setProducers(p => [...p, { name: '', profileUrl: '' }])}
                  className="text-[10px] font-black text-[#ff7a1a] hover:opacity-80 flex items-center gap-0.5">
                  <Plus className="w-3.5 h-3.5" />Thêm Nhà Sản Xuất
                </button>
              }
            >
              {producers.length === 0 ? (
                <p className="text-zinc-500 text-xs py-4 text-center italic">Chưa có nhà sản xuất. Nhấp "Thêm Nhà Sản Xuất".</p>
              ) : (
                <div className="space-y-2">
                  {producers.map((p, idx) => (
                    <div
                      key={idx}
                      draggable
                      onDragStart={() => handleDragStart(idx, 'producers')}
                      onDragOver={handleDragOver}
                      onDragEnter={() => handleDragEnter(idx, 'producers', producers, setProducers)}
                      onDragEnd={handleDragEnd}
                      className={`flex items-center gap-3 bg-zinc-900 border border-zinc-800 p-2.5 px-4 rounded-xl transition-all duration-200 ${draggedType === 'producers' && draggedIdx === idx ? 'opacity-40 scale-95 border-[#ff7a1a]/40 bg-neutral-950' : ''}`}
                    >
                      <div className="cursor-grab text-zinc-650 hover:text-zinc-400">
                        <GripVertical className="w-4 h-4" />
                      </div>
                      <LazyImage
                        src={p.profileUrl || p.profileImageUrl || DEFAULT_AVATAR}
                        alt=""
                        containerClassName="w-7 h-7 rounded-full border border-zinc-800 shrink-0"
                        className="rounded-full object-cover"
                      />
                      <div className="flex-grow grid grid-cols-1 sm:grid-cols-2 gap-2">
                        <input
                          type="text"
                          value={p.name || ''}
                          onChange={e => setProducers(prev => prev.map((item, i) => i === idx ? { ...item, name: e.target.value } : item))}
                          placeholder="Tên nhà sản xuất (Bắt buộc)"
                          className="bg-[#050506] border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-[#ff7a1a]/30 min-w-0"
                        />
                        <input
                          type="text"
                          value={p.profileUrl || p.profileImageUrl || ''}
                          onChange={e => setProducers(prev => prev.map((item, i) => i === idx ? { ...item, profileUrl: e.target.value, profileImageUrl: e.target.value } : item))}
                          placeholder="URL ảnh đại diện (Tùy chọn)"
                          className="bg-[#050506] border border-zinc-800 rounded-lg py-1.5 px-3 text-xs text-zinc-100 outline-none focus:border-[#ff7a1a]/30 min-w-0"
                        />
                      </div>
                      <button
                        type="button"
                        onClick={() => setProducers(prev => prev.filter((_, i) => i !== idx))}
                        className="p-1.5 text-zinc-500 hover:text-red-400 transition-colors"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </FormSection>
          </div>
        )}

        {/* TAB 3: Media */}
        {activeFormTab === 'media' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 animate-fade-in">
            {/* Poster URL */}
            <div className="space-y-6">
              <div className="bg-[#050506] border border-zinc-800 rounded-2xl p-5 space-y-5">
                <h3 className="text-xs font-black text-white uppercase tracking-wider border-b border-zinc-800 pb-2 flex items-center gap-1.5">
                  <ImageIcon className="w-4.5 h-4.5 text-[#ff7a1a]" />
                  <span>Poster phim</span>
                </h3>
                <div className="space-y-2">
                  <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Poster URL</label>
                  <input type="text" value={posterUrl} onChange={e => setPosterUrl(e.target.value)}
                    placeholder="https://..."
                    className="w-full bg-[#050506] border border-zinc-800 rounded-xl py-2 px-3 text-xs text-zinc-100 focus:outline-none focus:border-[#ff7a1a]/40" />
                  <div className="w-full h-72 bg-neutral-900 border border-zinc-800 rounded-xl overflow-hidden flex items-center justify-center select-none">
                    {posterUrl?.trim() ? (
                      <LazyImage
                        src={posterUrl}
                        alt="Poster preview"
                        containerClassName="w-full h-full border-none rounded-none"
                        className="object-contain"
                      />
                    ) : (
                      <div className="text-zinc-650 flex flex-col items-center gap-1">
                        <ImageIcon className="w-7 h-7" />
                        <span className="text-[10px]">Chưa có Poster</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* Banners & Backdrops */}
            <div className="lg:col-span-2 space-y-6">
              <div className="bg-[#050506] border border-zinc-800 rounded-2xl p-5 space-y-5">
                <div className="flex justify-between items-center border-b border-zinc-800 pb-2">
                  <h3 className="text-xs font-black text-white uppercase tracking-wider flex items-center gap-1.5">
                    <ImageIcon className="w-4.5 h-4.5 text-[#ff7a1a]" />
                    <span>Backdrop & Banner phim</span>
                  </h3>
                  <button type="button" onClick={addBanner} className="text-[10px] font-black text-[#ff7a1a] hover:opacity-80 flex items-center gap-0.5">
                    <Plus className="w-3.5 h-3.5" />Thêm Banner
                  </button>
                </div>

                {availableBackdrops.length > 0 && (
                  <div className="bg-zinc-900 border border-zinc-850 p-4 rounded-xl space-y-2">
                    <label className="text-zinc-400 text-[10px] font-black uppercase tracking-wider block">
                      Số lượng Banner import (TMDB: {availableBackdrops.length})
                    </label>
                    <input
                      type="number"
                      min="0"
                      max={availableBackdrops.length}
                      value={backdropImportCount}
                      onChange={e => {
                        const count = Math.max(0, Math.min(availableBackdrops.length, Number(e.target.value)));
                        setBackdropImportCount(count);
                        setBannerUrls(availableBackdrops.slice(0, count).map(b => b.url || ''));
                      }}
                      className="w-24 bg-[#050506] border border-zinc-800 rounded-lg py-1 px-3 text-xs text-zinc-100 focus:outline-none focus:border-[#ff7a1a]/40 outline-none"
                    />
                  </div>
                )}

                <div className="space-y-3">
                  {bannerUrls.map((b, bIdx) => (
                    <div
                      key={bIdx}
                      draggable
                      onDragStart={() => handleDragStart(bIdx, 'banners')}
                      onDragOver={handleDragOver}
                      onDragEnter={() => handleDragEnter(bIdx, 'banners', bannerUrls, setBannerUrls)}
                      onDragEnd={handleDragEnd}
                      className={`flex gap-3 bg-zinc-900 border rounded-xl p-3 transition-all duration-200 ${draggedType === 'banners' && draggedIdx === bIdx ? 'opacity-40 scale-[0.98] border-[#ff7a1a]/40 bg-neutral-950' : 'border-zinc-800'}`}
                    >
                      <div className="flex flex-col items-center justify-center gap-1 cursor-grab text-zinc-600 hover:text-zinc-400 pt-1 shrink-0">
                        <GripVertical className="w-4 h-4" />
                        {bIdx === 0 && (
                          <span className="text-[8px] font-black text-[#ff7a1a] uppercase tracking-wider">Chính</span>
                        )}
                      </div>

                      <div className="w-28 h-16 bg-zinc-950 border border-zinc-800 rounded-lg overflow-hidden flex-shrink-0 flex items-center justify-center">
                        {b?.trim() ? (
                          <LazyImage
                            src={b}
                            alt={`Banner ${bIdx + 1}`}
                            containerClassName="w-full h-full border-none rounded-none"
                          />
                        ) : (
                          <div className="text-zinc-700 flex flex-col items-center gap-0.5">
                            <ImageIcon className="w-4 h-4" />
                            <span className="text-[9px]">Banner {bIdx + 1}</span>
                          </div>
                        )}
                      </div>

                      <div className="flex-1 min-w-0 flex flex-col justify-center gap-1.5">
                        <div className="flex items-center gap-1 text-[9px] font-black text-zinc-500 uppercase tracking-wider">
                          <span>Banner #{bIdx + 1}</span>
                        </div>
                        <div className="flex gap-2">
                          <input
                            type="text"
                            value={b}
                            onChange={e => updateBanner(bIdx, e.target.value)}
                            placeholder="URL hình ảnh Banner (https://...)"
                            className="flex-1 bg-[#050506] border border-zinc-800 rounded-lg py-1.5 px-2.5 text-xs text-zinc-100 focus:outline-none focus:border-[#ff7a1a]/30 min-w-0 font-mono"
                          />
                          {bannerUrls.length > 1 && (
                            <button type="button" onClick={() => removeBanner(bIdx)} className="p-1.5 text-zinc-650 hover:text-red-400 transition-colors shrink-0">
                              <X className="w-3.5 h-3.5" />
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Trailer */}
              <div className="bg-[#050506] border border-zinc-800 rounded-2xl p-5 space-y-5">
                <h3 className="text-xs font-black text-white uppercase tracking-wider border-b border-zinc-800 pb-2 flex items-center gap-1.5">
                  <Play className="w-4.5 h-4.5 text-[#ff7a1a]" />
                  <span>Trailer chính thức</span>
                </h3>
                <div className="space-y-2">
                  <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">YouTube Trailer URL</label>
                  <input type="text" value={trailerUrl} onChange={e => setTrailerUrl(e.target.value)}
                    placeholder="https://youtube.com/watch?v=..."
                    className="w-full bg-[#050506] border border-zinc-800 rounded-xl py-2 px-3 text-xs text-zinc-100 focus:outline-none focus:border-[#ff7a1a]/40" />
                  {getYoutubeEmbedUrl(trailerUrl) ? (
                    <div className="w-full aspect-video rounded-xl overflow-hidden border border-zinc-800 relative bg-black">
                      {playTrailer ? (
                        <iframe src={`${getYoutubeEmbedUrl(trailerUrl)}?autoplay=1`} title="Trailer" className="w-full h-full border-none" allow="autoplay; encrypted-media" allowFullScreen />
                      ) : (
                        <div
                          onClick={() => setPlayTrailer(true)}
                          className="w-full h-full cursor-pointer relative flex items-center justify-center select-none group/play"
                        >
                          <LazyImage
                            src={`https://img.youtube.com/vi/${getYoutubeId(trailerUrl)}/hqdefault.jpg`}
                            alt="Trailer preview"
                            containerClassName="w-full h-full border-none rounded-none"
                            className="opacity-70 group-hover/play:opacity-90 transition-opacity duration-300"
                          />
                          <div className="absolute inset-0 bg-black/20" />
                          <div className="absolute w-14 h-14 rounded-full bg-[#ff7a1a]/95 text-zinc-950 flex items-center justify-center shadow-2xl transition-all duration-300 group-hover/play:scale-110">
                            <Play className="w-6 h-6 fill-current ml-1" />
                          </div>
                        </div>
                      )}
                    </div>
                  ) : (
                    <div className="w-full aspect-video bg-neutral-900 border border-zinc-800 rounded-xl flex flex-col items-center justify-center text-zinc-650 gap-1.5">
                      <Play className="w-7 h-7 animate-pulse" />
                      <span className="text-[10px]">Chưa cấu hình Trailer hoặc đường dẫn sai</span>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}
      </form>
    </div>
  );
}

function FormSection({ icon, title, children, headerAction }) {
  return (
    <div className="bg-[#050506] border border-zinc-800 rounded-2xl p-5 space-y-4">
      <div className="flex justify-between items-center border-b border-zinc-800 pb-2">
        <h3 className="text-xs font-black text-white uppercase tracking-wider flex items-center gap-2">
          {icon}<span>{title}</span>
        </h3>
        {headerAction}
      </div>
      {children}
    </div>
  );
}
