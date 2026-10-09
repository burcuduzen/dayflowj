# DayFlowJ mimarisi

## Amaç ve kapsam

Tek kullanıcılı, yerel çalışan bir üretkenlik uygulaması. Notlar, görevler, takvim, hatırlatıcılar, odaklanma oturumları, verimlilik analizi ve otomatik planlama içerir. Bu belge hedef mimariyi tarif eder; görev API’si ve görev arayüzü uygulanmıştır, diğer özellikler henüz uygulanmamıştır. Spring Boot başlangıç projesi backend klasörüne eklenmiştir.

## Çalışma düzeni

Frontend (HTML/CSS/JavaScript) REST API üzerinden tek Spring Boot backend ile iletişim kurar. Backend Spring Data JPA aracılığıyla dosya tabanlı H2 veritabanına erişir. Frontend kaynakları repoda ayrı klasördedir; Maven bu dosyaları backend’in statik kaynaklarına kopyalar. Tarayıcı arayüz ve API için aynı yerel adresi kullanır. Tek backend süreci yeterlidir. Backend yalnızca yerel arayüzde dinleyecek şekilde yapılandırılmalıdır.

## Backend katmanları

| Katman | Sorumluluk |
|---|---|
| Controller | HTTP isteğini alır, girdiyi doğrular ve yanıt döndürür |
| Service | İş kurallarını ve işlemler arasındaki koordinasyonu uygular |
| Repository | Veritabanına erişir |
| Entity | Kalıcı veri kaydını temsil eder |
| DTO | API istek ve yanıtlarını temsil eder |

Paketler özelliğe göre ayrılır. Controller doğrudan Repository kullanmaz. API yanıtları Entity yerine DTO üzerinden oluşturulur.

## Modüller

| Modül | İşlev |
|---|---|
| task | Görevler, öncelikler, son tarihler ve durumlar |
| note | Notlar, arama ve isteğe bağlı görev bağlantısı |
| calendar | Sabit etkinlikler ve planlanan görevlerin takvimde gösterimi |
| reminder | Hatırlatma zamanı, erteleme ve bildirim durumu |
| focus | Odaklanma oturumları, molalar ve gerçekleşen süre |
| planning | Boş zamanlara göre onaylanabilir plan önerileri |
| analytics | Kaydedilmiş görev ve oturum verilerinden raporlar |

## Veri modeli

| Kayıt | Temel alanlar |
|---|---|
| Task | id, title, description, dueDate, priority, status, estimatedMinutes, completedAt |
| Note | id, title, content, createdAt, updatedAt, taskId (isteğe bağlı) |
| CalendarEvent | id, title, startAt, endAt |
| Reminder | id, taskId, remindAt, status |
| FocusSession | id, taskId (isteğe bağlı), startAt, endAt, focusedSeconds, status |
| PlannedBlock | id, taskId, startAt, endAt |
| UserSettings | çalışma saatleri, odak süresi, mola süresi ve saat dilimi |

Bir görevin birden fazla notu, hatırlatıcısı, odaklanma oturumu ve planlanan zaman bloğu olabilir. Planlanan süre ve gerçekleşen süre ayrı tutulur. Görev silme işleminde bağlı kayıtlar için açık bir silme veya arşivleme politikası belirlenecek.

## İlk API kapsamı

| Yöntem | Yol | İşlem |
|---|---|---|
| POST | /api/tasks | Görev oluştur |
| GET | /api/tasks | Görevleri listele |
| GET | /api/tasks/{id} | Tek görev getir |
| PUT | /api/tasks/{id} | Görevi güncelle; tamamlanma durumunu da değiştirebilir |
| PATCH | /api/tasks/{id}/status | Görevi tamamla veya yeniden aç |
| DELETE | /api/tasks/{id} | Görevi sil |

Başlık boş olamaz, tahmini süre pozitif olmalıdır. Geçersiz girdiler 400, bulunamayan kayıtlar 404 döndürür. Görev tamamlandığında completedAt kaydedilir; yeniden açıldığında temizlenir.

## Akıllı özellikler

İlk sürüm kurallara dayalıdır; harici yapay zekâ servisi gerektirmez.

- Planlama: çalışma saatlerinden sabit etkinlikleri çıkarır, görevleri son tarih, öncelik ve tahmini süreye göre yerleştirir. Kullanıcı öneriyi onaylar. Sığmayan görevler açıkça gösterilir.
- Hatırlatma: son tarih ve tahmini süreye göre öneri üretir. Focus oturumunda acil olmayan bildirimler ertelenebilir.
- Analiz: tamamlanma tarihleri ve gerçek odaklanma kayıtlarından günlük süreleri, tamamlanan görevleri ve planlanan/gerçekleşen süre farkını hesaplar.

## Yerel çalışma sınırları

Backend çalışmıyorsa hatırlatma zamanlayıcısı işlemez. Tarayıcı bildirimleri için frontend'in açık olması ve bildirim izni gerekir. Uygulama yeniden açıldığında kaçırılan hatırlatmaları ele alma politikası uygulanmalıdır. Focus sayacı yalnızca ekran sayacına güvenmez; başlangıç zamanı kaydedilir ve mola süreleri ayrı tutulur.

## Uygulama aşamaları

1. Spring Boot/Maven altyapısı, dosya tabanlı H2 ve görev CRUD API'si.
2. Görev arayüzü; kapatıp açınca verilerin kalıcılığını doğrulama.
3. Notlar ve takvim.
4. Hatırlatma zamanlayıcısı ve frontend bildirimleri.
5. Focus oturumları ve mola kayıtları.
6. Verimlilik raporları.
7. Otomatik planlama ve akıllı hatırlatma kuralları.

Veritabanı dosyaları, derleme çıktıları, yerel ortam ayarları ve şifreler repoya eklenmez.
