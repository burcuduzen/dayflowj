# DayFlowJ gönderici hesabı kurulumu

Bu belge geliştirici içindir. Kullanıcı yalnızca maili ve DayFlowJ parolasıyla kayıt olur, doğrulama bağlantısını açar ve bildirim tercihini seçer. Kullanıcının Gmail parolası veya SMTP bilgileri istenmez. Maili DayFlowJ için ayrılmış bir gönderici hesabı gönderir.

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

1. DayFlowJ açıldığında mail ve en az 12 karakterlik DayFlowJ parolasıyla kayıt ol. Görev hatırlatmalarını kayıt sırasında seç.
2. Gelen doğrulama bağlantısını uygulamanın çalıştığı bilgisayarda aç. Bağlantı 1 saat geçerlidir ve tek kullanımlıktır.
3. Doğrulamadan sonra mail ve DayFlowJ parolasıyla giriş yap. Görevlerini oluştur.
4. E-posta ekranında tercihlerini aç/kapat veya test maili gönder. Alıcı adresi doğrulanmış hesabından gelir ve bu ekranda değiştirilmez.

Gönderici kurulmamışsa kayıt kapalıdır ve ekranda kurulumun tamamlanmadığı belirtilir. Gerçek SMTP gönderimi olmadan doğrulama başarılı gösterilmez. Gönderici ayarlarının mevcut olması başarılı kimlik doğrulamasını kanıtlamaz; SMTP hata verirse kayıt geri alınır. Mail gönderiminin test edilmesi gerekir.

## Yerel hesap ve oturum

Bu sürüm her yerel kurulumda tek hesap destekler. Mevcut görevler, notlar ve streak geçmişi korunur ve bu yerel hesabın kullanımı için kalır. Çok kullanıcılı hizmet, kullanıcılar arasında veri ayrımı, parola sıfırlama ve mail adresi değiştirme henüz yoktur. Hesabı unuttuğunda veritabanını silme; veri kaybı olur.

API erişimi doğrulanmış hesap ve oturum gerektirir. Parolalar PBKDF2-HMAC-SHA256 (600.000 yineleme ve rastgele salt) ile özetlenir. Doğrulama tokenının yalnızca SHA-256 özeti veritabanındadır. Oturum HttpOnly, SameSite=Lax çereziyle tutulur; girişte eski oturum yenilenir ve çıkışta silinir. Yazma istekleri aynı kaynak denetimi ve özel istek başlığı gerektirir. Giriş/kayıt/yeniden gönderim istekleri dakika başına sınırlanır. Yerel HTTP ve localhost ile sınırlıdır; internete açılacak bir çok kullanıcı sistemi için tasarlanmamıştır.

Doğrulama bağlantısının adresi varsayılan olarak `http://127.0.0.1:8080` olur. Farklı yerel port kullanıyorsan geliştirici `DAYFLOW_PUBLIC_URL` değerini de değiştirmelidir. Bağlantı telefonda uygulamayı açamaz; bu bilgisayarda açılmalıdır. Oturum en fazla 8 saat hareketsiz kalır. Gönderimler oturum kapalıyken de backend açıksa çalışır.

## Gönderim davranışı

Backend her dakika açık, tarihli ve hatırlatma seçeneği açık görevleri kontrol eder. Her kontrolde görevin durumuna göre tek en yakın aşama seçilir: 24 saat içi, 1 saat içi veya son tarih geldi. Her aşama, aynı görev ve aynı son tarih için başarılı gönderim kaydı varsa tekrar gönderilmez. Bir günden eski gecikmiş görevler e-postayla gönderilmez. Gönderim hatasında en az 15 dakika beklenir.

Backend açıkken tarayıcı sekmesi kapalı olabilir. Bilgisayar kapalı veya uykudayken gönderim çalışmaz. SMTP kabulü, e-postanın gelen kutusuna kesin ulaştığı anlamına gelmez. Gönderimden hemen sonra uygulama çökerse kayıt yazılamadığı için sonraki denemede aynı e-posta tekrar gönderilebilir.

Bu sürümde 5/10/30 dakika erteleme uygulama içi/masaüstü hatırlatmaları etkiler; e-posta aşamaları son tarihe göre çalışır. E-posta tercihini kapatmak sonraki gönderimleri durdurur; başlamış bir gönderimi geri alamaz.
