# COILIX-00006: Legalność, licencjonowanie i publikacja

## Cel

Ustalić, czy i na jakich warunkach projekt można opublikować: wybrać licencję, sprawdzić licencje zależności, upewnić się, że kod nie jest plagiatem Springa ani innego frameworka, i przygotować listę wymagań do publikacji artefaktów (Maven Central, JetBrains Marketplace).

Ten task to analiza i decyzje. Implementacja (pliki licencji, konfiguracja publikacji) to osobne taski utworzone na jego podstawie.

## Stan obecny

- Brak pliku `LICENSE`, `NOTICE` i informacji o licencji w `build.sbt` (`licenses`, `homepage`, `scmInfo`, `developers`).
- `organization` wskazuje domenę `.org` zbieżną z nazwą projektu, ale nie wiadomo, czy mamy prawo do tej przestrzeni nazw.
- Framework świadomie naśladuje API Springa: nazwy adnotacji (`@Component`, `@Service`, `@Autowired`, `@Value`, `@Configuration`, `@Bean`, `@ConfigurationProperties`), pojęcia (`ApplicationContext`, `ApplicationRunner`) i dokumentacja („w stylu Springa”).
- Moduł `11_intellij_plugin` korzysta z IntelliJ Platform SDK i pluginu Scala.
- Zasada „bez komentarzy” z `CLAUDE.md` koliduje z ewentualnymi nagłówkami licencyjnymi w plikach.

## Zakres

1. **Wybór licencji projektu**
   - porównanie Apache-2.0, MIT i EPL-2.0 pod kątem zgodności z zależnościami, ochrony patentowej i wymagań `NOTICE`,
   - rekomendacja z uzasadnieniem.
2. **Audyt licencji zależności**
   - pełna lista zależności z licencjami (bezpośrednie i przechodnie), np. przez `sbt-license-report` albo `sbt dependencyLicenseInfo`,
   - w szczególności: Scala 3, Typesafe Config, snakeyaml-engine, Reflections, SLF4J, munit, wartremover, scalafmt, IntelliJ Platform SDK i plugin Scala,
   - zgodność każdej licencji z wybraną licencją projektu i obowiązki (atrybucja, `NOTICE`, kopia licencji w dystrybucji).
3. **Analiza podobieństwa do Springa (plagiat, prawa autorskie)**
   - potwierdzenie, że żaden fragment kodu, dokumentacji ani testów nie został skopiowany ze Springa ani innego projektu (porównanie narzędziem do wykrywania duplikatów, np. PMD CPD albo ScanCode, na źródłach Spring Framework i Spring Boot),
   - ocena, czy odtwarzanie nazw i kształtu API (adnotacje, pojęcia) jest dozwolone, z odwołaniem do licencji Apache-2.0 Springa i orzecznictwa dotyczącego API (np. Google v. Oracle w USA, dyrektywa 2009/24/WE i sprawa SAS Institute v World Programming w UE),
   - lista miejsc, gdzie podobieństwo wykracza poza API (np. identyczne komunikaty błędów, teksty dokumentacji, banner), do przeredagowania.
4. **Znaki towarowe i nazewnictwo**
   - „Spring” i „Spring Boot” to znaki towarowe Broadcom. Sprawdzić, jak wolno ich używać w opisie projektu (użycie opisowe „inspired by”) i czego unikać (nazwa, logo, sugestia powiązania),
   - sprawdzić kolizje nazwy projektu i pakietu z istniejącymi projektami i znakami towarowymi.
5. **Prawa autorskie i pochodzenie kodu**
   - kto jest właścicielem praw (autor, ewentualny pracodawca, umowy),
   - status prawny kodu powstałego z pomocą narzędzi generujących kod i warunki tych narzędzi,
   - decyzja o modelu kontrybucji zewnętrznych: DCO albo CLA.
6. **Wymagania publikacji**
   - Maven Central (Sonatype Central Portal): weryfikacja przestrzeni nazw (domena albo `io.github.<login>`), podpis PGP, jary źródeł i dokumentacji, metadane POM, kompatybilność wtyczek publikujących z sbt 2,
   - JetBrains Marketplace dla `11_intellij_plugin`: wymagania licencyjne, vendor, Plugin Verifier, zgodność z warunkami Marketplace,
   - publikacja kodu źródłowego (GitHub): `LICENSE`, `NOTICE`, `README` z zastrzeżeniem o braku powiązania ze Springiem, `CONTRIBUTING`.
7. **Raport końcowy** w `docs/legal/` z decyzjami, uzasadnieniem i listą tasków implementacyjnych.

## Proponowany projekt

- `docs/legal/license-decision.md`: wybór licencji i uzasadnienie.
- `docs/legal/dependency-licenses.md`: tabela zależności, licencja, obowiązki i źródło informacji.
- `docs/legal/spring-similarity.md`: wynik porównania kodu, ocena API i lista zmian do wprowadzenia.
- `docs/legal/publication-checklist.md`: lista kroków do Maven Central i JetBrains Marketplace.
- Decyzja, jak pogodzić nagłówki licencyjne z zasadą „bez komentarzy” (np. tylko plik `LICENSE` bez nagłówków w plikach).

## Kryteria akceptacji

1. Wybrana licencja projektu z uzasadnieniem.
2. Kompletna lista zależności z licencjami, bez niewyjaśnionych pozycji i bez konfliktów z licencją projektu.
3. Raport podobieństwa do Springa: brak skopiowanego kodu albo lista miejsc do przepisania.
4. Rekomendacja co do nazwy, pakietu i opisu projektu względem znaków towarowych.
5. Lista wymagań publikacji dla Maven Central i JetBrains Marketplace.
6. Lista tasków implementacyjnych dopisana do backlogu.

## Poza zakresem

- Dodawanie plików `LICENSE`/`NOTICE`, konfiguracja `build.sbt` do publikacji i sama publikacja.
- Wiążąca porada prawna. Wynik to analiza robocza, a sporne punkty trzeba skonsultować z prawnikiem.

## Ryzyka i otwarte pytania

- Ocena prawna ochrony API różni się między USA a UE. Przy wątpliwościach decyduje konsultacja z prawnikiem.
- Licencje przechodnich zależności mogą być niepełne w metadanych POM i wymagać ręcznej weryfikacji.
- Obecna przestrzeń nazw na Maven Central wymaga domeny. Bez niej trzeba zmienić `organization`, co dotyka wszystkich pakietów.
- Status praw autorskich do kodu tworzonego z pomocą narzędzi generujących zależy od jurysdykcji i warunków tych narzędzi.
