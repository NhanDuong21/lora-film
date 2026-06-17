import { useMemo } from 'react';
import { 
  Ticket, 
  Users, 
  Activity, 
  Coins, 
  UserCheck, 
  Settings
} from 'lucide-react';

export default function AdminDashboardView({ timeFilter }) {

  // Multi-tier KPI metrics mapping based on selected time filter
  const kpiData = useMemo(() => {
    const data = {
      today: {
        revenue: '24.620.000 đ',
        tickets: '1.420 vé',
        ticketsCounter: '620 -vé',
        ticketsCounterNum: '620 vé',
        ticketsOnline: '800 -vé',
        ticketsOnlineNum: '800 vé',
        customers: '8.950 hội viên',
        occupancy: '78.4%'
      },
      '7days': {
        revenue: '184.500.000 đ',
        tickets: '9.840-vé',
        ticketsCounter: '4.120-vé',
        ticketsCounterNum: '4.120 vé',
        ticketsOnline: '5.720 vé',
        customers: '9.020 hội viên',
        occupancy: '82.1%'
      },
      month: {
        revenue: '752.000.000 đ',
        tickets: '38.150 vé',
        ticketsCounter: '15.200-vé',
        ticketsCounterNum: '15.200 vé',
        ticketsOnline: '22.950 vé',
        customers: '9.480 hội viên',
        occupancy: '80.5%'
      },
      year: {
        revenue: '8.940.000.000 đ',
        tickets: '412.300-vé',
        ticketsCounter: '172.100-vé',
        ticketsCounterNum: '172.100 vé',
        ticketsOnline: '240.200-vé',
        ticketsOnlineNum: '240.200 vé',
        customers: '12.450 hội viên',
        occupancy: '76.2%'
      }
    };
    // Normalize mapping
    const res = data[timeFilter];
    return {
      revenue: res.revenue,
      tickets: res.tickets.replace('-vé', ' vé'),
      ticketsCounter: res.ticketsCounterNum || res.ticketsCounter.replace('-vé', ' vé'),
      ticketsOnline: res.ticketsOnlineNum || res.ticketsOnline.replace('-vé', ' vé'),
      customers: res.customers,
      occupancy: res.occupancy
    };
  }, [timeFilter]);

  // Section 3: SVG Chart Paths computations
  // 7-day revenue dataset path for SVG
  const revenuePoints = [
    { day: '25/05', val: 120 },
    { day: '26/05', val: 150 },
    { day: '27/05', val: 110 },
    { day: '28/05', val: 180 },
    { day: '29/05', val: 220 },
    { day: '30/05', val: 260 },
    { day: '31/05', val: 240 }
  ];

  // 7-day tickets quantity dataset for bar heights
  const ticketBars = [
    { day: '25/05', val: 320 },
    { day: '26/05', val: 410 },
    { day: '27/05', val: 290 },
    { day: '28/05', val: 490 },
    { day: '29/05', val: 580 },
    { day: '30/05', val: 680 },
    { day: '31/05', val: 620 }
  ];

  return (
    <div className="space-y-8">
      
      {/* SECTION 2: ROW OF 4 CORE KPI COUNTERS */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8 mt-2">
        
        {/* KPI 1: Tổng Doanh Thu */}
        <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-5 flex items-center justify-between shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300">
          <div>
            <span className="text-zinc-400 text-[10px] font-black uppercase tracking-wide block mb-1.5">
              TỔNG DOANH THU
            </span>
            <span className="text-2xl font-bold text-emerald-400 font-mono">
              {kpiData.revenue}
            </span>
          </div>
          <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl">
            <Coins className="w-5 h-5 text-emerald-400" />
          </div>
        </div>

        {/* KPI 2: Tổng Vé Đã Bán with Breakdown */}
        <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-5 flex items-center justify-between shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300">
          <div className="space-y-1">
            <span className="text-zinc-400 text-[10px] font-black uppercase tracking-wide block">
              TỔNG VÉ ĐÃ BÁN
            </span>
            <span className="text-2xl font-bold text-zinc-50">
              {kpiData.tickets}
            </span>
            <div className="flex items-center gap-2 pt-0.5 border-t border-zinc-800/60 mt-1">
              <span className="text-zinc-400 text-[10px]">
                Quầy: <strong className="font-semibold text-zinc-300">{kpiData.ticketsCounter}</strong>
              </span>
              <span className="text-zinc-500 text-[10px]">|</span>
              <span className="text-amber-400 text-[10px] font-medium">
                Online: <strong className="font-bold">{kpiData.ticketsOnline}</strong>
              </span>
            </div>
          </div>
          <div className="p-3 bg-amber-500/10 border border-amber-500/20 rounded-xl self-start">
            <Ticket className="w-5 h-5 text-amber-500" />
          </div>
        </div>

        {/* KPI 3: Tổng Khách Hàng */}
        <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-5 flex items-center justify-between shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300">
          <div>
            <span className="text-zinc-400 text-[10px] font-black uppercase tracking-wide block mb-1.5">
              TỔNG KHÁCH HÀNG
            </span>
            <span className="text-2xl font-bold text-zinc-50">
              {kpiData.customers}
            </span>
          </div>
          <div className="p-3 bg-indigo-500/10 border border-indigo-500/20 rounded-xl">
            <Users className="w-5 h-5 text-indigo-400" />
          </div>
        </div>

        {/* KPI 4: Tỷ Lệ Lấp Đầy */}
        <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-5 flex items-center justify-between shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300">
          <div>
            <span className="text-zinc-400 text-[10px] font-black uppercase tracking-wide block mb-1.5">
              TỶ LỆ LẤP ĐẦY PHÒNG
            </span>
            <span className="text-2xl font-bold text-indigo-400">
              {kpiData.occupancy}
            </span>
          </div>
          <div className="p-3 bg-indigo-500/10 border border-indigo-500/20 rounded-xl">
            <Activity className="w-5 h-5 text-indigo-400" />
          </div>
        </div>

      </div>

      {/* SECTION 3: REVENUE & SALES CHART GRID */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        
        {/* Left Panel: Doanh Thu 7 Ngày Qua */}
        <div className="bg-zinc-900/40 border border-zinc-800 rounded-2xl p-6 min-h-[320px] flex flex-col justify-between shadow-xl shadow-black/30 hover:border-zinc-750 transition-colors">
          <div className="flex items-center justify-between border-b border-zinc-800/60 pb-3 mb-2">
            <span className="text-xs font-black uppercase tracking-wide text-zinc-400">
              BIỂU ĐỒ DOANH THU 7 NGÀY QUA
            </span>
            <span className="text-[10px] text-emerald-400 font-mono font-bold uppercase">
              Đơn vị: Triệu VNĐ
            </span>
          </div>
          
          {/* SVG Line Curve representation */}
          <div className="flex-1 w-full relative min-h-[180px] mt-4 flex items-end">
            <svg className="w-full h-full" viewBox="0 0 500 150" preserveAspectRatio="none">
              <defs>
                <linearGradient id="revGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#10b981" stopOpacity="0.25" />
                  <stop offset="100%" stopColor="#10b981" stopOpacity="0" />
                </linearGradient>
              </defs>
              
              {/* Gridlines */}
              <line x1="0" y1="37.5" x2="500" y2="37.5" stroke="#27272a" strokeWidth="1" strokeDasharray="4" />
              <line x1="0" y1="75" x2="500" y2="75" stroke="#27272a" strokeWidth="1" strokeDasharray="4" />
              <line x1="0" y1="112.5" x2="500" y2="112.5" stroke="#27272a" strokeWidth="1" strokeDasharray="4" />

              {/* Area Under Curve */}
              <path
                d="M 10 130 Q 80 100 150 115 T 290 80 T 430 40 L 490 50 L 490 140 L 10 140 Z"
                fill="url(#revGrad)"
              />

              {/* Line Curve */}
              <path
                d="M 10 130 Q 80 100 150 115 T 290 80 T 430 40 L 490 50"
                fill="none"
                stroke="#10b981"
                strokeWidth="3.5"
                strokeLinecap="round"
              />
              
              {/* Nodes */}
              <circle cx="10" cy="130" r="4" fill="#059669" stroke="#34d399" strokeWidth="2" />
              <circle cx="80" cy="100" r="4" fill="#059669" stroke="#34d399" strokeWidth="2" />
              <circle cx="150" cy="115" r="4" fill="#059669" stroke="#34d399" strokeWidth="2" />
              <circle cx="290" cy="80" r="4" fill="#059669" stroke="#34d399" strokeWidth="2" />
              <circle cx="430" cy="40" r="4" fill="#059669" stroke="#34d399" strokeWidth="2" />
              <circle cx="490" cy="50" r="4" fill="#059669" stroke="#34d399" strokeWidth="2" />
            </svg>
          </div>

          <div className="flex justify-between items-center text-[10px] text-zinc-500 font-mono mt-4 pt-2 border-t border-zinc-800/40">
            {revenuePoints.map((pt, idx) => (
              <span key={idx}>{pt.day}</span>
            ))}
          </div>
        </div>

        {/* Right Panel: Vé Bán Ra 7 Ngày Qua */}
        <div className="bg-zinc-900/40 border border-zinc-800 rounded-2xl p-6 min-h-[320px] flex flex-col justify-between shadow-xl shadow-black/30 hover:border-zinc-750 transition-colors">
          <div className="flex items-center justify-between border-b border-zinc-800/60 pb-3 mb-2">
            <span className="text-xs font-black uppercase tracking-wide text-zinc-400">
              SẢN LƯỢNG VÉ BÁN 7 NGÀY QUA
            </span>
            <span className="text-[10px] text-amber-500 font-mono font-bold uppercase">
              Đơn vị: Vé
            </span>
          </div>

          {/* Bar Chart representation */}
          <div className="flex-1 w-full mt-4 flex items-end justify-between gap-3 min-h-[180px] px-2">
            {ticketBars.map((bar, idx) => {
              const maxVal = 700;
              const barHeight = `${(bar.val / maxVal) * 100}%`;
              return (
                <div key={idx} className="flex-1 flex flex-col items-center gap-2 group">
                  <div className="w-full relative flex items-end justify-center h-32">
                    {/* Tooltip value */}
                    <span className="absolute -top-6 bg-zinc-950 border border-zinc-800 text-amber-400 font-mono text-[9px] px-1.5 py-0.5 rounded opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none">
                      {bar.val}
                    </span>
                    <div 
                      className="w-full bg-amber-500/10 border-t-2 border-l border-r border-amber-500/30 group-hover:bg-amber-500/25 group-hover:border-amber-400 rounded-t-lg transition-all duration-300"
                      style={{ height: barHeight }}
                    />
                  </div>
                  <span className="text-[9px] text-zinc-500 font-mono group-hover:text-zinc-350 transition-colors">
                    {bar.day}
                  </span>
                </div>
              );
            })}
          </div>
        </div>

      </div>

      {/* SECTION 4: TOP MOVIES & RECENT ACTIVITIES GRID */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        
        {/* Left Column: Top Phim Bán Chạy Nhất */}
        <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-6 shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300">
          <div className="flex items-center justify-between border-b border-zinc-800/60 pb-3 mb-4">
            <span className="text-xs font-black uppercase tracking-wide text-zinc-400">
              TOP PHIM BÁN CHẠY NHẤT
            </span>
            <span className="text-[10px] text-zinc-500 font-mono">
              Doanh thu tuần này
            </span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-zinc-400">
              <thead className="text-[10px] text-zinc-500 font-black uppercase tracking-wider border-b border-zinc-800/60">
                <tr>
                  <th className="py-2.5 pb-3">Hạng</th>
                  <th className="py-2.5 pb-3">Tên Phim</th>
                  <th className="py-2.5 pb-3 text-center">Số Vé</th>
                  <th className="py-2.5 pb-3 text-right">Doanh Thu</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/40">
                <tr className="hover:bg-zinc-850/20 transition-colors">
                  <td className="py-3.5 font-bold">
                    <span className="w-5 h-5 rounded bg-amber-500/20 border border-amber-500/30 text-amber-400 text-[10px] flex items-center justify-center font-bold">
                      1
                    </span>
                  </td>
                  <td className="py-3.5 text-zinc-200 font-bold truncate max-w-[160px]">
                    John Wick: Ballerina
                  </td>
                  <td className="py-3.5 text-center font-mono text-zinc-300">
                    580
                  </td>
                  <td className="py-3.5 text-right font-mono text-emerald-400 font-bold">
                    58.200.000 đ
                  </td>
                </tr>
                <tr className="hover:bg-zinc-850/20 transition-colors">
                  <td className="py-3.5 font-bold">
                    <span className="w-5 h-5 rounded bg-zinc-800 border border-zinc-700 text-zinc-300 text-[10px] flex items-center justify-center font-bold">
                      2
                    </span>
                  </td>
                  <td className="py-3.5 text-zinc-200 font-bold truncate max-w-[160px]">
                    Định Mệnh Là Yêu
                  </td>
                  <td className="py-3.5 text-center font-mono text-zinc-300">
                    410
                  </td>
                  <td className="py-3.5 text-right font-mono text-emerald-400 font-bold">
                    32.800.000 đ
                  </td>
                </tr>
                <tr className="hover:bg-zinc-850/20 transition-colors">
                  <td className="py-3.5 font-bold">
                    <span className="w-5 h-5 rounded bg-amber-900/10 border border-amber-950 text-amber-700 text-[10px] flex items-center justify-center font-bold">
                      3
                    </span>
                  </td>
                  <td className="py-3.5 text-zinc-200 font-bold truncate max-w-[160px]">
                    Mật Vụ Ong
                  </td>
                  <td className="py-3.5 text-center font-mono text-zinc-300">
                    280
                  </td>
                  <td className="py-3.5 text-right font-mono text-emerald-400 font-bold">
                    22.400.000 đ
                  </td>
                </tr>
                <tr className="hover:bg-zinc-850/20 transition-colors">
                  <td className="py-3.5">
                    <span className="w-5 h-5 text-[10px] flex items-center justify-center text-zinc-650">
                      4
                    </span>
                  </td>
                  <td className="py-3.5 text-zinc-300 font-medium truncate max-w-[160px]">
                    Kẻ Huỷ Diệt
                  </td>
                  <td className="py-3.5 text-center font-mono text-zinc-400">
                    150
                  </td>
                  <td className="py-3.5 text-right font-mono text-emerald-400">
                    12.000.000 đ
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        {/* Right Column: Hoạt Động Gần Đây */}
        <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-6 shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300">
          <div className="flex items-center justify-between border-b border-zinc-800/60 pb-3 mb-4">
            <span className="text-xs font-black uppercase tracking-wide text-zinc-400">
              HOẠT ĐỘNG GẦN ĐÂY
            </span>
            <span className="text-[10px] text-zinc-500 font-mono">
              Nhật ký kiểm toán
            </span>
          </div>

          <div className="space-y-4 max-h-[220px] overflow-y-auto pr-1 scrollbar-thin scrollbar-thumb-zinc-800 scrollbar-track-transparent">
            
            {/* Log item 1 */}
            <div className="flex items-start gap-3 border-b border-zinc-800/30 pb-3">
              <div className="p-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/20 shrink-0 mt-0.5">
                <Ticket className="w-3.5 h-3.5 text-emerald-400" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs text-zinc-300 leading-snug">
                  Nhân viên thu ngân <strong className="font-semibold text-zinc-200">NV-04</strong> vừa xuất 02 vé tại quầy Lora Nguyễn Du
                </p>
                <span className="text-zinc-500 font-mono text-[10px] block mt-1">10:45</span>
              </div>
            </div>

            {/* Log item 2 */}
            <div className="flex items-start gap-3 border-b border-zinc-800/30 pb-3">
              <div className="p-1.5 rounded-lg bg-amber-500/10 border border-amber-500/20 shrink-0 mt-0.5">
                <UserCheck className="w-3.5 h-3.5 text-amber-500" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs text-zinc-300 leading-snug">
                  Khách hàng <strong className="font-semibold text-zinc-200">Phạm Minh Đức</strong> vừa thanh toán thành công Combo Solo trực tuyến
                </p>
                <span className="text-zinc-500 font-mono text-[10px] block mt-1">10:30</span>
              </div>
            </div>

            {/* Log item 3 */}
            <div className="flex items-start gap-3 border-b border-zinc-800/30 pb-3">
              <div className="p-1.5 rounded-lg bg-indigo-500/10 border border-indigo-500/20 shrink-0 mt-0.5">
                <Settings className="w-3.5 h-3.5 text-indigo-400" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs text-zinc-300 leading-snug">
                  Quản trị viên đã cập nhật suất chiếu phim <strong className="font-semibold text-zinc-200">John Wick: Ballerina</strong> tại Phòng 3
                </p>
                <span className="text-zinc-500 font-mono text-[10px] block mt-1">09:15</span>
              </div>
            </div>

            {/* Log item 4 */}
            <div className="flex items-start gap-3">
              <div className="p-1.5 rounded-lg bg-zinc-800 border border-zinc-700 shrink-0 mt-0.5">
                <Activity className="w-3.5 h-3.5 text-zinc-400" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs text-zinc-300 leading-snug">
                  Hệ thống tự động kích hoạt ngưỡng trễ 15 phút lịch chiếu phim
                </p>
                <span className="text-zinc-500 font-mono text-[10px] block mt-1">08:00</span>
              </div>
            </div>

          </div>
        </div>

      </div>

    </div>
  );
}
