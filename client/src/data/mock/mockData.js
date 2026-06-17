// Centralized Unified Mock Database Store for LoraFilm Management System

export const INITIAL_MOVIES = [
  {
    id: "m1",
    title: "Định Mệnh Là Yêu",
    rating: 4.5,
    genres: ["Lãng Mạn", "Hài Hước"],
    genre: "Lãng Mạn, Hài Hước",
    duration: 112,
    ageRating: "T13",
    releaseYear: 2026,
    status: "DANG_CHIEU",
    synopsis: "Câu chuyện tình yêu cực kỳ ngọt ngào và hài hước giữa hai người trẻ tưởng chừng như không có điểm chung. Những tình huống dở khóc dở cười diễn ra liên tiếp khiến họ nhận ra đối phương là mảnh ghép hoàn hảo của cuộc đời mình.",
    posterUrl: "https://images.unsplash.com/photo-1524712245354-2c4e5e7124c5?w=600&auto=format&fit=crop&q=80",
    image: "https://images.unsplash.com/photo-1524712245354-2c4e5e7124c5?w=600&auto=format&fit=crop&q=80",
    trailerId: "eHp3MbsQgzk",
    trailerEmbedUrl: "https://www.youtube.com/embed/eHp3MbsQgzk",
    actorIds: ["a3", "a4"],
    director: {
      name: "Trấn Thành",
      avatarUrl: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80",
      bioSummary: "Đạo diễn kiêm nhà sản xuất phim xuất sắc của điện ảnh Việt Nam với doanh thu nghìn tỷ.",
      metrics: { birth: "05/02/1987", height: "1m70", nationality: "Việt Nam", tags: ["Đa Tài", "Kỷ Lục", "Truyền Cảm Hứng"] }
    }
  },
  {
    id: "m2",
    title: "Từ Vũ Trụ John Wick: Ballerina",
    rating: 4.8,
    genres: ["Hành Động", "Kịch Tính"],
    genre: "Hành Động, Kịch Tính",
    duration: 125,
    ageRating: "T18",
    releaseYear: 2026,
    status: "DANG_CHIEU",
    synopsis: "Bộ phim thương mại spin-off từ vũ trụ sát thủ John Wick, xoay quanh nữ sát thủ Eve Macarro trên con đường trả thù cho gia đình bị sát hại. Cô phải đối mặt với cả thế giới ngầm và rèn luyện các kỹ năng chiến đấu sinh tử để sinh tồn.",
    posterUrl: "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
    image: "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
    trailerId: "yJ9w2zD244U",
    trailerEmbedUrl: "https://www.youtube.com/embed/yJ9w2zD244U",
    actorIds: ["a1", "a2"],
    director: {
      name: "Chad Stahelski",
      avatarUrl: "https://images.unsplash.com/photo-1560250097-0b93528c311a?w=300&auto=format&fit=crop&q=80",
      bioSummary: "Đạo diễn hành động Hollywood nổi tiếng đứng sau loạt bom tấn John Wick hoành tráng.",
      metrics: { birth: "20/09/1968", height: "1m85", nationality: "Mỹ", tags: ["John Wick", "Hành Động", "Võ Thuật"] }
    }
  },
  {
    id: "m3",
    title: "Biệt Đội Sấm Sét",
    rating: 4.2,
    genres: ["Hành Động", "Viễn Tưởng"],
    genre: "Hành Động, Viễn Tưởng",
    duration: 130,
    ageRating: "T16",
    releaseYear: 2026,
    status: "DANG_CHIEU",
    synopsis: "Biệt đội anti-hero mới của Marvel gồm những kẻ bất hảo hợp lực làm nhiệm vụ tối mật cho chính phủ. Tập hợp những tính cách lập dị nhưng có kỹ năng đặc biệt hứa hẹn mang đến những trận chiến mãn nhãn và đầy bất ngờ.",
    posterUrl: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
    image: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
    trailerId: "vB8VvA88d6I",
    trailerEmbedUrl: "https://www.youtube.com/embed/vB8VvA88d6I",
    actorIds: ["a5", "a6"],
    director: {
      name: "Jake Schreier",
      avatarUrl: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=300&auto=format&fit=crop&q=80",
      bioSummary: "Nhà làm phim trẻ tài năng nổi tiếng với lối kể chuyện hiện đại và các tác phẩm Marvel.",
      metrics: { birth: "29/09/1981", height: "1m80", nationality: "Mỹ", tags: ["Marvel", "Hiện Đại", "Cảm Xúc"] }
    }
  },
  {
    id: "m4",
    title: "Dế Mèn Cuộc Phiêu Lưu Ký Thú",
    rating: 4.0,
    genres: ["Hoạt Hình", "Phiêu Lưu"],
    genre: "Hoạt Hình, Phiêu Lưu",
    duration: 95,
    ageRating: "P",
    releaseYear: 2025,
    status: "DANG_CHIEU",
    synopsis: "Cuộc hành trình phiêu lưu kỳ thú của chú Dế Mèn trẻ tuổi, học hỏi những bài học quý giá về tình bạn, lòng dũng cảm và hoài bão tương lai. Đi qua những vùng đất xa lạ và vượt qua những hiểm nguy đầy rẫy trong thiên nhiên.",
    posterUrl: "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80",
    image: "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80",
    trailerId: "fS_f0n-dF3Y",
    trailerEmbedUrl: "https://www.youtube.com/embed/fS_f0n-dF3Y",
    actorIds: ["a4", "a7"],
    director: {
      name: "Phùng Đình Dũng",
      avatarUrl: "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=300&auto=format&fit=crop&q=80",
      bioSummary: "Đạo diễn phim hoạt hình hàng đầu Việt Nam mang nét vẽ dân gian hội nhập thế giới.",
      metrics: { birth: "12/03/1980", height: "1m72", nationality: "Việt Nam", tags: ["Hoạt Hinh", "Dân Gian", "Tỉ Mỉ"] }
    }
  },
  {
    id: "m5",
    title: "Buôn Thần Bán Thánh",
    rating: 4.9,
    genres: ["Kinh Dị", "Tâm Linh"],
    genre: "Kinh Dị, Tâm Linh",
    duration: 108,
    ageRating: "T18",
    releaseYear: 2026,
    status: "DANG_CHIEU",
    synopsis: "Câu chuyện kinh dị tâm linh giữa gia đình và âm linh đối cổ. Sự xen kẽ giữa những yếu tố hài hước và rùng rợn kiến tạo cảm xúc thú vị. Liệu sự giàu có có đến từ những thỏa thuận xấu xa với thế lực ở âm ty?",
    posterUrl: "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
    image: "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
    trailerId: "R-v3W0Yp3dE",
    trailerEmbedUrl: "https://www.youtube.com/embed/R-v3W0Yp3dE",
    actorIds: ["a3", "a7"],
    director: {
      name: "Charlie Nguyễn",
      avatarUrl: "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=300&auto=format&fit=crop&q=80",
      bioSummary: "Đạo diễn kỳ cựu đi đầu trong thể loại hài hành động và tâm lý sâu sắc của Việt Nam.",
      metrics: { birth: "25/09/1968", height: "1m73", nationality: "Mỹ / Việt Nam", tags: ["Hài Hước", "Hành Động", "Kinh Nghiệm"] }
    }
  },
  {
    id: "m6",
    title: "Nam Muội",
    rating: 4.1,
    genres: ["Tâm Lý", "Học Đường"],
    genre: "Tâm Lý, Học Đường",
    duration: 100,
    ageRating: "T16",
    releaseYear: 2026,
    status: "DANG_CHIEU",
    synopsis: "Nam Muội là một bộ phim kinh dị học đường tâm lý, xoay quanh truyền thuyết đô thị ma mị và những góc khuất học đường đáng sợ không thể ngờ tới. Trò chơi tâm linh vô tình gọi lên oan hồn đói khát sự trả thù.",
    posterUrl: "https://images.unsplash.com/photo-1516627145497-ae6968895b74?w=600&auto=format&fit=crop&q=80",
    image: "https://images.unsplash.com/photo-1516627145497-ae6968895b74?w=600&auto=format&fit=crop&q=80",
    trailerId: "3Z8vX90i-bU",
    trailerEmbedUrl: "https://www.youtube.com/embed/3Z8vX90i-bU",
    actorIds: ["a4", "a7"],
    director: {
      name: "Nguyễn Hữu Hoàng",
      avatarUrl: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&auto=format&fit=crop&q=80",
      bioSummary: "Đạo diễn tài năng trẻ tiên phong trong dòng phim trinh thám học đường ly kỳ bí ẩn.",
      metrics: { birth: "08/11/1991", height: "1m76", nationality: "Việt Nam", tags: ["Trinh Thám", "Học Đường", "Góc Quay Đẹp"] }
    }
  },
  {
    id: "m7",
    title: "Thám Tử Kiên: Kỳ Án Buồng Bầu",
    rating: 4.6,
    genres: ["Trinh Thám", "Hình Sự"],
    genre: "Trinh Thám, Hình Sự",
    duration: 118,
    ageRating: "T16",
    releaseYear: 2026,
    status: "DANG_CHIEU",
    synopsis: "Thám tử Kiên vào cuộc điều tra kỳ án ẩn giấu nơi buồng bầu tôn nghiêm của dòng họ quý tộc. Sự thật phức tạp dần được bóc trần qua mỗi manh mối suy luận thông thái nhưng đầy hiểm nguy.",
    posterUrl: "https://images.unsplash.com/photo-1509248961158-e54f6934749c?w=600&auto=format&fit=crop&q=80",
    image: "https://images.unsplash.com/photo-1509248961158-e54f6934749c?w=600&auto=format&fit=crop&q=80",
    trailerId: "1Z_v7Gq7948",
    trailerEmbedUrl: "https://www.youtube.com/embed/1Z_v7Gq7948",
    actorIds: ["a3", "a7"],
    director: {
      name: "Victor Vũ",
      avatarUrl: "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=300&auto=format&fit=crop&q=80",
      bioSummary: "Ông hoàng phim giật gân, tâm linh và chuyển thể văn học của Việt Nam với các góc quay đỉnh cao.",
      metrics: { birth: "25/11/1975", height: "1m75", nationality: "Việt Nam / Mỹ", tags: ["Kinh Dị", "Giật Gân", "Chuyển Thể"] }
    }
  },
  {
    id: "m8",
    title: "Mufasa: Vua Sư Tử",
    rating: 4.7,
    genres: ["Phiêu Lưu", "Gia Đình"],
    genre: "Phiêu Lưu, Gia Đình",
    duration: 120,
    ageRating: "P",
    releaseYear: 2025,
    status: "DANG_CHIEU",
    synopsis: "Khai thác quá khứ của Mufasa, từ một chú sư tử con mồ côi trở thành vị vua huyền thoại của vùng đất kiêu hãnh.",
    posterUrl: "https://images.unsplash.com/photo-1546182990-dffeafbe841d?w=600&auto=format&fit=crop&q=80",
    image: "https://images.unsplash.com/photo-1546182990-dffeafbe841d?w=600&auto=format&fit=crop&q=80",
    trailerId: "o17MF994zMs",
    trailerEmbedUrl: "https://www.youtube.com/embed/o17MF994zMs",
    actorIds: ["a8"],
    director: {
      name: "Barry Jenkins",
      avatarUrl: "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=300&auto=format&fit=crop&q=80",
      bioSummary: "Đạo diễn nổi tiếng thắng giải Oscar với lối kể chuyện giàu chiều sâu điện ảnh và nghệ thuật.",
      metrics: { birth: "24/09/1979", height: "1m78", nationality: "Mỹ", tags: ["Oscar", "Nghệ Thuật", "Cảm Xúc"] }
    }
  },
  {
    id: "m9",
    title: "Captain America: Thế Giới Mới",
    rating: 4.3,
    genres: ["Hành Động", "Viễn Tưởng"],
    genre: "Hành Động, Viễn Tưởng",
    duration: 122,
    ageRating: "T13",
    releaseYear: 2026,
    status: "SAP_CHIEU",
    synopsis: "Sam Wilson chính thức khoác lên mình chiếc khiên Captain America để đối mặt với một âm mưu chính trị nguy hiểm toàn cầu. Những cuộc xung đột không khoan nhượng.",
    posterUrl: "https://images.unsplash.com/photo-1478720143022-9057235c7443?w=600&auto=format&fit=crop&q=80",
    image: "https://images.unsplash.com/photo-1478720143022-9057235c7443?w=600&auto=format&fit=crop&q=80",
    trailerId: "lh8Lal1N14A",
    trailerEmbedUrl: "https://www.youtube.com/embed/lh8Lal1N14A",
    actorIds: ["a5"],
    director: {
      name: "Julius Onah",
      avatarUrl: "https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?w=300&auto=format&fit=crop&q=80",
      bioSummary: "Đạo diễn và nhà biên kịch người Mỹ gốc Nigeria chịu trách nhiệm về tương lai MCU.",
      metrics: { birth: "10/02/1983", height: "1m78", nationality: "Mỹ / Nigeria", tags: ["Sci-Fi", "Marvel", "Chính Trị"] }
    }
  },
  {
    id: "m10",
    title: "Sonic 3: Thế Nhím",
    rating: 4.4,
    genres: ["Hoạt Hình", "Hành Động"],
    genre: "Hoạt Hình, Hành Động",
    duration: 110,
    ageRating: "P",
    releaseYear: 2025,
    status: "SAP_CHIEU",
    synopsis: "Sonic trở lại trong cuộc đối đầu mới chống lại kẻ thù kiệt xuất Shadow. Tập hợp đồng minh cùng lúc xuất trận.",
    posterUrl: "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80",
    image: "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80",
    trailerId: "qSu6i2iFES0",
    trailerEmbedUrl: "https://www.youtube.com/embed/qSu6i2iFES0",
    actorIds: ["a9"],
    director: {
      name: "Jeff Fowler",
      avatarUrl: "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=300&auto=format&fit=crop&q=80",
      bioSummary: "Đạo diễn kiêm họa sĩ VFX người Mỹ đứng sau loạt phim live-action Sonic ăn khách.",
      metrics: { birth: "27/07/1978", height: "1m80", nationality: "Mỹ", tags: ["Sonic", "Live-action", "Hài Hước"] }
    }
  },
  {
    id: "m11",
    title: "Kỵ Binh Bất Tử",
    rating: 4.3,
    genres: ["Viễn Tưởng", "Hành Động"],
    genre: "Viễn Tưởng, Hành Động",
    duration: 142,
    ageRating: "T18",
    releaseYear: 2026,
    status: "SAP_CHIEU",
    synopsis: "Bộ phim khoa học viễn tưởng về đội kỵ binh mang gen bất tử bảo vệ nền văn minh cổ đại khỏi sự diệt vong.",
    posterUrl: "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
    image: "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
    trailerId: "nh5Yp4cTz98",
    trailerEmbedUrl: "https://www.youtube.com/embed/nh5Yp4cTz98",
    actorIds: ["a2"],
    director: {
      name: "Gina Prince-Bythewood",
      avatarUrl: "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=300&auto=format&fit=crop&q=80",
      bioSummary: "Nữ đạo diễn Mỹ cá tính đứng sau những bộ phim chính kịch hành động sâu sắc.",
      metrics: { birth: "10/06/1969", height: "1m72", nationality: "Mỹ", tags: ["Nữ Quyền", "Hành Động", "Kịch Bản Đẹp"] }
    }
  },
  {
    id: "m12",
    title: "Chuyến Tàu Vô Cực",
    rating: 4.5,
    genres: ["Giả Tưởng", "Kịch Tính"],
    genre: "Giả Tưởng, Kịch Tính",
    duration: 135,
    ageRating: "T16",
    releaseYear: 2026,
    status: "SAP_CHIEU",
    synopsis: "Khai thác bối cảnh chuyến tàu lượn lách giữa những chiều không gian song song để tìm kiếm mảnh đất hứa.",
    posterUrl: "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
    image: "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
    trailerId: "1Z_v7Gq7948",
    trailerEmbedUrl: "https://www.youtube.com/embed/1Z_v7Gq7948",
    actorIds: ["a1"],
    director: {
      name: "Haruo Sotozaki",
      avatarUrl: "https://images.unsplash.com/photo-1527980965255-d3b416303d12?w=300&auto=format&fit=crop&q=80",
      bioSummary: "Đạo diễn danh giá đứng sau bom tấn hoạt hình chuyển thể làm rung chuyển màn ảnh.",
      metrics: { birth: "18/04/1970", height: "1m74", nationality: "Nhật Bản", tags: ["Anime", "Kỷ Lục", "Hoành Tráng"] }
    }
  }
];

export const INITIAL_ACTORS = [
  {
    id: "a1",
    name: "Ana de Armas",
    nationality: "Cuba/Tây Ban Nha",
    avatarUrl: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80",
    bio: "Ana de Armas sinh ra tại Cuba và là một trong những minh tinh Hollywood nổi tiếng nhất hiện nay. Cô ghi dấu ấn mạnh mẽ qua các vai diễn trong Blade Runner 2049, Knives Out, Blonde và mới nhất là tác phẩm hành động gay cấn từ vũ trụ John Wick mang tên Ballerina.",
    linkedMovieIds: ["m2", "m12"]
  },
  {
    id: "a2",
    name: "Keanu Reeves",
    nationality: "Canada",
    avatarUrl: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80",
    bio: "Keanu Charles Reeves là một nam diễn viên, nhà sản xuất phim, đạo diễn và nhạc sĩ người Canada. Anh nổi tiếng thế giới qua loạt phim hành động võ thuật kinh điển The Matrix (Ma Trận) và vũ trụ sát thủ John Wick.",
    linkedMovieIds: ["m2", "m11"]
  },
  {
    id: "a3",
    name: "Trấn Thành",
    nationality: "Việt Nam",
    avatarUrl: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80",
    bio: "Trấn Thành là đạo diễn, biên kịch, diễn viên và MC nổi bật hàng đầu tại Việt Nam. Anh là người tạo nên những kỷ lục phòng vé nghìn tỷ của điện ảnh nước nhà với các bộ phim Bố Già, Nhà Bà Nữ, Mai và Định Mệnh Là Yêu.",
    linkedMovieIds: ["m1", "m5", "m7"]
  },
  {
    id: "a4",
    name: "Kaity Nguyễn",
    nationality: "Việt Nam",
    avatarUrl: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&auto=format&fit=crop&q=80",
    bio: "Kaity Nguyễn là một nữ diễn viên trẻ tài năng thuộc thế hệ mới của điện ảnh Việt Nam. Cô nhanh chóng vươn lên thành sao hạng A sau những bộ phim điện ảnh ăn khách như Em Chưa 18, Tiệc Trăng Máu, Gái Già Lắm Chiêu V và Người Vợ Cuối Cùng.",
    linkedMovieIds: ["m1", "m4", "m6"]
  },
  {
    id: "a5",
    name: "Sebastian Stan",
    nationality: "Mỹ",
    avatarUrl: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&auto=format&fit=crop&q=80",
    bio: "Sebastian Stan là nam diễn viên người Mỹ gốc Romania. Anh nổi tiếng toàn cầu với vai diễn Bucky Barnes / Winter Soldier trong vũ trụ điện ảnh Marvel (MCU), bắt đầu từ Captain America: The First Avenger.",
    linkedMovieIds: ["m3", "m9"]
  },
  {
    id: "a6",
    name: "Florence Pugh",
    nationality: "Anh",
    avatarUrl: "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400&auto=format&fit=crop&q=80",
    bio: "Florence Pugh là nữ diễn viên người Anh xuất sắc từng nhận đề cử giải Oscar. Cô ghi dấu ấn mạnh mẽ qua các bộ phim như Midsommar, Little Women, Black Widow và Oppenheimer.",
    linkedMovieIds: ["m3"]
  },
  {
    id: "a7",
    name: "Kiều Minh Tuấn",
    nationality: "Việt Nam",
    avatarUrl: "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=400&auto=format&fit=crop&q=80",
    bio: "Kiều Minh Tuấn là nam diễn viên điện ảnh thực lực của Việt Nam. Anh được biết đến rộng rãi với lối diễn xuất đa dạng từ vai hài hước tới tâm lý phức tạp qua các bộ phim Em Chưa 18, Chị Mười Ba, Tiệc Trăng Máu, Lật Mặt 7.",
    linkedMovieIds: ["m4", "m5", "m6", "m7"]
  },
  {
    id: "a8",
    name: "Aaron Pierre",
    nationality: "Anh",
    avatarUrl: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&auto=format&fit=crop&q=80",
    bio: "Aaron Pierre là nam diễn viên người Anh lồng tiếng xuất sắc cho nhân vật Mufasa thời trẻ trong bom tấn Mufasa: The Lion King của Disney.",
    linkedMovieIds: ["m8"]
  },
  {
    id: "a9",
    name: "Ben Schwartz",
    nationality: "Mỹ",
    avatarUrl: "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400&auto=format&fit=crop&q=80",
    bio: "Ben Schwartz là một nam diễn viên hài, biên kịch người Mỹ nổi tiếng lồng tiếng cho nhân vật Sonic trong loạt phim điện ảnh Sonic the Hedgehog.",
    linkedMovieIds: ["m10"]
  }
];

export const INITIAL_CINEMAS = [
  {
    id: "c1",
    name: "Lora Nguyễn Du",
    address: "116 Nguyễn Du, Quận 1, TP. Hồ Chí Minh",
    halls: [
      { id: "h1-1", name: "Phòng Chiếu 1 (IMAX)", capacity: 120, format: "IMAX 3D" },
      { id: "h1-2", name: "Phòng Chiếu 2 (Standard)", capacity: 120, format: "2D Digital" }
    ]
  },
  {
    id: "c2",
    name: "Lora Thảo Điền",
    address: "Vincom Mega Mall, Thảo Điền, Quận 2, TP. Hồ Chí Minh",
    halls: [
      { id: "h2-1", name: "Phòng Chiếu 3 (Gold Class)", capacity: 120, format: "2D Digital" },
      { id: "h2-2", name: "Phòng Chiếu 4 (Standard)", capacity: 120, format: "3D Digital" }
    ]
  },
  {
    id: "c3",
    name: "Lora Royal City",
    address: "B2-R4 Vincom Mega Mall, Thanh Xuân, Hà Nội",
    halls: [
      { id: "h3-1", name: "Phòng Chiếu 5 (IMAX)", capacity: 120, format: "IMAX 3D" },
      { id: "h3-2", name: "Phòng Chiếu 6 (Standard)", capacity: 120, format: "2D Digital" }
    ]
  }
];

export const INITIAL_SHOWTIMES = [
  { id: "st1", movieId: "m1", cinemaId: "c1", hallId: "h1-2", date: "2026-05-29", time: "09:30", price: 80000 },
  { id: "st2", movieId: "m1", cinemaId: "c1", hallId: "h1-2", date: "2026-05-29", time: "13:15", price: 80000 },
  { id: "st3", movieId: "m1", cinemaId: "c1", hallId: "h1-2", date: "2026-05-29", time: "16:45", price: 90000 },
  { id: "st4", movieId: "m2", cinemaId: "c1", hallId: "h1-1", date: "2026-05-29", time: "13:15", price: 150000 },
  { id: "st5", movieId: "m2", cinemaId: "c2", hallId: "h2-2", date: "2026-05-29", time: "19:30", price: 120000 },
  { id: "st6", movieId: "m5", cinemaId: "c1", hallId: "h1-2", date: "2026-05-29", time: "20:00", price: 100000 }
];

export const INITIAL_EVENTS = [
  {
    id: "e1",
    title: "Đồng Giá Vé Lora Thứ Hai - 60k",
    type: "PROMOTION",
    status: "DANG_DIEN_RA",
    dateRange: "2026-06-01 - 2026-06-30",
    rewardDetails: "Giá vé áp dụng toàn bộ suất chiếu 2D vào ngày thứ 2 hàng tuần chỉ 60.000 VNĐ."
  },
  {
    id: "e2",
    title: "Ngày Hội Thành Viên Lora Member Day",
    type: "MEMBER_DISCOUNT",
    status: "DANG_DIEN_RA",
    dateRange: "2026-06-01 - 2026-12-31",
    rewardDetails: "Nhân đôi điểm thưởng tích lũy cho mỗi giao dịch mua vé và bắp nước vào thứ Tư đầu tiên của tháng."
  },
  {
    id: "e3",
    title: "Sự Kiện Ra Mắt John Wick Spin-off",
    type: "EVENT",
    status: "SAP_DIEN_RA",
    dateRange: "2026-06-15 - 2026-06-20",
    rewardDetails: "Quà tặng độc quyền mini poster Ballerina cho 100 khách hàng đầu tiên check-in tại rạp."
  }
];

export const INITIAL_CONCESSIONS = [
  { id: 1, name: "Combo Solo", details: "1 Bắp Ngọt Vừa + 1 Coke Vừa", price: 79000, salesCount: 10 },
  { id: 2, name: "Combo Couple", details: "1 Bắp Ngọt Lớn + 2 Coke Vừa", price: 109000, salesCount: 10 },
  { id: 3, name: "Combo Family", details: "2 Bắp Ngọt Lớn + 3 Coke Lớn", price: 189000, salesCount: 5 },
  { id: 4, name: "Bắp Phô Mai", details: "1 Bắp vừa vị phô mai đặc biệt", price: 49000, salesCount: 29 },
  { id: 5, name: "Coca Cola", details: "1 Lon Coca Cola mát lạnh", price: 29000, salesCount: 106 }
];

export const INITIAL_TICKETS = [
  {
    id: "TKT-8492-9582",
    customerName: "Lê Văn Sơn",
    customerEmail: "leson@gmail.com",
    movieTitle: "Từ Vũ Trụ John Wick: Ballerina",
    theaterName: "Lora Nguyễn Du",
    time: "19:30",
    date: "2026-05-29",
    seats: ["F4", "F5"],
    totalAmount: 220000,
    status: "DA_KIEM_TRA",
    timestamp: "2026-05-29T10:15:30Z"
  },
  {
    id: "TKT-1082-4829",
    customerName: "Trần Thị Hoa",
    customerEmail: "hoatran@gmail.com",
    movieTitle: "Định Mệnh Là Yêu",
    theaterName: "Lora Nguyễn Du",
    time: "09:30",
    date: "2026-05-29",
    seats: ["A12"],
    totalAmount: 80000,
    status: "CHUA_KIEM_TRA",
    timestamp: "2026-05-29T08:44:12Z"
  }
];

export const INITIAL_CUSTOMERS = [
  { id: 1, name: "Phạm Minh Đức", email: "member@gmail.com", tier: "Standard", points: 150, ticketsBought: 5 },
  { id: 2, name: "Lê Văn Sơn", email: "leson@gmail.com", tier: "VIP", points: 920, ticketsBought: 28 },
  { id: 3, name: "Nguyễn Thị Mai", email: "mai.nguyen@yahoo.com", tier: "Standard", points: 80, ticketsBought: 2 },
  { id: 4, name: "Trần Văn Bảo", email: "baotran@gmail.com", tier: "VIP", points: 1200, ticketsBought: 45 }
];

export const INITIAL_EMPLOYEES = [
  { id: 1, name: "Nguyễn Văn Hà", role: "Thu ngân", email: "staff@lorafilm.com", hoursWorked: 160, hourlyWage: 25000, activeMultiplier: 1.0 },
  { id: 2, name: "Trần Thị Cúc", role: "Giám sát", email: "cuc.tran@lorafilm.com", hoursWorked: 180, hourlyWage: 40000, activeMultiplier: 1.1 },
  { id: 3, name: "Lê Quang Sáng", role: "Thu ngân", email: "sang.le@lorafilm.com", hoursWorked: 140, hourlyWage: 25000, activeMultiplier: 1.0 }
];

// Backwards compatibility legacy exports
export const MOVIES = INITIAL_MOVIES;
export const movies = INITIAL_MOVIES;
export const CINEMA_CLUSTERS = [
  "Lora Nguyễn Du",
  "Lora Thảo Điền",
  "Lora Royal City"
];
export const SHOWTIMES = [
  "09:30",
  "13:15",
  "16:45",
  "19:30",
  "22:15"
];

export const SYSTEM_SETTINGS = {
  maxDelay: 15,
  autoClose: true,
  imaxSurcharge: 50000,
  weekendMultiplier: 1.2
};
