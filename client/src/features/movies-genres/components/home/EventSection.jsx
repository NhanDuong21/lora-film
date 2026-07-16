import { ArrowRight } from "lucide-react";

export default function EventSection() {
    return (
        <section id="events-section" className="w-full bg-zinc-950 text-zinc-100 py-16 border-t border-b border-zinc-900">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                {/* Section Header */}
                <div className="flex justify-between items-center pb-4 border-b border-zinc-900/80 mb-6">
                    <h2 className="text-lg md:text-xl font-black uppercase tracking-wider text-white">
                        Sự Kiện & Ưu Đãi Hot
                    </h2>
                    <button className="text-xs font-bold text-zinc-550 flex items-center gap-1 cursor-not-allowed">
                        <span>Xem tất cả</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                    </button>
                </div>

                {/* Asymmetric Skeletons Matrix Grid */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6">
                    
                    {/* Left: Featured Event Skeleton Placeholders */}
                    <div className="lg:col-span-2 relative overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900 aspect-[16/9] md:aspect-[21/9] flex flex-col justify-end p-6 md:p-8">
                        <div className="absolute inset-0 bg-gradient-to-r from-transparent via-zinc-800/50 to-transparent -translate-x-full animate-[shimmer_1.5s_infinite] z-0" />
                        <div className="relative z-10 space-y-3">
                            <div className="h-4 bg-zinc-800 rounded w-28 animate-pulse" />
                            <div className="h-8 bg-zinc-800 rounded w-1/2 animate-pulse" />
                            <div className="h-4 bg-zinc-800 rounded w-2/3 animate-pulse" />
                        </div>
                    </div>

                    {/* Right: Stacked Mini-Promos Column Skeletons */}
                    <div className="lg:col-span-1 flex flex-col gap-4 justify-between min-h-[300px] lg:min-h-0">
                        {[1, 2].map((index) => (
                            <div
                                key={index}
                                className="w-full h-[47%] bg-zinc-900/50 border border-zinc-800/60 rounded-xl p-3 flex gap-4 overflow-hidden relative"
                            >
                                <div className="absolute inset-0 bg-gradient-to-r from-transparent via-zinc-800/50 to-transparent -translate-x-full animate-[shimmer_1.5s_infinite] z-0" />
                                
                                {/* Image skeleton */}
                                <div className="w-24 h-full shrink-0 rounded-lg bg-zinc-800 animate-pulse z-10" />
                                
                                {/* Info skeleton */}
                                <div className="flex-grow flex flex-col justify-between py-1 z-10">
                                    <div className="space-y-2">
                                        <div className="h-4 bg-zinc-800 rounded w-3/4 animate-pulse" />
                                        <div className="h-3 bg-zinc-800 rounded w-1/2 animate-pulse" />
                                    </div>
                                    <div className="h-3 bg-zinc-800 rounded w-1/4 animate-pulse" />
                                </div>
                            </div>
                        ))}
                    </div>

                </div>
            </div>
        </section>
    );
}
