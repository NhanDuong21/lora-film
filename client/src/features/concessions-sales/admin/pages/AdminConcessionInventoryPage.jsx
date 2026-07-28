import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Archive,
  Coffee,
  Image as ImageIcon,
  Pencil,
  Plus,
  RefreshCcw,
  RotateCcw,
  Search,
  TriangleAlert
} from 'lucide-react';
import CustomerNoticeModal from '@/components/common/CustomerNoticeModal';
import apiClient from '@/services/apiClient';
import ConcessionArchiveModal from '../components/ConcessionArchiveModal';
import ConcessionCatalogFormModal from '../components/ConcessionCatalogFormModal';

const typeFilters = [
  { value: 'ALL', label: 'Tất cả loại' },
  { value: 'FOOD', label: 'Bắp & đồ ăn' },
  { value: 'DRINK', label: 'Nước uống' },
  { value: 'COMBO', label: 'Combo' }
];

const statusFilters = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'SELLING', label: 'Đang bán' },
  { value: 'PAUSED', label: 'Tạm dừng' },
  { value: 'ARCHIVED', label: 'Đã lưu trữ' }
];

const typeLabels = {
  FOOD: 'Bắp & đồ ăn',
  DRINK: 'Nước uống',
  COMBO: 'Combo'
};

const getStatus = item => {
  if (item.deleted) return 'ARCHIVED';
  if (item.active && item.sellable && !item.disabled) return 'SELLING';
  return 'PAUSED';
};

const statusPresentation = {
  SELLING: {
    label: 'Đang bán',
    className: 'border-emerald-500/25 bg-emerald-500/10 text-emerald-300',
    dotClassName: 'bg-emerald-400'
  },
  PAUSED: {
    label: 'Tạm dừng',
    className: 'border-amber-500/25 bg-amber-500/10 text-amber-300',
    dotClassName: 'bg-amber-400'
  },
  ARCHIVED: {
    label: 'Đã lưu trữ',
    className: 'border-zinc-600 bg-zinc-800 text-zinc-400',
    dotClassName: 'bg-zinc-500'
  }
};

const typeClassNames = {
  FOOD: 'border-amber-500/30 bg-amber-500/10 text-amber-300',
  DRINK: 'border-sky-500/30 bg-sky-500/10 text-sky-300',
  COMBO: 'border-purple-500/30 bg-purple-500/10 text-purple-300'
};

const resolveImageUrl = imageUrl => {
  if (!imageUrl) return '';
  if (/^https?:\/\//i.test(imageUrl) || imageUrl.startsWith('/')) return imageUrl;
  return '';
};

const getCatalogErrorMessage = (error, fallback) => {
  const errorCode = error?.errorCode || error?.code || error?.response?.data?.errorCode;

  const messages = {
    CONCESSION_CODE_EXISTS: 'Mã sản phẩm này đã tồn tại. Vui lòng sử dụng một mã khác.',
    CONCESSION_ARCHIVED: 'Sản phẩm đã được lưu trữ. Hãy khôi phục sản phẩm trước khi chỉnh sửa.',
    CONCESSION_CODE_IMMUTABLE: 'Mã sản phẩm không thể thay đổi sau khi tạo.',
    INVALID_CONCESSION_IMAGE: 'Ảnh không hợp lệ. Chỉ chấp nhận JPG, PNG hoặc WEBP, tối đa 5 MB.',
    CONCESSION_IMAGE_STORAGE_UNAVAILABLE: 'Kho lưu trữ ảnh chưa được cấu hình. Hãy nhập đường dẫn ảnh HTTPS hoặc cấu hình Cloudinary rồi thử lại.',
    VALIDATION_FAILED: 'Thông tin sản phẩm chưa hợp lệ. Vui lòng kiểm tra lại các trường bắt buộc.',
    MALFORMED_REQUEST_BODY: 'Dữ liệu sản phẩm không đúng định dạng. Vui lòng thử lại.',
    INTERNAL_SERVER_ERROR: 'Hệ thống chưa thể xử lý yêu cầu lúc này. Vui lòng thử lại sau.'
  };

  if (messages[errorCode]) return messages[errorCode];
  if (error?.code === 'ERR_NETWORK') {
    return 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối và thử lại.';
  }
  return fallback;
};

const buildCatalogFormData = ({ payload, image }) => {
  const formData = new FormData();
  formData.append(
    'item',
    new Blob([JSON.stringify(payload)], { type: 'application/json' })
  );
  if (image) formData.append('image', image);
  return formData;
};

export default function AdminConcessionInventoryPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedType, setSelectedType] = useState('ALL');
  const [selectedStatus, setSelectedStatus] = useState('ALL');
  const [editingItem, setEditingItem] = useState(undefined);
  const [formOpen, setFormOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [archiveTarget, setArchiveTarget] = useState(null);
  const [archiving, setArchiving] = useState(false);
  const [restoringId, setRestoringId] = useState(null);
  const [notice, setNotice] = useState(null);

  const fetchItems = useCallback(async ({ background = false } = {}) => {
    if (!background) setLoading(true);
    setLoadError('');
    try {
      const response = await apiClient.get('/api/admin/foods');
      setItems(response?.data?.data || []);
    } catch (error) {
      setLoadError(getCatalogErrorMessage(
        error,
        'Không thể tải danh mục bắp nước. Vui lòng thử lại.'
      ));
    } finally {
      if (!background) setLoading(false);
    }
  }, []);

  useEffect(() => {
    // Remote catalog state is intentionally loaded once when the page is mounted.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchItems();
  }, [fetchItems]);

  const counts = useMemo(() => items.reduce((result, item) => {
    const status = getStatus(item);
    result[status] += 1;
    return result;
  }, { SELLING: 0, PAUSED: 0, ARCHIVED: 0 }), [items]);

  const filteredItems = useMemo(() => {
    const normalizedSearch = searchQuery.trim().toLocaleLowerCase('vi-VN');

    return items.filter(item => {
      const matchesSearch = !normalizedSearch
        || item.name?.toLocaleLowerCase('vi-VN').includes(normalizedSearch)
        || item.code?.toLocaleLowerCase('vi-VN').includes(normalizedSearch);
      const matchesType = selectedType === 'ALL' || item.type === selectedType;
      const matchesStatus = selectedStatus === 'ALL' || getStatus(item) === selectedStatus;
      return matchesSearch && matchesType && matchesStatus;
    });
  }, [items, searchQuery, selectedStatus, selectedType]);

  const openCreateModal = () => {
    setEditingItem(undefined);
    setFormOpen(true);
  };

  const openEditModal = item => {
    setEditingItem(item);
    setFormOpen(true);
  };

  const closeFormModal = () => {
    if (saving) return;
    setFormOpen(false);
    setEditingItem(undefined);
  };

  const handleSave = async formValue => {
    setSaving(true);
    try {
      const formData = buildCatalogFormData(formValue);
      const config = { headers: { 'Content-Type': 'multipart/form-data' } };

      if (editingItem) {
        await apiClient.put(`/api/admin/foods/${editingItem.id}`, formData, config);
      } else {
        await apiClient.post('/api/admin/foods', formData, config);
      }

      setFormOpen(false);
      setEditingItem(undefined);
      await fetchItems({ background: true });
      setNotice({
        variant: 'success',
        title: editingItem ? 'Đã cập nhật sản phẩm' : 'Đã thêm sản phẩm',
        message: editingItem
          ? 'Thông tin sản phẩm và trạng thái bán đã được cập nhật.'
          : 'Sản phẩm mới đã được thêm vào danh mục bắp nước.'
      });
    } catch (error) {
      setNotice({
        variant: 'error',
        title: 'Không thể lưu sản phẩm',
        message: getCatalogErrorMessage(
          error,
          'Hệ thống chưa thể lưu sản phẩm. Vui lòng kiểm tra thông tin và thử lại.'
        )
      });
    } finally {
      setSaving(false);
    }
  };

  const handleArchive = async () => {
    if (!archiveTarget) return;
    setArchiving(true);
    try {
      await apiClient.delete(`/api/admin/foods/${archiveTarget.id}`);
      setArchiveTarget(null);
      await fetchItems({ background: true });
      setNotice({
        variant: 'success',
        title: 'Đã lưu trữ sản phẩm',
        message: 'Sản phẩm đã ngừng bán và được chuyển vào danh sách đã lưu trữ.'
      });
    } catch (error) {
      setNotice({
        variant: 'error',
        title: 'Không thể lưu trữ sản phẩm',
        message: getCatalogErrorMessage(
          error,
          'Hệ thống chưa thể lưu trữ sản phẩm. Vui lòng thử lại.'
        )
      });
    } finally {
      setArchiving(false);
    }
  };

  const handleRestore = async item => {
    setRestoringId(item.id);
    try {
      await apiClient.patch(`/api/admin/foods/${item.id}/restore`);
      await fetchItems({ background: true });
      setNotice({
        variant: 'success',
        title: 'Đã khôi phục sản phẩm',
        message: 'Sản phẩm đã được khôi phục ở trạng thái tạm dừng. Hãy kiểm tra rồi bật bán khi sẵn sàng.'
      });
    } catch (error) {
      setNotice({
        variant: 'error',
        title: 'Không thể khôi phục sản phẩm',
        message: getCatalogErrorMessage(
          error,
          'Hệ thống chưa thể khôi phục sản phẩm. Vui lòng thử lại.'
        )
      });
    } finally {
      setRestoringId(null);
    }
  };

  return (
    <div className="flex min-h-[400px] flex-1 flex-col space-y-6 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <header className="flex flex-col items-start justify-between gap-4 border-b border-zinc-800 pb-6 sm:flex-row sm:items-center">
        <div>
          <h1 className="text-xl font-black uppercase tracking-wider text-white md:text-2xl">
            Danh mục bắp nước
          </h1>
          <p className="mt-1 text-xs text-zinc-400">
            Quản lý sản phẩm bán kèm, giá bán và trạng thái hiển thị cho khách hàng.
          </p>
        </div>
        <button
          type="button"
          onClick={openCreateModal}
          className="flex items-center gap-2 rounded-xl bg-gradient-to-r from-orange-500 to-amber-500 px-5 py-3 text-xs font-black uppercase tracking-wider text-white shadow-lg shadow-orange-500/20 transition-all hover:from-orange-600 hover:to-amber-600 active:scale-95"
        >
          <Plus className="h-4 w-4" />
          Thêm sản phẩm
        </button>
      </header>

      <section className="grid gap-4 sm:grid-cols-3" aria-label="Tổng quan trạng thái sản phẩm">
        {[
          { label: 'Đang bán', value: counts.SELLING, color: 'text-emerald-400' },
          { label: 'Tạm dừng', value: counts.PAUSED, color: 'text-amber-400' },
          { label: 'Đã lưu trữ', value: counts.ARCHIVED, color: 'text-zinc-400' }
        ].map(card => (
          <div key={card.label} className="rounded-2xl border border-zinc-800 bg-zinc-900/50 px-5 py-4">
            <p className="text-[10px] font-black uppercase tracking-wider text-zinc-500">{card.label}</p>
            <p className={`mt-1 text-2xl font-black ${card.color}`}>{card.value}</p>
          </div>
        ))}
      </section>

      <section className="space-y-4 rounded-2xl border border-zinc-800/60 bg-zinc-900/45 p-4" aria-label="Bộ lọc danh mục">
        <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-center">
          <div className="relative w-full lg:max-w-md">
            <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-500" />
            <input
              type="search"
              placeholder="Tìm theo tên hoặc mã sản phẩm..."
              value={searchQuery}
              onChange={event => setSearchQuery(event.target.value)}
              className="w-full rounded-xl border border-zinc-800 bg-zinc-950 py-3 pl-10 pr-4 text-sm text-white outline-none transition-colors focus:border-orange-500"
            />
          </div>
          <button
            type="button"
            onClick={() => fetchItems()}
            disabled={loading}
            className="inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-4 py-3 text-xs font-bold text-zinc-300 transition-colors hover:bg-zinc-800 hover:text-white disabled:opacity-50"
          >
            <RefreshCcw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </button>
        </div>

        <div className="grid gap-3 md:grid-cols-2">
          <label className="space-y-1.5">
            <span className="text-[10px] font-black uppercase tracking-wider text-zinc-500">Phân loại</span>
            <select
              value={selectedType}
              onChange={event => setSelectedType(event.target.value)}
              className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-2.5 text-sm text-white outline-none focus:border-orange-500"
            >
              {typeFilters.map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </label>
          <label className="space-y-1.5">
            <span className="text-[10px] font-black uppercase tracking-wider text-zinc-500">Trạng thái vận hành</span>
            <select
              value={selectedStatus}
              onChange={event => setSelectedStatus(event.target.value)}
              className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-2.5 text-sm text-white outline-none focus:border-orange-500"
            >
              {statusFilters.map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </label>
        </div>
      </section>

      {loading ? (
        <div className="flex flex-col items-center justify-center gap-3 py-20">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-orange-500 border-t-transparent" />
          <p className="text-xs text-zinc-500">Đang tải danh mục bắp nước...</p>
        </div>
      ) : loadError ? (
        <div role="alert" className="flex flex-col items-center rounded-3xl border border-red-500/25 bg-red-500/5 px-6 py-12 text-center">
          <TriangleAlert className="h-10 w-10 text-red-400" />
          <h2 className="mt-4 text-base font-black text-white">Không thể tải danh mục</h2>
          <p className="mt-2 max-w-lg text-sm leading-6 text-zinc-400">{loadError}</p>
          <button
            type="button"
            onClick={() => fetchItems()}
            className="mt-5 rounded-xl bg-orange-500 px-5 py-3 text-xs font-black uppercase text-white hover:bg-orange-600"
          >
            Thử lại
          </button>
        </div>
      ) : filteredItems.length === 0 ? (
        <div className="rounded-3xl border border-dashed border-zinc-800 bg-zinc-900/20 py-16 text-center">
          <Coffee className="mx-auto h-10 w-10 text-zinc-600" />
          <p className="mt-3 text-sm font-bold text-zinc-300">Không tìm thấy sản phẩm phù hợp</p>
          <p className="mt-1 text-xs text-zinc-500">Hãy thay đổi từ khóa hoặc bộ lọc trạng thái.</p>
        </div>
      ) : (
        <section className="overflow-hidden rounded-3xl border border-zinc-800/70 bg-zinc-900/55 shadow-xl" aria-label="Danh sách sản phẩm">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[980px] border-collapse text-left">
              <thead>
                <tr className="border-b border-zinc-800 bg-zinc-900/60">
                  <th className="px-6 py-4 text-xs font-black uppercase tracking-wider text-zinc-400">Sản phẩm</th>
                  <th className="px-6 py-4 text-xs font-black uppercase tracking-wider text-zinc-400">Mã</th>
                  <th className="px-6 py-4 text-xs font-black uppercase tracking-wider text-zinc-400">Phân loại</th>
                  <th className="px-6 py-4 text-right text-xs font-black uppercase tracking-wider text-zinc-400">Giá bán</th>
                  <th className="px-6 py-4 text-center text-xs font-black uppercase tracking-wider text-zinc-400">Trạng thái</th>
                  <th className="px-6 py-4 text-right text-xs font-black uppercase tracking-wider text-zinc-400">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/70">
                {filteredItems.map(item => {
                  const status = getStatus(item);
                  const presentation = statusPresentation[status];
                  const imageUrl = resolveImageUrl(item.imageUrl);

                  return (
                    <tr key={item.id} className={`transition-colors hover:bg-zinc-900/70 ${status === 'ARCHIVED' ? 'opacity-65' : ''}`}>
                      <td className="px-6 py-4">
                        <div className="flex min-w-0 items-center gap-3">
                          <div className="flex h-14 w-14 shrink-0 items-center justify-center overflow-hidden rounded-xl border border-zinc-800 bg-zinc-950">
                            {imageUrl ? (
                              <img
                                src={imageUrl}
                                alt={item.name}
                                onError={event => {
                                  event.currentTarget.hidden = true;
                                  event.currentTarget.nextElementSibling?.classList.remove('hidden');
                                }}
                                className="h-full w-full object-cover"
                              />
                            ) : null}
                            <ImageIcon className={`h-5 w-5 text-zinc-600 ${imageUrl ? 'hidden' : ''}`} />
                          </div>
                          <div className="min-w-0">
                            <p className="max-w-xs truncate text-sm font-bold text-white" title={item.name}>{item.name}</p>
                            <p className="mt-1 text-[11px] text-zinc-500">
                              {imageUrl ? 'Có ảnh sản phẩm' : 'Chưa có ảnh hợp lệ'}
                            </p>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 font-mono text-xs font-bold text-orange-400">{item.code}</td>
                      <td className="px-6 py-4">
                        <span className={`rounded-lg border px-2.5 py-1 text-[10px] font-black uppercase tracking-wider ${typeClassNames[item.type] || typeClassNames.FOOD}`}>
                          {typeLabels[item.type] || item.type}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-right text-sm font-black text-white">
                        {Number(item.price || 0).toLocaleString('vi-VN')}đ
                      </td>
                      <td className="px-6 py-4 text-center">
                        <span className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-[10px] font-black uppercase tracking-wider ${presentation.className}`}>
                          <span className={`h-1.5 w-1.5 rounded-full ${presentation.dotClassName}`} />
                          {presentation.label}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center justify-end gap-2">
                          {status === 'ARCHIVED' ? (
                            <button
                              type="button"
                              disabled={restoringId === item.id}
                              onClick={() => handleRestore(item)}
                              aria-label={`Khôi phục ${item.name}`}
                              title="Khôi phục ở trạng thái tạm dừng"
                              className="inline-flex items-center gap-2 rounded-xl border border-emerald-500/25 bg-emerald-500/5 px-3 py-2 text-xs font-bold text-emerald-300 transition-colors hover:bg-emerald-500/10 disabled:cursor-wait disabled:opacity-50"
                            >
                              <RotateCcw className={`h-4 w-4 ${restoringId === item.id ? 'animate-spin' : ''}`} />
                              Khôi phục
                            </button>
                          ) : (
                            <>
                              <button
                                type="button"
                                onClick={() => openEditModal(item)}
                                aria-label={`Chỉnh sửa ${item.name}`}
                                title="Chỉnh sửa sản phẩm"
                                className="rounded-xl border border-zinc-700 bg-zinc-950 p-2.5 text-zinc-400 transition-colors hover:border-orange-500/40 hover:text-orange-400"
                              >
                                <Pencil className="h-4 w-4" />
                              </button>
                              <button
                                type="button"
                                onClick={() => setArchiveTarget(item)}
                                aria-label={`Lưu trữ ${item.name}`}
                                title="Lưu trữ sản phẩm"
                                className="rounded-xl border border-zinc-700 bg-zinc-950 p-2.5 text-zinc-400 transition-colors hover:border-red-500/40 hover:text-red-400"
                              >
                                <Archive className="h-4 w-4" />
                              </button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <footer className="border-t border-zinc-800 px-6 py-3 text-xs text-zinc-500">
            Hiển thị {filteredItems.length} trong tổng số {items.length} sản phẩm.
          </footer>
        </section>
      )}

      {formOpen && (
        <ConcessionCatalogFormModal
          key={editingItem?.id || 'new-concession'}
          item={editingItem}
          pending={saving}
          onClose={closeFormModal}
          onSubmit={handleSave}
        />
      )}

      {archiveTarget && (
        <ConcessionArchiveModal
          item={archiveTarget}
          pending={archiving}
          onClose={() => {
            if (!archiving) setArchiveTarget(null);
          }}
          onConfirm={handleArchive}
        />
      )}

      {notice && (
        <CustomerNoticeModal
          {...notice}
          actionLabel="Đóng"
          onClose={() => setNotice(null)}
        />
      )}
    </div>
  );
}
