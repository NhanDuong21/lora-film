import { useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getEmployeeDocuments, uploadEmployeeDocument, downloadEmployeeDocument, deleteEmployeeDocument } from '../services/userAdminService';
import { AsyncState, Input, Select } from '@/components/common/ui/uiKit';
import { ArrowLeft, File, Download, Trash, FileText, FileBadge, FileCheck, UploadCloud, FileImage, ShieldAlert } from 'lucide-react';

export default function AdminEmployeeDocumentPage() {
  const { accountId } = useParams();
  const navigate = useNavigate();
  const [documents, setDocuments] = useState([]);
  const [state, setState] = useState({ loading: true, error: '' });
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({ file: null, documentType: 'CONTRACT', documentName: '', issuedDate: '', expiredDate: '' });
  const [stats, setStats] = useState({ total: 0, contracts: 0, ids: 0 });

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const data = await getEmployeeDocuments(accountId, true);
      const docs = data || [];
      setDocuments(docs);
      
      setStats({
        total: docs.length,
        contracts: docs.filter(d => d.documentType === 'CONTRACT').length,
        ids: docs.filter(d => d.documentType === 'ID_CARD').length
      });
      
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải tài liệu nhân viên.' });
    }
  }, [accountId]);

  useEffect(() => { load(); }, [load]);

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!formData.file) {
      alert('Vui lòng chọn file đính kèm');
      return;
    }
    try {
      await uploadEmployeeDocument(accountId, formData);
      setIsModalOpen(false);
      setFormData({ file: null, documentType: 'CONTRACT', documentName: '', issuedDate: '', expiredDate: '' });
      await load();
    } catch (error) {
      alert(error?.message || 'Lỗi khi tải lên tài liệu');
    }
  };

  const handleDelete = async (docId) => {
    if (!window.confirm('Hành động này không thể hoàn tác. Bạn có chắc muốn xóa tài liệu này?')) return;
    try {
      await deleteEmployeeDocument(accountId, docId);
      await load();
    } catch (error) {
      alert(error?.message || 'Lỗi khi xóa tài liệu');
    }
  };

  const handleDownload = async (docId, fileName) => {
    try {
      const blob = await downloadEmployeeDocument(accountId, docId);
      const url = window.URL.createObjectURL(new Blob([blob]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', fileName || 'document.pdf');
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
    } catch (error) {
      alert('Lỗi khi tải xuống tài liệu');
    }
  };

  const getDocIcon = (type) => {
    switch(type) {
      case 'CONTRACT': return <FileText className="w-6 h-6 text-emerald-400" />;
      case 'ID_CARD': return <FileBadge className="w-6 h-6 text-blue-400" />;
      case 'CERTIFICATE': return <FileCheck className="w-6 h-6 text-purple-400" />;
      default: return <FileImage className="w-6 h-6 text-amber-400" />;
    }
  };

  const getDocTypeLabel = (type) => {
    switch(type) {
      case 'CONTRACT': return 'Hợp đồng lao động';
      case 'ID_CARD': return 'CMND / CCCD';
      case 'CERTIFICATE': return 'Chứng chỉ / Bằng cấp';
      default: return 'Tài liệu khác';
    }
  };

  const StatCard = ({ title, value, icon: Icon, colorClass }) => (
    <div className="bg-zinc-900/50 border border-zinc-800 rounded-2xl p-5 flex items-center justify-between hover:bg-zinc-900 transition-colors">
      <div>
        <p className="text-zinc-500 text-xs font-bold uppercase tracking-wider mb-1">{title}</p>
        <h3 className="text-3xl font-black text-white">{value}</h3>
      </div>
      <div className={`w-12 h-12 rounded-full flex items-center justify-center ${colorClass}`}>
        <Icon size={24} />
      </div>
    </div>
  );

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-[#050506] p-6 text-white md:p-8">
      <header className="flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
        <div className="flex items-center gap-4">
          <button onClick={() => navigate('/admin/staff')} className="w-10 h-10 flex items-center justify-center bg-zinc-900 border border-zinc-800 hover:bg-zinc-800 rounded-full transition-colors">
            <ArrowLeft className="w-5 h-5 text-zinc-400" />
          </button>
          <div>
            <h1 className="text-2xl font-black uppercase tracking-wider text-white">Hồ sơ <span className="text-brand-orange">Tài liệu</span></h1>
            <p className="mt-1 text-sm text-zinc-500">Quản lý hợp đồng, CMND/CCCD và các giấy tờ liên quan của nhân viên.</p>
          </div>
        </div>
        <button onClick={() => setIsModalOpen(true)} className="rounded-xl bg-brand-orange px-5 py-2.5 text-sm font-bold text-zinc-950 hover:bg-orange-600 transition-colors shadow-lg shadow-brand-orange/20 flex items-center gap-2">
          <UploadCloud size={18} />
          <span>Tải Lên Tài Liệu</span>
        </button>
      </header>

      {/* Dashboard Statistics */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard title="Tổng tài liệu" value={stats.total} icon={FileText} colorClass="bg-blue-500/10 text-blue-500 border border-blue-500/20" />
        <StatCard title="Hợp đồng" value={stats.contracts} icon={FileCheck} colorClass="bg-emerald-500/10 text-emerald-500 border border-emerald-500/20" />
        <StatCard title="Định danh" value={stats.ids} icon={FileBadge} colorClass="bg-purple-500/10 text-purple-500 border border-purple-500/20" />
      </div>

      <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!documents.length} emptyMessage="Chưa có tài liệu nào">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {documents.map(doc => (
            <div key={doc.id} className="rounded-2xl border border-zinc-800 bg-zinc-900/50 hover:bg-zinc-900 transition-colors p-5 flex flex-col justify-between group">
              <div>
                <div className="flex items-start gap-4 mb-4">
                  <div className="w-12 h-12 rounded-xl bg-zinc-950 border border-zinc-800 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                    {getDocIcon(doc.documentType)}
                  </div>
                  <div className="min-w-0 flex-1">
                    <h3 className="font-bold text-zinc-100 truncate text-sm" title={doc.documentName}>{doc.documentName || doc.originalFileName}</h3>
                    <span className="inline-block px-2 py-0.5 rounded-md bg-zinc-800 text-[10px] font-bold text-zinc-400 mt-1 uppercase tracking-wider">
                      {getDocTypeLabel(doc.documentType)}
                    </span>
                  </div>
                </div>
                
                <div className="bg-zinc-950 rounded-xl p-3 text-[11px] text-zinc-400 space-y-2 mb-4 border border-zinc-800/50">
                  <div className="flex justify-between">
                    <span>Ngày cấp:</span>
                    <span className="font-mono text-zinc-300">{doc.issuedDate ? new Date(doc.issuedDate).toLocaleDateString('vi-VN') : '—'}</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Hết hạn:</span>
                    <span className="font-mono text-zinc-300">{doc.expiredDate ? new Date(doc.expiredDate).toLocaleDateString('vi-VN') : 'Vô thời hạn'}</span>
                  </div>
                  <div className="flex justify-between border-t border-zinc-800/50 pt-2 mt-2">
                    <span>Kích thước:</span>
                    <span className="font-mono text-zinc-300">{(doc.fileSize / 1024).toFixed(1)} KB</span>
                  </div>
                </div>
              </div>
              <div className="flex justify-between items-center gap-2 pt-2">
                <button onClick={() => handleDelete(doc.id)} className="p-2 rounded-lg text-zinc-500 hover:text-red-400 hover:bg-red-500/10 transition-colors" title="Xóa tài liệu">
                  <Trash size={16} />
                </button>
                <button onClick={() => handleDownload(doc.id, doc.originalFileName)} className="flex-1 flex items-center justify-center gap-2 px-3 py-2 rounded-lg bg-zinc-800 hover:bg-zinc-700 text-xs font-bold text-zinc-300 transition-colors">
                  <Download size={14} /> <span>Tải xuống</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      </AsyncState>

      {/* Modal Upload */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4 animate-fade-in">
          <form onSubmit={handleUpload} className="w-full max-w-lg rounded-2xl border border-zinc-800 bg-zinc-950 p-6 space-y-5 shadow-2xl">
            <div className="flex items-center justify-between border-b border-zinc-800 pb-4">
              <h2 className="text-xl font-black uppercase tracking-wider text-white flex items-center gap-2">
                <UploadCloud className="text-brand-orange" size={24} />
                Tải lên hồ sơ nhân sự
              </h2>
              <button type="button" onClick={() => setIsModalOpen(false)} className="text-zinc-500 hover:text-white transition-colors">
                X
              </button>
            </div>

            <div className="bg-sky-500/10 border border-sky-500/20 p-3 rounded-xl flex gap-3 text-sm text-sky-400">
               <ShieldAlert size={18} className="shrink-0 mt-0.5" />
               <p>Hỗ trợ định dạng PDF, JPG, PNG. Kích thước tối đa mỗi file là 5MB. Vui lòng đảm bảo tài liệu rõ nét.</p>
            </div>
            
            <div className="space-y-4">
              <div className="space-y-1.5 relative">
                <label className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">Phân loại tài liệu <span className="text-brand-orange">*</span></label>
                <select 
                  value={formData.documentType} 
                  onChange={e => setFormData({ ...formData, documentType: e.target.value })} 
                  required 
                  className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm focus:border-brand-orange outline-none transition-colors appearance-none"
                >
                  <option value="CONTRACT">Hợp đồng lao động</option>
                  <option value="ID_CARD">CMND / CCCD</option>
                  <option value="CERTIFICATE">Chứng chỉ / Bằng cấp</option>
                  <option value="OTHER">Tài liệu khác</option>
                </select>
              </div>
              
              <div className="space-y-1.5">
                <label className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">Tên hiển thị (Tùy chọn)</label>
                <input 
                  value={formData.documentName} 
                  onChange={e => setFormData({ ...formData, documentName: e.target.value })} 
                  placeholder="VD: Hợp đồng thử việc 01/2024" 
                  className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm focus:border-brand-orange outline-none transition-colors"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">Ngày cấp</label>
                  <input 
                    type="date" 
                    value={formData.issuedDate} 
                    onChange={e => setFormData({ ...formData, issuedDate: e.target.value })} 
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm focus:border-brand-orange outline-none transition-colors font-mono"
                  />
                </div>
                <div className="space-y-1.5">
                  <label className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">Hết hạn</label>
                  <input 
                    type="date" 
                    value={formData.expiredDate} 
                    onChange={e => setFormData({ ...formData, expiredDate: e.target.value })} 
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm focus:border-brand-orange outline-none transition-colors font-mono"
                  />
                </div>
              </div>
              
              <div className="space-y-1.5">
                <label className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">Chọn tệp tin đính kèm <span className="text-brand-orange">*</span></label>
                <div className="relative overflow-hidden w-full bg-zinc-900 border-2 border-dashed border-zinc-700 hover:border-brand-orange/50 transition-colors rounded-xl flex flex-col items-center justify-center p-6 cursor-pointer">
                  <input 
                    type="file" 
                    onChange={e => setFormData({ ...formData, file: e.target.files[0] })} 
                    required 
                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer" 
                    accept=".pdf,.jpg,.jpeg,.png"
                  />
                  <div className="flex flex-col items-center justify-center gap-2 pointer-events-none">
                    {formData.file ? (
                      <>
                        <div className="w-12 h-12 rounded-full bg-emerald-500/20 flex items-center justify-center text-emerald-500">
                          <FileCheck size={24} />
                        </div>
                        <p className="text-sm font-bold text-emerald-400 truncate max-w-[250px]">{formData.file.name}</p>
                        <p className="text-xs text-zinc-500">{(formData.file.size / 1024 / 1024).toFixed(2)} MB</p>
                      </>
                    ) : (
                      <>
                        <div className="w-12 h-12 rounded-full bg-zinc-800 flex items-center justify-center text-zinc-400">
                          <UploadCloud size={24} />
                        </div>
                        <p className="text-sm font-bold text-zinc-300">Click để chọn hoặc kéo thả file vào đây</p>
                        <p className="text-xs text-zinc-500">PDF, JPG, PNG (Tối đa 5MB)</p>
                      </>
                    )}
                  </div>
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-6 border-t border-zinc-800">
              <button type="button" onClick={() => setIsModalOpen(false)} className="rounded-xl border border-zinc-700 bg-zinc-900 px-5 py-2.5 text-sm font-bold text-zinc-300 hover:bg-zinc-800 transition-colors">
                Hủy
              </button>
              <button type="submit" className="rounded-xl bg-brand-orange px-5 py-2.5 text-sm font-bold text-zinc-950 hover:bg-orange-600 transition-colors shadow-lg shadow-brand-orange/20 flex items-center gap-2">
                <UploadCloud size={16} /> Lưu tài liệu
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}
