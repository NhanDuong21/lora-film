const COUNTRY_LABELS = {
  VN: 'Việt Nam',
  JP: 'Nhật Bản',
  US: 'Hoa Kỳ',
  KR: 'Hàn Quốc',
  CN: 'Trung Quốc',
  TW: 'Đài Loan',
  HK: 'Hồng Kông',
  TH: 'Thái Lan',
  FR: 'Pháp',
  GB: 'Vương quốc Anh',
  UK: 'Vương quốc Anh',
  CA: 'Canada',
  AU: 'Úc',
  IN: 'Ấn Độ'
};

const LANGUAGE_LABELS = {
  VI: 'Tiếng Việt',
  EN: 'Tiếng Anh',
  JA: 'Tiếng Nhật',
  JP: 'Tiếng Nhật',
  KO: 'Tiếng Hàn',
  KR: 'Tiếng Hàn',
  ZH: 'Tiếng Trung',
  CN: 'Tiếng Trung',
  TH: 'Tiếng Thái',
  FR: 'Tiếng Pháp',
  DE: 'Tiếng Đức',
  ES: 'Tiếng Tây Ban Nha',
  IT: 'Tiếng Ý'
};

const SCREEN_TYPE_LABELS = {
  STANDARD: 'Tiêu chuẩn',
  IMAX: 'IMAX',
  FOUR_DX: '4DX',
  '4DX': '4DX',
  PREMIUM: 'Premium'
};

const SOUND_TYPE_LABELS = {
  STANDARD: 'Âm thanh tiêu chuẩn',
  DOLBY_ATMOS: 'Dolby Atmos'
};

const normalizeCode = value => String(value || '').trim().toUpperCase();

export const formatCountryLabel = value => {
  if (!value) return 'Đang cập nhật';
  return COUNTRY_LABELS[normalizeCode(value)] || value;
};

export const formatLanguageLabel = value => {
  if (!value) return 'Đang cập nhật';
  return LANGUAGE_LABELS[normalizeCode(value)] || value;
};

export const formatCityLabel = value => {
  const normalized = String(value || '').trim();
  if (!normalized) return 'Khu vực khác';
  if (/^(ho chi minh city|hồ chí minh|tp\.?\s*hcm)$/i.test(normalized)) return 'TP. Hồ Chí Minh';
  if (/^(hanoi|ha noi|hà nội)$/i.test(normalized)) return 'Hà Nội';
  return normalized;
};

export const formatGenreLabels = genres => (genres || [])
  .map(genre => typeof genre === 'string' ? genre : genre?.genreName || genre?.name)
  .filter(Boolean)
  .map(genre => genre.replace(/^Phim\s+/i, '').trim());

export const formatScreenTypeLabel = value => SCREEN_TYPE_LABELS[normalizeCode(value)] || value || null;

export const formatSoundTypeLabel = value => SOUND_TYPE_LABELS[normalizeCode(value)] || value || null;

export const formatAuditoriumLabel = value => {
  if (!value) return 'Phòng chiếu';
  return value
    .replace(/^Screen\s*/i, 'Phòng ')
    .replace(/\s*-\s*Standard\b/gi, ' · Tiêu chuẩn')
    .replace(/\s*-\s*Premium\b/gi, ' · Premium');
};

export const formatVersionLabel = version => {
  const format = version?.format || version?.versionName?.match(/\b(2D|3D|IMAX|4DX)\b/i)?.[1];
  const subtitle = version?.subtitleLanguage
    ? `Phụ đề ${formatLanguageLabel(version.subtitleLanguage).replace(/^Tiếng\s+/i, '')}`
    : null;
  const dubbed = version?.dubLanguage
    ? `Lồng tiếng ${formatLanguageLabel(version.dubLanguage).replace(/^Tiếng\s+/i, '')}`
    : null;
  const audio = version?.audioLanguage && !dubbed
    ? formatLanguageLabel(version.audioLanguage)
    : null;
  return [format, audio, dubbed || subtitle].filter(Boolean).join(' · ') || 'Phiên bản tiêu chuẩn';
};

export const actorPresentation = actor => {
  const rawCharacter = String(actor?.characterName || '').trim();
  const isVoice = /\((voice|lồng tiếng)\)/i.test(rawCharacter);
  return {
    name: actor?.fullName || 'Đang cập nhật',
    character: rawCharacter.replace(/\s*\((voice|lồng tiếng)\)\s*/gi, '').trim(),
    role: isVoice ? 'Lồng tiếng' : 'Diễn viên'
  };
};

export const getShowtimeSalesState = (showtime, now = Date.now()) => {
  const startTime = Date.parse(showtime?.startTime);
  const currentTime = now instanceof Date ? now.getTime() : Number(now);
  if (Number.isFinite(startTime) && Number.isFinite(currentTime) && startTime <= currentTime) {
    return { label: 'Đã qua giờ bán', disabled: true, tone: 'muted' };
  }

  const rawStatus = normalizeCode(
    showtime?.availabilityStatus || showtime?.salesStatus || showtime?.status
  );
  const remainingSeats = Number(showtime?.remainingSeats);
  const totalSeats = Number(showtime?.totalSeats || showtime?.capacity);

  if (rawStatus === 'SOLD_OUT' || rawStatus === 'FULL' || remainingSeats === 0) {
    return { label: 'Hết vé', disabled: true, tone: 'danger' };
  }
  if (['SUSPENDED', 'PAUSED', 'CANCELLED', 'CLOSED'].includes(rawStatus)) {
    return { label: 'Tạm ngưng', disabled: true, tone: 'danger' };
  }
  if (rawStatus && !['OPEN_FOR_BOOKING', 'LOW_SEATS', 'ALMOST_FULL'].includes(rawStatus)) {
    return { label: 'Chưa mở bán', disabled: true, tone: 'muted' };
  }

  const isLowSeats = ['LOW_SEATS', 'ALMOST_FULL'].includes(rawStatus)
    || (Number.isFinite(remainingSeats) && remainingSeats > 0 && remainingSeats <= 10)
    || (Number.isFinite(remainingSeats) && Number.isFinite(totalSeats) && totalSeats > 0
      && remainingSeats / totalSeats <= 0.15);
  if (isLowSeats) return { label: 'Sắp hết ghế', disabled: false, tone: 'warning' };
  return { label: 'Đang mở bán', disabled: false, tone: 'success' };
};
