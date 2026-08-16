import {
  ArrowLeft,
  CheckCircle2,
  CircleHelp,
  CreditCard,
  FileText,
  Mail,
  Phone,
  RotateCcw,
  ShieldCheck,
} from 'lucide-react';
import { Link, Navigate, useParams } from 'react-router-dom';

const topics = {
  payment: {
    eyebrow: 'Hướng dẫn khách hàng',
    title: 'Hướng dẫn thanh toán',
    description: 'Các cách thanh toán đang được LoraFilm hỗ trợ khi đặt vé trực tuyến và tại quầy.',
    icon: CreditCard,
    sections: [
      {
        heading: 'Đặt vé trực tuyến',
        items: [
          'Chọn phim, rạp, suất chiếu và ghế trước khi chuyển sang bước thanh toán.',
          'Kiểm tra kỹ suất chiếu, ghế, món ăn và tổng tiền trước khi xác nhận.',
          'Thanh toán qua VNPay hoặc MoMo và không đóng trang khi giao dịch đang xử lý.',
          'Sau khi thành công, vé điện tử xuất hiện trong lịch sử đặt vé của tài khoản.',
        ],
      },
      {
        heading: 'Thanh toán tại quầy',
        items: [
          'LoraFilm hỗ trợ tiền mặt tại quầy đối với đơn được nhân viên tạo trực tiếp.',
          'Hãy giữ mã đơn hoặc vé điện tử để được hỗ trợ nhanh khi cần đối chiếu.',
        ],
      },
    ],
  },
  refunds: {
    eyebrow: 'Chính sách đặt vé',
    title: 'Đổi, hủy và hoàn vé',
    description: 'Thông tin cần biết khi lịch chiếu thay đổi hoặc giao dịch cần được kiểm tra.',
    icon: RotateCcw,
    sections: [
      {
        heading: 'Khi nào cần gửi yêu cầu?',
        items: [
          'Giao dịch đã trừ tiền nhưng đơn chưa ghi nhận thanh toán thành công.',
          'Suất chiếu bị LoraFilm hủy hoặc điều chỉnh do sự cố vận hành.',
          'Thông tin trên vé điện tử không trùng với đơn đã thanh toán.',
        ],
      },
      {
        heading: 'Cách được hỗ trợ',
        items: [
          'Mở chi tiết đơn trong Lịch sử đặt vé để kiểm tra trạng thái mới nhất.',
          'Liên hệ LoraFilm và cung cấp mã đơn, mã giao dịch cùng phương thức thanh toán.',
          'Điều kiện và thời gian hoàn tiền phụ thuộc trạng thái đơn và kênh thanh toán; bộ phận hỗ trợ sẽ thông báo rõ trước khi xử lý.',
        ],
      },
    ],
  },
  faq: {
    eyebrow: 'Hỗ trợ nhanh',
    title: 'Câu hỏi thường gặp',
    description: 'Câu trả lời ngắn cho những tình huống thường gặp khi đặt vé tại LoraFilm.',
    icon: CircleHelp,
    sections: [
      {
        heading: 'Tôi xem vé điện tử ở đâu?',
        items: ['Đăng nhập, mở Lịch sử đặt vé và chọn đúng đơn để xem mã vé cùng thông tin suất chiếu.'],
      },
      {
        heading: 'Thanh toán bị gián đoạn thì làm gì?',
        items: ['Không thanh toán lại ngay. Hãy tải lại chi tiết đơn để kiểm tra trạng thái hoặc liên hệ hỗ trợ kèm mã giao dịch.'],
      },
      {
        heading: 'Tôi có thể nhận ưu đãi ở đâu?',
        items: ['Mở trang Ưu đãi, nhận voucher còn hiệu lực và chọn voucher phù hợp tại bước thanh toán.'],
      },
    ],
  },
  terms: {
    eyebrow: 'Thông tin pháp lý',
    title: 'Điều khoản sử dụng',
    description: 'Các nguyên tắc cơ bản khi sử dụng dịch vụ đặt vé và tài khoản LoraFilm.',
    icon: FileText,
    sections: [
      {
        heading: 'Trách nhiệm của khách hàng',
        items: [
          'Cung cấp thông tin chính xác và tự bảo vệ thông tin đăng nhập của tài khoản.',
          'Kiểm tra phim, rạp, suất chiếu, ghế và tổng tiền trước khi xác nhận thanh toán.',
          'Không sử dụng dịch vụ cho hành vi gian lận, phá hoại hoặc ảnh hưởng đến khách hàng khác.',
        ],
      },
      {
        heading: 'Vận hành dịch vụ',
        items: [
          'LoraFilm có thể cập nhật lịch chiếu, giá vé và điều kiện ưu đãi theo từng thời điểm.',
          'Khi có thay đổi ảnh hưởng đến vé đã mua, LoraFilm sẽ hỗ trợ theo trạng thái thực tế của đơn.',
        ],
      },
    ],
  },
  privacy: {
    eyebrow: 'Bảo vệ khách hàng',
    title: 'Chính sách bảo mật',
    description: 'Cách LoraFilm sử dụng thông tin cần thiết để cung cấp và hỗ trợ dịch vụ đặt vé.',
    icon: ShieldCheck,
    sections: [
      {
        heading: 'Thông tin được sử dụng',
        items: [
          'Thông tin tài khoản, liên hệ, đơn đặt vé và giao dịch phục vụ việc phát hành vé và hỗ trợ khách hàng.',
          'Dữ liệu kỹ thuật cần thiết để bảo vệ phiên đăng nhập, ngăn ngừa gian lận và cải thiện tính ổn định của hệ thống.',
        ],
      },
      {
        heading: 'Quyền của khách hàng',
        items: [
          'Bạn có thể kiểm tra và cập nhật thông tin cá nhân trong trang tài khoản.',
          'Bạn có thể liên hệ bộ phận hỗ trợ để hỏi về dữ liệu gắn với tài khoản hoặc báo cáo truy cập bất thường.',
        ],
      },
    ],
  },
};

export default function CustomerInformationPage() {
  const { topic } = useParams();
  const content = topics[topic];

  if (!content) return <Navigate to="/support/faq" replace />;

  const Icon = content.icon;

  return (
    <div className="min-h-[70vh] bg-zinc-950 px-4 py-12 text-zinc-100 sm:px-6 lg:px-8">
      <div className="mx-auto w-full max-w-4xl">
        <Link
          to="/"
          className="inline-flex items-center gap-2 text-sm font-bold text-zinc-500 transition-colors hover:text-brand-orange"
        >
          <ArrowLeft className="h-4 w-4" /> Về trang chủ
        </Link>

        <header className="mt-7 rounded-3xl border border-zinc-800 bg-zinc-900/50 p-7 sm:p-9">
          <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-orange/10 text-brand-orange">
            <Icon className="h-6 w-6" />
          </span>
          <p className="mt-6 text-[11px] font-black uppercase tracking-[0.22em] text-brand-orange">
            {content.eyebrow}
          </p>
          <h1 className="mt-2 text-2xl font-black text-white sm:text-3xl">{content.title}</h1>
          <p className="mt-3 max-w-2xl text-sm leading-6 text-zinc-400">{content.description}</p>
        </header>

        <div className="mt-6 space-y-5">
          {content.sections.map(section => (
            <section key={section.heading} className="rounded-2xl border border-zinc-800 bg-zinc-900/30 p-6 sm:p-7">
              <h2 className="text-base font-black text-white">{section.heading}</h2>
              <ul className="mt-4 space-y-3">
                {section.items.map(item => (
                  <li key={item} className="flex gap-3 text-sm leading-6 text-zinc-400">
                    <CheckCircle2 className="mt-1 h-4 w-4 shrink-0 text-brand-orange" />
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </div>

        <aside className="mt-7 flex flex-col gap-4 rounded-2xl border border-brand-orange/20 bg-brand-orange/[0.05] p-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="font-black text-white">Bạn vẫn cần hỗ trợ?</h2>
            <p className="mt-1 text-sm text-zinc-500">Liên hệ LoraFilm và chuẩn bị mã đơn hoặc mã giao dịch nếu có.</p>
          </div>
          <div className="flex flex-wrap gap-3 text-sm font-bold">
            <a href="tel:19006868" className="inline-flex items-center gap-2 rounded-xl bg-brand-orange px-4 py-2.5 text-white hover:bg-orange-600">
              <Phone className="h-4 w-4" /> 1900 6868
            </a>
            <a href="mailto:support@lorafilm.vn" className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 px-4 py-2.5 text-zinc-200 hover:border-brand-orange">
              <Mail className="h-4 w-4" /> Gửi email
            </a>
          </div>
        </aside>
      </div>
    </div>
  );
}
