import { useState } from 'react';
import { 
    TrendingUp, 
    DollarSign, 
    Ticket, 
    Coffee, 
    Calendar,
    Download,
    Filter,
    ArrowUpRight,
    ArrowDownRight,
    Film
} from 'lucide-react';

export default function AdminFinancePage() {
    const [timeRange, setTimeRange] = useState('this_month');

    // Mock data for demonstration
    const stats = [
        {
            title: "Tổng Doanh Thu",
            value: "2.450.000.000 ₫",
            trend: "+15.2%",
            isPositive: true,
            icon: DollarSign,
            color: "text-emerald-400",
            bg: "bg-emerald-500/10"
        },
        {
            title: "Doanh Thu Vé",
            value: "1.820.000.000 ₫",
            trend: "+12.5%",
            isPositive: true,
            icon: Ticket,
            color: "text-blue-400",
            bg: "bg-blue-500/10"
        },
        {
            title: "Doanh Thu Bắp Nước",
            value: "630.000.000 ₫",
            trend: "+8.4%",
            isPositive: true,
            icon: Coffee,
            color: "text-amber-400",
            bg: "bg-amber-500/10"
        },
        {
            title: "Tỷ Lệ Hoàn Vé",
            value: "1.2%",
            trend: "-0.5%",
            isPositive: true, // lower refund is positive
            icon: TrendingUp,
            color: "text-rose-400",
            bg: "bg-rose-500/10"
        }
    ];

    const topMovies = [
        { id: 1, name: "Mai", revenue: "850.000.000 ₫", tickets: 9500, percent: 85 },
        { id: 2, name: "Đào, Phở và Piano", revenue: "520.000.000 ₫", tickets: 6200, percent: 65 },
        { id: 3, name: "Dune: Part Two", revenue: "410.000.000 ₫", tickets: 4800, percent: 50 },
        { id: 4, name: "Kung Fu Panda 4", revenue: "320.000.000 ₫", tickets: 3500, percent: 40 },
    ];

    const recentTransactions = [
        { id: "TRX-9823", time: "10:24, 26/07", type: "Vé + Combo", amount: "250.000 ₫", status: "Thành công" },
        { id: "TRX-9824", time: "10:30, 26/07", type: "Vé", amount: "120.000 ₫", status: "Thành công" },
        { id: "TRX-9825", time: "10:45, 26/07", type: "Vé + Combo", amount: "380.000 ₫", status: "Hoàn tiền" },
        { id: "TRX-9826", time: "11:02, 26/07", type: "Combo", amount: "150.000 ₫", status: "Thành công" },
        { id: "TRX-9827", time: "11:15, 26/07", type: "Vé", amount: "240.000 ₫", status: "Thành công" },
    ];

    return (
        <div className="p-6 md:p-8 space-y-6 max-w-7xl mx-auto pb-20">
            {/* Header Section */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl md:text-3xl font-black text-white tracking-tight">Báo Cáo Tài Chính</h1>
                    <p className="text-zinc-400 text-sm mt-1">Tổng quan doanh thu và hiệu suất kinh doanh</p>
                </div>
                
                <div className="flex items-center gap-3">
                    <div className="bg-zinc-900 border border-zinc-800 rounded-xl p-1 flex">
                        {['today', 'this_week', 'this_month'].map(range => (
                            <button
                                key={range}
                                onClick={() => setTimeRange(range)}
                                className={`px-4 py-1.5 text-sm font-medium rounded-lg transition-all ${
                                    timeRange === range 
                                        ? 'bg-amber-500/20 text-amber-400' 
                                        : 'text-zinc-400 hover:text-white hover:bg-zinc-800'
                                }`}
                            >
                                {range === 'today' ? 'Hôm nay' : range === 'this_week' ? 'Tuần này' : 'Tháng này'}
                            </button>
                        ))}
                    </div>
                    <button className="flex items-center gap-2 bg-zinc-800 hover:bg-zinc-700 text-white px-4 py-2 rounded-xl text-sm font-medium transition-colors">
                        <Download className="w-4 h-4" />
                        <span className="hidden md:inline">Xuất báo cáo</span>
                    </button>
                </div>
            </div>

            {/* Stats Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                {stats.map((stat, idx) => (
                    <div key={idx} className="bg-zinc-900/80 border border-zinc-800/80 rounded-2xl p-5 hover:border-zinc-700 transition-colors">
                        <div className="flex justify-between items-start">
                            <div className={`p-3 rounded-xl ${stat.bg}`}>
                                <stat.icon className={`w-5 h-5 ${stat.color}`} />
                            </div>
                            <div className={`flex items-center gap-1 text-xs font-bold px-2 py-1 rounded-full ${
                                stat.isPositive ? 'bg-emerald-500/10 text-emerald-400' : 'bg-rose-500/10 text-rose-400'
                            }`}>
                                {stat.isPositive ? <ArrowUpRight className="w-3 h-3" /> : <ArrowDownRight className="w-3 h-3" />}
                                {stat.trend}
                            </div>
                        </div>
                        <div className="mt-4">
                            <h3 className="text-zinc-400 text-sm font-medium">{stat.title}</h3>
                            <p className="text-white text-2xl font-black tracking-tight mt-1">{stat.value}</p>
                        </div>
                    </div>
                ))}
            </div>

            <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
                {/* Top Movies Chart (Mock) */}
                <div className="xl:col-span-2 bg-zinc-900/80 border border-zinc-800/80 rounded-2xl p-6">
                    <div className="flex items-center justify-between mb-6">
                        <h2 className="text-lg font-bold text-white flex items-center gap-2">
                            <Film className="w-5 h-5 text-amber-500" />
                            Doanh thu theo phim
                        </h2>
                        <button className="text-zinc-400 hover:text-white transition-colors">
                            <Filter className="w-4 h-4" />
                        </button>
                    </div>
                    
                    <div className="space-y-5">
                        {topMovies.map(movie => (
                            <div key={movie.id} className="space-y-2">
                                <div className="flex justify-between items-center text-sm">
                                    <span className="font-semibold text-zinc-200">{movie.name}</span>
                                    <div className="flex items-center gap-4 text-zinc-400">
                                        <span className="text-xs">{movie.tickets} vé</span>
                                        <span className="font-bold text-white">{movie.revenue}</span>
                                    </div>
                                </div>
                                <div className="h-2 w-full bg-zinc-800 rounded-full overflow-hidden">
                                    <div 
                                        className="h-full bg-gradient-to-r from-amber-600 to-amber-400 rounded-full" 
                                        style={{ width: `${movie.percent}%` }}
                                    ></div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Recent Transactions */}
                <div className="bg-zinc-900/80 border border-zinc-800/80 rounded-2xl p-6 flex flex-col">
                    <h2 className="text-lg font-bold text-white mb-6">Giao dịch gần đây</h2>
                    
                    <div className="flex-1 overflow-y-auto pr-2 space-y-4">
                        {recentTransactions.map((trx, idx) => (
                            <div key={idx} className="flex items-center justify-between p-3 rounded-xl hover:bg-zinc-800/50 transition-colors border border-transparent hover:border-zinc-800">
                                <div>
                                    <p className="text-sm font-bold text-zinc-200">{trx.id}</p>
                                    <p className="text-xs text-zinc-500 mt-0.5">{trx.time} • {trx.type}</p>
                                </div>
                                <div className="text-right">
                                    <p className="text-sm font-bold text-white">{trx.amount}</p>
                                    <span className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full mt-1 inline-block ${
                                        trx.status === 'Thành công' 
                                            ? 'bg-emerald-500/10 text-emerald-400' 
                                            : 'bg-rose-500/10 text-rose-400'
                                    }`}>
                                        {trx.status}
                                    </span>
                                </div>
                            </div>
                        ))}
                    </div>
                    
                    <button className="w-full mt-4 py-2.5 text-sm font-medium text-amber-500 hover:text-amber-400 hover:bg-amber-500/10 rounded-xl transition-colors">
                        Xem tất cả giao dịch
                    </button>
                </div>
            </div>
        </div>
    );
}
