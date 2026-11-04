# Zasady dokumentacji

Jak piszemy, tłumaczymy, wersjonujemy i publikujemy dokumentację Coilix. Decyzja o narzędziu jest w [ADR 0010](../adr/0010-docusaurus-documentation-site.md), a zasady Scaladoc w sekcji „Dokumentacja” w [CLAUDE.md](../../CLAUDE.md).

Zasada ogólna: każda informacja ma jedno miejsce. Jeśli coś trzeba napisać drugi raz, linkuje się do pierwszego miejsca.

## Rodzaje dokumentacji

| Rodzaj | Gdzie | Język | Na stronie |
|---|---|---|---|
| Dokumentacja API | Scaladoc i Javadoc w `src/main` | angielski | `/api/<wersja>/` |
| Przewodniki | `12_docs/website/docs/` | angielski | `/docs/` |
| Tłumaczenie przewodników | `12_docs/website/i18n/pl/` | polski | `/pl/docs/` |
| Przykłady kodu | `12_docs/examples/` | kod | wklejane do przewodników |
| Changelog | `changelog/<grupa>/` | angielski | `/changelog` |
| README | `README.md` | angielski | nie, tylko GitHub |
| Dokumenty wewnętrzne (proces, ADR, sprawy prawne, taski) | `docs/`, `tasks/` | polski | nigdy |

Strona powstaje wyłącznie z `12_docs/`. Nic z `docs/` ani `tasks/` nie trafia na stronę, nawet przez link.

## Struktura `12_docs`

```
12_docs/
  examples/
    next/                     przykłady dla bieżącej wersji, kompilowane przez sbt
      scala/                  pliki .scala
      resources/              pliki .yaml, .conf, .properties
    0.1/                      zamrożone przykłady wydanej linii, tylko do odczytu
  src/test/scala/             test, który wczytuje pliki konfiguracji z examples/next/resources
  website/
    docs/                     przewodniki EN dla wersji next
    versioned_docs/           kopie przewodników EN wydanych linii (generowane)
    versioned_sidebars/       kopie menu wydanych linii (generowane)
    versions.json             lista wydanych linii (generowana)
    i18n/pl/
      docusaurus-plugin-content-docs/
        current/              przewodniki PL dla wersji next
        version-0.1/          przewodniki PL wydanych linii
      docusaurus-theme-classic/  tłumaczenia menu i stopki (generowane, potem tłumaczone)
      code.json               tłumaczenia tekstów interfejsu
    src/pages/                strona startowa i changelog
    src/components/           komponenty React używane przez strony
    src/generated/            changelog.md z plików changelog/ (generowany przy budowaniu, poza gitem)
    static/img/               obrazy
    scripts/                  skrypty sprawdzające i ich testy, changelog i skrypt nowej wersji
      allowed-licenses.json   dozwolone licencje zależności npm
    docusaurus.config.ts
    sidebars.ts
    package.json, package-lock.json, .nvmrc
```

Katalogów oznaczonych jako „generowane” nie zakłada się ręcznie. Powstają przez skrypt nowej wersji (sekcja [Wersje](#wersje)).

## Struktura menu

Menu ma stałe działy, w tej kolejności:

| Dział | Katalog | Zawartość |
|---|---|---|
| Getting started | `getting-started/` | instalacja, pierwsza aplikacja, struktura projektu |
| Guides | `guides/` | jedna funkcja na stronę: komponenty, wstrzykiwanie, konfiguracja, start aplikacji, `@Slf4s`, plugin IntelliJ |
| Reference | `reference/` | tabele: adnotacje, klucze konfiguracji `coilix.*`, punkty rozszerzeń |
| Migration | `migration/` | przejście między liniami, np. `0.1-to-0.2.md` |

Nowy dział, zmiana kolejności działów albo strona poza nimi wymaga zmiany tego dokumentu w tym samym PR. Menu jest opisane w `sidebars.ts` jawnie, bez automatycznego generowania z katalogów.

## Strona przewodnika

- **Plik:** nazwa po angielsku w kebab-case (`configuration-properties.mdx`), taka sama w obu językach. Rozszerzenie `.mdx`, gdy strona wkleja przykład, w innym przypadku `.md`.
- **Nagłówek pliku (front matter)** ma zawsze cztery pola:

  ```yaml
  ---
  id: configuration-properties
  title: Configuration properties
  description: Bind a section of the configuration to a case class.
  sidebar_position: 3
  ---
  ```

  `id` jest równe nazwie pliku i nie zmienia się po publikacji, bo jest częścią adresu strony.
- **Jeden temat na stronę.** Strona dłuższa niż około 1500 słów dzieli się na kilka.
- **Pierwszy akapit** mówi, czego czytelnik się nauczy i kiedy tego potrzebuje.
- **Nagłówki** do poziomu `###`. Tytuły w sentence case: „Configuration properties”, nie „Configuration Properties”.
- **Linki** między przewodnikami są względne i prowadzą do pliku (`./components.md`), nie do adresu URL. Do API linkuje się ścieżką `pathname:///api/next/...` (sekcja [Dokumentacja API](#dokumentacja-api)). Przedrostek `pathname://` jest potrzebny, bo dokumentacja API nie jest częścią builda Docusaurusa i zwykła ścieżka `/api/...` przerwałaby build jako zepsuty link.
- **Obrazy** w `static/img/<dział>/`, w formacie SVG albo PNG, zawsze z tekstem alternatywnym.
- **Wyróżnienia** tylko trzech rodzajów: `:::note`, `:::tip`, `:::warning`.

## Styl

- **Angielski:** zwracamy się do czytelnika per „you”, czas teraźniejszy, krótkie zdania, pisownia amerykańska.
- **Polski:** zwracamy się per „ty”, czas teraźniejszy, krótkie zdania. Nazwy adnotacji, typów i kluczy zostają w oryginale.
- Opisujemy, jak działa Coilix. Nie porównujemy się ze Springiem ani nie kopiujemy jego dokumentacji, także w parafrazie (`docs/legal/spring-similarity.md`).
- Przewodnik opisuje zachowanie wersji, do której należy. Funkcja, której nie ma w wydaniu, nie pojawia się w jego dokumentacji, nawet z dopiskiem „wkrótce”.

### Słownik terminów

| English | Polski |
|---|---|
| bean | bean |
| application context | kontekst aplikacji |
| dependency injection | wstrzykiwanie zależności |
| component scanning | skanowanie komponentów |
| configuration | konfiguracja |
| configuration key | klucz konfiguracji |
| placeholder | placeholder |
| runner | runner |
| extension point | punkt rozszerzenia |
| startup | start aplikacji |
| shutdown | zamykanie aplikacji |

Nowy termin trafia do słownika w PR, w którym pojawia się pierwszy raz.

## Przykłady kodu

- Każdy przykład w języku Scala, YAML, HOCON albo properties to osobny plik w `12_docs/examples/next/`. Strona wkleja cały plik:

  ```mdx
  import CodeBlock from '@theme/CodeBlock';
  import FirstApplication from '!!raw-loader!@examples/scala/FirstApplication.scala';

  <CodeBlock language="scala" title="FirstApplication.scala">{FirstApplication}</CodeBlock>
  ```

- Pliki `.scala` kompiluje projekt sbt `docs` z tymi samymi zasadami co reszta kodu (`-Werror`, wartremover, scalafmt). Pliki konfiguracji wczytuje test w tym projekcie, więc błąd składni też przerywa build.
- Jeden przykład to jeden plik, bez wycinania fragmentów. Krótki przykład jest lepszy niż długi plik z zaznaczonym fragmentem.
- W tekście strony wolno wpisać kod ręcznie tylko wtedy, gdy nie jest to Scala ani plik konfiguracji, np. komendę `sbt` albo linię `libraryDependencies`.
- Oba języki wklejają ten sam plik przykładu. Kod i identyfikatory w przykładach są po angielsku.
- Przykłady są w pakiecie `com.example`, tak jak kod aplikacji czytelnika, a nie w pakiecie `dev.coilix`.
- Alias `@examples` wskazuje na `examples/next/`, a `@examples-X.Y` na zamrożone przykłady wydanej linii. Aliasy definiuje wtyczka `coilix-examples` w `docusaurus.config.ts`.
- Strony `.md` są czytane jako CommonMark, a `.mdx` jako MDX (`markdown.format: 'detect'`), więc znaki `<` i `{` w zwykłym tekście psują tylko strony `.mdx`.

## Tłumaczenia

- Angielski jest źródłem. Każda zmiana strony EN zmienia odpowiadającą jej stronę PL **w tym samym PR**. Dotyczy to też nowych i usuwanych stron.
- Strona PL leży pod tą samą ścieżką względną w `i18n/pl/docusaurus-plugin-content-docs/current/` i ma te same `id` oraz `sidebar_position`.
- Teksty interfejsu (menu, stopka, przyciski) tłumaczy się w plikach JSON w `i18n/pl/`. Nowe klucze dodaje `npm run write-translations -- --locale pl`.
- Tłumaczenie maszynowe może być szkicem, ale autor PR czyta je w całości i poprawia przed wysłaniem.
- Changelog i dokumentacja API nie są tłumaczone. Polska strona changelogu pokazuje wersję angielską z jednym zdaniem wyjaśnienia.

## Wersje

- Wersja dokumentacji to linia `MINOR`: `next`, `0.2`, `0.1`. Domyślnie strona pokazuje najnowszą wydaną linię, a `next` jest opisana jako „Next (unreleased)”.
- `docs/` i `i18n/pl/.../current/` zawsze opisują stan `master`. Zmiana kodu widoczna dla użytkownika aktualizuje przewodnik w tym samym PR.
- **Nowa linia** powstaje w PR wydania `X.Y.0` (gałąź `release/core-vX.Y.0`) przez:

  ```sh
  cd 12_docs/website
  npm run version -- X.Y
  ```

  Skrypt wykonuje `docusaurus docs:version X.Y`, kopiuje przewodniki PL do `version-X.Y`, kopiuje `examples/next/` do `examples/X.Y/` i podmienia w skopiowanych stronach `@examples/` na `@examples-X.Y/` oraz `/api/next/` na `/api/X.Y/`. Ręcznie nie robi się żadnego z tych kroków.
- **Wydanie poprawkowe** `X.Y.Z` nie tworzy nowej wersji dokumentacji. Poprawka opisu błędu, który dotyczy wydanej linii, trafia do `docs/` i do `versioned_docs/version-X.Y/` (oraz ich odpowiedników PL) w tym samym PR co poprawka kodu.
- Zamrożone przykłady w `examples/X.Y/` zmienia się tylko razem z poprawką w wydanej linii.
- Linie inne niż najnowsza dostają baner „unmaintained”, zgodnie z polityką wsparcia w `SECURITY.md`. Wydanej wersji dokumentacji się nie usuwa, bo zewnętrzne linki przestałyby działać.

## Changelog

- Jedynym źródłem jest katalog `changelog/` w katalogu głównym: podkatalog dla każdej grupy wydań, `unreleased.md` i plik `<wersja>.md` dla każdego wydania ([ADR 0011](../adr/0011-release-groups-and-changelog-files.md)). Format opisuje [changelog/README.md](../../changelog/README.md), a zasady wpisów sekcja „Wersjonowanie” w `CLAUDE.md`.
- Strona `/changelog` powstaje przy budowaniu (`scripts/generate-changelog.mjs`) z plików wydań, bez `unreleased.md`: najpierw `core`, potem moduły funkcji alfabetycznie, na końcu plugin IntelliJ, w każdej grupie od najnowszej wersji. Wydanie ma kotwicę `#<grupa>-<wersja>`, np. `/changelog#core-0.1.0`, a wydanie modułu funkcji pokazuje linię rdzenia z `requires-core`.
- Wygenerowany plik jest w `.gitignore` i nikt go nie edytuje.
- Skrypty changelogu są w `scripts/changelog.mjs` i mają testy w `scripts/changelog.test.mjs` (`npm test`). `release.yml` bierze z nich notkę wydania (`scripts/release-notes.mjs`).

## Dokumentacja API

- Scaladoc wszystkich publikowanych modułów Scali (`common`, `slf4s-plugin`, `config`, `context`, `boot`) jest budowany jako jedna strona przez projekt sbt `docs`, a Javadoc `02_annotations` obok niej w katalogu `javadoc/`. Robi to `sbt docs/apiSite`, wynik jest w `target/out/jvm/scala-<wersja>/coilix-docs/api-site/`.
- Projekt `docs` podaje do Scaladoc pliki TASTy tych modułów (`Compile / doc / tastyFiles`), więc nie potrzebuje sbt-unidoc. Nowy publikowany moduł Scali dopisuje się do tej listy w `build.sbt`.
- Linki Scaladoc do adnotacji z `dev.coilix.annotation` prowadzą do Javadoc tej samej linii (`-external-mappings`).
- Adresy: `/api/next/` z `master` oraz `/api/X.Y/` dla każdej wydanej linii, nadpisywane przy każdym wydaniu poprawkowym tej linii.
- Przewodniki w `docs/` linkują do `/api/next/`. Skrypt nowej wersji zamienia te linki na `/api/X.Y/` w kopii wersji.

## Publikacja

- Stronę publikuje tylko GitHub Actions (`.github/workflows/docs.yml`) na gałąź `gh-pages`. Nikt nie wypycha zmian na `gh-pages` ręcznie.
- Push na `master` buduje przewodniki we wszystkich wersjach i obu językach oraz `/api/next/`.
- Tag `core-vX.Y.Z` buduje `/api/X.Y/`.
- Publikacja przewodników zastępuje całą zawartość `gh-pages` poza katalogiem `api/` i plikiem `CNAME`. Dzięki temu usunięta strona znika ze strony, a dokumentacja API starszych linii zostaje.
- Adres `docs.coilix.dev` wskazuje na GitHub Pages rekordem CNAME w Cloudflare.

## Sprawdzenia w CI

Job `Documentation site` w `.github/workflows/ci.yml` buduje stronę w każdym PR i przy każdym pushu na `master`, bez publikacji. Działa zawsze, a nie tylko przy zmianie `12_docs/`, bo zmiana publicznego API albo pliku w `changelog/` też może zepsuć stronę. Build przerywa się, gdy:

| Problem | Co sprawdza |
|---|---|
| link wewnętrzny, link do kotwicy albo link w Markdownie prowadzi donikąd | `npm run build` (`onBrokenLinks`, `onBrokenAnchors` i `markdown.hooks.onBrokenMarkdownLinks` ustawione na `throw`) |
| zbiór stron EN i PL różni się w którejkolwiek wersji albo tłumaczenie ma inne `id` lub `sidebar_position` | `npm run check` (`scripts/check-pages.mjs`) |
| strona nie ma któregoś z czterech pól nagłówka albo `id` różni się od nazwy pliku | `npm run check` |
| strona nie jest w `sidebars.ts` albo dział najwyższego poziomu nie jest jednym z działów z sekcji [Struktura menu](#struktura-menu) lub stoi w złej kolejności | `npm run check` |
| strona ma blok kodu Scali albo pliku konfiguracji wpisany ręcznie (poza liniami `libraryDependencies`, `addCompilerPlugin`, `addSbtPlugin` i `scalaVersion`) | `npm run check` |
| przykład w `examples/next/` się nie kompiluje albo plik konfiguracji się nie wczytuje | `sbt docs/testFull` |
| plik w `changelog/` ma zły format (opis w `changelog/README.md`), np. nieznaną sekcję, wpis bez ID taska albo nagłówek wydania niezgodny z nazwą pliku | `npm run check` (`scripts/check-changelog.mjs`) |
| skrypt strony nie przechodzi swoich testów | `npm test` |
| zależność npm ma licencję spoza `scripts/allowed-licenses.json` | `npm run check` (`scripts/check-licenses.mjs`) |
| Scaladoc zgłasza `Couldn't resolve a member for the given link query` | `sbt docs/apiSite` i `sbt doc` w jobie `Build` |

Lokalnie te same sprawdzenia uruchamia się w `12_docs/website` przez `npm ci`, `npm test`, `npm run check` i `npm run build`, a podgląd strony przez `npm start` (tylko angielski) albo `npm start -- --locale pl`.

## Przegląd

Przed scaleniem PR ze zmianą dokumentacji:

- Czy zmiana jest w obu językach i oba teksty mówią to samo?
- Czy strona jest we właściwym dziale i opisuje jeden temat?
- Czy przykłady pochodzą z plików w `examples/next/`?
- Czy przewodnik opisuje tylko to, co jest w kodzie tej wersji?
- Czy nowe terminy są w słowniku?
