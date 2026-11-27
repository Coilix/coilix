# COILIX-00034: Komunikaty błędów z kluczem i moduł tłumaczeń w rdzeniu

> Uwaga opiekuna z 2026-11-27: `Violation` daje późniejsze łatwe tłumaczenie komunikatów, gdy dojdzie i18n, a dziś komunikaty błędów są definiowane w miejscu rzucenia. Komunikat, który trafi do API, nie da się wtedy przetłumaczyć. Moduł tłumaczeń prawdopodobnie powinien być częścią rdzenia Coilix. Przykłady są w implicits i bulldozerze.

## Cel

- Każdy komunikat błędu, który może trafić do użytkownika końcowego (odpowiedź API, błąd walidacji, błąd startu widoczny dla operatora), ma stały klucz i argumenty, a tekst powstaje dopiero przy prezentacji, w języku odbiorcy.
- Rdzeń Coilix daje moduł tłumaczeń: pakiety komunikatów na język, wybór języka (`Locale`), liczba mnoga i nazwane argumenty, a brak tłumaczenia wraca do tekstu domyślnego.

## Stan obecny

- `Violation` i `ViolationTemplate` w `common/validation` (COILIX-00022) już mają kod wyprowadzany z nazwy `val` i argumenty, więc nadają się do tłumaczenia.
- Typy błędów z ADR 0050–0053 (`ContextError`, `StartupError`, `ConfigError`, `ProcessingSetupError`, `ProcessorError`, `EventListenerError`, `PublishError`) budują `message` z interpolacji w swoim `match`. Tekst jest po angielsku i nie ma klucza, więc nie da się go przetłumaczyć ani rozpoznać w kliencie inaczej niż po treści.
- Wyjątki frameworka (`ConfigurationException`, `InvalidProcessorException`…) dostają gotowy tekst w konstruktorze.

## Zakres

1. Model komunikatu: klucz (jak kod `Violation`), argumenty nazwane i tekst domyślny. Wspólny dla `Violation`, przypadków enumów błędów i wyjątków frameworka.
2. Przypadki enumów błędów z ADR 0050–0053 dostają klucz i argumenty zamiast gotowego tekstu; `message` zostaje jako tekst domyślny po angielsku, więc logi się nie zmieniają.
3. Moduł tłumaczeń w rdzeniu: ładowanie pakietów z classpath (format do wyboru: `.properties`, PO jak w implicits), `Locale` z konfiguracji i z żądania, liczba mnoga (reguły CLDR), nazwane argumenty, tryb bez tłumaczeń jako domyślny.
4. Integracja z warstwą HTTP (COILIX-00023): błąd typowany zamieniony na odpowiedź niesie klucz, argumenty i tekst w języku z `Accept-Language`.
5. Narzędzie, które wypisuje wszystkie klucze komunikatów frameworka i aplikacji, żeby tłumacz dostał kompletną listę.

## Proponowany projekt

Koncepcje z `org.optimus.i18n` (implicits): `I18n.resolve(name, args, locale)`, `TranslationBundle`, `TranslationLoader`, `PluralRule`, `MessageInterpolator`, `PoParser` i `ViolationI18n`, które tłumaczy `Violation` po jego nazwie. Do sprawdzenia też podejście bulldozera do komunikatów w odpowiedziach API. Bierzemy koncepcje, nie kod 1:1: zamiast globalnego `AtomicReference` z konfiguracją bean tłumaczeń w kontekście, API w `IO` tam, gdzie ładuje pliki, i nazwy w stylu Coilix.

## Kryteria akceptacji

1. Błąd walidacji i błąd typowany zwrócone przez HTTP mają klucz, argumenty i tekst w języku z `Accept-Language`, a bez tłumaczenia tekst domyślny.
2. Żaden przypadek enumów błędów z ADR 0050–0053 nie składa tekstu bez klucza.
3. Aplikacja dodaje tłumaczenie komunikatu frameworka plikiem na classpath, bez kodu.

## Poza zakresem

- Tłumaczenie logów: logi zostają po angielsku, dla operatorów.

## Ryzyka i otwarte pytania

- Format plików: `.properties` (prosty, znany w JVM) czy PO z liczbą mnogą i kontekstem (jak w implicits).
- Czy moduł tłumaczeń ma być w `common`, osobnym module rdzenia, czy w `web`; uwaga opiekuna wskazuje rdzeń.
- Zależność od COILIX-00023 (HTTP) dla kryterium 1; pierwsze kroki (model komunikatu, klucze w enumach błędów) nie czekają na HTTP.
