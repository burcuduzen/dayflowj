import { taskApi, noteApi, reminderApi } from './api.js';

const $ = id => document.getElementById(id);
const state = { tasks: [], view: 'CALENDAR', loading: true, loaded: false, editing: null, saving: false, busy: new Set() };
const statusLabels = { TODO: 'Yapılacak', IN_PROGRESS: 'Devam ediyor', COMPLETED: 'Tamamlandı' };
const priorityLabels = { HIGH: 'Yüksek öncelik', MEDIUM: 'Orta öncelik', LOW: 'Düşük öncelik' };
const viewLabels = {
  PLAN: ['Bugünkü planım', 'Bugün, gecikmiş ve henüz tarihlendirmediğin görevler.', 'Sıradaki işler'],
  CALENDAR: ['Takvimin, görev haritan.', 'Bir gün seç, görevlerini gözden geçir ve sıradaki adımını ekle.', 'Takvim'],
  NOTES: ['Not defteri', 'Fikirlerini ve çalışma notlarını bir yerde tut.', 'Not defteri'],
  ALL: ['Tüm görevler', 'Açık ve tamamlanmış bütün görevlerin.', 'Görevlerin'],
  TODAY: ['Bugüne odaklan.', 'Bugün son tarihi gelen açık görevlerin.', 'Bugünkü görevler'],
  UPCOMING: ['Önünü daha net gör.', 'Bugünden sonraki açık görevlerin.', 'Yaklaşan görevler'],
  COMPLETED: ['İlerlediğini gör.', 'Bitirdiğin işler, attığın adımlar.', 'Tamamlanan görevler']
};
let toastTimer;
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
  $('tasks-panel').hidden = notesView || calendarView;
  $('notes-panel').hidden = !notesView;
  $('calendar-panel').hidden = !calendarView;
  $('calendar-workspace').hidden = !calendarView;
  document.querySelector('.metrics').hidden = notesView;
  $('new-task').textContent = notesView ? '+ Yeni not' : '+ Yeni görev';
  if (calendarView) renderCalendar();
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
  if (savedSuccessfully) await loadTasks();
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
  await loadTasks(); await pollReminders();
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
  render();
}));
setInterval(() => { if (!state.loading) render(); }, 60000);
loadTasks();

const noteState = { notes: [], loading: false, loaded: false, editing: null, saving: false };
let reminders = [];
let reminderPolling = false;
let remindersBusy = new Set();
const notified = new Set();
try { JSON.parse(sessionStorage.getItem('dayflow-notified') || '[]').forEach(key => notified.add(key)); } catch { /* storage is optional */ }

async function loadNotes() {
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
    if (mode === 'MONTH') {
      const dots = node('div','dots');
      tasks.slice(0,6).forEach(task => { const dot = node('i',`dot ${task.status === 'COMPLETED' ? 'completed' : task.priority}`);dot.title=task.title;dots.append(dot); });
      if (tasks.length > 6) dots.append(node('span','more-dots',`+${tasks.length-6}`));
      cell.append(dots);
    } else tasks.forEach(task => {
      const button = node('button',`calendar-task ${task.priority}${task.status === 'COMPLETED' ? ' done' : ''}`, `${new Date(task.dueDate).toLocaleTimeString('tr-TR',{hour:'2-digit',minute:'2-digit'})} ${task.title}`);
      button.onclick = () => openForm(task);cell.append(button);
    });
    grid.append(cell);
  }
  renderAgenda();
}
function renderAgenda() {
  $('selected-label').textContent = selectedDate.toLocaleDateString('tr-TR',{weekday:'long',day:'numeric',month:'long',year:'numeric'}).toLocaleUpperCase('tr');
  const tasks = state.tasks.filter(t => t.dueDate && dayKey(t.dueDate) === dayKey(selectedDate)).sort((a,b)=>new Date(a.dueDate)-new Date(b.dueDate));
  const list = $('agenda-list');list.replaceChildren();
  if (state.loading) { list.append(node('p','empty','Görevler yükleniyor…'));return; }
  if (!state.loaded) { list.append(node('p','empty','Görevler yüklenemedi. Üstteki Yenile butonuyla tekrar deneyebilirsin.'));return; }
  if (!tasks.length) list.append(node('p','empty','Bu gün için tarihli görev yok. Aşağıdan ilk görevini ekle.'));
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
