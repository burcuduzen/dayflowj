# User-visible system status

Performance and operation logs are available to every DayFlowJ user through the main toolbar **Sistem durumu** button and desktop **Sistem > Performans ve işlem logları** menu. No developer environment flag or administrator account is required. In Electron the page opens in a separate protected window; in browser mode it opens at `/diagnostics.html`.

Normal desktop-token, origin, sandbox and IPC sender checks still apply. The renderer has no Node integration. The preload exposes read-only metrics and an operation that opens the system-status window.

## Metrics and interpretation

- Java CPU is sampled from process CPU-time deltas every five seconds and divided by logical CPU count. The first sample is unavailable, not zero.
- Java heap used/max, non-heap memory, uptime and thread count come from JVM management beans. Heap is not total process RAM.
- Electron CPU values are displayed exactly as supplied by Electron for each process. They are labelled separately from the normalized Java CPU percentage; the percentages are not added together.
- Electron working set uses KB-to-byte conversion. Its sum can double count shared pages, so it is labelled as a working-set sum, not exact total application RAM.
- Windows Java working set is obtained by querying only the owned Java PID. Unsupported or failed queries show unavailable. No other app is inspected.
- Backend request average/P95 use the latest 100 application API requests. Counts/errors are for the current backend session. Diagnostic polling itself is excluded. History keeps at most 60 samples (about five minutes).
- Sampling and UI refresh are bounded. Java samples while the backend is running. UI polling pauses while its system-status window is hidden or explicitly paused. Electron sampling is requested only by this window and cached to prevent overlapping subprocess queries.

## Operation logs

Log records contain timestamp, level, generic operation category, HTTP result and duration. Successful API reads update latency counters without filling the activity history. Mutations, failed requests, lifecycle events, Gmail import summaries, and mail reminder success/failure are recorded. No request body, query string, user title, mailbox content, account email, OAuth/SMTP credential or provider error message is logged by this feature.

The latest 300 entries are filterable in the system-status window and can be exported as TSV. Files live under `DAYFLOW_DATA_DIR/diagnostics` for desktop mode, or `backend/data/diagnostics` when started from the backend directory. `activity.tsv` rotates after approximately 512 KiB to `activity.previous.tsv`; only the current and previous files are retained. Older entries are not permanent audit records. Restart restores bounded log history but resets performance counters. Storage failure leaves the main operation working and marks log persistence unavailable.

Existing `desktop.log` is the raw backend process log and is separate from the sanitized activity log. This feature does not export that file or alter its existing retention policy.

## Validation

- `node --test desktop/tests/diagnostics.test.cjs`: memory units and unavailable values.
- `./mvnw test` inside backend: log capacity/filtering/restart/storage failure, normal-mode accessible page/API, and enabled-mode sanitized logs.
- Standalone Java store checks cover average/P95, bounded history, persistence, rotation and storage failure.
- Full Maven tests and actual Windows Electron runtime must pass before distributing a rebuilt installer. The authoring environment could not reach Maven Central; no live Windows runtime validation was possible there.

Reference: https://www.electronjs.org/docs/latest/api/app#appgetappmetrics
