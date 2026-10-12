export function createStopwatch({ $ }) {
  // Wall-clock elapsed time remains accurate when the browser throttles background tabs.
  const stopwatchKey = 'dayflow-simple-stopwatch-v1';
  let stopwatch = { elapsed: 0, startedAt: null };
  try {
    const saved = JSON.parse(localStorage.getItem(stopwatchKey));
    if (
      saved &&
      Number.isFinite(saved.elapsed) &&
      saved.elapsed >= 0 &&
      (saved.startedAt === null ||
        (Number.isFinite(saved.startedAt) && saved.startedAt > 0))
    )
      stopwatch = saved;
  } catch {
    /* Storage may be unavailable; the stopwatch still works in this tab. */
  }
  function stopwatchElapsed() {
    return (
      stopwatch.elapsed +
      (stopwatch.startedAt === null
        ? 0
        : Math.max(0, Date.now() - stopwatch.startedAt))
    );
  }
  function saveStopwatch() {
    try {
      localStorage.setItem(stopwatchKey, JSON.stringify(stopwatch));
    } catch {}
  }
  function renderStopwatch() {
    const total = Math.floor(stopwatchElapsed() / 1000);
    const hours = String(Math.floor(total / 3600)).padStart(2, '0');
    const minutes = String(Math.floor(total / 60) % 60).padStart(2, '0');
    const seconds = String(total % 60).padStart(2, '0');
    $('stopwatch-time').textContent = `${hours}:${minutes}:${seconds}`;
    const running = stopwatch.startedAt !== null;
    $('stopwatch-toggle').textContent = running ? 'Ⅱ Duraklat' : '▶ Başlat';
    $('stopwatch-toggle').setAttribute(
      'aria-label',
      running ? 'Kronometreyi duraklat' : 'Kronometreyi başlat',
    );
    $('stopwatch-toggle').setAttribute('aria-pressed', String(running));
    $('stopwatch-panel').classList.toggle('running', running);
  }
  $('stopwatch-toggle').onclick = () => {
    if (stopwatch.startedAt === null) stopwatch.startedAt = Date.now();
    else {
      stopwatch.elapsed = stopwatchElapsed();
      stopwatch.startedAt = null;
    }
    saveStopwatch();
    renderStopwatch();
  };
  $('stopwatch-reset').onclick = () => {
    stopwatch = { elapsed: 0, startedAt: null };
    saveStopwatch();
    renderStopwatch();
  };
  window.addEventListener('storage', (event) => {
    if (event.key !== stopwatchKey) return;
    try {
      const value = JSON.parse(event.newValue);
      if (
        value &&
        Number.isFinite(value.elapsed) &&
        value.elapsed >= 0 &&
        (value.startedAt === null ||
          (Number.isFinite(value.startedAt) && value.startedAt > 0))
      ) {
        stopwatch = value;
        renderStopwatch();
      }
    } catch {}
  });
  setInterval(renderStopwatch, 250);
  document.addEventListener('visibilitychange', renderStopwatch);
  renderStopwatch();
}
