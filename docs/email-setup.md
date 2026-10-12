# DayFlowJ gönderici hesabı kurulumu

Bu belge geliştirici içindir. Uygulama kayıt veya giriş istemez. Kullanıcı isteğe bağlı olarak mailini yazar ve doğrulama bağlantısını açar. Kullanıcının Gmail parolası veya SMTP bilgisi istenmez. Maili DayFlowJ için ayrılmış bir gönderici hesabı gönderir.

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
read -s "DAYFLOW_MAIL_PASSWORD?Gönderici uygulama parolası: "
export DAYFLOW_MAIL_PASSWORD
```

İlk komuttan sonra parolanı yazıp Enter’a bas; yazdıkların görünmez. Sonra aynı terminalde backend klasöründen `./mvnw spring-boot:run` çalıştır. Yeni bir terminal bu değişkenleri kendiliğinden devralmaz. Uygulama `.env` dosyasını otomatik okumaz.

Port 587 için STARTTLS (`DAYFLOW_MAIL_SSL=false`), sağlayıcı 465/implicit TLS istiyorsa port 465 ve `DAYFLOW_MAIL_SSL=true` kullan. TLS kapalı bağlantı desteklenmez. Doğru değerler sağlayıcının belgelerinden alınmalıdır.

## Kullanıcı akışı

1. Uygulamayı aç ve görevlerini kullan. Kayıt, parola ve giriş gerekmez.
2. İstersen üstteki Maille hatırlat butonuna bas, mail adresini gir ve Doğrulama maili gönder’e bas.
3. Gelen bağlantıyı uygulamanın çalıştığı bilgisayarda aç. Bağlantı tek kullanımlıktır ve 1 saat geçerlidir. Doğrulama başarıyla tamamlanınca mail hatırlatmaları açılır.
4. Maille hatırlat ekranında tercihini kapatabilir veya test maili gönderebilirsin. Adresi değiştirirsen yeni adrese doğrulama gider; mevcut doğrulanmış adres yeni doğrulama tamamlanana kadar korunur. Yeniden mail göndermek için en az 1 dakika beklenir.

Gönderici kurulmamışsa yalnızca mail gönderme seçeneği kullanılamaz. Görevler, notlar ve streak kullanımı devam eder. Gerçek SMTP gönderimi olmadan mail gönderildi veya doğrulandı gösterilmez. SMTP hata verirse doğrulama tercihi kaydedilmez. Gönderici ayarlarının mevcut olması başarılı kimlik doğrulamasını kanıtlamaz; gerçek mail gönderimini test et.

## Yerel çalışma ve doğrulama

Bu kurulum tek yerel çalışma alanıdır; çok kullanıcılı bir hizmet değildir. Mevcut görevler, notlar ve streak geçmişi korunur. Önceki zorunlu hesap ekranı kaldırıldı; eski hesap tablosu verileri varsa kullanılmadan korunur. Önceden kayıtlı mail adreslerinin yeniden doğrulanması gerekir. Mail doğrulanmadan görev bildirimi gönderilmez.

Doğrulama tokenının yalnızca SHA-256 özeti veritabanındadır. Doğrulama adresi varsayılan olarak `http://127.0.0.1:8080` olur; başka yerel port kullanıyorsan geliştirici `DAYFLOW_PUBLIC_URL` değerini de değiştirmelidir. Bağlantıyı bu bilgisayarda aç; telefondaki localhost bu uygulamaya erişemez. Backend açıkken tarayıcı sekmesi kapalı olabilir.

Yerel API giriş istemez. Yazma istekleri aynı kaynak denetimi ve özel başlık gerektirir. Mail doğrulama ve test gönderimleri dakika başına sınırlanır. Backend yalnızca 127.0.0.1’e bağlanır. İnternete açılacak bir sistem için kullanıcı ayrımı ve HTTPS gibi farklı bir mimari gerekir.

## Gönderim davranışı

Backend her dakika açık, tarihli ve hatırlatma seçeneği açık görevleri kontrol eder. Her kontrolde görevin durumuna göre tek en yakın aşama seçilir: 24 saat içi, 1 saat içi veya son tarih geldi. Her aşama, aynı görev ve aynı son tarih için başarılı gönderim kaydı varsa tekrar gönderilmez. Bir günden eski gecikmiş görevler e-postayla gönderilmez. Gönderim hatasında en az 15 dakika beklenir.

Backend açıkken tarayıcı sekmesi kapalı olabilir. Bilgisayar kapalı veya uykudayken gönderim çalışmaz. SMTP kabulü, e-postanın gelen kutusuna kesin ulaştığı anlamına gelmez. Gönderimden hemen sonra uygulama çökerse kayıt yazılamadığı için sonraki denemede aynı e-posta tekrar gönderilebilir.

Bu sürümde 5/10/30 dakika erteleme uygulama içi/masaüstü hatırlatmaları etkiler; e-posta aşamaları son tarihe göre çalışır. E-posta tercihini kapatmak sonraki gönderimleri durdurur; başlamış bir gönderimi geri alamaz.
