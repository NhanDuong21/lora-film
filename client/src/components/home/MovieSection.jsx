export default function MovieSection() {
    return (
        <section id="movies-section" className="relative px-6 md:px-12 py-16 bg-zinc-950">
            {/* Header & Tabs */}
            <div className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mb-8 border-b border-zinc-800/80 pb-4">
                <div className="flex items-center gap-8">
                    <button className="text-lg md:text-xl font-black tracking-wider uppercase pb-2 transition-all relative text-brand-coral border-b-2 border-brand-coral drop-shadow-[0_0_10px_rgba(216,129,116,0.4)]">
                        Phim Đang Chiếu
                    </button>
                    <button className="text-lg md:text-xl font-black tracking-wider uppercase pb-2 transition-all relative text-zinc-500 hover:text-zinc-300">
                        Phim Sắp Chiếu
                    </button>
                </div>
            </div>

            {/* Empty State / Grid Skeletons */}
            <div className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 text-zinc-155 bg-zinc-950">
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
                    {[1, 2, 3, 4].map((index) => (
                        <div key={index} className="w-full flex flex-col gap-3">
                            {/* Skeleton Card */}
                            <div className="relative w-full aspect-[2/3] rounded-2xl bg-zinc-900 border border-zinc-800/80 overflow-hidden flex flex-col justify-end p-5">
                                <div className="absolute inset-0 bg-gradient-to-t from-zinc-950 via-zinc-950/20 to-transparent z-10" />
                                
                                {/* Pulse loader placeholder */}
                                <div className="absolute inset-0 bg-gradient-to-r from-transparent via-zinc-800/50 to-transparent -translate-x-full animate-[shimmer_1.5s_infinite] z-0" />
                                
                                <div className="relative z-20 space-y-2">
                                    <div className="h-4 bg-zinc-800 rounded w-16 animate-pulse" />
                                    <div className="h-5 bg-zinc-800 rounded w-3/4 animate-pulse" />
                                    <div className="h-3 bg-zinc-800 rounded w-1/2 animate-pulse" />
                                </div>
                            </div>
                        </div>
                    ))}
                </div>

                <div className="text-center mt-12 text-zinc-550 text-xs tracking-wider uppercase font-semibold">
                    Danh sach phim dang duoc cap nhat
                </div>
            </div>
        </section>
    );
}
