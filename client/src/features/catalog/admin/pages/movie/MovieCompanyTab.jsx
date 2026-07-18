import { useState, useEffect } from 'react';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import { useOutletContext } from 'react-router-dom';
import { Select, Input } from '@/components/common/ui/uiKit';
import { Loader2, Save, Plus, Trash2 } from 'lucide-react';
import { parseApiError } from '@/utils/apiErrorHandler';

const COMPANY_ROLES = [
  { value: 'PRODUCTION', label: 'Sản xuất' },
  { value: 'DISTRIBUTOR', label: 'Phân phối' },
  { value: 'STUDIO', label: 'Studio' }
];

export default function MovieCompanyTab({ movie, onUpdate }) {
  const { triggerToast } = useOutletContext();
  const [companies, setCompanies] = useState([]);
  const [isSaving, setIsSaving] = useState(false);
  const [newCompany, setNewCompany] = useState({ name: '', role: 'PRODUCTION' });
  const [isAddingCompany, setIsAddingCompany] = useState(false);

  useEffect(() => {
    if (movie) {
      const allCompanies = [
        ...(movie.productionCompanies || []).map(c => ({ ...c, role: 'PRODUCTION' })),
        ...(movie.distributors || []).map(c => ({ ...c, role: 'DISTRIBUTOR' })),
        ...(movie.studios || []).map(c => ({ ...c, role: 'STUDIO' }))
      ];
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setCompanies(allCompanies.map((c, i) => ({ ...c, localId: Date.now() + i })));
    }
  }, [movie]);

  const handleAddCompanyLocal = async () => {
    if (!newCompany.name.trim()) {
      triggerToast('Tên hãng không được bỏ trống', 'error');
      return;
    }
    setIsAddingCompany(true);
    try {
      const company = await adminMovieService.ensureProductionCompanyExists(newCompany.name);
      if (!company) {
        triggerToast('Không thể xác thực/tạo hãng phim', 'error');
        return;
      }

      // eslint-disable-next-line react-hooks/set-state-in-effect
      setCompanies(prev => [...prev, {
        companyPublicId: company.publicId,
        publicId: company.publicId,
        name: company.name,
        role: newCompany.role,
        localId: Date.now()
      }]);
      setNewCompany({ name: '', role: 'PRODUCTION' });
    } catch (err) {
      triggerToast(parseApiError(err), 'error');
    } finally {
      setIsAddingCompany(false);
    }
  };

  const removeCompanyLocal = (localId) => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
      setCompanies(prev => prev.filter(c => c.localId !== localId));
  };

  const handleSave = async () => {
    setIsSaving(true);
    try {
      const payload = companies.map((c) => ({
        companyPublicId: c.companyPublicId || c.publicId,
        role: c.role
      }));
      const res = await adminMovieService.assignProductionCompanies(movie.publicId, payload);
      if (res?.success) {
        triggerToast('Cập nhật hãng phim thành công');
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
        <h3 className="text-sm font-semibold text-zinc-100">Hãng phim & Phân phối</h3>
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
        <h4 className="text-xs font-semibold text-zinc-400">Thêm hãng mới</h4>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
          <Input 
            label="Tên hãng" 
            value={newCompany.name} 
            onChange={e => setNewCompany(p => ({ ...p, name: e.target.value }))}
            placeholder="Vd: Warner Bros."
          />
          <Select 
            label="Vai trò" 
            value={newCompany.role} 
            onChange={e => setNewCompany(p => ({ ...p, role: e.target.value }))}
          >
            {COMPANY_ROLES.map(r => <option key={r.value} value={r.value}>{r.label}</option>)}
          </Select>
          <button
            onClick={handleAddCompanyLocal}
            disabled={isAddingCompany}
            className="flex items-center justify-center gap-2 bg-zinc-800 text-white px-4 py-2 rounded-lg text-xs font-semibold hover:bg-zinc-700 transition-colors h-[42px] disabled:opacity-50"
          >
            {isAddingCompany ? <Loader2 size={14} className="animate-spin" /> : <Plus size={14} />}
            Thêm vào ds
          </button>
        </div>
      </div>

      <div className="bg-[#0a0a0a] border border-zinc-800 rounded-xl overflow-hidden">
        <table className="w-full text-left text-sm text-zinc-400">
          <thead className="text-xs text-zinc-500 bg-[#050506] border-b border-zinc-800">
            <tr>
              <th className="px-4 py-3 font-medium">Tên hãng</th>
              <th className="px-4 py-3 font-medium">Vai trò</th>
              <th className="px-4 py-3 font-medium text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-800/50">
            {companies.map((c) => (
              <tr key={c.localId} className="hover:bg-zinc-800/20 transition-colors">
                <td className="px-4 py-3 text-zinc-200">{c.name}</td>
                <td className="px-4 py-3">{COMPANY_ROLES.find(r => r.value === c.role)?.label || c.role}</td>
                <td className="px-4 py-3 text-right">
                  <button
                    onClick={() => removeCompanyLocal(c.localId)}
                    className="p-1.5 text-zinc-500 hover:text-red-500 hover:bg-red-500/10 rounded-lg transition-colors"
                  >
                    <Trash2 size={14} />
                  </button>
                </td>
              </tr>
            ))}
            {companies.length === 0 && (
              <tr>
                <td colSpan="3" className="px-4 py-8 text-center text-zinc-500 border-dashed border border-zinc-800/50">
                  Chưa có hãng nào được thêm.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
