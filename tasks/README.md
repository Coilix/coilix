# Taski

Backlog kolejnych etapów implementacji. Jeden plik to jeden task. Identyfikator `COILIX-NNNNN` wyznacza kolejność realizacji i jest tym samym numerem, którym zaczynają się wiadomości commitów. `COILIX-00000` oznacza zmiany spoza tasków (szkielet projektu, build, narzędzia).

| ID | Task | Priorytet | Status |
|---|---|---|---|
| COILIX-00001 | [Wsparcie `@Slf4s` w IntelliJ](COILIX-00001_intellij_slf4s_support.md) | najwyższy | zaimplementowany, czeka na weryfikację w IDE |
| COILIX-00002 | [Uruchamianie aplikacji w stylu Spring Boot](COILIX-00002_spring_like_startup.md) | wysoki | zrobiony |
| COILIX-00003 | [Konfiguracja: pliki, `@Value`, `@Configuration` i nadpisywanie komponentów](COILIX-00003_configuration.md) | wysoki | zrobiony |
| COILIX-00004 | [Cykl życia beanów: `@PostConstruct` i `@PreDestroy`](COILIX-00004_bean_lifecycle.md) | wysoki | do zrobienia |
| COILIX-00005 | [Zarządzanie wątkami](COILIX-00005_thread_management.md) | średni | do zrobienia |
| COILIX-00006 | [Legalność, licencjonowanie i publikacja](COILIX-00006_legal_and_publication.md) | wysoki | zrobiony ([docs/legal](../docs/legal/README.md)) |
| COILIX-00007 | [Dokumentacja API i strona na GitHub Pages z wersjami](COILIX-00007_documentation_site.md) | wysoki | do zrobienia |
| COILIX-00008 | [Nazwa Coilix, licencja, przestrzeń nazw i metadane publikacji](COILIX-00008_license_and_publication_setup.md) | wysoki | zrobiony |
| COILIX-00009 | [Wersjonowanie, kompatybilność i proces wydań](COILIX-00009_versioning_and_releases.md) | wysoki | wersje, CI i publikacja gotowe, zostały sekrety, TASTy-MiMa i plugin w Marketplace |
| COILIX-00010 | [Niezależność modułów opcjonalnych](COILIX-00010_module_independence.md) | wysoki | do zrobienia |
| COILIX-00011 | [Proces developmentu i model gałęzi](COILIX-00011_development_workflow.md) | wysoki | zrobiony, reguły ochrony gałęzi po upublicznieniu repozytorium |

Zależności między taskami:

- COILIX-00003 korzysta z `ApplicationArguments` z COILIX-00002 (opcje z linii poleceń nadpisują config).
- COILIX-00004 potrzebuje zamykania kontekstu, które wprowadza COILIX-00002 (shutdown hook).
- COILIX-00005 korzysta z `@PreDestroy` i zamykania kontekstu z COILIX-00004, żeby wyłączać pule wątków, a rozmiary pul czyta z configu z COILIX-00003.
- COILIX-00006 nie zależy od innych tasków, ale musi być skończony przed pierwszą publikacją.
- COILIX-00007: Scaladoc w kodzie i moduł `12_docs` można robić od razu, ale publikacja na GitHub Pages czeka na upublicznienie repozytorium i wersję z tagu z COILIX-00009.
- COILIX-00008 wymaga zatwierdzonych decyzji z COILIX-00006. Zmiana pakietów dotyka całego kodu, więc warto ją zrobić przed COILIX-00007 (Scaladoc linkuje pełnymi nazwami).
- COILIX-00010 zmienia zestaw publikowanych modułów, więc musi być przed pierwszym wydaniem z COILIX-00009.
- COILIX-00009 wymaga COILIX-00008 (metadane POM, przestrzeń nazw).
- COILIX-00011 opisuje proces, według którego realizowane są taski od COILIX-00008. Ochrona gałęzi na GitHubie czeka na upublicznienie repozytorium.

## Zasady

Każdy task realizujemy zgodnie z [CLAUDE.md](../CLAUDE.md): najpierw trait, jeden typ w pliku, brak `var` i `println`, testy munit dla każdej nowej klasy, `[T: ClassTag]` zamiast `Class[T]` w API.

Task prowadzi się według [procesu developmentu](../docs/process/development.md): własna gałąź `feature/COILIX-NNNNN-opis`, PR do `master`, definicja ukończenia. Po skończeniu zmień status w tabeli powyżej.

## Szablon taska

```markdown
# COILIX-NNNNN: Tytuł

## Cel
## Stan obecny
## Zakres
## Proponowany projekt
## Kryteria akceptacji
## Poza zakresem
## Ryzyka i otwarte pytania
```
