import { useState, useEffect, useCallback } from 'react';
import { ShoppingCart, Plus, Minus, Trash2, CheckCircle2, XCircle, Coffee, Coins } from 'lucide-react';
import apiClient from "@/services/apiClient";

export default function EmployeePOSPage() {
  const [concessions, setConcessions] = useState([]);
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState(null);
  const [processing, setProcessing] = useState(false);

  // Fetch concessions list
  const fetchConcessions = useCallback(async () => {
    try {
      const res = await apiClient.get('/api/customer/concessions');
      setConcessions(res.data.data || []);
    } catch (err) {
      console.error("Lỗi lấy danh sách bắp nước:", err);
    }
  }, []);

  // Fetch current cart
  const fetchCart = useCallback(async () => {
    try {
      const res = await apiClient.get('/api/customer/cart');
      setCart(res.data.data);
    } catch (err) {
      console.error("Lỗi lấy giỏ hàng:", err);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true);
    Promise.all([fetchConcessions(), fetchCart()]).finally(() => setLoading(false));
  }, [fetchConcessions, fetchCart]);

  // Find quantity and item ID in current cart
  const getCartItemInfo = (productId) => {
    if (!cart || !cart.items) return { quantity: 0, itemId: null };
    const item = cart.items.find(i => i.productId === productId);
    return item ? { quantity: item.quantity, itemId: item.id } : { quantity: 0, itemId: null };
  };

  // Handle adding or changing quantity
  const handleQuantityChange = async (product, increment) => {
    if (updatingId || processing) return;
    setUpdatingId(product.id);

    const { quantity, itemId } = getCartItemInfo(product.id);
    const newQty = quantity + (increment ? 1 : -1);

    try {
      let res;
      if (quantity === 0 && increment) {
        res = await apiClient.post('/api/customer/cart/items', { productId: product.id, quantity: 1 });
        setCart(res.data.data);
      } else if (newQty > 0) {
        res = await apiClient.put(`/api/customer/cart/items/${itemId}`, { quantity: newQty });
        setCart(res.data.data);
      } else {
        await apiClient.delete(`/api/customer/cart/items/${itemId}`);
        await fetchCart(); // Refresh cart to ensure consistency
      }
    } catch (err) {
      alert("Lỗi cập nhật giỏ hàng: " + (err.response?.data?.message || err.message));
    } finally {
      setUpdatingId(null);
    }
  };

  // Remove completely
  const handleRemoveItem = async (itemId) => {
    if (updatingId || processing) return;
    setUpdatingId(itemId);
    try {
      await apiClient.delete(`/api/customer/cart/items/${itemId}`);
      await fetchCart();
    } catch (err) {
      alert("Lỗi xóa mặt hàng: " + (err.response?.data?.message || err.message));
    } finally {
      setUpdatingId(null);
    }
  };

  // Checkout (Confirm Order)
  const handleCheckout = async () => {
    if (!cart || !cart.items || cart.items.length === 0 || processing) return;
    setProcessing(true);
    try {
      const res = await apiClient.post('/api/customer/cart/checkout');
      setCart(res.data.data);
    } catch (err) {
      alert("Lỗi checkout: " + (err.response?.data?.message || err.message));
    } finally {
      setProcessing(false);
    }
  };

  // Mock Payment
  const handleMockPay = async (success) => {
    if (processing) return;
    setProcessing(true);
    try {
      const res = await apiClient.post(`/api/customer/cart/mock-pay?success=${success}`);
      alert(success ? "Thanh toán thành công!" : "Thanh toán thất bại!");
      setCart(res.data.data);
      
      // If payment is completed or cancelled, we might want to clear the POS view
      // Just re-fetching will get the new pending cart
      setTimeout(() => {
        fetchCart();
      }, 1500);
    } catch (err) {
      alert("Lỗi giả lập thanh toán: " + (err.response?.data?.message || err.message));
    } finally {
      setProcessing(false);
    }
  };

  const formatCurrency = (val) => (val || 0).toLocaleString('vi-VN') + 'đ';

  const isCartConfirmed = cart?.status === 'CONFIRMED' && cart?.paymentStatus !== 'SUCCESS';
  const isCartTerminal = (cart?.status === 'CONFIRMED' && cart?.paymentStatus === 'SUCCESS') || cart?.status === 'CANCELLED';

  if (loading) {
    return (
      <div className="flex-1 h-full flex items-center justify-center p-6 bg-zinc-950">
        <div className="w-10 h-10 border-4 border-amber-500 border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  return (
    <div className="flex-1 h-full overflow-hidden flex flex-col p-6 bg-zinc-950 space-y-6">
      <div className="flex justify-between items-center pb-2 border-b border-zinc-800 shrink-0">
        <div>
          <h3 className="text-base font-bold text-zinc-50 uppercase tracking-wide">Bán Hàng Tại Quầy (POS)</h3>
          <p className="text-xs text-zinc-400 mt-1 uppercase tracking-wide">Quản lý đơn hàng bắp nước độc lập</p>
        </div>
      </div>
      
      <div className="flex-1 flex gap-6 overflow-hidden">
        {/* Left Side: Product Grid */}
        <div className="flex-[3] overflow-y-auto pr-2 pb-20 space-y-4">
          <div className="grid grid-cols-2 lg:grid-cols-3 gap-4">
            {concessions.map(product => {
              const { quantity } = getCartItemInfo(product.id);
              const isUpdating = updatingId === product.id;

              return (
                <div key={product.id} className="bg-zinc-900 border border-zinc-850 rounded-2xl p-4 flex flex-col transition-all hover:border-amber-500/50">
                  <div className="aspect-square bg-zinc-950 rounded-xl mb-3 overflow-hidden border border-zinc-800">
                    <img 
                      src={product.imageUrl} 
                      alt={product.name}
                      className="w-full h-full object-cover"
                      onError={(e) => {
                        e.target.onerror = null;
                        e.target.src = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' fill='%2318181b'><rect width='100%' height='100%'/></svg>";
                      }}
                    />
                  </div>
                  <h4 className="text-sm font-black text-white line-clamp-1">{product.name}</h4>
                  <p className="text-xs font-bold text-amber-500 mt-1">{formatCurrency(product.price)}</p>
                  
                  <div className="mt-4 pt-3 border-t border-zinc-800 flex items-center justify-between">
                    {quantity > 0 ? (
                      <div className="flex items-center gap-3 bg-zinc-950 border border-zinc-800 rounded-lg p-1 w-full justify-between">
                        <button
                          disabled={isUpdating || isCartConfirmed || isCartTerminal}
                          onClick={() => handleQuantityChange(product, false)}
                          className="w-8 h-8 flex items-center justify-center bg-zinc-800 hover:bg-zinc-700 text-white rounded font-black transition-colors disabled:opacity-50"
                        >
                          <Minus size={14} />
                        </button>
                        <span className="font-bold text-sm text-white w-6 text-center">
                          {isUpdating ? '...' : quantity}
                        </span>
                        <button
                          disabled={isUpdating || isCartConfirmed || isCartTerminal}
                          onClick={() => handleQuantityChange(product, true)}
                          className="w-8 h-8 flex items-center justify-center bg-amber-500 hover:bg-amber-600 text-black rounded font-black transition-colors disabled:opacity-50"
                        >
                          <Plus size={14} />
                        </button>
                      </div>
                    ) : (
                      <button
                        disabled={isUpdating || isCartConfirmed || isCartTerminal}
                        onClick={() => handleQuantityChange(product, true)}
                        className="w-full py-2 bg-zinc-800 hover:bg-amber-500 hover:text-black text-zinc-300 font-bold text-xs uppercase rounded-lg transition-colors disabled:opacity-50"
                      >
                        Thêm vào đơn
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Right Side: Cart Summary & Checkout */}
        <div className="flex-[2] bg-zinc-900 border border-zinc-800 rounded-3xl flex flex-col h-full overflow-hidden shadow-2xl">
          <div className="p-5 border-b border-zinc-800 bg-zinc-950/50 flex items-center gap-3">
            <ShoppingCart className="text-amber-500" />
            <h2 className="text-sm font-black uppercase tracking-wider text-white">
              Đơn hàng {cart?.publicId ? `#${cart.publicId.substring(0, 8).toUpperCase()}` : ''}
            </h2>
            {cart?.status && (
              <span className={`ml-auto text-[10px] font-black px-2 py-1 rounded-md uppercase ${
                cart.status === 'PENDING' ? 'bg-zinc-800 text-zinc-400' :
                (isCartTerminal && cart.paymentStatus === 'SUCCESS') ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' :
                isCartConfirmed ? 'bg-blue-500/20 text-blue-400 border border-blue-500/30' :
                'bg-red-500/20 text-red-400 border border-red-500/30'
              }`}>
                {(isCartTerminal && cart.paymentStatus === 'SUCCESS') ? 'COMPLETED' : cart.status}
              </span>
            )}
          </div>

          <div className="flex-1 overflow-y-auto p-2">
            {!cart?.items || cart.items.length === 0 ? (
              <div className="h-full flex flex-col items-center justify-center text-zinc-500 gap-3">
                <Coffee size={40} className="opacity-20" />
                <p className="text-xs font-bold uppercase tracking-wider">Đơn hàng trống</p>
              </div>
            ) : (
              <div className="space-y-2 p-3">
                {cart.items.map(item => (
                  <div key={item.id} className="bg-zinc-950 border border-zinc-850 rounded-xl p-3 flex gap-3">
                    <div className="flex-1 space-y-1">
                      <h4 className="text-xs font-bold text-zinc-200 line-clamp-1">{item.productName}</h4>
                      <p className="text-[10px] text-amber-500 font-bold">{formatCurrency(item.unitPrice)} x {item.quantity}</p>
                    </div>
                    <div className="flex flex-col items-end justify-between">
                      <span className="text-sm font-black text-white">{formatCurrency(item.finalAmount)}</span>
                      {!isCartConfirmed && !isCartTerminal && (
                        <button
                          disabled={updatingId === item.id || processing}
                          onClick={() => handleRemoveItem(item.id)}
                          className="text-zinc-600 hover:text-red-500 transition-colors"
                        >
                          <Trash2 size={14} />
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Totals & Actions */}
          <div className="p-5 bg-zinc-950 border-t border-zinc-800 space-y-4 shrink-0">
            <div className="space-y-2 text-sm">
              <div className="flex justify-between text-zinc-400">
                <span>Tổng số lượng:</span>
                <span className="font-bold text-white">{cart?.totalQuantity || 0}</span>
              </div>
              <div className="flex justify-between text-zinc-400">
                <span>Tạm tính:</span>
                <span className="font-bold text-white">{formatCurrency(cart?.subtotal || 0)}</span>
              </div>
              <div className="flex justify-between items-center pt-2 border-t border-zinc-850">
                <span className="text-xs font-black uppercase text-zinc-500">Khách phải trả</span>
                <span className="text-xl font-black text-amber-500">{formatCurrency(cart?.finalAmount || 0)}</span>
              </div>
            </div>

            {/* State Actions */}
            <div className="pt-2">
              {isCartTerminal ? (
                <button
                  onClick={fetchCart}
                  className="w-full py-4 bg-zinc-800 hover:bg-zinc-700 text-white font-black uppercase text-xs rounded-xl transition-all flex items-center justify-center gap-2"
                >
                  <Plus size={16} /> Tạo đơn hàng mới
                </button>
              ) : isCartConfirmed ? (
                <div className="space-y-3">
                  <div className="p-3 border border-blue-500/30 bg-blue-500/10 rounded-xl text-center">
                    <p className="text-[10px] font-bold text-blue-400 uppercase">Chờ thanh toán</p>
                  </div>
                  <div className="flex gap-2">
                    <button
                      disabled={processing}
                      onClick={() => handleMockPay(true)}
                      className="flex-1 py-3 bg-emerald-500 hover:bg-emerald-600 text-black font-black uppercase text-[10px] rounded-xl transition-all flex items-center justify-center gap-2"
                    >
                      <CheckCircle2 size={16} /> Pay Success
                    </button>
                    <button
                      disabled={processing}
                      onClick={() => handleMockPay(false)}
                      className="flex-1 py-3 bg-red-500 hover:bg-red-600 text-white font-black uppercase text-[10px] rounded-xl transition-all flex items-center justify-center gap-2"
                    >
                      <XCircle size={16} /> Pay Fail
                    </button>
                  </div>
                  <button
                    disabled={processing}
                    onClick={fetchCart}
                    className="w-full py-2 text-zinc-500 hover:text-zinc-300 font-bold text-[10px] uppercase transition-colors"
                  >
                    Hủy & Làm mới
                  </button>
                </div>
              ) : (
                <button
                  disabled={!cart?.items || cart.items.length === 0 || processing}
                  onClick={handleCheckout}
                  className="w-full py-4 bg-amber-500 hover:bg-amber-600 disabled:bg-zinc-800 disabled:text-zinc-600 text-black font-black uppercase text-xs rounded-xl shadow-lg transition-all flex items-center justify-center gap-2"
                >
                  <Coins size={16} /> Checkout & Thanh toán
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
