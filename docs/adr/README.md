# Decyzje architektoniczne (ADR)

Krótkie zapisy decyzji, które trudno odwrócić: kontekst, decyzja i konsekwencje. Format według [Michaela Nygarda](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions).

Decyzje 0002–0007 zapadły przed wprowadzeniem ADR i zostały zapisane wstecz 2026-10-30 na podstawie kodu, tasków i dokumentów w `docs/`.

| Nr | Decyzja | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Zapisywanie decyzji jako ADR | przyjęta |
| [0002](0002-runtime-reflection-and-java-annotations.md) | Refleksja w runtime i adnotacje w Javie | przyjęta |
| [0003](0003-replaceable-framework-components.md) | Komponenty frameworka podmieniane w configu | przyjęta |
| [0004](0004-immutable-context-and-constructor-injection.md) | Niemutowalny kontekst i wstrzykiwanie przez konstruktor | przyjęta |
| [0005](0005-slf4s-compiler-plugin.md) | Logger `@Slf4s` przez plugin kompilatora | przyjęta, punkt o pluginie IntelliJ zastąpiony przez 0014 |
| [0006](0006-scala-lts-and-jdk-baseline.md) | Scala z linii LTS i JDK 17 dla publikowanych modułów | przyjęta |
| [0007](0007-apache-license-and-dco.md) | Apache-2.0 i DCO zamiast CLA | przyjęta |
| [0008](0008-squash-merge.md) | Scalanie przez squash, jeden PR to jedna zmiana | przyjęta |
| [0009](0009-dev-coilix-namespace.md) | Przestrzeń nazw `dev.coilix` | przyjęta |
| [0010](0010-docusaurus-documentation-site.md) | Strona dokumentacji w Docusaurusie pod `docs.coilix.dev` | przyjęta, punkty 4, 5 i 7 zastąpione przez 0011 |
| [0011](0011-release-groups-and-changelog-files.md) | Grupy wydań, changelog w plikach i publikacja przy tagu | przyjęta, grupa `intellij` zastąpiona przez 0014 |
| [0012](0012-value-types-and-enums-by-annotations.md) | Typy wartości i enumy przez adnotacje | przyjęta, punkt o kodzie naruszeń zastąpiony przez 0013 |
| [0013](0013-compile-time-checked-violations.md) | Naruszenia walidacji sprawdzane w czasie kompilacji | proponowana |
| [0014](0014-intellij-plugin-in-separate-repository.md) | Plugin IntelliJ w osobnym repozytorium | przyjęta |
| [0015](0015-http-and-serialization-by-macros-and-givens.md) | HTTP i serializacja przez makra i givens | proponowana, szkic |
| [0016](0016-testkit-module.md) | Moduł testkit w rdzeniu | proponowana |
| [0017](0017-components-collected-by-modules.md) | Komponenty zbierane przez moduły | proponowana |
| [0018](0018-module-directory-number-ranges.md) | Zakresy numerów katalogów modułów | proponowana |
| [0019](0019-effect-model-and-parameterized-bean-types.md) | Model efektów i beany rozróżniane po argumentach typów | proponowana |
| [0020](0020-concurrency-module.md) | Moduł `concurrency` z wykonawcą na wątkach wirtualnych | proponowana, częściowo zastąpiona przez 0028 |
| [0021](0021-cluster-replica-identity.md) | Moduł `cluster` i tożsamość repliki | proponowana |
| [0022](0022-definition-processors-and-async-proxies.md) | Procesory definicji z modułów i proxy dla `@Async` | proponowana |
| [0023](0023-ci-caching.md) | Cache i pomijanie kroków w CI bez pomijania testów | proponowana |
| [0024](0024-closing-phases-and-retry-policy.md) | Fazy zamykania kontekstu i wspólna polityka ponowień | proponowana |
| [0025](0025-in-memory-application-events.md) | Lekkie zdarzenia aplikacji tylko w pamięci | proponowana |
| [0026](0026-framework-events-and-testkit.md) | Zdarzenia frameworka i zdarzenia w testkicie | proponowana |
| [0027](0027-fatal-errors-shut-down.md) | Błąd fatalny wyłącza aplikację | proponowana |
| [0028](0028-one-compute-pool.md) | Jedna pula obliczeniowa i osobny wykonawca blokujący | proponowana |
| [0029](0029-context-propagation.md) | Przekazywanie kontekstu wątku do zadań | proponowana |
| [0030](0030-prototype-and-lazy-beans.md) | Beany prototype i leniwe w niemutowalnym rejestrze | proponowana |
| [0031](0031-cats-effect-runtime.md) | Runtime cats-effect jako jedyna pula obliczeniowa | proponowana |
| [0032](0032-effectful-beans.md) | Beany z IO i Resource przez interpreter planu z modułu | proponowana |
| [0033](0033-effectful-lifecycle-and-runners.md) | Cykl życia i runnery w IO | proponowana |
| [0034](0034-coilix-io-app.md) | CoilixIOApp z runtime'em IOApp jako beanem | proponowana |
| [0035](0035-testkit-cats-effect.md) | Moduł testkit-cats-effect | proponowana |
| [0036](0036-pekko-module.md) | Moduł pekko z ActorSystem na pulach Coilix | proponowana |

## Nowa decyzja

1. Plik `NNNN-tytul-w-kebab-case.md` z kolejnym numerem, według szablonu niżej.
2. Status `proponowana` w PR, `przyjęta` po scaleniu.
3. Decyzji się nie edytuje. Zmiana kierunku to nowy ADR ze statusem `zastępuje NNNN`, a stary dostaje status `zastąpiona przez NNNN`.

```markdown
# NNNN: Tytuł

- Status: proponowana | przyjęta | zastąpiona przez NNNN
- Data: RRRR-MM-DD
- Task: COILIX-NNNNN

## Kontekst
## Decyzja
## Konsekwencje
```
