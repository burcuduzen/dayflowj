# DayFlowJ

Windows için tek kullanıcılı, yerel görev ve takvim uygulaması. Electron masaüstü penceresi, Java 17 / Spring Boot backend ve dosya tabanlı H2 kullanır. Arayüz HTML, CSS ve JavaScript özellik modüllerinden oluşur.

## Kullanım

`desktop/dist/DayFlowJ-Setup-1.1.0.exe` kurulumunu aç. Kurulum masaüstüne DayFlowJ kısayolu ekler. Geliştirme bilgisayarında `desktop/dist/win-unpacked/DayFlowJ.exe` de doğrudan açılır. Java pakete dahildir; normal kullanımda terminal gerekmez.

## Veriler ve yedekleme

Masaüstü sürümü kayıtları `%APPDATA%/DayFlowJ/data` içinde tutar. Kurulum dosyaları güncellenince bu klasör korunur. İlk açılışta eski `Downloads/dayflowj-main/backend/data` veritabanı yalnızca kapalıysa kopyalanır; orijinali silinmez. Eski uygulama/backend açıksa önce kapatılmalıdır.

Veriler menüsünden ZIP yedeği alabilir, yedeği geri yükleyebilir veya veri klasörünü açabilirsin. Geri yükleme önce mevcut verinin kopyasını saklar. Başlangıç yedeklerinden son yedisi korunur; elle alınan yedekler otomatik silinmez. Yedekler kişisel verileri içerir; güvenli yerde sakla.

Flyway veritabanı şemasını sürümlü SQL geçişleriyle yönetir. Hibernate şemayı değiştirmek yerine doğrular. Eski Focus tabloları geçmiş verileri silmemek için arşiv olarak veritabanında kalabilir; Focus API kaldırılmıştır.

## Davranışlar

Takvim başlangıç ekranıdır. Kronometre yalnızca başlat/duraklat/sıfırla işlevlerini içerir; yerel arayüz depolamasında saklanır, veritabanı ZIP yedeğine dahil değildir.

Seri ve analiz görevin ilk tamamlandığı günü sayar. Yeniden açıp tamamlamak ikinci kez sayılmaz. Görevi silmek geçmiş tamamlanma kaydını silmez. Analizdeki dakika gerçek çalışma ölçümü değil, ilk tamamlamadaki tahmini süredir; sonradan süre değiştirildiğinde geçmiş değişmez.

Masaüstü açılışı kendine ait backend başlatır; kapanışta kontrollü olarak kapatır. 8080 portu başka bir uygulamada kullanılıyorsa o uygulamayı benimsemez veya durdurmaz. Masaüstü API istekleri uygulamaya özel geçici anahtarla doğrulanır.

## Gmail ve mail

Gmail bağlantısında kısa ömürlü, tek kullanımlık state ve PKCE vardır; sistem tarayıcısı ile Electron aynı oturum çerezini paylaşmak zorunda değildir. Gerçek Google hesabıyla izin ve uçtan uca gönderim ayrıca doğrulanmalıdır. SMTP TLS sertifika doğrulaması kapatılmaz. Kurumsal ağ sertifika hatası varsa güvenilir sertifika zinciri veya ağ erişimi düzeltilmelidir.

Bağlantı sırları kaynak koda yazılmaz. Yerel DPAPI ayarları aynı Windows kullanıcısıyla okunur. Gmail OAuth belirteçleri bellektedir; uygulama yeniden açılınca bağlantı gerekebilir. Otomatik güncelleme yoktur; yeni kurulumla güncellenir.

## Geliştirme ve doğrulama

Backend: `backend/mvnw.cmd clean package`. Java 17 gerekir.

Masaüstü: `desktop` içinde `pnpm install`, `pnpm test`, `pnpm run build`. Küçültülmüş Java ortamı `desktop/jre-small` içine hazırlanır. Installer electron-builder / NSIS ile üretilir.

Backend testleri iş kuralları, OAuth state, kapalı veritabanı taşıma, geçersiz yedek ve şema geçişlerini kapsar. Masaüstü entegrasyon testleri geçici verilerle açılış, özel API anahtarı, geçmiş süre, yedekleme/geri yükleme, kalıcılık, kontrollü kapanış ve port çakışmasını kontrol eder.
