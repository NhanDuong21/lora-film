import { useState, useEffect } from 'react';
import adminGenreService from '@/features/catalog/admin/services/adminGenreService';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import { useOutletContext } from 'react-router-dom';
import { AsyncState } from '@/components/common/ui/uiKit';
import { Loader2, Save } from 'lucide-react';
import { parseApiError } from '@/utils/apiErrorHandler';

export default function MovieGenreTab({ movie, onUpdate }) {
  const { triggerToast } = useOutletContext();
  const [genres, setGenres] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    const fetchGenres = async () => {
      try {
        const res = await adminGenreService.getAllGenres();
        if (res?.success) {
          // Normalizing page response if it comes wrapped in content/data
          const data = res.data?.content || res.data?.data || res.data || [];
          setGenres(Array.isArray(data) ? data : []);
        } else {
          setError('Không thể tải danh sách thể loại.');
        }
      } catch (err) {
        setError(parseApiError(err));
      } finally {
        setIsLoading(false);
      }
    };
    fetchGenres();
  }, []);

  useEffect(() => {
    if (movie?.genres) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setSelectedIds(movie.genres.map(g => g.publicId));
    }
  }, [movie]);

  const toggleGenre = (id) => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
      setSelectedIds(prev => 
      prev.includes(id) ? prev.filter(gid => gid !== id) : [...prev, id]
    );
  };

  const handleSave = async () => {
    setIsSaving(true);
    try {
      const res = await adminMovieService.assignGenres(movie.publicId, selectedIds);
      if (res?.success) {
        triggerToast('Cập nhật thể loại thành công');
        onUpdate?.();
      } else {
        triggerToast('Cập nhật thất bại', 'error');
      }
    } catch (err) {
      triggerToast(parseApiError(err), 'error');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <AsyncState isLoading={isLoading} error={error} onRetry={() => window.location.reload()}>
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-zinc-100">Thể loại phim</h3>
          <button
            onClick={handleSave}
            disabled={isSaving}
            className="flex items-center gap-2 bg-brand-orange text-white px-4 py-2 rounded-lg text-xs font-semibold hover:bg-brand-orange/90 transition-colors disabled:opacity-50"
          >
            {isSaving ? <Loader2 size={14} className="animate-spin" /> : <Save size={14} />}
            Lưu thay đổi
          </button>
        </div>

        <div className="flex flex-wrap gap-3">
          {genres.map(g => {
            const isSelected = selectedIds.includes(g.publicId);
            return (
              <label 
                key={g.publicId}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl cursor-pointer border transition-colors text-sm ${
                  isSelected 
                    ? 'border-brand-orange bg-brand-orange/10 text-brand-orange' 
                    : 'border-zinc-800 bg-zinc-900/50 text-zinc-400 hover:text-zinc-200'
                }`}
              >
                <input
                  type="checkbox"
                  className="hidden"
                  checked={isSelected}
                  onChange={() => toggleGenre(g.publicId)}
                />
                {g.name}
              </label>
            );
          })}
        </div>
      </AsyncState>
    </div>
  );
}
