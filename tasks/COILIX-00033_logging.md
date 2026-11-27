# COILIX-00033: Logowanie: logF i backend z poziomami z configu

> Decyzje opiekuna z 2026-11-26: logowanie przenosimy z implicits. `logF` dodaje `@Slf4s`, aplikacja wyłącza loggery i nadpisuje ich poziomy przez config, także dla całych pakietów, poziomy da się zmienić w runtime, a format logów jest jak w implicits. Biblioteki dodawane do aplikacji mają logować w tym samym formacie. Kierunek zapisuje ADR 0046.

## Cel

- Kod w `IO` loguje przez `logF` bez opakowywania w `IO(...)`.
- Aplikacja z modułem `logging` ma jeden format logów dla własnego kodu i wszystkich bibliotek.
- Poziomy i wyłączenia loggerów są w `coilix.logging`, a w działającej aplikacji można je zmienić bez restartu.

## Kroki (osobne PR-y)

1. `IOLogger` w `common`, pole `logF` z `@Slf4s`, przewodnik po logowaniu, ADR 0046 i ten task. Framework i demo używają `logF` w kodzie w `IO`.
2. Moduł funkcji `logging`: provider SLF4J 2, poziomy z `coilix.logging` (poziom główny, mapa nazw pakietów i klas, `off`), format z implicits (czas, wątek, poziom w kolorze, skrócona nazwa loggera), appender asynchroniczny, MDC, mosty `jul-to-slf4j`, `log4j-to-slf4j` i `jcl-over-slf4j`, bean do zmiany poziomów w runtime z API w `IO`. Demo przechodzi z Log4j 2 na moduł.
3. Plugin IntelliJ w `coilix-intellij` zna pole `logF`.
4. `logF[F]` dla dowolnego efektu z `Sync` (`LoggerF[F]`), na wzór `logF.info[F]` z implicits, obok `logF` w `IO`: zrobione.

## Kryteria akceptacji

1. Klasa z `@Slf4s` w module z `common` ma `logF`, a kod demo i frameworka w `IO` nie zawiera `IO(log.…)`.
2. `coilix.logging.levels { "org.apache.kafka": warn, "com.example.noisy": off }` zmienia poziomy loggerów tych pakietów i ich podpakietów.
3. Zmiana poziomu przez bean modułu działa od razu i nie wymaga restartu.
4. Log biblioteki na `java.util.logging` ma ten sam format co log aplikacji.
