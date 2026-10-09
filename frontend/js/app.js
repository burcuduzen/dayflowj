import { taskApi } from './api.js';

const $ = id => document.getElementById(id);
const state = { tasks: [], view: 'ALL', loading: true, loaded: false, editing: null, saving: false, busy: new Set() };
const statusLabels = { TODO: 'Yapılacak', IN_PROGRESS: 'Devam ediyor', COMPLETED: 'Tamamlandı' };
const priorityLabels = { HIGH: 'Yüksek öncelik', MEDIUM: 'Orta öncelik', LOW: 'Düşük öncelik' };
const viewLabels = {
  ALL: ['Gününü hafiflet.', 'Yapacakların bir yerde. Kontrol sende.', 'Görevlerin'],
  TODAY: ['Bugüne odaklan.', 'Bugün son tarihi gelen açık görevlerin.', 'Bugünkü görevler'],
  UPCOMING: ['Önünü daha net gör.', 'Bugünden sonraki açık görevlerin.', 'Yaklaşan görevler'],
  COMPLETED: ['İlerlediğini gör.', 'Bitirdiğin işler, attığın adımlar.', 'Tamamlanan görevler']
};
let toastTimer;
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
  const percentage = tasks.length ? Math.round(done / tasks.length * 100) : 0;
  $('progress-stat').replaceChildren(document.createTextNode(String(percentage)), node('em', '', '%'));
  $('progress-bar').style.width = `${percentage}%`;
  $('remaining-label').textContent = `${open} açık görev`;
  const [title, description, listTitle] = viewLabels[state.view];
  $('view-title').textContent = title;
  $('view-description').textContent = description;
  $('list-title').textContent = listTitle;
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
  const firstTask = tasks.length === 0 && state.view === 'ALL' && !query && !status && !priority;
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
function openForm(task = null) {
  if (state.saving) return;
  state.editing = task;
  $('task-form').reset(); $('title').setCustomValidity(''); $('form-error').hidden = true;
  $('dialog-title').textContent = task ? 'Görevi düzenle' : 'Yeni görev';
  $('save-task').textContent = task ? 'Değişiklikleri kaydet' : 'Görevi ekle';
  $('status-field').hidden = !task;
  if (task) {
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
  const data = {
    title, description: $('description').value.trim() || null,
    dueDate: $('dueDate').value ? new Date($('dueDate').value).toISOString() : null,
    priority: $('priority').value,
    estimatedMinutes: $('estimatedMinutes').value ? Number($('estimatedMinutes').value) : null
  };
  const editing = state.editing;
  if (editing) data.status = $('status').value;
  setSaving(true);
  try {
    const saved = editing ? await taskApi.update(editing.id, data) : await taskApi.create(data);
    state.tasks = editing ? state.tasks.map(task => task.id === saved.id ? saved : task) : [saved, ...state.tasks];
    $('task-dialog').close();
    notify(editing ? 'Görev güncellendi.' : 'Yeni görevin eklendi.');
    if (!state.loaded) { state.loaded = true; }
  } catch (error) { $('form-error').textContent = error.message; $('form-error').hidden = false; }
  finally { setSaving(false); }
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
}
async function removeTask(task) {
  if (state.busy.has(task.id) || !window.confirm(`“${task.title}” görevini silmek istiyor musun?`)) return;
  state.busy.add(task.id); render();
  try { await taskApi.remove(task.id); state.tasks = state.tasks.filter(t => t.id !== task.id); notify('Görev silindi.'); }
  catch (error) { notify(error.message, true); }
  finally { state.busy.delete(task.id); render(); }
}
$('date-label').textContent = new Date().toLocaleDateString('tr-TR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
$('new-task').addEventListener('click', () => openForm());
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
  render();
}));
setInterval(() => { if (!state.loading) render(); }, 60000);
loadTasks();
