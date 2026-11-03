# COILIX-00021: Audyt podobieństwa kodu do publicznych projektów

## Cel

Pipeline w GitHub Actions, który przy pull requeście wskazuje fragmenty kodu podobne do kodu z publicznie dostępnych projektów, zwłaszcza open source, i zapisuje czytelny raport do ręcznej oceny.

Wynik skanowania jest sygnałem do sprawdzenia, a nie dowodem. Dopasowanie nie oznacza naruszenia cudzych praw, a brak dopasowań nie dowodzi, że kod nigdzie indziej nie występuje.

## Stan obecny

- Workflow: `ci.yml` (formatowanie, licencje zależności, testy, MiMa, `doc`, pokrycie) i `release.yml`. Nie ma skanera podobieństwa kodu.
- `sbt checkLicenses` sprawdza licencje zależności. To analiza zależności, a nie treści kodu, więc nie wykrywa skopiowanych fragmentów.
- [docs/legal/spring-similarity.md](../docs/legal/spring-similarity.md) opisuje ręczny przegląd podobieństwa do Springa (komunikaty, logi, banner, dokumentacja). Porównanie narzędziem typu PMD CPD między Javą a Scalą uznano tam za niewiarygodne.
- Kod źródłowy: `src/main` i `src/test` modułów `01`–`11` (Scala 3, Scala 2.13 w `11_intellij_plugin`, Java w `02_annotations`), pliki buildu (`build.sbt`, `project/*.scala`). Do pominięcia: `target/`, `.bsp/`, `.idea/`, katalogi generowane przez sbt i przyszłe `12_docs/website/node_modules/`.
- Licencja Apache-2.0, autorstwo przez DCO (`Signed-off-by`), brak kodu vendored.

## Zakres

1. **Rozpoznanie:** potwierdzenie listy katalogów ze źródłami i katalogów do pominięcia.
2. **Wybór narzędzia** na podstawie aktualnej oficjalnej dokumentacji dostawców. Krótkie porównanie czterech różnych rzeczy, których nie wolno traktować jako równoważnych:
   - wykrywanie skopiowanych albo zmodyfikowanych fragmentów kodu (dopasowanie do bazy znanych projektów),
   - analiza zależności i ich licencji,
   - zwykłe wyszukiwanie tekstu w publicznych repozytoriach,
   - sprawdzanie sugestii AI w momencie ich generowania.
3. **Kryteria wyboru:** koszt i potrzeba konta lub klucza API, obsługa Scali 3, Scali 2.13 i Javy, czy kod albo jego odciski są wysyłane do dostawcy, możliwość uruchomienia w GitHub Actions, informacja o znalezionym źródle i jego licencji.
4. **Workflow** `.github/workflows/similarity.yml` zgodny z konwencją `ci.yml`:
   - uruchamiany przy pull requeście i ręcznie (`workflow_dispatch`),
   - skanuje zmieniony kod albo cały projekt, zależnie od zaleceń narzędzia,
   - pomija katalogi z punktu 1,
   - zapisuje raport jako artefakt, a SARIF tylko wtedy, gdy narzędzie poprawnie go obsługuje,
   - minimalne uprawnienia (`permissions`), tokeny tylko w GitHub Secrets,
   - **informacyjny**: nie blokuje PR i nie trafia do wymaganych checków w rulesecie, chyba że narzędzie wiarygodnie oznacza dokładne dopasowania i łatwo ustawić ręczną kontrolę.
5. **Dane wysyłane na zewnątrz:** jeśli narzędzie wymaga wysłania kodu albo odcisków, ta opcja jest domyślnie wyłączona, a dokumentacja opisuje, jak ją włączyć. Włączenie wymaga decyzji opiekuna.
6. **Dokumentacja** w `docs/legal/code-similarity-scan.md`, z linkiem z `docs/legal/README.md`:
   - co pipeline wykrywa, a czego nie,
   - jakie dane opuszczają repozytorium,
   - jak skonfigurować token lub usługę,
   - gdzie jest raport i jak czytać dopasowanie,
   - dlaczego brak wyników nie dowodzi, że kodu nie ma nigdzie indziej,
   - że każde dopasowanie wymaga ręcznej oceny źródła, zakresu kodu i licencji.

## Proponowany projekt

Do ustalenia w kroku 2. Jeśli nie ma narzędzia bezpłatnego, wiarygodnego i obsługującego Scalę, task kończy się zapisem tego wniosku w dokumentacji (z porównaniem narzędzi), bez workflow. Nie powstaje własne rozwiązanie oparte na niepełnym wyszukiwaniu w internecie, przedstawiane jako sprawdzanie oryginalności.

Wybór narzędzia, które wysyła kod do zewnętrznej usługi, zapisuje się jako ADR.

## Kryteria akceptacji

1. Porównanie narzędzi z kryteriami z punktu 3 i uzasadnieniem wyboru (albo uzasadnieniem, że żadne się nie nadaje).
2. Workflow uruchamia się przy PR i ręcznie, pomija katalogi spoza źródeł i zapisuje raport jako artefakt.
3. Workflow nie blokuje scalania na podstawie niezweryfikowanych dopasowań.
4. Żaden sekret nie jest zapisany w repozytorium. Wysyłanie kodu na zewnątrz jest domyślnie wyłączone.
5. Dokumentacja z punktu 6 istnieje i jest podlinkowana z `docs/legal/README.md`.
6. Repozytorium nie zawiera wymyślonych wyników ani przykładowych dopasowań sugerujących, że skanowanie się odbyło.

## Poza zakresem

- Zmiany w kodzie aplikacji i w workflow niezwiązanych z tym taskiem.
- Ocena prawna znalezionych dopasowań. Robi ją opiekun ręcznie dla każdego przypadku.
- Pełna kompilacja i testy projektu poza tym, co potrzebne do sprawdzenia workflow.

## Ryzyka i otwarte pytania

- Komercyjne skanery dopasowań fragmentów (np. z baz projektów open source) często wymagają płatnego konta, a obsługa Scali bywa ograniczona albo żadna.
- Narzędzia oparte na odciskach kodu mogą wysyłać te odciski do dostawcy. Trzeba sprawdzić w dokumentacji, co dokładnie opuszcza repozytorium.
- Scala i Java różnią się składnią, więc kodu przeniesionego z Javy do Scali skaner tekstowy najpewniej nie wykryje. Wtedy dalej obowiązuje ręczny przegląd z `spring-similarity.md`.
