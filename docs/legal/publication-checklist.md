# Lista kroków do publikacji

## Przestrzeń nazw

Maven Central wymaga `groupId` w przestrzeni nazw, którą kontrolujemy: domena potwierdzona rekordem DNS TXT albo `io.github.<login>`, który Central Portal przyznaje automatycznie po zalogowaniu kontem GitHub.

**Domena `.org` zbieżna z roboczą nazwą projektu jest zarejestrowana przez kogoś innego**, więc odpada jako `groupId`. Pakiet oparty na tej domenie też powinien się zmienić, bo konwencja JVM zakłada pakiety z odwróconej domeny właściciela, a zostawienie cudzej domeny grozi kolizją klas.

Projekt zmienia nazwę na **Coilix** ([spring-similarity.md](spring-similarity.md#nazwa-projektu)).

**Decyzja (2026-11-03): `groupId` `dev.coilix` i pakiety `dev.coilix.*`** ([ADR 0009](../adr/0009-dev-coilix-namespace.md)). Domena [coilix.dev](https://coilix.dev) należy do projektu. Central Portal weryfikuje przestrzeń nazw rekordem DNS TXT w tej domenie.

Wcześniejsza decyzja z 2026-10-29 (`io.github.coilix`, weryfikacja przez repozytorium organizacji GitHub [Coilix](https://github.com/Coilix)) obowiązywała do zakupu domeny. Nic nie zostało pod nią wydane.

Rozważane i odrzucone warianty:

| Wariant | `groupId` | Dlaczego nie |
|---|---|---|
| `io.github.<login>` | `io.github.pawelwojcik7` | długie importy, nazwa związana z osobą |
| organizacja GitHub | `io.github.coilix` | nazwa zależna od jednego serwisu, dłuższe importy, inny adres niż strona i poczta projektu |

Przestrzeń nazw zmienia się tylko przed pierwszym wydaniem, bo później to zmiana niezgodna dla wszystkich użytkowników.

## Maven Central (Central Portal)

Wymagania według [central.sonatype.org/publish/requirements](https://central.sonatype.org/publish/requirements/):

- [ ] konto na central.sonatype.com i zweryfikowana przestrzeń nazw,
- [ ] wersja bez `-SNAPSHOT`,
- [ ] dla każdego jara: `-sources.jar` i `-javadoc.jar` (dla Scali: Scaladoc z `sbt doc`),
- [ ] podpis PGP (`.asc`) dla każdego pliku, klucz publiczny na publicznym serwerze kluczy,
- [ ] sumy `.md5` i `.sha1` (generuje je narzędzie publikujące),
- [ ] POM z polami: `name`, `description`, `url`, `licenses`, `developers`, `scm` (`connection`, `developerConnection`, `url`),
- [ ] bez `<repositories>` w POM-ie,
- [ ] narzędzie publikujące zgodne z sbt 2 (sbt-ci-release ma build dla sbt 2, wersję trzeba sprawdzić przy wdrożeniu),
- [ ] sekrety w GitHub Actions: `PGP_SECRET`, `PGP_PASSPHRASE`, `SONATYPE_USERNAME`, `SONATYPE_PASSWORD` (token z Central Portal),
- [ ] `publish / skip := true` dla `root` i `demo`,
- [ ] `scala3-compiler` w `slf4s-plugin` jako `Provided`.

Publikowane artefakty: `common`, `annotations`, `slf4s-plugin`, `config`, `context`, `boot`, później `data`, `web`, `security`. Nie publikujemy `demo`.

## JetBrains Marketplace

Plugin IntelliJ jest w osobnym repozytorium [Coilix/coilix-intellij](https://github.com/Coilix/coilix-intellij) (ADR 0014) i tam ma własną listę kroków publikacji w Marketplace.

## GitHub

- [x] decyzja o upublicznieniu repozytorium (publiczne od 2026-11-02),
- [ ] `README.md`: opis, przykład, instalacja, zastrzeżenie o braku powiązania ze Springiem i atrybucja znaku (wzór w [spring-similarity.md](spring-similarity.md#znaki-towarowe)),
- [ ] `LICENSE`, `NOTICE`,
- [ ] `CONTRIBUTING.md` (DCO, zasady kodu, format commitów),
- [x] changelog w katalogu `changelog/` ([versioning.md](../architecture/versioning.md#changelog)),
- [ ] `SECURITY.md` z adresem do zgłaszania podatności,
- [ ] `CODE_OF_CONDUCT.md` (Contributor Covenant),
- [ ] ochrona gałęzi `master`: wymagane CI, bez force-push,
- [ ] historia gita bez sekretów i danych prywatnych (np. `git log -p | grep` po tokenach; e-mail autora w commitach będzie publiczny, można go zamienić na adres `noreply` GitHuba).

## Kolejność

1. Decyzje z [README.md](README.md#decyzje) i odpowiedź w sprawie praw pracodawcy (zrobione 2026-10-29).
2. COILIX-00008: licencja, przestrzeń nazw, metadane, README.
3. COILIX-00010: niezależność modułów (zmienia strukturę artefaktów, więc przed pierwszym wydaniem).
4. COILIX-00009: wersjonowanie i CI wydań.
5. Upublicznienie repozytorium, pierwsze wydanie `0.1.0`, potem COILIX-00007 (dokumentacja na GitHub Pages).
