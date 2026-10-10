# DayFlowJ özellik durumu

## Uygulanan özellikler

- Görev ekleme, düzenleme, silme, tamamlama ve yeniden açma.
- Başlık, açıklama, son tarih/saat, öncelik, tahmini süre.
- Bugünkü planım: bugün, gecikmiş ve tarihsiz açık görevler.
- Arama, öncelik ve durum filtreleri.
- Günlük, haftalık ve aylık görev takvimi. Takvimden görevi düzenleme veya güne görev ekleme.
- Not defteri: kalıcı not ekleme, düzenleme, silme ve arama.
- Tarihli görevler için isteğe bağlı hatırlatma.
- Uygulama içi hatırlatma; desteklenen tarayıcılarda kullanıcı izin verirse masaüstü bildirimi.
- Hatırlatmayı kapatma ve 5, 10, 30 dakika erteleme. Erteleme veritabanında saklanır.
- Her gün, hafta veya ay tekrarlayan görev. Sonraki görev mevcut görev tamamlandığında oluşur.
- Backend odak oturumları: isteğe bağlı göreve bağlama, duraklatma, devam ettirme ve tamamlama.
- Odak ve mola süreleri sunucu zamanına göre hesaplanır; aynı anda yalnızca bir aktif oturum olabilir.
- Günlük verimlilik analizi: tamamlanan görev, gerçekleşen odak süresi, tahmini süre ve süre farkı.
- Görev dışı sabit takvim etkinlikleri için tarih aralığı sorgulamalı CRUD API.
- Son tarih, öncelik ve tahmini süreye göre boş çalışma saatlerine görev yerleştiren plan önerisi API.
- Önerilen blokları çakışma kontrolüyle topluca onaylama, listeleme ve kaldırma.
- Günlük görev hedefi, çalışma saatleri, odak/mola süreleri ve saat dilimi için kalıcı kullanıcı ayarları.
- Türkçe cümleden başlık, tarih, saat, öncelik, süre ve tekrarlama çıkararak görev önizleme veya oluşturma.
- Görev bazında son tarihte veya 5 dakika ile 1 hafta arasında erken hatırlatma seçimi.
- Focus zamanlayıcısı: görev seçme, başlatma, duraklatma, devam ettirme ve tamamlama.
- Son 7 gün için özet kartları ve günlük çubuk grafik içeren verimlilik analizi.
- Kişisel günlük hedef, çalışma saatleri, odak/mola süresi ve saat dilimi ayar ekranı.
- Doğal dille görev yazma, ayrıştırılan alanları önizleme ve onaylayarak oluşturma.
- Sabit takvim etkinliklerini ekleme, düzenleme, silme ve tüm takvim görünümlerinde gösterme.
- Otomatik plan önerisi oluşturma, uygun olmayan görevlerin nedenlerini gösterme ve plan bloklarını onaylama veya kaldırma.

## Streak ve e-posta

Günlük en az bir farklı görevin ilk tamamlanmasıyla devam serisi oluşur. Aynı görevi yeniden açıp tamamlamak yeni bir gün kazandırmaz. Tamamlama geçmişi görev silinse de saklanır. Önceki sürümlerde tamamlanmış ve hâlâ kayıtlı görevler geçmişe alınır. Günler tarayıcının saat diliminde hesaplanır; seyahatte saat dilimi değişirse gün dağılımı değişebilir. Bugün tamamlanmadıysa dünün serisi bugün boyunca korunur; bir gün atlandığında sıfırlanır. En uzun seri ayrıca saklanan geçmişten hesaplanır.

`GET /api/progress`, seri bilgisine ek olarak `dailyTaskGoal`, `completedTodayCount` ve `dailyGoalReached` alanlarını döndürür. İstekte `timeZone` verilmezse kayıtlı kullanıcı saat dilimi kullanılır. Frontend `dailyGoalReached` ilk kez doğru olduğunda hedef kutlamasını gösterebilir.

Tek yerel hesap için mail ve parolayla kayıt, tek kullanımlık mail doğrulama, oturumla giriş/çıkış eklendi. Bildirimler doğrulanmış hesap adresine gider. Kullanıcıya SMTP kurulumu sorulmaz; geliştirici DayFlowJ gönderici hesabını bir kez kurar. Aç/kapat ayarı, test maili ve tekrar gönderim kayıtları uygulandı. Gönderici kurulumu: [E-posta kurulumu](email-setup.md).

## Hatırlatma davranışı

Frontend zamanı gelmiş hatırlatmaları 15 saniyede bir kontrol eder; pencereye geri dönüldüğünde de kontrol eder. Backend ve uygulama sekmesi açık olmalıdır. Bilgisayar veya tarayıcı kapalıyken bildirim gönderilmez. Tarayıcı arka planda zamanlayıcıları yavaşlatabilir. Uygulama tekrar açıldığında kaçırılan ve henüz kapatılmamış hatırlatmalar gösterilir.

Masaüstü bildirimi için üstteki Bildirimleri aç butonuna bas. İzin reddedilirse uygulama içi hatırlatma devam eder. Mobil tarayıcılarda Notification constructor desteklenmeyebilir; bu durumda uygulama içi hatırlatma kullanılır. Aynı bildirim için sekme oturumu boyunca tekrar masaüstü bildirimi gönderilmez.

Hatırlatıcı görev formunda seçilen süreye göre son tarihte ya da 5 dakika, 15 dakika, 30 dakika, 1 saat, 1 gün veya 1 hafta önce çalışır. API `reminderMinutesBefore` alanında 0–10080 arasında özel bir dakika değeri de kabul eder. Tarihi olmayan görev için hatırlatma oluşmaz. Yeni görevlerde hatırlatma varsayılan olarak açıktır. Tekrarlayan görevler aynı erken hatırlatma ayarını devralır. Kapalı hatırlatıcı, zaman veya erken uyarı süresi değiştirilmedikçe ya da kullanıcı hatırlatıcıyı tekrar açmadıkça yeniden kurulmaz.

## Tekrarlama davranışı

- Tekrarlama için son tarih gereklidir; saat dilimi tarayıcıdan alınır.
- Görev tamamlanınca sonraki gelecek tarihli görev oluşur; kaçırılmış aralıklar tek tek oluşturulmaz.
- Aynı görev tekrar tamamlandığında veya yeniden açılıp tamamlandığında ikinci bir sonraki görev oluşturulmaz.
- Günlük/haftalık takvim artışları yerel saati yaz saati değişimleri boyunca korur; var olmayan yerel saatler Java saat dilimi kurallarına göre ileri kaydırılabilir.
- Aylık tekrarda kısa aylarda son geçerli gün kullanılır. Yeni oluşan görev kendi tarihinden tekrar eder.

## Focus API

| Yöntem | Yol | İşlem |
|---|---|---|
| `GET` | `/api/focus-sessions` | Tüm odak oturumlarını listeler |
| `GET` | `/api/focus-sessions/active` | Aktif oturumu döndürür; yoksa `204` |
| `POST` | `/api/focus-sessions` | Oturum başlatır; gövde isteğe bağlı olarak `{"taskId": 1}` olabilir |
| `POST` | `/api/focus-sessions/{id}/pause` | Çalışan oturumu duraklatır |
| `POST` | `/api/focus-sessions/{id}/resume` | Duraklatılmış oturumu devam ettirir |
| `POST` | `/api/focus-sessions/{id}/complete` | Oturumu tamamlar |

Durumla uyumsuz işlemler ve ikinci aktif oturum başlatma denemesi `409` döndürür. Diğer korumalı API işlemlerinde olduğu gibi mutasyon isteklerinde `X-DayFlowJ-Request: 1` başlığı ve doğrulanmış oturum gerekir.

## Analiz API

`GET /api/analytics` varsayılan olarak kullanıcının saat dilimindeki son 7 günü döndürür. `from`, `to` ve `timeZone` parametreleriyle en fazla 366 günlük aralık istenebilir:

```text
/api/analytics?from=2026-10-01&to=2026-10-09&timeZone=Europe/Istanbul
```

Yanıt hem toplamları hem de boş günler dahil günlük satırları içerir. `varianceMinutes`, gerçekleşen odak dakikasından tamamlanan görevlerin tahmini dakikalarının çıkarılmasıyla hesaplanır. Silinmiş bir görevin tamamlama geçmişi korunur ancak tahmini süresi artık bulunamadığı için analize `0` dakika olarak girer. Tamamlanmış odak oturumunun süresi, oturumun bittiği yerel güne yazılır.

## Takvim etkinliği API

| Yöntem | Yol | İşlem |
|---|---|---|
| `GET` | `/api/calendar-events` | Etkinlikleri başlangıç zamanına göre listeler |
| `GET` | `/api/calendar-events?from=...&to=...` | Aralıkla çakışan etkinlikleri listeler |
| `POST` | `/api/calendar-events` | Sabit etkinlik oluşturur |
| `PUT` | `/api/calendar-events/{id}` | Etkinliği günceller |
| `DELETE` | `/api/calendar-events/{id}` | Etkinliği siler |

Başlangıç ve bitiş zamanları ISO-8601 saat dilimi ofsetiyle gönderilir. Bitiş başlangıçtan sonra olmalıdır. Aralık sorgularında `from` ve `to` birlikte verilir ve en fazla 366 gün istenebilir. Aralığın sınırından önce başlayan ancak aralık içinde devam eden etkinlikler de sonuçlara dahildir.

## Otomatik planlama API

`POST /api/planning/suggestions` açık görevler için en fazla 14 günlük bir plan önizlemesi üretir. Örnek gövde:

```json
{
  "from": "2026-10-10",
  "to": "2026-10-12",
  "workStart": "09:00",
  "workEnd": "18:00",
  "timeZone": "Europe/Istanbul",
  "taskIds": [1, 2, 3]
}
```

`taskIds` verilmezse henüz planlanmamış tüm açık görevler değerlendirilir. Görevler önce son tarihe, sonra önceliğe göre işlenir. Motor sabit takvim etkinlikleri ve önceden onaylanmış planlarla çakışmaz, geçmiş saate öneri koymaz ve görevi tek parça halinde yerleştirir. Tahmini süresi olmayan görevler `MISSING_ESTIMATE`, uygun boşluğa sığmayanlar `NO_AVAILABLE_SLOT` nedeni ile `unscheduled` listesinde döner.

Öneriler `POST /api/planning/blocks/approve` ile topluca kalıcı hale getirilir:

```json
{
  "blocks": [
    { "taskId": 1, "startAt": "2026-10-10T09:00:00+03:00", "endAt": "2026-10-10T10:00:00+03:00" }
  ]
}
```

Onay sırasında görev hâlâ açık olmalı, blok süresi tahmini süreyle eşleşmeli ve sabit etkinliklerle, mevcut planlarla veya aynı onay grubundaki diğer bloklarla çakışmamalıdır. Her görev yalnızca bir kez planlanabilir. Herhangi bir blok geçersizse grubun tamamı reddedilir. `GET /api/planning/blocks` blokları listeler; isteğe bağlı `from` ve `to` aralığı kullanılabilir. `DELETE /api/planning/blocks/{id}` bir bloğu kaldırır.

## Kullanıcı ayarları API

`GET /api/settings` ayarları döndürür ve ilk kullanımda güvenli varsayılanları oluşturur: günlük hedef `3`, çalışma saatleri `09:00–18:00`, odak `25` dakika, mola `5` dakika ve saat dilimi `Europe/Istanbul`.

`PUT /api/settings` tüm ayarları birlikte günceller:

```json
{
  "dailyTaskGoal": 5,
  "workStart": "09:00",
  "workEnd": "18:00",
  "focusMinutes": 50,
  "breakMinutes": 10,
  "timeZone": "Europe/Istanbul"
}
```

## Doğal dille görev API

`POST /api/tasks/natural-language/preview` ayrıştırılan alanları görev oluşturmadan döndürür. `POST /api/tasks/natural-language` aynı ayrıştırmayı kullanarak görevi oluşturur. Örnek gövde:

```json
{
  "text": "Yarın saat 14:30 acil 2 saat sunumu hazırla",
  "timeZone": "Europe/Istanbul"
}
```

Bu örnek `sunumu hazırla` başlığını, yüksek önceliği, 120 dakikalık tahmini süreyi ve yarın 14:30 son tarihini üretir. `timeZone` verilmezse kayıtlı ayar kullanılır. Desteklenen ifadeler:

- `bugün`, `yarın`, ISO tarih (`2026-10-15`) ve Türkçe hafta günleri.
- `saat 14`, `14:30` veya `14.30`.
- `30 dk`, `45 dakika`, `2 saat`.
- `acil`, `yüksek öncelik`, `düşük öncelik`.
- `her gün`, `her hafta`, `her ay`.

Ayrıştırma kurallara dayalı ve yereldir; metin harici bir servise gönderilmez. Önizleme endpoint’i belirsiz sonuçları kullanıcıya onaylatmak için kullanılmalıdır.

## Kapsam dışında

Parola kasası ertelenmiştir ve mevcut kapsamda değildir.

## Kontrol adımları

1. Backend klasöründe `./mvnw test` ve ardından `./mvnw spring-boot:run` çalıştır.
2. Göndericiyi kur, http://127.0.0.1:8080 aç, kayıt ol, mailini doğrula ve giriş yap. Son tarihi bir dakika sonrası olan görev oluştur; bildirim izni ver.
3. Son tarihten sonra uygulama içi hatırlatmayı kontrol et; 5 dakika ertele. Backend’i yeniden başlatıp erteleme kaydının kalıcılığını kontrol et.
4. Her gün tekrarlayan bir görevi tamamla. Yaklaşanlar veya takvimde yeni görevi kontrol et. Önceki görevi yeniden açıp tamamla; fazladan kayıt oluşmamalı.
5. Not ekle, düzenle ve sayfayı yenile. Kaydın kalıcılığını kontrol et.
6. Takvimi gün/hafta/ay arasında değiştir; bir tarihin + butonundan görev ekle.
7. Focus ekranında bir görev seçip oturumu başlat, duraklat, devam ettir ve tamamla.
8. Analiz ekranında son 7 günün özetini ve grafiğini kontrol et.
9. Ayarlardan çalışma saatlerini değiştir; otomatik plan önerisinin yeni saatleri kullandığını doğrula.
10. Takvime sabit etkinlik ekle; ay, hafta, gün ve ajanda görünümlerinde göründüğünü kontrol et.
