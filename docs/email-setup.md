# Yerel e-posta kurulumu

DayFlowJ, SMTP üzerinden e-posta gönderir. Gönderici hesabın SMTP erişimine izin vermelidir; bazı sağlayıcılar uygulama parolası, kurum izni veya OAuth gerektirir. Bu sürüm kullanıcı adı ve parola/uygulama parolası ile SMTP destekler; OAuth veya gelen kutusu okuma içermez.

## Bağlantı bilgileri

Sağlayıcından SMTP sunucusu, port, TLS yöntemi ve uygulama parolası bilgilerini al. Hesap parolanı veya uygulama parolanı GitHub’a, sohbet mesajına veya arayüze yazma. Kimlik bilgileri yalnızca backend’in yerel ortam değişkenlerinde bulunur.

Backend terminalinde çalışan uygulamayı Ctrl+C ile durdur. Aşağıdaki değerleri kendi sağlayıcının bilgileriyle değiştir:

```sh
export DAYFLOW_MAIL_HOST='smtp.sunucun.com'
export DAYFLOW_MAIL_PORT='587'
export DAYFLOW_MAIL_USERNAME='gonderici@ornek.com'
export DAYFLOW_MAIL_FROM='gonderici@ornek.com'
export DAYFLOW_MAIL_SSL='false'
```

Parolayı terminal geçmişine yazmadan gir:

```sh
read -s DAYFLOW_MAIL_PASSWORD
export DAYFLOW_MAIL_PASSWORD
```

İlk komuttan sonra parolanı yazıp Enter’a bas; yazdıkların görünmez. Sonra aynı terminalde backend klasöründen `./mvnw spring-boot:run` çalıştır. Yeni bir terminal bu değişkenleri kendiliğinden devralmaz. Uygulama `.env` dosyasını otomatik okumaz.

Port 587 için STARTTLS (`DAYFLOW_MAIL_SSL=false`), sağlayıcı 465/implicit TLS istiyorsa port 465 ve `DAYFLOW_MAIL_SSL=true` kullan. TLS kapalı bağlantı desteklenmez. Doğru değerler sağlayıcının belgelerinden alınmalıdır.

## Arayüzde etkinleştirme

1. Üstte E-posta butonuna bas.
2. Hatırlatma alacağın adresi yaz, e-posta gönder seçeneğini işaretle ve Kaydet’e bas.
3. E-posta ayarlarını yeniden açıp Test maili gönder’e bas. Spam klasörünü de kontrol et.

SMTP ayarlarının hazır görünmesi kimlik doğrulamasının başarılı olduğunu kanıtlamaz; gerçek gönderim test mailiyle kontrol edilir. SMTP eksikse tercih kaydedilebilir ancak e-posta gönderilmez. Gönderim başlaması için hem tercih açık hem SMTP yapılandırılmış olmalıdır.

## Gönderim davranışı

Backend her dakika açık, tarihli ve hatırlatma seçeneği açık görevleri kontrol eder. Her kontrolde görevin durumuna göre tek en yakın aşama seçilir: 24 saat içi, 1 saat içi veya son tarih geldi. Her aşama, aynı görev ve aynı son tarih için başarılı gönderim kaydı varsa tekrar gönderilmez. Bir günden eski gecikmiş görevler e-postayla gönderilmez. Gönderim hatasında en az 15 dakika beklenir.

Backend açıkken tarayıcı sekmesi kapalı olabilir. Bilgisayar kapalı veya uykudayken gönderim çalışmaz. SMTP kabulü, e-postanın gelen kutusuna kesin ulaştığı anlamına gelmez. Gönderimden hemen sonra uygulama çökerse kayıt yazılamadığı için sonraki denemede aynı e-posta tekrar gönderilebilir.

Bu sürümde 5/10/30 dakika erteleme uygulama içi/masaüstü hatırlatmaları etkiler; e-posta aşamaları son tarihe göre çalışır. E-posta tercihini kapatmak sonraki gönderimleri durdurur; başlamış bir gönderimi geri alamaz.
