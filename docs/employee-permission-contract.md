# Employee permission contract

`EMPLOYEE` is the only workforce role used by the employee portal. `ADMIN` retains an explicit override for support and verification. A request to an employee API must carry both the `EMPLOYEE` role and its required permission; the frontend mirrors this rule for routing and navigation.

| Capability | Client route | Required permission | Protected backend operations |
|---|---|---|---|
| Employee dashboard | `/employee/dashboard` | `EMPLOYEE_DASHBOARD_VIEW` | Dashboard only calls data APIs separately permitted below |
| Schedule and leave history | `/employee/schedules` | `EMPLOYEE_SCHEDULE_VIEW` | Own shifts and own leave history |
| Create/cancel leave | Schedule action | `EMPLOYEE_LEAVE_CREATE` | Create or cancel an owned leave request |
| Attendance | `/employee/checkin` | `EMPLOYEE_ATTENDANCE_VIEW` and `EMPLOYEE_ATTENDANCE_UPDATE` | Own attendance, check-in and check-out |
| Payroll | `/employee/payroll` | `EMPLOYEE_PAYROLL_VIEW` | Own published payroll records |
| Counter cash collection | `/employee/payments/cash` | `PAYMENT_CASH_COLLECT` | Booking lookup and cash create/collect/cancel |

Permissions are assigned to the `EMPLOYEE` role, so a change affects all employee accounts. `RoleServiceImpl` revokes credentials for accounts using an updated role; users must log in again to receive a JWT with the new permission set.

Deployment order:

1. Apply `docs/database/mysql/migrations/20260808_unify_employee_role_permissions.sql` to `auth_db`.
2. Deploy auth-service so new employee accounts and JWTs use `EMPLOYEE`.
3. Deploy user-service and payment-service so backend authorization uses the permission contract.
4. Deploy the client.
5. Require existing workforce users to authenticate again.
