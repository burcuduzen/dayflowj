import { calendarEventApi, planningApi, taskApi } from '../api.js';

export function createCalendar({
  $,
  state,
  eventState,
  node,
  notify,
  dayKey,
  openForm,
  changeStatus,
  loadTasks,
  pollReminders,
  toLocalInput,
  statusLabels,
}) {
  let calendarDate = new Date();
  let selectedDate = new Date();
  let agendaSaving = false;
  function eventsOnDay(date) {
    const start = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    const end = new Date(start);
    end.setDate(end.getDate() + 1);
    const planned = eventState.blocks.flatMap((block) => {
      const task = state.tasks.find((item) => item.id === block.taskId);
      return task
        ? [{ ...block, title: task.title, plannedTaskId: task.id }]
        : [];
    });
    return [...eventState.events, ...planned]
      .filter(
        (event) =>
          new Date(event.startAt) < end && new Date(event.endAt) > start,
      )
      .sort((a, b) => new Date(a.startAt) - new Date(b.startAt));
  }

  function renderCalendar() {
    const mode = $('calendar-mode').value;
    const start = new Date(
      calendarDate.getFullYear(),
      calendarDate.getMonth(),
      calendarDate.getDate(),
    );
    let count = 1;
    if (mode === 'MONTH') {
      start.setDate(1);
      start.setDate(start.getDate() - ((start.getDay() + 6) % 7));
      count =
        Math.ceil(
          (((new Date(
            calendarDate.getFullYear(),
            calendarDate.getMonth(),
            1,
          ).getDay() +
            6) %
            7) +
            new Date(
              calendarDate.getFullYear(),
              calendarDate.getMonth() + 1,
              0,
            ).getDate()) /
            7,
        ) * 7;
    }
    if (mode === 'WEEK') {
      start.setDate(start.getDate() - ((start.getDay() + 6) % 7));
      count = 7;
    }
    $('calendar-title').textContent = calendarDate
      .toLocaleDateString(
        'tr-TR',
        mode === 'MONTH'
          ? { month: 'long', year: 'numeric' }
          : { day: 'numeric', month: 'long', year: 'numeric' },
      )
      .toLocaleUpperCase('tr');
    const grid = $('calendar-grid');
    grid.className = `calendar-grid ${mode.toLowerCase()}`;
    grid.replaceChildren();
    if (mode !== 'DAY')
      ['PZT', 'SAL', 'ÇAR', 'PER', 'CUM', 'CMT', 'PAZ'].forEach((day) =>
        grid.append(node('div', 'calendar-weekday', day)),
      );
    for (let i = 0; i < count; i++) {
      const date = new Date(start);
      date.setDate(start.getDate() + i);
      const key = dayKey(date);
      const cell = node(
        'div',
        `calendar-cell${key === dayKey(new Date()) ? ' current-day' : ''}${key === dayKey(selectedDate) ? ' selected' : ''}${mode === 'MONTH' && date.getMonth() !== calendarDate.getMonth() ? ' other-month' : ''}`,
      );
      const head = node('div', 'calendar-cell-head');
      const select = node(
        'button',
        'day-number',
        mode === 'DAY'
          ? date.toLocaleDateString('tr-TR', {
              weekday: 'long',
              day: 'numeric',
              month: 'long',
            })
          : String(date.getDate()),
      );
      select.setAttribute(
        'aria-label',
        date.toLocaleDateString('tr-TR', {
          weekday: 'long',
          day: 'numeric',
          month: 'long',
          year: 'numeric',
        }),
      );
      select.setAttribute('aria-pressed', String(key === dayKey(selectedDate)));
      select.onclick = () => {
        selectedDate = new Date(date);
        renderCalendar();
      };
      head.append(select);
      const add = node('button', 'calendar-add', '+');
      add.setAttribute('aria-label', `${key}: görev ekle`);
      add.onclick = () => {
        selectedDate = new Date(date);
        openForm(null, date);
      };
      head.append(add);
      cell.append(head);
      const tasks = state.tasks
        .filter((t) => t.dueDate && dayKey(t.dueDate) === key)
        .sort((a, b) => new Date(a.dueDate) - new Date(b.dueDate));
      const fixedEvents = eventsOnDay(date);
      if (mode === 'MONTH') {
        const dots = node('div', 'dots');
        tasks.slice(0, 6).forEach((task) => {
          const dot = node(
            'i',
            `dot ${task.status === 'COMPLETED' ? 'completed' : task.priority}`,
          );
          dot.title = task.title;
          dots.append(dot);
        });
        fixedEvents.slice(0, 3).forEach((event) => {
          const dot = node('i', 'dot event-dot');
          dot.title = event.title;
          dots.append(dot);
        });
        if (tasks.length + fixedEvents.length > 9)
          dots.append(
            node(
              'span',
              'more-dots',
              `+${tasks.length + fixedEvents.length - 9}`,
            ),
          );
        cell.append(dots);
      } else {
        fixedEvents.forEach((event) => {
          const button = node(
            'button',
            'calendar-task calendar-event',
            `${new Date(event.startAt).toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })} ${event.title}`,
          );
          button.onclick = () => openCalendarEvent(event);
          cell.append(button);
        });
        tasks.forEach((task) => {
          const button = node(
            'button',
            `calendar-task ${task.priority}${task.status === 'COMPLETED' ? ' done' : ''}`,
            `${new Date(task.dueDate).toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })} ${task.title}`,
          );
          button.onclick = () => openForm(task);
          cell.append(button);
        });
      }
      grid.append(cell);
    }
    renderAgenda();
  }
  function renderAgenda() {
    $('selected-label').textContent = selectedDate
      .toLocaleDateString('tr-TR', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric',
      })
      .toLocaleUpperCase('tr');
    const tasks = state.tasks
      .filter((t) => t.dueDate && dayKey(t.dueDate) === dayKey(selectedDate))
      .sort((a, b) => new Date(a.dueDate) - new Date(b.dueDate));
    const fixedEvents = eventsOnDay(selectedDate);
    const list = $('agenda-list');
    list.replaceChildren();
    if (state.loading) {
      list.append(node('p', 'empty', 'Görevler yükleniyor…'));
      return;
    }
    if (!state.loaded) {
      list.append(
        node(
          'p',
          'empty',
          'Görevler yüklenemedi. Üstteki Yenile butonuyla tekrar deneyebilirsin.',
        ),
      );
      return;
    }
    if (!tasks.length && !fixedEvents.length)
      list.append(node('p', 'empty', 'Bu gün için görev veya etkinlik yok.'));
    fixedEvents.forEach((item) => {
      const event = node('article', 'event fixed-event');
      event.append(
        node('i', 'stripe event-stripe'),
        node(
          'span',
          'event-time',
          new Date(item.startAt).toLocaleTimeString('tr-TR', {
            hour: '2-digit',
            minute: '2-digit',
          }),
        ),
      );
      const content = node('div', 'event-content');
      const title = node('button', 'event-name', item.title);
      title.onclick = () => openCalendarEvent(item);
      content.append(
        title,
        node(
          'p',
          'event-type',
          item.plannedTaskId ? 'Planlanmış görev' : 'Sabit etkinlik',
        ),
      );
      event.append(content);
      list.append(event);
    });
    tasks.forEach((task) => {
      const event = node(
        'article',
        `event${task.status === 'COMPLETED' ? ' done' : ''}`,
      );
      event.append(
        node('i', `stripe ${task.priority}`),
        node(
          'span',
          'event-time',
          new Date(task.dueDate).toLocaleTimeString('tr-TR', {
            hour: '2-digit',
            minute: '2-digit',
          }),
        ),
      );
      const content = node('div', 'event-content');
      const title = node('button', 'event-name', task.title);
      title.onclick = () => openForm(task);
      content.append(title, node('p', 'event-type', statusLabels[task.status]));
      const check = node(
        'button',
        'task-check' + (task.status === 'COMPLETED' ? ' completed' : ''),
        task.status === 'COMPLETED' ? '✓' : '',
      );
      check.setAttribute(
        'aria-label',
        `${task.title}: ${task.status === 'COMPLETED' ? 'yeniden aç' : 'tamamla'}`,
      );
      check.disabled = state.busy.has(task.id);
      check.onclick = () => changeStatus(task);
      event.append(content, check);
      list.append(event);
    });
  }
  $('agenda-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    if (agendaSaving || !state.loaded) return;
    const title = $('agenda-title').value.trim();
    if (!title) {
      $('agenda-title').setCustomValidity('Görev başlığı boş olamaz.');
      $('agenda-title').reportValidity();
      return;
    }
    const dueDate = new Date(
      `${dayKey(selectedDate)}T${$('agenda-time').value}`,
    );
    agendaSaving = true;
    $('agenda-form')
      .querySelectorAll('input,select,button')
      .forEach((el) => (el.disabled = true));
    $('agenda-error').hidden = true;
    try {
      await taskApi.create({
        title,
        dueDate: dueDate.toISOString(),
        priority: $('agenda-priority').value,
        recurrence: 'NONE',
        reminderEnabled: true,
        timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      });
      $('agenda-title').value = '';
      notify('Görev ajandaya eklendi.');
      await loadTasks();
      await pollReminders();
    } catch (error) {
      $('agenda-error').textContent = error.message;
      $('agenda-error').hidden = false;
    } finally {
      agendaSaving = false;
      $('agenda-form')
        .querySelectorAll('input,select,button')
        .forEach((el) => (el.disabled = false));
    }
  });
  $('agenda-title').addEventListener('input', () =>
    $('agenda-title').setCustomValidity(''),
  );
  function moveCalendar(direction) {
    const mode = $('calendar-mode').value;
    if (mode === 'MONTH') {
      calendarDate.setDate(1);
      calendarDate.setMonth(calendarDate.getMonth() + direction);
    } else
      calendarDate.setDate(
        calendarDate.getDate() + direction * (mode === 'WEEK' ? 7 : 1),
      );
    renderCalendar();
  }
  $('calendar-prev').onclick = () => moveCalendar(-1);
  $('calendar-next').onclick = () => moveCalendar(1);
  $('calendar-today').onclick = () => {
    calendarDate = new Date();
    selectedDate = new Date();
    renderCalendar();
  };
  $('calendar-mode').onchange = renderCalendar;

  async function loadCalendarEvents() {
    if (eventState.loading) return;
    eventState.loading = true;
    try {
      const [events, blocks] = await Promise.all([
        calendarEventApi.list(),
        planningApi.blocks(),
      ]);
      eventState.events = events;
      eventState.blocks = blocks;
      eventState.loaded = true;
      renderCalendar();
    } catch (error) {
      notify(error.message, true);
    } finally {
      eventState.loading = false;
    }
  }
  function openCalendarEvent(event = null) {
    if (event?.plannedTaskId) {
      const task = state.tasks.find((item) => item.id === event.plannedTaskId);
      if (task) openForm(task);
      return;
    }
    if (eventState.saving) return;
    eventState.editing = event;
    $('event-form').reset();
    $('event-error').hidden = true;
    $('event-dialog-title').textContent = event
      ? 'Etkinliği düzenle'
      : 'Yeni etkinlik';
    $('delete-event').hidden = !event;
    if (event) {
      $('event-title').value = event.title;
      $('event-description').value = event.description || '';
      $('event-start').value = toLocalInput(event.startAt);
      $('event-end').value = toLocalInput(event.endAt);
    } else {
      const start = new Date(selectedDate);
      start.setHours(10, 0, 0, 0);
      const end = new Date(start);
      end.setHours(11);
      $('event-start').value = toLocalInput(start);
      $('event-end').value = toLocalInput(end);
    }
    $('event-dialog').showModal();
    $('event-title').focus();
  }
  function setEventBusy(busy) {
    eventState.saving = busy;
    $('event-form')
      .querySelectorAll('input,textarea,button')
      .forEach((element) => (element.disabled = busy));
  }
  $('new-event').onclick = () => openCalendarEvent();
  ['close-event', 'cancel-event'].forEach(
    (id) =>
      ($(id).onclick = () => {
        if (!eventState.saving) $('event-dialog').close();
      }),
  );
  $('event-dialog').addEventListener('cancel', (event) => {
    if (eventState.saving) event.preventDefault();
  });
  $('event-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    if (eventState.saving) return;
    setEventBusy(true);
    $('event-error').hidden = true;
    const data = {
      title: $('event-title').value.trim(),
      description: $('event-description').value.trim() || null,
      startAt: new Date($('event-start').value).toISOString(),
      endAt: new Date($('event-end').value).toISOString(),
    };
    try {
      if (eventState.editing)
        await calendarEventApi.update(eventState.editing.id, data);
      else await calendarEventApi.create(data);
      $('event-dialog').close();
      notify(
        eventState.editing ? 'Etkinlik güncellendi.' : 'Etkinlik eklendi.',
      );
      await loadCalendarEvents();
    } catch (error) {
      $('event-error').textContent = error.message;
      $('event-error').hidden = false;
    } finally {
      setEventBusy(false);
    }
  });
  $('delete-event').onclick = async () => {
    const event = eventState.editing;
    if (
      !event ||
      eventState.saving ||
      !confirm(`“${event.title}” etkinliğini silmek istiyor musun?`)
    )
      return;
    setEventBusy(true);
    try {
      await calendarEventApi.remove(event.id);
      $('event-dialog').close();
      notify('Etkinlik silindi.');
      await loadCalendarEvents();
    } catch (error) {
      $('event-error').textContent = error.message;
      $('event-error').hidden = false;
    } finally {
      setEventBusy(false);
    }
  };

  return { renderCalendar, loadCalendarEvents };
}
