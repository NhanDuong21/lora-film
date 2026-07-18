# Full Movie Service Frontend Implementation Plan

## Branch

`feature/full-movie-service-frontend`

## Strategy

Một issue, một branch, một Merge Request.

Implementation được chia thành nhiều phase nhỏ.
Mỗi phase phải:
- có scope độc lập;
- build/lint trước và sau;
- có commit riêng;
- không nhảy sang phase khác khi phase hiện tại chưa hoàn tất.

## Phase 1 — Shared Frontend Foundation

- [x] Normalize API errors
- [x] Normalize pagination
- [x] Add date/time helpers
- [x] Standardize async UI states
- [x] Standardize confirm/toast helpers
- [x] Add Location API environment config
- [x] Build pass
- [x] Lint pass
- [x] Commit and push

## Phase 2 — Complete Catalog Admin

- [x] Movie CRUD hardening
- [x] Genre management hardening
- [ ] Movie Versions (EXISTING EMBEDDED FLOW ONLY / PARTIAL)
- [ ] Movie Media (EXISTING EMBEDDED FLOW ONLY / PARTIAL)
- [ ] People (EXISTING EMBEDDED FLOW ONLY / PARTIAL)
- [ ] Credits (EXISTING EMBEDDED FLOW ONLY / PARTIAL)
- [ ] Production Companies (EXISTING EMBEDDED FLOW ONLY / PARTIAL)
- [x] Remove obsolete TMDB search mock where appropriate

## Phase 3 — Complete Facilities Admin

- [ ] Cinema CRUD hardening
- [ ] Cinema Media
- [ ] Operating Hours
- [ ] Closure Periods
- [ ] Auditorium maintenance
- [ ] Seat Types
- [ ] Seat Layout hardening

## Phase 4 — Integrate Global Location API

- [ ] Address autocomplete
- [ ] Debounce
- [ ] Abort stale requests
- [ ] Map address data
- [ ] Error and empty states
- [ ] Cinema create/edit integration

## Phase 5 — Manual Showtime Management

- [ ] Resolve Admin list contract
- [ ] List/filter showtimes
- [ ] Create showtime
- [ ] Update showtime
- [ ] Conflict validation
- [ ] Booking window support

## Phase 6 — Showtime Lifecycle and Pricing

- [ ] Status transitions
- [ ] Cancellation
- [ ] Status history
- [ ] Showtime prices

## Phase 7 — Auto Showtime Scheduling

- [ ] Generate preview
- [ ] Preview summary
- [ ] Candidate pagination/filtering
- [ ] Update selections
- [ ] Apply preview
- [ ] Expiry/conflict/error handling

## Phase 8 — Customer Movie/Cinema/Showtime

- [ ] Home
- [ ] Movie discovery
- [ ] Movie detail
- [ ] Cinema detail
- [ ] Showtime discovery
- [ ] Seat layout

## Phase 9 — TMDB Catalog UX

- [ ] TMDB sync status presentation
- [ ] Manual resync by TMDB ID
- [ ] Catalog filter for TMDB movies
- [ ] Remove obsolete direct-search flow
- [ ] Do not implement per-movie approval workflow

## Phase 10 — E2E Hardening

- [ ] Responsive
- [ ] Accessibility
- [ ] Error scenarios
- [ ] Pagination
- [ ] Timezone
- [ ] Build
- [ ] Lint
- [ ] Tests
