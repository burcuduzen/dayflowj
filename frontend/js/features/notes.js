import { noteApi } from '../api.js';

export function createNotes({ $, node, notify }) {
  const noteState = {
    notes: [],
    loading: false,
    loaded: false,
    editing: null,
    saving: false,
  };
  async function loadNotes() {
    if (noteState.loading || noteState.saving) return;
    noteState.loading = true;
    $('note-error').hidden = true;
    renderNotes();
    try {
      noteState.notes = await noteApi.list();
      noteState.loaded = true;
    } catch (error) {
      $('note-error').textContent = error.message;
      $('note-error').hidden = false;
    } finally {
      noteState.loading = false;
      renderNotes();
    }
  }
  function renderNotes() {
    const query = $('note-search').value.trim().toLocaleLowerCase('tr');
    const visible = noteState.notes.filter((n) =>
      `${n.title} ${n.content}`.toLocaleLowerCase('tr').includes(query),
    );
    $('note-count').textContent = `${visible.length} not`;
    if (noteState.loading) {
      $('note-list').replaceChildren(
        node('p', 'loading', 'Notlar yükleniyor…'),
      );
      return;
    }
    $('note-list').replaceChildren(
      ...visible.map((note) => {
        const card = node('article', 'note-card');
        const heading = node('div', 'note-card-heading');
        heading.append(node('h3', '', note.title));
        const edit = node('button', 'icon-button', '✎');
        edit.setAttribute('aria-label', `${note.title}: notu düzenle`);
        edit.onclick = () => openNote(note);
        const remove = node('button', 'icon-button danger', '×');
        remove.setAttribute('aria-label', `${note.title}: notu sil`);
        remove.onclick = () => deleteNote(note);
        heading.append(edit, remove);
        card.append(
          heading,
          node('p', 'note-preview', note.content || 'İçerik henüz boş.'),
          node('small', '', new Date(note.updatedAt).toLocaleString('tr-TR')),
        );
        return card;
      }),
    );
    if (!visible.length && noteState.loaded)
      $('note-list').append(
        node(
          'p',
          'loading',
          query
            ? 'Aramanla eşleşen not yok.'
            : 'Yeni not butonuyla ilk notunu ekle.',
        ),
      );
  }
  function openNote(note = null) {
    if (noteState.saving) return;
    noteState.editing = note;
    $('note-form').reset();
    $('note-title').setCustomValidity('');
    $('note-form-error').hidden = true;
    $('note-dialog-title').textContent = note ? 'Notu düzenle' : 'Yeni not';
    $('note-title').value = note?.title || '';
    $('note-content').value = note?.content || '';
    $('note-dialog').showModal();
    $('note-title').focus();
  }
  async function saveNote(event) {
    event.preventDefault();
    if (noteState.saving) return;
    const title = $('note-title').value.trim();
    if (!title) {
      $('note-title').setCustomValidity('Başlık boş olamaz.');
      $('note-title').reportValidity();
      return;
    }
    noteState.saving = true;
    $('note-form')
      .querySelectorAll('button,input,textarea')
      .forEach((el) => {
        el.disabled = true;
      });
    $('note-form-error').hidden = true;
    const body = { title, content: $('note-content').value };
    try {
      const saved = noteState.editing
        ? await noteApi.update(noteState.editing.id, body)
        : await noteApi.create(body);
      noteState.notes = [
        saved,
        ...noteState.notes.filter((n) => n.id !== saved.id),
      ];
      noteState.loaded = true;
      $('note-dialog').close();
      notify('Not kaydedildi.');
      renderNotes();
    } catch (error) {
      $('note-form-error').textContent = error.message;
      $('note-form-error').hidden = false;
    } finally {
      noteState.saving = false;
      $('note-form')
        .querySelectorAll('button,input,textarea')
        .forEach((el) => {
          el.disabled = false;
        });
    }
  }
  async function deleteNote(note) {
    if (!confirm(`“${note.title}” notunu silmek istiyor musun?`)) return;
    try {
      await noteApi.remove(note.id);
      noteState.notes = noteState.notes.filter((n) => n.id !== note.id);
      renderNotes();
      notify('Not silindi.');
    } catch (error) {
      notify(error.message, true);
    }
  }
  $('note-search').addEventListener('input', renderNotes);
  $('note-title').addEventListener('input', () =>
    $('note-title').setCustomValidity(''),
  );
  $('note-form').addEventListener('submit', saveNote);
  ['close-note', 'cancel-note'].forEach(
    (id) =>
      ($(id).onclick = () => {
        if (!noteState.saving) $('note-dialog').close();
      }),
  );
  $('note-dialog').addEventListener('cancel', (event) => {
    if (noteState.saving) event.preventDefault();
  });

  return { loadNotes, renderNotes, openNote };
}
