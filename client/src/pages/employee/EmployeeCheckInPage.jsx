import SystemUpdating from '../../components/common/SystemUpdating';

export default function KiểmTraVéView() {
  return (
    <div className="flex-1 h-full overflow-y-auto p-6 bg-zinc-950 space-y-6">
      {/* Page Header Titles */}
      <div className="flex justify-between items-center pb-2 border-b border-zinc-800">
        <div>
          <h3 className="text-base font-bold text-zinc-50 uppercase tracking-wide">Kiểm Tra Vé</h3>
          <p className="text-xs text-zinc-400 mt-1 uppercase tracking-wide">Quét mã QR và xác thực vé vào cổng</p>
        </div>
      </div>
      
      <SystemUpdating />
    </div>
  );
}
