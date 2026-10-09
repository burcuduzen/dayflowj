# Görev API’si

Backend’i `backend` klasöründe `./mvnw spring-boot:run` ile başlat. Aşağıdaki curl komutlarını **ayrı bir terminalde** çalıştır; backend terminali açık kalsın. Görev kimliklerini yanıtların `id` alanından al. Örneklerde `1` yerine kendi kimliğini yaz.

## Görev oluştur

```sh
curl -i -X POST http://127.0.0.1:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{"title":"Java çalış","description":"Görev API’sini incele","priority":"HIGH","estimatedMinutes":45}'
```

Başarı: 201 Created ve Location başlığı. `title` zorunlu ve en fazla 200 karakterdir. Açıklama en fazla 5000 karakterdir. Öncelik verilmezse MEDIUM, durum TODO olur. `estimatedMinutes` isteğe bağlıdır; verilirse pozitif olmalıdır. `dueDate` isteğe bağlıdır ve saat dilimi içermelidir; örneğin `2026-10-15T18:00:00+03:00`. Geçmiş tarihler gecikmiş görevleri kaydetmek için kabul edilir.

## Listele ve tek görev getir

```sh
curl http://127.0.0.1:8080/api/tasks
curl 'http://127.0.0.1:8080/api/tasks?status=TODO'
curl http://127.0.0.1:8080/api/tasks/1
```

Yeni oluşturulan görevler önce gelir. Durumlar: TODO, IN_PROGRESS, COMPLETED. Öncelikler: LOW, MEDIUM, HIGH.

## Düzenle

```sh
curl -i -X PUT http://127.0.0.1:8080/api/tasks/1 \
  -H 'Content-Type: application/json' \
  -d '{"title":"Java ve Spring çalış","description":"CRUD işlemlerini incele","priority":"HIGH","estimatedMinutes":60,"status":"IN_PROGRESS"}'
```

PUT tam güncellemedir: title, priority ve status zorunludur. İsteğe bağlı alanlar gönderilmezse temizlenir. Oluşturulma zamanı korunur.

## Tamamla veya yeniden aç

```sh
curl -i -X PATCH http://127.0.0.1:8080/api/tasks/1/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"COMPLETED"}'
```

Yeniden açmak için status değerini TODO yap. İlk tamamlamada completedAt kaydedilir; tekrar COMPLETED gönderildiğinde tarih korunur; görev yeniden açıldığında temizlenir.

## Sil

```sh
curl -i -X DELETE http://127.0.0.1:8080/api/tasks/1
```

Başarı: 204 No Content. Olmayan görev için 404 döner.

## Hatalar ve kalıcılık

Geçersiz alanlar, tarih biçimi veya enum değerleri 400; bulunamayan görevler 404 döndürür. Hatalar ProblemDetail JSON biçimindedir. Alan doğrulama hatalarında `errors` alanı bulunur.

Görev oluşturduktan sonra backend’i Ctrl+C ile durdur, aynı backend klasöründen yeniden başlat ve listele. Dosya tabanlı H2 sayesinde görev kalır. Testler ayrı bellek veritabanı kullanır ve kişisel görev verilerini değiştirmez.

Tarayıcıda `/api/tasks` açıldığında JSON görünür. Kullanıcı arayüzü için http://127.0.0.1:8080 adresini aç.

## Tekrarlama ve hatırlatma

Görev oluşturma ve düzenleme isteklerinde `recurrence`: NONE, DAILY, WEEKLY veya MONTHLY; `reminderEnabled`: true/false ve `timeZone`: örneğin Europe/Istanbul alanları kullanılabilir. Tekrarlama için dueDate zorunludur. Yeni görevde recurrence varsayılan NONE, reminderEnabled varsayılan true’dur.

`GET /api/reminders` zamanı gelmiş açık görev hatırlatmalarını getirir. `POST /api/reminders/{taskId}/snooze` gövdesi `{"minutes":5}` (5,10,30); `POST /api/reminders/{taskId}/dismiss` hatırlatmayı kapatır.

Not API’si: GET/POST `/api/notes`, PUT/DELETE `/api/notes/{id}`. Oluşturma/düzenleme gövdesi `{"title":"Not başlığı","content":"İçerik"}`.
