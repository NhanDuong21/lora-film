# Employee operational runbook

The canonical workforce role is `EMPLOYEE`. The employee workspace is assembled from permissions in the access token.

1. Apply `20260808_unify_employee_role_permissions.sql` to `auth_db`.
2. Login again with an `EMPLOYEE` account so the new permission claims are issued.
3. `/employee` redirects to the first permitted screen, preferring the dashboard.
4. Verify that the menu contains only functions granted to the `EMPLOYEE` role.
5. For cash collection, grant `PAYMENT_CASH_COLLECT`, enter a pending booking code and complete the collect flow.

Permission changes on the built-in `EMPLOYEE` role apply to every account assigned that role. Updating the role revokes their current credentials; users must authenticate again to receive the new claims.
