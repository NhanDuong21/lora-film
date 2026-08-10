import { Info, Phone } from 'lucide-react';

export default function CinemaBasicInfo({ formData, setFormData, formErrors }) {
  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-4 rounded-2xl border border-zinc-800 bg-zinc-900/60 p-6">
        <div className="flex items-center gap-2 border-b border-zinc-800 pb-3">
          <Info className="h-4 w-4 text-orange-500" />
          <div>
            <h2 className="text-sm font-bold uppercase tracking-wider text-white">
              Thông tin nhận diện
            </h2>
            <p className="mt-1 text-xs text-zinc-500">
              Đây là nội dung quản trị viên và khách hàng sẽ nhìn thấy.
            </p>
          </div>
        </div>

        <label className="flex flex-col gap-1.5">
          <span className="text-[10px] font-black uppercase tracking-wider text-zinc-500">
            Tên cụm rạp <span className="text-rose-500">*</span>
          </span>
          <input
            type="text"
            value={formData.name}
            onChange={(event) => setFormData({ ...formData, name: event.target.value })}
            placeholder="Ví dụ: LoraFilm Sense City Cần Thơ"
            className={`rounded-xl border bg-zinc-950 px-3.5 py-2.5 text-xs text-zinc-100 outline-none ${
              formErrors.name ? 'border-red-500' : 'border-zinc-800 focus:border-orange-500/40'
            }`}
          />
          {formErrors.name && <span className="text-[10px] text-red-500">{formErrors.name}</span>}
        </label>

        <label className="flex flex-col gap-1.5">
          <span className="text-[10px] font-black uppercase tracking-wider text-zinc-500">
            Mô tả ngắn
          </span>
          <textarea
            rows={4}
            value={formData.description}
            onChange={(event) => setFormData({ ...formData, description: event.target.value })}
            placeholder="Giới thiệu không gian, dịch vụ và điểm nổi bật của cụm rạp..."
            className="resize-none rounded-xl border border-zinc-800 bg-zinc-950 px-3.5 py-2.5 text-xs text-zinc-100 outline-none focus:border-orange-500/40"
          />
        </label>
      </section>

      <section className="flex flex-col gap-4 rounded-2xl border border-zinc-800 bg-zinc-900/60 p-6">
        <div className="flex items-center gap-2 border-b border-zinc-800 pb-3">
          <Phone className="h-4 w-4 text-orange-500" />
          <h2 className="text-sm font-bold uppercase tracking-wider text-white">Liên hệ hỗ trợ</h2>
        </div>
        <label className="flex flex-col gap-1.5">
          <span className="text-[10px] font-black uppercase tracking-wider text-zinc-500">
            Số hotline
          </span>
          <input
            type="text"
            value={formData.hotline}
            onChange={(event) => setFormData({ ...formData, hotline: event.target.value })}
            placeholder="Ví dụ: 1900 6803"
            className="rounded-xl border border-zinc-800 bg-zinc-950 px-3.5 py-2.5 text-xs text-zinc-100 outline-none focus:border-orange-500/40"
          />
        </label>
      </section>
    </div>
  );
}
