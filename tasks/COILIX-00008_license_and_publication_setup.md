# COILIX-00008: Nazwa Coilix, licencja, przestrzeń nazw i metadane publikacji

## Cel

Wprowadzić decyzje z [docs/legal/](../docs/legal/README.md): nową nazwę projektu (Coilix), licencję, nową przestrzeń nazw, metadane wymagane przez Maven Central i pliki repozytorium potrzebne do upublicznienia.

## Stan obecny

- Projekt ma roboczą nazwę, którą trzeba zmienić. Nazwa występuje w ponad 230 plikach: w pakietach, typach, kluczach configu i dokumentacji.
- Brak `LICENSE`, `NOTICE`, `README.md`, `CONTRIBUTING.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md`.
- `organization` i pakiety oparte na domenie `.org`, która należy do kogoś innego.
- `scala3-compiler` w `slf4s-plugin` jest zwykłą zależnością.
- Log startu `Started <app> in <n> seconds` brzmi prawie jak log Spring Boota.

## Zakres

1. Zatwierdzenie decyzji z [docs/legal/README.md](../docs/legal/README.md#decyzje) i potwierdzenie praw do kodu (zrobione 2026-10-29).
2. `LICENSE` (Apache-2.0) i `NOTICE`.
3. Zmiana nazwy i przestrzeni nazw w jednym kroku (`git mv` dla plików):
   - `groupId` i pakiety: `io.github.coilix`, `idePackagePrefix` w `build.sbt`,
   - typy: `Coilix`, `CoilixApplication`, `CoilixApplicationRunner`, `CoilixBootstrap`, `CoilixException` i ich implementacje `Default*` oraz testy,
   - klucze configu pod `coilix.*` (wszystkie `ConfigPath`, `application.yaml` w demo, zasoby testowe), klucze `class` z pełnymi nazwami klas,
   - `name := "coilix"` w `root`, nazwa jara pluginu IntelliJ (`coilix-slf4s.jar`) i jego `plugin.xml`,
   - banner w `10_demo` z napisem „Coilix”,
   - `CLAUDE.md` (tytuł, tabela modułów, pakiety, przykłady), dokumenty w `docs/` i taski.
4. Metadane w `build.sbt`: `licenses`, `homepage`, `scmInfo`, `developers`, `startYear`, `description` dla każdego modułu.
5. `META-INF/LICENSE` i `META-INF/NOTICE` w publikowanych jarach.
6. `publish / skip := true` dla `root`, `demo`, `intellij-plugin`. `scala3-compiler` w `slf4s-plugin` jako `Provided`.
7. `README.md`: opis, przykład, instalacja, zastrzeżenie o braku powiązania ze Springiem, atrybucja znaku Spring, licencja.
8. `CONTRIBUTING.md` (DCO, zasady kodu, commity), `SECURITY.md`, `CODE_OF_CONDUCT.md`.
9. Nowy tekst logu startu zamiast `Started <app> in <n> seconds`.
10. Audyt licencji w CI: zadanie sbt, które przerywa build, gdy pojawi się zależność z licencją spoza listy dozwolonych z [dependency-licenses.md](../docs/legal/dependency-licenses.md#obowiązki).
11. Sprawdzenie nazwy „Coilix” w TMview (EUIPO) i USPTO (klasy 9 i 42). Wynik dopisać do [spring-similarity.md](../docs/legal/spring-similarity.md#nazwa-projektu).

## Kryteria akceptacji

1. Repozytorium ma `LICENSE`, `NOTICE`, `README.md`, `CONTRIBUTING.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md`.
2. `sbt publishLocal` tworzy POM-y z licencją, SCM, deweloperami i URL-em, a jary zawierają `META-INF/LICENSE`.
3. Żaden plik nie używa starej nazwy w pakietach, typach ani kluczach configu, `sbt testFull` i `sbt demo/run` przechodzą, a demo pokazuje banner Coilix.
4. Audyt licencji działa w buildzie i przechodzi.
5. Wynik sprawdzenia nazwy jest zapisany.

## Poza zakresem

- Sama publikacja na Maven Central i Marketplace (COILIX-00009).

## Ryzyka i otwarte pytania

- Zmiana pakietu przerwie configi użytkowników, ale przed pierwszym wydaniem nie ma jeszcze użytkowników. Po wydaniu taka zmiana byłaby zmianą `MAJOR`.
- Katalog lokalny i projekt w IntelliJ trzeba przemianować ręcznie po zmianie. Konfiguracja Claude Code dla projektu jest przypisana do ścieżki katalogu.
