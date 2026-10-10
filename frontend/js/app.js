import { taskApi, noteApi, reminderApi, progressApi, emailApi, authApi, naturalTaskApi, settingsApi, focusApi, analyticsApi, planningApi, calendarEventApi } from './api.js';

const $ = id => document.getElementById(id);
const state = { tasks: [], view: 'CALENDAR', loading: true, loaded: false, editing: null, saving: false, busy: new Set() };
const eventState = { events: [], loaded: false, loading: false, editing: null, saving: false };
const statusLabels = { TODO: 'Yapılacak', IN_PROGRESS: 'Devam ediyor', COMPLETED: 'Tamamlandı' };
const priorityLabels = { HIGH: 'Yüksek öncelik', MEDIUM: 'Orta öncelik', LOW: 'Düşük öncelik' };
const viewLabels = {
  PLAN: ['Bugünkü planım', 'Bugün, gecikmiş ve henüz tarihlendirmediğin görevler.', 'Sıradaki işler'],
  CALENDAR: ['Takvimin, görev haritan.', 'Bir gün seç, görevlerini gözden geçir ve sıradaki adımını ekle.', 'Takvim'],
  NOTES: ['Not defteri', 'Fikirlerini ve çalışma notlarını bir yerde tut.', 'Not defteri'],
  FOCUS: ['Dikkatini koru.', 'Tek bir işe odaklan, molalarını ve gerçek çalışma süreni kaydet.', 'Focus'],
  ANALYTICS: ['İlerlemeni görünür kıl.', 'Tamamladığın görevleri ve gerçek odak süreni son yedi günde incele.', 'Analiz'],
  PLANNING: ['Haftanı otomatik planla.', 'Açık görevlerini çalışma saatlerindeki uygun boşluklara yerleştir.', 'Otomatik plan'],
  ALL: ['Tüm görevler', 'Açık ve tamamlanmış bütün görevlerin.', 'Görevlerin'],
  TODAY: ['Bugüne odaklan.', 'Bugün son tarihi gelen açık görevlerin.', 'Bugünkü görevler'],
  UPCOMING: ['Önünü daha net gör.', 'Bugünden sonraki açık görevlerin.', 'Yaklaşan görevler'],
  COMPLETED: ['İlerlediğini gör.', 'Bitirdiğin işler, attığın adımlar.', 'Tamamlanan görevler']
};
let toastTimer;
let accountReady = false;
let accountExists = false;
let authBusy = false;
let mailReady = false;
let calendarDate = new Date();
let selectedDate = new Date();
let agendaSaving = false;
const dayKey = value => {
  const d = new Date(value);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};
const isOpen = task => task.status !== 'COMPLETED';
const isToday = task => task.dueDate && dayKey(task.dueDate) === dayKey(new Date());
const isUpcoming = task => task.dueDate && dayKey(task.dueDate) > dayKey(new Date());
function notify(message, error = false) {
  clearTimeout(toastTimer);
  $('toast').textContent = message;
  $('toast').classList.toggle('error', error);
  $('toast').hidden = false;
  toastTimer = setTimeout(() => { $('toast').hidden = true; }, 4000);
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
  const today = tasks.filter(t => isOpen(t) && isToday(t)).length;
  $('all-count').textContent = tasks.length;
  $('today-count').textContent = today;
  $('upcoming-count').textContent = tasks.filter(t => isOpen(t) && isUpcoming(t)).length;
  $('completed-count').textContent = done;
  $('open-stat').textContent = open;
  $('today-stat').textContent = today;
  $('done-stat').textContent = done;
  const todayTasks = tasks.filter(isToday);
  const todayDone = todayTasks.filter(t => !isOpen(t)).length;
  const high = tasks.filter(t => isOpen(t) && t.priority === 'HIGH').length;
  $('daily-ratio').textContent = `${todayDone} / ${todayTasks.length}`;
  $('priority-ratio').textContent = `${high} / ${open}`;
  $('priority-tag').textContent = `${high} ÖNCELİKLİ`;
  fillMeter('daily-meter', todayTasks.length ? todayDone / todayTasks.length : 0);
  fillMeter('priority-meter', open ? high / open : 0);
  const percentage = tasks.length ? Math.round(done / tasks.length * 100) : 0;
  $('progress-stat').replaceChildren(document.createTextNode(String(percentage)), node('em', '', '%'));
  fillMeter('completion-meter', percentage / 100);
  $('progress-bar').style.width = `${percentage}%`;
  $('remaining-label').textContent = `${open} açık görev`;
  const [title, description, listTitle] = viewLabels[state.view];
  $('view-title').textContent = title;
  $('view-description').textContent = description;
  $('list-title').textContent = listTitle;
  const notesView = state.view === 'NOTES';
  const calendarView = state.view === 'CALENDAR';
  const focusView = state.view === 'FOCUS';
  const analyticsView = state.view === 'ANALYTICS';
  const planningView = state.view === 'PLANNING';
  $('tasks-panel').hidden = notesView || calendarView || focusView || analyticsView || planningView;
  $('notes-panel').hidden = !notesView;
  $('focus-panel').hidden = !focusView;
  $('analytics-panel').hidden = !analyticsView;
  $('planning-panel').hidden = !planningView;
  $('calendar-panel').hidden = !calendarView;
  $('calendar-workspace').hidden = !calendarView;
  $('new-event').hidden = !calendarView;
  document.querySelector('.metrics').hidden = notesView || focusView || analyticsView || planningView;
  $('new-task').textContent = notesView ? '+ Yeni not' : '+ Yeni görev';
  if (calendarView) { renderCalendar(); if (!eventState.loaded) loadCalendarEvents(); }
  if (notesView) renderNotes();
  $('task-list').setAttribute('aria-busy', String(state.loading));
  $('refresh').disabled = state.loading || state.saving || state.busy.size > 0;
  $('new-task').disabled = state.loading || !state.loaded || state.saving;
  if (state.loading) {
    $('task-list').replaceChildren(node('div', 'loading', 'Görevlerin yükleniyor…'));
    $('empty-state').hidden = true;
    $('visible-count').textContent = 'Yükleniyor…';
    return;
  }
  const query = $('search').value.trim().toLocaleLowerCase('tr');
  const status = $('status-filter').value;
  const priority = $('priority-filter').value;
  const visible = tasks.filter(task => {
    if (state.view === 'PLAN' && (!isOpen(task) || task.dueDate && dayKey(task.dueDate) > dayKey(new Date()))) return false;
    if (state.view === 'TODAY' && !(isOpen(task) && isToday(task))) return false;
    if (state.view === 'UPCOMING' && !(isOpen(task) && isUpcoming(task))) return false;
    if (state.view === 'COMPLETED' && isOpen(task)) return false;
    return (!status || task.status === status) && (!priority || task.priority === priority)
      && `${task.title} ${task.description || ''}`.toLocaleLowerCase('tr').includes(query);
  });
  $('task-list').replaceChildren(...visible.map(renderTask));
  $('visible-count').textContent = `${visible.length} görev`;
  const empty = state.loaded && visible.length === 0;
  $('empty-state').hidden = !empty;
  const firstTask = tasks.length === 0 && ['ALL', 'PLAN'].includes(state.view) && !query && !status && !priority;
  $('empty-title').textContent = firstTask ? 'Yeni bir başlangıca yer var.' : 'Bu görünümde görev yok.';
  $('empty-description').textContent = firstTask ? 'İlk görevini ekle, gününü adım adım planla.' : 'Başka bir görünüm seçebilir veya filtreleri değiştirebilirsin.';
  $('empty-add').hidden = !firstTask;
}
function renderTask(task) {
  const row = node('article', `task-row${isOpen(task) ? '' : ' done'}`);
  const toggle = node('button', `task-check${isOpen(task) ? '' : ' completed'}`, isOpen(task) ? '' : '✓');
  toggle.type = 'button';
  toggle.setAttribute('aria-label', `${task.title}: ${isOpen(task) ? 'tamamla' : 'yeniden aç'}`);
  toggle.setAttribute('aria-pressed', String(!isOpen(task)));
  toggle.disabled = state.busy.has(task.id);
  toggle.addEventListener('click', () => changeStatus(task));
  const main = node('div', 'task-main');
  main.append(node('h3', 'task-title', task.title));
  if (task.description) main.append(node('p', 'task-description', task.description));
  const meta = node('div', 'task-meta');
  meta.append(node('span', `badge ${task.priority}`, priorityLabels[task.priority]));
  meta.append(node('span', 'status-badge', statusLabels[task.status]));
  if (task.dueDate) {
    const date = new Date(task.dueDate);
    const overdue = isOpen(task) && date.getTime() < Date.now();
    const label = date.toLocaleString('tr-TR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
    meta.append(node('span', overdue ? 'overdue' : '', `${overdue ? 'Gecikmiş · ' : ''}${label}`));
  }
  if (task.recurrence && task.recurrence !== 'NONE') meta.append(node('span', '', {DAILY:'Her gün',WEEKLY:'Her hafta',MONTHLY:'Her ay'}[task.recurrence]));
  if (task.estimatedMinutes) meta.append(node('span', '', `${task.estimatedMinutes} dk`));
  main.append(meta);
  const actions = node('div', 'task-actions');
  const edit = node('button', 'icon-button', '✎');
  edit.type = 'button'; edit.setAttribute('aria-label', `${task.title}: düzenle`); edit.title = 'Düzenle';
  edit.disabled = state.busy.has(task.id); edit.addEventListener('click', () => openForm(task));
  const remove = node('button', 'icon-button danger', '×');
  remove.type = 'button'; remove.setAttribute('aria-label', `${task.title}: sil`); remove.title = 'Sil';
  remove.disabled = state.busy.has(task.id); remove.addEventListener('click', () => removeTask(task));
  actions.append(edit, remove); row.append(toggle, main, actions);
  return row;
}
async function loadTasks() {
  if (!accountReady) return;
  if (state.saving || state.busy.size || state.loading && state.loaded) return;
  state.loading = true; $('load-error').hidden = true; render();
  try { state.tasks = await taskApi.list(); state.loaded = true; }
  catch (error) { $('load-error').textContent = error.message; $('load-error').hidden = false; }
  finally { state.loading = false; render(); }
}
function toLocalInput(value) {
  if (!value) return '';
  const date = new Date(value);
  return `${dayKey(date)}T${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}
function openForm(task = null, defaultDate = null) {
  if (state.saving) return;
  state.editing = task;
  $('task-form').reset(); $('title').setCustomValidity(''); $('form-error').hidden = true;
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
  $('task-dialog').showModal(); $('title').focus();
}
function setSaving(saving) {
  state.saving = saving;
  $('task-form').querySelectorAll('button, input, textarea, select').forEach(el => { el.disabled = saving; });
  $('save-task').textContent = saving ? 'Kaydediliyor…' : state.editing ? 'Değişiklikleri kaydet' : 'Görevi ekle';
  render();
}
async function saveTask(event) {
  event.preventDefault();
  if (state.saving) return;
  const title = $('title').value.trim();
  if (!title) { $('title').setCustomValidity('Görev başlığı boş olamaz.'); $('title').reportValidity(); return; }
  $('form-error').hidden = true;
  if ($('recurrence').value !== 'NONE' && !$('dueDate').value) {
    $('form-error').textContent = 'Tekrarlayan görev için bir son tarih seç.'; $('form-error').hidden = false; return;
  }
  const data = {
    recurrence: $('recurrence').value, reminderEnabled: $('reminderEnabled').checked,
    reminderMinutesBefore: Number($('reminderMinutesBefore').value),
    timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Istanbul',
    title, description: $('description').value.trim() || null,
    dueDate: $('dueDate').value ? new Date($('dueDate').value).toISOString() : null,
    priority: $('priority').value,
    estimatedMinutes: $('estimatedMinutes').value ? Number($('estimatedMinutes').value) : null
  };
  const editing = state.editing;
  if (editing) data.status = $('status').value;
  setSaving(true);
  let savedSuccessfully = false;
  try {
    const saved = editing ? await taskApi.update(editing.id, data) : await taskApi.create(data);
    state.tasks = editing ? state.tasks.map(task => task.id === saved.id ? saved : task) : [saved, ...state.tasks];
    savedSuccessfully = true;
    $('task-dialog').close();
    notify(editing ? 'Görev güncellendi.' : 'Yeni görevin eklendi.');
    if (!state.loaded) { state.loaded = true; }
  } catch (error) { $('form-error').textContent = error.message; $('form-error').hidden = false; }
  finally { setSaving(false); }
  if (savedSuccessfully) { await loadTasks(); await loadProgress(); }
  await pollReminders();
}
async function changeStatus(task) {
  if (state.busy.has(task.id)) return;
  state.busy.add(task.id); render();
  try {
    const updated = await taskApi.status(task.id, isOpen(task) ? 'COMPLETED' : 'TODO');
    state.tasks = state.tasks.map(t => t.id === task.id ? updated : t);
    notify(updated.status === 'COMPLETED' ? 'Bir adım daha tamamlandı.' : 'Görev yeniden açıldı.');
  } catch (error) { notify(error.message, true); }
  finally { state.busy.delete(task.id); render(); }
  await loadTasks(); await pollReminders(); await loadProgress();
}
async function removeTask(task) {
  if (state.busy.has(task.id) || !window.confirm(`“${task.title}” görevini silmek istiyor musun?`)) return;
  state.busy.add(task.id); render();
  try { await taskApi.remove(task.id); state.tasks = state.tasks.filter(t => t.id !== task.id); notify('Görev silindi.'); }
  catch (error) { notify(error.message, true); }
  finally { state.busy.delete(task.id); render(); }
  await pollReminders();
}
$('date-label').textContent = new Date().toLocaleDateString('tr-TR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
$('new-task').addEventListener('click', () => state.view === 'NOTES' ? openNote() : openForm());
$('empty-add').addEventListener('click', () => openForm());
$('refresh').addEventListener('click', loadTasks);
$('task-form').addEventListener('submit', saveTask);
$('title').addEventListener('input', () => $('title').setCustomValidity(''));
['close-dialog', 'cancel-dialog'].forEach(id => $(id).addEventListener('click', () => { if (!state.saving) $('task-dialog').close(); }));
$('task-dialog').addEventListener('cancel', event => { if (state.saving) event.preventDefault(); });
['search', 'status-filter', 'priority-filter'].forEach(id => $(id).addEventListener(id === 'search' ? 'input' : 'change', render));
document.querySelectorAll('[data-view]').forEach(button => button.addEventListener('click', () => {
  state.view = button.dataset.view;
  $('status-filter').value = ''; $('priority-filter').value = '';
  document.querySelectorAll('[data-view]').forEach(item => {
    const active = item === button;
    item.classList.toggle('active', active);
    if (active) item.setAttribute('aria-current', 'page'); else item.removeAttribute('aria-current');
  });
  if (state.view === 'NOTES') loadNotes();
  if (state.view === 'FOCUS') loadFocus();
  if (state.view === 'ANALYTICS') loadAnalytics();
  if (state.view === 'PLANNING') initializePlanning();
  render();
}));
setInterval(() => { if (accountReady && !state.loading) render(); }, 60000);
loadTasks();

const noteState = { notes: [], loading: false, loaded: false, editing: null, saving: false };
let reminders = [];
let reminderPolling = false;
let remindersBusy = new Set();
const notified = new Set();
try { JSON.parse(sessionStorage.getItem('dayflow-notified') || '[]').forEach(key => notified.add(key)); } catch { /* storage is optional */ }

async function loadNotes() {
  if (!accountReady) return;
  if (noteState.loading || noteState.saving) return;
  noteState.loading = true; $('note-error').hidden = true; renderNotes();
  try { noteState.notes = await noteApi.list(); noteState.loaded = true; }
  catch (error) { $('note-error').textContent = error.message; $('note-error').hidden = false; }
  finally { noteState.loading = false; renderNotes(); }
}
function renderNotes() {
  const query = $('note-search').value.trim().toLocaleLowerCase('tr');
  const visible = noteState.notes.filter(n => `${n.title} ${n.content}`.toLocaleLowerCase('tr').includes(query));
  $('note-count').textContent = `${visible.length} not`;
  if (noteState.loading) { $('note-list').replaceChildren(node('p', 'loading', 'Notlar yükleniyor…')); return; }
  $('note-list').replaceChildren(...visible.map(note => {
    const card = node('article', 'note-card');
    const heading = node('div', 'note-card-heading');
    heading.append(node('h3', '', note.title));
    const edit = node('button', 'icon-button', '✎'); edit.setAttribute('aria-label', `${note.title}: notu düzenle`); edit.onclick = () => openNote(note);
    const remove = node('button', 'icon-button danger', '×'); remove.setAttribute('aria-label', `${note.title}: notu sil`); remove.onclick = () => deleteNote(note);
    heading.append(edit, remove); card.append(heading, node('p', 'note-preview', note.content || 'İçerik henüz boş.'), node('small', '', new Date(note.updatedAt).toLocaleString('tr-TR')));
    return card;
  }));
  if (!visible.length && noteState.loaded) $('note-list').append(node('p', 'loading', query ? 'Aramanla eşleşen not yok.' : 'Yeni not butonuyla ilk notunu ekle.'));
}
function openNote(note = null) {
  if (noteState.saving) return;
  noteState.editing = note; $('note-form').reset(); $('note-title').setCustomValidity('');
  $('note-form-error').hidden = true;
  $('note-dialog-title').textContent = note ? 'Notu düzenle' : 'Yeni not';
  $('note-title').value = note?.title || ''; $('note-content').value = note?.content || '';
  $('note-dialog').showModal(); $('note-title').focus();
}
async function saveNote(event) {
  event.preventDefault(); if (noteState.saving) return;
  const title = $('note-title').value.trim();
  if (!title) { $('note-title').setCustomValidity('Başlık boş olamaz.'); $('note-title').reportValidity(); return; }
  noteState.saving = true;
  $('note-form').querySelectorAll('button,input,textarea').forEach(el => { el.disabled = true; });
  $('note-form-error').hidden = true;
  const body = { title, content: $('note-content').value };
  try {
    const saved = noteState.editing ? await noteApi.update(noteState.editing.id, body) : await noteApi.create(body);
    noteState.notes = [saved, ...noteState.notes.filter(n => n.id !== saved.id)]; noteState.loaded = true;
    $('note-dialog').close(); notify('Not kaydedildi.'); renderNotes();
  } catch (error) { $('note-form-error').textContent = error.message; $('note-form-error').hidden = false; }
  finally { noteState.saving = false; $('note-form').querySelectorAll('button,input,textarea').forEach(el => { el.disabled = false; }); }
}
async function deleteNote(note) {
  if (!confirm(`“${note.title}” notunu silmek istiyor musun?`)) return;
  try { await noteApi.remove(note.id); noteState.notes = noteState.notes.filter(n => n.id !== note.id); renderNotes(); notify('Not silindi.'); }
  catch (error) { notify(error.message, true); }
}
$('note-search').addEventListener('input', renderNotes);
$('note-title').addEventListener('input', () => $('note-title').setCustomValidity(''));
$('note-form').addEventListener('submit', saveNote);
['close-note','cancel-note'].forEach(id => $(id).onclick = () => { if (!noteState.saving) $('note-dialog').close(); });
$('note-dialog').addEventListener('cancel', event => { if (noteState.saving) event.preventDefault(); });

function fillMeter(id, ratio) {
  const meter = $(id);
  const percentage = Math.round(Math.min(1, Math.max(0, ratio)) * 100);
  meter.setAttribute('aria-valuenow', String(percentage));
  meter.replaceChildren(...Array.from({length:10}, (_,index) => node('i', index < Math.round(percentage/10) ? 'filled' : '')));
}
function eventsOnDay(date) {
  const start = new Date(date.getFullYear(),date.getMonth(),date.getDate());
  const end = new Date(start); end.setDate(end.getDate()+1);
  return eventState.events.filter(event => new Date(event.startAt) < end && new Date(event.endAt) > start)
    .sort((a,b)=>new Date(a.startAt)-new Date(b.startAt));
}
function renderCalendar() {
  const mode = $('calendar-mode').value;
  const start = new Date(calendarDate.getFullYear(), calendarDate.getMonth(), calendarDate.getDate());
  let count = 1;
  if (mode === 'MONTH') { start.setDate(1); start.setDate(start.getDate() - (start.getDay() + 6) % 7); count = Math.ceil(((new Date(calendarDate.getFullYear(),calendarDate.getMonth(),1).getDay()+6)%7 + new Date(calendarDate.getFullYear(),calendarDate.getMonth()+1,0).getDate())/7)*7; }
  if (mode === 'WEEK') { start.setDate(start.getDate() - (start.getDay() + 6) % 7); count = 7; }
  $('calendar-title').textContent = calendarDate.toLocaleDateString('tr-TR', mode === 'MONTH' ? {month:'long',year:'numeric'} : {day:'numeric',month:'long',year:'numeric'}).toLocaleUpperCase('tr');
  const grid = $('calendar-grid'); grid.className = `calendar-grid ${mode.toLowerCase()}`; grid.replaceChildren();
  if (mode !== 'DAY') ['PZT','SAL','ÇAR','PER','CUM','CMT','PAZ'].forEach(day => grid.append(node('div','calendar-weekday',day)));
  for (let i = 0; i < count; i++) {
    const date = new Date(start); date.setDate(start.getDate() + i);
    const key = dayKey(date);
    const cell = node('div', `calendar-cell${key === dayKey(new Date()) ? ' current-day' : ''}${key === dayKey(selectedDate) ? ' selected' : ''}${mode === 'MONTH' && date.getMonth() !== calendarDate.getMonth() ? ' other-month' : ''}`);
    const head = node('div','calendar-cell-head');
    const select = node('button','day-number',mode === 'DAY' ? date.toLocaleDateString('tr-TR',{weekday:'long',day:'numeric',month:'long'}) : String(date.getDate()));
    select.setAttribute('aria-label',date.toLocaleDateString('tr-TR',{weekday:'long',day:'numeric',month:'long',year:'numeric'}));
    select.setAttribute('aria-pressed',String(key === dayKey(selectedDate)));
    select.onclick = () => { selectedDate = new Date(date); renderCalendar(); };
    head.append(select);
    const add = node('button','calendar-add','+');add.setAttribute('aria-label',`${key}: görev ekle`);add.onclick = () => { selectedDate = new Date(date); openForm(null,date); };head.append(add);cell.append(head);
    const tasks = state.tasks.filter(t => t.dueDate && dayKey(t.dueDate) === key).sort((a,b) => new Date(a.dueDate)-new Date(b.dueDate));
    const fixedEvents = eventsOnDay(date);
    if (mode === 'MONTH') {
      const dots = node('div','dots');
      tasks.slice(0,6).forEach(task => { const dot = node('i',`dot ${task.status === 'COMPLETED' ? 'completed' : task.priority}`);dot.title=task.title;dots.append(dot); });
      fixedEvents.slice(0,3).forEach(event => { const dot=node('i','dot event-dot');dot.title=event.title;dots.append(dot); });
      if (tasks.length + fixedEvents.length > 9) dots.append(node('span','more-dots',`+${tasks.length+fixedEvents.length-9}`));
      cell.append(dots);
    } else {
      fixedEvents.forEach(event => { const button=node('button','calendar-task calendar-event',`${new Date(event.startAt).toLocaleTimeString('tr-TR',{hour:'2-digit',minute:'2-digit'})} ${event.title}`);button.onclick=()=>openCalendarEvent(event);cell.append(button); });
      tasks.forEach(task => {
      const button = node('button',`calendar-task ${task.priority}${task.status === 'COMPLETED' ? ' done' : ''}`, `${new Date(task.dueDate).toLocaleTimeString('tr-TR',{hour:'2-digit',minute:'2-digit'})} ${task.title}`);
      button.onclick = () => openForm(task);cell.append(button);
      });
    }
    grid.append(cell);
  }
  renderAgenda();
}
function renderAgenda() {
  $('selected-label').textContent = selectedDate.toLocaleDateString('tr-TR',{weekday:'long',day:'numeric',month:'long',year:'numeric'}).toLocaleUpperCase('tr');
  const tasks = state.tasks.filter(t => t.dueDate && dayKey(t.dueDate) === dayKey(selectedDate)).sort((a,b)=>new Date(a.dueDate)-new Date(b.dueDate));
  const fixedEvents = eventsOnDay(selectedDate);
  const list = $('agenda-list');list.replaceChildren();
  if (state.loading) { list.append(node('p','empty','Görevler yükleniyor…'));return; }
  if (!state.loaded) { list.append(node('p','empty','Görevler yüklenemedi. Üstteki Yenile butonuyla tekrar deneyebilirsin.'));return; }
  if (!tasks.length && !fixedEvents.length) list.append(node('p','empty','Bu gün için görev veya etkinlik yok.'));
  fixedEvents.forEach(item => {
    const event=node('article','event fixed-event');event.append(node('i','stripe event-stripe'),node('span','event-time',new Date(item.startAt).toLocaleTimeString('tr-TR',{hour:'2-digit',minute:'2-digit'})));
    const content=node('div','event-content');const title=node('button','event-name',item.title);title.onclick=()=>openCalendarEvent(item);content.append(title,node('p','event-type','Sabit etkinlik'));event.append(content);list.append(event);
  });
  tasks.forEach(task => {
    const event = node('article',`event${task.status === 'COMPLETED' ? ' done' : ''}`);
    event.append(node('i',`stripe ${task.priority}`),node('span','event-time',new Date(task.dueDate).toLocaleTimeString('tr-TR',{hour:'2-digit',minute:'2-digit'})));
    const content = node('div','event-content');const title=node('button','event-name',task.title);title.onclick=()=>openForm(task);
    content.append(title,node('p','event-type',statusLabels[task.status]));
    const check=node('button','task-check'+(task.status === 'COMPLETED' ? ' completed' : ''),task.status === 'COMPLETED'?'✓':'');
    check.setAttribute('aria-label',`${task.title}: ${task.status === 'COMPLETED'?'yeniden aç':'tamamla'}`);check.disabled=state.busy.has(task.id);check.onclick=()=>changeStatus(task);
    event.append(content,check);list.append(event);
  });
}
$('agenda-form').addEventListener('submit', async event => {
  event.preventDefault();if(agendaSaving || !state.loaded)return;
  const title=$('agenda-title').value.trim();if(!title){$('agenda-title').setCustomValidity('Görev başlığı boş olamaz.');$('agenda-title').reportValidity();return;}
  const dueDate=new Date(`${dayKey(selectedDate)}T${$('agenda-time').value}`);
  agendaSaving=true;$('agenda-form').querySelectorAll('input,select,button').forEach(el=>el.disabled=true);$('agenda-error').hidden=true;
  try {
    await taskApi.create({title,dueDate:dueDate.toISOString(),priority:$('agenda-priority').value,recurrence:'NONE',reminderEnabled:true,timeZone:Intl.DateTimeFormat().resolvedOptions().timeZone});
    $('agenda-title').value='';notify('Görev ajandaya eklendi.');await loadTasks();await pollReminders();
  } catch(error){$('agenda-error').textContent=error.message;$('agenda-error').hidden=false;}
  finally{agendaSaving=false;$('agenda-form').querySelectorAll('input,select,button').forEach(el=>el.disabled=false);}
});
$('agenda-title').addEventListener('input',()=>$('agenda-title').setCustomValidity(''));
function moveCalendar(direction) {
  const mode = $('calendar-mode').value;
  if (mode === 'MONTH') { calendarDate.setDate(1); calendarDate.setMonth(calendarDate.getMonth() + direction); }
  else calendarDate.setDate(calendarDate.getDate() + direction * (mode === 'WEEK' ? 7 : 1));
  renderCalendar();
}
$('calendar-prev').onclick = () => moveCalendar(-1);
$('calendar-next').onclick = () => moveCalendar(1);
$('calendar-today').onclick = () => { calendarDate = new Date(); selectedDate = new Date(); renderCalendar(); };
$('calendar-mode').onchange = renderCalendar;

async function loadCalendarEvents() {
  if (!accountReady || eventState.loading) return;
  eventState.loading=true;
  try { eventState.events=await calendarEventApi.list();eventState.loaded=true;renderCalendar(); }
  catch(error){notify(error.message,true);}
  finally{eventState.loading=false;}
}
function openCalendarEvent(event=null) {
  if(eventState.saving)return;
  eventState.editing=event;$('event-form').reset();$('event-error').hidden=true;
  $('event-dialog-title').textContent=event?'Etkinliği düzenle':'Yeni etkinlik';$('delete-event').hidden=!event;
  if(event){$('event-title').value=event.title;$('event-description').value=event.description||'';$('event-start').value=toLocalInput(event.startAt);$('event-end').value=toLocalInput(event.endAt);}
  else {const start=new Date(selectedDate);start.setHours(10,0,0,0);const end=new Date(start);end.setHours(11);$('event-start').value=toLocalInput(start);$('event-end').value=toLocalInput(end);}
  $('event-dialog').showModal();$('event-title').focus();
}
function setEventBusy(busy){eventState.saving=busy;$('event-form').querySelectorAll('input,textarea,button').forEach(element=>element.disabled=busy);}
$('new-event').onclick=()=>openCalendarEvent();
['close-event','cancel-event'].forEach(id=>$(id).onclick=()=>{if(!eventState.saving)$('event-dialog').close();});
$('event-dialog').addEventListener('cancel',event=>{if(eventState.saving)event.preventDefault();});
$('event-form').addEventListener('submit',async event=>{
  event.preventDefault();if(eventState.saving)return;setEventBusy(true);$('event-error').hidden=true;
  const data={title:$('event-title').value.trim(),description:$('event-description').value.trim()||null,startAt:new Date($('event-start').value).toISOString(),endAt:new Date($('event-end').value).toISOString()};
  try{if(eventState.editing)await calendarEventApi.update(eventState.editing.id,data);else await calendarEventApi.create(data);$('event-dialog').close();notify(eventState.editing?'Etkinlik güncellendi.':'Etkinlik eklendi.');await loadCalendarEvents();}
  catch(error){$('event-error').textContent=error.message;$('event-error').hidden=false;}
  finally{setEventBusy(false);}
});
$('delete-event').onclick=async()=>{
  const event=eventState.editing;if(!event||eventState.saving||!confirm(`“${event.title}” etkinliğini silmek istiyor musun?`))return;
  setEventBusy(true);try{await calendarEventApi.remove(event.id);$('event-dialog').close();notify('Etkinlik silindi.');await loadCalendarEvents();}catch(error){$('event-error').textContent=error.message;$('event-error').hidden=false;}finally{setEventBusy(false);}
};

function updateNotificationButton() {
  const button = $('enable-notifications');
  if (!('Notification' in window)) { button.textContent = 'Uygulama içi hatırlatma'; button.disabled = true; }
  else if (Notification.permission === 'granted') { button.textContent = 'Bildirimler açık'; button.disabled = true; }
  else if (Notification.permission === 'denied') { button.textContent = 'Bildirim izni kapalı'; button.disabled = false; }
}
$('enable-notifications').onclick = async () => {
  if (!('Notification' in window)) return;
  if (Notification.permission === 'denied') { notify('Tarayıcının site ayarlarından bildirim iznini açabilirsin.', true); return; }
  await Notification.requestPermission(); updateNotificationButton();
};
async function pollReminders() {
  if (!accountReady) return;
  if (reminderPolling) return;
  reminderPolling = true;
  try {
    reminders = await reminderApi.list(); $('reminder-state').textContent = ''; renderReminders();
    for (const reminder of reminders) {
      const key = `${reminder.taskId}:${reminder.remindAt}`;
      if ('Notification' in window && Notification.permission === 'granted' && !notified.has(key)) {
        try {
          const notification = new Notification('DayFlowJ · Görev zamanı', {body:reminder.title, tag:key});
          notification.onclick = () => { window.focus(); notification.close(); };
          notified.add(key);
          try { sessionStorage.setItem('dayflow-notified',JSON.stringify([...notified].slice(-200))); } catch {}
        } catch { /* The in-app reminder remains available on unsupported browsers. */ }
      }
    }
  } catch { if (reminders.length) $('reminder-state').textContent = 'Bağlantı yok; tekrar denenecek.'; }
  finally { reminderPolling = false; }
}
function renderReminders() {
  $('reminders-panel').hidden = reminders.length === 0;
  $('reminder-list').replaceChildren(...reminders.map(reminder => {
    const row = node('div','reminder-row');row.append(node('strong','',reminder.title));
    const actions = node('div','reminder-actions');
    for (const minutes of [5,10,30]) {
      const button = node('button','secondary',`${minutes} dk ertele`);button.disabled=remindersBusy.has(reminder.taskId);
      button.onclick=()=>handleReminder(reminder.taskId,minutes);actions.append(button);
    }
    const dismiss = node('button','text-button','Kapat');dismiss.disabled=remindersBusy.has(reminder.taskId);dismiss.onclick=()=>handleReminder(reminder.taskId);actions.append(dismiss);row.append(actions);return row;
  }));
}
async function handleReminder(id, minutes = null) {
  if (remindersBusy.has(id)) return;
  remindersBusy.add(id);renderReminders();
  try {
    if (minutes) await reminderApi.snooze(id,minutes); else await reminderApi.dismiss(id);
    reminders=reminders.filter(r=>r.taskId!==id);renderReminders();
    notify(minutes ? `Hatırlatıcı ${minutes} dakika ertelendi.` : 'Hatırlatıcı kapatıldı.');
  } catch(error) { notify(error.message,true); }
  finally { remindersBusy.delete(id);renderReminders(); }
}
updateNotificationButton();
pollReminders();
setInterval(pollReminders,15000);
window.addEventListener('focus', () => { pollReminders(); });

let progressLoading = false;
let emailBusy = false;
let emailConfigured = false;
let emailSavedRecipient = '';
async function loadProgress() {
  if (!accountReady) return;
  if (progressLoading) return;
  progressLoading = true;
  try {
    const result = await progressApi.get(Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Istanbul');
    $('streak-count').textContent = result.currentStreak;
    $('streak-count').parentElement.title = `En uzun seri: ${result.longestStreak} gün`;
  } catch {
    $('streak-count').textContent = '—';
    $('streak-count').parentElement.title = 'Seri yüklenemedi. Backend bağlantısını kontrol et.';
  }
  finally { progressLoading = false; }
}
function setEmailBusy(busy) {
  emailBusy=busy;
  $('email-form').querySelectorAll('input,button').forEach(el=>el.disabled=busy);
  if (!busy) $('test-email').disabled = !emailConfigured || !emailSavedRecipient;
}
async function openEmail() {
  $('email-dialog').showModal();$('email-error').hidden=true;setEmailBusy(true);
  try {
    const settings = await emailApi.get();
    emailConfigured=settings.smtpConfigured;emailSavedRecipient=settings.recipient || '';
    $('email-recipient').value=emailSavedRecipient;$('email-enabled').checked=settings.enabled;
    $('email-connection').textContent=emailConfigured ? 'Bildirimler hesabının doğrulanmış mail adresine gönderilecek.' : 'DayFlowJ mail göndericisi şu anda hazır değil.';
  } catch(error) {$('email-error').textContent=error.message;$('email-error').hidden=false;}
  finally {setEmailBusy(false);}
}
$('open-email').onclick=openEmail;
$('close-email').onclick=()=>{if(!emailBusy)$('email-dialog').close();};
$('email-dialog').addEventListener('cancel',event=>{if(emailBusy)event.preventDefault();});
$('email-form').addEventListener('submit',async event=>{
  event.preventDefault();if(emailBusy)return;
  const enabled=$('email-enabled').checked;
  setEmailBusy(true);$('email-error').hidden=true;
  try {
    const settings=await emailApi.save({enabled});emailSavedRecipient=settings.recipient;emailConfigured=settings.smtpConfigured;
    notify(enabled && !emailConfigured ? 'Tercihin kaydedildi. DayFlowJ mail göndericisi hazır olduğunda bildirim alabilirsin.' : 'E-posta tercihleri kaydedildi.');
    $('email-dialog').close();
  }catch(error){$('email-error').textContent=error.message;$('email-error').hidden=false;}
  finally{setEmailBusy(false);}
});
$('test-email').onclick=async()=>{
  if(emailBusy)return;
  setEmailBusy(true);$('email-error').hidden=true;
  try{await emailApi.test();notify('Test maili gönderildi. Gelen kutunu kontrol et.');}
  catch(error){$('email-error').textContent=error.message;$('email-error').hidden=false;}
  finally{setEmailBusy(false);}
};
loadProgress();
setInterval(loadProgress,60000);
window.addEventListener('focus',loadProgress);

let quickBusy = false;
let quickDraft = null;
function quickPayload() {
  return {text:$('quick-text').value.trim(),timeZone:Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Istanbul'};
}
function setQuickBusy(busy) {
  quickBusy = busy;
  $('quick-form').querySelectorAll('input,button').forEach(element => element.disabled = busy);
  $('quick-create').disabled = busy;
}
$('quick-form').addEventListener('submit', async event => {
  event.preventDefault(); if (quickBusy) return;
  $('quick-error').hidden = true; $('quick-result').hidden = true; quickDraft = null;
  setQuickBusy(true);
  try {
    quickDraft = await naturalTaskApi.preview(quickPayload());
    $('quick-result-title').textContent = quickDraft.title;
    const details = [priorityLabels[quickDraft.priority]];
    if (quickDraft.dueDate) details.push(new Date(quickDraft.dueDate).toLocaleString('tr-TR',{day:'numeric',month:'short',hour:'2-digit',minute:'2-digit'}));
    if (quickDraft.estimatedMinutes) details.push(`${quickDraft.estimatedMinutes} dk`);
    if (quickDraft.recurrence !== 'NONE') details.push({DAILY:'Her gün',WEEKLY:'Her hafta',MONTHLY:'Her ay'}[quickDraft.recurrence]);
    $('quick-result-meta').textContent = details.join(' · '); $('quick-result').hidden = false;
  } catch(error) { $('quick-error').textContent = error.message; $('quick-error').hidden = false; }
  finally { setQuickBusy(false); }
});
$('quick-create').onclick = async () => {
  if (quickBusy || !quickDraft) return;
  setQuickBusy(true); $('quick-error').hidden = true;
  try {
    await naturalTaskApi.create(quickPayload());
    $('quick-text').value = ''; $('quick-result').hidden = true; quickDraft = null;
    notify('Görev doğal dilden oluşturuldu.'); await loadTasks(); await pollReminders();
  } catch(error) { $('quick-error').textContent = error.message; $('quick-error').hidden = false; }
  finally { setQuickBusy(false); }
};

let settingsBusy = false;
function setSettingsBusy(busy) {
  settingsBusy = busy;
  $('settings-form').querySelectorAll('input,button').forEach(element => element.disabled = busy);
}
async function openSettings() {
  $('settings-dialog').showModal(); $('settings-error').hidden = true; setSettingsBusy(true);
  try {
    const value = await settingsApi.get();
    $('settings-goal').value = value.dailyTaskGoal; $('settings-zone').value = value.timeZone;
    $('settings-start').value = value.workStart.slice(0,5); $('settings-end').value = value.workEnd.slice(0,5);
    $('settings-focus').value = value.focusMinutes; $('settings-break').value = value.breakMinutes;
  } catch(error) { $('settings-error').textContent = error.message; $('settings-error').hidden = false; }
  finally { setSettingsBusy(false); }
}
$('open-settings').onclick = openSettings;
['close-settings','cancel-settings'].forEach(id => $(id).onclick = () => { if (!settingsBusy) $('settings-dialog').close(); });
$('settings-dialog').addEventListener('cancel', event => { if (settingsBusy) event.preventDefault(); });
$('settings-form').addEventListener('submit', async event => {
  event.preventDefault(); if (settingsBusy) return;
  setSettingsBusy(true); $('settings-error').hidden = true;
  const data = {dailyTaskGoal:Number($('settings-goal').value),timeZone:$('settings-zone').value.trim(),
    workStart:$('settings-start').value,workEnd:$('settings-end').value,
    focusMinutes:Number($('settings-focus').value),breakMinutes:Number($('settings-break').value)};
  try { await settingsApi.save(data); $('settings-dialog').close(); notify('Ayarların kaydedildi.'); await loadProgress(); }
  catch(error) { $('settings-error').textContent = error.message; $('settings-error').hidden = false; }
  finally { setSettingsBusy(false); }
});

let activeFocus = null;
let focusLoadedAt = 0;
let focusBusy = false;
function focusSeconds() {
  if (!activeFocus) return 0;
  return activeFocus.focusedSeconds + (activeFocus.status === 'RUNNING' ? Math.max(0,Math.floor((Date.now()-focusLoadedAt)/1000)) : 0);
}
function renderFocus() {
  const seconds = focusSeconds();
  $('focus-time').textContent = `${String(Math.floor(seconds/60)).padStart(2,'0')}:${String(seconds%60).padStart(2,'0')}`;
  $('focus-state').textContent = !activeFocus ? 'Hazır' : activeFocus.status === 'RUNNING' ? 'Odaklanıyor' : 'Duraklatıldı';
  const task = activeFocus?.taskId ? state.tasks.find(item => item.id === activeFocus.taskId) : null;
  $('focus-task-label').textContent = task?.title || (activeFocus ? 'Serbest odak oturumu' : 'Dikkatini tek bir işe ver.');
  $('focus-start').hidden = Boolean(activeFocus); $('focus-task').disabled = focusBusy || Boolean(activeFocus);
  $('focus-pause').hidden = !activeFocus; $('focus-complete').hidden = !activeFocus;
  if (activeFocus) $('focus-pause').textContent = activeFocus.status === 'PAUSED' ? 'Devam et' : 'Duraklat';
  ['focus-start','focus-pause','focus-complete'].forEach(id => $(id).disabled = focusBusy);
}
async function loadFocus() {
  if (!accountReady || focusBusy) return;
  $('focus-task').replaceChildren(new Option('Görev seçmeden başla',''), ...state.tasks.filter(isOpen).map(task => new Option(task.title,String(task.id))));
  try { activeFocus = await focusApi.active(); focusLoadedAt = Date.now(); $('focus-error').hidden = true; }
  catch(error) { $('focus-error').textContent = error.message; $('focus-error').hidden = false; }
  renderFocus();
}
async function focusAction(action) {
  if (focusBusy) return;
  focusBusy = true; $('focus-error').hidden = true; renderFocus();
  try {
    if (action === 'start') activeFocus = await focusApi.start($('focus-task').value ? Number($('focus-task').value) : null);
    else if (action === 'complete') { await focusApi.complete(activeFocus.id); activeFocus = null; notify('Focus oturumu tamamlandı.'); }
    else activeFocus = activeFocus.status === 'PAUSED' ? await focusApi.resume(activeFocus.id) : await focusApi.pause(activeFocus.id);
    focusLoadedAt = Date.now();
  } catch(error) { $('focus-error').textContent = error.message; $('focus-error').hidden = false; }
  finally { focusBusy = false; renderFocus(); }
}
$('focus-start').onclick = () => focusAction('start');
$('focus-pause').onclick = () => focusAction('toggle');
$('focus-complete').onclick = () => focusAction('complete');
setInterval(() => { if (state.view === 'FOCUS') renderFocus(); },1000);

let analyticsBusy = false;
async function loadAnalytics() {
  if (!accountReady || analyticsBusy) return;
  analyticsBusy = true; $('analytics-refresh').disabled = true; $('analytics-error').hidden = true;
  try {
    const data = await analyticsApi.get(Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Istanbul');
    $('analytics-range').textContent = `${new Date(`${data.from}T12:00:00`).toLocaleDateString('tr-TR',{day:'numeric',month:'short'})} – ${new Date(`${data.to}T12:00:00`).toLocaleDateString('tr-TR',{day:'numeric',month:'short'})}`;
    $('analytics-completed').textContent = data.completedTasks; $('analytics-focused').textContent = data.focusedMinutes;
    $('analytics-estimated').textContent = data.estimatedMinutes; $('analytics-variance').textContent = data.varianceMinutes > 0 ? `+${data.varianceMinutes}` : data.varianceMinutes;
    const max = Math.max(1,...data.days.map(day => day.focusedMinutes));
    $('analytics-chart').replaceChildren(...data.days.map(day => {
      const column = node('div','chart-day'); const bar = node('div','chart-bar');
      bar.style.height = `${Math.max(3,day.focusedMinutes/max*170)}px`; bar.title = `${day.focusedMinutes} dakika odak`;
      column.append(node('strong','',String(day.focusedMinutes)),bar,node('span','',new Date(`${day.date}T12:00:00`).toLocaleDateString('tr-TR',{weekday:'short'}))); return column;
    }));
  } catch(error) { $('analytics-error').textContent = error.message; $('analytics-error').hidden = false; }
  finally { analyticsBusy = false; $('analytics-refresh').disabled = false; }
}
$('analytics-refresh').onclick = loadAnalytics;

let planningBusy = false;
let planningDraft = null;
let planningInitialized = false;
function setPlanningBusy(busy) {
  planningBusy = busy;
  $('planning-form').querySelectorAll('input,button').forEach(element => element.disabled = busy);
  $('planning-approve').disabled = busy;
}
function renderPlanRows(result) {
  const rows = result.blocks.map(block => {
    const row = node('article','plan-row');
    row.append(node('span','plan-time',`${new Date(block.startAt).toLocaleDateString('tr-TR',{weekday:'short',day:'numeric',month:'short'})} · ${new Date(block.startAt).toLocaleTimeString('tr-TR',{hour:'2-digit',minute:'2-digit'})}`));
    const main = node('div','plan-main'); main.append(node('strong','',block.title),node('span','',`${block.minutes} dakika · ${new Date(block.endAt).toLocaleTimeString('tr-TR',{hour:'2-digit',minute:'2-digit'})} bitiş`)); row.append(main); return row;
  });
  result.unscheduled.forEach(item => {
    const row=node('article','plan-row unscheduled-row');row.append(node('span','plan-time','Yerleşmedi'));
    const main=node('div','plan-main');main.append(node('strong','',item.title),node('span','',item.reason==='MISSING_ESTIMATE'?'Tahmini süre eklenmeli.':'Uygun boş zaman bulunamadı.'));row.append(main);rows.push(row);
  });
  $('planning-results').replaceChildren(...(rows.length ? rows : [node('p','loading','Planlanabilecek açık görev bulunamadı.')]));
  $('planning-summary').textContent = `${result.blocks.length} blok önerildi · ${result.unscheduled.length} görev yerleşmedi`;
  $('planning-approve').hidden = result.blocks.length === 0;
}
async function loadPlannedBlocks() {
  try {
    const blocks = await planningApi.blocks();
    $('planning-existing').replaceChildren();
    if (blocks.length) $('planning-existing').append(node('h3','planned-heading','Onaylanmış planlar'));
    blocks.slice(0,12).forEach(block => {
      const task = state.tasks.find(item => item.id === block.taskId);
      const row=node('article','plan-row');row.append(node('span','plan-time',new Date(block.startAt).toLocaleString('tr-TR',{day:'numeric',month:'short',hour:'2-digit',minute:'2-digit'})));
      const main=node('div','plan-main');main.append(node('strong','',task?.title || `Görev #${block.taskId}`),node('span','',`${new Date(block.endAt).toLocaleTimeString('tr-TR',{hour:'2-digit',minute:'2-digit'})} bitiş`));
      const remove=node('button','icon-button danger','×');remove.title='Planı kaldır';remove.onclick=async()=>{if(!confirm('Bu plan bloğunu kaldırmak istiyor musun?'))return;try{await planningApi.remove(block.id);notify('Plan bloğu kaldırıldı.');await loadPlannedBlocks();}catch(error){notify(error.message,true);}};
      row.append(main,remove);$('planning-existing').append(row);
    });
  } catch(error) { $('planning-error').textContent=error.message;$('planning-error').hidden=false; }
}
async function initializePlanning() {
  if (!planningInitialized) {
    const today=dayKey(new Date());const end=new Date();end.setDate(end.getDate()+6);
    $('planning-from').value=today;$('planning-to').value=dayKey(end);
    try { const settings=await settingsApi.get();$('planning-start').value=settings.workStart.slice(0,5);$('planning-end').value=settings.workEnd.slice(0,5); }
    catch { $('planning-start').value='09:00';$('planning-end').value='18:00'; }
    planningInitialized=true;
  }
  loadPlannedBlocks();
}
$('planning-form').addEventListener('submit',async event=>{
  event.preventDefault();if(planningBusy)return;setPlanningBusy(true);$('planning-error').hidden=true;$('planning-approve').hidden=true;
  try {
    planningDraft=await planningApi.suggest({from:$('planning-from').value,to:$('planning-to').value,workStart:$('planning-start').value,workEnd:$('planning-end').value,timeZone:Intl.DateTimeFormat().resolvedOptions().timeZone||'Europe/Istanbul'});
    renderPlanRows(planningDraft);
  }catch(error){$('planning-error').textContent=error.message;$('planning-error').hidden=false;}
  finally{setPlanningBusy(false);}
});
$('planning-approve').onclick=async()=>{
  if(planningBusy||!planningDraft?.blocks.length)return;setPlanningBusy(true);$('planning-error').hidden=true;
  try{await planningApi.approve(planningDraft.blocks.map(block=>({taskId:block.taskId,startAt:block.startAt,endAt:block.endAt})));notify('Plan onaylandı.');planningDraft=null;$('planning-approve').hidden=true;$('planning-results').replaceChildren(node('p','loading','Plan kaydedildi. Yeni bir öneri oluşturabilirsin.'));await loadPlannedBlocks();}
  catch(error){$('planning-error').textContent=error.message;$('planning-error').hidden=false;}
  finally{setPlanningBusy(false);}
};

function renderAuth() {
  $('planner').hidden = !accountReady;
  $('auth-screen').hidden = accountReady;
  $('auth-title').textContent = accountExists ? 'Tekrar hoş geldin' : 'Gününü birlikte planlayalım';
  $('auth-description').textContent = accountExists ? 'Doğruladığın mail adresinle giriş yap.' : 'Mailinle kaydol, adresini doğrula ve görev hatırlatmalarını al.';
  $('auth-reminders-label').hidden = accountExists;
  $('auth-resend').hidden = !accountExists;
  $('auth-password').autocomplete = accountExists ? 'current-password' : 'new-password';
  $('auth-submit').textContent = accountExists ? 'Giriş yap' : 'Kayıt ol';
  $('auth-submit').disabled = authBusy || (!accountExists && !mailReady);
  $('auth-resend').disabled = authBusy || !mailReady;
  if (!accountExists && !mailReady) {
    $('auth-description').textContent = 'DayFlowJ mail göndericisi henüz kurulmadı. Kurulum tamamlandığında buradan mailinle kayıt olabilirsin.';
  }
}
async function enterPlanner(status) {
  accountReady = true; accountExists = true; mailReady = status.mailReady;
  $('auth-password').value=''; renderAuth();
  await loadTasks(); await pollReminders(); await loadProgress();
}
async function initializeAccount() {
  try {
    const status = await authApi.status(); accountExists=status.accountExists;mailReady=status.mailReady;
    if (status.authenticated) await enterPlanner(status); else renderAuth();
  } catch(error) { $('auth-error').textContent=error.message;$('auth-error').hidden=false;$('auth-submit').disabled=true; }
  const verification = new URLSearchParams(location.search).get('verification');
  if (verification) {
    $('auth-message').hidden=false;
    $('auth-message').textContent=verification==='success' ? 'Mailin doğrulandı. Şimdi giriş yapabilirsin.' : 'Bağlantının süresi dolmuş veya daha önce kullanılmış. Giriş yapmayı dene; gerekirse yeni doğrulama maili iste.';
    history.replaceState(null,'',location.pathname);
  }
}
$('auth-form').addEventListener('submit',async event=>{
  event.preventDefault(); if(authBusy)return;
  authBusy=true;renderAuth();$('auth-error').hidden=true;
  const credentials={email:$('auth-email').value.trim(),password:$('auth-password').value};
  try {
    if(accountExists) await enterPlanner(await authApi.login(credentials));
    else {
      await authApi.register({...credentials,reminders:$('auth-reminders').checked});
      accountExists=true;$('auth-password').value='';$('auth-message').hidden=false;
      $('auth-message').textContent='Doğrulama maili gönderildi. Bu bilgisayarda mailindeki bağlantıyı aç, ardından giriş yap. Spam klasörünü de kontrol edebilirsin.';
    }
  } catch(error) { $('auth-error').textContent=error.message;$('auth-error').hidden=false; }
  finally {authBusy=false;renderAuth();}
});
$('auth-resend').onclick=async()=>{
  if(authBusy || !$('auth-form').reportValidity())return;
  authBusy=true;renderAuth();$('auth-error').hidden=true;
  try {
    await authApi.resend({email:$('auth-email').value.trim(),password:$('auth-password').value});
    $('auth-message').textContent='Adresin henüz doğrulanmadıysa yeni doğrulama maili gönderildi.';$('auth-message').hidden=false;
  }catch(error){$('auth-error').textContent=error.message;$('auth-error').hidden=false;}
  finally{authBusy=false;renderAuth();}
};
$('logout').onclick=async()=>{
  try{await authApi.logout();location.reload();}catch(error){notify(error.message,true);}
};
window.addEventListener('dayflow-auth-required',()=>{
  if(!accountReady)return;
  accountReady=false;state.tasks=[];reminders=[];noteState.notes=[];
  document.querySelectorAll('dialog[open]').forEach(dialog=>dialog.close());
  renderAuth();$('auth-message').textContent='Oturumun sona erdi. Tekrar giriş yap.';$('auth-message').hidden=false;
});
initializeAccount();
