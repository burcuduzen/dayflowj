import { createNotes } from './features/notes.js';
import { createCalendar } from './features/calendar.js';
import { createReminders } from './features/reminders.js';
import { createProgress } from './features/progress.js';
import { createEmail } from './features/email.js';
import { createQuickTask } from './features/quickTask.js';
import { createAnalytics } from './features/analytics.js';
import { createPlanning } from './features/planning.js';
import { createGmail } from './features/gmail.js';
import { createStopwatch } from './features/stopwatch.js';
import { taskApi } from './api.js';

const $ = (id) => document.getElementById(id);
const state = {
  tasks: [],
  view: 'CALENDAR',
  loading: true,
  loaded: false,
  editing: null,
  saving: false,
  busy: new Set(),
};
const eventState = {
  events: [],
  blocks: [],
  loaded: false,
  loading: false,
  editing: null,
  saving: false,
};
const statusLabels = {
  TODO: 'Yapılacak',
  IN_PROGRESS: 'Devam ediyor',
  COMPLETED: 'Tamamlandı',
};
const priorityLabels = {
  HIGH: 'Yüksek öncelik',
  MEDIUM: 'Orta öncelik',
  LOW: 'Düşük öncelik',
};
const viewLabels = {
  CALENDAR: [
    'Takvimin, görev haritan.',
    'Bir gün seç, görevlerini gözden geçir ve sıradaki adımını ekle.',
    'Takvim',
  ],
  NOTES: [
    'Not defteri',
    'Fikirlerini ve çalışma notlarını bir yerde tut.',
    'Not defteri',
  ],
  FOCUS: ['Focus', '', ''],
  ANALYTICS: [
    'İlerlemeni görünür kıl.',
    'Tamamladığın görevleri ve günlük ilerlemeni son yedi günde incele.',
    'Analiz',
  ],
  PLANNING: [
    'Haftanı otomatik planla.',
    'Açık görevlerini çalışma saatlerindeki uygun boşluklara yerleştir.',
    'Otomatik plan',
  ],
  UPCOMING: [
    'Önünü daha net gör.',
    'Bugünden sonraki açık görevlerin.',
    'Yaklaşan görevler',
  ],
  COMPLETED: [
    'İlerlediğini gör.',
    'Bitirdiğin işler, attığın adımlar.',
    'Tamamlanan görevler',
  ],
};
let toastTimer;
const dayKey = (value) => {
  const d = new Date(value);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};
const isOpen = (task) => task.status !== 'COMPLETED';
const isToday = (task) =>
  task.dueDate && dayKey(task.dueDate) === dayKey(new Date());
const isUpcoming = (task) =>
  task.dueDate && dayKey(task.dueDate) > dayKey(new Date());
function notify(message, error = false) {
  clearTimeout(toastTimer);
  $('toast').textContent = message;
  $('toast').classList.toggle('error', error);
  $('toast').hidden = false;
  toastTimer = setTimeout(() => {
    $('toast').hidden = true;
  }, 4000);
}
function node(tag, className, text) {
  const element = document.createElement(tag);
  if (className) element.className = className;
  if (text !== undefined) element.textContent = text;
  return element;
}
function render() {
  const tasks = state.tasks;
  const open = tasks.filter(isOpen).length;
  const done = tasks.length - open;
  const today = tasks.filter((t) => isOpen(t) && isToday(t)).length;

  $('upcoming-count').textContent = tasks.filter(
    (t) => isOpen(t) && isUpcoming(t),
  ).length;
  $('completed-count').textContent = done;
  $('open-stat').textContent = open;
  $('today-stat').textContent = today;
  $('done-stat').textContent = done;
  const todayTasks = tasks.filter(isToday);
  const todayDone = todayTasks.filter((t) => !isOpen(t)).length;
  const high = tasks.filter((t) => isOpen(t) && t.priority === 'HIGH').length;
  $('daily-ratio').textContent = `${todayDone} / ${todayTasks.length}`;
  $('priority-ratio').textContent = `${high} / ${open}`;
  $('priority-tag').textContent = `${high} ÖNCELİKLİ`;
  fillMeter(
    'daily-meter',
    todayTasks.length ? todayDone / todayTasks.length : 0,
  );
  fillMeter('priority-meter', open ? high / open : 0);
  const percentage = tasks.length ? Math.round((done / tasks.length) * 100) : 0;
  $('progress-stat').replaceChildren(
    document.createTextNode(String(percentage)),
    node('em', '', '%'),
  );
  fillMeter('completion-meter', percentage / 100);
  $('progress-bar').style.width = `${percentage}%`;
  $('remaining-label').textContent = `${open} açık görev`;
  const [title, description, listTitle] = viewLabels[state.view];
  $('view-title').textContent = title;
  $('view-description').textContent = description;
  $('list-title').textContent = listTitle;
  const focusView = state.view === 'FOCUS';
  $('stopwatch-panel').hidden = !focusView;
  document.querySelector('.page-heading').hidden = focusView;
  const notesView = state.view === 'NOTES';
  const calendarView = state.view === 'CALENDAR';
  const analyticsView = state.view === 'ANALYTICS';
  const planningView = state.view === 'PLANNING';
  $('tasks-panel').hidden =
    notesView || calendarView || analyticsView || planningView || focusView;
  $('notes-panel').hidden = !notesView;
  $('analytics-panel').hidden = !analyticsView;
  $('planning-panel').hidden = !planningView;
  $('calendar-panel').hidden = !calendarView;
  $('calendar-workspace').hidden = !calendarView;
  $('new-event').hidden = !calendarView;
  document.querySelector('.metrics').hidden = !calendarView;
  $('new-task').textContent = '+ Yeni not';
  $('new-task').hidden = !notesView;
  if (calendarView) {
    renderCalendar();
    if (!eventState.loaded) loadCalendarEvents();
  }
  if (notesView) renderNotes();
  $('task-list').setAttribute('aria-busy', String(state.loading));
  $('refresh').disabled = state.loading || state.saving || state.busy.size > 0;
  $('new-task').disabled = state.loading || !state.loaded || state.saving;
  if (state.loading) {
    $('task-list').replaceChildren(
      node('div', 'loading', 'Görevlerin yükleniyor…'),
    );
    $('empty-state').hidden = true;
    $('visible-count').textContent = 'Yükleniyor…';
    return;
  }
  const query = $('search').value.trim().toLocaleLowerCase('tr');
  const status = $('status-filter').value;
  const priority = $('priority-filter').value;
  const visible = tasks.filter((task) => {
    if (state.view === 'UPCOMING' && !(isOpen(task) && isUpcoming(task)))
      return false;
    if (state.view === 'COMPLETED' && isOpen(task)) return false;
    return (
      (!status || task.status === status) &&
      (!priority || task.priority === priority) &&
      `${task.title} ${task.description || ''}`
        .toLocaleLowerCase('tr')
        .includes(query)
    );
  });
  $('task-list').replaceChildren(...visible.map(renderTask));
  $('visible-count').textContent = `${visible.length} görev`;
  const empty = state.loaded && visible.length === 0;
  $('empty-state').hidden = !empty;
  const firstTask = false;
  $('empty-title').textContent = firstTask
    ? 'Yeni bir başlangıca yer var.'
    : 'Bu görünümde görev yok.';
  $('empty-description').textContent = firstTask
    ? 'İlk görevini ekle, gününü adım adım planla.'
    : 'Başka bir görünüm seçebilir veya filtreleri değiştirebilirsin.';
  $('empty-add').hidden = !firstTask;
}
function renderTask(task) {
  const row = node('article', `task-row${isOpen(task) ? '' : ' done'}`);
  const toggle = node(
    'button',
    `task-check${isOpen(task) ? '' : ' completed'}`,
    isOpen(task) ? '' : '✓',
  );
  toggle.type = 'button';
  toggle.setAttribute(
    'aria-label',
    `${task.title}: ${isOpen(task) ? 'tamamla' : 'yeniden aç'}`,
  );
  toggle.setAttribute('aria-pressed', String(!isOpen(task)));
  toggle.disabled = state.busy.has(task.id);
  toggle.addEventListener('click', () => changeStatus(task));
  const main = node('div', 'task-main');
  main.append(node('h3', 'task-title', task.title));
  if (task.description)
    main.append(node('p', 'task-description', task.description));
  const meta = node('div', 'task-meta');
  meta.append(
    node('span', `badge ${task.priority}`, priorityLabels[task.priority]),
  );
  meta.append(node('span', 'status-badge', statusLabels[task.status]));
  if (task.dueDate) {
    const date = new Date(task.dueDate);
    const overdue = isOpen(task) && date.getTime() < Date.now();
    const label = date.toLocaleString('tr-TR', {
      day: 'numeric',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
    });
    meta.append(
      node(
        'span',
        overdue ? 'overdue' : '',
        `${overdue ? 'Gecikmiş · ' : ''}${label}`,
      ),
    );
  }
  if (task.recurrence && task.recurrence !== 'NONE')
    meta.append(
      node(
        'span',
        '',
        { DAILY: 'Her gün', WEEKLY: 'Her hafta', MONTHLY: 'Her ay' }[
          task.recurrence
        ],
      ),
    );
  if (task.estimatedMinutes)
    meta.append(node('span', '', `${task.estimatedMinutes} dk`));
  main.append(meta);
  const actions = node('div', 'task-actions');
  const edit = node('button', 'icon-button', '✎');
  edit.type = 'button';
  edit.setAttribute('aria-label', `${task.title}: düzenle`);
  edit.title = 'Düzenle';
  edit.disabled = state.busy.has(task.id);
  edit.addEventListener('click', () => openForm(task));
  const remove = node('button', 'icon-button danger', '×');
  remove.type = 'button';
  remove.setAttribute('aria-label', `${task.title}: sil`);
  remove.title = 'Sil';
  remove.disabled = state.busy.has(task.id);
  remove.addEventListener('click', () => removeTask(task));
  actions.append(edit, remove);
  row.append(toggle, main, actions);
  return row;
}
async function loadTasks() {
  if (state.saving || state.busy.size || (state.loading && state.loaded))
    return;
  state.loading = true;
  $('load-error').hidden = true;
  render();
  try {
    state.tasks = await taskApi.list();
    state.loaded = true;
  } catch (error) {
    $('load-error').textContent = error.message;
    $('load-error').hidden = false;
  } finally {
    state.loading = false;
    render();
  }
}
function toLocalInput(value) {
  if (!value) return '';
  const date = new Date(value);
  return `${dayKey(date)}T${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}
function openForm(task = null, defaultDate = null) {
  if (state.saving) return;
  state.editing = task;
  $('task-form').reset();
  $('title').setCustomValidity('');
  $('form-error').hidden = true;
  $('dialog-title').textContent = task ? 'Görevi düzenle' : 'Yeni görev';
  $('save-task').textContent = task ? 'Değişiklikleri kaydet' : 'Görevi ekle';
  $('status-field').hidden = !task;
  if (defaultDate) $('dueDate').value = `${dayKey(defaultDate)}T18:00`;
  if (task) {
    $('recurrence').value = task.recurrence || 'NONE';
    $('reminderEnabled').checked = Boolean(task.reminderEnabled);
    $('reminderMinutesBefore').value = String(task.reminderMinutesBefore || 0);
    $('title').value = task.title;
    $('description').value = task.description || '';
    $('dueDate').value = toLocalInput(task.dueDate);
    $('estimatedMinutes').value = task.estimatedMinutes || '';
    $('priority').value = task.priority;
    $('status').value = task.status;
  }
  $('task-dialog').showModal();
  $('title').focus();
}
function setSaving(saving) {
  state.saving = saving;
  $('task-form')
    .querySelectorAll('button, input, textarea, select')
    .forEach((el) => {
      el.disabled = saving;
    });
  $('save-task').textContent = saving
    ? 'Kaydediliyor…'
    : state.editing
      ? 'Değişiklikleri kaydet'
      : 'Görevi ekle';
  render();
}
async function saveTask(event) {
  event.preventDefault();
  if (state.saving) return;
  const title = $('title').value.trim();
  if (!title) {
    $('title').setCustomValidity('Görev başlığı boş olamaz.');
    $('title').reportValidity();
    return;
  }
  $('form-error').hidden = true;
  if ($('recurrence').value !== 'NONE' && !$('dueDate').value) {
    $('form-error').textContent = 'Tekrarlayan görev için bir son tarih seç.';
    $('form-error').hidden = false;
    return;
  }
  const data = {
    recurrence: $('recurrence').value,
    reminderEnabled: $('reminderEnabled').checked,
    reminderMinutesBefore: Number($('reminderMinutesBefore').value),
    timeZone:
      Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Istanbul',
    title,
    description: $('description').value.trim() || null,
    dueDate: $('dueDate').value
      ? new Date($('dueDate').value).toISOString()
      : null,
    priority: $('priority').value,
    estimatedMinutes: $('estimatedMinutes').value
      ? Number($('estimatedMinutes').value)
      : null,
  };
  const editing = state.editing;
  if (editing) data.status = $('status').value;
  setSaving(true);
  let savedSuccessfully = false;
  try {
    const saved = editing
      ? await taskApi.update(editing.id, data)
      : await taskApi.create(data);
    state.tasks = editing
      ? state.tasks.map((task) => (task.id === saved.id ? saved : task))
      : [saved, ...state.tasks];
    savedSuccessfully = true;
    $('task-dialog').close();
    notify(editing ? 'Görev güncellendi.' : 'Yeni görevin eklendi.');
    if (!state.loaded) {
      state.loaded = true;
    }
  } catch (error) {
    $('form-error').textContent = error.message;
    $('form-error').hidden = false;
  } finally {
    setSaving(false);
  }
  if (savedSuccessfully) {
    await loadTasks();
    await loadProgress();
  }
  await pollReminders();
}
async function changeStatus(task) {
  if (state.busy.has(task.id)) return;
  state.busy.add(task.id);
  render();
  try {
    const updated = await taskApi.status(
      task.id,
      isOpen(task) ? 'COMPLETED' : 'TODO',
    );
    state.tasks = state.tasks.map((t) => (t.id === task.id ? updated : t));
    notify(
      updated.status === 'COMPLETED'
        ? 'Bir adım daha tamamlandı.'
        : 'Görev yeniden açıldı.',
    );
  } catch (error) {
    notify(error.message, true);
  } finally {
    state.busy.delete(task.id);
    render();
  }
  await loadTasks();
  await pollReminders();
  await loadProgress();
}
async function removeTask(task) {
  if (
    state.busy.has(task.id) ||
    !window.confirm(`“${task.title}” görevini silmek istiyor musun?`)
  )
    return;
  state.busy.add(task.id);
  render();
  try {
    await taskApi.remove(task.id);
    state.tasks = state.tasks.filter((t) => t.id !== task.id);
    notify('Görev silindi.');
  } catch (error) {
    notify(error.message, true);
  } finally {
    state.busy.delete(task.id);
    render();
  }
  await pollReminders();
}
$('date-label').textContent = new Date().toLocaleDateString('tr-TR', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
});
$('new-task').addEventListener('click', () =>
  state.view === 'NOTES' ? openNote() : openForm(),
);
$('empty-add').addEventListener('click', () => openForm());
$('refresh').addEventListener('click', async () => {
  await loadTasks();
  if (state.view === 'NOTES') await loadNotes();
  else if (state.view === 'ANALYTICS') await loadAnalytics();
  else if (state.view === 'PLANNING') await loadPlannedBlocks();
  else if (state.view === 'CALENDAR') await loadCalendarEvents();
});
$('task-form').addEventListener('submit', saveTask);
$('title').addEventListener('input', () => $('title').setCustomValidity(''));
['close-dialog', 'cancel-dialog'].forEach((id) =>
  $(id).addEventListener('click', () => {
    if (!state.saving) $('task-dialog').close();
  }),
);
$('task-dialog').addEventListener('cancel', (event) => {
  if (state.saving) event.preventDefault();
});
['search', 'status-filter', 'priority-filter'].forEach((id) =>
  $(id).addEventListener(id === 'search' ? 'input' : 'change', render),
);
document.querySelectorAll('[data-view]').forEach((button) =>
  button.addEventListener('click', () => {
    state.view = button.dataset.view;
    $('status-filter').value = '';
    $('priority-filter').value = '';
    document.querySelectorAll('[data-view]').forEach((item) => {
      const active = item === button;
      item.classList.toggle('active', active);
      if (active) item.setAttribute('aria-current', 'page');
      else item.removeAttribute('aria-current');
    });
    if (state.view === 'NOTES') loadNotes();
    if (state.view === 'ANALYTICS') loadAnalytics();
    if (state.view === 'PLANNING') initializePlanning();
    render();
  }),
);
setInterval(() => {
  if (!state.loading) render();
}, 60000);

function fillMeter(id, ratio) {
  const meter = $(id);
  const percentage = Math.round(Math.min(1, Math.max(0, ratio)) * 100);
  meter.setAttribute('aria-valuenow', String(percentage));
  meter.replaceChildren(
    ...Array.from({ length: 10 }, (_, index) =>
      node('i', index < Math.round(percentage / 10) ? 'filled' : ''),
    ),
  );
}

// Feature controllers receive only the state and operations they use.
const { loadNotes, renderNotes, openNote } = createNotes({ $, node, notify });
const { renderCalendar, loadCalendarEvents } = createCalendar({
  $,
  state,
  eventState,
  node,
  notify,
  dayKey,
  openForm,
  changeStatus,
  loadTasks,
  pollReminders: () => pollReminders(),
  toLocalInput,
  statusLabels,
});
const { pollReminders } = createReminders({ $, node, notify });
const { loadProgress } = createProgress({ $ });
createEmail({ $, notify });
createQuickTask({ $, priorityLabels, notify, loadTasks, pollReminders });
const { loadAnalytics } = createAnalytics({ $, node });
const { initializePlanning, loadPlannedBlocks } = createPlanning({
  $,
  state,
  eventState,
  node,
  notify,
  dayKey,
  renderCalendar,
});
createGmail({ $, node, notify, loadCalendarEvents });
createStopwatch({ $ });
loadTasks();
