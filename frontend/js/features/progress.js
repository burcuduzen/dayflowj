import { progressApi } from '../api.js';

export function createProgress({ $ }) {
  let progressLoading = false;
  async function loadProgress() {
    if (progressLoading) return;
    progressLoading = true;
    try {
      const result = await progressApi.get(
        Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Istanbul',
      );
      $('streak-count').textContent = result.currentStreak;
      $('streak-count').parentElement.title =
        `En uzun seri: ${result.longestStreak} gün`;
    } catch {
      $('streak-count').textContent = '—';
      $('streak-count').parentElement.title =
        'Seri yüklenemedi. Backend bağlantısını kontrol et.';
    } finally {
      progressLoading = false;
    }
  }

  loadProgress();
  setInterval(loadProgress, 60000);
  window.addEventListener('focus', loadProgress);

  return { loadProgress };
}
