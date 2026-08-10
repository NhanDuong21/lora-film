import { Check, ChevronRight, CircleHelp, X } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { getOptimizedImageUrl } from '@/utils/imageOptimization';
import { getRoleFallbackAvatar } from './avatarUtils';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '';
const resolveMediaUrl = value => value?.startsWith('/') ? `${apiBaseUrl}${value}` : value;

const STEP_TONES = {
  done: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300',
  active: 'border-orange-500/40 bg-orange-500/10 text-orange-200',
  waiting: 'border-white/10 bg-white/[0.025] text-zinc-500'
};

export function HrHero({ title, description, context, actions }) {
  return (
    <header className="relative overflow-hidden rounded-[28px] border border-white/10 bg-[#0b0b0d] px-6 py-7 md:px-8 md:py-9">
      <div className="pointer-events-none absolute -right-20 -top-24 h-72 w-72 rounded-full bg-orange-500/10 blur-3xl" />
      <div className="pointer-events-none absolute bottom-0 left-1/3 h-24 w-64 bg-blue-500/5 blur-3xl" />
      <div className="relative flex flex-col gap-6 2xl:flex-row 2xl:items-end 2xl:justify-between">
        <div className="max-w-3xl">
          <p className="mb-3 text-[11px] font-black uppercase tracking-[0.24em] text-orange-400">
            {context || 'Không gian vận hành nhân sự'}
          </p>
          <h1 className="text-3xl font-black tracking-[-0.03em] text-white md:text-4xl">{title}</h1>
          <p className="mt-3 max-w-2xl text-sm leading-6 text-zinc-400">{description}</p>
        </div>
        {actions ? <div className="flex flex-wrap gap-2">{actions}</div> : null}
      </div>
    </header>
  );
}

export function WorkflowSteps({ steps = [] }) {
  return (
    <div className="overflow-x-auto rounded-2xl border border-white/10 bg-[#0b0b0e] p-3">
      <div className="flex min-w-max items-center">
        {steps.map((step, index) => (
          <div key={step.label} className="flex items-center">
            <div className={'flex min-w-40 items-center gap-3 rounded-xl border px-4 py-3 ' + (STEP_TONES[step.state] || STEP_TONES.waiting)}>
              <span className="grid h-7 w-7 shrink-0 place-items-center rounded-full border border-current/30 text-xs font-black">
                {step.state === 'done' ? <Check size={15} /> : index + 1}
              </span>
              <div>
                <p className="text-xs font-black">{step.label}</p>
                {step.hint ? <p className="mt-0.5 text-[10px] opacity-70">{step.hint}</p> : null}
              </div>
            </div>
            {index < steps.length - 1 ? <ChevronRight className="mx-1 text-zinc-700" size={18} /> : null}
          </div>
        ))}
      </div>
    </div>
  );
}

export function PersonAvatar({ name = '', avatarUrl = '', role = '', size = 'md' }) {
  const [failedAvatarUrl, setFailedAvatarUrl] = useState('');
  const [failedFallbackUrl, setFailedFallbackUrl] = useState('');
  const initials = name.trim().split(/\s+/).slice(-2).map(part => part[0]).join('').toUpperCase() || 'NV';
  const dimensions = size === 'lg' ? 'h-12 w-12 text-sm' : 'h-9 w-9 text-xs';
  const imageDimensions = size === 'lg' ? 96 : 72;
  const resolvedAvatarUrl = getOptimizedImageUrl(resolveMediaUrl(avatarUrl), {
    width: imageDimensions,
    height: imageDimensions,
    quality: 90,
    gravity: 'face'
  });
  const fallbackAvatarUrl = getRoleFallbackAvatar(role);

  if (resolvedAvatarUrl && failedAvatarUrl !== resolvedAvatarUrl) {
    return <img
      src={resolvedAvatarUrl}
      alt={`Ảnh đại diện của ${name}`}
      className={'shrink-0 rounded-xl border border-orange-500/20 object-cover ' + dimensions}
      onError={() => setFailedAvatarUrl(resolvedAvatarUrl)}
    />;
  }

  if (fallbackAvatarUrl && failedFallbackUrl !== fallbackAvatarUrl) {
    return <img
      src={fallbackAvatarUrl}
      alt={`Ảnh mặc định theo vai trò của ${name}`}
      className={'shrink-0 rounded-xl border border-orange-500/20 bg-orange-500/10 object-cover ' + dimensions}
      onError={() => setFailedFallbackUrl(fallbackAvatarUrl)}
    />;
  }

  return (
    <span className={'grid shrink-0 place-items-center rounded-xl border border-orange-500/20 bg-orange-500/10 font-black text-orange-300 ' + dimensions}>
      {initials}
    </span>
  );
}

export function UatGuide({ compact = false }) {
  const [open, setOpen] = useState(false);
  const checks = [
    { title: 'Tạo hồ sơ nhân viên', text: 'Chọn một tài khoản nhân viên và gắn đúng phòng ban, vị trí, lương cơ bản.', href: '/admin/staff' },
    { title: 'Phân ca trong tuần', text: 'Tạo ca, kiểm tra ca xuất hiện đúng ngày và không bị trùng giờ.', href: '/admin/workforce' },
    { title: 'Duyệt nghỉ phép', text: 'Đăng nhập nhân viên để gửi đơn, sau đó dùng tài khoản admin khác để duyệt.', href: '/admin/approvals' },
    { title: 'Kiểm tra chấm công', text: 'Xem giờ vào/ra, trường hợp đi muộn và thử hiệu chỉnh có nhập lý do.', href: '/admin/workforce?view=attendance' },
    { title: 'Chạy kỳ lương', text: 'Sinh dữ liệu từ chấm công, duyệt, gửi ngân hàng và đối soát theo đúng thứ tự.', href: '/admin/payroll' },
    { title: 'Kiểm tra lịch sử', text: 'Mở hồ sơ nhân viên để xem các lần điều chuyển hoặc thay đổi lương.', href: '/admin/staff' }
  ];

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className={compact
          ? 'inline-flex items-center gap-2 rounded-xl border border-white/10 px-3 py-2 text-xs font-black text-zinc-300 hover:bg-white/5'
          : 'inline-flex items-center gap-2 rounded-xl border border-blue-400/20 bg-blue-400/10 px-4 py-2.5 text-sm font-black text-blue-200 hover:bg-blue-400/15'}
      >
        <CircleHelp size={17} /> Cách kiểm tra
      </button>
      {open ? (
        <div className="fixed inset-0 z-[70] flex justify-end bg-black/75 backdrop-blur-sm" onMouseDown={event => event.target === event.currentTarget && setOpen(false)}>
          <aside role="dialog" aria-modal="true" aria-label="Hướng dẫn kiểm tra nghiệp vụ nhân sự" className="flex h-full w-full max-w-lg flex-col border-l border-white/10 bg-[#09090b]">
            <header className="flex items-start justify-between border-b border-white/10 p-6">
              <div>
                <p className="text-[10px] font-black uppercase tracking-[0.2em] text-blue-300">Kiểm thử dành cho người vận hành</p>
                <h2 className="mt-2 text-2xl font-black text-white">Bạn chỉ cần kiểm tra 6 việc</h2>
                <p className="mt-2 text-sm leading-6 text-zinc-400">Không cần biết API. Làm lần lượt, hệ thống sẽ tự chặn nếu sai quy trình.</p>
              </div>
              <button type="button" aria-label="Đóng hướng dẫn" onClick={() => setOpen(false)} className="rounded-xl p-2 text-zinc-500 hover:bg-white/5 hover:text-white"><X size={20} /></button>
            </header>
            <div className="flex-1 space-y-3 overflow-y-auto p-6">
              {checks.map((check, index) => (
                <Link key={check.title} to={check.href} onClick={() => setOpen(false)} className="group flex gap-4 rounded-2xl border border-white/10 bg-white/[0.025] p-4 hover:border-orange-500/30 hover:bg-orange-500/5">
                  <span className="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-white/5 text-xs font-black text-zinc-300">{index + 1}</span>
                  <span>
                    <span className="block text-sm font-black text-zinc-100 group-hover:text-orange-300">{check.title}</span>
                    <span className="mt-1 block text-xs leading-5 text-zinc-500">{check.text}</span>
                  </span>
                </Link>
              ))}
            </div>
            <footer className="border-t border-white/10 p-5 text-xs leading-5 text-zinc-500">
              Mẹo: với bước duyệt nghỉ hoặc duyệt lương, người tạo và người duyệt phải là hai tài khoản khác nhau.
            </footer>
          </aside>
        </div>
      ) : null}
    </>
  );
}

export function EmptyWorkspace({ title, description, action }) {
  return (
    <div className="grid min-h-52 place-items-center rounded-2xl border border-dashed border-white/10 bg-white/[0.015] p-8 text-center">
      <div>
        <p className="font-black text-zinc-200">{title}</p>
        <p className="mt-2 max-w-md text-sm leading-6 text-zinc-500">{description}</p>
        {action ? <div className="mt-4">{action}</div> : null}
      </div>
    </div>
  );
}
