import { planningApi, settingsApi } from '../api.js';

export function createPlanning({
  $,
  state,
  eventState,
  node,
  notify,
  dayKey,
  renderCalendar,
}) {
  let planningBusy = false;
  let planningDraft = null;
  let planningInitialized = false;
  function setPlanningBusy(busy) {
    planningBusy = busy;
    $('planning-form')
      .querySelectorAll('input,button')
      .forEach((element) => (element.disabled = busy));
    $('planning-approve').disabled = busy;
  }
  function renderPlanRows(result) {
    const rows = result.blocks.map((block) => {
      const row = node('article', 'plan-row');
      row.append(
        node(
          'span',
          'plan-time',
          `${new Date(block.startAt).toLocaleDateString('tr-TR', { weekday: 'short', day: 'numeric', month: 'short' })} · ${new Date(block.startAt).toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })}`,
        ),
      );
      const main = node('div', 'plan-main');
      main.append(
        node('strong', '', block.title),
        node(
          'span',
          '',
          `${block.minutes} dakika · ${new Date(block.endAt).toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })} bitiş`,
        ),
      );
      row.append(main);
      return row;
    });
    result.unscheduled.forEach((item) => {
      const row = node('article', 'plan-row unscheduled-row');
      row.append(node('span', 'plan-time', 'Yerleşmedi'));
      const main = node('div', 'plan-main');
      main.append(
        node('strong', '', item.title),
        node(
          'span',
          '',
          item.reason === 'MISSING_ESTIMATE'
            ? 'Tahmini süre eklenmeli.'
            : 'Uygun boş zaman bulunamadı.',
        ),
      );
      row.append(main);
      rows.push(row);
    });
    $('planning-results').replaceChildren(
      ...(rows.length
        ? rows
        : [node('p', 'loading', 'Planlanabilecek açık görev bulunamadı.')]),
    );
    $('planning-summary').textContent =
      `${result.blocks.length} blok önerildi · ${result.unscheduled.length} görev yerleşmedi`;
    $('planning-approve').hidden = result.blocks.length === 0;
  }
  async function loadPlannedBlocks() {
    try {
      const blocks = await planningApi.blocks();
      eventState.blocks = blocks;
      if (state.view === 'CALENDAR') renderCalendar();
      $('planning-existing').replaceChildren();
      if (blocks.length)
        $('planning-existing').append(
          node('h3', 'planned-heading', 'Onaylanmış planlar'),
        );
      blocks.slice(0, 12).forEach((block) => {
        const task = state.tasks.find((item) => item.id === block.taskId);
        const row = node('article', 'plan-row');
        row.append(
          node(
            'span',
            'plan-time',
            new Date(block.startAt).toLocaleString('tr-TR', {
              day: 'numeric',
              month: 'short',
              hour: '2-digit',
              minute: '2-digit',
            }),
          ),
        );
        const main = node('div', 'plan-main');
        main.append(
          node('strong', '', task?.title || `Görev #${block.taskId}`),
          node(
            'span',
            '',
            `${new Date(block.endAt).toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })} bitiş`,
          ),
        );
        const remove = node('button', 'icon-button danger', '×');
        remove.title = 'Planı kaldır';
        remove.onclick = async () => {
          if (!confirm('Bu plan bloğunu kaldırmak istiyor musun?')) return;
          try {
            await planningApi.remove(block.id);
            notify('Plan bloğu kaldırıldı.');
            await loadPlannedBlocks();
          } catch (error) {
            notify(error.message, true);
          }
        };
        row.append(main, remove);
        $('planning-existing').append(row);
      });
    } catch (error) {
      $('planning-error').textContent = error.message;
      $('planning-error').hidden = false;
    }
  }
  async function initializePlanning() {
    if (!planningInitialized) {
      const today = dayKey(new Date());
      const end = new Date();
      end.setDate(end.getDate() + 6);
      $('planning-from').value = today;
      $('planning-to').value = dayKey(end);
      try {
        const settings = await settingsApi.get();
        $('planning-start').value = settings.workStart.slice(0, 5);
        $('planning-end').value = settings.workEnd.slice(0, 5);
      } catch {
        $('planning-start').value = '09:00';
        $('planning-end').value = '18:00';
      }
      planningInitialized = true;
    }
    loadPlannedBlocks();
  }
  $('planning-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    if (planningBusy) return;
    setPlanningBusy(true);
    $('planning-error').hidden = true;
    $('planning-approve').hidden = true;
    try {
      planningDraft = await planningApi.suggest({
        from: $('planning-from').value,
        to: $('planning-to').value,
        workStart: $('planning-start').value,
        workEnd: $('planning-end').value,
        timeZone:
          Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Istanbul',
      });
      renderPlanRows(planningDraft);
    } catch (error) {
      $('planning-error').textContent = error.message;
      $('planning-error').hidden = false;
    } finally {
      setPlanningBusy(false);
    }
  });
  $('planning-approve').onclick = async () => {
    if (planningBusy || !planningDraft?.blocks.length) return;
    setPlanningBusy(true);
    $('planning-error').hidden = true;
    try {
      await planningApi.approve(
        planningDraft.blocks.map((block) => ({
          taskId: block.taskId,
          startAt: block.startAt,
          endAt: block.endAt,
        })),
      );
      notify('Plan onaylandı.');
      planningDraft = null;
      $('planning-approve').hidden = true;
      $('planning-results').replaceChildren(
        node(
          'p',
          'loading',
          'Plan kaydedildi. Yeni bir öneri oluşturabilirsin.',
        ),
      );
      await loadPlannedBlocks();
    } catch (error) {
      $('planning-error').textContent = error.message;
      $('planning-error').hidden = false;
    } finally {
      setPlanningBusy(false);
    }
  };

  return { initializePlanning, loadPlannedBlocks };
}
