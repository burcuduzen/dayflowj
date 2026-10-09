# Backend

Gönderilen Spring Initializr projesi bu klasöre yerleştirildi. Java 17, Spring Boot, Maven Wrapper, Web MVC, Spring Data JPA, Validation ve H2 bağımlılıkları bulunur. ZIP’teki Spring Boot ve Maven sürümleri korunmuştur.

Uygulama sınıfı `com.burcuduzen.dayflow.DayflowApplication`, proje adı `dayflowj` olarak düzenlendi. Yapılandırma `src/main/resources/application.properties` içindedir. Backend localhost üzerinde çalışır; H2 dosya tabanlıdır.

Çalıştırma komutları [ana README](../README.md#backendi-çalıştırma) içindedir. Özellikler henüz uygulanmadı.

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
