# Frontend

HTML, CSS ve JavaScript ile görev yönetimi arayüzü. Harici font, CDN, framework veya npm kurulumu gerekmez.

## Çalıştırma

Proje kökünden:

```sh
cd backend
./mvnw spring-boot:run
```

Tarayıcıda http://127.0.0.1:8080 aç. Maven, frontend dosyalarını backend’in statik kaynaklarına kopyalar. Ayrı frontend sunucusu veya CORS ayarı gerekmez. Dosyalar değiştiğinde backend’i yeniden başlat.

## Dosyalar

- `index.html`: görev listesi, filtreler ve görev formu.
- `css/styles.css`: responsive görünüm.
- `js/api.js`: REST API istemcisi ve hata yanıtları.
- `js/app.js`: görev ekleme, düzenleme, silme, durum değiştirme, arama ve filtreler.

Bugün ve Yaklaşanlar görünümleri açık görevleri son tarihlerine göre kullanıcının yerel saat diliminde filtreler. Görev formundaki yerel tarih API’ye ISO saat dilimli tarih olarak gönderilir. Tamamlanma oranı tüm görevler üzerinden hesaplanır. Görev içerikleri düz metin olarak gösterilir.

Notlar, takvim etkinlikleri ve focus mode henüz uygulanmadı.
