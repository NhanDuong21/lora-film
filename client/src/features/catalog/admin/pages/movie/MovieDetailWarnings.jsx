import { AlertTriangle, HelpCircle } from 'lucide-react';
import { getMovieReadinessView } from '../../utils/movieReadiness';

export default function MovieDetailWarnings({ movie }) {
  if (!movie) return null;

  const readiness = getMovieReadinessView(movie);

  if (readiness.healthStatus === 'UNKNOWN') {
    return (
      <div className="bg-zinc-500/10 border border-zinc-500/20 rounded-xl p-4">
        <p className="text-zinc-400 text-sm flex items-center gap-2">
          <HelpCircle size={16} />
          Chưa xác định trạng thái sẵn sàng từ máy chủ.
        </p>
      </div>
    );
  }

  if (readiness.issues.length === 0) return null;

  const isBlocked = readiness.healthStatus === 'BLOCKED';

  return (
    <div className={`${isBlocked ? 'bg-red-500/10 border-red-500/20' : 'bg-amber-500/10 border-amber-500/20'} border rounded-xl p-4 space-y-2`}>
      <h3 className={`${isBlocked ? 'text-red-500' : 'text-amber-500'} text-sm font-semibold flex items-center gap-2`}>
        <AlertTriangle size={16} />
        {isBlocked ? 'Điều kiện đang chặn phim' : 'Thông tin cần kiểm tra'}
      </h3>
      <ul className={`list-disc list-inside text-sm ${isBlocked ? 'text-red-500/80' : 'text-amber-500/80'} space-y-1 ml-1`}>
        {readiness.issues.map((issue, i) => (
          <li key={`${issue.code || 'issue'}-${i}`}>{issue.message || issue.code}</li>
        ))}
      </ul>
    </div>
  );
}
