# Role and navigation matrix

| Surface | Guest | CUSTOMER | STAFF | ADMIN |
|---|---|---|---|---|
| Public discovery | Allow | Allow | Allow | Allow |
| Account/profile | Login redirect | Allow owner | Allow own account | Allow own account |
| Customer booking/history/result | Login redirect | Allow owner | Deny | Admin routes only |
| Employee root | Login redirect | Deny | Cash workflow | Allow by admin bypass |
| Admin root | Login redirect | Deny | Deny | Allow |
| Cash payment API | Deny | Deny | Allow | Allow |
| Admin payment/score | Deny | Deny | Deny | Allow |

Canonical operational role is `STAFF`. Client permission names and backend authorities must not independently redefine this matrix.
