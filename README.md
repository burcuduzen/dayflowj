# DayFlowJ

A local productivity app with notes, calendar, smart reminders, focus sessions, automatic planning, and productivity analytics. Built with Java.

## Proje durumu

Bu repo Spring Boot başlangıç projesini, özellik klasörlerini ve mimari planını içerir. Görev CRUD API’si ve durum güncelleme endpoint’i uygulandı. Kullanıcı arayüzü henüz uygulanmadı.

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

Backend `http://127.0.0.1:8080` adresinde çalışır. Görevleri listelemek için `http://127.0.0.1:8080/api/tasks` adresini aç. Kök adreste henüz sayfa bulunmadığı için 404 yanıtı normaldir. H2 verileri backend klasöründen çalıştırıldığında `backend/data/` altında saklanır.

Testleri çalıştırmak için backend klasöründe `./mvnw test` (Windows: `.\mvnw.cmd test`) kullan.

Görev API’si kullanım örnekleri: [Görev API’si](docs/task-api.md).
