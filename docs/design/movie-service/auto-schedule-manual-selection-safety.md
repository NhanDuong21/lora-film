# Auto-Schedule Manual Selection Safety

## Scope and API

`PUT /api/admin/showtime-schedules/{previewPublicId}/items` keeps its existing request and response shapes. Requests carry a non-negative `expectedVersion` and between 1 and 10,000 unique item updates. Updates are partial: unmentioned items retain their persisted selected state.

The service builds the complete proposed final selected set before writing anything. It begins with every selected item in the preview and applies all requested selections and deselections in memory. Duplicate, missing, foreign, ineligible, malformed, or conflicting requests leave item flags, selection metadata, counters, version, and timestamps unchanged.

## Canonical occupancy invariant

Manual final-state validation and apply-time candidate-to-candidate validation share one pure backend algorithm:

- partition only by auditorium identity;
- compare half-open `[startTime, occupancyEndTime)` intervals;
- allow exact adjacency;
- sort by start, occupancy end, and stable item public ID;
- retain the maximum prior occupancy end and its supplying interval;
- reject when the next start is earlier than that maximum.

Tracking the maximum prior end detects nested intervals that an adjacent-pair scan can miss. The algorithm is independent of cinema, `serviceDate`, preview filters, ranking, score, and frontend state. Cross-midnight and cross-service-date occupancy therefore conflicts whenever the absolute instants overlap in one auditorium. Its complexity is `O(N log N)` time and `O(N)` auxiliary memory, and it does not mutate its input.

## Eligibility and legacy remediation

A requested new selection must be `VALID`, `PENDING`, have an auditorium, and satisfy `startTime < endTime <= occupancyEndTime`. A `REJECTED` selection retains its existing error. Occupancy is never fabricated from film end.

Any item may be deselected, including rejected, applied, or malformed legacy data. A retained malformed selected item blocks additions in its auditorium; one without auditorium identity blocks every addition. Well-formed retained selections participate in overlap validation regardless of validation or apply status. Null legacy `serviceDate` is permitted and is not an overlap partition.

## Atomicity and concurrency

Manual selection runs at `READ_COMMITTED` after the existing separate expiration normalization. Read-only scalar snapshots are validated first. A changed request then conditionally updates the `PREVIEWED` preview row at the supplied version, sets the final selected-valid count, increments the version exactly once, and updates its timestamp. A failed compare-and-set returns `AUTO_SCHEDULE_PREVIEW_VERSION_CONFLICT`.

After that guard, at most two bulk item updates apply selected and deselected changes with the same actor and timestamp. A row-count mismatch fails with inconsistent-preview semantics; the transaction rolls back the preview and item writes together. A validated no-op performs no compare-and-set or bulk update, so its version and timestamps remain unchanged. The final summary is reloaded with cinema data without initializing the preview item collection.

## Error contract

- `AUTO_SCHEDULE_SELECTION_OVERLAP` (409): the manual proposed final set overlaps.
- `AUTO_SCHEDULE_INVALID_ITEM_SELECTION` (409): a requested selection is malformed, non-pending, otherwise ineligible, or unsafe beside retained malformed data.
- `AUTO_SCHEDULE_SELECTED_ITEMS_OVERLAP` (409): apply-time selected candidates overlap.

Existing lifecycle, version, duplicate, item-not-found, ownership, and rejected-item errors remain unchanged. The standard safe response envelope is retained.

Frontend interval checks remain assistance. On a backend selection rejection the client maps the focused error and reloads every preview page to restore authoritative selected IDs, count, and version. Apply continues to perform release-window, operating-hour, closure, maintenance, existing-Showtime, duration, and cleaning-buffer validation under its existing API, lifecycle, locking, idempotency, pricing, and Showtime-creation flow.
