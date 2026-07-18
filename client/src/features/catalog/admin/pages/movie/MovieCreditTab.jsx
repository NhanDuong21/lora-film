import { useState, useEffect } from 'react';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import { useOutletContext } from 'react-router-dom';
import { Select, Input } from '@/components/common/ui/uiKit';
import { Loader2, Save, Plus, Trash2 } from 'lucide-react';
import { parseApiError } from '@/utils/apiErrorHandler';

const ROLE_TYPES = [
  { value: 'DIRECTOR', label: 'Đạo diễn' },
  { value: 'MAIN_ACTOR', label: 'Diễn viên chính' },
  { value: 'SUPPORTING_ACTOR', label: 'Diễn viên phụ' },
  { value: 'VOICE_ACTOR', label: 'Diễn viên lồng tiếng' },
  { value: 'WRITER', label: 'Biên kịch' },
  { value: 'PRODUCER', label: 'Nhà sản xuất' },
  { value: 'GUEST', label: 'Khách mời' }
];

export default function MovieCreditTab({ movie, onUpdate }) {
  const { triggerToast } = useOutletContext();
  const [credits, setCredits] = useState([]);
  const [isSaving, setIsSaving] = useState(false);
  const [newCredit, setNewCredit] = useState({ fullName: '', roleType: 'MAIN_ACTOR', characterName: '', profileImageUrl: '' });
  const [isAddingPerson, setIsAddingPerson] = useState(false);

  useEffect(() => {
    if (movie) {
      const allCredits = [
        ...(movie.directors || []).map(p => ({ ...p, roleType: 'DIRECTOR' })),
        ...(movie.actors || []).map(p => ({ ...p, roleType: p.roleType || 'MAIN_ACTOR' })), // the DTO might just dump actors here
        ...(movie.writers || []).map(p => ({ ...p, roleType: 'WRITER' })),
        ...(movie.producers || []).map(p => ({ ...p, roleType: 'PRODUCER' }))
      ];
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setCredits(allCredits.map((c, i) => ({ ...c, localId: Date.now() + i, displayOrder: i + 1 })));
    }
  }, [movie]);

  const handleAddCreditLocal = async () => {
    if (!newCredit.fullName.trim()) {
      triggerToast('Tên nhân sự không được bỏ trống', 'error');
      return;
    }
    setIsAddingPerson(true);
    try {
      // Create or find person to get publicId
      const person = await adminMovieService.ensurePersonExists(newCredit.fullName, newCredit.profileImageUrl);
      if (!person) {
        triggerToast('Không thể xác thực/tạo nhân sự', 'error');
        return;
      }

      // eslint-disable-next-line react-hooks/set-state-in-effect
      setCredits(prev => [...prev, {
        personPublicId: person.publicId,
        publicId: person.publicId, // Display purpose
        fullName: person.fullName,
        roleType: newCredit.roleType,
        characterName: newCredit.characterName,
        localId: Date.now(),
        displayOrder: prev.length + 1
      }]);
      setNewCredit({ fullName: '', roleType: 'MAIN_ACTOR', characterName: '', profileImageUrl: '' });
    } catch (err) {
      triggerToast(parseApiError(err), 'error');
    } finally {
      setIsAddingPerson(false);
    }
  };

  const removeCreditLocal = (localId) => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
      setCredits(prev => prev.filter(c => c.localId !== localId));
  };

  const handleSave = async () => {
    setIsSaving(true);
    try {
      const payload = credits.map((c, index) => ({
        personPublicId: c.personPublicId || c.publicId,
        roleType: c.roleType,
        characterName: c.characterName || null,
        displayOrder: index + 1
      }));
      const res = await adminMovieService.assignCredits(movie.publicId, payload);
      if (res?.success) {
        triggerToast('Cập nhật đội ngũ thành công');
        onUpdate?.();
      } else {
        triggerToast('Cập nhật thất bại', 'error');
      }
    } catch (err) {
      triggerToast(parseApiError(err), 'error');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-zinc-100">Đội ngũ làm phim</h3>
        <button
          onClick={handleSave}
          disabled={isSaving}
          className="flex items-center gap-2 bg-brand-orange text-white px-4 py-2 rounded-lg text-xs font-semibold hover:bg-brand-orange/90 transition-colors disabled:opacity-50"
        >
          {isSaving ? <Loader2 size={14} className="animate-spin" /> : <Save size={14} />}
          Lưu danh sách
        </button>
      </div>

      <div className="bg-[#050506] border border-zinc-800 rounded-xl p-4 space-y-4">
        <h4 className="text-xs font-semibold text-zinc-400">Thêm nhân sự mới</h4>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
          <Input 
            label="Họ tên" 
            value={newCredit.fullName} 
            onChange={e => setNewCredit(p => ({ ...p, fullName: e.target.value }))}
            placeholder="Vd: Christopher Nolan"
          />
          <Select 
            label="Vai trò" 
            value={newCredit.roleType} 
            onChange={e => setNewCredit(p => ({ ...p, roleType: e.target.value }))}
          >
            {ROLE_TYPES.map(r => <option key={r.value} value={r.value}>{r.label}</option>)}
          </Select>
          <Input 
            label="Vai diễn (Nếu là Diễn viên)" 
            value={newCredit.characterName} 
            onChange={e => setNewCredit(p => ({ ...p, characterName: e.target.value }))}
            placeholder="Tên nhân vật..."
          />
          <button
            onClick={handleAddCreditLocal}
            disabled={isAddingPerson}
            className="flex items-center justify-center gap-2 bg-zinc-800 text-white px-4 py-2 rounded-lg text-xs font-semibold hover:bg-zinc-700 transition-colors h-[42px] disabled:opacity-50"
          >
            {isAddingPerson ? <Loader2 size={14} className="animate-spin" /> : <Plus size={14} />}
            Thêm vào ds
          </button>
        </div>
      </div>

      <div className="bg-[#0a0a0a] border border-zinc-800 rounded-xl overflow-hidden">
        <table className="w-full text-left text-sm text-zinc-400">
          <thead className="text-xs text-zinc-500 bg-[#050506] border-b border-zinc-800">
            <tr>
              <th className="px-4 py-3 font-medium">Họ tên</th>
              <th className="px-4 py-3 font-medium">Vai trò</th>
              <th className="px-4 py-3 font-medium">Vai diễn</th>
              <th className="px-4 py-3 font-medium text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-800/50">
            {credits.map((c) => (
              <tr key={c.localId} className="hover:bg-zinc-800/20 transition-colors">
                <td className="px-4 py-3 text-zinc-200">{c.fullName}</td>
                <td className="px-4 py-3">{ROLE_TYPES.find(r => r.value === c.roleType)?.label || c.roleType}</td>
                <td className="px-4 py-3">{c.characterName || '-'}</td>
                <td className="px-4 py-3 text-right">
                  <button
                    onClick={() => removeCreditLocal(c.localId)}
                    className="p-1.5 text-zinc-500 hover:text-red-500 hover:bg-red-500/10 rounded-lg transition-colors"
                  >
                    <Trash2 size={14} />
                  </button>
                </td>
              </tr>
            ))}
            {credits.length === 0 && (
              <tr>
                <td colSpan="4" className="px-4 py-8 text-center text-zinc-500 border-dashed border border-zinc-800/50">
                  Chưa có nhân sự nào được thêm.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
