import { useState, useMemo } from 'react';
import { Search, Film, Award, Star, Heart, Calendar, MapPin, Ruler } from 'lucide-react';
import { useData } from '../../contexts/DataContext';

export default function DirectorRegistryView({ directorName, onBackHome, onBookTicket, onNavigate }) {
  const { movies, cinemas } = useData();
  const [searchQuery, setSearchQuery] = useState('');
  
  // Country & Sort list filter states
  const [countryFilter, setCountryFilter] = useState('ALL');
  const [sortFilter, setSortFilter] = useState('VIEWS');

  // Quick booking sidebar states
  const [quickMovieId, setQuickMovieId] = useState('');
  const [quickCinema, setQuickCinema] = useState('');
  const [quickDate, setQuickDate] = useState('');

  // 1. Like/View engagement state (Session reactive store)
  const [engagement, setEngagement] = useState({
    "Trấn Thành": { likes: 3500, views: 62000, liked: false },
    "Chad Stahelski": { likes: 2900, views: 51200, liked: false },
    "Jake Schreier": { likes: 1800, views: 32100, liked: false },
    "Phùng Đình Dũng": { likes: 1200, views: 21800, liked: false },
    "Charlie Nguyễn": { likes: 2700, views: 48900, liked: false },
    "Nguyễn Hữu Hoàng": { likes: 1450, views: 25400, liked: false },
    "Victor Vũ": { likes: 3100, views: 56900, liked: false },
    "Lý Hải": { likes: 3800, views: 71000, liked: false },
    "Julius Onah": { likes: 1100, views: 19800, liked: false },
    "Jared Hess": { likes: 1500, views: 27200, liked: false },
    "James Gunn": { likes: 3400, views: 61800, liked: false },
    "Yuzuru Tachikawa": { likes: 1900, views: 34500, liked: false }
  });

  // Country mapping mapping helper
  const getDirectorCountry = (name) => {
    const vnDirectors = ["Trấn Thành", "Phùng Đình Dũng", "Charlie Nguyễn", "Nguyễn Hữu Hoàng", "Victor Vũ", "Lý Hải"];
    return vnDirectors.includes(name) ? "Việt Nam" : "Mỹ";
  };

  // Structured metrics database for deep dossier
  const directorMetrics = {
    "Trấn Thành": { birth: "05/02/1987", height: "1m70", nationality: "Việt Nam", tags: ["Trăm Tỷ", "Tài Năng", "Kỷ Lục"] },
    "Chad Stahelski": { birth: "20/09/1968", height: "1m85", nationality: "Mỹ", tags: ["John Wick", "Hành Động", "Võ Thuật"] },
    "Jake Schreier": { birth: "29/09/1981", height: "1m80", nationality: "Mỹ", tags: ["Marvel", "Hiện Đại", "Cảm Xúc"] },
    "Phùng Đình Dũng": { birth: "12/03/1980", height: "1m72", nationality: "Việt Nam", tags: ["Hoạt Hinh", "Dân Gian", "Mỹ Thuật"] },
    "Charlie Nguyễn": { birth: "25/09/1968", height: "1m73", nationality: "Mỹ / Việt Nam", tags: ["Hành Động", "Hài Hước", "Kinh Nghiệm"] },
    "Nguyễn Hữu Hoàng": { birth: "08/11/1991", height: "1m76", nationality: "Việt Nam", tags: ["Trinh Thám", "Học Đường", "Góc Quay Đẹp"] },
    "Victor Vũ": { birth: "25/11/1975", height: "1m75", nationality: "Việt Nam / Mỹ", tags: ["Kinh Dị", "Giật Gân", "Chuyển Thể"] },
    "Lý Hải": { birth: "28/09/1968", height: "1m69", nationality: "Việt Nam", tags: ["Lật Mặt", "Gia Đình", "Mộc Mạc"] },
    "Julius Onah": { birth: "10/02/1983", height: "1m78", nationality: "Mỹ / Nigeria", tags: ["Sci-Fi", "Marvel", "Chính Trị"] },
    "Jared Hess": { birth: "20/06/1979", height: "1m82", nationality: "Mỹ", tags: ["Minecraft", "Hài Hước", "Độc Đáo"] },
    "James Gunn": { birth: "05/08/1966", height: "1m83", nationality: "Mỹ", tags: ["Superman", "Sáng Tạo", "Vũ Trụ"] },
    "Yuzuru Tachikawa": { birth: "02/12/1981", height: "1m70", nationality: "Nhật Bản", tags: ["Anime", "Conan", "Kịch Tính"] }
  };

  // Fictional biography details database
  const getDirectorBiography = (name) => {
    const bios = {
      "Trấn Thành": "Là một trong những nhà làm phim thương mại thành công nhất lịch sử điện ảnh Việt Nam. Anh nắm giữ nhiều bộ phim có doanh thu phòng vé phá kỷ lục và tư duy nghệ thuật hiện đại thấu cảm người xem.\n\nTại LoraFilm, các tác phẩm của anh luôn thu hút đông đảo mọi gia đình Việt và thúc đẩy các cuộc thảo luận văn hóa xã hội sâu sắc.",
      "Chad Stahelski": "Đạo diễn và cựu võ sư đóng thế huyền thoại của Hollywood, người tạo nên cuộc cách mạng trong dòng phim hành động đấu súng qua loạt tác phẩm John Wick.\n\nSự chỉ đạo nghệ thuật võ thuật và thiết kế ánh sáng Neon độc đáo của anh mang đến các màn hành động tuyệt đỉnh chất lượng cao nhất cho rạp chiếu LoraFilm.",
      "Jake Schreier": "Đạo diễn tài năng Mỹ nổi bật với phong cách kể chuyện tập trung vào tâm lý nhân vật đương đại và hình ảnh sắc sảo. Anh được Marvel tin tưởng giao trọng trách thực hiện các bom tấn thế hệ mới.\n\nCác suất chiếu phim anh làm đạo diễn luôn để lại ấn tượng mạnh mẽ cho khán giả trẻ năng động tại hệ thống rạp.",
      "Phùng Đình Dũng": "Đạo diễn và nhà hoạt họa tâm huyết đã dành nhiều năm nghiên cứu mang truyện cổ dân gian lên màn ảnh rộng 3D hoành tráng.\n\nTác phẩm dế mèn phiêu lưu ký của anh mang màu sắc kỳ ảo kết hợp kỹ thuật hàng đầu thế giới cống hiến giá trị nghệ thuật đích thực.",
      "Charlie Nguyễn": "Nhà làm phim tiên phong mở đường cho kỷ nguyên phim hài hành động bom tấn chiếu rạp của điện ảnh nước nhà từ những năm 2010.\n\nKinh nghiệm làm việc chuyên nghiệp chuẩn Hollywood cùng các tình huống hài thông minh tạo dựng vị thế không thể lay chuyển của anh.",
      "Nguyễn Hữu Hoàng": "Đạo diễn đại diện cho làn sóng trẻ tiên phong dám thử thách các đề tài tâm lý giật gân, bí ẩn học đường phá án.\n\nCác tác phẩm của anh có nhịp phim nhanh kết hợp âm hưởng ma mị độc đáo chiếm trọn cảm tình của giới học sinh sinh viên.",
      "Victor Vũ": "Bậc thầy điện ảnh chuyên trị các dòng phim kinh dị, giật gân ly kỳ kết hợp các yếu tố văn hóa tâm linh truyền thống Việt Nam.\n\nSự duy mỹ trong từng khung hình và cốt truyện lắt léo bất ngờ luôn biến các buổi chiếu phim của anh thành trải nghiệm giật gân đỉnh cao.",
      "Lý Hải": "Hiện tượng đặc biệt của điện ảnh nước nhà khi đi từ ca sĩ chuyển hướng làm đạo diễn thành công chuỗi bom tấn Lật Mặt vang danh.\n\nAnh chinh phục hàng triệu con tim nhờ lối kể chuyện mộc mạc, bình dị chứa chan tình mẫu tử gia đình chân thành nhất.",
      "Julius Onah": "Đạo diễn người Mỹ gốc Phi sở hữu tư duy khoa học viễn tưởng sắc sảo kết hợp các vấn đề xã hội thời đại vào trong dòng phim siêu anh hùng.\n\nAnh mang lại các trận giao đấu kịch tính cùng thế giới chính trị đầy bí ẩn hứa hẹn bùng nổ màn ảnh rộng.",
      "Jared Hess": "Đạo diễn kỳ cựu chuyên trị những bộ phim hài độc lạ xoay quanh những nhân vật phản anh hùng cá tính đáng yêu.\n\nSự nhào nặn tài tình thế giới trò chơi khối vuông Minecraft của anh thành phim live-action mang lại tiếng cười giòn giã cho trẻ em.",
      "James Gunn": "Vị đạo diễn tài ba bậc nhất Hollywood hiện tại, người có công tái sinh cả một vũ trụ siêu anh hùng vĩ đại.\n\nTư duy hài hước độc đáo kết hợp âm nhạc thập niên kinh niên biến mỗi bộ phim của anh thành lễ hội điện ảnh rực rỡ sắc màu.",
      "Yuzuru Tachikawa": "Đạo diễn Anime thiên tài người Nhật Bản nổi tiếng với việc chỉ đạo các pha rượt đuổi, phá án kỹ xảo hành động vô cùng hoành tráng.\n\nTác phẩm trinh thám Conan dưới sự nhào nặn của anh luôn đạt doanh thu phòng vé khổng lồ trên toàn thế giới."
    };

    return bios[name] || `Là nhà làm phim tài năng sở hữu nhiều cống hiến nổi bật và các tác phẩm điện ảnh xuất sắc mang tầm ảnh hưởng lớn tại hệ thống phòng vé LoraFilm.`;
  };

  const getDirectorGallery = (name, avatar) => {
    const base = [
      avatar,
      "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=400&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?w=400&auto=format&fit=crop&q=80"
    ];
    return base;
  };

  // 1. The Reverse-Graph Casting Algorithm (Client-Side) - Extracts unique director profiles from movies
  const consolidatedDirectors = useMemo(() => {
    const registry = {};

    movies.forEach(movie => {
      if (movie.director && movie.director.name) {
        const name = movie.director.name;
        if (!registry[name]) {
          registry[name] = {
            name,
            avatarUrl: movie.director.avatarUrl,
            bioSummary: movie.director.bioSummary,
            metrics: movie.director.metrics,
            filmography: []
          };
        }
        registry[name].filmography.push({
          id: movie.id,
          title: movie.title,
          posterUrl: movie.posterUrl || movie.image,
          status: movie.status,
          rating: movie.rating
        });
      }
    });

    return Object.values(registry);
  }, [movies]);

  // 2. Filter & Sort directors for the List view
  const processedDirectorsList = useMemo(() => {
    let result = consolidatedDirectors;

    // A. Apply Search Query
    if (searchQuery.trim()) {
      result = result.filter(dir =>
        dir.name.toLowerCase().includes(searchQuery.toLowerCase())
      );
    }

    // B. Apply Country Filter
    if (countryFilter !== 'ALL') {
      result = result.filter(dir => {
        const country = getDirectorCountry(dir.name);
        return countryFilter === 'VN' ? country === 'Việt Nam' : country !== 'Việt Nam';
      });
    }

    // C. Apply Sorting
    result = [...result].sort((a, b) => {
      const engA = engagement[a.name] || { likes: 0, views: 0 };
      const engB = engagement[b.name] || { likes: 0, views: 0 };
      
      if (sortFilter === 'LIKES') {
        return engB.likes - engA.likes;
      }
      return engB.views - engA.views;
    });

    return result;
  }, [consolidatedDirectors, searchQuery, countryFilter, sortFilter, engagement]);

  // Find targeted director if detail view is active
  const targetDirectorData = useMemo(() => {
    if (!directorName) return null;
    return consolidatedDirectors.find(dir => dir.name === directorName) || null;
  }, [consolidatedDirectors, directorName]);

  // Click on like pill toggle
  const handleLikeClick = (e, name) => {
    e.stopPropagation();
    setEngagement(prev => {
      const current = prev[name] || { likes: 0, views: 0, liked: false };
      const nextLiked = !current.liked;
      return {
        ...prev,
        [name]: {
          ...current,
          liked: nextLiked,
          likes: nextLiked ? current.likes + 1 : current.likes - 1
        }
      };
    });
  };

  // Launch direct booking payload from director filmography timeline
  const handleDirectBook = (movie) => {
    const bookingPayload = {
      movieId: movie.id,
      movieTitle: movie.title,
      cinema: 'Lora Nguyen Du',
      time: '19:30',
      format: '2D DIGITAL',
      date: new Date().toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }),
      fullDate: new Date().toLocaleDateString('vi-VN'),
      selectedSeats: []
    };
    onBookTicket(bookingPayload);
  };

  const cinemaClusters = useMemo(() => {
    return cinemas.map(c => c.name);
  }, [cinemas]);

  // Submit quick booking sidebar widget
  const handleQuickBookingSubmit = (e) => {
    e.preventDefault();
    if (!quickMovieId || !quickCinema || !quickDate) return;

    const matchedMovie = movies.find(m => String(m.id) === String(quickMovieId));
    if (!matchedMovie) return;

    const bookingPayload = {
      movieId: matchedMovie.id,
      movieTitle: matchedMovie.title,
      cinema: quickCinema,
      time: '19:30',
      format: '2D DIGITAL',
      date: quickDate === 'Hôm nay' 
        ? new Date().toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
        : new Date(Date.now() + 86400000).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }),
      fullDate: quickDate === 'Hôm nay' 
        ? new Date().toLocaleDateString('vi-VN')
        : new Date(Date.now() + 86400000).toLocaleDateString('vi-VN'),
      selectedSeats: []
    };
    onBookTicket(bookingPayload);
  };

  return (
    <div className="bg-zinc-950 text-zinc-100 min-h-screen py-8 px-4 md:px-8">
      <div className="max-w-7xl mx-auto space-y-8">

        {/* Global Page Title Bar */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-zinc-900">
          <div>
            <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">Đạo Diễn Điện Ảnh</h1>
            <p className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider mt-1">
              Khám phá chân dung các nhà làm phim thiên tài đứng sau những tác phẩm kiệt tác
            </p>
          </div>
          <button
            onClick={onBackHome}
            className="text-xs font-bold text-zinc-500 hover:text-brand-coral transition-colors self-start sm:self-center focus:outline-none"
          >
            Quay lại trang chủ
          </button>
        </div>

        {/* ASYMMETRIC MULTI-COLUMN LAYOUT FRAMEWORK */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* LEFT PANEL (2/3 Width) */}
          <div className="lg:col-span-2 space-y-8">

            {!directorName ? (
              // ============================================
              // 1. THE DIRECTOR LIST SUB-VIEW (Màn Hình Danh Sách)
              // ============================================
              <div className="space-y-6">
                
                {/* Upper Filter Bar with Country and Sort option dropdowns */}
                <div className="flex flex-col sm:flex-row items-center gap-4 bg-zinc-900 border border-zinc-800 rounded-2xl p-4 shadow-xl justify-between">
                  <div className="relative w-full max-w-xs">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" />
                    <input
                      type="text"
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      placeholder="Tìm đạo diễn..."
                      className="bg-zinc-950 border border-zinc-800 focus:border-amber-500 text-white rounded-xl pl-10 pr-4 py-2.5 w-full text-xs font-semibold focus:outline-none transition-colors"
                    />
                  </div>

                  <div className="flex items-center gap-3 w-full sm:w-auto">
                    {/* Country Selector Dropdown */}
                    <select
                      value={countryFilter}
                      onChange={(e) => setCountryFilter(e.target.value)}
                      className="bg-zinc-950 border border-zinc-850 text-zinc-300 text-xs font-bold rounded-xl py-2.5 px-4 focus:border-amber-500 focus:outline-none transition-colors w-1/2 sm:w-40"
                    >
                      <option value="ALL">Tất cả quốc tịch</option>
                      <option value="VN">Việt Nam</option>
                      <option value="INT">Quốc tế</option>
                    </select>

                    {/* Sorting Selector Dropdown */}
                    <select
                      value={sortFilter}
                      onChange={(e) => setSortFilter(e.target.value)}
                      className="bg-zinc-950 border border-zinc-855 text-zinc-300 text-xs font-bold rounded-xl py-2.5 px-4 focus:border-amber-500 focus:outline-none transition-colors w-1/2 sm:w-44"
                    >
                      <option value="VIEWS">Xem Nhiều Nhất</option>
                      <option value="LIKES">Được Thích Nhiều Nhất</option>
                    </select>
                  </div>
                </div>

                {/* Vertical Row List container */}
                {processedDirectorsList.length === 0 ? (
                  <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-12 text-center space-y-3">
                    <Film className="w-12 h-18 text-zinc-700 mx-auto animate-pulse" />
                    <h3 className="text-sm font-black uppercase tracking-wider text-white">Không tìm thấy đạo diễn điện ảnh</h3>
                    <p className="text-zinc-500 text-xs max-w-sm mx-auto">
                      Vui lòng thử lại với các lựa chọn lọc hoặc từ khóa tìm kiếm khác.
                    </p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {processedDirectorsList.map((director, idx) => {
                      const eng = engagement[director.name] || { likes: 0, views: 0, liked: false };
                      const bioText = getDirectorBiography(director.name).split('\n\n')[0];
                      const country = getDirectorCountry(director.name);

                      return (
                        <div
                          key={idx}
                          onClick={() => onNavigate('director-detail', { directorName: director.name })}
                          className="w-full bg-zinc-900 hover:bg-zinc-900/60 border border-zinc-800/80 hover:border-zinc-700/80 rounded-2xl p-4 flex flex-col sm:flex-row gap-6 cursor-pointer transition-all duration-300 shadow-lg group relative overflow-hidden"
                        >
                          {/* Left Thumbnail picture slot */}
                          <div className="w-full sm:w-40 aspect-[3/4] rounded-xl border border-zinc-800 shrink-0 overflow-hidden bg-zinc-950 relative">
                            <img
                              src={director.avatarUrl}
                              alt={director.name}
                              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                              onError={(e) => {
                                e.target.onerror = null;
                                e.target.src = 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=300&auto=format&fit=crop&q=80';
                              }}
                            />
                            {/* Country badge overlay */}
                            <span className="absolute top-2 left-2 text-[8px] font-black uppercase tracking-wider bg-black/60 backdrop-blur-md text-zinc-300 px-2 py-0.5 rounded border border-zinc-800/50">
                              {country}
                            </span>
                          </div>

                          {/* Right Information Section */}
                          <div className="flex flex-col justify-between flex-1 py-1 space-y-4 sm:space-y-0">
                            <div>
                              
                              {/* Header block with Likes Toggle */}
                              <div className="flex justify-between items-start gap-4">
                                <h3 className="text-base md:text-lg font-black text-white hover:text-orange-500 transition-colors group-hover:text-amber-500">
                                  {director.name}
                                </h3>

                                <div className="flex items-center gap-3">
                                  {/* View Counter tag */}
                                  <span className="text-[10px] text-zinc-500 font-bold">
                                    {eng.views.toLocaleString()} lượt xem
                                  </span>

                                  {/* Thích action pill trigger */}
                                  <button
                                    onClick={(e) => handleLikeClick(e, director.name)}
                                    className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-[9px] font-black uppercase tracking-wider transition-all duration-200 border focus:outline-none ${
                                      eng.liked 
                                        ? 'bg-blue-600 border-blue-500 text-white shadow-lg shadow-blue-600/15'
                                        : 'bg-zinc-950 border-zinc-800 text-zinc-450 hover:text-white hover:bg-zinc-800'
                                    }`}
                                  >
                                    <Heart className={`w-3 h-3 ${eng.liked ? 'fill-current' : ''}`} />
                                    <span>{eng.likes}</span>
                                  </button>
                                </div>
                              </div>

                              {/* Introductory text overview paragraph */}
                              <p className="text-xs text-zinc-400 leading-relaxed mt-3 line-clamp-3">
                                {bioText}
                              </p>

                            </div>

                            {/* Click invitation bar */}
                            <div className="text-[9px] font-black uppercase tracking-widest text-amber-500/80 group-hover:text-amber-500 transition-colors flex items-center gap-1">
                              <span>Xem thông tin hồ sơ chi tiết</span>
                              <span>➔</span>
                            </div>

                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}

              </div>
            ) : (
              // ============================================
              // 2. THE DEEP DIRECTOR PORTFOLIO DETAIL SUB-VIEW
              // ============================================
              <div className="space-y-8 animate-in fade-in duration-300">
                
                {/* Breadcrumb Navigator */}
                <div className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider flex items-center gap-1.5 bg-zinc-900/40 p-3 rounded-xl border border-zinc-900/50">
                  <button onClick={onBackHome} className="hover:text-white transition-colors focus:outline-none">Trang chủ</button>
                  <span>/</span>
                  <button onClick={() => onNavigate('directors', null)} className="hover:text-white transition-colors focus:outline-none">Đạo diễn</button>
                  <span>/</span>
                  <span className="text-amber-500">{directorName}</span>
                </div>

                {targetDirectorData ? (
                  <div className="space-y-8">
                    
                    {/* Upper Identity Row Block */}
                    <div className="flex flex-col md:flex-row gap-8 items-start bg-zinc-900 border border-zinc-800 rounded-3xl p-6 shadow-xl relative overflow-hidden">
                      <div className="absolute top-0 right-0 w-64 h-64 bg-amber-500/5 rounded-full blur-3xl pointer-events-none" />

                      {/* Left Column Snapshot */}
                      <img
                        src={targetDirectorData.avatarUrl}
                        alt={targetDirectorData.name}
                        className="w-full md:w-56 aspect-[3/4] rounded-2xl object-cover border border-zinc-800 shadow-2xl shrink-0"
                        onError={(e) => {
                          e.target.onerror = null;
                          e.target.src = 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=300&auto=format&fit=crop&q=80';
                        }}
                      />

                      {/* Right Column Identity parameters */}
                      <div className="space-y-4 flex-1">
                        <div className="space-y-2">
                          <h2 className="text-2xl font-black text-white uppercase tracking-wider">{targetDirectorData.name}</h2>
                          
                          {/* Tags row */}
                          <div className="flex flex-wrap gap-2.5">
                            {directorMetrics[targetDirectorData.name]?.tags.map((tag, tIdx) => (
                              <span
                                key={tIdx}
                                className="bg-blue-950/80 border border-blue-500/10 text-blue-400 text-[8px] font-black uppercase tracking-wider px-2.5 py-1 rounded-full"
                              >
                                {tag}
                              </span>
                            ))}
                          </div>
                        </div>

                        {/* Brief summary paragraph */}
                        <p className="text-xs text-zinc-300 leading-relaxed">
                          {getDirectorBiography(targetDirectorData.name).split('\n\n')[0]}
                        </p>

                        {/* Bio metrics bullets */}
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-3 border-t border-zinc-800 text-xs font-semibold text-zinc-400">
                          <div className="flex items-center gap-2">
                            <Calendar className="w-4 h-4 text-zinc-500 shrink-0" />
                            <span>Ngày sinh: <strong className="text-zinc-200">{directorMetrics[targetDirectorData.name]?.birth || "01/01/1980"}</strong></span>
                          </div>
                          <div className="flex items-center gap-2">
                            <Ruler className="w-4 h-4 text-zinc-500 shrink-0" />
                            <span>Chiều cao: <strong className="text-zinc-200">{directorMetrics[targetDirectorData.name]?.height || "1m75"}</strong></span>
                          </div>
                          <div className="flex items-center gap-2">
                            <MapPin className="w-4 h-4 text-zinc-500 shrink-0" />
                            <span>Quốc tịch: <strong className="text-zinc-200">{directorMetrics[targetDirectorData.name]?.nationality || "Mỹ"}</strong></span>
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* "HÌNH ẢNH" Block */}
                    <div className="space-y-3">
                      <div className="flex items-center gap-2 border-b border-zinc-800 pb-2.5">
                        <Film className="w-4 h-4 text-amber-500 shrink-0" />
                        <h3 className="text-xs font-black text-white uppercase tracking-wider">HÌNH ẢNH HOẠT ĐỘNG</h3>
                      </div>
                      
                      {/* 4 Photo thumbnails */}
                      <div className="grid grid-cols-4 gap-4 py-2">
                        {getDirectorGallery(targetDirectorData.name, targetDirectorData.avatarUrl).map((imgUrl, imgIdx) => (
                          <div
                            key={imgIdx}
                            className="aspect-square rounded-xl overflow-hidden border border-zinc-800/80 bg-zinc-900 group cursor-zoom-in relative"
                          >
                            <img
                              src={imgUrl}
                              alt={`${targetDirectorData.name} still ${imgIdx}`}
                              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                            />
                            <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                              <span className="text-[8px] font-black uppercase text-white bg-zinc-950/80 px-2 py-1 rounded border border-zinc-800">Zoom</span>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* "PHIM ĐÃ THAM GIA" Grid Matrix Layout */}
                    <div className="space-y-4">
                      <div className="flex items-center gap-2 border-b border-zinc-800 pb-2.5">
                        <Award className="w-4 h-4 text-amber-500 shrink-0" />
                        <h3 className="text-xs font-black text-white uppercase tracking-wider">Tác phẩm đã đạo diễn</h3>
                      </div>

                      {/* 2-column list layout */}
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        {targetDirectorData.filmography.map((work, wIdx) => (
                          <div
                            key={wIdx}
                            className="bg-zinc-900 border border-zinc-800 rounded-2xl p-3.5 flex items-center justify-between gap-4 hover:border-zinc-700 transition-colors"
                          >
                            <div className="flex items-center gap-3">
                              <div className="w-12 h-18 rounded-lg overflow-hidden shrink-0 bg-zinc-950 border border-zinc-800">
                                <img src={work.posterUrl} alt={work.title} className="w-full h-full object-cover" />
                              </div>
                              <div className="space-y-1">
                                <h4 className="text-xs font-black text-white leading-tight line-clamp-1">
                                  {work.title}
                                </h4>
                                <p className="text-[10px] text-zinc-400 font-semibold line-clamp-1">
                                  Vai trò: <span className="text-amber-500">Đạo diễn</span>
                                </p>
                                <div className="flex items-center gap-1.5">
                                  <span className={`text-[8px] font-black uppercase tracking-wider px-1.5 py-0.5 rounded ${
                                    work.status === 'NOW_SHOWING' || work.status === 'DANG_CHIEU'
                                      ? 'bg-emerald-950/80 text-emerald-400 border border-emerald-500/10' 
                                      : 'bg-blue-950/80 text-blue-400 border border-blue-500/10'
                                  }`}>
                                    {work.status === 'NOW_SHOWING' || work.status === 'DANG_CHIEU' ? 'Đang chiếu' : 'Sắp chiếu'}
                                  </span>
                                  <div className="flex items-center gap-0.5 text-brand-yellow">
                                    <Star className="w-2.5 h-2.5 fill-brand-yellow text-brand-yellow" />
                                    <span className="text-[9px] font-bold text-zinc-400">{work.rating}</span>
                                  </div>
                                </div>
                              </div>
                            </div>

                            {/* Direct Booking CTA */}
                            {work.status === 'NOW_SHOWING' || work.status === 'DANG_CHIEU' ? (
                              <button
                                onClick={() => handleDirectBook(work)}
                                className="bg-amber-500 hover:bg-amber-600 text-black text-[10px] font-black uppercase py-2 px-3 rounded-xl transition-colors shrink-0 shadow-lg shadow-amber-500/10 focus:outline-none"
                              >
                                Đặt Vé
                              </button>
                            ) : (
                              <span className="text-[9px] font-bold text-zinc-600 uppercase tracking-wider shrink-0 pr-2">
                                Sắp Chiếu
                              </span>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* "TIỂU SỬ" Block (Bottom Layout Section) */}
                    <div className="space-y-3 bg-zinc-900 border border-zinc-800 rounded-3xl p-6 shadow-xl">
                      <div className="flex items-center gap-2 border-b border-zinc-800 pb-2.5">
                        <Award className="w-4 h-4 text-amber-500 shrink-0" />
                        <h3 className="text-xs font-black text-white uppercase tracking-wider">Tiểu sử chi tiết</h3>
                      </div>
                      <div className="space-y-4 pt-2">
                        {getDirectorBiography(targetDirectorData.name).split('\n\n').map((paragraph, pIdx) => (
                          <p key={pIdx} className="text-[11px] text-zinc-400 leading-relaxed">
                            {paragraph}
                          </p>
                        ))}
                      </div>
                    </div>

                  </div>
                ) : (
                  <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-12 text-center">
                    <h3 className="text-sm font-black uppercase tracking-wider text-white">Không tìm thấy hồ sơ đạo diễn</h3>
                  </div>
                )}

              </div>
            )}

          </div>

          {/* RIGHT PANEL (1/3 Width) */}
          <div className="space-y-6 lg:sticky lg:top-24 h-fit">
            
            {/* Widget A: "Mua Vé Nhanh" (Sequential Booking Dropdown Card) */}
            <div className="bg-zinc-900 border border-zinc-850 rounded-2xl overflow-hidden shadow-2xl">
              
              {/* Blue/zinc header card style */}
              <div className="bg-gradient-to-r from-blue-700 to-indigo-900 text-white font-black text-xs uppercase tracking-wider py-4 px-5 shadow-inner flex items-center justify-between">
                <span>Mua Vé Nhanh</span>
                <Film className="w-4 h-4 text-white/50" />
              </div>

              {/* Form Body */}
              <form onSubmit={handleQuickBookingSubmit} className="p-5 space-y-4">
                
                {/* 1. Chọn Phim Dropdown */}
                <div className="space-y-1.5">
                  <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider block">1. Chọn Phim</label>
                  <select
                    value={quickMovieId}
                    onChange={(e) => {
                      setQuickMovieId(e.target.value);
                      setQuickCinema('');
                      setQuickDate('');
                    }}
                    className="w-full bg-zinc-950 border border-zinc-800 text-zinc-200 text-xs font-semibold rounded-xl py-3 px-3.5 focus:border-blue-600 focus:outline-none transition-colors"
                  >
                    <option value="">-- Chọn Phim --</option>
                    {movies.filter(m => m.status === 'NOW_SHOWING' || m.status === 'DANG_CHIEU').map(m => (
                      <option key={m.id} value={m.id}>{m.title}</option>
                    ))}
                  </select>
                </div>

                {/* 2. Chọn Rạp Dropdown */}
                <div className="space-y-1.5">
                  <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider block">2. Chọn Rạp</label>
                  <select
                    disabled={!quickMovieId}
                    value={quickCinema}
                    onChange={(e) => {
                      setQuickCinema(e.target.value);
                      setQuickDate('');
                    }}
                    className={`w-full bg-zinc-950 border text-xs font-semibold rounded-xl py-3 px-3.5 focus:outline-none transition-colors ${
                      quickMovieId 
                        ? 'border-zinc-800 text-zinc-200 focus:border-blue-600 cursor-default' 
                        : 'border-zinc-900 text-zinc-655 cursor-not-allowed select-none'
                    }`}
                  >
                    <option value="">-- Chọn Rạp --</option>
                    {cinemaClusters.map(c => (
                      <option key={c} value={c}>{c}</option>
                    ))}
                  </select>
                </div>

                {/* 3. Chọn Ngày Dropdown */}
                <div className="space-y-1.5">
                  <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider block">3. Chọn Ngày Chiếu</label>
                  <select
                    disabled={!quickCinema}
                    value={quickDate}
                    onChange={(e) => setQuickDate(e.target.value)}
                    className={`w-full bg-zinc-950 border text-xs font-semibold rounded-xl py-3 px-3.5 focus:outline-none transition-colors ${
                      quickCinema 
                        ? 'border-zinc-800 text-zinc-200 focus:border-blue-600 cursor-default' 
                        : 'border-zinc-900 text-zinc-655 cursor-not-allowed select-none'
                    }`}
                  >
                    <option value="">-- Chọn Ngày --</option>
                    <option value="Hôm nay">Hôm nay</option>
                    <option value="Ngày mai">Ngày mai</option>
                  </select>
                </div>

                {/* Submit Action Button */}
                <button
                  type="submit"
                  disabled={!quickMovieId || !quickCinema || !quickDate}
                  className={`w-full font-black py-3.5 rounded-xl text-xs uppercase tracking-wider transition-all duration-300 ${
                    quickMovieId && quickCinema && quickDate
                      ? 'bg-blue-600 hover:bg-blue-700 text-white cursor-pointer shadow-lg shadow-blue-600/20 active:scale-[0.98]'
                      : 'bg-zinc-800 text-zinc-500 cursor-not-allowed select-none'
                  }`}
                >
                  Mua Vé Nhanh
                </button>

              </form>

            </div>

            {/* Widget B: "PHIM ĐANG CHIẾU" (Mini Promotion Grid Banner) */}
            <div className="bg-zinc-900 border border-zinc-850 rounded-2xl p-5 space-y-4 shadow-2xl">
              
              <div className="border-b border-zinc-800 pb-2 flex justify-between items-center">
                <span className="text-white text-[10px] font-black uppercase tracking-wider">Phim Đang Chiếu</span>
                <span className="text-[8px] font-black uppercase tracking-widest text-brand-yellow animate-pulse">Hot Now</span>
              </div>

              {/* Stack list */}
              <div className="space-y-4">
                {movies.slice(0, 3).map((movie) => (
                  <div 
                    key={movie.id}
                    onClick={() => handleDirectBook(movie)}
                    className="flex gap-3 hover:bg-white/5 p-1.5 rounded-xl transition-colors cursor-pointer group"
                  >
                    {/* Poster */}
                    <div className="w-12 h-18 rounded-lg overflow-hidden shrink-0 bg-zinc-950 border border-zinc-800">
                      <img src={movie.posterUrl || movie.image} alt={movie.title} className="w-full h-full object-cover" />
                    </div>

                    {/* Details */}
                    <div className="space-y-1.5 flex flex-col justify-center">
                      <h4 className="text-xs font-extrabold text-zinc-200 group-hover:text-brand-coral transition-colors line-clamp-1">
                        {movie.title}
                      </h4>
                      <div className="flex items-center gap-2">
                        <span className="text-[8px] font-black uppercase bg-zinc-950 border border-zinc-800 text-brand-yellow px-1.5 py-0.5 rounded">
                          {movie.ageRating || 'T16'}
                        </span>
                        <div className="flex items-center gap-0.5">
                          <Star className="w-3 h-3 fill-brand-yellow text-brand-yellow" />
                          <span className="text-[9px] font-bold text-zinc-400">{movie.rating}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

            </div>

          </div>

        </div>

      </div>

    </div>
  );
}
