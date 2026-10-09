package com.burcuduzen.dayflow.note;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.OffsetDateTime;
import java.util.List;

@Service @Transactional
public class NoteService {
    private final NoteRepository repository;
    public NoteService(NoteRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true)
    public List<Note> list() { return repository.findAllByOrderByUpdatedAtDescIdDesc(); }
    public Note create(String title, String content) { return repository.save(new Note(title.strip(), content)); }
    public Note update(Long id, String title, String content) {
        Note note = find(id); note.title = title.strip(); note.content = content;
        note.updatedAt = OffsetDateTime.now(); return repository.save(note);
    }
    public void delete(Long id) { repository.delete(find(id)); }
    private Note find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not bulunamadı."));
    }
}
