# DayFlowJ masaüstü sürümü

DayFlowJ artık Electron penceresinde çalışan hibrit bir Windows masaüstü uygulamasıdır. HTML/CSS/JavaScript arayüz korunur; Java/Spring Boot backend aynı bilgisayarda çalışır. Normal açılışta tarayıcı sekmesi veya terminal açılmaz.

## Kullanım

Masaüstündeki **DayFlowJ** kısayoluna çift tıkla. Doğrudan çalıştırılabilir dosya: `desktop/runtime/DayFlowJ.exe`.

- Backend kapalıysa uygulama arka planda başlatır.
- Önceden çalışan DayFlowJ backend'i varsa onu kullanır.
- Pencere kapanınca yalnızca kendisinin başlattığı backend işlem ağacını durdurur.
- İkinci kez açılış yeni pencere oluşturmak yerine mevcut pencereyi öne getirir.
- Paketlenmiş Java `desktop/jre` altında bulunur; bu pakette ayrı Java kurulumu gerekmez.
- Görev, takvim ve not kayıtları mevcut `backend/data` içinde kalır.
- Masaüstü kronometresi ve bildirim tercihleri `%APPDATA%/DayFlowJ` profilinde tutulur. Tarayıcıdaki kronometre durumu ayrı profilde kalır.
- Kapatınca arka planda hatırlatma göndermez; uygulama veya ayrı backend açık olmalıdır.

## Paket sınırı

Bu sürüm mevcut proje klasörü içinde çalıştırılabilir bir Windows paketidir. Tek dosyalı kurulum sihirbazı değildir. Yalnızca EXE'yi başka yere kopyalamak yeterli olmaz; Electron çalışma dosyaları, backend JAR'ı ve Java çalışma ortamı birlikte bulunmalıdır. Klasörü taşırsan masaüstü kısayolunu yeniden oluşturmalısın.

Gmail'den takvime özelliği önceki kapsamda ertelendi; dış tarayıcıdaki Google OAuth dönüşünün masaüstü oturumuyla bütünleştirilmesi ayrıca ele alınmalıdır. SMTP bağlantısındaki kurum sertifikası sorunu bu dönüşümle çözülmez.

## Geliştirici için

Kaynak: `main.cjs`. Backend başlatıcısı: `Start-Backend.ps1`. `pnpm install` sonrasında Electron indirmesi gerekirse `node node_modules/electron/install.js` çalıştırılır. Uygulama kapalıyken `node build.cjs`, Electron dosyalarını `runtime` altına kopyalar. Backend JAR'ı Maven ile ayrıca paketlenir. Java çalışma ortamı JDK 17'nin jlink aracıyla oluşturulmuştur.

Renderer'da Node erişimi kapalıdır; context isolation, sandbox ve web güvenliği açıktır. Uygulama içi gezinme localhost ile sınırlıdır; HTTPS dış bağlantılar sistem tarayıcısına yönlendirilir.

