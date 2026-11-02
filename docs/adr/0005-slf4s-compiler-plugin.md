# 0005: Logger `@Slf4s` przez plugin kompilatora

- Status: przyjęta
- Data: 2026-10-30 (zapis wsteczny)
- Task: COILIX-00001

## Kontekst

Każda klasa frameworka i wiele klas aplikacji potrzebuje loggera SLF4J. Ręczne `private val log = LoggerFactory.getLogger(...)` to powtarzany kod, a mixin trait zmienia hierarchię typów i publiczne API klasy.

## Decyzja

- Adnotacja `@Slf4s` (Java, `02_annotations`) i plugin kompilatora Scali 3 (`03_slf4s_plugin`), który dodaje pole `log: org.slf4j.Logger` w trakcie kompilacji.
- Plugin IntelliJ (`11_intellij_plugin`) dodaje to samo pole jako syntetyczny element w edytorze. Jest w Scali 2.13, bo działa w classloaderze pluginu Scala dla IntelliJ ([README](../../11_intellij_plugin/README.md)).

## Konsekwencje

- Zero kosztu w runtime i brak zmian w hierarchii typów.
- Plugin kompilatora zależy od wewnętrznego API kompilatora Scali, więc nowa wersja Scali może wymagać poprawek.
- Plugin IntelliJ trzeba utrzymywać razem z wersjami IDE i pluginu Scala.
- Instrumentacja pokrycia (scoverage) psuje ładowanie pluginu, więc `slf4s-plugin` jest wyłączony z pomiaru pokrycia.
