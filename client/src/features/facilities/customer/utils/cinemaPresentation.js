const WEEKDAY_TO_API_DAY = {
  Mon: 1,
  Tue: 2,
  Wed: 3,
  Thu: 4,
  Fri: 5,
  Sat: 6,
  Sun: 7
};

export const DAY_LABELS = {
  1: 'Thứ Hai',
  2: 'Thứ Ba',
  3: 'Thứ Tư',
  4: 'Thứ Năm',
  5: 'Thứ Sáu',
  6: 'Thứ Bảy',
  7: 'Chủ Nhật'
};

export function getCinemaImages(cinema) {
  const media = Array.isArray(cinema?.gallery) ? cinema.gallery : [];
  return media
    .filter(item =>
      item?.url
      && item.status !== 'INACTIVE'
      && ['BANNER', 'GALLERY'].includes(item.mediaType)
    )
    .sort((left, right) => {
      if (Boolean(left.isPrimary) !== Boolean(right.isPrimary)) {
        return left.isPrimary ? -1 : 1;
      }
      return (left.displayOrder ?? 0) - (right.displayOrder ?? 0);
    });
}

export function formatOperatingHour(hour) {
  if (!hour) return 'Chưa cập nhật';
  if (hour.isClosed) return 'Đóng cửa';
  if (!hour.openTime || !hour.closeTime) return 'Chưa cập nhật';
  return `${hour.openTime} – ${hour.closeTime}`;
}

export function getCurrentOperatingHour(cinema, now = new Date()) {
  const operatingHours = Array.isArray(cinema?.operatingHours) ? cinema.operatingHours : [];
  if (operatingHours.length === 0) return null;

  let dayOfWeek;
  try {
    const weekday = new Intl.DateTimeFormat('en-US', {
      timeZone: cinema?.timezone || 'Asia/Ho_Chi_Minh',
      weekday: 'short'
    }).format(now);
    dayOfWeek = WEEKDAY_TO_API_DAY[weekday];
  } catch {
    const jsDay = now.getDay();
    dayOfWeek = jsDay === 0 ? 7 : jsDay;
  }

  return operatingHours.find(hour => Number(hour.dayOfWeek) === dayOfWeek) || null;
}

export function buildCinemaMap(cinema) {
  const latitude = Number(cinema?.latitude);
  const longitude = Number(cinema?.longitude);
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) return null;

  const delta = 0.008;
  const bbox = [
    longitude - delta,
    latitude - delta,
    longitude + delta,
    latitude + delta
  ].join(',');

  return {
    embedUrl: `https://www.openstreetmap.org/export/embed.html?bbox=${encodeURIComponent(bbox)}&layer=mapnik&marker=${encodeURIComponent(`${latitude},${longitude}`)}`,
    externalUrl: `https://www.openstreetmap.org/?mlat=${encodeURIComponent(latitude)}&mlon=${encodeURIComponent(longitude)}#map=17/${encodeURIComponent(latitude)}/${encodeURIComponent(longitude)}`
  };
}

export function summarizeTicketPrices(layouts) {
  const priceBySeatType = new Map();

  layouts.forEach(layout => {
    const seats = Array.isArray(layout?.seats) ? layout.seats : [];
    seats.forEach(seat => {
      const price = Number(seat?.price);
      if (!seat?.priced || !Number.isFinite(price) || price <= 0) return;

      const code = seat.seatType || 'OTHER';
      const currency = seat.currency || 'VND';
      const key = `${code}:${currency}`;
      const existing = priceBySeatType.get(key);

      if (!existing) {
        priceBySeatType.set(key, {
          code,
          name: seat.seatTypeName || code,
          currency,
          minPrice: price,
          maxPrice: price
        });
        return;
      }

      existing.minPrice = Math.min(existing.minPrice, price);
      existing.maxPrice = Math.max(existing.maxPrice, price);
    });
  });

  return Array.from(priceBySeatType.values()).sort((left, right) => {
    if (left.minPrice !== right.minPrice) return left.minPrice - right.minPrice;
    return left.name.localeCompare(right.name, 'vi');
  });
}

export function formatTicketPrice(priceSummary) {
  const formatter = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: priceSummary.currency || 'VND',
    maximumFractionDigits: 0
  });
  const minimum = formatter.format(priceSummary.minPrice);
  if (priceSummary.minPrice === priceSummary.maxPrice) return minimum;
  return `${minimum} – ${formatter.format(priceSummary.maxPrice)}`;
}
