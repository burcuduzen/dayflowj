import { gmailApi } from '../api.js';

export function createGmail({ $, node, notify, loadCalendarEvents }) {
  let gmailBusy = false;
  function gmailFailure(error) {
    $('gmail-error').textContent = error.message;
    $('gmail-error').hidden = false;
  }
  async function gmailAction(action) {
    if (gmailBusy) return;
    gmailBusy = true;
    $('gmail-error').hidden = true;
    $('gmail-dialog')
      .querySelectorAll('button,input')
      .forEach((e) => (e.disabled = true));
    try {
      await action();
    } catch (error) {
      gmailFailure(error);
    } finally {
      gmailBusy = false;
      $('gmail-dialog')
        .querySelectorAll('button,input')
        .forEach((e) => (e.disabled = false));
    }
  }
  async function renderGmail() {
    const status = await gmailApi.status();
    $('gmail-status').textContent = status.connected
      ? `Bağlı: ${status.account} · Her 5 dakikada kontrol edilir.`
      : status.configured
        ? 'Gmail hesabını bağlayarak başlayabilirsin.'
        : 'Google bağlantısı henüz hazır değil. Geliştirici OAuth ayarlarını tamamlamalı.';
    $('gmail-connect').hidden = status.connected;
    $('gmail-preferences').hidden = !status.connected;
    $('gmail-disconnect').hidden = !status.connected;
    $('gmail-query').value = status.query;
    $('gmail-auto').checked = status.automatic;
    if (status.error) gmailFailure(new Error(status.error));
    $('gmail-candidates').replaceChildren();
    if (!status.connected) return;
    const candidates = await gmailApi.candidates();
    if (!candidates.length) {
      $('gmail-candidates').append(
        node('p', 'hint', 'Henüz etkinlik önerisi yok.'),
      );
      return;
    }
    for (const c of candidates) {
      const card = node('article', 'note-card');
      card.append(node('h3', '', c.title), node('p', 'hint', c.reason));
      const link = node('a', 'text-button', 'Kaynak maili aç');
      link.href = `https://mail.google.com/mail/u/0/#all/${encodeURIComponent(c.messageId)}`;
      link.target = '_blank';
      link.rel = 'noopener noreferrer';
      card.append(link);
      if (c.status === 'PENDING') {
        const form = node('form', 'add-form'),
          title = node('input'),
          start = node('input'),
          end = node('input');
        title.value = c.title;
        title.maxLength = 200;
        title.required = true;
        start.type = end.type = 'datetime-local';
        start.required = end.required = true;
        const localValue = (value) => {
          if (!value) return '';
          const d = new Date(value);
          return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}T${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
        };
        start.value = localValue(c.startAt);
        end.value = localValue(c.endAt);
        for (const [label, input] of [
          ['Başlık', title],
          ['Başlangıç', start],
          ['Bitiş', end],
        ]) {
          const field = node('label', 'field', label);
          field.append(input);
          form.append(field);
        }
        const approve = node('button', 'primary', 'Takvime ekle'),
          dismiss = node('button', 'secondary', 'Öneriyi kaldır');
        approve.type = 'submit';
        dismiss.type = 'button';
        dismiss.onclick = () =>
          gmailAction(async () => {
            await gmailApi.dismiss(c.id);
            await renderGmail();
          });
        form.append(approve, dismiss);
        form.onsubmit = (event) => {
          event.preventDefault();
          gmailAction(async () => {
            await gmailApi.approve(c.id, {
              title: title.value.trim(),
              startAt: new Date(start.value).toISOString(),
              endAt: new Date(end.value).toISOString(),
            });
            await renderGmail();
            await loadCalendarEvents();
            notify('Maildeki etkinlik takvime eklendi.');
          });
        };
        card.append(form);
      } else
        card.append(
          node(
            'p',
            'hint',
            c.status === 'ADDED' ? '✓ Takvime eklendi' : 'Öneri kaldırıldı',
          ),
        );
      $('gmail-candidates').append(card);
    }
  }
  $('open-gmail').onclick = () => {
    $('gmail-dialog').showModal();
    gmailAction(renderGmail);
  };
  $('close-gmail').onclick = () => {
    $('gmail-dialog').close();
  };
  $('gmail-connect').onclick = () =>
    gmailAction(async () => {
      const result = await gmailApi.connect();
      window.location.assign(result.url);
      notify('Google iznini tarayıcıda tamamlayıp DayFlowJ’ye dön.');
    });
  $('gmail-preferences').onsubmit = (event) => {
    event.preventDefault();
    gmailAction(async () => {
      await gmailApi.preferences({
        query: $('gmail-query').value,
        automatic: $('gmail-auto').checked,
      });
      await renderGmail();
      notify('Mail aktarım tercihleri kaydedildi.');
    });
  };
  $('gmail-sync').onclick = () =>
    gmailAction(async () => {
      const r = await gmailApi.sync();
      await renderGmail();
      await loadCalendarEvents();
      notify(
        `${r.added} etkinlik eklendi, ${r.pending} öneri onay bekliyor.${r.more ? ' Kalan mailler sonraki kontrolde işlenecek.' : ''}`,
      );
    });
  $('gmail-disconnect').onclick = () =>
    gmailAction(async () => {
      await gmailApi.disconnect();
      await renderGmail();
      notify('DayFlowJ Gmail bağlantısı kesildi.');
    });
  const gmailResult = new URLSearchParams(window.location.search).get('gmail');
  if (gmailResult) {
    history.replaceState(null, '', window.location.pathname);
    $('gmail-dialog').showModal();
    gmailAction(async () => {
      await renderGmail();
      if (gmailResult === 'failed')
        throw new Error('Google bağlantısı tamamlanamadı; yeniden bağlan.');
      if (gmailResult === 'cancelled')
        throw new Error('Google izni verilmedi.');
    });
  }

  window.addEventListener('focus', () => {
    if ($('gmail-dialog').open) gmailAction(renderGmail);
  });
  setInterval(() => {
    if ($('gmail-dialog').open && !gmailBusy && !$('gmail-connect').hidden)
      gmailAction(renderGmail);
  }, 3000);
}
