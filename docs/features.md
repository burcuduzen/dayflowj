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

## Henüz uygulanmayan özellikler

- Focus mode ve odaklanma kayıtları.
- Kişisel günlük hedef, devam serisi ve hedef kutlaması.
- Gerçek sürelerden verimlilik analizi.
- Otomatik planlama.
- Doğal dille görev oluşturma.
- Takvimde görev dışı sabit etkinlikler.

Parola kasası ertelenmiştir ve mevcut kapsamda değildir.

## Kontrol adımları

1. Backend klasöründe `./mvnw test` ve ardından `./mvnw spring-boot:run` çalıştır.
2. http://127.0.0.1:8080 aç. Son tarihi bir dakika sonrası olan görev oluştur; bildirim izni ver.
3. Son tarihten sonra uygulama içi hatırlatmayı kontrol et; 5 dakika ertele. Backend’i yeniden başlatıp erteleme kaydının kalıcılığını kontrol et.
4. Her gün tekrarlayan bir görevi tamamla. Yaklaşanlar veya takvimde yeni görevi kontrol et. Önceki görevi yeniden açıp tamamla; fazladan kayıt oluşmamalı.
5. Not ekle, düzenle ve sayfayı yenile. Kaydın kalıcılığını kontrol et.
6. Takvimi gün/hafta/ay arasında değiştir; bir tarihin + butonundan görev ekle.
