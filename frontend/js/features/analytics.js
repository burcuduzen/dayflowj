import { analyticsApi } from '../api.js';

export function createAnalytics({ $, node }) {
  let analyticsBusy = false;
  async function loadAnalytics() {
    if (analyticsBusy) return;
    analyticsBusy = true;
    $('analytics-refresh').disabled = true;
    $('analytics-error').hidden = true;
    try {
      const data = await analyticsApi.get(
        Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Istanbul',
      );
      $('analytics-range').textContent =
        `${new Date(`${data.from}T12:00:00`).toLocaleDateString('tr-TR', { day: 'numeric', month: 'short' })} – ${new Date(`${data.to}T12:00:00`).toLocaleDateString('tr-TR', { day: 'numeric', month: 'short' })}`;
      $('analytics-completed').textContent = data.completedTasks;
      $('analytics-active-days').textContent = data.days.filter(
        (day) => day.completedTasks > 0,
      ).length;
      $('analytics-estimated').textContent = data.estimatedMinutes;
      $('analytics-average').textContent = (
        data.completedTasks / Math.max(1, data.days.length)
      ).toLocaleString('tr-TR', { maximumFractionDigits: 1 });
      const max = Math.max(1, ...data.days.map((day) => day.completedTasks));
      $('analytics-chart').replaceChildren(
        ...data.days.map((day) => {
          const column = node('div', 'chart-day');
          const bar = node('div', 'chart-bar');
          bar.style.height = `${Math.max(3, (day.completedTasks / max) * 170)}px`;
          bar.title = `${day.completedTasks} görev tamamlandı`;
          column.append(
            node('strong', '', String(day.completedTasks)),
            bar,
            node(
              'span',
              '',
              new Date(`${day.date}T12:00:00`).toLocaleDateString('tr-TR', {
                weekday: 'short',
              }),
            ),
          );
          return column;
        }),
      );
    } catch (error) {
      $('analytics-error').textContent = error.message;
      $('analytics-error').hidden = false;
    } finally {
      analyticsBusy = false;
      $('analytics-refresh').disabled = false;
    }
  }
  $('analytics-refresh').onclick = loadAnalytics;

  return { loadAnalytics };
}
