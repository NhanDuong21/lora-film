/* eslint-disable react-hooks/set-state-in-effect */
import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import {
  Award,
  Bell,
  CalendarDays,
  Camera,
  ChevronRight,
  CircleHelp,
  Clock3,
  Gift,
  KeyRound,
  LayoutDashboard,
  LockKeyhole,
  Mail,
  Phone,
  ReceiptText,
  ShieldCheck,
  Smartphone,
  Sparkles,
  Ticket,
  User,
  WalletCards,
} from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import CustomerBookingHistory from "@/features/booking/customer/components/CustomerBookingHistory";
import {
  getBookingHistory,
  getBookingSpendingSummary,
} from "@/features/booking/customer/services/bookingService";
import CustomerNotificationCenter from "@/features/notifications/customer/components/CustomerNotificationCenter";
import CustomerPromotionCenterPage from "@/features/promotion/customer/pages/CustomerPromotionCenterPage";
import useCustomerScore from "@/features/score/customer/hooks/useCustomerScore";
import LoyaltyCenterPage from "@/features/score/customer/pages/LoyaltyCenterPage";
import { updateUserProfile, uploadAvatar } from "@/features/auth/services/userService";
import ChangeEmail from "@/features/auth/pages/ChangeEmail";
import ChangePassword from "@/features/auth/pages/ChangePassword";
import SessionsPage from "@/features/auth/pages/SessionsPage";

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "";
const resolveMediaUrl = (value) =>
  value?.startsWith("/") ? `${apiBaseUrl}${value}` : value;

const navGroups = [
  {
    label: "Hoạt động",
    items: [
      { key: "overview", label: "Tổng quan", path: "/account", icon: LayoutDashboard },
      { key: "tickets", label: "Vé & đơn hàng", path: "/account/tickets", icon: Ticket },
      { key: "offers", label: "Ưu đãi của tôi", path: "/account/offers", icon: Gift },
      { key: "loyalty", label: "Điểm & hạng thành viên", path: "/account/loyalty", icon: Award },
      { key: "notifications", label: "Thông báo", path: "/account/notifications", icon: Bell },
    ],
  },
  {
    label: "Tài khoản",
    items: [
      { key: "profile", label: "Hồ sơ cá nhân", path: "/account/profile", icon: User },
      { key: "security", label: "Bảo mật & đăng nhập", path: "/account/security", icon: ShieldCheck },
    ],
  },
  {
    label: "Hỗ trợ",
    items: [
      { key: "help", label: "Trung tâm trợ giúp", path: "/account/help", icon: CircleHelp },
    ],
  },
];

const pageMeta = {
  overview: {
    title: "Tổng quan tài khoản",
    description: "Vé sắp tới, ưu đãi và quyền lợi thành viên của bạn trong một nơi.",
  },
  tickets: {
    title: "Vé & đơn hàng",
    description: "Theo dõi vé đã đặt, đơn chờ thanh toán và các giao dịch trước đây.",
  },
  offers: {
    title: "Ưu đãi của tôi",
    description: "Xem voucher có thể dùng, ưu đãi có thể nhận và chương trình tự động áp dụng.",
  },
  loyalty: {
    title: "Điểm & hạng thành viên",
    description: "Theo dõi điểm khả dụng, tiến độ lên hạng và lịch sử tích điểm LoraFilm.",
  },
  notifications: {
    title: "Thông báo",
    description: "Cập nhật quan trọng về vé, thanh toán, ưu đãi và tài khoản của bạn.",
  },
  profile: {
    title: "Hồ sơ cá nhân",
    description: "Kiểm tra thông tin định danh và cập nhật cách LoraFilm liên hệ với bạn.",
  },
  security: {
    title: "Bảo mật & đăng nhập",
    description: "Quản lý email đăng nhập, mật khẩu và các thiết bị đang truy cập tài khoản.",
  },
  "security-email": {
    title: "Thay đổi email đăng nhập",
    description: "Email mới sẽ được dùng để đăng nhập và nhận các thông báo quan trọng.",
  },
  "security-password": {
    title: "Đổi mật khẩu",
    description: "Tạo mật khẩu mạnh và không sử dụng lại mật khẩu từ dịch vụ khác.",
  },
  "security-devices": {
    title: "Thiết bị đăng nhập",
    description: "Kiểm tra và đăng xuất những thiết bị bạn không còn sử dụng.",
  },
  help: {
    title: "Trung tâm trợ giúp",
    description: "Tìm câu trả lời nhanh hoặc liên hệ đội ngũ chăm sóc khách hàng LoraFilm.",
  },
};

const legacyTabMap = {
  info: "profile",
  history: "tickets",
  notifications: "notifications",
  gifts: "offers",
  policy: "loyalty",
  loyalty: "loyalty",
};

const resolveSection = (pathname, search) => {
  if (pathname === "/profile") {
    const tab = new URLSearchParams(search).get("tab");
    return legacyTabMap[tab] || "profile";
  }
  if (pathname.endsWith("/security/email")) return "security-email";
  if (pathname.endsWith("/security/password")) return "security-password";
  if (pathname.endsWith("/security/devices")) return "security-devices";
  if (pathname.endsWith("/tickets")) return "tickets";
  if (pathname.endsWith("/offers")) return "offers";
  if (pathname.endsWith("/loyalty")) return "loyalty";
  if (pathname.endsWith("/notifications")) return "notifications";
  if (pathname.endsWith("/profile")) return "profile";
  if (pathname.endsWith("/security")) return "security";
  if (pathname.endsWith("/help")) return "help";
  return "overview";
};

const baseSectionOf = (section) =>
  section.startsWith("security-") ? "security" : section;

const friendlyTierName = (tier) => {
  const value = String(tier || "Thành viên").trim();
  return value
    .replace(/\b(vip|member|membership)\b/gi, "")
    .replace(/\s+/g, " ")
    .trim() || "Thành viên";
};

const formatCurrency = (value) =>
  new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(Number(value) || 0);

const formatDate = (value) => {
  if (!value) return "Chưa cập nhật";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Chưa cập nhật";
  return date.toLocaleDateString("vi-VN");
};

const formatDateTime = (value) => {
  if (!value) return "Đang cập nhật";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Đang cập nhật";
  return date.toLocaleString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
};

function AccountNav({ activeSection, onNavigate }) {
  return (
    <nav aria-label="Danh mục tài khoản" className="space-y-6">
      {navGroups.map((group) => (
        <div key={group.label}>
          <p className="mb-2 px-3 text-[10px] font-black uppercase tracking-[0.18em] text-zinc-600">
            {group.label}
          </p>
          <div className="space-y-1">
            {group.items.map(({ key, label, path, icon: Icon }) => {
              const active = activeSection === key;
              return (
                <Link
                  key={key}
                  to={path}
                  aria-current={active ? "page" : undefined}
                  onClick={onNavigate}
                  className={`group flex min-h-11 items-center gap-3 rounded-xl px-3 text-sm font-bold transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange/70 ${
                    active
                      ? "bg-brand-orange text-white shadow-lg shadow-brand-orange/10"
                      : "text-zinc-400 hover:bg-zinc-800/70 hover:text-white"
                  }`}
                >
                  <Icon className={`h-4 w-4 ${active ? "text-white" : "text-zinc-600 group-hover:text-brand-orange"}`} />
                  <span>{label}</span>
                </Link>
              );
            })}
          </div>
        </div>
      ))}
    </nav>
  );
}

function ProfileSummary({ avatarUrl, name, tier, points }) {
  return (
    <div className="mb-7 flex items-center gap-3 border-b border-zinc-800 pb-6">
      <div className="flex h-14 w-14 shrink-0 items-center justify-center overflow-hidden rounded-full border-2 border-brand-orange/70 bg-zinc-950 text-brand-orange">
        {avatarUrl ? (
          <img src={avatarUrl} alt={name} className="h-full w-full object-cover" referrerPolicy="no-referrer" />
        ) : (
          <User className="h-6 w-6" />
        )}
      </div>
      <div className="min-w-0">
        <p className="truncate text-sm font-black text-white">{name}</p>
        <p className="mt-1 text-xs font-semibold text-zinc-400">
          <span className="text-brand-orange">{tier}</span> · {points === null ? "Đang tải điểm" : `${points.toLocaleString("vi-VN")} điểm`}
        </p>
      </div>
    </div>
  );
}

function OverviewCard({ icon: Icon, eyebrow, title, description, to, action, tone = "orange" }) {
  const toneClass = tone === "green"
    ? "bg-emerald-500/10 text-emerald-400"
    : tone === "blue"
      ? "bg-sky-500/10 text-sky-400"
      : "bg-brand-orange/10 text-brand-orange";
  return (
    <article className="flex min-h-56 flex-col rounded-2xl border border-zinc-800 bg-zinc-900/45 p-5 transition-colors hover:border-zinc-700 sm:p-6">
      <div className={`flex h-11 w-11 items-center justify-center rounded-xl ${toneClass}`}>
        <Icon className="h-5 w-5" />
      </div>
      <p className="mt-5 text-[10px] font-black uppercase tracking-[0.16em] text-zinc-600">{eyebrow}</p>
      <h2 className="mt-1 text-lg font-black leading-snug text-white">{title}</h2>
      <p className="mt-2 flex-1 text-xs leading-5 text-zinc-400">{description}</p>
      <Link to={to} className="mt-5 inline-flex items-center gap-1 text-xs font-black text-brand-orange hover:text-orange-300">
        {action} <ChevronRight className="h-3.5 w-3.5" />
      </Link>
    </article>
  );
}

function OverviewSection({ user, scoreData, spendingSummary, upcomingBooking }) {
  const firstName = user?.fullName?.trim().split(/\s+/).pop() || "bạn";
  const availablePoints = Number(scoreData?.currentPoints ?? 0);
  const tier = friendlyTierName(scoreData?.currentTier?.tierName);
  const presentation = upcomingBooking?.presentation || upcomingBooking?.snapshot || {};
  const movieTitle = upcomingBooking?.movieTitle || presentation.movieTitle;
  const showtime = upcomingBooking?.showtimeStart || presentation.showtimeStart;

  return (
    <div className="space-y-7">
      <section className="relative overflow-hidden rounded-3xl border border-brand-orange/20 bg-gradient-to-br from-brand-orange/[0.12] via-zinc-900/70 to-zinc-950 p-6 sm:p-8">
        <div className="absolute -right-16 -top-20 h-56 w-56 rounded-full bg-brand-orange/10 blur-3xl" />
        <div className="relative max-w-2xl">
          <p className="text-xs font-black uppercase tracking-[0.18em] text-brand-orange">Chào mừng trở lại</p>
          <h2 className="mt-2 text-2xl font-black text-white sm:text-3xl">Xin chào, {firstName}</h2>
          <p className="mt-3 text-sm leading-6 text-zinc-400">
            Mọi thứ bạn cần cho lần xem phim tiếp theo đã sẵn sàng ở đây.
          </p>
          <div className="mt-6 flex flex-wrap gap-3 text-xs font-bold">
            <span className="rounded-full border border-brand-orange/25 bg-brand-orange/10 px-3 py-2 text-brand-orange">{tier}</span>
            <span className="rounded-full border border-zinc-700 bg-zinc-900/70 px-3 py-2 text-zinc-300">{availablePoints.toLocaleString("vi-VN")} điểm khả dụng</span>
            <span className="rounded-full border border-zinc-700 bg-zinc-900/70 px-3 py-2 text-zinc-300">
              {spendingSummary === undefined ? "Đang tính chi tiêu năm" : `${formatCurrency(spendingSummary?.totalSpending)} chi tiêu năm nay`}
            </span>
          </div>
        </div>
      </section>

      <div className="grid gap-4 md:grid-cols-2">
        <OverviewCard
          icon={Ticket}
          eyebrow={upcomingBooking ? "Vé sắp tới" : "Vé của bạn"}
          title={movieTitle || "Chưa có suất chiếu sắp tới"}
          description={upcomingBooking ? `${formatDateTime(showtime)} · ${upcomingBooking.cinemaName || presentation.cinemaName || "LoraFilm"}` : "Chọn một bộ phim yêu thích và đặt vé cho buổi xem tiếp theo."}
          to="/account/tickets"
          action={upcomingBooking ? "Xem vé & đơn hàng" : "Khám phá vé của bạn"}
        />
        <OverviewCard
          icon={Award}
          eyebrow="Thành viên LoraFilm"
          title={`${tier} · ${availablePoints.toLocaleString("vi-VN")} điểm`}
          description="Điểm khả dụng dùng để đổi ưu đãi; tổng điểm đã tích lũy được dùng để xét hạng."
          to="/account/loyalty"
          action="Xem điểm & quyền lợi"
          tone="green"
        />
        <OverviewCard
          icon={WalletCards}
          eyebrow="Ưu đãi dành cho bạn"
          title="Voucher và ưu đãi thành viên"
          description="Kiểm tra voucher có thể dùng ngay và những ưu đãi mới đang chờ nhận."
          to="/account/offers"
          action="Xem ưu đãi"
          tone="blue"
        />
        <OverviewCard
          icon={Bell}
          eyebrow="Thông báo"
          title="Không bỏ lỡ cập nhật quan trọng"
          description="Theo dõi trạng thái vé, thanh toán và chương trình dành riêng cho thành viên."
          to="/account/notifications"
          action="Mở hộp thư"
        />
      </div>
    </div>
  );
}

function ProfileRow({ icon: Icon, label, value, note, action }) {
  return (
    <div className="flex flex-col gap-3 border-b border-zinc-800/80 py-5 last:border-0 sm:flex-row sm:items-center">
      <div className="flex min-w-0 flex-1 items-start gap-3">
        <span className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-zinc-800/80 text-zinc-500">
          <Icon className="h-4 w-4" />
        </span>
        <div className="min-w-0">
          <p className="text-[10px] font-black uppercase tracking-[0.14em] text-zinc-600">{label}</p>
          <p className="mt-1 break-words text-sm font-bold text-zinc-100">{value}</p>
          {note && <p className="mt-1 text-xs leading-5 text-zinc-500">{note}</p>}
        </div>
      </div>
      {action && <div className="shrink-0 sm:pl-4">{action}</div>}
    </div>
  );
}

function ProfileSection({ profile, email, avatarUrl, updateUser, onAvatarChanged }) {
  const [editing, setEditing] = useState(false);
  const [phone, setPhone] = useState(profile?.phoneNumber || "");
  const [saving, setSaving] = useState(false);
  const [avatarFile, setAvatarFile] = useState(null);
  const [avatarSaving, setAvatarSaving] = useState(false);
  const [notice, setNotice] = useState(null);

  useEffect(() => setPhone(profile?.phoneNumber || ""), [profile?.phoneNumber]);

  const saveProfile = async (event) => {
    event.preventDefault();
    if (!phone.trim()) {
      setNotice({ kind: "error", text: "Vui lòng nhập số điện thoại." });
      return;
    }
    setSaving(true);
    try {
      const updated = await updateUserProfile({ phoneNumber: phone.trim() });
      updateUser(updated);
      setEditing(false);
      setNotice({ kind: "success", text: "Thông tin liên hệ đã được cập nhật." });
    } catch (error) {
      setNotice({ kind: "error", text: error?.message || "Không thể cập nhật hồ sơ." });
    } finally {
      setSaving(false);
    }
  };

  const saveAvatar = async () => {
    if (!avatarFile) return;
    setAvatarSaving(true);
    try {
      const result = await uploadAvatar(avatarFile);
      updateUser({ avatarUrl: result.avatarUrl });
      onAvatarChanged(resolveMediaUrl(result.avatarUrl));
      setAvatarFile(null);
      setNotice({ kind: "success", text: "Ảnh đại diện đã được cập nhật." });
    } catch (error) {
      setNotice({ kind: "error", text: error?.message || "Không thể cập nhật ảnh đại diện." });
    } finally {
      setAvatarSaving(false);
    }
  };

  const genderLabel = { MALE: "Nam", FEMALE: "Nữ", OTHER: "Khác" }[profile?.gender] || "Chưa cập nhật";

  return (
    <div className="space-y-5">
      {notice && (
        <div role={notice.kind === "error" ? "alert" : "status"} className={`rounded-xl border px-4 py-3 text-sm ${notice.kind === "error" ? "border-red-500/25 bg-red-500/10 text-red-300" : "border-emerald-500/25 bg-emerald-500/10 text-emerald-300"}`}>
          {notice.text}
        </div>
      )}

      <section className="rounded-2xl border border-zinc-800 bg-zinc-900/45 p-5 sm:p-6">
        <div className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-4">
            <div className="flex h-20 w-20 items-center justify-center overflow-hidden rounded-full border-2 border-brand-orange/60 bg-zinc-950 text-zinc-600">
              {avatarUrl ? <img src={avatarUrl} alt={profile?.fullName || "Ảnh đại diện"} className="h-full w-full object-cover" /> : <User className="h-8 w-8" />}
            </div>
            <div>
              <h2 className="text-lg font-black text-white">Ảnh đại diện</h2>
              <p className="mt-1 text-xs text-zinc-500">JPEG, PNG hoặc WebP · tối đa 5 MB</p>
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <label className="inline-flex min-h-10 cursor-pointer items-center gap-2 rounded-xl border border-zinc-700 px-4 text-xs font-bold text-zinc-300 hover:border-brand-orange/50 hover:text-white">
              <Camera className="h-4 w-4" /> Chọn ảnh
              <input className="sr-only" type="file" accept="image/jpeg,image/png,image/webp" onChange={(event) => setAvatarFile(event.target.files?.[0] || null)} />
            </label>
            {avatarFile && (
              <button type="button" onClick={saveAvatar} disabled={avatarSaving} className="min-h-10 rounded-xl bg-brand-orange px-4 text-xs font-black text-white disabled:opacity-50">
                {avatarSaving ? "Đang tải..." : "Lưu ảnh"}
              </button>
            )}
          </div>
        </div>
      </section>

      <section className="rounded-2xl border border-zinc-800 bg-zinc-900/45 p-5 sm:p-6">
        <div className="flex items-start justify-between gap-4 border-b border-zinc-800 pb-5">
          <div>
            <h2 className="text-lg font-black text-white">Thông tin cá nhân</h2>
            <p className="mt-1 text-xs text-zinc-500">Thông tin định danh được bảo vệ và chỉ hiển thị khi cần thiết.</p>
          </div>
          {!editing && (
            <button type="button" onClick={() => setEditing(true)} className="shrink-0 rounded-xl border border-brand-orange/30 px-4 py-2 text-xs font-black text-brand-orange hover:bg-brand-orange/10">
              Chỉnh sửa
            </button>
          )}
        </div>

        {editing ? (
          <form onSubmit={saveProfile} className="mt-5 max-w-xl space-y-4">
            <div>
              <label htmlFor="profile-phone" className="mb-2 block text-xs font-bold text-zinc-400">Số điện thoại</label>
              <div className="relative">
                <Phone className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-600" />
                <input id="profile-phone" type="tel" value={phone} onChange={(event) => setPhone(event.target.value)} className="h-12 w-full rounded-xl border border-zinc-700 bg-zinc-950 pl-11 pr-4 text-sm text-white outline-none focus:border-brand-orange" />
              </div>
              <p className="mt-2 text-xs text-zinc-600">Họ tên, ngày sinh và giới tính được đồng bộ từ hồ sơ đã xác minh.</p>
            </div>
            <div className="flex gap-2">
              <button type="button" onClick={() => { setEditing(false); setPhone(profile?.phoneNumber || ""); }} className="rounded-xl border border-zinc-700 px-5 py-3 text-xs font-bold text-zinc-300">Hủy</button>
              <button type="submit" disabled={saving} className="rounded-xl bg-brand-orange px-5 py-3 text-xs font-black text-white disabled:opacity-50">{saving ? "Đang lưu..." : "Lưu thay đổi"}</button>
            </div>
          </form>
        ) : (
          <div>
            <ProfileRow icon={User} label="Họ và tên" value={profile?.fullName || "Chưa cập nhật"} />
            <ProfileRow icon={CalendarDays} label="Ngày sinh" value={formatDate(profile?.birthday)} />
            <ProfileRow icon={Phone} label="Số điện thoại" value={profile?.phoneNumber || "Chưa cập nhật"} />
            <ProfileRow icon={User} label="Giới tính" value={genderLabel} />
            <ProfileRow
              icon={ShieldCheck}
              label="Căn cước công dân"
              value={profile?.cccdMasked || "Chưa cập nhật"}
              note={profile?.cccdMasked ? "Thông tin đã được che để bảo vệ dữ liệu cá nhân. Liên hệ hỗ trợ nếu cần thay đổi." : "Thông tin này chỉ được sử dụng cho quy trình xác minh hồ sơ."}
            />
          </div>
        )}
      </section>

      <section className="rounded-2xl border border-zinc-800 bg-zinc-900/45 p-5 sm:p-6">
        <ProfileRow
          icon={Mail}
          label="Email đăng nhập"
          value={email || "Chưa cập nhật"}
          note="Đã xác minh · Thay đổi email sẽ thay đổi định danh đăng nhập của bạn."
          action={<Link to="/account/security/email" className="inline-flex items-center gap-1 text-xs font-black text-brand-orange">Thay đổi <ChevronRight className="h-3.5 w-3.5" /></Link>}
        />
      </section>
    </div>
  );
}

function SecurityCard({ icon: Icon, title, value, description, to, action }) {
  return (
    <article className="flex flex-col gap-4 border-b border-zinc-800 py-5 first:pt-0 last:border-0 last:pb-0 sm:flex-row sm:items-center">
      <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-zinc-800 text-zinc-500"><Icon className="h-5 w-5" /></span>
      <div className="min-w-0 flex-1">
        <h2 className="text-sm font-black text-white">{title}</h2>
        <p className="mt-1 break-words text-xs font-bold text-zinc-300">{value}</p>
        <p className="mt-1 text-xs leading-5 text-zinc-500">{description}</p>
      </div>
      {to ? <Link to={to} className="inline-flex min-h-10 shrink-0 items-center justify-center gap-1 rounded-xl border border-zinc-700 px-4 text-xs font-black text-zinc-200 hover:border-brand-orange/50 hover:text-brand-orange">{action} <ChevronRight className="h-3.5 w-3.5" /></Link> : <span className="shrink-0 rounded-full bg-zinc-800 px-3 py-1.5 text-[10px] font-black text-zinc-500">Sắp ra mắt</span>}
    </article>
  );
}

function SecuritySection({ user, email }) {
  const googleOnly = user?.hasPassword === false || String(user?.authProvider || user?.provider || "").toUpperCase() === "GOOGLE";
  return (
    <section className="rounded-2xl border border-zinc-800 bg-zinc-900/45 p-5 sm:p-6">
      <SecurityCard icon={Mail} title="Email đăng nhập" value={`${email || "Chưa cập nhật"} · Đã xác minh`} description="Dùng để đăng nhập và nhận cảnh báo bảo mật." to="/account/security/email" action="Thay đổi" />
      <SecurityCard icon={LockKeyhole} title="Mật khẩu" value={googleOnly ? "Đăng nhập bằng Google" : "Đã thiết lập"} description={googleOnly ? "Tài khoản của bạn đang được xác minh qua Google." : "Nên thay đổi nếu bạn nghi ngờ mật khẩu đã bị lộ."} to={googleOnly ? undefined : "/account/security/password"} action="Đổi mật khẩu" />
      <SecurityCard icon={Smartphone} title="Thiết bị đăng nhập" value="Kiểm tra các thiết bị đang hoạt động" description="Đăng xuất thiết bị lạ hoặc những thiết bị bạn không còn sử dụng." to="/account/security/devices" action="Quản lý" />
      <SecurityCard icon={KeyRound} title="Xác thực hai bước" value="Chưa bật" description="Thêm một lớp bảo vệ cho những thao tác nhạy cảm." />
    </section>
  );
}

function HelpSection() {
  const items = [
    { icon: ReceiptText, title: "Hướng dẫn thanh toán", description: "Các phương thức thanh toán và cách xử lý giao dịch chưa hoàn tất.", to: "/support/payment" },
    { icon: Ticket, title: "Đổi, hủy và hoàn vé", description: "Kiểm tra điều kiện áp dụng trước khi gửi yêu cầu hỗ trợ.", to: "/support/refunds" },
    { icon: CircleHelp, title: "Câu hỏi thường gặp", description: "Câu trả lời nhanh về đặt vé, điểm thành viên và tài khoản.", to: "/support/faq" },
  ];
  return (
    <div className="grid gap-4 md:grid-cols-3">
      {items.map(({ icon: Icon, title, description, to }) => (
        <Link key={to} to={to} className="group rounded-2xl border border-zinc-800 bg-zinc-900/45 p-5 transition-colors hover:border-brand-orange/40">
          <Icon className="h-5 w-5 text-brand-orange" />
          <h2 className="mt-4 text-sm font-black text-white">{title}</h2>
          <p className="mt-2 text-xs leading-5 text-zinc-500">{description}</p>
          <span className="mt-5 inline-flex items-center gap-1 text-xs font-black text-brand-orange">Xem hướng dẫn <ChevronRight className="h-3.5 w-3.5" /></span>
        </Link>
      ))}
      <section className="rounded-2xl border border-brand-orange/20 bg-brand-orange/[0.06] p-5 md:col-span-3">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-sm font-black text-white">Bạn vẫn cần hỗ trợ?</h2>
            <p className="mt-1 text-xs text-zinc-400">Hotline 1900 6868 · 10:00–22:00 mỗi ngày</p>
          </div>
          <a href="mailto:support@lorafilm.vn" className="inline-flex min-h-10 items-center justify-center rounded-xl bg-brand-orange px-4 text-xs font-black text-white">Liên hệ LoraFilm</a>
        </div>
      </section>
    </div>
  );
}

export default function AccountCenterPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const {
    user,
    profile,
    email,
    accountId,
    isAuthenticated,
    profileLoading,
    profilePending,
    profileError,
    refreshProfile,
    updateUser,
  } = useAuth();
  const { scoreData } = useCustomerScore();
  const section = resolveSection(location.pathname, location.search);
  const activeSection = baseSectionOf(section);
  const meta = pageMeta[section] || pageMeta.overview;
  const [spendingSummary, setSpendingSummary] = useState(undefined);
  const [upcomingBooking, setUpcomingBooking] = useState(null);
  const [avatarUrl, setAvatarUrl] = useState(resolveMediaUrl(user?.avatarUrl || profile?.avatarUrl || ""));

  useEffect(() => {
    if (!isAuthenticated || !accountId) navigate("/login", { replace: true });
  }, [accountId, isAuthenticated, navigate]);

  useEffect(() => {
    setAvatarUrl(resolveMediaUrl(user?.avatarUrl || profile?.avatarUrl || ""));
  }, [profile?.avatarUrl, user?.avatarUrl]);

  useEffect(() => {
    if (!accountId) return;
    let active = true;
    getBookingSpendingSummary(new Date().getFullYear())
      .then((value) => { if (active) setSpendingSummary(value); })
      .catch(() => { if (active) setSpendingSummary(null); });
    return () => { active = false; };
  }, [accountId]);

  useEffect(() => {
    if (section !== "overview" || !accountId) return;
    let active = true;
    getBookingHistory({ page: 0, size: 1, status: "CONFIRMED", sort: "createdAt,desc" })
      .then((page) => { if (active) setUpcomingBooking(page?.content?.[0] || null); })
      .catch(() => { if (active) setUpcomingBooking(null); });
    return () => { active = false; };
  }, [accountId, section]);

  const tier = friendlyTierName(scoreData?.currentTier?.tierName);
  const points = scoreData
    ? Number(scoreData.currentPoints ?? 0)
    : user?.points !== undefined
      ? Number(user.points)
      : null;
  const profileName = profile?.fullName || user?.fullName || email?.split("@")[0] || "Thành viên LoraFilm";
  const mobileNavItems = useMemo(() => navGroups.flatMap((group) => group.items), []);

  const content = (() => {
    if (profileLoading) {
      return <div className="flex min-h-80 items-center justify-center gap-3 text-sm font-bold text-zinc-500"><div className="h-6 w-6 animate-spin rounded-full border-2 border-brand-orange border-t-transparent" /> Đang tải tài khoản...</div>;
    }
    if (profilePending) {
      return <div className="rounded-2xl border border-amber-500/20 bg-amber-500/[0.05] p-8 text-center"><Clock3 className="mx-auto h-8 w-8 text-amber-400" /><h2 className="mt-4 font-black text-white">Hồ sơ đang được khởi tạo</h2><p className="mt-2 text-sm text-zinc-400">Vui lòng thử lại sau ít phút.</p><button type="button" onClick={refreshProfile} className="mt-5 rounded-xl bg-brand-orange px-5 py-3 text-xs font-black text-white">Tải lại hồ sơ</button></div>;
    }
    if (profileError && section === "profile") {
      return <div className="rounded-2xl border border-red-500/20 bg-red-500/[0.05] p-8 text-center"><p className="text-sm font-bold text-red-300">{profileError}</p><button type="button" onClick={refreshProfile} className="mt-5 rounded-xl bg-zinc-800 px-5 py-3 text-xs font-black text-white">Thử lại</button></div>;
    }
    switch (section) {
      case "tickets": return <CustomerBookingHistory />;
      case "offers": return <CustomerPromotionCenterPage embedded />;
      case "loyalty": return <LoyaltyCenterPage embedded />;
      case "notifications": return <CustomerNotificationCenter />;
      case "profile": return <ProfileSection profile={profile || user || {}} email={email} avatarUrl={avatarUrl} updateUser={updateUser} onAvatarChanged={setAvatarUrl} />;
      case "security": return <SecuritySection user={user} email={email} />;
      case "security-email": return <ChangeEmail embedded />;
      case "security-password": return <ChangePassword embedded />;
      case "security-devices": return <SessionsPage embedded />;
      case "help": return <HelpSection />;
      default: return <OverviewSection user={user} scoreData={scoreData} spendingSummary={spendingSummary} upcomingBooking={upcomingBooking} />;
    }
  })();

  return (
    <div className="min-h-[calc(100vh-5rem)] bg-[#070708] px-4 py-8 text-white sm:px-6 lg:px-8 lg:py-12">
      <div className="mx-auto max-w-7xl">
        <div className="mb-7 flex items-center gap-2 text-xs font-black uppercase tracking-[0.2em] text-brand-orange">
          <Sparkles className="h-4 w-4" /> Tài khoản của tôi
        </div>

        <div className="mb-6 lg:hidden">
          <label htmlFor="account-mobile-nav" className="mb-2 block text-[10px] font-black uppercase tracking-[0.16em] text-zinc-500">Danh mục tài khoản</label>
          <select id="account-mobile-nav" value={activeSection} onChange={(event) => navigate(mobileNavItems.find((item) => item.key === event.target.value)?.path || "/account")} className="h-12 w-full rounded-xl border border-zinc-800 bg-zinc-900 px-4 text-sm font-bold text-white outline-none focus:border-brand-orange">
            {navGroups.map((group) => (
              <optgroup key={group.label} label={group.label}>
                {group.items.map((item) => <option key={item.key} value={item.key}>{item.label}</option>)}
              </optgroup>
            ))}
          </select>
        </div>

        <div className="grid items-start gap-8 lg:grid-cols-[280px_minmax(0,1fr)]">
          <aside className="sticky top-28 hidden rounded-2xl border border-zinc-800 bg-zinc-900/55 p-4 shadow-2xl shadow-black/20 lg:block">
            <ProfileSummary avatarUrl={avatarUrl} name={profileName} tier={tier} points={points} />
            <AccountNav activeSection={activeSection} />
          </aside>

          <section className="min-w-0" aria-labelledby="account-page-title">
            {section.startsWith("security-") && (
              <Link to="/account/security" className="mb-4 inline-flex items-center gap-1 text-xs font-bold text-zinc-500 hover:text-brand-orange">← Quay lại bảo mật tài khoản</Link>
            )}
            <header className="mb-7 border-b border-zinc-800 pb-6">
              <h1 id="account-page-title" className="text-2xl font-black tracking-tight text-white sm:text-3xl">{meta.title}</h1>
              <p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-400">{meta.description}</p>
            </header>
            {content}
          </section>
        </div>
      </div>
    </div>
  );
}
