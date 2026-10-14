# Developer-only diagnostics

The normal user interface is unchanged. Performance collection, operation-log writing and `/api/diagnostics/*` endpoints are disabled by default. `/diagnostics.html` returns 404 outside developer mode. This is a developer launch mode, not a new end-user account role.

## Enable for a developer session

In the PowerShell session used to launch DayFlowJ:

```powershell
$env:DAYFLOW_DIAGNOSTICS_ENABLED = 'true'
```

Then launch the desktop executable from that same terminal, or use `pnpm exec electron .` inside `desktop` with dependencies installed and the backend JAR/runtime prepared. Electron shows **Geliştirici > Performans ve işlem logları** only in this mode. This opens a separate window and does not add a button to the user's task/calendar screens. Exit and restart without this environment variable to disable it:

```powershell
Remove-Item Env:DAYFLOW_DIAGNOSTICS_ENABLED -ErrorAction SilentlyContinue
```

For backend-only development, the same variable enables `/diagnostics.html`; Electron measurements remain unavailable in a browser. Packaged desktop requests continue to require the existing private desktop token. The preload exposes one read-only metrics operation, validates the developer window as the IPC sender, and does not enable renderer Node integration.

## Metrics and interpretation

- Java CPU is sampled from process CPU-time deltas every five seconds and divided by logical CPU count. The first sample is unavailable, not zero.
- Java heap used/max, non-heap memory, uptime and thread count come from JVM management beans. Heap is not total process RAM.
- Electron CPU values are displayed exactly as supplied by Electron for each process. They are labelled separately from the normalized Java CPU percentage; the percentages are not added together.
- Electron working set uses KB-to-byte conversion. Its sum can double count shared pages, so it is labelled as a working-set sum, not exact total application RAM.
- Windows Java working set is obtained by querying only the owned Java PID. Unsupported or failed queries show unavailable. No other app is inspected.
- Backend request average/P95 use the latest 100 application API requests. Counts/errors are for the current backend session. Diagnostic polling itself is excluded. History keeps at most 60 samples (about five minutes).
- Sampling and UI refresh are bounded. Java samples only in developer mode. UI polling pauses while its developer window is hidden or explicitly paused. Electron sampling is requested only by this window and cached to prevent overlapping subprocess queries.

## Operation logs

Log records contain timestamp, level, generic operation category, HTTP result and duration. Successful API reads update latency counters without filling the activity history. Mutations, failed requests, lifecycle events, Gmail import summaries, and mail reminder success/failure are recorded. No request body, query string, user title, mailbox content, account email, OAuth/SMTP credential or provider error message is logged by this feature.

The latest 300 entries are filterable in the developer window and can be exported as TSV. Files live under `DAYFLOW_DATA_DIR/diagnostics` for desktop mode, or `backend/data/diagnostics` when started from the backend directory. `activity.tsv` rotates after approximately 512 KiB to `activity.previous.tsv`; only the current and previous files are retained. Older entries are not permanent audit records. Restart restores bounded log history but resets performance counters. Storage failure leaves the main operation working and marks log persistence unavailable.

Existing `desktop.log` is the raw backend process log and is separate from the sanitized activity log. This feature does not export that file or alter its existing retention policy.

## Validation

- `node --test desktop/tests/diagnostics.test.cjs`: memory units and unavailable values.
- `./mvnw test` inside backend: log capacity/filtering/restart/storage failure, normal-mode inaccessible page/API, and enabled-mode sanitized logs.
- Standalone Java store checks cover average/P95, bounded history, persistence, rotation and storage failure.
- Full Maven tests and actual Windows Electron runtime must pass before distributing a rebuilt installer. The authoring environment could not reach Maven Central; no live Windows runtime validation was possible there.

Reference: https://www.electronjs.org/docs/latest/api/app#appgetappmetrics
