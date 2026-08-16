import { describe, expect, it } from 'vitest';
import {
  actorPresentation,
  formatAuditoriumLabel,
  formatCityLabel,
  formatCountryLabel,
  formatGenreLabels,
  formatLanguageLabel,
  formatSoundTypeLabel,
  formatVersionLabel,
  getShowtimeSalesState
} from './movieDetailPresentation';

describe('movie detail customer presentation', () => {
  it('localizes raw catalog and auditorium codes', () => {
    expect(formatCountryLabel('JP')).toBe('Nhật Bản');
    expect(formatLanguageLabel('EN')).toBe('Tiếng Anh');
    expect(formatCityLabel('Ho Chi Minh City')).toBe('TP. Hồ Chí Minh');
    expect(formatAuditoriumLabel('Screen 01 - Standard')).toBe('Phòng 01 · Tiêu chuẩn');
    expect(formatSoundTypeLabel('DOLBY_ATMOS')).toBe('Dolby Atmos');
    expect(formatGenreLabels(['Phim Hành Động', 'Phim Hài'])).toEqual(['Hành Động', 'Hài']);
    expect(formatVersionLabel({ format: '2D', audioLanguage: 'EN', subtitleLanguage: 'VI' }))
      .toBe('2D · Tiếng Anh · Phụ đề Việt');
  });

  it('turns voice credits into a Vietnamese customer label', () => {
    expect(actorPresentation({ fullName: 'Jun Fukuyama', characterName: 'Koro-sensei (voice)' }))
      .toEqual({ name: 'Jun Fukuyama', character: 'Koro-sensei', role: 'Lồng tiếng' });
  });

  it('maps sales states to the matching UI behavior', () => {
    const future = { startTime: '2099-01-01T09:00:00Z' };
    expect(getShowtimeSalesState({ ...future, status: 'OPEN_FOR_BOOKING' }).label).toBe('Đang mở bán');
    expect(getShowtimeSalesState({ ...future, status: 'LOW_SEATS' }).label).toBe('Sắp hết ghế');
    expect(getShowtimeSalesState({ ...future, status: 'SOLD_OUT' })).toMatchObject({ label: 'Hết vé', disabled: true });
    expect(getShowtimeSalesState({ ...future, status: 'SUSPENDED' })).toMatchObject({ label: 'Tạm ngưng', disabled: true });
    expect(getShowtimeSalesState({ ...future, status: 'DRAFT' })).toMatchObject({ label: 'Chưa mở bán', disabled: true });
    expect(getShowtimeSalesState({ startTime: '2020-01-01T09:00:00Z', status: 'OPEN_FOR_BOOKING' }))
      .toMatchObject({ label: 'Đã qua giờ bán', disabled: true });
  });
});
