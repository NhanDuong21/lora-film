import SystemUpdating from '../../components/common/SystemUpdating';

export default function SeatSelectionView() {
  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-[400px] bg-zinc-950 text-white">
      <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white mb-6">Chọn Ghế</h1>
      <SystemUpdating />
    </div>
  );
}
