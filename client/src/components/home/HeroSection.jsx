import { useState } from "react";
import { Play, ChevronDown } from "lucide-react";

export default function HeroSection() {
    const videoSource = "https://res.cloudinary.com/dqc4hufot/video/upload/0603_ugvo4m.mp4";

    // Select states
    const [selectedMovie, setSelectedMovie] = useState("");
    const [selectedCinema, setSelectedCinema] = useState("");
    const [selectedDate, setSelectedDate] = useState("");
    const [selectedTime, setSelectedTime] = useState("");

    return (
        <>
            <section className="relative min-h-[85vh] md:min-h-[90vh] flex flex-col justify-between pt-28 pb-16 px-6 md:px-12 overflow-hidden bg-brand-dark">
                {/* Background Video */}
                <div className="absolute inset-0 w-full h-full z-0 overflow-hidden">
                    <video
                        autoPlay
                        loop
                        muted
                        playsInline
                        className="w-full h-full object-cover scale-105 opacity-60 transition-all duration-700"
                        src={videoSource}
                    />
                </div>

                {/* Backdrop Overlay Gradient */}
                <div className="absolute inset-0 bg-gradient-to-t from-brand-dark via-brand-dark/20 to-brand-dark/40 z-10 backdrop-blur-[1px]"></div>

                {/* Main Content Area */}
                <div className="relative z-20 max-w-4xl mt-auto mb-8 animate-in fade-in slide-in-from-bottom duration-1000">
                    <div className="inline-flex items-center gap-2 px-3 py-1 bg-red-950/40 backdrop-blur-md border border-red-500/30 rounded-full shadow-[0_0_15px_rgba(239,68,68,0.15)] mb-4">
                        <span className="relative flex h-2 w-2">
                            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
                            <span className="relative inline-flex rounded-full h-2 w-2 bg-red-500"></span>
                        </span>
                        <span className="text-red-400 text-xs font-bold uppercase tracking-widest">
                            TOP PICK | Lựa chọn của bạn hôm nay
                        </span>
                    </div>

                    <h1 className="text-4xl md:text-7xl font-black text-white tracking-tight uppercase mb-2 leading-none">
                        Khám phá thế giới <br />
                        <span className="text-brand-coral">phim của bạn</span>
                    </h1>

                    <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-4 mb-6">
                        <span className="text-2xl md:text-3xl font-black text-brand-coral tracking-widest uppercase">
                            Lora Film
                        </span>
                        <span className="hidden sm:inline text-zinc-650">|</span>
                        <span className="text-sm md:text-base text-zinc-400 font-medium italic tracking-wider">
                            "Movie Tickets, Your Way"
                        </span>
                    </div>

                    <div className="flex flex-wrap gap-4">
                        <button
                            onClick={() => {
                                const element = document.getElementById("movies-section");
                                element?.scrollIntoView({ behavior: "smooth" });
                            }}
                            className="group flex items-center gap-2 bg-brand-coral text-white font-bold px-8 py-4 rounded-full hover:bg-opacity-95 hover:shadow-brand-coral/25 shadow-lg transition-all transform hover:scale-105 duration-300 cursor-pointer"
                        >
                            <Play className="w-5 h-5 fill-current text-white" />
                            ĐẶT VÉ NGAY
                        </button>
                        
                        <button
                            onClick={() => {
                                const element = document.getElementById("booking-steps");
                                element?.scrollIntoView({ behavior: "smooth" });
                            }}
                            className="flex items-center gap-2 bg-white/5 hover:bg-white/10 text-white border border-white/10 font-bold px-6 py-4 rounded-full transition-all duration-300 cursor-pointer"
                        >
                            Tìm hiểu thêm
                        </button>
                    </div>
                </div>
            </section>

            {/* Cinematic Capsule Booking Widget */}
            <div className="relative z-30 w-full max-w-5xl mx-auto -mt-10 px-4">
                <div className="bg-zinc-900/90 backdrop-blur-md border border-zinc-800 rounded-2xl md:rounded-full flex flex-col md:flex-row items-center justify-between p-3 gap-4 shadow-2xl">
                    <div className="flex-1 grid grid-cols-2 md:grid-cols-4 w-full gap-2 items-center">
                        {/* Step 1: Phim */}
                        <div className="flex items-center gap-2 px-4 border-r border-zinc-800/60 h-10">
                            <div className="flex-1 min-w-0">
                                <label className="block text-[8px] font-black text-zinc-500 uppercase tracking-widest">Phim</label>
                                <select
                                    value={selectedMovie}
                                    onChange={(e) => setSelectedMovie(e.target.value)}
                                    className="w-full bg-transparent text-xs font-bold text-zinc-300 outline-none cursor-pointer appearance-none border-0 p-0 focus:ring-0 focus:outline-none truncate"
                                >
                                    <option value="" className="bg-zinc-950 text-zinc-600">Chọn Phim...</option>
                                    <option value="1" className="bg-zinc-950 text-white">Chờ tải phim...</option>
                                </select>
                            </div>
                            <ChevronDown className="w-3.5 h-3.5 text-zinc-500 shrink-0 pointer-events-none" />
                        </div>

                        {/* Step 2: Rạp */}
                        <div className="flex items-center gap-2 px-4 border-r border-zinc-800/60 h-10">
                            <div className="flex-1 min-w-0">
                                <label className="block text-[8px] font-black text-zinc-500 uppercase tracking-widest">Rạp</label>
                                <select
                                    value={selectedCinema}
                                    onChange={(e) => setSelectedCinema(e.target.value)}
                                    className="w-full bg-transparent text-xs font-bold text-zinc-300 outline-none cursor-pointer appearance-none border-0 p-0 focus:ring-0 focus:outline-none truncate"
                                >
                                    <option value="" className="bg-zinc-950 text-zinc-600">Chọn Rạp...</option>
                                    <option value="1" className="bg-zinc-950 text-white">Chờ tải rạp...</option>
                                </select>
                            </div>
                            <ChevronDown className="w-3.5 h-3.5 text-zinc-500 shrink-0 pointer-events-none" />
                        </div>

                        {/* Step 3: Ngày */}
                        <div className="flex items-center gap-2 px-4 border-r border-zinc-800/60 h-10">
                            <div className="flex-1 min-w-0">
                                <label className="block text-[8px] font-black text-zinc-500 uppercase tracking-widest">Ngày</label>
                                <select
                                    value={selectedDate}
                                    onChange={(e) => setSelectedDate(e.target.value)}
                                    className="w-full bg-transparent text-xs font-bold text-zinc-300 outline-none cursor-pointer appearance-none border-0 p-0 focus:ring-0 focus:outline-none truncate"
                                >
                                    <option value="" className="bg-zinc-950 text-zinc-600">Chọn Ngày...</option>
                                    <option value="1" className="bg-zinc-950 text-white">Hôm nay</option>
                                </select>
                            </div>
                            <ChevronDown className="w-3.5 h-3.5 text-zinc-500 shrink-0 pointer-events-none" />
                        </div>

                        {/* Step 4: Suất */}
                        <div className="flex items-center gap-2 px-4 h-10">
                            <div className="flex-1 min-w-0">
                                <label className="block text-[8px] font-black text-zinc-500 uppercase tracking-widest">Suất</label>
                                <select
                                    value={selectedTime}
                                    onChange={(e) => setSelectedTime(e.target.value)}
                                    className="w-full bg-transparent text-xs font-bold text-zinc-300 outline-none cursor-pointer appearance-none border-0 p-0 focus:ring-0 focus:outline-none truncate"
                                >
                                    <option value="" className="bg-zinc-950 text-zinc-600">Chọn Suất...</option>
                                    <option value="1" className="bg-zinc-950 text-white">Chờ tải suất...</option>
                                </select>
                            </div>
                            <ChevronDown className="w-3.5 h-3.5 text-zinc-500 shrink-0 pointer-events-none" />
                        </div>
                    </div>

                    <button
                        disabled
                        className="bg-brand-coral opacity-60 text-white font-black px-8 h-12 rounded-xl md:rounded-full cursor-not-allowed text-xs uppercase tracking-widest shrink-0 w-full md:w-auto"
                    >
                        Mua vé nhanh
                    </button>
                </div>
            </div>
        </>
    );
}
