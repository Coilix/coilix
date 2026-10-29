# Analiza prawna i publikacji (SCALING-00006)

Wynik taska [SCALING-00006](../../tasks/SCALING-00006_legal_and_publication.md). To analiza robocza, a nie porada prawna. Punkty oznaczone jako „do konsultacji” warto sprawdzić z prawnikiem przed pierwszą publikacją.

## Odpowiedź: czy można opublikować projekt inspirowany Springiem?

**Tak, pod warunkami opisanymi niżej.** Trzy niezależne argumenty:

1. **Spring jest na licencji Apache-2.0.** Ta licencja pozwala używać, modyfikować i redystrybuować nawet sam kod Springa, pod warunkiem zachowania informacji o prawach autorskich i pliku `NOTICE`. Odtworzenie nazw adnotacji i pojęć jest dużo słabszą formą zapożyczenia niż kopia kodu, więc mieści się w tym, na co licencja i tak pozwala.
2. **Interfejs API i funkcjonalność nie są w UE chronione jak kod.** TSUE w sprawie C-406/10 *SAS Institute v World Programming* (2012) orzekł, że funkcjonalność programu, język programowania i format plików nie są chronione prawem autorskim do programu komputerowego (dyrektywa 2009/24/WE). W USA Sąd Najwyższy w *Google v. Oracle* (2021) uznał ponowną implementację deklaracji API Javy za dozwolony użytek (*fair use*).
3. **Kod nie jest kopią.** Framework jest napisany w Scali 3 z inną architekturą (traity, `ConfigPath`, `ComponentProvider`, plugin kompilatora `@Slf4s`), a Spring w Javie. Komunikaty błędów i logi są napisane od nowa. Szczegóły są w [spring-similarity.md](spring-similarity.md).

Ryzyko nie leży w kodzie, tylko w **znaku towarowym**: słowa „Spring” i „Spring Boot” należą do Broadcom i obowiązują je zasady z [spring.io/trademarks](https://spring.io/trademarks). Nazwa „Scaling” ich nie narusza. Ograniczenia opisu projektu są w [spring-similarity.md](spring-similarity.md#znaki-towarowe).

## Decyzje do zatwierdzenia

| # | Decyzja | Rekomendacja | Dokument |
|---|---|---|---|
| 0 | Nazwa projektu | **Mortise** (zatwierdzona 2026-10-29) | [spring-similarity.md](spring-similarity.md#nazwa-projektu) |
| 1 | Licencja projektu | Apache-2.0 | [license-decision.md](license-decision.md) |
| 2 | Nagłówki licencyjne w plikach | brak, tylko `LICENSE` i `NOTICE` | [license-decision.md](license-decision.md#nagłówki-w-plikach) |
| 3 | Model kontrybucji | DCO (`Signed-off-by`), bez CLA | [license-decision.md](license-decision.md#kontrybucje) |
| 4 | `groupId` i pakiety | `dev.mortise`, jeśli domena `mortise.dev` jest wolna, w przeciwnym razie `io.github.<login>.mortise` | [publication-checklist.md](publication-checklist.md#przestrzeń-nazw) |
| 5 | Wersjonowanie | SemVer, jedna wersja dla wszystkich modułów, `early-semver`, wersja z tagu | [../architecture/versioning.md](../architecture/versioning.md) |
| 6 | Wersja Scali publikowanych artefaktów | Scala 3.9 LTS zamiast 3.10 | [../architecture/versioning.md](../architecture/versioning.md#wersja-scali-i-jdk) |
| 7 | Niezależność modułów | `data`, `web`, `security` opcjonalne, podłączane przez SPI | [../architecture/modularity.md](../architecture/modularity.md) |

## Pytania, na które odpowiedzieć może tylko autor

- **Pracodawca i umowy.** Jeśli kod powstaje w ramach obowiązków pracowniczych, majątkowe prawa autorskie do programu należą do pracodawcy (art. 74 ust. 3 ustawy o prawie autorskim i prawach pokrewnych). To samo może wynikać z umowy B2B albo regulaminu pracy. Trzeba potwierdzić, że projekt powstaje poza obowiązkami i na własnym sprzęcie albo mieć zgodę pracodawcy na piśmie.
- **Login GitHub** dla przestrzeni nazw `io.github.<login>` i adresu GitHub Pages.

## Dokumenty

- [license-decision.md](license-decision.md): wybór licencji, nagłówki, kontrybucje, kod tworzony z pomocą AI.
- [dependency-licenses.md](dependency-licenses.md): licencje wszystkich zależności i obowiązki.
- [spring-similarity.md](spring-similarity.md): podobieństwo do Springa, znaki towarowe, nazwa projektu.
- [publication-checklist.md](publication-checklist.md): Maven Central, JetBrains Marketplace, GitHub.
- [../architecture/versioning.md](../architecture/versioning.md): standard wersjonowania i wydań.
- [../architecture/modularity.md](../architecture/modularity.md): niezależność modułów.

## Zadania implementacyjne

- [SCALING-00008](../../tasks/SCALING-00008_license_and_publication_setup.md): licencja, metadane publikacji i przestrzeń nazw.
- [SCALING-00009](../../tasks/SCALING-00009_versioning_and_releases.md): wersjonowanie, kompatybilność i proces wydań.
- [SCALING-00010](../../tasks/SCALING-00010_module_independence.md): niezależność modułów opcjonalnych.
