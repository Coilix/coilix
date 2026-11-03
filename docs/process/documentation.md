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
| Changelog | `CHANGELOG.md` | angielski | `/changelog` |
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
    static/img/               obrazy
    scripts/                  skrypty sprawdzające i skrypt nowej wersji
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
- **Linki** między przewodnikami są względne i prowadzą do pliku (`./components.md`), nie do adresu URL. Do API linkuje się ścieżką `/api/next/...` (sekcja [Dokumentacja API](#dokumentacja-api)).
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

## Tłumaczenia

- Angielski jest źródłem. Każda zmiana strony EN zmienia odpowiadającą jej stronę PL **w tym samym PR**. Dotyczy to też nowych i usuwanych stron.
- Strona PL leży pod tą samą ścieżką względną w `i18n/pl/docusaurus-plugin-content-docs/current/` i ma te same `id` oraz `sidebar_position`.
- Teksty interfejsu (menu, stopka, przyciski) tłumaczy się w plikach JSON w `i18n/pl/`. Nowe klucze dodaje `npm run write-translations -- --locale pl`.
- Tłumaczenie maszynowe może być szkicem, ale autor PR czyta je w całości i poprawia przed wysłaniem.
- Changelog i dokumentacja API nie są tłumaczone. Polska strona changelogu pokazuje wersję angielską z jednym zdaniem wyjaśnienia.

## Wersje

- Wersja dokumentacji to linia `MINOR`: `next`, `0.2`, `0.1`. Domyślnie strona pokazuje najnowszą wydaną linię, a `next` jest opisana jako „Next (unreleased)”.
- `docs/` i `i18n/pl/.../current/` zawsze opisują stan `master`. Zmiana kodu widoczna dla użytkownika aktualizuje przewodnik w tym samym PR.
- **Nowa linia** powstaje w PR wydania `X.Y.0` (gałąź `release/vX.Y.0`) przez:

  ```sh
  cd 12_docs/website
  npm run version -- X.Y
  ```

  Skrypt wykonuje `docusaurus docs:version X.Y`, kopiuje przewodniki PL do `version-X.Y`, kopiuje `examples/next/` do `examples/X.Y/` i podmienia w skopiowanych stronach ścieżki `@examples/` oraz `/api/next/` na wersję `X.Y`. Ręcznie nie robi się żadnego z tych kroków.
- **Wydanie poprawkowe** `X.Y.Z` nie tworzy nowej wersji dokumentacji. Poprawka opisu błędu, który dotyczy wydanej linii, trafia do `docs/` i do `versioned_docs/version-X.Y/` (oraz ich odpowiedników PL) w tym samym PR co poprawka kodu.
- Zamrożone przykłady w `examples/X.Y/` zmienia się tylko razem z poprawką w wydanej linii.
- Linie inne niż najnowsza dostają baner „unmaintained”, zgodnie z polityką wsparcia w `SECURITY.md`. Wydanej wersji dokumentacji się nie usuwa, bo zewnętrzne linki przestałyby działać.

## Changelog

- Jedynym źródłem jest `CHANGELOG.md` w formacie [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Zasady wpisów są w sekcji „Wersjonowanie” w `CLAUDE.md`.
- Strona `/changelog` powstaje przy budowaniu z kopii tego pliku. Kopia jest w `.gitignore` i nikt jej nie edytuje.

## Dokumentacja API

- Scaladoc wszystkich publikowanych modułów jest budowany jako jedna strona przez projekt sbt `docs`, a Javadoc `02_annotations` obok niej.
- Adresy: `/api/next/` z `master` oraz `/api/X.Y/` dla każdej wydanej linii, nadpisywane przy każdym wydaniu poprawkowym tej linii.
- Przewodniki w `docs/` linkują do `/api/next/`. Skrypt nowej wersji zamienia te linki na `/api/X.Y/` w kopii wersji.

## Publikacja

- Stronę publikuje tylko GitHub Actions (`.github/workflows/docs.yml`) na gałąź `gh-pages`. Nikt nie wypycha zmian na `gh-pages` ręcznie.
- Push na `master` buduje przewodniki we wszystkich wersjach i obu językach oraz `/api/next/`.
- Tag `vX.Y.Z` buduje `/api/X.Y/`.
- Publikacja przewodników zastępuje całą zawartość `gh-pages` poza katalogiem `api/` i plikiem `CNAME`. Dzięki temu usunięta strona znika ze strony, a dokumentacja API starszych linii zostaje.
- Adres `docs.coilix.dev` wskazuje na GitHub Pages rekordem CNAME w Cloudflare.

## Sprawdzenia w CI

Każdy PR, który zmienia `12_docs/`, `CHANGELOG.md` albo publiczne API, uruchamia budowanie strony. Build przerywa się, gdy:

- link wewnętrzny, link do kotwicy albo link w Markdownie prowadzi donikąd (`onBrokenLinks`, `onBrokenAnchors` i `onBrokenMarkdownLinks` ustawione na `throw`),
- zbiór stron EN i PL różni się w którejkolwiek wersji (brak tłumaczenia albo tłumaczenie bez strony EN),
- strona nie ma któregoś z czterech pól nagłówka albo `id` różni się od nazwy pliku,
- strona nie jest przypisana do żadnego działu w `sidebars.ts`,
- przykład w `examples/next/` się nie kompiluje albo plik konfiguracji się nie wczytuje,
- zależność npm ma licencję spoza listy z `docs/legal/dependency-licenses.md`,
- Scaladoc zgłasza `Couldn't resolve a member for the given link query`.

## Przegląd

Przed scaleniem PR ze zmianą dokumentacji:

- Czy zmiana jest w obu językach i oba teksty mówią to samo?
- Czy strona jest we właściwym dziale i opisuje jeden temat?
- Czy przykłady pochodzą z plików w `examples/next/`?
- Czy przewodnik opisuje tylko to, co jest w kodzie tej wersji?
- Czy nowe terminy są w słowniku?
