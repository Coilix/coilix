# 0051: Błędy typowane konfiguracji

- Status: proponowana
- Data: 2026-11-26
- Task: COILIX-00031 (krok 10)

## Kontekst

ADR 0050 wprowadził `ContextError` i `StartupError`, ale konfiguracja dalej zgłaszała każdy błąd wyjątkiem `ConfigurationException`. Runner łapał go klasą wyjątku, żeby zrobić z niego `StartupError.Configuration`. Plik, którego nie da się sparsować, i podstawienie `${...}` bez wartości kończyły się wyjątkiem biblioteki (`ConfigException`, `YamlEngineException`), więc trafiały do logu jako defekt ze stosem wywołań, a nie jako zwykły błąd konfiguracji.

## Decyzja

- **`ConfigError` jako typ warstwy `config`.** Przypadki mówią, co poszło nie tak i pod jaką ścieżką: `Unparsable`, `Unresolved`, `Unreadable`, `Missing`, `UnsupportedScalar`, `UnsupportedTarget`, `UnsupportedCollection`, `InvalidValue`, `UnknownClass`, `IncompatibleClass`, `AbstractTarget`, `AmbiguousConstructor`, `CreationFailed`, `TypeMismatch`. `message` daje te same komunikaty co wcześniej, a `exception` zwraca `ConfigurationException` z tym komunikatem.
- **`Either` od dołu do góry.** Metody czytające `ConfigTree`, `ScalarReader.read`, `ConfigBinder.bind`, `ConfigFormat.parse`, `Profiles.active` i `EnvironmentLoader.load` zwracają `Either[ConfigError, A]`. Implementacje łapią wyjątki bibliotek (Typesafe Config, SnakeYAML) w miejscu, w którym powstają.
- **Dwie formy w API dla komponentów.** `Environment` ma `lookup`, `binding` i `evaluate` zwracające `Either`, a `get`, `bind` i `resolve` są finalnymi metodami, które rzucają `exception` błędu. `ComponentProvider` ma tak samo `lookup` i `get`. Kod, który czyta konfigurację w czasie tworzenia beanów, zostaje synchroniczny (ADR 0043) i dalej używa form rzucających; na granicy builder zamienia wyjątek na `ContextError.CreationFailed`.
- **Start.** `StartupError.Configuration` trzyma `ConfigError` zamiast wyjątku, a runner przekazuje wynik `load` bez łapania wyjątków. `CoilixIOApp`, który czyta liczbę wątków przed startem, przy błędzie konfiguracji uruchamia runtime z wartościami domyślnymi, a błąd zgłasza start. Testkit, jako warstwa najwyższa testu, rzuca `exception`.

## Konsekwencje

- Zepsuty plik konfiguracji, brak wartości podstawienia i źle podane profile kończą start czytelnym komunikatem i kodem wyjścia `1`, bez stosu wywołań.
- Komponent, który chce sam obsłużyć brak albo zły format wartości, używa `lookup` albo `binding` i dostaje typ błędu zamiast łapać wyjątek.
- Zmiana niezgodna API publicznego (`0.x`: `MINOR`): własne `ConfigTree`, `ScalarReader`, `ConfigBinder`, `ConfigFormat`, `EnvironmentLoader`, `Environment` i `ComponentProvider` zwracają `Either` (w dwóch ostatnich implementuje się `lookup`, `binding` i `evaluate`).
- Ustawienia modułów (`ProcessorSettings`, `LoggingSettings`) dalej rzucają `ConfigurationException` dla wartości spoza dozwolonych, bo czytają je beany w czasie tworzenia; przejdą na `ConfigError` razem z przepisaniem `processing` na `IO`.
