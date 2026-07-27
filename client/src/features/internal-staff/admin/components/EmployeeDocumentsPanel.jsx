import { useCallback, useEffect, useRef, useState } from 'react';
import {
  deleteEmployeeDocument,
  downloadEmployeeDocument,
  getEmployeeDocuments,
  uploadEmployeeDocument
} from '../services/userAdminService';

const DOCUMENT_TYPES = [
  ['IDENTITY_CARD', 'Căn cước công dân'],
  ['PASSPORT', 'Hộ chiếu'],
  ['LABOR_CONTRACT', 'Hợp đồng lao động'],
  ['CERTIFICATE', 'Chứng chỉ'],
  ['DIPLOMA', 'Bằng cấp'],
  ['OTHER', 'Khác']
];

const initialForm = {
  documentType: 'LABOR_CONTRACT',
  documentName: '',
  issuedDate: '',
  expiredDate: '',
  file: null
};

export default function EmployeeDocumentsPanel({ employee, onClose }) {
  const [documents, setDocuments] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [includeHistory, setIncludeHistory] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const fileInputRef = useRef(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getEmployeeDocuments(employee.accountId, includeHistory);
      setDocuments(Array.isArray(result) ? result : []);
      setError('');
    } catch (reason) {
      setError(reason?.message || 'Không thể tải hồ sơ nhân viên.');
    } finally {
      setLoading(false);
    }
  }, [employee.accountId, includeHistory]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  const change = (name) => (event) => {
    const value = name === 'file' ? event.target.files?.[0] || null : event.target.value;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const submit = async (event) => {
    event.preventDefault();
    if (!form.file) {
      setError('Vui lòng chọn tệp hồ sơ.');
      return;
    }
    if (form.expiredDate && form.issuedDate && form.expiredDate < form.issuedDate) {
      setError('Ngày hết hạn không thể trước ngày cấp.');
      return;
    }
    setSubmitting(true);
    try {
      await uploadEmployeeDocument(employee.accountId, form);
      setForm(initialForm);
      if (fileInputRef.current) fileInputRef.current.value = '';
      await load();
    } catch (reason) {
      setError(reason?.message || 'Không thể tải hồ sơ lên.');
    } finally {
      setSubmitting(false);
    }
  };

  const download = async (document) => {
    try {
      const blob = await downloadEmployeeDocument(employee.accountId, document.id);
      const url = URL.createObjectURL(blob);
      const anchor = window.document.createElement('a');
      anchor.href = url;
      anchor.download = document.documentName;
      window.document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
    } catch (reason) {
      setError(reason?.message || 'Không thể tải hồ sơ xuống.');
    }
  };

  const remove = async (document) => {
    if (!window.confirm(`Xóa hồ sơ "${document.documentName}"?`)) return;
    try {
      await deleteEmployeeDocument(employee.accountId, document.id);
      await load();
    } catch (reason) {
      setError(reason?.message || 'Không thể xóa hồ sơ.');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4"
      role="dialog" aria-modal="true" aria-labelledby="employee-documents-title">
      <div className="max-h-[92vh] w-full max-w-4xl overflow-y-auto rounded-2xl border border-zinc-700 bg-zinc-900 p-5 shadow-2xl">
        <header className="flex items-start justify-between gap-4 border-b border-zinc-800 pb-4">
          <div>
            <h2 id="employee-documents-title" className="text-lg font-black uppercase">
              Hồ sơ nhân viên
            </h2>
            <p className="mt-1 text-sm text-zinc-400">
              {employee.fullName} · {employee.employeeCode}
            </p>
          </div>
          <button type="button" onClick={onClose}
            className="rounded-lg px-3 py-1 text-xl text-zinc-400 hover:bg-zinc-800 hover:text-white"
            aria-label="Đóng">×</button>
        </header>

        <form onSubmit={submit} className="mt-5 grid gap-3 rounded-xl bg-zinc-950 p-4 md:grid-cols-2">
          <label className="text-xs text-zinc-400">
            Loại hồ sơ
            <select value={form.documentType} onChange={change('documentType')}
              className="mt-1 block w-full rounded-lg border border-zinc-800 bg-zinc-900 px-3 py-2 text-sm text-white">
              {DOCUMENT_TYPES.map(([value, label]) =>
                <option value={value} key={value}>{label}</option>)}
            </select>
          </label>
          <label className="text-xs text-zinc-400">
            Tên hiển thị
            <input value={form.documentName} onChange={change('documentName')}
              maxLength={255} placeholder="Mặc định là tên tệp"
              className="mt-1 block w-full rounded-lg border border-zinc-800 bg-zinc-900 px-3 py-2 text-sm text-white" />
          </label>
          <label className="text-xs text-zinc-400">
            Ngày cấp
            <input type="date" value={form.issuedDate} onChange={change('issuedDate')}
              className="mt-1 block w-full rounded-lg border border-zinc-800 bg-zinc-900 px-3 py-2 text-sm text-white" />
          </label>
          <label className="text-xs text-zinc-400">
            Ngày hết hạn
            <input type="date" value={form.expiredDate} onChange={change('expiredDate')}
              className="mt-1 block w-full rounded-lg border border-zinc-800 bg-zinc-900 px-3 py-2 text-sm text-white" />
          </label>
          <label className="text-xs text-zinc-400 md:col-span-2">
            Tệp hồ sơ
            <input ref={fileInputRef} required type="file"
              accept=".pdf,.docx,.jpg,.jpeg,.png,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,image/jpeg,image/png"
              onChange={change('file')}
              className="mt-1 block w-full rounded-lg border border-zinc-800 bg-zinc-900 px-3 py-2 text-sm text-white" />
          </label>
          <p className="text-xs text-zinc-500 md:col-span-2">
            PDF, DOCX, JPEG hoặc PNG; tối đa 10 MB. Nội dung tệp được kiểm tra trước khi lưu.
          </p>
          <button disabled={submitting}
            className="rounded-lg bg-orange-500 px-4 py-2 font-bold text-zinc-950 disabled:opacity-50 md:col-span-2">
            {submitting ? 'Đang tải lên…' : 'Tải hồ sơ lên'}
          </button>
        </form>

        <div className="mt-5 flex items-center justify-between">
          <h3 className="text-sm font-black uppercase">Tệp đã lưu</h3>
          <label className="flex items-center gap-2 text-xs text-zinc-400">
            <input type="checkbox" checked={includeHistory}
              onChange={(event) => setIncludeHistory(event.target.checked)} />
            Hiện lịch sử đã xóa
          </label>
        </div>
        {error && <p className="mt-3 rounded-lg bg-red-950/50 p-3 text-sm text-red-300">{error}</p>}
        {loading ? (
          <p className="py-8 text-center text-sm text-zinc-500">Đang tải…</p>
        ) : documents.length === 0 ? (
          <p className="py-8 text-center text-sm text-zinc-500">Chưa có hồ sơ nào.</p>
        ) : (
          <div className="mt-3 divide-y divide-zinc-800 overflow-hidden rounded-xl border border-zinc-800">
            {documents.map((document) => (
              <article key={document.id}
                className={`flex flex-wrap items-center justify-between gap-3 p-4 ${document.deleted ? 'opacity-50' : ''}`}>
                <div>
                  <p className="font-semibold">{document.documentName}</p>
                  <p className="mt-1 text-xs text-zinc-500">
                    {document.documentType} · {(document.fileSize / 1024).toFixed(1)} KB
                    {document.expiredDate ? ` · Hết hạn ${document.expiredDate}` : ''}
                    {document.deleted ? ' · Đã xóa' : ''}
                  </p>
                </div>
                {!document.deleted && <div className="space-x-3 text-sm">
                  <button type="button" onClick={() => download(document)}
                    className="text-sky-400">Tải xuống</button>
                  <button type="button" onClick={() => remove(document)}
                    className="text-red-400">Xóa</button>
                </div>}
              </article>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
