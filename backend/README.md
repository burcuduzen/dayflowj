# Backend

Gönderilen Spring Initializr projesi bu klasöre yerleştirildi. Java 17, Spring Boot, Maven Wrapper, Web MVC, Spring Data JPA, Validation ve H2 bağımlılıkları bulunur. ZIP’teki Spring Boot ve Maven sürümleri korunmuştur.

Uygulama sınıfı `com.burcuduzen.dayflow.DayflowApplication`, proje adı `dayflowj` olarak düzenlendi. Yapılandırma `src/main/resources/application.properties` içindedir. Backend localhost üzerinde çalışır; H2 dosya tabanlıdır.

Çalıştırma komutları [ana README](../README.md#backendi-çalıştırma) içindedir. Görev, not, hatırlatıcı, odak oturumu, verimlilik analizi, sabit takvim etkinliği, otomatik plan önerisi ve kalıcı plan bloğu API’leri uygulandı.

## Paket düzeni

`src/main/java/com/burcuduzen/dayflow/` altında her özellik kendi paketinde bulunur:

- `task`: görev yönetimi; `dto` API istek ve yanıtları.
- `note`: not yönetimi.
- `focus`: odak oturumlarını başlatma, duraklatma, devam ettirme, tamamlama ve süre kaydı.
- `analytics`: görev tamamlamaları ve odak kayıtlarından günlük verimlilik özeti.
- `calendar`: sabit etkinlikler ve tarih aralığı sorguları.
- `reminder`: hatırlatma zamanları ve bildirim durumu.
- `planning`: görevleri uygun boş zamanlara yerleştiren öneriler ve onaylanmış plan blokları.
- `common/config`: ortak yapılandırmalar.
- `common/exception`: ortak hata yönetimi.

Kaynak dosyaları `src/main/resources/`, backend testleri `src/test/java/com/burcuduzen/dayflow/` altında bulunur.

Uygulanan modül `task`: Task, TaskController, TaskService, TaskRepository ve DTO sınıfları. H2 dosya tabanlı yapılandırıldı; yerel veritabanı dosyaları Git'e eklenmeyecek.

API örnekleri: [Görev API’si](../docs/task-api.md).

Frontend dosyaları Maven resources ayarıyla statik kaynaklara kopyalanır. Backend’i başlattıktan sonra http://127.0.0.1:8080 adresinden görev arayüzüne ulaşılır.
