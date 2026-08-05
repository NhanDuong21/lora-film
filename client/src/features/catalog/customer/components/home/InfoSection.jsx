export default function InfoSection() {
    return (
        <section className="w-full border-t border-zinc-900 bg-zinc-950 py-10 text-zinc-400">
            <div className="mx-auto max-w-7xl px-4 text-sm leading-relaxed sm:px-6 lg:px-8">
                <div className="mb-6 flex items-center gap-3 border-l-4 border-brand-orange pl-3">
                    <h2 className="text-base font-bold uppercase tracking-widest text-zinc-100 md:text-lg">
                        Về LoraFilm
                    </h2>
                </div>

                <div className="grid gap-4 md:grid-cols-3">
                    <article className="rounded-2xl border border-zinc-800 bg-zinc-900/50 p-5">
                        <h3 className="mb-2 font-bold text-zinc-100">Khám phá phim và lịch chiếu</h3>
                        <p>
                            Xem thông tin phim, rạp và các suất chiếu đang mở bán từ dữ liệu cập nhật của hệ thống.
                        </p>
                    </article>

                    <article className="rounded-2xl border border-zinc-800 bg-zinc-900/50 p-5">
                        <h3 className="mb-2 font-bold text-zinc-100">Đặt vé trong một luồng thống nhất</h3>
                        <p>
                            Chọn suất chiếu, vị trí ghế và theo dõi trạng thái thanh toán ngay trên tài khoản LoraFilm.
                        </p>
                    </article>

                    <article className="rounded-2xl border border-zinc-800 bg-zinc-900/50 p-5">
                        <h3 className="mb-2 font-bold text-zinc-100">Quản lý vé và ưu đãi</h3>
                        <p>
                            Tra cứu lịch sử đặt vé, vé điện tử, điểm thành viên và các ưu đãi đang khả dụng.
                        </p>
                    </article>
                </div>
            </div>
        </section>
    );
}
