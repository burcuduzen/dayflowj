import { emailApi } from '../api.js';

export function createEmail({ $, notify }) {
  let emailBusy = false;
  let emailConfigured = false;
  let emailSavedRecipient = '';
  let emailVerified = false;
  function setEmailBusy(busy) {
    emailBusy = busy;
    $('email-form')
      .querySelectorAll('input,button')
      .forEach((el) => (el.disabled = busy));
    if (!busy) {
      $('test-email').disabled = !emailConfigured || !emailVerified;
      $('save-email').disabled = !emailConfigured && !emailVerified;
    }
  }
  function renderEmailSettings(settings) {
    emailConfigured = settings.smtpConfigured;
    emailSavedRecipient = settings.recipient || '';
    emailVerified = settings.verified;
    $('email-recipient').value =
      settings.pendingRecipient || emailSavedRecipient;
    $('email-enabled').checked = settings.enabled;
    $('email-enabled-label').hidden = !emailVerified;
    $('test-email').hidden = !emailVerified;
    $('save-email').textContent =
      emailVerified && !settings.pendingRecipient
        ? 'Kaydet'
        : 'Doğrulama maili gönder';
    $('email-connection').textContent = !emailConfigured
      ? 'Maille hatırlatma henüz hazır değil. Görevlerini kullanmaya devam edebilirsin.'
      : settings.pendingRecipient
        ? 'Doğrulama mailini gönderdik. Gelen bağlantıya bas; gerekirse bir dakika sonra yeniden gönder.'
        : emailVerified
          ? `Doğrulanmış adres: ${emailSavedRecipient}`
          : 'Mail adresini doğrula, yaklaşan görevlerin için bildirim al.';
  }
  async function openEmail() {
    $('email-dialog').showModal();
    $('email-error').hidden = true;
    setEmailBusy(true);
    try {
      renderEmailSettings(await emailApi.get());
    } catch (error) {
      $('email-error').textContent = error.message;
      $('email-error').hidden = false;
    } finally {
      setEmailBusy(false);
    }
  }
  $('open-email').onclick = openEmail;
  $('close-email').onclick = () => {
    if (!emailBusy) $('email-dialog').close();
  };
  $('email-dialog').addEventListener('cancel', (event) => {
    if (emailBusy) event.preventDefault();
  });
  $('email-recipient').addEventListener('input', () => {
    const unchanged =
      emailVerified &&
      $('email-recipient').value.trim().toLowerCase() === emailSavedRecipient;
    $('save-email').textContent = unchanged
      ? 'Kaydet'
      : 'Doğrulama maili gönder';
    $('save-email').disabled = emailBusy || (!emailConfigured && !unchanged);
  });
  $('email-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    if (emailBusy) return;
    const recipient = $('email-recipient').value.trim().toLowerCase();
    setEmailBusy(true);
    $('email-error').hidden = true;
    try {
      if (emailVerified && recipient === emailSavedRecipient) {
        renderEmailSettings(
          await emailApi.save({ enabled: $('email-enabled').checked }),
        );
        notify('Mail tercihin kaydedildi.');
        $('email-dialog').close();
      } else {
        await emailApi.connect(recipient);
        renderEmailSettings(await emailApi.get());
        notify(
          'Doğrulama maili gönderildi. Gelen kutunu veya spam klasörünü kontrol et.',
        );
      }
    } catch (error) {
      $('email-error').textContent = error.message;
      $('email-error').hidden = false;
    } finally {
      setEmailBusy(false);
    }
  });
  $('test-email').onclick = async () => {
    if (emailBusy) return;
    setEmailBusy(true);
    $('email-error').hidden = true;
    try {
      await emailApi.test();
      notify('Test maili gönderildi.');
    } catch (error) {
      $('email-error').textContent = error.message;
      $('email-error').hidden = false;
    } finally {
      setEmailBusy(false);
    }
  };
  const emailVerification = new URLSearchParams(location.search).get(
    'emailVerification',
  );
  if (emailVerification) {
    history.replaceState(null, '', location.pathname);
    openEmail().then(() => {
      if (emailVerification === 'success')
        notify('Mailin doğrulandı. Görev hatırlatmaları açıldı.');
      else {
        $('email-error').textContent =
          'Bağlantı geçersiz veya süresi dolmuş. Yeni doğrulama maili gönderebilirsin.';
        $('email-error').hidden = false;
      }
    });
  }
}
