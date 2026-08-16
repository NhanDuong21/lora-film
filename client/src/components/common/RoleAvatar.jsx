import { useState } from 'react';
import { getSignedInUserFallbackAvatar } from '@/features/internal-staff/admin/components/avatarUtils';
import { getOptimizedImageUrl } from '@/utils/imageOptimization';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '';

const resolveMediaUrl = value => (
  value?.startsWith('/') ? `${apiBaseUrl}${value}` : value
);

export default function RoleAvatar({
  user,
  alt,
  fallbackAlt,
  className = 'h-10 w-10',
}) {
  const [failedSource, setFailedSource] = useState('');
  const displayName = user?.fullName || user?.name || user?.email || 'tài khoản';
  const uploadedAvatarUrl = getOptimizedImageUrl(resolveMediaUrl(user?.avatarUrl), {
    width: 256,
    height: 256,
    quality: 90,
    gravity: 'face',
  });
  const fallbackAvatarUrl = getSignedInUserFallbackAvatar(user);
  const canUseUploadedAvatar = Boolean(uploadedAvatarUrl) && failedSource !== uploadedAvatarUrl;
  const canUseFallbackAvatar = Boolean(fallbackAvatarUrl) && failedSource !== fallbackAvatarUrl;
  const source = canUseUploadedAvatar
    ? uploadedAvatarUrl
    : (canUseFallbackAvatar ? fallbackAvatarUrl : '');

  if (source) {
    const isFallback = source === fallbackAvatarUrl;
    return (
      <img
        src={source}
        alt={isFallback
          ? (fallbackAlt || `Ảnh mặc định theo vai trò của ${displayName}`)
          : (alt || `Ảnh đại diện của ${displayName}`)}
        className={`${className} shrink-0 rounded-full object-cover`}
        onError={() => setFailedSource(source)}
      />
    );
  }

  return (
    <span className={`${className} grid shrink-0 place-items-center rounded-full bg-zinc-800 font-black text-brand-orange`}>
      {displayName.slice(0, 2).toUpperCase()}
    </span>
  );
}
