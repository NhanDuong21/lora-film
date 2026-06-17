/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState } from 'react';

const DataContext = createContext();

const initialMovies = [
  {
    id: "1",
    title: "Dune: Phần Hai",
    status: "NOW_SHOWING",
    posterUrl: "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&auto=format&fit=crop&q=80",
    ageRating: "T13",
    genres: ["Khoa Học Viễn Tưởng", "Hành Động", "Phiêu Lưu"],
    trailerEmbedUrl: "https://www.youtube.com/embed/Way9Dexny3w",
    cast: [
      { name: "Timothée Chalamet", avatarUrl: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop&q=80" },
      { name: "Zendaya", avatarUrl: "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=100&auto=format&fit=crop&q=80" }
    ]
  },
  {
    id: "2",
    title: "Mai",
    status: "NOW_SHOWING",
    posterUrl: "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?w=500&auto=format&fit=crop&q=80",
    ageRating: "T18",
    genres: ["Tâm Lý", "Tình Cảm"],
    trailerEmbedUrl: "https://www.youtube.com/embed/EX6clvId19s",
    cast: [
      { name: "Phương Anh Đào", avatarUrl: "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=100&auto=format&fit=crop&q=80" },
      { name: "Tuấn Trần", avatarUrl: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&auto=format&fit=crop&q=80" }
    ]
  },
  {
    id: "3",
    title: "Godzilla x Kong: Đế Chế Mới",
    status: "NOW_SHOWING",
    posterUrl: "https://images.unsplash.com/photo-1568832359672-e36cf5d74f54?w=500&auto=format&fit=crop&q=80",
    ageRating: "K",
    genres: ["Hành Động", "Khoa Học Viễn Tưởng"],
    trailerEmbedUrl: "https://www.youtube.com/embed/lV1OOlGwExM",
    cast: [
      { name: "Rebecca Hall", avatarUrl: "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=100&auto=format&fit=crop&q=80" },
      { name: "Dan Stevens", avatarUrl: "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=100&auto=format&fit=crop&q=80" }
    ]
  },
  {
    id: "4",
    title: "Kung Fu Panda 4",
    status: "NOW_SHOWING",
    posterUrl: "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=500&auto=format&fit=crop&q=80",
    ageRating: "P",
    genres: ["Hoạt Hình", "Hài Hước", "Gia Đình"],
    trailerEmbedUrl: "https://www.youtube.com/embed/fTlgVlK253M",
    cast: [
      { name: "Jack Black", avatarUrl: "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=100&auto=format&fit=crop&q=80" },
      { name: "Awkwafina", avatarUrl: "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=100&auto=format&fit=crop&q=80" }
    ]
  },
  {
    id: "5",
    title: "Deadpool & Wolverine",
    status: "COMING_SOON",
    posterUrl: "https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?w=500&auto=format&fit=crop&q=80",
    ageRating: "T18",
    genres: ["Hành Động", "Hài Hước", "Viễn Tưởng"],
    trailerEmbedUrl: "https://www.youtube.com/embed/73_1biulkYw",
    cast: [
      { name: "Ryan Reynolds", avatarUrl: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&auto=format&fit=crop&q=80" },
      { name: "Hugh Jackman", avatarUrl: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100&auto=format&fit=crop&q=80" }
    ]
  },
  {
    id: "6",
    title: "Inside Out 2",
    status: "COMING_SOON",
    posterUrl: "https://images.unsplash.com/photo-1608889175123-8ec330b86f84?w=500&auto=format&fit=crop&q=80",
    ageRating: "P",
    genres: ["Hoạt Hình", "Gia Đình", "Hài Hước"],
    trailerEmbedUrl: "https://www.youtube.com/embed/LEjhY28558A",
    cast: [
      { name: "Amy Poehler", avatarUrl: "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=100&auto=format&fit=crop&q=80" }
    ]
  }
];

const initialTheaters = [
  { id: "1", name: "Lora Nguyễn Du", location: "Quận 1, TP. HCM" },
  { id: "2", name: "Lora Nguyễn Trãi", location: "Quận 5, TP. HCM" },
  { id: "3", name: "Lora Hùng Vương", location: "Quận 5, TP. HCM" },
  { id: "4", name: "Lora Quang Trung", location: "Gò Vấp, TP. HCM" }
];

const initialEvents = [
  {
    id: "e1",
    title: "Thứ Hai Vui Vẻ - Đồng Giá Vé 60K",
    dateRange: "Đến 31/12/2026",
    rewardDetails: "Cơ hội thưởng thức phim bom tấn với giá cực hời mỗi ngày thứ ba hàng tuần tại hệ thống LoraFilm."
  },
  {
    id: "e2",
    title: "Thành Viên Vàng LoraFilm - Nhân Đôi Điểm Tích Lũy",
    dateRange: "Đến 30/06/2026",
    rewardDetails: "Nhận x2 điểm thưởng khi mua vé online trong suốt tháng 6."
  },
  {
    id: "e3",
    title: "Combo Bắp Nước Siêu Anh Hùng - Tặng Bình Nước",
    dateRange: "Đến 31/12/2026",
    rewardDetails: "Nhận bình nước độc quyền khi mua Combo phim bom tấn."
  }
];

const initialActors = [
  { id: "a1", name: "Timothée Chalamet", avatarUrl: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop&q=80", birthDate: "1995-12-27" },
  { id: "a2", name: "Ryan Reynolds", avatarUrl: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&auto=format&fit=crop&q=80", birthDate: "1976-10-23" }
];

const initialShowtimes = [
  { id: "s1", movieId: "1", theaterId: "1", date: new Date().toLocaleDateString('vi-VN'), time: "19:30" },
  { id: "s2", movieId: "2", theaterId: "1", date: new Date().toLocaleDateString('vi-VN'), time: "22:15" }
];

const initialTickets = [
  { id: "t1", bookingId: "B001", seatCode: "H7", price: 90000, date: new Date().toLocaleDateString('vi-VN') }
];

const initialConcessions = [
  { id: "c1", name: "Bắp Phô Mai Lớn", price: 55000, stock: 150 },
  { id: "c2", name: "Nước Ngọt Lớn", price: 35000, stock: 200 }
];

const initialCustomers = [
  { id: "cu1", fullName: "Nguyễn Văn A", email: "user@example.com", phoneNumber: "0901234567" }
];

const initialEmployees = [
  { id: "em1", name: "Trần Nhân Viên", email: "staff@lorafilm.com", role: "ROLE_STAFF" }
];

export function DataProvider({ children }) {
  const [movies, setMovies] = useState(initialMovies);
  const [theaters, setTheaters] = useState(initialTheaters);
  const [events, setEvents] = useState(initialEvents);
  const [actors, setActors] = useState(initialActors);
  const [showtimes, setShowtimes] = useState(initialShowtimes);
  const [tickets] = useState(initialTickets);
  const [concessions, setConcessions] = useState(initialConcessions);
  const [customers, setCustomers] = useState(initialCustomers);
  const [employees, setEmployees] = useState(initialEmployees);

  // Map cinemas to theaters for consistency across components
  const cinemas = theaters;
  const setCinemas = setTheaters;

  return (
    <DataContext.Provider value={{
      movies, setMovies,
      theaters, setTheaters,
      cinemas, setCinemas,
      events, setEvents,
      actors, setActors,
      showtimes, setShowtimes,
      tickets,
      concessions, setConcessions,
      customers, setCustomers,
      employees, setEmployees
    }}>
      {children}
    </DataContext.Provider>
  );
}

export function useData() {
  const context = useContext(DataContext);
  if (!context) {
    throw new Error('useData must be used within a DataProvider');
  }
  return context;
}
