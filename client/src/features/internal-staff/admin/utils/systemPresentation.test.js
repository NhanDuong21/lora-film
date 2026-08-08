import { describe, expect, it } from 'vitest';
import {
  getAuditActionLabel,
  getAuditTone,
  getDeviceLabel,
  getPermissionGroupKey,
  getPermissionLabel,
  getRolePresentation,
  getTargetLabel,
  summarizeAuditDetails,
} from './systemPresentation';

describe('system operation presentation', () => {
  it('presents fixed system roles in operational Vietnamese', () => {
    expect(getRolePresentation({ code: 'ADMIN' }).label).toBe('Quản trị hệ thống');
    expect(getRolePresentation({ code: 'MANAGER' }).scope).toBe('Theo rạp được phân công');
    expect(getRolePresentation({ code: 'EMPLOYEE' }).label).toBe('Nhân viên');
    expect(getRolePresentation({ code: 'CUSTOMER' }).label).toBe('Khách hàng');
  });

  it('translates and groups permissions by business task', () => {
    expect(getPermissionLabel({ code: 'EMPLOYEE_ATTENDANCE_UPDATE' })).toBe('Chấm công vào và ra ca');
    expect(getPermissionGroupKey({ code: 'EMPLOYEE_ATTENDANCE_UPDATE' })).toBe('EMPLOYEE_SELF');
    expect(getPermissionGroupKey({ code: 'BOOKING_MANAGE' })).toBe('BOOKING');
    expect(getPermissionGroupKey({ code: 'SYSTEM_CONFIGURATION' })).toBe('SYSTEM');
  });

  it('turns technical audit payloads into readable activity text', () => {
    expect(getAuditActionLabel('LOGIN_FAILED_INVALID_PASSWORD')).toBe('Đăng nhập thất bại do sai mật khẩu');
    expect(getAuditTone('LOGIN_FAILED_INVALID_PASSWORD')).toBe('danger');
    expect(getAuditActionLabel('WORK_SHIFT_CREATED')).toBe('Đã tạo ca làm việc');
    expect(getTargetLabel('WORK_SHIFT', '5')).toBe('Ca làm việc #5');
    expect(summarizeAuditDetails('employeeId=3,batchSize=1')).toBe('Nhân viên: 3 · Số ca: 1');
  });

  it('summarizes browser user agents without exposing the raw string', () => {
    expect(getDeviceLabel('Mozilla/5.0 (Windows NT 10.0) Chrome/120.0')).toBe('Chrome trên Windows');
  });
});
