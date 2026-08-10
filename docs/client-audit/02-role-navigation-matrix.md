# Role and navigation matrix

| Surface | Guest | CUSTOMER | EMPLOYEE | ADMIN |
|---|---|---|---|---|
| Public discovery | Allow | Allow | Allow | Allow |
| Account/profile | Login redirect | Allow owner | Allow own account | Allow own account |
| Customer booking/history/result | Login redirect | Allow owner | Deny | Admin routes only |
| Employee root | Login redirect | Deny | First permitted employee screen | Allow by admin bypass |
| Admin root | Login redirect | Deny | Allow only with an admin-area permission | Allow |
| Cash payment API | Deny | Deny | Require `PAYMENT_CASH_COLLECT` | Allow |
| Admin payment/score | Deny | Deny | Deny | Allow |

Canonical workforce role is `EMPLOYEE`; `STAFF` is retired. Frontend route/menu checks and backend authorization use the same permission codes, while the backend remains authoritative.
