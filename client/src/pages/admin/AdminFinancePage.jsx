import { useState, useMemo } from 'react';
import { Search, CircleDollarSign, CupSoda, TrendingUp, DollarSign } from 'lucide-react';

export default function AdminFinanceView({
  activeTab,
  tickets,
  concessions,
  customers,
  employees,
  updateCustomersState,
  updateEmployeesState,
  triggerToast
}) {
  const [ticketSearch, setTicketSearch] = useState('');
  const [customerSearch, setCustomerSearch] = useState('');
  const [isPayrollApproved, setIsPayrollApproved] = useState(false);

  // 1. Filter Tickets
  const filteredTickets = useMemo(() => {
    if (!tickets) return [];
    return tickets.filter(t => 
      t?.id?.toLowerCase()?.includes(ticketSearch?.toLowerCase() ?? '') || 
      t?.customerName?.toLowerCase()?.includes(ticketSearch?.toLowerCase() ?? '')
    );
  }, [tickets, ticketSearch]);

  // 2. Filter Customers
  const filteredCustomers = useMemo(() => {
    if (!customers) return [];
    return customers.filter(c => 
      c?.name?.toLowerCase()?.includes(customerSearch?.toLowerCase() ?? '') || 
      c?.email?.toLowerCase()?.includes(customerSearch?.toLowerCase() ?? '')
    );
  }, [customers, customerSearch]);

  // 3. Customer Tier Toggle
  const handleToggleCustomerTier = (id) => {
    const updated = customers.map(c => {
      if (c.id === id) {
        const newTier = c.tier === 'VIP' ? 'Standard' : 'VIP';
        return { 
          ...c, 
          tier: newTier, 
          points: newTier === 'VIP' ? Math.max(c.points, 500) : c.points 
        };
      }
      return c;
    });
    updateCustomersState(updated);
    triggerToast('Cập nhật hạng thành viên thành công!');
  };

  // 4. Employee Payroll Handlers
  const handleAdjustWage = (id, newWage) => {
    if (isPayrollApproved) return;
    const updated = employees.map(emp => 
      emp.id === id ? { ...emp, hourlyWage: parseInt(newWage) || 0 } : emp
    );
    updateEmployeesState(updated);
  };

  const handleAdjustMultiplier = (id, newMultiplier) => {
    if (isPayrollApproved) return;
    const updated = employees.map(emp => 
      emp.id === id ? { ...emp, activeMultiplier: parseFloat(newMultiplier) || 1.0 } : emp
    );
    updateEmployeesState(updated);
  };

  const handlePayrollApprove = () => {
    setIsPayrollApproved(true);
    triggerToast('Duyệt chi lương tháng này thành công! Hóa đơn kế toán đã được kết xuất.');
  };

  return (
    <div className="space-y-6">
      {/* Tab: Tickets (Quản lý vé bán) */}
      {activeTab === 'tickets' && (
        <div className="space-y-6">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-zinc-800 pb-4">
            <div>
              <h3 className="text-base font-bold text-zinc-50 uppercase tracking-wide">SỔ NHẬT KÝ BÁN VÉ</h3>
              <p className="text-xs text-zinc-400 mt-1 uppercase tracking-wide">Tra cứu lịch sử xuất vé, hóa đơn điện tử và trạng thái check-in của khách hàng</p>
            </div>
            <div className="relative w-full sm:w-80">
              <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-zinc-500">
                <Search className="w-4 h-4" />
              </span>
              <input
                type="text"
                value={ticketSearch}
                onChange={(e) => setTicketSearch(e.target.value)}
                placeholder="Tìm kiếm theo mã vé hoặc tên khách hàng..."
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl py-2.5 pl-9 pr-4 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral transition-colors"
              />
            </div>
          </div>

          <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-zinc-400">
                <thead className="bg-zinc-950/80 text-zinc-400 font-black uppercase tracking-wider border-b border-zinc-800">
                  <tr>
                    <th className="py-4 px-6">Mã vé</th>
                    <th className="py-4 px-6">Khách hàng</th>
                    <th className="py-4 px-6">Phim</th>
                    <th className="py-4 px-6">Suất chiếu</th>
                    <th className="py-4 px-6">Ghế</th>
                    <th className="py-4 px-6">Tổng tiền</th>
                    <th className="py-4 px-6">Trạng thái</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-800/60">
                  {filteredTickets && filteredTickets.length > 0 ? (
                    filteredTickets.map((t, index) => (
                      <tr key={t?.id ?? index} className="hover:bg-zinc-900/20 transition-colors border-b border-zinc-800/40">
                        <td className="py-4 px-6 font-mono text-brand-yellow font-bold">{t?.id ?? ''}</td>
                        <td className="py-4 px-6 font-bold text-zinc-200">
                          <div>{t?.customerName ?? 'Chưa rõ khách'}</div>
                          <div className="text-[10px] text-zinc-400 mt-0.5">{t?.customerEmail ?? ''}</div>
                        </td>
                        <td className="py-4 px-6 text-zinc-200 font-medium">{t?.movieTitle ?? ''}</td>
                        <td className="py-4 px-6 font-semibold text-brand-coral">{t?.time ?? ''} | {t?.date ?? ''}</td>
                        <td className="py-4 px-6 text-zinc-200 font-medium">{(t?.seats ?? []).join(', ')}</td>
                        <td className="py-4 px-6 text-emerald-400 font-black text-sm">{(t?.totalAmount ?? 0).toLocaleString('vi-VN')} đ</td>
                        <td className="py-4 px-6">
                          <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase border ${
                            t?.status === 'DA_KIEM_TRA' 
                              ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' 
                              : 'bg-zinc-800 text-zinc-400 border-zinc-700/50'
                          }`}>
                            {t?.status === 'DA_KIEM_TRA' ? 'ĐÃ KIỂM TRA' : 'CHƯA CHECK-IN'}
                          </span>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="7" className="py-12 text-center text-zinc-500 font-semibold">
                        Không tìm thấy vé nào phù hợp.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Tab: Concessions (Doanh thu bắp nước - Concession Counter Stocks Tracker) */}
      {activeTab === 'concessions' && (() => {
        const totalConcessionRevenue = (concessions ?? []).reduce((acc, c) => acc + ((c?.price ?? 0) * (c?.salesCount ?? 0)), 0);
        const totalSalesCount = (concessions ?? []).reduce((acc, c) => acc + (c?.salesCount ?? 0), 0);
        const bestSellerItem = [...(concessions ?? [])].sort((a, b) => (b.salesCount ?? 0) - (a.salesCount ?? 0))[0];
        let bestSellerName = bestSellerItem?.name ?? 'Không rõ';
        if (bestSellerName === 'Popcorn Ngot') bestSellerName = 'Popcorn Ngọt';

        return (
          <div className="space-y-6 animate-fade-in">
            {/* Header Layer */}
            <div className="border-b border-zinc-900 pb-4">
              <p className="text-xs text-zinc-500 mt-1 uppercase tracking-wider">Hệ thống phân tích doanh thu ẩm thực chuyên sâu và quản lý sản lượng counter LoraFilm</p>
            </div>

            {/* Layer 1: High-Contrast KPI Summary Stream */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {/* Card 1 */}
              <div className="bg-zinc-900/60 border border-zinc-800 p-5 rounded-2xl flex items-center justify-between shadow-xl shadow-black/30">
                <div className="space-y-1">
                  <span className="text-[10px] font-mono font-black text-zinc-500 uppercase tracking-widest block">TỔNG DOANH SỐ KHU ẨM THỰC</span>
                  <span className="text-xl font-mono font-bold text-amber-400 block">{totalConcessionRevenue.toLocaleString('vi-VN')} đ</span>
                </div>
                <div className="w-10 h-10 rounded-xl bg-amber-500/10 flex items-center justify-center text-amber-500 border border-amber-500/20">
                  <DollarSign className="w-5 h-5" />
                </div>
              </div>

              {/* Card 2 */}
              <div className="bg-zinc-900/60 border border-zinc-800 p-5 rounded-2xl flex items-center justify-between shadow-xl shadow-black/30">
                <div className="space-y-1">
                  <span className="text-[10px] font-mono font-black text-zinc-500 uppercase tracking-widest block">TỔNG SẢN LƯỢNG ĐÃ BÁN</span>
                  <span className="text-xl font-mono font-bold text-zinc-100 block">{totalSalesCount} phần</span>
                </div>
                <div className="w-10 h-10 rounded-xl bg-brand-coral/10 flex items-center justify-center text-brand-coral border border-brand-coral/20">
                  <CupSoda className="w-5 h-5" />
                </div>
              </div>

              {/* Card 3 */}
              <div className="bg-zinc-900/60 border border-zinc-800 p-5 rounded-2xl flex items-center justify-between shadow-xl shadow-black/30">
                <div className="space-y-1">
                  <span className="text-[10px] font-mono font-black text-zinc-500 uppercase tracking-widest block">SẢN PHẨM BEST-SELLER</span>
                  <span className="text-xl font-bold text-emerald-400 block truncate">{bestSellerName}</span>
                </div>
                <div className="w-10 h-10 rounded-xl bg-emerald-500/10 flex items-center justify-center text-emerald-400 border border-emerald-500/20">
                  <TrendingUp className="w-5 h-5" />
                </div>
              </div>
            </div>

            {/* Layer 2: Visual Consumption Breakdown & Charts */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Left Area (2/3 columns width) */}
              <div className="lg:col-span-2 bg-zinc-900/60 border border-zinc-800 p-5 rounded-2xl flex flex-col gap-4 shadow-xl">
                <div className="flex items-center justify-between pb-2 border-b border-zinc-800/60">
                  <span className="text-[10px] font-mono font-black text-zinc-400 uppercase tracking-widest">BIỂU ĐỒ SẢN LƯỢNG TIÊU THỤ</span>
                  <span className="text-[10px] text-zinc-500 font-mono">TỶ LỆ % ĐÓNG GÓP DOANH THU</span>
                </div>
                <div className="space-y-4">
                  {(concessions ?? []).map((c, index) => {
                    let displayName = c?.name ?? 'Sản phẩm dịch vụ';
                    if (c?.name === 'Popcorn Ngot') displayName = 'Popcorn Ngọt';
                    
                    const price = c?.price ?? 0;
                    const salesCount = c?.salesCount ?? 0;
                    const totalIncome = price * salesCount;
                    const percentage = totalConcessionRevenue > 0 ? ((totalIncome / totalConcessionRevenue) * 100).toFixed(1) : 0;

                    return (
                      <div key={c?.id ?? index} className="space-y-1">
                        <div className="flex justify-between items-center text-xs">
                          <span className="text-zinc-200 font-medium">{displayName}</span>
                          <span className="font-mono text-zinc-400 text-[11px]">{salesCount} phần ({percentage}%)</span>
                        </div>
                        <div className="w-full bg-zinc-950 h-2.5 rounded-full overflow-hidden border border-zinc-900">
                          <div 
                            className="bg-gradient-to-r from-amber-500 to-orange-500 h-full rounded-full transition-all duration-500" 
                            style={{ width: `${percentage}%` }}
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Right Area (1/3 column width) */}
              <div className="bg-zinc-900/60 border border-zinc-800 p-5 rounded-2xl flex flex-col gap-3 shadow-xl">
                <span className="text-[10px] font-mono font-black text-zinc-500 uppercase tracking-widest pb-1 border-b border-zinc-800/60">DANH MỤC SẢN PHẨM</span>
                <div className="flex flex-col gap-2 divide-y divide-zinc-800/50">
                  {(concessions ?? []).map((c, index) => {
                    let displayName = c?.name ?? 'Sản phẩm dịch vụ';
                    if (c?.name === 'Popcorn Ngot') displayName = 'Popcorn Ngọt';

                    const price = c?.price ?? 0;
                    const salesCount = c?.salesCount ?? 0;
                    const totalIncome = price * salesCount;

                    return (
                      <div key={c?.id ?? index} className="flex justify-between items-center text-xs text-zinc-300 py-2 border-b border-zinc-800/50 first:pt-0 last:border-b-0">
                        <span>{displayName}</span>
                        <span className="font-mono text-zinc-400 text-right">{salesCount} phần • <span className="text-emerald-400 font-semibold">{totalIncome.toLocaleString('vi-VN')} đ</span></span>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>
        );
      })()}

      {/* Tab: Customers (Danh sách khách hàng - Member Registration Directory) */}
      {activeTab === 'customers' && (
        <div className="space-y-6">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-zinc-900 pb-4">
            <div>
              <h3 className="text-base font-bold text-zinc-100 uppercase tracking-wider">Danh Sách Hội Viên</h3>
              <p className="text-xs text-zinc-500 mt-1 uppercase tracking-wider">Quản lý cấp bậc, điểm tích lũy của thành viên đăng ký</p>
            </div>
            <div className="relative w-full sm:w-80">
              <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-zinc-500">
                <Search className="w-4 h-4" />
              </span>
              <input
                type="text"
                value={customerSearch}
                onChange={(e) => setCustomerSearch(e.target.value)}
                placeholder="Tìm kiếm khách hàng..."
                className="w-full bg-zinc-950 border border-zinc-900 rounded-xl py-2.5 pl-9 pr-4 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral transition-colors"
              />
            </div>
          </div>

          <div className="bg-zinc-900/40 backdrop-blur-md border border-zinc-900 rounded-2xl shadow-xl overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-zinc-400">
                <thead className="bg-zinc-950/80 text-zinc-500 font-black uppercase tracking-wider border-b border-zinc-900">
                  <tr>
                    <th className="py-4 px-6">Tên Khách Hàng</th>
                    <th className="py-4 px-6">Email</th>
                    <th className="py-4 px-6">Hạng Thành Viên</th>
                    <th className="py-4 px-6">Điểm Tích Lũy</th>
                    <th className="py-4 px-6">Số Vé Đã Mua</th>
                    <th className="py-4 px-6 text-center w-40">Hành Động</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-900/60">
                  {filteredCustomers && filteredCustomers.length > 0 ? (
                    filteredCustomers.map((c, index) => {
                      const isVIP = c?.tier?.toUpperCase() === 'VIP';
                      return (
                        <tr key={c?.id ?? index} className="hover:bg-zinc-900/20 transition-colors">
                          <td className="py-4 px-6 font-bold text-zinc-100">{c?.name ?? 'Không rõ khách'}</td>
                          <td className="py-4 px-6 font-mono text-zinc-400">{c?.email ?? ''}</td>
                          <td className="py-4 px-6">
                            <span className={`px-2.5 py-0.5 rounded text-[10px] font-black uppercase border ${
                              isVIP
                                ? 'bg-amber-500/10 text-amber-500 border-amber-500/20' 
                                : 'bg-zinc-800 text-zinc-400 border-zinc-700/50'
                            }`}>
                              {isVIP ? 'VIP' : 'STANDARD'}
                            </span>
                          </td>
                          <td className="py-4 px-6 font-bold text-zinc-300">{c?.points ?? 0} điểm</td>
                          <td className="py-4 px-6 text-zinc-300 font-medium">{c?.ticketsBought ?? 0} vé</td>
                          <td className="py-4 px-6 text-center">
                            <button
                              onClick={() => handleToggleCustomerTier(c.id)}
                              className="text-[11px] font-black text-brand-coral hover:underline focus:outline-none"
                            >
                              {isVIP ? 'Hạ xuống Standard' : 'Thăng lên VIP'}
                            </button>
                          </td>
                        </tr>
                      );
                    })
                  ) : (
                    <tr>
                      <td colSpan="6" className="py-12 text-center text-zinc-500 font-semibold">
                        Không tìm thấy khách hàng nào phù hợp.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Tab: Payroll (Bảng lương nhân viên - Employee Payroll Ledger) */}
      {activeTab === 'payroll' && (
        <div className="space-y-6">
          <div className="flex justify-between items-center border-b border-zinc-900 pb-4">
            <div>
              <h3 className="text-base font-bold text-zinc-100 uppercase tracking-wider">BẢNG LƯƠNG NHÂN VIÊN</h3>
              <p className="text-xs text-zinc-500 mt-1 uppercase tracking-wider">Xem giờ công thực tế, tùy chỉnh hệ số lương và duyệt chi lương nhân sự hệ thống LoraFilm</p>
            </div>
            {isPayrollApproved ? (
              <button
                disabled
                className="bg-zinc-800 text-zinc-500 cursor-not-allowed border border-zinc-700/50 shadow-none px-6 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-all flex items-center gap-2"
              >
                BẢNG LƯƠNG ĐÃ DUYỆT
              </button>
            ) : (
              <button
                onClick={handlePayrollApprove}
                className="bg-emerald-500 hover:bg-emerald-600 text-black font-black px-6 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-all shadow-lg shadow-emerald-500/10 flex items-center gap-2"
              >
                CHỐT SỔ & DUYỆT CHI LƯƠNG
              </button>
            )}
          </div>

          <div className="bg-zinc-900/40 backdrop-blur-md border border-zinc-900 rounded-2xl shadow-xl overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-zinc-400">
                <thead className="bg-zinc-950/80 text-zinc-500 font-black uppercase tracking-wider border-b border-zinc-900">
                  <tr>
                    <th className="py-4 px-6">Tên Nhân Viên</th>
                    <th className="py-4 px-6">Chức Vụ</th>
                    <th className="py-4 px-6">Giờ Làm Việc</th>
                    <th className="py-4 px-6">Lương Giờ (VNĐ)</th>
                    <th className="py-4 px-6">Hệ Số Nhân</th>
                    <th className="py-4 px-6 text-right">Lương Thực Nhận</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-900/60">
                  {employees && employees.length > 0 ? (
                    employees.map((emp, index) => {
                      const hoursWorked = emp?.hoursWorked ?? 0;
                      const hourlyWage = emp?.hourlyWage ?? 0;
                      const activeMultiplier = emp?.activeMultiplier ?? 1.0;
                      const finalSalary = hoursWorked * hourlyWage * activeMultiplier;
                      return (
                        <tr key={emp?.id ?? index} className="hover:bg-zinc-900/20 transition-colors">
                          <td className="py-4 px-6 font-bold text-zinc-100">
                            <div>{emp?.name ?? 'Chưa rõ nhân viên'}</div>
                            <div className="text-[10px] text-zinc-500 font-mono mt-0.5">{emp?.email ?? ''}</div>
                          </td>
                          <td className="py-4 px-6 font-bold text-zinc-300">
                            {emp?.role === 'Cashier' ? 'Thu Ngân' : emp?.role === 'Supervisor' ? 'Giám Sát' : (emp?.role ?? '')}
                          </td>
                          <td className="py-4 px-6 text-zinc-300 font-medium">{hoursWorked} giờ</td>
                          <td className="py-4 px-6">
                            <input
                              type="number"
                              value={hourlyWage}
                              disabled={isPayrollApproved}
                              onChange={(e) => handleAdjustWage(emp.id, e.target.value)}
                              className="bg-zinc-950 border border-zinc-800 focus:border-amber-500 rounded-lg px-3 py-1.5 text-zinc-100 font-mono text-sm w-28 outline-none transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                            />
                          </td>
                          <td className="py-4 px-6">
                            <input
                              type="number"
                              step="0.1"
                              value={activeMultiplier}
                              disabled={isPayrollApproved}
                              onChange={(e) => handleAdjustMultiplier(emp.id, e.target.value)}
                              className="bg-zinc-950 border border-zinc-800 focus:border-amber-500 rounded-lg px-3 py-1.5 text-zinc-100 font-mono text-sm w-20 outline-none transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                            />
                          </td>
                          <td className="p-4 text-right text-emerald-400 font-mono font-bold text-sm">
                            {finalSalary.toLocaleString('vi-VN')} đ
                          </td>
                        </tr>
                      );
                    })
                  ) : (
                    <tr>
                      <td colSpan="6" className="text-center p-8 text-zinc-500 font-medium">
                        Chưa ghi nhận dữ liệu lương nhân sự.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
