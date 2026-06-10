import { Film, Facebook, Instagram, Youtube, Video } from "lucide-react";

export default function Footer() {
    return (
        <footer className="bg-zinc-950">
            <div className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 py-12 border-t border-zinc-900 text-zinc-400 text-sm">
                
                {/* Column 1: Brand Overview & Social Connect */}
                <div className="space-y-4">
                    <div className="flex items-center gap-2 select-none">
                        <div className="bg-brand-coral/10 p-1.5 rounded-lg">
                            <Film className="w-5 h-5 text-brand-coral" />
                        </div>
                        <span className="text-lg font-black tracking-tight text-white">
                            <span className="text-brand-coral">Lora</span>
                            Film
                        </span>
                    </div>
                    <p className="text-xs text-zinc-500 leading-relaxed">
                        He thong rap chieu phim hien dai mang den trai nghiem dien anh vuot chuan vuot gioi han voi cong nghe dinh cao.
                    </p>
                    <div className="flex items-center gap-3 mt-4">
                        <a
                            href="https://www.facebook.com"
                            target="_blank"
                            rel="noopener noreferrer"
                            className="w-8 h-8 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-400 hover:text-white hover:border-brand-coral hover:bg-brand-coral/10 transition-all duration-300"
                            aria-label="Facebook"
                        >
                            <Facebook className="w-4 h-4" />
                        </a>
                        <a
                            href="https://www.instagram.com"
                            target="_blank"
                            rel="noopener noreferrer"
                            className="w-8 h-8 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-400 hover:text-white hover:border-brand-coral hover:bg-brand-coral/10 transition-all duration-300"
                            aria-label="Instagram"
                        >
                            <Instagram className="w-4 h-4" />
                        </a>
                        <a
                            href="https://www.youtube.com"
                            target="_blank"
                            rel="noopener noreferrer"
                            className="w-8 h-8 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-400 hover:text-white hover:border-brand-coral hover:bg-brand-coral/10 transition-all duration-300"
                            aria-label="YouTube"
                        >
                            <Youtube className="w-4 h-4" />
                        </a>
                        <a
                            href="https://nyanmovie.site"
                            target="_blank"
                            rel="noopener noreferrer"
                            className="w-8 h-8 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-400 hover:text-white hover:border-brand-coral hover:bg-brand-coral/10 transition-all duration-300"
                            aria-label="Video"
                        >
                            <Video className="w-4 h-4" />
                        </a>
                    </div>
                </div>

                {/* Column 2: Terms & Compliance */}
                <div className="space-y-4">
                    <h3 className="text-zinc-100 font-bold uppercase tracking-wider text-xs">
                        Dieu Khoan Su Dung
                    </h3>
                    <ul className="space-y-2 text-xs">
                        <li>
                            <a href="#/" className="hover:text-zinc-100 transition-colors duration-200">
                                Dieu Khoan Chung
                            </a>
                        </li>
                        <li>
                            <a href="#/" className="hover:text-zinc-100 transition-colors duration-200">
                                Chinh Sach Thanh Toan
                            </a>
                        </li>
                        <li>
                            <a href="#/" className="hover:text-zinc-100 transition-colors duration-200">
                                Chinh Sach Bao Mat
                            </a>
                        </li>
                        <li>
                            <a href="#/" className="hover:text-zinc-100 transition-colors duration-200">
                                Quy Che Hoat Dong
                            </a>
                        </li>
                    </ul>
                </div>

                {/* Column 3: Site Links */}
                <div className="space-y-4">
                    <h3 className="text-zinc-100 font-bold uppercase tracking-wider text-xs">
                        Danh Muc LoraFilm
                    </h3>
                    <ul className="space-y-2 text-xs">
                        <li>
                            <a href="#/" className="hover:text-zinc-100 transition-colors duration-200">
                                Phim Dang Chieu
                            </a>
                        </li>
                        <li>
                            <a href="#/" className="hover:text-zinc-100 transition-colors duration-200">
                                He Thong Rap
                            </a>
                        </li>
                        <li>
                            <a href="#/" className="hover:text-zinc-100 transition-colors duration-200">
                                Su Kien & Uu Dai
                            </a>
                        </li>
                        <li>
                            <a href="#/" className="hover:text-zinc-100 transition-colors duration-200">
                                Goc Dien Anh
                            </a>
                        </li>
                    </ul>
                </div>

                {/* Column 4: Customer Support & Verification */}
                <div className="space-y-4">
                    <h3 className="text-zinc-100 font-bold uppercase tracking-wider text-xs">
                        Cham Soc Khach Hang
                    </h3>
                    <ul className="space-y-2 text-xs text-zinc-500">
                        <li className="text-zinc-400">
                            Hotline: <span className="text-zinc-300 font-medium">1900 LORA (10:00 - 22:00)</span>
                        </li>
                        <li className="text-zinc-400">
                            Email: <span className="text-zinc-300 font-medium">support@lorafilm.vn</span>
                        </li>
                    </ul>
                    
                    <div className="pt-2">
                        <div className="border border-red-500/25 bg-red-950/10 text-red-500 text-[10px] uppercase font-black tracking-widest px-3 py-1.5 rounded inline-flex items-center gap-1.5 select-none">
                            <span className="w-1.5 h-1.5 bg-red-500 rounded-full animate-pulse"></span>
                            Da Dang Ky Bo Cong Thuong
                        </div>
                    </div>
                </div>
            </div>

            {/* Baseline Copyright Separation Banner */}
            <div className="bg-zinc-950 border-t border-zinc-900/60">
                <div className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 flex flex-col md:flex-row justify-between items-center gap-4 text-xs text-zinc-500 text-center md:text-left">
                    <span>
                        © 2026 LoraFilm. Movie Tickets, Your Way. All rights reserved.
                    </span>
                    <span className="text-[10px] text-zinc-600 font-medium">
                        Co so hoat dong: FPT University - Campus CanTho.
                    </span>
                </div>
            </div>
        </footer>
    );
}
