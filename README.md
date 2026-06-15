# Spyro Backend — Carbon Intensity API

Backend (Spring Boot) liczący udział czystej energii w brytyjskiej sieci energetycznej
i wyznaczający najlepsze okno na ładowanie w oparciu o publiczne dane
[UK Carbon Intensity API](https://carbonintensity.org.uk/). 

Aplikacja pobiera prognozę miksu energetycznego (wiatr, słońce, gaz, węgiel itd.)
w 30-minutowych interwałach i na jej podstawie udostępnia dwa endpointy

---

## Stack technologiczny

| | |
|---|---|
| Język | Java 17 |
| Framework | Spring Boot 3 (Web MVC) |
| Klient HTTP | Spring WebClient (WebFlux) |
| Cache | Caffeine (in-memory) |
| Testy | JUnit 5, MockWebServer |
| Deploy | Docker → Render |

---

## Endpointy API

Bazowy adres: `http://localhost:8080/api`

### 1. `GET /api/energy-mix`

Zwraca uśredniony miks energetyczny na 3 dni (dziś, jutro, pojutrze) wraz z procentem
czystej energii dla każdego dnia. Dane dla 3 dnia nie są z pełnej doby, 
bo API zwraca tylko interwały maksymalnie 48h w przod.


### 2. `GET /api/charging-window?hours={1-6}`

Wyznacza optymalne okno czasowe na ładowanie o zadanej długości (1–6 h) w ciągu
najbliższych dwóch dni — czyli przedział, w którym średni udział czystej energii jest najwyższy.

## Logika 
1. Jedno zapytanie do zewnętrznego API, współdzielone przez oba endpointy.
`CarbonIntensityClient` pobiera  okno 3 dni (`/generation/{from}/{to}`) icache'uje
wynik (Caffeine, TTL 30 min). Dzięki temu `/energy-mix` i `/charging-window` korzystają z tych
samych danych zamiast odpytywac API osobno

2. Procent czystej energii (`EnergyCalculator#cleanPercentage`)
to suma udziałów źródeł z listy: `biomass, nuclear, hydro, wind, solar`.

3. Średni miks dzienny(`/energy-mix`) — interwały grupowane po dacie (strefa `Europe/London`),
a następnie uśredniane per typ paliwa.

4. Najlepsze okno ładowania (`/charging-window`)
- okno o długości `hours` to `2 × hours` interwałów po 30 min,
- dla każdego możliwego położenia liczona jest średnia czystość,
- pomijane są okna z luką czasową (interwały muszą następować po sobie — `isContiguous`),
- zwracane jest okno z najwyższą średnią
- brane sa pod uwagę interwały z przedzialu (teraz, teraz +48h)

Dane z zewnętrznego API są sortowane chronologicznie(nie zakładałam, że przychodzą po kolei).

---

## Obsługa błędów

Wszystkie błędy zwracane są w jednolitym formacie `{ "code", "message" }`:

| Sytuacja                       | Status HTTP | `code` |
|--------------------------------|---|---|
| Liczba godzina poza 1–6        | `400 Bad Request` | `VALIDATION_ERROR` |
| Za mało danych na okno         | `422 Unprocessable Content` | `INSUFFICIENT_DATA` |
| Zewnętrzne API niedostępne / timeout | `503 Service Unavailable` | `UPSTREAM_ERROR` |

---

## Deploy
Adres frontendu konfigurowany
przez `APP_CORS_ALLOWED_ORIGINS`.
https://spyro-frontend-manmar.onrender.com
