import { useState } from 'react';
import { ChevronDown, ChevronUp } from 'lucide-react';

const Accent = ({ children }) => (
  <strong className="font-bold text-brand-orange">{children}</strong>
);

const Emphasis = ({ children }) => (
  <strong className="font-bold text-zinc-100">{children}</strong>
);

export default function InfoSection() {
  const [expanded, setExpanded] = useState(true);

  return (
    <section
      id="gioi-thieu"
      aria-labelledby="lorafilm-information-title"
      className="w-full scroll-mt-24 border-t border-zinc-900 bg-[#09090b] px-6 py-10 text-zinc-100 md:px-12"
    >
      <div className="mx-auto max-w-7xl border-b border-zinc-800/70 pb-9">
        <header>
          <h2
            id="lorafilm-information-title"
            className="border-l-4 border-brand-orange pl-3 text-base font-black uppercase tracking-[0.08em] text-white sm:text-lg"
          >
            Thông tin
          </h2>
        </header>

        <div className="mt-6 space-y-4 text-[13px] leading-6 text-zinc-400 sm:text-sm sm:leading-6">
          <p>
            Thành lập từ năm 2003, <Emphasis>LoraFilm</Emphasis> đã và đang khẳng định thương
            hiệu rạp chiếu phim hàng đầu Việt Nam. Hệ thống LoraFilm nổi tiếng bởi chất lượng
            phòng chiếu hiện đại, dịch vụ thân thiện và nhiều trải nghiệm vượt chuẩn
            hơn-cả-rạp-chiếu-phim. Ngoài các công nghệ trình chiếu hàng đầu như{' '}
            <Accent>IMAX Laser</Accent> và <Accent>Onyx x Dolby Atmos</Accent>, LoraFilm còn sở
            hữu những phòng chiếu đặc biệt đẳng cấp như <em className="font-semibold text-zinc-200">Lagom</em>,{' '}
            <em className="font-semibold text-zinc-200">Romántico</em>,{' '}
            <em className="font-semibold text-zinc-200">Laurus</em>,{' '}
            <em className="font-semibold text-zinc-200">Aqualis</em>... mang lại không gian điện
            ảnh đỉnh cao cho mọi tín đồ điện ảnh.
          </p>

          <div id="lorafilm-information-details" className={expanded ? 'space-y-4' : 'hidden'}>
            <p>
              Đến với LoraFilm, quý khách có thể trải nghiệm phòng chờ thượng lưu{' '}
              <Accent>Boulevard Lounge</Accent>, khu ẩm thực phong phú{' '}
              <Accent>CineMunch Eatery</Accent>, hệ thống công nghệ tương tác DIDIM Playground
              cùng khu vui chơi phức hợp dành riêng cho trẻ em. Tất cả tạo nên một tổ hợp giải
              trí All-in-one khép kín hoàn hảo ngay trong lòng cụm rạp.
            </p>

            <p>
              Không chỉ tiên phong tại rạp vật lý, LoraFilm còn hấp dẫn khán giả bởi hệ thống
              website trực tuyến vô cùng hiện đại, tối ưu trải nghiệm Single-Page mượt mà. Với
              thanh tìm kiếm thông minh <Emphasis>Omni-Search Bar Interface</Emphasis> ngay trên
              Header, người dùng có thể quét từ khóa song song theo Tên Phim, Diễn Viên hoặc Đạo
              Diễn để tìm ra kết quả mong muốn ngay lập tức. Lịch chiếu tại tất cả hệ thống rạp
              LoraFilm luôn được cập nhật thường xuyên, đầy đủ và chuẩn xác theo thời gian thực.
            </p>

            <p>
              Đặt vé tại LoraFilm trở nên dễ dàng hơn bao giờ hết nhờ thanh{' '}
              <Emphasis>Mua Vé Nhanh dạng Capsule tối giản</Emphasis> được tích hợp ngay trên
              Banner Hero trang chủ. Chỉ với 4 bước bấm tuần tự:{' '}
              <em className="font-bold text-brand-orange">
                Chọn Phim ➔ Chọn Rạp ➔ Chọn Ngày ➔ Chọn Suất Chiếu
              </em>
              , hệ thống sẽ mở khóa và đưa thẳng quý khách vào sơ đồ chọn ghế trực quan, kết hợp
              menu bắp nước tiện lợi và cổng thanh toán bảo mật cao. Sau khi hoàn tất, mã QR đặt
              vé thành công sẽ được gửi thẳng vào Email/SMS của bạn, giúp bạn một bước quét mã
              tiến thẳng vào phòng chiếu mà không cần xếp hàng chờ đợi.
            </p>

            <p>
              Hệ thống website còn sở hữu chuyên mục <Emphasis>Góc Điện Ảnh</Emphasis> – nơi lưu
              trữ kho dữ liệu khổng lồ về các ngôi sao điện ảnh thông qua các chuyên trang{' '}
              <em className="font-semibold text-zinc-200">Actor &amp; Director Portfolio Directory</em>.
              Tại đây, người yêu phim dễ dàng tra cứu tiểu sử, bộ sưu tập hình ảnh cinematic
              cũng như toàn bộ danh mục tác phẩm (Filmography) của các Diễn viên và Đạo diễn
              mình yêu thích nhờ thuật toán liên kết dữ liệu tự động. Bên cạnh đó, LoraFilm luôn
              mang đến hàng loạt chương trình ưu đãi, sự kiện đồng giá vé hấp dẫn hàng tuần, và
              đặc quyền giá vé U22 cực đỉnh dành riêng cho thế hệ trẻ.
            </p>
          </div>
        </div>

        <div className="mt-6 flex justify-center">
          <button
            type="button"
            aria-expanded={expanded}
            aria-controls="lorafilm-information-details"
            onClick={() => setExpanded(current => !current)}
            className="inline-flex min-h-8 items-center gap-1.5 rounded-full border border-zinc-700 bg-zinc-900 px-4 text-[10px] font-black uppercase tracking-wide text-zinc-200 transition-colors hover:border-brand-orange/50 hover:text-brand-orange focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange/60"
          >
            {expanded ? 'Thu gọn' : 'Xem thêm'}
            {expanded
              ? <ChevronUp aria-hidden="true" className="h-3.5 w-3.5" />
              : <ChevronDown aria-hidden="true" className="h-3.5 w-3.5" />}
          </button>
        </div>
      </div>
    </section>
  );
}
