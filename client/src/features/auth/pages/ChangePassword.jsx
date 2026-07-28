import { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import AuthActionCard from '../components/AuthActionCard';
import { changePassword } from '../services/authService';
import { useAuth } from '@/contexts/AuthContext';
import { useNavigate } from 'react-router-dom';

const strongPassword = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,100}$/;

export default function ChangePassword() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ oldPassword: '', newPassword: '', confirm: '' });
  const [showPassword, setShowPassword] = useState({ old: false, new: false, confirm: false });
  const [feedback, setFeedback] = useState({ error: '', success: '' });
  const [loading, setLoading] = useState(false);
  
  const update = (field) => (event) => setForm((value) => ({ ...value, [field]: event.target.value }));
  const toggleVisibility = (field) => () => setShowPassword((prev) => ({ ...prev, [field]: !prev[field] }));

  const calculateStrength = (pwd) => {
    if (!pwd) return 0;
    let score = 0;
    if (pwd.length >= 8) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/[a-z]/.test(pwd)) score++;
    if (/[0-9]/.test(pwd)) score++;
    if (/[!@#$%^&*(),.?":{}|<>]/.test(pwd)) score++;
    return score;
  };

  const strength = calculateStrength(form.newPassword);
  const strengthLabels = ["", "Rất yếu", "Yếu", "Trung bình", "Khá", "Rất mạnh"];
  const strengthColors = ["bg-zinc-800", "bg-red-500", "bg-orange-500", "bg-amber-400", "bg-emerald-400", "bg-emerald-500"];

  const submit = async (event) => {
    event.preventDefault();
    if (!strongPassword.test(form.newPassword)) return setFeedback({ error: 'Mật khẩu mới cần chữ hoa, chữ thường, số, ký tự đặc biệt và ít nhất 8 ký tự.', success: '' });
    if (form.newPassword !== form.confirm) return setFeedback({ error: 'Mật khẩu xác nhận không khớp.', success: '' });
    setLoading(true);
    try {
      await changePassword(form.oldPassword, form.newPassword);
      setForm({ oldPassword: '', newPassword: '', confirm: '' });
      setFeedback({ error: '', success: 'Đổi mật khẩu thành công. Đang chuyển đến trang đăng nhập...' });
      await logout();
      navigate('/login', {
        replace: true,
        state: { message: 'Mật khẩu đã được thay đổi. Vui lòng đăng nhập lại.' }
      });
    } catch (reason) {
      setFeedback({ error: reason?.message || 'Không thể đổi mật khẩu.', success: '' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthActionCard title="Đổi mật khẩu">
      <form onSubmit={submit} className="space-y-4">
        <div className="relative">
          <input aria-label="Mật khẩu hiện tại" type={showPassword.old ? "text" : "password"} required placeholder="Mật khẩu hiện tại" value={form.oldPassword} onChange={update('oldPassword')} className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 pr-12" />
          <button type="button" onClick={toggleVisibility('old')} className="absolute right-4 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-white focus:outline-none">
            {showPassword.old ? <EyeOff size={20} /> : <Eye size={20} />}
          </button>
        </div>
        <div className="relative">
          <div className="relative">
            <input aria-label="Mật khẩu mới" type={showPassword.new ? "text" : "password"} required placeholder="Mật khẩu mới" value={form.newPassword} onChange={update('newPassword')} className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 pr-12" />
            <button type="button" onClick={toggleVisibility('new')} className="absolute right-4 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-white focus:outline-none">
              {showPassword.new ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          </div>
          {form.newPassword && (
            <div className="w-full mt-2 space-y-1.5 animate-fade-in">
              <div className="flex gap-1 h-1.5 w-full">
                {[1, 2, 3, 4, 5].map((level) => (
                  <div
                    key={level}
                    className={`h-full flex-1 rounded-full transition-all duration-300 ${
                      strength >= level ? strengthColors[strength] : "bg-zinc-800"
                    }`}
                  />
                ))}
              </div>
              <p className={`text-[10px] font-bold uppercase tracking-wider text-right ${
                strength <= 2 ? 'text-red-400' : strength <= 4 ? 'text-amber-400' : 'text-emerald-400'
              }`}>
                {strengthLabels[strength]}
              </p>
            </div>
          )}
        </div>
        <div className="relative">
          <input aria-label="Xác nhận mật khẩu" type={showPassword.confirm ? "text" : "password"} required placeholder="Xác nhận mật khẩu" value={form.confirm} onChange={update('confirm')} className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 pr-12" />
          <button type="button" onClick={toggleVisibility('confirm')} className="absolute right-4 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-white focus:outline-none">
            {showPassword.confirm ? <EyeOff size={20} /> : <Eye size={20} />}
          </button>
        </div>
        {feedback.error && <p className="text-sm text-red-400">{feedback.error}</p>}
        {feedback.success && <p className="text-sm text-emerald-400">{feedback.success}</p>}
        <button disabled={loading} className="w-full rounded-xl bg-orange-500 py-3 font-black text-zinc-950 disabled:opacity-60">{loading ? 'Đang cập nhật...' : 'Đổi mật khẩu'}</button>
      </form>
    </AuthActionCard>
  );
}
