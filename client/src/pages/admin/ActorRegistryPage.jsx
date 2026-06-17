import { useState, useMemo } from 'react';
import { Search, Film, Award, Star, Heart, Calendar, MapPin, Ruler } from 'lucide-react';
import { useData } from '../../contexts/DataContext';

export default function ActorRegistryView({ actorName, onBackHome, onBookTicket, onNavigate }) {
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
    "Kaity Nguyễn": { likes: 1420, views: 24500, liked: false },
    "Trấn Thành": { likes: 3105, views: 56200, liked: false },
    "Kiều Minh Tuấn": { likes: 2540, views: 42100, liked: false },
    "Ana de Armas": { likes: 1980, views: 38400, liked: false },
    "Keanu Reeves": { likes: 5400, views: 98000, liked: false },
    "Florence Pugh": { likes: 1890, views: 35200, liked: false },
    "Sebastian Stan": { likes: 2200, views: 42100, liked: false },
    "Anthony Mackie": { likes: 1750, views: 31900, liked: false },
    "Jack Black": { likes: 2650, views: 49800, liked: false },
    "Jason Momoa": { likes: 2980, views: 54100, liked: false },
    "David Corenswet": { likes: 1450, views: 27800, liked: false },
    "Rachel Brosnahan": { likes: 1520, views: 28900, liked: false }
  });

  // Country mapping mapping helper
  const getActorCountry = (name) => {
    const vnActors = ["Kaity Nguyễn", "Trấn Thành", "Kiều Minh Tuấn"];
    return vnActors.includes(name) ? "Việt Nam" : "Mỹ";
  };

  // Structured metrics database for deep dossier
  const actorMetrics = {
    "Kaity Nguyễn": { birth: "05/03/1999", height: "1m50", nationality: "Việt Nam", tags: ["Ngọc Nữ", "Phòng Vé", "Thực Lực"] },
    "Trấn Thành": { birth: "05/02/1987", height: "1m70", nationality: "Việt Nam", tags: ["Đa Tài", "Đạo Diễn", "Kỷ Lục"] },
    "Kiều Minh Tuấn": { birth: "26/02/1988", height: "1m74", nationality: "Việt Nam", tags: ["Biến Hóa", "Kịch Tính", "Thực Lực"] },
    "Ana de Armas": { birth: "30/04/1988", height: "1m68", nationality: "Cuba", tags: ["Hành Động", "Quyến Rũ", "Oscar Nominated"] },
    "Keanu Reeves": { birth: "02/09/1964", height: "1m86", nationality: "Canada", tags: ["Bất Tử", "Hành Động", "Nhân Hậu"] },
    "Florence Pugh": { birth: "03/01/1996", height: "1m62", nationality: "Anh Quốc", tags: ["Cá Tính", "Gai Góc", "Thế Hệ Mới"] },
    "Sebastian Stan": { birth: "13/08/1982", height: "1m83", nationality: "Romania / Mỹ", tags: ["Marvel", "Chiến Binh", "Chính Kịch"] },
    "Anthony Mackie": { birth: "23/09/1978", height: "1m78", nationality: "Mỹ", tags: ["Marvel", "Captain America", "Năng Lượng"] },
    "Jack Black": { birth: "28/08/1969", height: "1m68", nationality: "Mỹ", tags: ["Hài Kịch", "Huyền Thoại", "Bùng Nổ"] },
    "Jason Momoa": { birth: "01/08/1979", height: "1m93", nationality: "Mỹ", tags: ["Cơ Bắp", "Hoang Dã", "Phiêu Lưu"] },
    "David Corenswet": { birth: "08/07/1993", height: "1m93", nationality: "Mỹ", tags: ["Superman", "Cổ Điển", "Tài Năng Trẻ"] },
    "Rachel Brosnahan": { birth: "12/07/1990", height: "1m61", nationality: "Mỹ", tags: ["Emmy Winner", "Lôi Cuốn", "Tài Sắc"] }
  };

  // Fictional narrative details database
  const getActorBiography = (name) => {
    const bios = {
      "Kaity Nguyễn": "Sinh năm 1999, Kaity Nguyễn nhanh chóng vươn lên thành một trong những ngọc nữ đắt giá nhất màn ảnh Việt. Sở hữu đôi mắt to tròn biết nói và khả năng diễn xuất bản năng thiên bẩm, cô liên tục khẳng định mình qua các tác phẩm phòng vé trăm tỷ và đạt nhiều giải thưởng danh giá trong nước.\n\nTại LoraFilm, cô không chỉ góp mặt trong những bộ phim tâm lý lãng mạn mà còn thử thách bản thân với các dự án hoạt hình lồng tiếng đầy màu sắc, chứng minh năng lực sáng tạo đa dạng và sức hút đại chúng bền bỉ.",
      "Trấn Thành": "Nghệ sĩ đa tài hàng đầu Việt Nam hoạt động năng nổ ở nhiều vai trò diễn viên kịch, MC, diễn viên điện ảnh và đạo diễn. Anh sở hữu tư duy làm phim thực tế sắc bén kết hợp cùng kỹ năng thấu hiểu tâm lý nhân vật tinh tế sâu sắc.\n\nTrấn Thành liên tục thiết lập các kỷ lục phòng vé lịch sử tại LoraFilm nhờ các tựa phim gia đình đầy cảm xúc mang tính hiện thực xã hội cao, tạo nên những cuộc thảo luận tích cực trên mọi diễn đàn.",
      "Kiều Minh Tuấn": "Là gương mặt gạo cội đại diện cho thế hệ diễn viên thực lực đỉnh cao. Anh ghi điểm sâu sắc trong lòng khán giả nhờ phong cách diễn xuất tự nhiên như hơi thở, biến hóa khôn lường từ hình tượng hài hước, bụi bặm phong trần cho đến những vai diễn phản diện độc đoán hoặc tâm lý phức tạp.\n\nSự hiện diện của anh trong bất kỳ dự án nào tại hệ thống rạp LoraFilm luôn là lời bảo chứng vững chắc nhất cho chất lượng nghệ thuật lẫn doanh thu thương mại của tác phẩm đó.",
      "Ana de Armas": "Nữ minh tinh người gốc Cuba nổi tiếng thế giới với vẻ ngoài quyến rũ chết người cùng ý chí bền bỉ vươn lên đỉnh cao Hollywood. Cô gây sốt toàn cầu qua các vai diễn hành động bắn súng giật gân kịch tính vô cùng điêu luyện.\n\nTại LoraFilm, cô mang đến làn gió mới rực lửa thông qua dự án hành động sát thủ Ballerina tầm cỡ thế giới ngầm, cống hiến những cảnh đấu tay đôi mãn nhãn vô tiền khoáng hậu.",
      "Keanu Reeves": "Biểu tượng hành động huyền thoại gắn liền với các thương hiệu điện ảnh lớn nhất thế kỷ 21. Anh nổi tiếng toàn cầu không chỉ vì tài năng trên phim trường mà còn vì tấm lòng nhân ái, hào phóng và lối sống giản dị bình dị giữa đời thường.\n\nMọi suất chiếu của nam tài tử tại LoraFilm đều chật kín phòng vé, chứng minh sức nóng chưa bao giờ giảm sút của người hùng cô độc đi tìm công lý.",
      "Florence Pugh": "Nữ diễn viên tài năng xuất chúng người Anh sở hữu hàng loạt vai diễn chính kịch nặng đô gai góc. Florence Pugh nổi bật với thần thái mạnh mẽ độc lập, giọng nói khàn cuốn hút đặc trưng và khả năng làm chủ khung hình xuất sắc.\n\nCô đại diện cho thế hệ diễn viên tiên phong mới, luôn mang đến những màn hóa thân đầy chiều sâu nội tâm dữ dội trên hệ thống rạp toàn quốc.",
      "Sebastian Stan": "Nam diễn viên thực lực người Romania khẳng định tên tuổi vững chắc qua cả dòng phim bom tấn giải trí lẫn nghệ thuật độc lập đầy thách thức. Anh ghi dấu ấn đậm nét nhất qua hình tượng người hùng cô độc sở hữu cánh tay kim loại.\n\nSebastian Stan mang đến khả năng biến hóa tâm lý phức tạp đầy ấn tượng, chinh phục những khán giả khó tính nhất tại LoraFilm.",
      "Anthony Mackie": "Tài tử da màu tràn đầy năng lượng tích cực với khả năng làm chủ sân khấu cực đỉnh. Anh hiện đảm nhận chiếc khiên đại diện cho công lý thế giới mới trong kỷ nguyên tiếp theo.\n\nSức hút nam tính, mạnh mẽ cùng các pha hành động không trung đẳng cấp của Anthony Mackie luôn mang lại cảm xúc bùng nổ tột độ cho người xem.",
      "Jack Black": "Ngôi sao hài kịch kiêm ca sĩ nhạc Rock huyền thoại sở hữu lối diễn bùng nổ, vui nhộn ngập tràn tiếng cười sảng khoái. Bất kỳ tác phẩm nào có sự góp mặt của anh đều mang đậm tính giải trí chất lượng cao.\n\nJack Black mang đến thế giới khối vuông Minecraft đầy sáng tạo và ngộ nghĩnh, thu hút hàng ngàn khán giả gia đình đến rạp.",
      "Jason Momoa": "Người hùng cơ bắp với mái tóc dài hoang dã bụi bặm đặc trưng của biển cả. Anh ghi dấu ấn qua hình tượng lực sĩ phóng khoáng cùng nụ cười ngạo nghễ cuốn hút.\n\nSự góp mặt của anh trong các dự án hành động phiêu lưu kỳ vĩ tại LoraFilm luôn mang lại sự phấn khích tột độ nhờ kỹ xảo và thể hình ấn tượng.",
      "David Corenswet": "Tài năng trẻ sáng giá mang vẻ đẹp cổ điển lịch lãm tựa như các tài tử thời hoàng kim Hollywood. Anh được tin tưởng giao phó trọng trách tái sinh hình tượng anh hùng mang tính biểu tượng toàn cầu.\n\nDavid Corenswet hứa hẹn sẽ mang đến những màn trình diễn tràn đầy hy vọng và công lý mãn nhãn nhất trong năm nay.",
      "Rachel Brosnahan": "Nữ diễn viên tài sắc vẹn toàn từng giành vô số giải thưởng truyền hình danh giá. Cô chinh phục người xem bởi nét thông minh hóm hỉnh rạng ngời và khả năng biểu cảm khuôn mặt đỉnh cao.\n\nSự đồng hành của cô trong các bom tấn tương lai hứa hẹn mang lại câu chuyện tình yêu và công lý tuyệt vời nhất tại LoraFilm."
    };

    return bios[name] || `Là ngôi sao điện ảnh tài năng sở hữu nhiều cống hiến nổi bật và các vai diễn để lại dấu ấn sâu sắc trong lòng khán giả yêu thích các dự án phim chiếu rạp tại LoraFilm. Tác phẩm của nghệ sĩ luôn truyền tải những thông điệp nghệ thuật tích cực và ý nghĩa.`;
  };

  // Unsplash gallery portraits for each actor
  const getActorGallery = (name, avatar) => {
    const base = [
      avatar,
      "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=400&auto=format&fit=crop&q=80",
      "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?w=400&auto=format&fit=crop&q=80"
    ];
    return base;
  };

  // 1. The Reverse-Graph Casting Algorithm (Client-Side)
  // Extracts unique actor profiles across all movies and consolidates their filmographies.
  const consolidatedActors = useMemo(() => {
    const registry = {};

    movies.forEach(movie => {
      if (movie.cast && Array.isArray(movie.cast)) {
        movie.cast.forEach(actor => {
          const name = actor.name;
          if (!registry[name]) {
            registry[name] = {
              name,
              avatarUrl: actor.avatarUrl,
              filmography: []
            };
          }
          // Push this movie's metadata into the star's portfolio
          registry[name].filmography.push({
            id: movie.id,
            title: movie.title,
            role: actor.role,
            posterUrl: movie.posterUrl || movie.image,
            status: movie.status,
            rating: movie.rating
          });
        });
      }
    });

    return Object.values(registry);
  }, [movies]);

  // 2. Filter & Sort actors for the List view
  const processedActorsList = useMemo(() => {
    let result = consolidatedActors;

    // A. Apply Search Query
    if (searchQuery.trim()) {
      result = result.filter(actor =>
        actor.name.toLowerCase().includes(searchQuery.toLowerCase())
      );
    }

    // B. Apply Country Filter
    if (countryFilter !== 'ALL') {
      result = result.filter(actor => {
        const country = getActorCountry(actor.name);
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
  }, [consolidatedActors, searchQuery, countryFilter, sortFilter, engagement]);

  // Find targeted actor if detail view is active
  const targetActorData = useMemo(() => {
    if (!actorName) return null;
    return consolidatedActors.find(actor => actor.name === actorName) || null;
  }, [consolidatedActors, actorName]);

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

  // Launch direct booking payload from actor portfolio timeline or mini promos
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
            <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">Ngôi Sao Điện Ảnh</h1>
            <p className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider mt-1">
              Gặp gỡ dàn diễn viên gạo cội và khám phá các vai diễn của họ tại LoraFilm
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

            {!actorName ? (
              // ============================================
              // 1. THE ACTOR LIST SUB-VIEW (Màn Hình Danh Sách)
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
                      placeholder="Tìm diễn viên..."
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
                {processedActorsList.length === 0 ? (
                  <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-12 text-center space-y-3">
                    <Film className="w-12 h-18 text-zinc-700 mx-auto animate-pulse" />
                    <h3 className="text-sm font-black uppercase tracking-wider text-white">Không tìm thấy ngôi sao điện ảnh</h3>
                    <p className="text-zinc-500 text-xs max-w-sm mx-auto">
                      Vui lòng thử lại với các lựa chọn lọc hoặc từ khóa tìm kiếm khác.
                    </p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {processedActorsList.map((actor, idx) => {
                      const eng = engagement[actor.name] || { likes: 0, views: 0, liked: false };
                      const bioText = getActorBiography(actor.name).split('\n\n')[0];
                      const country = getActorCountry(actor.name);

                      return (
                        <div
                          key={idx}
                          onClick={() => onNavigate('actor-detail', { actorName: actor.name })}
                          className="w-full bg-zinc-900 hover:bg-zinc-900/60 border border-zinc-800/80 hover:border-zinc-700/80 rounded-2xl p-4 flex flex-col sm:flex-row gap-6 cursor-pointer transition-all duration-300 shadow-lg group relative overflow-hidden"
                        >
                          {/* Left Thumbnail picture slot */}
                          <div className="w-full sm:w-40 aspect-[3/4] rounded-xl border border-zinc-800 shrink-0 overflow-hidden bg-zinc-950 relative">
                            <img
                              src={actor.avatarUrl}
                              alt={actor.name}
                              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                              onError={(e) => {
                                e.target.onerror = null;
                                e.target.src = 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&auto=format&fit=crop&q=80';
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
                                  {actor.name}
                                </h3>

                                <div className="flex items-center gap-3">
                                  {/* View Counter tag */}
                                  <span className="text-[10px] text-zinc-500 font-bold">
                                    {eng.views.toLocaleString()} lượt xem
                                  </span>

                                  {/* Thích action pill trigger */}
                                  <button
                                    onClick={(e) => handleLikeClick(e, actor.name)}
                                    className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-[9px] font-black uppercase tracking-wider transition-all duration-200 border focus:outline-none ${
                                      eng.liked 
                                        ? 'bg-blue-600 border-blue-500 text-white shadow-lg shadow-blue-600/15'
                                        : 'bg-zinc-950 border-zinc-800 text-zinc-400 hover:text-white hover:bg-zinc-800'
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
              // 2. THE DEEP ACTOR PORTFOLIO DETAIL SUB-VIEW
              // ============================================
              <div className="space-y-8 animate-in fade-in duration-300">
                
                {/* Breadcrumb Navigator */}
                <div className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider flex items-center gap-1.5 bg-zinc-900/40 p-3 rounded-xl border border-zinc-900/50">
                  <button onClick={onBackHome} className="hover:text-white transition-colors focus:outline-none">Trang chủ</button>
                  <span>/</span>
                  <button onClick={() => onNavigate('actors', null)} className="hover:text-white transition-colors focus:outline-none">Diễn viên</button>
                  <span>/</span>
                  <span className="text-amber-500">{actorName}</span>
                </div>

                {targetActorData ? (
                  <div className="space-y-8">
                    
                    {/* Upper Identity Row Block */}
                    <div className="flex flex-col md:flex-row gap-8 items-start bg-zinc-900 border border-zinc-800 rounded-3xl p-6 shadow-xl relative overflow-hidden">
                      <div className="absolute top-0 right-0 w-64 h-64 bg-amber-500/5 rounded-full blur-3xl pointer-events-none" />

                      {/* Left Column Snapshot */}
                      <img
                        src={targetActorData.avatarUrl}
                        alt={targetActorData.name}
                        className="w-full md:w-56 aspect-[3/4] rounded-2xl object-cover border border-zinc-800 shadow-2xl shrink-0"
                        onError={(e) => {
                          e.target.onerror = null;
                          e.target.src = 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&auto=format&fit=crop&q=80';
                        }}
                      />

                      {/* Right Column Identity parameters */}
                      <div className="space-y-4 flex-1">
                        <div className="space-y-2">
                          <h2 className="text-2xl font-black text-white uppercase tracking-wider">{targetActorData.name}</h2>
                          
                          {/* Tags row */}
                          <div className="flex flex-wrap gap-2.5">
                            {actorMetrics[targetActorData.name]?.tags.map((tag, tIdx) => (
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
                          {getActorBiography(targetActorData.name).split('\n\n')[0]}
                        </p>

                        {/* Bio metrics bullets */}
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-3 border-t border-zinc-800 text-xs font-semibold text-zinc-400">
                          <div className="flex items-center gap-2">
                            <Calendar className="w-4 h-4 text-zinc-500 shrink-0" />
                            <span>Ngày sinh: <strong className="text-zinc-200">{actorMetrics[targetActorData.name]?.birth || "01/01/1990"}</strong></span>
                          </div>
                          <div className="flex items-center gap-2">
                            <Ruler className="w-4 h-4 text-zinc-500 shrink-0" />
                            <span>Chiều cao: <strong className="text-zinc-200">{actorMetrics[targetActorData.name]?.height || "1m75"}</strong></span>
                          </div>
                          <div className="flex items-center gap-2">
                            <MapPin className="w-4 h-4 text-zinc-500 shrink-0" />
                            <span>Quốc tịch: <strong className="text-zinc-200">{actorMetrics[targetActorData.name]?.nationality || "Mỹ"}</strong></span>
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* "HÌNH ẢNH" Block */}
                    <div className="space-y-3">
                      <div className="flex items-center gap-2 border-b border-zinc-800 pb-2.5">
                        <Film className="w-4 h-4 text-amber-500 shrink-0" />
                        <h3 className="text-xs font-black text-white uppercase tracking-wider">HÌNH ẢNH NGHỆ THUẬT</h3>
                      </div>
                      
                      {/* 4 Photo thumbnails */}
                      <div className="grid grid-cols-4 gap-4 py-2">
                        {getActorGallery(targetActorData.name, targetActorData.avatarUrl).map((imgUrl, imgIdx) => (
                          <div
                            key={imgIdx}
                            className="aspect-square rounded-xl overflow-hidden border border-zinc-800/80 bg-zinc-900 group cursor-zoom-in relative"
                          >
                            <img
                              src={imgUrl}
                              alt={`${targetActorData.name} still ${imgIdx}`}
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
                        <h3 className="text-xs font-black text-white uppercase tracking-wider">Tác phẩm điện ảnh hợp tác</h3>
                      </div>

                      {/* 2-column list layout */}
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        {targetActorData.filmography.map((work, wIdx) => (
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
                                  Vai diễn: <span className="text-amber-500">{work.role}</span>
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
                    <div className="space-y-3 bg-zinc-900 border border-zinc-850 rounded-3xl p-6 shadow-xl">
                      <div className="flex items-center gap-2 border-b border-zinc-800 pb-2.5">
                        <Award className="w-4 h-4 text-amber-500 shrink-0" />
                        <h3 className="text-xs font-black text-white uppercase tracking-wider">Tiểu sử chi tiết</h3>
                      </div>
                      <div className="space-y-4 pt-2">
                        {getActorBiography(targetActorData.name).split('\n\n').map((paragraph, pIdx) => (
                          <p key={pIdx} className="text-[11px] text-zinc-400 leading-relaxed">
                            {paragraph}
                          </p>
                        ))}
                      </div>
                    </div>

                  </div>
                ) : (
                  <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-12 text-center">
                    <h3 className="text-sm font-black uppercase tracking-wider text-white">Không tìm thấy hồ sơ diễn viên</h3>
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
                        : 'border-zinc-900 text-zinc-650 cursor-not-allowed select-none'
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
                        : 'border-zinc-900 text-zinc-650 cursor-not-allowed select-none'
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
