# DayFlowJ

A local productivity app with notes, calendar, smart reminders, focus sessions, automatic planning, and productivity analytics. Built with Java.

## Proje durumu

Bu repo Spring Boot başlangıç projesini, özellik klasörlerini ve mimari planını içerir. Görev CRUD API’si ve durum güncelleme endpoint’i uygulandı. Görev arayüzü, not defteri, günlük/haftalık/aylık görev takvimi, tekrarlama, uygulama içi hatırlatma, izinli masaüstü bildirimi ve erteleme uygulandı.

## Hedef teknoloji yapısı

- Backend: Java, Spring Boot, Maven, Spring Data JPA ve Validation.
- Frontend: HTML, CSS ve JavaScript.
- Veritabanı: dosya tabanlı H2; veriler yerel bilgisayarda kalıcı tutulacak.
- Tek kullanıcılı yerel uygulama; frontend ve backend aynı bilgisayarda çalışacak.

## Klasörler

| Klasör | Amaç |
|---|---|
| `backend/` | Java backend, iş kuralları ve REST API |
| `frontend/` | Kullanıcı arayüzü ve API iletişimi |
| `docs/` | Mimari ve geliştirme planı |

Boş klasörler Git tarafından takip edilsin diye `.gitkeep` dosyaları içerir. Gerçek dosyalar eklendiğinde bu yer tutucular kaldırılabilir.

## Geliştirme sırası

1. Spring Boot altyapısı ve görev yönetimi API'si.
2. Görev yönetimi arayüzü.
3. Notlar ve takvim.
4. Hatırlatıcılar.
5. Focus mode.
6. Verimlilik analizi.
7. Otomatik planlama ve akıllı hatırlatıcılar.

Detaylar: [Mimari](docs/architecture.md), [Backend](backend/README.md), [Frontend](frontend/README.md).

## Backend’i çalıştırma

Java 17 kurulu olmalıdır. İlk çalıştırmada Maven ve bağımlılıkları indirmek için internet gerekir.

macOS / Linux:

```sh
cd backend
./mvnw spring-boot:run
```

Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Backend `http://127.0.0.1:8080` adresinde çalışır. Arayüz için `http://127.0.0.1:8080` adresini aç. JSON görev listesi `http://127.0.0.1:8080/api/tasks` adresindedir. Ayrı frontend komutu gerekmez. H2 verileri backend klasöründen çalıştırıldığında `backend/data/` altında saklanır.

Testleri çalıştırmak için backend klasöründe `./mvnw test` (Windows: `.\mvnw.cmd test`) kullan.

Görev API’si kullanım örnekleri: [Görev API’si](docs/task-api.md).

Güncel kapsam ve bildirim sınırları: [Özellik durumu](docs/features.md). Focus oturumları, günlük verimlilik analizi, sabit takvim etkinlikleri, otomatik plan önerileri ve plan onayı için backend API'leri eklendi; bunların arayüzleri, kişisel günlük hedefler ve doğal dil henüz uygulanmadı. Devam serisi ve SMTP e-posta hatırlatmaları eklendi. E-posta için [yerel kurulum](docs/email-setup.md) gerekir.

Yerel hesap: mail ve parola ile kayıt, mail doğrulama ve oturumla giriş. Her kurulumda tek hesap vardır; eski görevler korunur. Mail gönderimi için geliştirici DayFlowJ gönderici hesabını bir kez kurmalıdır: [Kurulum](docs/email-setup.md).
