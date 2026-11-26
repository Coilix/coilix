# 0046: Logowanie: logF z @Slf4s i własny backend SLF4J

- Status: proponowana
- Data: 2026-11-26
- Task: COILIX-00033

## Kontekst

`@Slf4s` (ADR 0005) dodaje do klasy pole `log` typu `org.slf4j.Logger`. Od ADR 0042 i 0043 kod frameworka i aplikacji jest w `IO`, więc każde wywołanie logu trzeba było opakowywać w `IO(log.info(...))`. Biblioteka implicits opiekuna ma `LoggableF` z polem `logF` w efekcie i własny backend SLF4J 2: poziomy nadpisywane per pakiet i klasa z konfiguracji, konsolę z kolorami, skrócone nazwy loggerów i appender asynchroniczny.

Opiekun 2026-11-26 chce w Coilix: `logF` dodawane przez `@Slf4s` jak w implicits, wyłączanie loggerów i nadpisywanie ich poziomów z configu, także dla całych pakietów, zmianę poziomów w runtime i format logów z implicits. Biblioteki dodawane do aplikacji (Kafka, http4s, sterowniki) mają logować w tym samym formacie.

## Decyzja

- **`IOLogger` w `common`.** `dev.coilix.common.logging.IOLogger` opakowuje `Logger` SLF4J metodami `trace`, `debug`, `info`, `warn` i `error` (z wyjątkiem i bez), które zwracają `IO[Unit]`. Wiadomość jest parametrem przekazywanym przez nazwę i powstaje tylko przy włączonym poziomie. Bez `F[_]`, bo Coilix stoi na `IO` (ADR 0043). `common` dostaje zależność od cats-effect i slf4j-api, więc `IOLogger` jest dostępny w każdym module, w którym działa `@Slf4s`.
- **`@Slf4s` dodaje `logF`.** Plugin kompilatora obok `private final val log` dodaje `private final val logF = IOLogger(log)`, gdy `IOLogger` jest na classpath kompilacji (sprawdza to przed typerem przez `getClassIfDefined`). Klasa z własnym `log` nie dostaje żadnego pola, a klasa z własnym `logF` je zachowuje. Plugin IntelliJ w `coilix-intellij` musi znać nowe pole, co jest osobną zmianą w tamtym repozytorium.
- **Backend jako moduł funkcji `logging`** (kolejny krok COILIX-00033), wzorowany na implicits, pisany od nowa w stylu Coilix:
  - provider SLF4J 2, więc każda biblioteka logująca przez SLF4J pisze w formacie Coilix; mosty dla `java.util.logging`, Log4j 2 i Commons Logging, żeby inne API też trafiały do tego samego backendu,
  - poziom główny i poziomy per pakiet albo klasa z `coilix.logging`, dopasowane do najdłuższego prefiksu nazwy; `off` wyłącza logger,
  - zmiana poziomów w runtime przez bean modułu z API w `IO`,
  - format z implicits: czas, wątek, poziom w kolorze, skrócona nazwa loggera, wiadomość i stack trace, z opcjonalnym appenderem asynchronicznym,
  - konfiguracja wczytywana przy pierwszym użyciu z tych samych plików co reszta aplikacji, a po starcie kontekstu uzupełniana o opcje wiersza poleceń.
  Aplikacja, która woli Logback albo Log4j 2, nie dodaje modułu.

## Konsekwencje

- Kod w `IO` loguje przez `logF.info(...)`, bez `IO(...)`. Zasada w `CLAUDE.md` mówi o obu polach.
- Każdy moduł z `common` ma cats-effect na classpath. Kontener nadal działa synchronicznie (ADR 0042).
- Moduł `logging` jest backendem tylko dla aplikacji, które go dodadzą. Testy frameworka zostają przy slf4j-simple i Log4j 2 tam, gdzie sprawdzają MDC.
