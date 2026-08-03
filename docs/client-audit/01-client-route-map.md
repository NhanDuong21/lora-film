# Client route map

## Public and customer

- Public discovery: `/`, `/home`, `/movies`, `/movies/:movieId`, `/movie/:movieId`, `/cinema/:id`, `/booking`.
- Auth: `/login`, `/register`, `/verify-otp`, `/forgot-password`, `/reset-password`, `/oauth2/redirect`.
- Protected account: `/profile`, `/change-password`, `/change-email`, `/sessions`, `/seat-selection`, `/promotions`, `/payments/return`.
- Booking: `/bookings/checkout`, `/bookings/success`, `/bookings/failed`, `/bookings`, `/bookings/history`, `/bookings/:bookingId`.
- Loyalty: `/loyalty`.

## Employee

- Root `/employee`; child routes: `pos`, `dashboard`, `checkin`, `schedules`, `payroll`, `payments/cash`.
- Approved demo scope is cash collection only. Other employee routes must not be visible to STAFF until their contracts are complete.

## Admin

- Content: movies, genres and movie operations.
- Facilities: cinemas, rooms, seat types and seat layout/detail routes.
- Scheduling/pricing: showtimes, schedule generation/previews, pricing policies and showtime pricing.
- Commerce: bookings, concessions, concession sales, promotions and payments.
- Loyalty: score viewer, tiers, dashboard, adjustments, reconciliation and audit logs.
- People/security: members, staff, departments, positions, payroll, accounts, roles, permissions and audits.
- Operations: analytics, notifications, templates and notification operations.

Every wildcard route must render the application 404 page. Detail routes use public IDs and must survive refresh/deep linking.
