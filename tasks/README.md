# Taski

Backlog kolejnych etapów implementacji. Jeden plik to jeden task. Identyfikator `SCALING-NNNNN` wyznacza kolejność realizacji i jest tym samym numerem, którym zaczynają się wiadomości commitów. `SCALING-00000` oznacza zmiany spoza tasków (szkielet projektu, build, narzędzia).

| ID | Task | Priorytet | Status |
|---|---|---|---|
| SCALING-00001 | [Wsparcie `@Slf4s` w IntelliJ](SCALING-00001_intellij_slf4s_support.md) | najwyższy | zaimplementowany, czeka na weryfikację w IDE |
| SCALING-00002 | [Uruchamianie aplikacji w stylu Spring Boot](SCALING-00002_spring_like_startup.md) | wysoki | zrobiony |
| SCALING-00003 | [Konfiguracja: pliki, `@Value`, `@Configuration` i nadpisywanie komponentów](SCALING-00003_configuration.md) | wysoki | zrobiony |
| SCALING-00004 | [Cykl życia beanów: `@PostConstruct` i `@PreDestroy`](SCALING-00004_bean_lifecycle.md) | wysoki | do zrobienia |
| SCALING-00005 | [Zarządzanie wątkami](SCALING-00005_thread_management.md) | średni | do zrobienia |
| SCALING-00006 | [Legalność, licencjonowanie i publikacja](SCALING-00006_legal_and_publication.md) | wysoki | analiza gotowa ([docs/legal](../docs/legal/README.md)), czeka na zatwierdzenie decyzji |
| SCALING-00007 | [Dokumentacja API i strona na GitHub Pages z wersjami](SCALING-00007_documentation_site.md) | wysoki | do zrobienia |
| SCALING-00008 | [Nazwa Mortise, licencja, przestrzeń nazw i metadane publikacji](SCALING-00008_license_and_publication_setup.md) | wysoki | do zrobienia |
| SCALING-00009 | [Wersjonowanie, kompatybilność i proces wydań](SCALING-00009_versioning_and_releases.md) | wysoki | do zrobienia |
| SCALING-00010 | [Niezależność modułów opcjonalnych](SCALING-00010_module_independence.md) | wysoki | do zrobienia |

Zależności między taskami:

- SCALING-00003 korzysta z `ApplicationArguments` z SCALING-00002 (opcje z linii poleceń nadpisują config).
- SCALING-00004 potrzebuje zamykania kontekstu, które wprowadza SCALING-00002 (shutdown hook).
- SCALING-00005 korzysta z `@PreDestroy` i zamykania kontekstu z SCALING-00004, żeby wyłączać pule wątków, a rozmiary pul czyta z configu z SCALING-00003.
- SCALING-00006 nie zależy od innych tasków, ale musi być skończony przed pierwszą publikacją.
- SCALING-00007: Scaladoc w kodzie i moduł `12_docs` można robić od razu, ale publikacja na GitHub Pages czeka na upublicznienie repozytorium i wersję z tagu z SCALING-00009.
- SCALING-00008 wymaga zatwierdzonych decyzji z SCALING-00006. Zmiana pakietów dotyka całego kodu, więc warto ją zrobić przed SCALING-00007 (Scaladoc linkuje pełnymi nazwami).
- SCALING-00010 zmienia zestaw publikowanych modułów, więc musi być przed pierwszym wydaniem z SCALING-00009.
- SCALING-00009 wymaga SCALING-00008 (metadane POM, przestrzeń nazw).

## Zasady

Każdy task realizujemy zgodnie z [CLAUDE.md](../CLAUDE.md): najpierw trait, jeden typ w pliku, brak `var` i `println`, testy munit dla każdej nowej klasy, `[T: ClassTag]` zamiast `Class[T]` w API.

Task jest skończony, gdy wszystkie kryteria akceptacji są spełnione, `sbt testFull` przechodzi, a `sbt demo/run` pokazuje nową funkcję w działaniu. Po skończeniu zmień status w tabeli powyżej.

## Szablon taska

```markdown
# SCALING-NNNNN: Tytuł

## Cel
## Stan obecny
## Zakres
## Proponowany projekt
## Kryteria akceptacji
## Poza zakresem
## Ryzyka i otwarte pytania
```
