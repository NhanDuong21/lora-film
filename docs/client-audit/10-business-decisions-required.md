# Business decisions and adopted defaults

| Topic | Adopted implementation default |
|---|---|
| Employee workspace | Dashboard, schedule/leave, attendance, payroll and cash collection are permission-controlled |
| Canonical workforce role | `EMPLOYEE`; migrate and retire `STAFF` |
| Accessible seats | Bookable and priced equal to STANDARD |
| Cancelled tickets | Retained for audit but visibly void; no QR/admission instruction |
| TMDB | Optional; never blocks saved movie operations |
| Mock payment | Explicit development feature flag only |
| Unsupported marketing | Remove or hide |
| Customer booking routes | Authentication guard plus return-to URL |
| Database | Apply `20260808_unify_employee_role_permissions.sql` before the matching services |
