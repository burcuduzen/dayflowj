# Backend

Bu klasör Java + Spring Boot backend için ayrılmıştır. Henüz Maven/Spring Boot projesi oluşturulmadı.

İlk adım: Spring Initializr üzerinden Maven projesi oluştur; Spring Web, Spring Data JPA, Validation ve H2 Database bağımlılıklarını ekle. Paket adı `com.burcuduzen.dayflow` olacak. Oluşan `pom.xml`, Maven Wrapper ve uygulama sınıfını bu klasöre yerleştir.

## Paket düzeni

`src/main/java/com/burcuduzen/dayflow/` altında her özellik kendi paketinde bulunur:

- `task`: görev yönetimi; `dto` API istek ve yanıtları.
- `note`: not yönetimi.
- `calendar`: sabit etkinlikler ve takvim görünümü için veri.
- `reminder`: hatırlatma zamanları ve bildirim durumu.
- `focus`: odaklanma oturumları ve gerçekleşen süre.
- `planning`: görevler için plan önerileri.
- `analytics`: görev ve odaklanma kayıtlarından raporlar.
- `common/config`: ortak yapılandırmalar.
- `common/exception`: ortak hata yönetimi.

Kaynak dosyaları `src/main/resources/`, backend testleri `src/test/java/com/burcuduzen/dayflow/` altında bulunur.

İlk tamamlanacak modül `task`: Task, TaskController, TaskService, TaskRepository ve DTO sınıfları. H2 dosya tabanlı yapılandırılacak; yerel veritabanı dosyaları Git'e eklenmeyecek.
