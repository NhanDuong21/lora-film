import React from 'react';
import { Search, MapPin, Trash2, Plus, Phone } from 'lucide-react';
import SkeletonTable from '@/components/common/SkeletonTable';

export default function CinemaTable({
  cinemas,
  isLoading,
  searchTerm,
  setSearchTerm,
  cityFilter,
  setCityFilter,
  statusFilter,
  setStatusFilter,
  currentPage,
  setCurrentPage,
  pageSize,
  totalPages,
  totalElements,
  citiesList,
  onDelete,
  onStatusChange,
  onOpenCreate
}) {
  const handleSearch = (e) => {
    setSearchTerm(e.target.value);
    setCurrentPage(0);
  };

  const handleCityFilter = (e) => {
    setCityFilter(e.target.value);
    setCurrentPage(0);
  };

  const handleStatusFilter = (e) => {
    setStatusFilter(e.target.value);
    setCurrentPage(0);
  };

  const renderStatusBadge = (status) => {
    const config = {
      DRAFT: 'bg-zinc-800 text-zinc-400 border-zinc-700',
      ACTIVE: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
      MAINTENANCE: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
      TEMPORARILY_CLOSED: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
      INACTIVE: 'bg-zinc-800 text-zinc-500 border-zinc-700',
      PERMANENTLY_CLOSED: 'bg-red-500/10 text-red-500 border-red-500/20'
    };

    return (
      <span className={`px-2.5 py-1 text-[10px] font-black border rounded-full uppercase tracking-wider ${config[status] || config.DRAFT}`}>
        {status.replace('_', ' ')}
      </span>
    );
  };

  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-[400px] bg-zinc-950 text-white space-y-6 animate-fade-in">
      {/* Title Header */}
      <div className="flex flex-col border-b border-zinc-800 pb-4">
        <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">HỆ THỐNG CỤM RẠP</h1>
      </div>

      {/* Filter and search bar */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 bg-zinc-900/60 border border-zinc-850 p-4 rounded-2xl backdrop-blur-md">
        {/* Search by Keyword */}
        <div className="relative md:col-span-2">
          <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-zinc-500">
            <Search className="w-4 h-4" />
          </span>
          <input
            type="text"
            value={searchTerm}
            onChange={handleSearch}
            placeholder="Tìm tên rạp, địa chỉ..."
            className="w-full bg-zinc-950 border border-zinc-800 text-zinc-100 placeholder-zinc-500 focus:border-brand-orange/40 focus:ring-0 rounded-xl py-2.5 pl-9 pr-4 text-xs transition-colors"
          />
        </div>

        {/* City Filter */}
        <div>
          <select
            value={cityFilter}
            onChange={handleCityFilter}
            className="w-full bg-zinc-950 border border-zinc-800 text-zinc-300 focus:border-brand-orange/40 rounded-xl py-2.5 px-3.5 text-xs transition-colors cursor-pointer focus:outline-none"
          >
            <option value="">Tất cả thành phố</option>
            {citiesList.map(city => (
              <option key={city} value={city}>{city}</option>
            ))}
          </select>
        </div>

        {/* Status Filter */}
        <div>
          <select
            value={statusFilter}
            onChange={handleStatusFilter}
            className="w-full bg-zinc-950 border border-zinc-800 text-zinc-300 focus:border-brand-orange/40 rounded-xl py-2.5 px-3.5 text-xs transition-colors cursor-pointer focus:outline-none"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="DRAFT">DRAFT</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="MAINTENANCE">MAINTENANCE</option>
            <option value="TEMPORARILY_CLOSED">TEMPORARILY CLOSED</option>
            <option value="INACTIVE">INACTIVE</option>
            <option value="PERMANENTLY_CLOSED">PERMANENTLY CLOSED</option>
          </select>
        </div>
      </div>

      {/* Button add cinema */}
      <div className="flex justify-end">
        <button
          onClick={onOpenCreate}
          className="bg-brand-orange hover:bg-opacity-90 text-zinc-950 font-black px-5 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-all duration-300 shadow-lg shadow-brand-orange/10 flex items-center gap-2 cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          <span>THÊM CỤM RẠP</span>
        </button>
      </div>

      {/* Data Table */}
      {isLoading ? (
        <SkeletonTable rows={5} columns={6} />
      ) : (
        <div className="bg-zinc-950 border border-zinc-900 rounded-2xl overflow-hidden w-full shadow-2xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse whitespace-nowrap">
              <thead>
                <tr className="bg-zinc-900/40 border-b border-zinc-900 text-[10px] font-black text-zinc-400 uppercase tracking-wider">
                  <th className="py-4 px-6 w-16 text-center">STT</th>
                  <th className="py-4 px-6">TÊN CỤM RẠP</th>
                  <th className="py-4 px-6">ĐỊA CHỈ</th>
                  <th className="py-4 px-6">HOTLINE</th>
                  <th className="py-4 px-6 w-44">TRẠNG THÁI</th>
                  <th className="py-4 px-6 w-24 text-right">THAO TÁC</th>
                </tr>
              </thead>
              <tbody>
                {cinemas.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="py-16 text-center text-zinc-500 text-sm font-semibold">
                      <div className="flex flex-col items-center justify-center gap-2">
                        <MapPin className="w-8 h-8 text-zinc-800" />
                        <span>Không tìm thấy cụm rạp nào.</span>
                      </div>
                    </td>
                  </tr>
                ) : (
                  cinemas.map((cinema, index) => (
                    <tr key={cinema.publicId} className="border-b border-zinc-900/60 hover:bg-zinc-900/30 transition-colors group">
                      <td className="py-4 px-6 text-center">
                        <span className="text-xs font-black text-zinc-500">
                          {((currentPage * pageSize) + index + 1).toString().padStart(2, '0')}
                        </span>
                      </td>
                      <td className="py-4 px-6">
                        <div className="flex flex-col gap-0.5">
                          <span className="text-sm font-bold text-zinc-200 group-hover:text-amber-400 transition-colors">
                            {cinema.name}
                          </span>
                          <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider">
                            {cinema.city} {cinema.district ? `- ${cinema.district}` : ''}
                          </span>
                        </div>
                      </td>
                      <td className="py-4 px-6">
                        <span className="text-xs text-zinc-400 font-medium block max-w-sm overflow-hidden text-ellipsis">
                          {cinema.address}
                        </span>
                      </td>
                      <td className="py-4 px-6">
                        <span className="text-xs font-semibold text-zinc-300">
                          {cinema.hotline || '—'}
                        </span>
                      </td>
                      <td className="py-4 px-6">
                        <div className="flex items-center gap-3">
                          {renderStatusBadge(cinema.status)}
                          
                          {/* Quick change dropdown */}
                          <select
                            value={cinema.status}
                            onChange={(e) => onStatusChange(cinema.publicId, e.target.value)}
                            className="bg-zinc-900 border border-zinc-800 text-[10px] text-zinc-400 font-black rounded-lg py-1 px-1.5 focus:outline-none focus:border-brand-orange/40 cursor-pointer hover:text-white transition-colors"
                          >
                            <option value="DRAFT">DRAFT</option>
                            <option value="ACTIVE">ACTIVE</option>
                            <option value="MAINTENANCE">MAINTENANCE</option>
                            <option value="TEMPORARILY_CLOSED">TEMPORARILY CLOSED</option>
                            <option value="INACTIVE">INACTIVE</option>
                            <option value="PERMANENTLY_CLOSED">PERMANENTLY CLOSED</option>
                          </select>
                        </div>
                      </td>
                      <td className="py-4 px-6 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => onDelete(cinema.publicId, cinema.name)}
                            className="p-2 text-zinc-500 hover:text-red-500 hover:bg-red-500/10 border border-transparent hover:border-red-500/20 rounded-lg transition-all cursor-pointer"
                            title="Xóa cụm rạp"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination Footer */}
          {totalPages > 1 && (
            <div className="flex justify-between items-center px-6 py-4 bg-zinc-900/20 border-t border-zinc-900">
              <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider">
                Hiển thị {cinemas.length} / {totalElements} cụm rạp
              </span>
              <div className="flex gap-2">
                <button
                  disabled={currentPage === 0}
                  onClick={() => setCurrentPage(currentPage - 1)}
                  className="px-3 py-1.5 bg-zinc-900 border border-zinc-800 disabled:opacity-50 text-xs text-zinc-300 rounded-lg hover:text-white transition-colors cursor-pointer"
                >
                  Trước
                </button>
                <span className="px-3 py-1.5 text-xs text-zinc-400 font-bold bg-zinc-950 border border-zinc-900 rounded-lg">
                  {currentPage + 1} / {totalPages}
                </span>
                <button
                  disabled={currentPage === totalPages - 1}
                  onClick={() => setCurrentPage(currentPage + 1)}
                  className="px-3 py-1.5 bg-zinc-900 border border-zinc-800 disabled:opacity-50 text-xs text-zinc-300 rounded-lg hover:text-white transition-colors cursor-pointer"
                >
                  Sau
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
