# Lista kroków do publikacji

## Przestrzeń nazw

Maven Central wymaga `groupId` w przestrzeni nazw, którą kontrolujemy: domena potwierdzona rekordem DNS TXT albo `io.github.<login>`, który Central Portal przyznaje automatycznie po zalogowaniu kontem GitHub.

**Domena `.org` zbieżna z roboczą nazwą projektu jest zarejestrowana przez kogoś innego**, więc odpada jako `groupId`. Pakiet oparty na tej domenie też powinien się zmienić, bo konwencja JVM zakłada pakiety z odwróconej domeny właściciela, a zostawienie cudzej domeny grozi kolizją klas.

Projekt zmienia nazwę na **Coilix** ([spring-similarity.md](spring-similarity.md#nazwa-projektu)).

**Decyzja (2026-10-29): `groupId` `io.github.coilix` i pakiety `io.github.coilix.*`.** Przestrzeń nazw należy do organizacji GitHub [Coilix](https://github.com/Coilix), a nie do osoby, więc przekazanie projektu innym opiekunom nie wymaga zmiany `groupId`. Central Portal weryfikuje ją przez publiczne repozytorium o podanej nazwie w organizacji, bez domeny i DNS.

Rozważane i odrzucone warianty:

| Wariant | `groupId` | Dlaczego nie |
|---|---|---|
| `io.github.<login>` | `io.github.pawelwojcik7` | długie importy, nazwa związana z osobą |
| własna domena | `dev.coilix` | koszt i weryfikacja DNS. Można przejść później, ale zmiana pakietu to zmiana niezgodna, więc tylko przed `1.0.0` |

Zmiana pakietu dotyka wszystkich plików, `idePackagePrefix` w `build.sbt`, kluczy `class` w configach (`io.github.coilix.boot.DefaultStartupMessageFormatter`) i dokumentacji. Trzeba ją zrobić **przed** pierwszym wydaniem, razem ze zmianą nazwy (COILIX-00008).

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
- [ ] `publish / skip := true` dla `root`, `demo` i `intellij-plugin`,
- [ ] `scala3-compiler` w `slf4s-plugin` jako `Provided`.

Publikowane artefakty: `common`, `annotations`, `slf4s-plugin`, `config`, `context`, `boot`, później `data`, `web`, `security`. Nie publikujemy `demo` ani `intellij-plugin` (ten idzie do JetBrains Marketplace).

## JetBrains Marketplace (`11_intellij_plugin`)

- [ ] konto vendora w JetBrains Marketplace (osoba albo organizacja), zaakceptowany Developer Agreement,
- [ ] `plugin.xml`: `id` w przestrzeni nazw projektu, `name` bez „Spring” i bez znaków JetBrains w sposób sugerujący oficjalny plugin, `vendor` z e-mailem i URL, `description`, `change-notes`, zakres `since-build`/`until-build`, zależność `org.intellij.scala`,
- [ ] licencja pluginu (Apache-2.0) i link do kodu źródłowego,
- [ ] przejście [IntelliJ Plugin Verifier](https://github.com/JetBrains/intellij-plugin-verifier) dla deklarowanego zakresu IDE,
- [ ] podpisanie pluginu certyfikatem: sprawdzić aktualny wymóg Marketplace; budowa pluginu sbt-em zamiast Gradle wymaga osobnego kroku podpisu, np. narzędziem `marketplace-zip-signer`,
- [ ] ikona pluginu (`pluginIcon.svg`) bez elementów logo Springa ani Scali.

## GitHub

- [ ] decyzja o upublicznieniu repozytorium (GitHub Pages dla repozytoriów prywatnych wymaga płatnego planu),
- [ ] `README.md`: opis, przykład, instalacja, zastrzeżenie o braku powiązania ze Springiem i atrybucja znaku (wzór w [spring-similarity.md](spring-similarity.md#znaki-towarowe)),
- [ ] `LICENSE`, `NOTICE`,
- [ ] `CONTRIBUTING.md` (DCO, zasady kodu, format commitów),
- [ ] `CHANGELOG.md` ([versioning.md](../architecture/versioning.md#changelog)),
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
