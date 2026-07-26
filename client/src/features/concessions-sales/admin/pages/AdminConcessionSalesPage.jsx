import { useState, useEffect, useCallback, useMemo } from 'react';
import { TrendingUp, Coins, Coffee, ShoppingBag, Search, AlertCircle } from 'lucide-react';
import apiClient from "@/services/apiClient";

export default function AdminConcessionSalesPage() {
  const [stats, setStats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Search filter
  const [searchQuery, setSearchQuery] = useState('');
  // Sort state
  const [sortBy, setSortBy] = useState('revenue'); // 'quantity' or 'revenue'

  const fetchStats = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.get('/api/admin/foods/statistics');
      setStats(response.data.data || []);
    } catch (err) {
      setError(err.response?.data?.message || err.message || "Không thể tải báo cáo doanh thu bắp nước.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  // Aggregate values
  const aggregates = useMemo(() => {
    let totalRevenue = 0;
    let totalQuantity = 0;
    let foodQty = 0;
    let drinkQty = 0;
    let comboQty = 0;

    stats.forEach(item => {
      totalRevenue += item.totalAmount || 0;
      totalQuantity += item.totalQuantity || 0;

      if (item.productType === 'FOOD') {
        foodQty += item.totalQuantity || 0;
      } else if (item.productType === 'DRINK') {
        drinkQty += item.totalQuantity || 0;
      } else if (item.productType === 'COMBO') {
        comboQty += item.totalQuantity || 0;
      }
    });

    return { totalRevenue, totalQuantity, foodQty, drinkQty, comboQty };
  }, [stats]);

  // Filtered and Sorted Stats
  const filteredAndSortedStats = useMemo(() => {
    const filtered = stats.filter(item => 
      item.productName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.productCode?.toLowerCase().includes(searchQuery.toLowerCase())
    );

    return filtered.sort((a, b) => {
      if (sortBy === 'revenue') {
        return (b.totalAmount || 0) - (a.totalAmount || 0);
      } else {
        return (b.totalQuantity || 0) - (a.totalQuantity || 0);
      }
    });
  }, [stats, searchQuery, sortBy]);

  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-[400px] bg-zinc-950 text-white space-y-6">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center border-b border-zinc-800 pb-6 gap-4">
        <div>
          <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">DOANH THU BẮP NƯỚC & COMBO</h1>
          <p className="text-xs text-zinc-400 mt-1">Báo cáo doanh số bán các mặt hàng dịch vụ đi kèm</p>
        </div>
        <button
          onClick={fetchStats}
          className="bg-zinc-900 border border-zinc-800 hover:bg-zinc-800 text-white text-xs font-black uppercase tracking-wider px-4 py-2.5 rounded-xl transition-all cursor-pointer"
        >
          Làm mới báo cáo
        </button>
      </div>

      {/* Aggregate Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        {/* Card 1: Total Revenue */}
        <div className="bg-zinc-900/40 border border-zinc-850 p-6 rounded-3xl flex items-center gap-4 relative overflow-hidden group shadow-lg">
          <div className="absolute right-0 top-0 w-24 h-24 bg-amber-500/5 rounded-full blur-2xl transform translate-x-4 -translate-y-4 group-hover:scale-125 transition-transform duration-300"></div>
          <div className="bg-amber-500/10 p-4 text-amber-500 rounded-2xl">
            <Coins className="w-6 h-6" />
          </div>
          <div>
            <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Tổng doanh thu</span>
            <h3 className="text-xl font-black text-zinc-100 tracking-tight mt-1">
              {aggregates.totalRevenue.toLocaleString()}đ
            </h3>
          </div>
        </div>

        {/* Card 2: Total Quantity */}
        <div className="bg-zinc-900/40 border border-zinc-850 p-6 rounded-3xl flex items-center gap-4 relative overflow-hidden group shadow-lg">
          <div className="absolute right-0 top-0 w-24 h-24 bg-blue-500/5 rounded-full blur-2xl transform translate-x-4 -translate-y-4 group-hover:scale-125 transition-transform duration-300"></div>
          <div className="bg-blue-500/10 p-4 text-blue-500 rounded-2xl">
            <ShoppingBag className="w-6 h-6" />
          </div>
          <div>
            <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Số lượng bán ra</span>
            <h3 className="text-xl font-black text-zinc-100 tracking-tight mt-1">
              {aggregates.totalQuantity.toLocaleString()} phần
            </h3>
          </div>
        </div>

        {/* Card 3: Food & Combo Items */}
        <div className="bg-zinc-900/40 border border-zinc-850 p-6 rounded-3xl flex items-center gap-4 relative overflow-hidden group shadow-lg">
          <div className="absolute right-0 top-0 w-24 h-24 bg-purple-500/5 rounded-full blur-2xl transform translate-x-4 -translate-y-4 group-hover:scale-125 transition-transform duration-300"></div>
          <div className="bg-purple-500/10 p-4 text-purple-500 rounded-2xl">
            <TrendingUp className="w-6 h-6" />
          </div>
          <div>
            <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Combo / Đồ ăn bán</span>
            <h3 className="text-xl font-black text-zinc-100 tracking-tight mt-1">
              {(aggregates.comboQty + aggregates.foodQty).toLocaleString()} phần
            </h3>
          </div>
        </div>

        {/* Card 4: Drinks */}
        <div className="bg-zinc-900/40 border border-zinc-850 p-6 rounded-3xl flex items-center gap-4 relative overflow-hidden group shadow-lg">
          <div className="absolute right-0 top-0 w-24 h-24 bg-emerald-500/5 rounded-full blur-2xl transform translate-x-4 -translate-y-4 group-hover:scale-125 transition-transform duration-300"></div>
          <div className="bg-emerald-500/10 p-4 text-emerald-500 rounded-2xl">
            <Coffee className="w-6 h-6" />
          </div>
          <div>
            <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Nước ngọt đã bán</span>
            <h3 className="text-xl font-black text-zinc-100 tracking-tight mt-1">
              {aggregates.drinkQty.toLocaleString()} ly
            </h3>
          </div>
        </div>
      </div>

      {/* Filter and Sorting Options */}
      <div className="flex flex-col md:flex-row gap-4 justify-between items-center bg-zinc-900/50 p-4 rounded-2xl border border-zinc-800/40">
        <div className="relative w-full md:w-96">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-500 w-4 h-4" />
          <input
            type="text"
            placeholder="Tìm kiếm theo mã hoặc tên bắp nước..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 bg-zinc-950 border border-zinc-800 rounded-xl text-sm focus:outline-none focus:border-amber-500 text-white transition-all"
          />
        </div>
        <div className="flex items-center gap-3 w-full md:w-auto">
          <span className="text-xs text-zinc-400 font-bold uppercase tracking-wider shrink-0">Sắp xếp theo:</span>
          <div className="flex bg-zinc-950 p-1 border border-zinc-800 rounded-xl w-full sm:w-auto">
            <button
              onClick={() => setSortBy('revenue')}
              className={`flex-1 sm:flex-none px-4 py-1.5 rounded-lg text-xs font-black uppercase tracking-wider transition-all ${
                sortBy === 'revenue' ? 'bg-amber-500 text-black' : 'text-zinc-400 hover:text-white'
              }`}
            >
              Doanh thu
            </button>
            <button
              onClick={() => setSortBy('quantity')}
              className={`flex-1 sm:flex-none px-4 py-1.5 rounded-lg text-xs font-black uppercase tracking-wider transition-all ${
                sortBy === 'quantity' ? 'bg-amber-500 text-black' : 'text-zinc-400 hover:text-white'
              }`}
            >
              Số lượng
            </button>
          </div>
        </div>
      </div>

      {/* Stats Table / List */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-16 gap-3">
          <div className="w-10 h-10 border-4 border-amber-500 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-zinc-500 text-xs">Đang xử lý thống kê doanh thu...</p>
        </div>
      ) : error ? (
        <div className="bg-red-950/50 border border-red-500/30 text-red-200 p-5 rounded-2xl flex items-center gap-3 text-sm">
          <AlertCircle size={18} className="text-red-400" />
          <span>{error}</span>
        </div>
      ) : filteredAndSortedStats.length === 0 ? (
        <div className="text-center py-16 bg-zinc-900/20 rounded-3xl border border-dashed border-zinc-850">
          <Coffee size={40} className="mx-auto text-zinc-600 mb-3" />
          <p className="text-zinc-400 text-sm font-medium">Chưa phát sinh giao dịch bắp nước nào</p>
          <p className="text-zinc-650 text-xs mt-1">Thông số thống kê sẽ xuất hiện sau khi khách thanh toán thành công đơn hàng</p>
        </div>
      ) : (
        <div className="bg-zinc-900/60 border border-zinc-800/60 rounded-3xl overflow-hidden shadow-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-zinc-800 bg-zinc-900/30">
                  <th className="px-6 py-4 text-xs font-black uppercase tracking-wider text-zinc-400">Mã sản phẩm</th>
                  <th className="px-6 py-4 text-xs font-black uppercase tracking-wider text-zinc-400">Tên bắp nước</th>
                  <th className="px-6 py-4 text-xs font-black uppercase tracking-wider text-zinc-400">Phân loại</th>
                  <th className="px-6 py-4 text-xs font-black uppercase tracking-wider text-zinc-400 text-right">Số lượng bán</th>
                  <th className="px-6 py-4 text-xs font-black uppercase tracking-wider text-zinc-400 text-right">Doanh thu bán</th>
                  <th className="px-6 py-4 text-xs font-black uppercase tracking-wider text-zinc-400">Tỷ trọng doanh số</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-850">
                {filteredAndSortedStats.map(item => {
                  const revenueShare = aggregates.totalRevenue > 0 
                    ? (item.totalAmount / aggregates.totalRevenue) * 100 
                    : 0;

                  return (
                    <tr key={item.productCode} className="hover:bg-zinc-900/40 transition-colors">
                      <td className="px-6 py-4 font-mono text-xs font-bold text-amber-500">{item.productCode}</td>
                      <td className="px-6 py-4 font-semibold text-sm">{item.productName}</td>
                      <td className="px-6 py-4">
                        <span className={`px-2.5 py-1 rounded-lg text-[10px] font-black uppercase tracking-wider ${
                          item.productType === 'COMBO' 
                            ? 'bg-purple-500/10 border border-purple-500/30 text-purple-400'
                            : item.productType === 'DRINK'
                              ? 'bg-blue-500/10 border border-blue-500/30 text-blue-400'
                              : 'bg-amber-500/10 border border-amber-500/30 text-amber-400'
                        }`}>
                          {item.productType}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-right font-bold text-sm text-zinc-350">
                        {item.totalQuantity?.toLocaleString()}
                      </td>
                      <td className="px-6 py-4 text-right font-black text-sm text-zinc-100">
                        {item.totalAmount?.toLocaleString()}đ
                      </td>
                      <td className="px-6 py-4 w-1/4">
                        <div className="flex items-center gap-3">
                          <div className="w-full bg-zinc-950 h-2 rounded-full overflow-hidden border border-zinc-850">
                            <div 
                              className="bg-gradient-to-r from-orange-500 to-amber-500 h-full rounded-full" 
                              style={{ width: `${Math.max(3, revenueShare)}%` }}
                            ></div>
                          </div>
                          <span className="text-xs font-mono font-bold text-zinc-400 shrink-0">
                            {revenueShare.toFixed(1)}%
                          </span>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

    </div>
  );
}
