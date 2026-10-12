import { reminderApi } from '../api.js';

export function createReminders({ $, node, notify }) {
  let reminders = [];
  let reminderPolling = false;
  let remindersBusy = new Set();
  const notified = new Set();
  try {
    JSON.parse(sessionStorage.getItem('dayflow-notified') || '[]').forEach(
      (key) => notified.add(key),
    );
  } catch {
    /* storage is optional */
  }

  function updateNotificationButton() {
    const button = $('enable-notifications');
    if (!('Notification' in window)) {
      button.textContent = 'Uygulama içi hatırlatma';
      button.disabled = true;
    } else if (Notification.permission === 'granted') {
      button.textContent = 'Bildirimler açık';
      button.disabled = true;
    } else if (Notification.permission === 'denied') {
      button.textContent = 'Bildirim izni kapalı';
      button.disabled = false;
    }
  }
  $('enable-notifications').onclick = async () => {
    if (!('Notification' in window)) return;
    if (Notification.permission === 'denied') {
      notify(
        'Tarayıcının site ayarlarından bildirim iznini açabilirsin.',
        true,
      );
      return;
    }
    await Notification.requestPermission();
    updateNotificationButton();
  };
  async function pollReminders() {
    if (reminderPolling) return;
    reminderPolling = true;
    try {
      reminders = await reminderApi.list();
      $('reminder-state').textContent = '';
      renderReminders();
      for (const reminder of reminders) {
        const key = `${reminder.taskId}:${reminder.remindAt}`;
        if (
          'Notification' in window &&
          Notification.permission === 'granted' &&
          !notified.has(key)
        ) {
          try {
            const notification = new Notification('DayFlowJ · Görev zamanı', {
              body: reminder.title,
              tag: key,
            });
            notification.onclick = () => {
              window.focus();
              notification.close();
            };
            notified.add(key);
            try {
              sessionStorage.setItem(
                'dayflow-notified',
                JSON.stringify([...notified].slice(-200)),
              );
            } catch {}
          } catch {
            /* The in-app reminder remains available on unsupported browsers. */
          }
        }
      }
    } catch {
      if (reminders.length)
        $('reminder-state').textContent = 'Bağlantı yok; tekrar denenecek.';
    } finally {
      reminderPolling = false;
    }
  }
  function renderReminders() {
    $('reminders-panel').hidden = reminders.length === 0;
    $('reminder-list').replaceChildren(
      ...reminders.map((reminder) => {
        const row = node('div', 'reminder-row');
        row.append(node('strong', '', reminder.title));
        const actions = node('div', 'reminder-actions');
        for (const minutes of [5, 10, 30]) {
          const button = node('button', 'secondary', `${minutes} dk ertele`);
          button.disabled = remindersBusy.has(reminder.taskId);
          button.onclick = () => handleReminder(reminder.taskId, minutes);
          actions.append(button);
        }
        const dismiss = node('button', 'text-button', 'Kapat');
        dismiss.disabled = remindersBusy.has(reminder.taskId);
        dismiss.onclick = () => handleReminder(reminder.taskId);
        actions.append(dismiss);
        row.append(actions);
        return row;
      }),
    );
  }
  async function handleReminder(id, minutes = null) {
    if (remindersBusy.has(id)) return;
    remindersBusy.add(id);
    renderReminders();
    try {
      if (minutes) await reminderApi.snooze(id, minutes);
      else await reminderApi.dismiss(id);
      reminders = reminders.filter((r) => r.taskId !== id);
      renderReminders();
      notify(
        minutes
          ? `Hatırlatıcı ${minutes} dakika ertelendi.`
          : 'Hatırlatıcı kapatıldı.',
      );
    } catch (error) {
      notify(error.message, true);
    } finally {
      remindersBusy.delete(id);
      renderReminders();
    }
  }
  updateNotificationButton();
  pollReminders();
  setInterval(pollReminders, 15000);
  window.addEventListener('focus', () => {
    pollReminders();
  });

  return { pollReminders };
}
