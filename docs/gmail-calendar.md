# Gmail'den DayFlowJ takvimine

Bu ilk sürüm Gmail API üzerinden salt okunur erişim ister. Hesap parolası alınmaz. Mail gönderme özelliğinden bağımsızdır; SMTP ayarlarını değiştirmez.

## Geliştirici kurulumu

1. https://console.cloud.google.com/ adresinde DayFlowJ projesi oluştur.
2. API Library içinden Gmail API'yi etkinleştir.
3. Google Auth Platform'da uygulama bilgilerini ve Audience/Test users bölümünde kendi Gmail hesabını ayarla.
4. Clients bölümünde **Web application** OAuth istemcisi oluştur.
5. Authorized redirect URI olarak tam olarak `http://127.0.0.1:8080/api/gmail/callback` ekle.
6. Client ID ve Client secret değerlerini yalnızca yerel backend terminalinde ortam değişkeni olarak ayarla:

```powershell
$env:DAYFLOW_GMAIL_CLIENT_ID = 'kendi-client-id-degerin'
$env:DAYFLOW_GMAIL_CLIENT_SECRET = 'kendi-client-secret-degerin'
.\mvnw.cmd spring-boot:run
```

Gerçek secret değerlerini GitHub'a veya sohbetlere yapıştırma. İstemci bilgilerini geliştirici bir kez oluşturur; uygulama kullanıcısı sadece Google izin ekranını kullanır.

## Kullanım

`http://127.0.0.1:8080/` adresinden Mailden takvime panelini aç. Gmail'i bağla ve Google izin ekranında hesabını seç. İlk bağlantıda otomatik ekleme kapalıdır. Gmail filtresini örneğin `from:insankaynaklari@firma.com newer_than:7d` yap; otomatik eklemeyi açıp Tercihleri kaydet'e bas. Şimdi kontrol et ile ilk aktarımı dene. Backend açıkken beş dakikada bir kontrol edilir.

Kesinleşmiş Türkçe toplantı metinleri, tek tarih ve tek saat içeriyorsa otomatik eklenebilir. Diğer etkinlik önerileri onay bekler. Tarih bulunamazsa başlangıç/bitiş kullanıcı tarafından seçilir. Tarih ve saat mailin alındığı zamana, Europe/Istanbul saat dilimine göre yorumlanır. Süre belirtilmezse 30 dakika kullanılır; arayüzde belirtilir.

Aynı Gmail hesabı ve mesaj ID'si bir kez işlenir. İptal ve değişiklikler otomatik olarak eski kaydı değiştirmez; onay listesine düşer. Kaynak mail bağlantısı etkinlik açıklamasında yer alır. Kaynak Gmail hesabın birden fazlaysa mail bağlantısını doğru hesapta açmalısın.

## Kayıt ve sınırlamalar

- Etkinlikler ve mail önerileri mevcut yerel H2 dosyasında saklanır. Mail gövdesi ve OAuth tokenları veritabanına yazılmaz.
- OAuth tokenları bellektedir. Backend yeniden başlayınca tekrar bağlanılır; Gmail filtresi ve otomatik ekleme tercihi de sıfırlanır.
- Bağlantıyı kes uygulamanın erişimini durdurur. Google hesabındaki kalıcı izni kaldırmak için Google Hesabı > üçüncü taraf bağlantıları bölümünden DayFlowJ erişimini ayrıca kaldır.
- İlk sürüm yalnızca düz metin Türkçe maillerde bugün/yarın veya YYYY-AA-GG tarihleri ve HH:MM / HH.MM saatleri işler. HTML-only mail gövdeleri, ICS davetleri, haftanın günleri ve genel serbest dil çözümlemesi henüz desteklenmez.
- Bir kontrol en çok 500 mesaj tarar; varsa sonraki sayfa bir sonraki kontrolde işlenir. Aynı e-posta farklı mesaj kimlikleriyle iletilirse farklı öneriler oluşabilir.
- Mailden aktarılan CalendarEvent kayıtları, görevlerin mevcut SMTP hatırlatma işine otomatik bağlanmaz. Etkinlikler için e-posta hatırlatması ayrı geliştirmedir.

## Doğrulama

`backend` içinde `./mvnw test` (Windows: `.\mvnw.cmd test`) çalıştır. MailEventParserTests: mail zamanına göre yarın, belirsiz saat, teklif, iptal/olumsuz onay, birden fazla saat, geçmiş etkinlik ve ilgisiz mail senaryolarını kapsar.

Kaynaklar: https://developers.google.com/identity/protocols/oauth2/web-server ve https://developers.google.com/workspace/gmail/api/guides/list-messages
