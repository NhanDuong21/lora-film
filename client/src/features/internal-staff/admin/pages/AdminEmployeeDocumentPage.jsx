import { useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getEmployeeDocuments, uploadEmployeeDocument, downloadEmployeeDocument, deleteEmployeeDocument } from '../services/userAdminService';
import { AsyncState, Input, Select } from '@/components/common/ui/uiKit';
import { ArrowLeft, File, Download, Trash } from 'lucide-react';

export default function AdminEmployeeDocumentPage() {
  const { accountId } = useParams();
  const navigate = useNavigate();
  const [documents, setDocuments] = useState([]);
  const [state, setState] = useState({ loading: true, error: '' });
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({ file: null, documentType: 'CONTRACT', documentName: '', issuedDate: '', expiredDate: '' });

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const data = await getEmployeeDocuments(accountId, true);
      setDocuments(data || []);
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải tài liệu nhân viên.' });
    }
  }, [accountId]);

  useEffect(() => { load(); }, [load]);

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!formData.file) {
      alert('Vui lòng chọn file');
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
    if (!window.confirm('Bạn có chắc muốn xóa tài liệu này?')) return;
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

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button onClick={() => navigate('/admin/staff')} className="p-2 hover:bg-zinc-800 rounded-full transition-colors">
            <ArrowLeft className="w-5 h-5 text-zinc-400" />
          </button>
          <div>
            <h1 className="text-2xl font-black uppercase">Hồ sơ & Tài liệu</h1>
            <p className="mt-1 text-sm text-zinc-500">Quản lý hợp đồng, CMND/CCCD và các giấy tờ liên quan của nhân viên.</p>
          </div>
        </div>
        <button onClick={() => setIsModalOpen(true)} className="rounded-xl bg-brand-orange px-4 py-2 text-sm font-bold text-zinc-950 hover:bg-brand-orange/90">
          + Tải lên Tài liệu
        </button>
      </div>

      <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!documents.length} emptyMessage="Chưa có tài liệu nào">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {documents.map(doc => (
            <div key={doc.id} className="rounded-2xl border border-zinc-800 bg-zinc-900 p-5 flex flex-col justify-between">
              <div>
                <div className="flex items-start gap-3 mb-3">
                  <div className="w-10 h-10 rounded bg-zinc-800 flex items-center justify-center shrink-0">
                    <File className="w-5 h-5 text-brand-orange" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <h3 className="font-bold text-white truncate" title={doc.documentName}>{doc.documentName || doc.originalFileName}</h3>
                    <p className="text-xs text-zinc-500 font-mono">{doc.documentType}</p>
                  </div>
                </div>
                <div className="text-xs text-zinc-400 space-y-1 mb-4">
                  {doc.issuedDate && <p>Ngày cấp: {new Date(doc.issuedDate).toLocaleDateString('vi-VN')}</p>}
                  {doc.expiredDate && <p>Ngày hết hạn: {new Date(doc.expiredDate).toLocaleDateString('vi-VN')}</p>}
                  <p>Kích thước: {(doc.fileSize / 1024).toFixed(1)} KB</p>
                </div>
              </div>
              <div className="flex justify-end gap-2 border-t border-zinc-800 pt-3">
                <button onClick={() => handleDownload(doc.id, doc.originalFileName)} className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-zinc-800 hover:bg-zinc-700 text-xs font-bold text-zinc-300">
                  <Download className="w-3.5 h-3.5" /> Tải xuống
                </button>
                <button onClick={() => handleDelete(doc.id)} className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-red-500/10 hover:bg-red-500/20 text-xs font-bold text-red-400">
                  <Trash className="w-3.5 h-3.5" /> Xóa
                </button>
              </div>
            </div>
          ))}
        </div>
      </AsyncState>

      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <form onSubmit={handleUpload} className="w-full max-w-md rounded-2xl border border-zinc-800 bg-zinc-900 p-6 space-y-4">
            <h2 className="text-xl font-bold text-white">Tải lên Tài liệu</h2>
            
            <div>
              <label className="text-xs font-bold text-zinc-400">Loại tài liệu</label>
              <Select value={formData.documentType} onChange={e => setFormData({ ...formData, documentType: e.target.value })} required>
                <option value="CONTRACT">Hợp đồng lao động</option>
                <option value="ID_CARD">CMND / CCCD</option>
                <option value="CERTIFICATE">Chứng chỉ / Bằng cấp</option>
                <option value="OTHER">Khác</option>
              </Select>
            </div>
            
            <div>
              <label className="text-xs font-bold text-zinc-400">Tên hiển thị (Tùy chọn)</label>
              <Input value={formData.documentName} onChange={e => setFormData({ ...formData, documentName: e.target.value })} placeholder="VD: Hợp đồng thử việc" />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-xs font-bold text-zinc-400">Ngày cấp (Tùy chọn)</label>
                <Input type="date" value={formData.issuedDate} onChange={e => setFormData({ ...formData, issuedDate: e.target.value })} />
              </div>
              <div>
                <label className="text-xs font-bold text-zinc-400">Ngày hết hạn (Tùy chọn)</label>
                <Input type="date" value={formData.expiredDate} onChange={e => setFormData({ ...formData, expiredDate: e.target.value })} />
              </div>
            </div>
            
            <div>
              <label className="text-xs font-bold text-zinc-400">Chọn File</label>
              <input type="file" onChange={e => setFormData({ ...formData, file: e.target.files[0] })} required className="block w-full text-sm text-zinc-400 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-xs file:font-bold file:bg-zinc-800 file:text-brand-orange hover:file:bg-zinc-700 mt-1 cursor-pointer" />
            </div>

            <div className="flex justify-end gap-2 pt-4">
              <button type="button" onClick={() => setIsModalOpen(false)} className="rounded-xl border border-zinc-700 px-4 py-2 text-sm text-zinc-300 hover:bg-zinc-800">Hủy</button>
              <button type="submit" className="rounded-xl bg-brand-orange px-4 py-2 text-sm font-bold text-zinc-950 hover:bg-brand-orange/90">Tải lên</button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}
