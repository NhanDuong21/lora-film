# Business decisions and adopted defaults

| Topic | Adopted implementation default |
|---|---|
| STAFF v1 | Cash collection only |
| Canonical role | `STAFF`, not a seed-role rewrite to `EMPLOYEE` |
| Accessible seats | Bookable and priced equal to STANDARD |
| Cancelled tickets | Retained for audit but visibly void; no QR/admission instruction |
| TMDB | Optional; never blocks saved movie operations |
| Mock payment | Explicit development feature flag only |
| Unsupported marketing | Remove or hide |
| Customer booking routes | Authentication guard plus return-to URL |
| Database | No schema change without new evidence and approval |
