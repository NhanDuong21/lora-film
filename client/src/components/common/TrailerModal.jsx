import { X } from 'lucide-react';

export default function TrailerModal({ isOpen, onClose, trailerId }) {
  if (!isOpen) return null;

  // Handle backdrop click
  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div
      className="fixed inset-0 bg-black/85 backdrop-blur-md flex items-center justify-center z-50 p-4"
      onClick={handleBackdropClick}
    >
      <div className="relative w-full max-w-4xl bg-zinc-900 rounded-2xl overflow-hidden shadow-2xl border border-zinc-800 aspect-video">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 z-10 p-2 rounded-full bg-black/60 text-white hover:bg-brand-coral hover:text-white transition-all duration-300"
          aria-label="Close trailer"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Video Embed */}
        <iframe
          className="w-full h-full"
          src={`https://www.youtube.com/embed/${trailerId}?autoplay=1`}
          title="Movie Trailer"
          frameBorder="0"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
          allowFullScreen
        ></iframe>
      </div>
    </div>
  );
}
