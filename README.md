# DayFlowJ

A local productivity app with notes, calendar, smart reminders, focus sessions, automatic planning, and productivity analytics. Built with Java.

## Proje durumu

Bu repo başlangıç klasör yapısını ve mimari planını içerir. Henüz çalıştırılabilir uygulama, Spring Boot projesi veya kullanıcı arayüzü bulunmuyor.

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
