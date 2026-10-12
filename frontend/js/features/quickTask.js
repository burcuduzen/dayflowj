import { naturalTaskApi } from '../api.js';

export function createQuickTask({
  $,
  priorityLabels,
  notify,
  loadTasks,
  pollReminders,
}) {
  let quickBusy = false;
  let quickDraft = null;
  function quickPayload() {
    return {
      text: $('quick-text').value.trim(),
      timeZone:
        Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Istanbul',
    };
  }
  function setQuickBusy(busy) {
    quickBusy = busy;
    $('quick-form')
      .querySelectorAll('input,button')
      .forEach((element) => (element.disabled = busy));
    $('quick-create').disabled = busy;
  }
  $('quick-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    if (quickBusy) return;
    $('quick-error').hidden = true;
    $('quick-result').hidden = true;
    quickDraft = null;
    setQuickBusy(true);
    try {
      quickDraft = await naturalTaskApi.preview(quickPayload());
      $('quick-result-title').textContent = quickDraft.title;
      const details = [priorityLabels[quickDraft.priority]];
      if (quickDraft.dueDate)
        details.push(
          new Date(quickDraft.dueDate).toLocaleString('tr-TR', {
            day: 'numeric',
            month: 'short',
            hour: '2-digit',
            minute: '2-digit',
          }),
        );
      if (quickDraft.estimatedMinutes)
        details.push(`${quickDraft.estimatedMinutes} dk`);
      if (quickDraft.recurrence !== 'NONE')
        details.push(
          { DAILY: 'Her gün', WEEKLY: 'Her hafta', MONTHLY: 'Her ay' }[
            quickDraft.recurrence
          ],
        );
      $('quick-result-meta').textContent = details.join(' · ');
      $('quick-result').hidden = false;
    } catch (error) {
      $('quick-error').textContent = error.message;
      $('quick-error').hidden = false;
    } finally {
      setQuickBusy(false);
    }
  });
  $('quick-create').onclick = async () => {
    if (quickBusy || !quickDraft) return;
    setQuickBusy(true);
    $('quick-error').hidden = true;
    try {
      await naturalTaskApi.create(quickPayload());
      $('quick-text').value = '';
      $('quick-result').hidden = true;
      quickDraft = null;
      notify('Görev doğal dilden oluşturuldu.');
      await loadTasks();
      await pollReminders();
    } catch (error) {
      $('quick-error').textContent = error.message;
      $('quick-error').hidden = false;
    } finally {
      setQuickBusy(false);
    }
  };
}
