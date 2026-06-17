import { useState } from 'react';
import { ChevronDown, ChevronUp } from 'lucide-react';

export default function BrandOverview() {
  const [isExpanded, setIsExpanded] = useState(false);

  return (
    <section className="w-full bg-zinc-950 text-zinc-400 py-10 border-t border-zinc-900">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-sm leading-relaxed">
        {/* Left-Aligned Section Header */}
        <div className="flex items-center gap-3 mb-6 border-l-4 border-amber-500 pl-3 select-none">
          <h2 className="text-base md:text-lg font-bold uppercase tracking-widest text-zinc-100">
            Thông Tin
          </h2>
        </div>

        {/* Collapsible Wrapper */}
        <div 
          className={`transition-all duration-500 ease-in-out relative ${
            isExpanded ? 'max-h-[1000px]' : 'max-h-48 overflow-hidden'
          }`}
        >
          <div className="space-y-4">
            <p>
              Thành lập từ năm 2003, <span className="text-zinc-100 font-bold">LoraFilm</span> đã và đang khẳng định thương hiệu rạp chiếu phim hàng đầu Việt Nam. Hệ thống LoraFilm nổi tiếng bởi chất lượng phòng chiếu hiện đại, dịch vụ thân thiện và nhiều trải nghiệm vượt chuẩn hơn-cả-rạp-chiếu-phim. Ngoài các công nghệ trình chiếu hàng đầu như <span className="text-amber-500 font-bold">IMAX Laser</span> và <span className="text-amber-500 font-bold">Onyx x Dolby Atmos</span>, LoraFilm còn sở hữu những phòng chiếu đặc biệt đẳng cấp như <span className="text-zinc-100 italic">Lagom</span>, <span className="text-zinc-100 italic">Romántico</span>, <span className="text-zinc-100 italic">Laurus</span>, <span className="text-zinc-100 italic">Aqualis</span>... mang lại không gian điện ảnh đỉnh cao cho mọi tín đồ điện ảnh.
            </p>

            <p>
              Đến với LoraFilm, quý khách có thể trải nghiệm phòng chờ thượng lưu <span className="text-amber-500 font-bold">Boulevard Lounge</span>, khu ẩm thực phong phú <span className="text-amber-500 font-bold">CineMunch Eatery</span>, hệ thống công nghệ tương tác DIDIM Playground cùng khu vui chơi phức hợp dành riêng cho trẻ em. Tất cả tạo nên một tổ hợp giải trí All-in-one khép kín hoàn hảo ngay trong lòng cụm rạp.
            </p>

            <p>
              Không chỉ tiên phong tại rạp vật lý, LoraFilm còn hấp dẫn khán giả bởi hệ thống website trực tuyến vô cùng hiện đại, tối ưu trải nghiệm Single-Page mượt mà. Với thanh tìm kiếm thông minh <span className="text-zinc-100 font-bold">Omni-Search Bar Interface</span> ngay trên Header, người dùng có thể quét từ khóa song song theo Tên Phim, Diễn Viên hoặc Đạo Diễn để tìm ra kết quả mong muốn ngay lập tức. Lịch chiếu tại tất cả hệ thống rạp LoraFilm luôn được cập nhật thường xuyên, đầy đủ và chuẩn xác theo thời gian thực.
            </p>

            <p>
              Đặt vé tại LoraFilm trở nên dễ dàng hơn bao giờ hết nhờ thanh <span className="text-zinc-100 font-bold">Mua Vé Nhanh dạng Capsule tối giản</span> được tích hợp ngay trên Banner Hero trang chủ. Chỉ với 4 bước bấm tuần tự: <span className="text-amber-500 italic font-semibold">Chọn Phim ➔ Chọn Rạp ➔ Chọn Ngày ➔ Chọn Suất Chiếu</span>, hệ thống sẽ mở khóa và đưa thẳng quý khách vào sơ đồ chọn ghế trực quan, kết hợp menu bắp nước tiện lợi và cổng thanh toán bảo mật cao. Sau khi hoàn tất, mã QR đặt vé thành công sẽ được gửi thẳng vào Email/SMS của bạn, giúp bạn một bước quét mã tiến thẳng vào phòng chiếu mà không cần xếp hàng chờ đợi.
            </p>

            <p>
              Hệ thống website còn sở hữu chuyên mục <span className="text-zinc-100 font-bold">Góc Điện Ảnh</span> – nơi lưu trữ kho dữ liệu khổng lồ về các ngôi sao điện ảnh thông qua các chuyên trang <span className="text-zinc-100 italic">Actor & Director Portfolio Directory</span>. Tại đây, người yêu phim dễ dàng tra cứu tiểu sử, bộ sưu tập hình ảnh cinematic cũng như toàn bộ danh mục tác phẩm (Filmography) của các Diễn viên và Đạo diễn mình yêu thích nhờ thuật toán liên kết dữ liệu tự động. Bên cạnh đó, LoraFilm luôn mang đến hàng loạt chương trình ưu đãi, sự kiện đồng giá vé hấp dẫn hàng tuần, và đặc quyền giá vé U22 cực đỉnh dành riêng cho thế hệ trẻ.
            </p>
          </div>

          {/* Fade-out Gradient cloak at the base */}
          {!isExpanded && (
            <div className="absolute bottom-0 inset-x-0 h-20 bg-gradient-to-t from-zinc-950 to-transparent pointer-events-none" />
          )}
        </div>

        {/* Toggle Button Container */}
        <div className="mt-6 flex justify-center">
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="flex items-center gap-1.5 text-zinc-100 hover:text-amber-500 font-bold text-xs uppercase tracking-wider transition-colors duration-300 py-1.5 px-4 bg-zinc-900 border border-zinc-800 rounded-full hover:border-amber-500/50 shadow-md focus:outline-none"
          >
            <span>{isExpanded ? 'Thu gọn' : 'Xem thêm'}</span>
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
