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

## Streak ve e-posta

Günlük en az bir farklı görevin ilk tamamlanmasıyla devam serisi oluşur. Aynı görevi yeniden açıp tamamlamak yeni bir gün kazandırmaz. Tamamlama geçmişi görev silinse de saklanır. Önceki sürümlerde tamamlanmış ve hâlâ kayıtlı görevler geçmişe alınır. Günler tarayıcının saat diliminde hesaplanır; seyahatte saat dilimi değişirse gün dağılımı değişebilir. Bugün tamamlanmadıysa dünün serisi bugün boyunca korunur; bir gün atlandığında sıfırlanır. En uzun seri ayrıca saklanan geçmişten hesaplanır.

Tek yerel hesap için mail ve parolayla kayıt, tek kullanımlık mail doğrulama, oturumla giriş/çıkış eklendi. Bildirimler doğrulanmış hesap adresine gider. Kullanıcıya SMTP kurulumu sorulmaz; geliştirici DayFlowJ gönderici hesabını bir kez kurar. Aç/kapat ayarı, test maili ve tekrar gönderim kayıtları uygulandı. Gönderici kurulumu: [E-posta kurulumu](email-setup.md).

## Hatırlatma davranışı

Frontend zamanı gelmiş hatırlatmaları 15 saniyede bir kontrol eder; pencereye geri dönüldüğünde de kontrol eder. Backend ve uygulama sekmesi açık olmalıdır. Bilgisayar veya tarayıcı kapalıyken bildirim gönderilmez. Tarayıcı arka planda zamanlayıcıları yavaşlatabilir. Uygulama tekrar açıldığında kaçırılan ve henüz kapatılmamış hatırlatmalar gösterilir.

Masaüstü bildirimi için üstteki Bildirimleri aç butonuna bas. İzin reddedilirse uygulama içi hatırlatma devam eder. Mobil tarayıcılarda Notification constructor desteklenmeyebilir; bu durumda uygulama içi hatırlatma kullanılır. Aynı bildirim için sekme oturumu boyunca tekrar masaüstü bildirimi gönderilmez.

Hatırlatıcı son tarihte çalışır; ayrı erken hatırlatma zamanı henüz yok. Tarihi olmayan görev için hatırlatma oluşmaz. Yeni görevlerde hatırlatma varsayılan olarak açıktır. Önceden oluşturulmuş görevlerde düzenleme formundan açık hale getirilebilir. Kapalı hatırlatıcı, zaman değiştirilmedikçe veya kullanıcı hatırlatıcıyı tekrar açmadıkça yeniden kurulmaz.

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

## Henüz uygulanmayan özellikler

- Focus mode arayüzü ve zamanlayıcı ekranı.
- Kişisel günlük hedef ve hedef kutlaması.
- Verimlilik analizi arayüzü ve grafikler.
- Otomatik planlama ve plan bloklarının arayüzü.
- Doğal dille görev oluşturma.
- Takvimde görev dışı sabit etkinliklerin arayüzü.

Parola kasası ertelenmiştir ve mevcut kapsamda değildir.

## Kontrol adımları

1. Backend klasöründe `./mvnw test` ve ardından `./mvnw spring-boot:run` çalıştır.
2. Göndericiyi kur, http://127.0.0.1:8080 aç, kayıt ol, mailini doğrula ve giriş yap. Son tarihi bir dakika sonrası olan görev oluştur; bildirim izni ver.
3. Son tarihten sonra uygulama içi hatırlatmayı kontrol et; 5 dakika ertele. Backend’i yeniden başlatıp erteleme kaydının kalıcılığını kontrol et.
4. Her gün tekrarlayan bir görevi tamamla. Yaklaşanlar veya takvimde yeni görevi kontrol et. Önceki görevi yeniden açıp tamamla; fazladan kayıt oluşmamalı.
5. Not ekle, düzenle ve sayfayı yenile. Kaydın kalıcılığını kontrol et.
6. Takvimi gün/hafta/ay arasında değiştir; bir tarihin + butonundan görev ekle.
