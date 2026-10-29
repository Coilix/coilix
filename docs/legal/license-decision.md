# Licencja projektu

## Rekomendacja: Apache-2.0

| Kryterium | Apache-2.0 | MIT | EPL-2.0 |
|---|---|---|---|
| Zgodność z zależnościami (Apache-2.0, MIT, BSD-3-Clause) | tak | tak | tak |
| Wyraźna licencja patentowa od kontrybutorów | tak (sekcja 3) | nie | tak |
| Wygaśnięcie licencji przy pozwie patentowym | tak | nie | tak |
| Copyleft | nie | nie | słaby, na poziomie pliku |
| Obowiązki użytkownika | `LICENSE`, `NOTICE`, oznaczenie zmian | kopia licencji | udostępnienie zmian w plikach EPL |
| Typowa w ekosystemie | Scala, Spring, Typesafe Config, munit | SLF4J, wiele bibliotek Scali | Eclipse, Jakarta EE |

Apache-2.0 wygrywa, bo:

- ma licencję patentową, która ma znaczenie dla frameworka używanego komercyjnie,
- jest licencją Scali, Springa i większości zależności, więc użytkownicy nie muszą jej osobno analizować,
- nie nakłada copyleftu, który zniechęca firmy (EPL-2.0),
- jej sekcja 5 sprawia, że kontrybucje domyślnie wchodzą na tej samej licencji, więc nie trzeba CLA.

MIT byłaby prostsza, ale nie daje ochrony patentowej. To dobra alternatywa tylko wtedy, gdy zależy nam na najkrótszej licencji.

## Co trzeba dodać

- `LICENSE`: pełny tekst Apache-2.0.
- `NOTICE`: `Coilix`, rok i autor praw. Bez wpisów o zależnościach, bo publikujemy tylko nasze jary, bez dołączania cudzych klas (patrz [dependency-licenses.md](dependency-licenses.md#obowiązki)).
- W `build.sbt`: `licenses`, `homepage`, `scmInfo`, `developers`, `startYear`.
- `META-INF/LICENSE` i `META-INF/NOTICE` w każdym publikowanym jarze.
- Sekcja „License” w `README.md`.

## Nagłówki w plikach

Apache-2.0 zaleca nagłówek w każdym pliku, ale go nie wymaga. Licencja obowiązuje przez plik `LICENSE` w repozytorium i w jarze oraz wpis w POM.

**Decyzja: bez nagłówków.** Jest to zgodne z zasadą „bez zwykłych komentarzy” z `CLAUDE.md` i nie wymaga wyjątku w scalafmt ani w regułach. Jeśli kiedyś pojawi się wymóg nagłówków (np. przy przekazaniu projektu fundacji), dodamy je jednorazowo pluginem sbt-header i zmienimy regułę.

## Kontrybucje

**Decyzja: DCO bez CLA.** Kontrybutor potwierdza [Developer Certificate of Origin](https://developercertificate.org/) przez `Signed-off-by` w commicie (`git commit -s`). Razem z sekcją 5 Apache-2.0 to wystarcza do przyjmowania zmian. CLA ma sens dopiero wtedy, gdy planujemy zmianę licencji albo podwójne licencjonowanie komercyjne.

Do dodania: `CONTRIBUTING.md` z opisem DCO, zasadami z `CLAUDE.md` i formatem commitów oraz sprawdzenie DCO w CI (np. aplikacja DCO na GitHubie).

## Kod tworzony z pomocą narzędzi AI

- Część kodu powstaje z pomocą narzędzi generujących kod. Prawo autorskie w UE chroni utwory będące „własną intelektualną twórczością autora”, a w USA Copyright Office wymaga wkładu człowieka. Fragmenty wygenerowane bez twórczego wkładu człowieka mogą nie być chronione prawem autorskim.
- **Nie blokuje to publikacji.** Licencja Apache-2.0 obejmuje to, do czego autor ma prawa. Brak ochrony części kodu oznacza najwyżej, że trudniej byłoby dochodzić naruszeń, a nie że użytkownik ryzykuje.
- Warunki dostawców narzędzi AI (np. Anthropic) przekazują użytkownikowi prawa do wyników w zakresie, w jakim istnieją. Przed publikacją warto potwierdzić aktualne warunki usługi, z której korzystamy.
- Ryzykiem byłoby wygenerowanie przez narzędzie fragmentu chronionego cudzego kodu. Ogranicza to analiza podobieństwa ze [spring-similarity.md](spring-similarity.md) i przegląd każdej zmiany przed commitem.

## Do konsultacji z prawnikiem

- Prawa pracodawcy do kodu (art. 74 ust. 3 PrAut), jeśli autor jest zatrudniony jako programista.
