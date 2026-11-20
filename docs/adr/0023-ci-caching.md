# 0023: Cache i pomijanie kroków w CI bez pomijania testów

- Status: proponowana
- Data: 2026-11-19
- Task: COILIX-00000

## Kontekst

Przebieg CI trwał ok. 2,5 minuty. Pomiar kroków pokazał, że przygotowanie środowiska (JDK, sbt, zależności) zajmuje ok. 10 s, a reszta to start sbt (ok. 35 s w każdym jobie), kompilacja i testy. Kompilacja trafiała w cache tylko w ok. 72%, bo `sbt/setup-sbt` zapisuje cache dyskowy sbt 2 pod kluczem z datą poniedziałku, więc przez tydzień przywraca ten sam, coraz starszy zapis. Każda zmiana, także sama dokumentacja, uruchamiała cały build, a job JDK 25 powtarzał formatowanie, licencje i MiMa.

Gotowy obraz Dockera nie pomaga na runnerach GitHuba: obraz z JDK, sbt i zależnościami jest pobierany od zera w każdym jobie (20–40 s), a obecne przygotowanie trwa 10 s.

Warunek opiekuna: optymalizacja nie może pominąć testu, który mógłby nie przejść.

## Decyzja

- Każdy job, który buduje, uruchamia `testFull`. Odrzucamy `testQuick` w PR-ach i pomijanie przebiegu po znaczniku wcześniejszego sukcesu dla tej samej treści, bo pominęłyby test niestabilny albo zależny od czegoś spoza wejść.
- `.github/scripts/needs-build.sh` pomija kroki buildu tylko w pull requeście, w którym zmieniły się wyłącznie pliki z listy, której nie czyta żadne zadanie sbt, test ani skrypt strony: `docs/**`, `tasks/**`, `.idea/**`, `.run/**` i opisowe pliki `.md` w katalogu głównym, a dla buildu także `changelog/**` (sprawdza go job strony). Każda inna ścieżka, pusty diff, brak commita scalającego i każde zdarzenie poza PR uruchamiają wszystko. Pomijane są kroki, a nie joby, więc wymagane checki z nazwami z macierzy są zawsze zgłaszane.
- Cache dyskowy sbt 2 (`~/.cache/sbt`, klasy kluczowane skrótem wejść) jest zapisywany po każdym pushu na `master` pod kluczem z SHA commita i przywracany w PR-ach z najnowszego zapisu dla danego JDK. PR-y go nie zapisują. Job pokrycia testami dalej działa bez cache, bo przywrócone klasy z instrumentacją nie mają katalogów `scoverage-data`.
- Pobrane zależności i sam sbt (`~/.cache/coursier`, `~/.ivy2/cache`, `~/.sbt/boot`) są w osobnym cache we wszystkich jobach, także w jobie pokrycia, pod kluczem ze skrótu plików buildu (`build.sbt`, `project/*.scala`, `project/*.sbt`, `project/build.properties`). Zmieniony build przywraca najnowszy starszy wpis (`restore-keys`) i pobiera tylko to, co nowe. Wcześniejsze `cache: sbt` z `setup-java` przywracało tylko wpis o identycznym kluczu, więc każda zmiana buildu i każdy przebieg pokrycia pobierały wszystko od zera, a Maven Central odpowiadał wtedy HTTP 429.
- Każde wywołanie sbt w workflowach idzie przez `.github/scripts/sbt-retry.sh`. Skrypt uruchamia całą komendę ponownie (do 4 prób, z przerwą 1, 2 i 3 minuty) tylko wtedy, gdy log zawiera błąd pobierania. Błąd testu, kompilacji albo formatowania kończy job od razu. Ponowienie powtarza wszystko, więc niczego nie pomija. Joby mają limit 60 minut.
- Job buildu wywołuje sbt raz z listą zadań zamiast osobnego startu klienta na każdy krok.
- JDK 25 uruchamia `testFull` i `doc`. Formatowanie, licencje i MiMa nie zależą od JDK, a wydania są sprawdzane na JDK 17 (`releaseVerify`).

## Konsekwencje

- PR zmieniający jeden moduł kompiluje tylko ten moduł i moduły od niego zależne. PR z samymi ADR-ami albo taskami kończy build i pokrycie po kilku sekundach.
- Nowy test, skrypt albo zadanie buildu, które czyta plik z listy w `needs-build.sh`, wymaga usunięcia tej ścieżki z listy w tym samym PR. Komentarz w skrypcie o tym przypomina.
- Wpisy cache z każdego commita na `master` zajmują miejsce w limicie 10 GB repozytorium. GitHub usuwa najstarsze, a PR-y biorą najnowszy.
- Ponowienie wydłuża job tylko wtedy, gdy repozytorium odmawia pobrania. Nowa zależność pobiera się raz, a potem trafia do cache.
- Własny runner z trwałym cache i obrazem może przyspieszyć start sbt, ale wymaga utrzymania maszyny i zasad dla PR-ów z forków. To osobna decyzja.
