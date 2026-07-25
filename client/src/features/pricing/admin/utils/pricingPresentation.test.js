import { describe, expect, it } from 'vitest';
import {
  getConflictPresentation,
  getPricingReasonPresentation,
  getRuleScopePresentation,
} from './pricingPresentation';

describe('pricing presentation', () => {
  it('localizes known pricing reasons and reserves unknown for a true fallback', () => {
    expect(getPricingReasonPresentation('PRICING_INCOMPLETE').label)
      .toBe('Thiếu chính sách hoặc quy tắc giá hiệu lực');
    expect(getPricingReasonPresentation('PRICING_AMBIGUOUS').label)
      .toBe('Có nhiều quy tắc giá cùng mức ưu tiên');
    expect(getPricingReasonPresentation('NOT_A_REAL_CODE').label)
      .toBe('Không xác định — xem chi tiết kỹ thuật');
  });

  it('presents structured overlap facts without using UUIDs as the primary message', () => {
    const presentation = getConflictPresentation({
      reasonCode: 'PRICE_POLICY_OVERLAP',
      firstRuleId: 'rule-uuid-1',
      secondRuleId: 'rule-uuid-2',
      seatTypeName: 'Ghế VIP',
      seatTypeCode: 'VIP',
      scope: 'AUDITORIUM',
      auditoriumName: 'Phòng 3',
      dayType: 'WEEKEND',
      timeBandStart: '18:00',
      timeBandEnd: '23:00',
      conflictingRuleCount: 2,
    });

    expect(presentation.title).toBe('Ghế VIP (VIP) · Phòng chiếu cụ thể: Phòng 3');
    expect(presentation.facts).toBe('Cuối tuần · 18:00–23:00 · 2 quy tắc xung đột');
    expect(presentation.title).not.toContain('rule-uuid');
    expect(presentation.technical.ruleIds).toEqual(['rule-uuid-1', 'rule-uuid-2']);
  });

  it('uses auditorium over screen type when presenting a rule scope', () => {
    expect(getRuleScopePresentation({
      auditoriumId: 'room-1',
      auditoriumName: 'Phòng 1',
      screenType: 'IMAX',
    })).toEqual({
      code: 'AUDITORIUM',
      label: 'Phòng chiếu cụ thể',
      detail: 'Phòng 1',
    });
  });
});
