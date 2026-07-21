import { AlertTriangle } from 'lucide-react';

export default function MovieDetailWarnings({ movie }) {
  if (!movie) return null;

  const warnings = [];

  // Duration
  if (!movie.durationMinutes || movie.durationMinutes <= 0) {
    warnings.push(`Thời lượng không hợp lệ: ${movie.durationMinutes || 0} phút`);
  } else if (movie.durationMinutes < 30) {
    warnings.push(`Cần kiểm tra thời lượng: ${movie.durationMinutes} phút`);
  }

  // Release Date
  if (!movie.releaseDate) {
    warnings.push('Thiếu ngày khởi chiếu');
  }

  // Poster
  const hasPoster = movie.media?.some(m => m.mediaType === 'POSTER' && m.isPrimary);
  if (!hasPoster) {
    warnings.push('Thiếu poster chính');
  }

  // Versions
  if (!movie.versions || movie.versions.length === 0) {
    warnings.push('Không có phiên bản chiếu nào');
  }

  // Genres
  if (!movie.genres || movie.genres.length === 0) {
    warnings.push('Không có thể loại nào được gán');
  }

  // Synopsis
  if (!movie.synopsis) {
    warnings.push('Thiếu nội dung tóm tắt (synopsis)');
  }

  if (warnings.length === 0) return null;

  return (
    <div className="bg-amber-500/10 border border-amber-500/20 rounded-xl p-4 space-y-2">
      <h3 className="text-amber-500 text-sm font-semibold flex items-center gap-2">
        <AlertTriangle size={16} />
        Thông tin cần kiểm tra
      </h3>
      <ul className="list-disc list-inside text-sm text-amber-500/80 space-y-1 ml-1">
        {warnings.map((w, i) => (
          <li key={i}>{w}</li>
        ))}
      </ul>
    </div>
  );
}
