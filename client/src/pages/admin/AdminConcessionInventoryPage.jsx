import SystemUpdating from '../../components/common/SystemUpdating';

const AdminConcessionInventoryPage = () => {
  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-[400px] bg-zinc-950 text-white space-y-6">
      <div className="flex flex-col border-b border-zinc-800 pb-4">
        <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">DANH MỤC BẮP NƯỚC</h1>
      </div>
      <SystemUpdating/>
    </div>
  );
};

export default AdminConcessionInventoryPage;
