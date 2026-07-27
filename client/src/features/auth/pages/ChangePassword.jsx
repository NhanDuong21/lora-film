import { useState } from 'react';
import AuthActionCard from '../components/AuthActionCard';
import { changePassword } from '../services/authService';

const strongPassword = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,100}$/;

export default function ChangePassword() {
  const [form, setForm] = useState({ oldPassword: '', newPassword: '', confirm: '' });
  const [feedback, setFeedback] = useState({ error: '', success: '' });
  const [loading, setLoading] = useState(false);
  const update = (field) => (event) => setForm((value) => ({ ...value, [field]: event.target.value }));

  const submit = async (event) => {
    event.preventDefault();
    if (!strongPassword.test(form.newPassword)) return setFeedback({ error: 'Mật khẩu mới cần chữ hoa, chữ thường, số, ký tự đặc biệt và ít nhất 8 ký tự.', success: '' });
    if (form.newPassword !== form.confirm) return setFeedback({ error: 'Mật khẩu xác nhận không khớp.', success: '' });
    setLoading(true);
    try {
      await changePassword(form.oldPassword, form.newPassword);
      setForm({ oldPassword: '', newPassword: '', confirm: '' });
      setFeedback({ error: '', success: 'Đổi mật khẩu thành công. Các phiên khác đã bị thu hồi.' });
    } catch (reason) {
      setFeedback({ error: reason?.message || 'Không thể đổi mật khẩu.', success: '' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthActionCard title="Đổi mật khẩu">
      <form onSubmit={submit} className="space-y-4">
        <input aria-label="Mật khẩu hiện tại" type="password" required placeholder="Mật khẩu hiện tại" value={form.oldPassword} onChange={update('oldPassword')} className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3" />
        <input aria-label="Mật khẩu mới" type="password" required placeholder="Mật khẩu mới" value={form.newPassword} onChange={update('newPassword')} className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3" />
        <input aria-label="Xác nhận mật khẩu" type="password" required placeholder="Xác nhận mật khẩu" value={form.confirm} onChange={update('confirm')} className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3" />
        {feedback.error && <p className="text-sm text-red-400">{feedback.error}</p>}
        {feedback.success && <p className="text-sm text-emerald-400">{feedback.success}</p>}
        <button disabled={loading} className="w-full rounded-xl bg-orange-500 py-3 font-black text-zinc-950 disabled:opacity-60">{loading ? 'Đang cập nhật...' : 'Đổi mật khẩu'}</button>
      </form>
    </AuthActionCard>
  );
}
