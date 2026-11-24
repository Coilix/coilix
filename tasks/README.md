# Taski

Backlog kolejnych etapów implementacji. Jeden plik to jeden task. Identyfikator `COILIX-NNNNN` wyznacza kolejność realizacji i jest tym samym numerem, którym zaczynają się wiadomości commitów. `COILIX-00000` oznacza zmiany spoza tasków (szkielet projektu, build, narzędzia).

| ID | Task | Priorytet | Status |
|---|---|---|---|
| COILIX-00001 | [Wsparcie `@Slf4s` w IntelliJ](COILIX-00001_intellij_slf4s_support.md) | najwyższy | przeniesiony do [Coilix/coilix-intellij](https://github.com/Coilix/coilix-intellij) (ADR 0014), sprawdzony ręcznie w IDE |
| COILIX-00002 | [Uruchamianie aplikacji w stylu Spring Boot](COILIX-00002_spring_like_startup.md) | wysoki | zrobiony |
| COILIX-00003 | [Konfiguracja: pliki, `@Value`, `@Configuration` i nadpisywanie komponentów](COILIX-00003_configuration.md) | wysoki | zrobiony |
| COILIX-00004 | [Cykl życia beanów: `@PostConstruct` i `@PreDestroy`](COILIX-00004_bean_lifecycle.md) | wysoki | zrobiony |
| COILIX-00005 | [Zarządzanie wątkami](COILIX-00005_thread_management.md) | średni | zrobiony (`06_concurrency`, `@Scheduled`, `@Async`, pula obliczeniowa i `BlockingExecutor`, przekazywanie MDC; ADR 0020, 0022, 0028 i 0029) |
| COILIX-00006 | [Legalność, licencjonowanie i publikacja](COILIX-00006_legal_and_publication.md) | wysoki | zrobiony ([docs/legal](../docs/legal/README.md)) |
| COILIX-00007 | [Dokumentacja API i strona na GitHub Pages z wersjami](COILIX-00007_documentation_site.md) | wysoki | w trakcie: Scaladoc, Javadoc, szkielet `99_docs` i workflow publikacji gotowe, dalej przewodniki; strona wyjdzie z `core-v0.1.0` |
| COILIX-00008 | [Nazwa Coilix, licencja, przestrzeń nazw i metadane publikacji](COILIX-00008_license_and_publication_setup.md) | wysoki | zrobiony |
| COILIX-00009 | [Wersjonowanie, kompatybilność i proces wydań](COILIX-00009_versioning_and_releases.md) | wysoki | wersje, CI, publikacja i grupy wydań z ADR 0011 gotowe (changelog, grupy w buildzie, wydanie przy tagu grupy, publikacja strony); zostało TASTy-MiMa (plugin IntelliJ i jego Marketplace w `coilix-intellij`, ADR 0014) |
| COILIX-00010 | [Niezależność modułów opcjonalnych](COILIX-00010_module_independence.md) | wysoki | zrobiony (`security-web` odłożony do pierwszej integracji, przenumerowanie w ADR 0018) |
| COILIX-00011 | [Proces developmentu i model gałęzi](COILIX-00011_development_workflow.md) | wysoki | zrobiony |
| COILIX-00012 | [Kolejność tworzenia przy zależnościach `Option[T]` i `List[T]`](COILIX-00012_optional_dependency_ordering.md) | wysoki | zrobiony |
| COILIX-00013 | [Wsparcie testów (`coilix-testkit`)](COILIX-00013_testkit.md) | wysoki | zrobiony (`16_testkit`, demo w `90_demo`, ADR 0016) |
| COILIX-00014 | [Profile i warunkowe beany](COILIX-00014_profiles_and_conditions.md) | wysoki | zrobiony |
| COILIX-00015 | [Zasięgi beanów i leniwe tworzenie](COILIX-00015_bean_scopes.md) | średni | zrobiony (`@Prototype`, `@Lazy`, `Provider[T]`, ADR 0030) |
| COILIX-00016 | [Zdarzenia aplikacji (`events`)](COILIX-00016_application_events.md) | wysoki | zrobione (fazy zamykania, `RetryPolicy`, moduł `08_events`, zdarzenia frameworka i testkit, ADR 0024–0026) |
| COILIX-00017 | [Benchmarki startu](COILIX-00017_startup_benchmarks.md) | średni | do zrobienia |
| COILIX-00018 | [Szablon nowego projektu](COILIX-00018_project_template.md) | niski | do zrobienia, po pierwszym wydaniu |
| COILIX-00019 | [Przestrzeń nazw `dev.coilix`](COILIX-00019_dev_coilix_namespace.md) | najwyższy | zrobiony |
| COILIX-00020 | [Integracja z cats-effect (`IO`)](COILIX-00020_effect_types.md) | wysoki | w trakcie: kroki 1–4, 5a, `@DependsOn`, 5b i 5c część 1 zrobione (ADR 0019, 0028, 0031–0034: `ContextPlan` z poziomami, moduł `23_cats_effect` z `IORuntime`, beany `IO` i `Resource`, cykl życia i runnery w `IO`, `CoilixIOApp`), dalej 5c część 2: `testkit-cats-effect` |
| COILIX-00021 | [Audyt podobieństwa kodu do publicznych projektów](COILIX-00021_code_similarity_audit.md) | średni | do zrobienia, przed pierwszym wydaniem |
| COILIX-00022 | [Typy wartości, enumy, walidacja i `EditOption` z biblioteki implicits](COILIX-00022_implicits_port.md) | wysoki | zrobiony w rdzeniu, mapowanie kolumn czeka na moduł `data` |
| COILIX-00023 | [Moduł HTTP (`web`)](COILIX-00023_http.md) | wysoki | szkic do doszlifowania |
| COILIX-00024 | [Serializacja: opis typu, circe i most do HTTP](COILIX-00024_serialization.md) | wysoki | szkic do doszlifowania |
| COILIX-00025 | [GraphQL na Sangrii](COILIX-00025_graphql.md) | średni | szkic do doszlifowania, po COILIX-00023 i COILIX-00024 |
| COILIX-00026 | [Przetwarzanie w tle (`processing`)](COILIX-00026_background_processing.md) | wysoki | szkic do doszlifowania, krok 2 (`ComponentCollector`) zrobiony (ADR 0017) |
| COILIX-00027 | [Klaster replik (`cluster`)](COILIX-00027_cluster.md) | wysoki | w trakcie: krok 1 zrobiony (`07_cluster`, tożsamość repliki, ADR 0021) |
| COILIX-00028 | [Statyczne kontrole zasad kodu](COILIX-00028_static_code_checks.md) | wysoki | szkic do doszlifowania, krok 3 przed `core-v0.1.0` |
| COILIX-00029 | [Integracja z Pekko Streams](COILIX-00029_pekko_streams.md) | wysoki | szkic do doszlifowania |
| COILIX-00030 | [Diagnostyka i błędy fatalne](COILIX-00030_diagnostics_and_fatal_errors.md) | wysoki | w toku (krok 1 z 3: błędy fatalne wyłączają aplikację, ADR 0027) |

Zależności między taskami:

- COILIX-00003 korzysta z `ApplicationArguments` z COILIX-00002 (opcje z linii poleceń nadpisują config).
- COILIX-00004 potrzebuje zamykania kontekstu, które wprowadza COILIX-00002 (shutdown hook).
- COILIX-00005 korzysta z `@PreDestroy` i zamykania kontekstu z COILIX-00004, żeby wyłączać pule wątków, a rozmiary pul czyta z configu z COILIX-00003.
- COILIX-00006 nie zależy od innych tasków, ale musi być skończony przed pierwszą publikacją.
- COILIX-00007: Scaladoc w kodzie i moduł `99_docs` można robić od razu, ale publikacja na GitHub Pages czeka na upublicznienie repozytorium i wersję z tagu z COILIX-00009.
- COILIX-00008 wymaga zatwierdzonych decyzji z COILIX-00006. Zmiana pakietów dotyka całego kodu, więc warto ją zrobić przed COILIX-00007 (Scaladoc linkuje pełnymi nazwami).
- COILIX-00010 zmienia zestaw publikowanych modułów, więc musi być przed pierwszym wydaniem z COILIX-00009.
- COILIX-00009 wymaga COILIX-00008 (metadane POM, przestrzeń nazw).
- COILIX-00011 opisuje proces, według którego realizowane są taski od COILIX-00008. Ochrona gałęzi działa od upublicznienia repozytorium 2026-11-02.
- COILIX-00012 to poprawka błędu, warto ją zrobić przed COILIX-00013 i COILIX-00014, które dokładają kolejne źródła beanów.
- Numery katalogów nowych modułów wynikają z zakresów z ADR 0018, bez przenumerowania istniejących.
- COILIX-00014 korzysta z `CoilixModule` z COILIX-00010 (`@ConditionalOnMissingBean` dla beanów z modułów).
- COILIX-00015 zmienia `RegisteredBean`, z którego korzysta `@PreDestroy` z COILIX-00004, więc idzie po nim.
- COILIX-00016 korzysta z `TaskExecutor`, `TaskScheduler` i faz zamykania z COILIX-00005 (fazy i `RetryPolicy` dodaje w kroku 1) oraz z `GenericType` z COILIX-00020. Zdarzenia są tylko w pamięci jednego procesu, a trwałe przetwarzanie, outbox i Kafka należą do COILIX-00026. `RetryPolicy` współdzieli z COILIX-00026.
- COILIX-00018 wymaga pierwszego wydania z COILIX-00009.
- COILIX-00020 korzysta z zamykania kontekstu z COILIX-00004 (finalizery zasobów), z modułów z COILIX-00010, z zasobów wykonania z COILIX-00005 (albo dodaje minimalne) i z testkitu z COILIX-00013. Pełne typy w definicjach beanów i `ContextPlan` zmieniają API rdzenia, więc powinny trafić przed `core-v0.1.0`. Daje `IORuntime` modułom `processing-cats-effect`, `web-cats-effect` i `pekko-cats-effect`.
- COILIX-00021 uzupełnia ręczny przegląd z `docs/legal/spring-similarity.md` (COILIX-00006). Warto go zrobić przed pierwszym wydaniem z COILIX-00009.
- COILIX-00022 rozszerza `ReflectiveConfigBinder` z COILIX-00003. Mapowanie kolumn czeka na specyfikację modułu `data`, a `Registry` z implicits trafia do COILIX-00010.
- COILIX-00023 korzysta z `CoilixModule` z COILIX-00010 i typów z COILIX-00022. Instancje `ResponseEffect` dla `IO` i `ZIO` czekają na COILIX-00020.
- COILIX-00024 dostarcza `BodyReader`/`BodyWriter` dla JSON do COILIX-00023 i opis typu, z którego korzysta COILIX-00025.
- COILIX-00025 wymaga COILIX-00023 (strumienie i WebSocket w silniku) i COILIX-00024 (opis typu).
- COILIX-00026 korzysta z cyklu życia z COILIX-00004, `CoilixModule` z COILIX-00010, `TaskExecutor` z COILIX-00005 i tożsamości repliki z COILIX-00027 (operacje w całym klastrze po kroku 2 COILIX-00027). Zmiana w rdzeniu (`ComponentCollector`, ADR 0017) jest zrobiona. Konektory Kafka, SFTP i Elasticsearch to osobne taski po nim, a `processing-web` czeka na COILIX-00023.
- COILIX-00030 korzysta z `TaskExecutor`, `TaskScheduler` i fabryk wątków z COILIX-00005, z zamykania kontekstu z COILIX-00004 i z `EventPublisher` z COILIX-00016 (zdarzenia `HealthChanged`). Reakcje na `Degraded` dokładają COILIX-00026 (wstrzymanie procesorów) i COILIX-00023 (503 i endpointy zdrowia).
- COILIX-00027 to moduł rdzenia bez zależności zewnętrznych. `ClusterCodec` trzeba uzgodnić z COILIX-00024, a `@Scheduled` z COILIX-00005 może z niego korzystać do zadań na jednej replice.
- COILIX-00028 zastępuje `checkForbiddenCalls` wartem i dodaje moduły narzędziowe poza numeracją modułów frameworka. Zmiana `Class[T]` na `ClassTag` w publicznym API (krok 3) i szkielet reguł migracyjnych (krok 8) powinny być przed wydaniem `core-v0.1.0` z COILIX-00009.
- COILIX-00029 wymaga COILIX-00020 dla mostów `IO` ↔ Pekko i silnika procesorów z COILIX-00026 (kroki 4–8) dla `processing-pekko`. Moduł `kafka-pekko` powstaje razem z taskiem konektora Kafki.

Plan wydań na podstawie tych tasków jest w [ROADMAP.md](../ROADMAP.md), a decyzje architektoniczne w [docs/adr](../docs/adr/README.md).

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

Task, który wprowadza decyzję architektoniczną (publiczne API, struktura modułów, zależności, proces wydań), dodaje ADR w `docs/adr/` w tym samym PR.
