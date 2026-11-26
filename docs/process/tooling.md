# Narzędzia i komendy

Szczegóły narzędzi, których nie trzeba znać przy każdym tasku. Najczęstsze komendy są w `CLAUDE.md` w sekcji „Komendy”.

## Krótkie wyjście sbt

`tools/sbt-lite.sh <argumenty sbt>` uruchamia sbt (z `--server` i `-Dsbt.server.autostart=false`, więc działa obok sbt z IntelliJ), zapisuje całe wyjście do `target/sbt-lite/last.log` i wypisuje tylko:

- wynik i czas (`sbt-lite: OK` albo `sbt-lite: FAILED (exit N)`),
- zsumowane liczby testów,
- przy błędzie nieudane testy (suite i test z miejscem asercji) i pierwsze linie `[error]` (limit `SBT_LITE_ERRORS`, domyślnie 40),
- zawsze problemy, które nie przerywają sbt, a przerywają CI (zepsute linki Scaladoc, `-Werror` javadoc).

`tools/sbt-lite.sh --summary [log]` streszcza zapisany log jeszcze raz. Pełne szczegóły są w logu, więc nie trzeba drugi raz uruchamiać sbt.

## Pokrycie testami

`sbt testCoverage` uruchamia testy z pomiarem pokrycia i przerywa się poniżej progu (90% instrukcji, 80% gałęzi). Raport HTML: `target/out/jvm/scala-<wersja>/coilix/scoverage-report/index.html`. `demo` i plugin kompilatora są wyłączone z pomiaru.

Cache dyskowy sbt 2 przywraca klasy z instrumentacją bez katalogów `scoverage-data`, więc lokalnie trzeba podać pusty katalog cache: `sbt --sbt-cache <pusty katalog> testCoverage`.

## Dokumentacja API i strona

- `sbt doc` generuje dokumentację API każdego modułu do `target/out/jvm/scala-<wersja>/coilix-<moduł>/api` (`02_annotations`: `target/out/jvm/u/coilix-annotations/api`).
- `sbt docs/apiSite` buduje jedną stronę Scaladoc publikowanych modułów z Javadoc adnotacji w `javadoc/` do `target/out/jvm/scala-<wersja>/coilix-docs/api-site`.
- `sbt docs/testFull` kompiluje przykłady z `99_docs/examples/next/` i wczytuje ich pliki konfiguracji.
- W `99_docs/website` (Node.js z `.nvmrc`): `npm ci` instaluje zależności, `npm test` uruchamia testy skryptów strony, `npm run check` sprawdza strony, pliki `changelog/` i licencje npm, `npm run build` buduje stronę w obu językach, `npm start` uruchamia podgląd (`-- --locale pl` dla polskiego).

## Wersje i wydania

- `sbt version` pokazuje wersję każdego modułu wyliczoną z tagów jego grupy (sbt-dynver), a `sbt "show releaseGroup"` grupę modułu. Wersji nie wpisuje się ręcznie.
- `sbt mimaReportBinaryIssues` sprawdza kompatybilność binarną z poprzednim wydaniem tej samej linii.
- `sbt "releaseVerify <grupa> <wersja>"` sprawdza przed wydaniem moduły grupy (wersja z tagu, testy, licencje, MiMa, `checkReleasedCore`). W `99_docs/website` `npm run prepare-release -- <grupa> <wersja>` przygotowuje PR wydania (procedura w [development.md](development.md#wydanie)).

## IntelliJ

- Wspólne konfiguracje uruchomieniowe są w `.run/` (`Demo` oraz `sbt testFull`, `sbt checkFormat`, `sbt demo/run`, `sbt doc`). Nową konfigurację dla wszystkich zapisuje się tam przez „Store as project file”.
- Styl kodu jest w `.idea/codeStyles/`: formatowanie Scali (Ctrl+Alt+L) idzie przez scalafmt z `.scalafmt.conf`, a kod generowany przez IDE ma klamry zamiast składni wcięć.
- IntelliJ buduje własnym kompilatorem (nie przez sbt shell, bo sbt 2 nie kopiuje zasobów do `classes`). Jar pluginu `@Slf4s` tworzy tylko sbt i ma stałą nazwę `coilix-slf4s-plugin.jar` (niezależną od wersji z gita). Gdy IntelliJ zgłasza `No plugin in path ...coilix-slf4s-plugin.jar`, trzeba uruchomić `sbt slf4s-plugin/package`. Jeśli jar istnieje, a błąd zostaje, trzeba zrestartować serwer kompilacji Scali w IntelliJ (trzyma starą wersję jara po przebudowie przez sbt).

## Workflowy GitHuba

- Każde wywołanie sbt ma `--server`, a powłoka to `bash` z `pipefail` (`defaults.run.shell`). Runner sbt 2 domyślnie używa cienkiego klienta, który nie przyjmuje kilku komend naraz i poddaje się, gdy serwer startuje dłużej niż 300 s, a bez `pipefail` `sbt ... | tee` przechodzi mimo błędu.
- Wywołanie sbt idzie przez `.github/scripts/sbt-retry.sh <log> <argumenty>`, który powtarza komendę tylko po błędzie pobierania (wzorce w zmiennej `download_failure`, np. HTTP 429 z Maven Central), a zależności są w cache `sbt-deps-*` z `restore-keys` (ADR 0023). Wyjątkiem jest publikacja (`ci-release`), której nie powtarza się automatycznie.
- Gdy job pada na pobieraniu mimo retry, najpierw sprawdza się, czy ten krok woła `sbt-retry.sh` i czy komunikat z logu pasuje do `download_failure`. Brakujący wzorzec dopisuje się do skryptu.
