import { useState } from "react";
import { ChevronDown, ChevronUp } from "lucide-react";

export default function InfoSection() {
    const [isExpanded, setIsExpanded] = useState(false);

    return (
        <section className="w-full bg-zinc-950 text-zinc-400 py-10 border-t border-zinc-900">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-sm leading-relaxed">
                {/* Section Header */}
                <div className="flex items-center gap-3 mb-6 border-l-4 border-brand-coral pl-3 select-none">
                    <h2 className="text-base md:text-lg font-bold uppercase tracking-widest text-zinc-100">
                        Thông Tin
                    </h2>
                </div>

                {/* Collapsible content */}
                <div 
                    className={`transition-all duration-500 ease-in-out relative ${
                        isExpanded ? "max-h-[1000px]" : "max-h-48 overflow-hidden"
                    }`}
                >
                    <div className="space-y-4">
                        <p>
                            Thanh lap tu nam 2003, <span className="text-zinc-100 font-bold">LoraFilm</span> da va dang khang dinh thuong hieu rap chieu phim hang dau Viet Nam. He thong LoraFilm noi tieng boi chat luong phong chieu hien dai, dich vu than thien va nhieu trai nghiem vuot chuan hon-ca-rap-chieu-phim. Ngoai cac cong nghe trinh chieu hang dau nhu <span className="text-brand-coral font-bold">IMAX Laser</span> va <span className="text-brand-coral font-bold">Onyx x Dolby Atmos</span>, LoraFilm con so huu nhung phong chieu dac biet dang cap nhu <span className="text-zinc-100 italic">Lagom</span>, <span className="text-zinc-100 italic">Romántico</span>, <span className="text-zinc-100 italic">Laurus</span>, <span className="text-zinc-100 italic">Aqualis</span>... mang lai khong gian dien anh dinh cao cho moi tin do dien anh.
                        </p>

                        <p>
                            Den voi LoraFilm, quy khach co the trai nghiem phong cho thuong luu <span className="text-brand-coral font-bold">Boulevard Lounge</span>, khu am thuc phong phu <span className="text-brand-coral font-bold">CineMunch Eatery</span>, he thong cong nghe tuong tac DIDIM Playground cung khu vui choi phuc hop danh rieng cho tre em. Tat ca tao nen mot to hop giai tri All-in-one khep kin hoan hao ngay trong long cum rap.
                        </p>

                        <p>
                            Khong chi tien phong tai rap vat ly, LoraFilm con hap dan khan gia boi he thong website truc tuyen vo cung hien dai, toi uu trai nghiem Single-Page muot ma. Voi thanh tim kiem thong minh <span className="text-zinc-100 font-bold">Omni-Search Bar Interface</span> ngay tren Header, nguoi dung co the quet tu khoa song song theo Ten Phim, Dien Vien hoac Dao Dien de tim ra ket qua mong muon ngay lap tuc. Lich chieu tai tat ca he thong rap LoraFilm luon duoc cap nhat thuong xuyen, day du va chuan xac theo thoi gian thuc.
                        </p>

                        <p>
                            Dat ve tai LoraFilm tro nen de dang hon bao gio het nho thanh <span className="text-zinc-100 font-bold">Mua Ve Nhanh dang Capsule toi gian</span> duoc tich hop ngay tren Banner Hero trang chu. Chi voi 4 buoc bam tuan tu: <span className="text-brand-coral italic font-semibold">Chon Phim ➔ Chon Rap ➔ Chon Ngay ➔ Chon Suat Chieu</span>, he thong se mo khoa va dua thang quy khach vao so do chon ghe truc quan, ket hop menu bap nuoc tien loi va cong thanh toan bao mat cao. Sau khi hoan tat, ma QR dat ve thanh cong se duoc gui thang vao Email/SMS cua ban, giup ban mot buoc quet ma tien thang vao phong chieu ma khong can xep hang cho doi.
                        </p>

                        <p>
                            He thong website con so huu chuyen muc <span className="text-zinc-100 font-bold">Goc Dien Anh</span> – noi luu tru kho du lieu khong lo ve cac ngoi sao dien anh thong qua cac chuyen trang <span className="text-zinc-100 italic">Actor & Director Portfolio Directory</span>. Tai day, nguoi yeu phim de dang tra cuu tieu su, bo suu tap hinh anh cinematic cung nhu toan bo danh muc tac pham (Filmography) cua cac Dien vien va Dao dien minh yeu thich nho thuat toan lien ket du lieu tu dong. Ben canh do, LoraFilm luon mang den hang loat chuong trinh uu dai, su kien dong gia ve hap dan hang tuan, va dac quyen gia ve U22 cuc dinh danh rieng cho the he tre.
                        </p>
                    </div>

                    {/* Gradient overlay when collapsed */}
                    {!isExpanded && (
                        <div className="absolute bottom-0 inset-x-0 h-20 bg-gradient-to-t from-zinc-950 to-transparent pointer-events-none" />
                    )}
                </div>

                {/* Toggle button */}
                <div className="mt-6 flex justify-center">
                    <button
                        onClick={() => setIsExpanded(!isExpanded)}
                        className="flex items-center gap-1.5 text-zinc-100 hover:text-brand-coral font-bold text-xs uppercase tracking-wider transition-colors duration-300 py-1.5 px-4 bg-zinc-900 border border-zinc-800 rounded-full hover:border-brand-coral/50 shadow-md focus:outline-none"
                    >
                        <span>{isExpanded ? "Thu gon" : "Xem them"}</span>
                        {isExpanded ? (
                            <ChevronUp className="w-4 h-4 text-zinc-400" />
                        ) : (
                            <ChevronDown className="w-4 h-4 text-zinc-400" />
                        )}
                    </button>
                </div>
            </div>
        </section>
    );
}
