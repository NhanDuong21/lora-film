import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '@/services/apiClient';
import AdminConcessionInventoryPage from './AdminConcessionInventoryPage';

vi.mock('@/services/apiClient', () => ({
  default: {
    delete: vi.fn(),
    get: vi.fn(),
    patch: vi.fn(),
    post: vi.fn(),
    put: vi.fn()
  }
}));

const sellingItem = {
  id: 1,
  code: 'POP_L',
  name: 'Bắp rang cỡ lớn',
  type: 'FOOD',
  price: 50000,
  imageUrl: 'https://cdn.example.com/popcorn.webp',
  active: true,
  sellable: true,
  deleted: false,
  disabled: false
};

describe('AdminConcessionInventoryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiClient.get.mockResolvedValue({ data: { data: [sellingItem] } });
    apiClient.delete.mockResolvedValue({ data: { success: true } });
    apiClient.patch.mockResolvedValue({ data: { success: true } });
    apiClient.post.mockResolvedValue({ data: { success: true } });
    apiClient.put.mockResolvedValue({ data: { success: true } });

    Object.defineProperty(URL, 'createObjectURL', {
      configurable: true,
      value: vi.fn(() => 'blob:preview')
    });
    Object.defineProperty(URL, 'revokeObjectURL', {
      configurable: true,
      value: vi.fn()
    });
  });

  it('archives a product through the branded confirmation modal without browser confirm', async () => {
    const browserConfirm = vi.spyOn(window, 'confirm').mockImplementation(() => true);
    render(<AdminConcessionInventoryPage />);

    expect(await screen.findByText('Bắp rang cỡ lớn')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Lưu trữ Bắp rang cỡ lớn' }));

    expect(screen.getByRole('alertdialog', { name: 'Lưu trữ sản phẩm' })).toBeInTheDocument();
    expect(screen.getByText(/Dữ liệu sản phẩm trong các đơn cũ vẫn được giữ nguyên/)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận lưu trữ' }));

    await waitFor(() => expect(apiClient.delete).toHaveBeenCalledWith('/api/admin/foods/1'));
    expect(browserConfirm).not.toHaveBeenCalled();
    expect(await screen.findByRole('alertdialog', { name: 'Đã lưu trữ sản phẩm' })).toBeInTheDocument();

    browserConfirm.mockRestore();
  });

  it('creates a product with the multipart item contract and selected image', async () => {
    apiClient.get.mockResolvedValue({ data: { data: [] } });
    render(<AdminConcessionInventoryPage />);

    await screen.findByText('Không tìm thấy sản phẩm phù hợp');
    fireEvent.click(screen.getByRole('button', { name: 'Thêm sản phẩm' }));
    const formDialog = screen.getByRole('dialog', { name: 'Thêm sản phẩm mới' });

    fireEvent.change(within(formDialog).getByLabelText(/Mã sản phẩm/), {
      target: { value: 'combo_family' }
    });
    fireEvent.change(within(formDialog).getByLabelText(/Tên sản phẩm/), {
      target: { value: 'Combo gia đình' }
    });
    fireEvent.change(within(formDialog).getByLabelText(/Giá bán/), {
      target: { value: '129000' }
    });
    fireEvent.change(within(formDialog).getByLabelText(/Phân loại/), {
      target: { value: 'COMBO' }
    });

    const image = new File(['image-data'], 'combo.webp', { type: 'image/webp' });
    const fileInput = formDialog.querySelector('input[type="file"]');
    fireEvent.change(fileInput, { target: { files: [image] } });
    const dropZone = within(formDialog).getByText(/Hoặc kéo ảnh/).parentElement;
    fireEvent.drop(dropZone, { dataTransfer: { files: [image] } });

    fireEvent.click(await screen.findByRole('button', { name: 'Lưu sản phẩm' }));

    await waitFor(() => expect(apiClient.post).toHaveBeenCalledTimes(1));
    const [url, formData, config] = apiClient.post.mock.calls[0];
    expect(url).toBe('/api/admin/foods');
    expect(formData).toBeInstanceOf(FormData);
    expect(formData.get('item')).toBeInstanceOf(Blob);
    expect(formData.get('image')).toBe(image);
    // Let Axios/the browser add the multipart boundary automatically.
    expect(config).toBeUndefined();
    expect(await screen.findByRole('alertdialog', { name: 'Đã thêm sản phẩm' })).toBeInTheDocument();
  });

  it('restores an archived product in paused state', async () => {
    const archivedItem = {
      ...sellingItem,
      id: 9,
      active: false,
      sellable: false,
      deleted: true
    };
    apiClient.get.mockResolvedValue({ data: { data: [archivedItem] } });
    render(<AdminConcessionInventoryPage />);

    fireEvent.click(await screen.findByRole('button', { name: 'Khôi phục Bắp rang cỡ lớn' }));

    await waitFor(() => expect(apiClient.patch).toHaveBeenCalledWith('/api/admin/foods/9/restore'));
    expect(await screen.findByRole('alertdialog', { name: 'Đã khôi phục sản phẩm' })).toBeInTheDocument();
  });

  it('loads optimized lazy thumbnails and keeps a bounded number of rows in the DOM', async () => {
    const items = Array.from({ length: 21 }, (_, index) => ({
      ...sellingItem,
      id: index + 1,
      code: `POP_${index + 1}`,
      name: `Sản phẩm ${index + 1}`,
      imageUrl: `https://images.unsplash.com/photo-${index + 1}`
    }));
    apiClient.get.mockResolvedValue({ data: { data: items } });

    render(<AdminConcessionInventoryPage />);

    const firstThumbnail = await screen.findByRole('img', { name: 'Sản phẩm 1' });
    expect(firstThumbnail).toHaveAttribute('loading', 'lazy');
    expect(firstThumbnail).toHaveAttribute('decoding', 'async');
    expect(firstThumbnail.getAttribute('src')).toContain('w=112');
    expect(firstThumbnail.getAttribute('src')).toContain('h=112');
    expect(screen.queryByText('Sản phẩm 21')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Sau' }));
    expect(await screen.findByText('Sản phẩm 21')).toBeInTheDocument();
    expect(screen.queryByText('Sản phẩm 1')).not.toBeInTheDocument();
  });
});
