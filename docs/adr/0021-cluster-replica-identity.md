# 0021: Moduł `cluster` i tożsamość repliki

- Status: proponowana
- Data: 2026-11-19
- Task: COILIX-00027 (krok 1)

## Kontekst

Procesory tła (COILIX-00026) dzielą pracę między repliki aplikacji, więc każda replika musi znać swój numer i liczbę replik. Pełny klaster (członkostwo, koordynator, API między replikami) to kolejne kroki COILIX-00027, a pierwsza wersja procesorów potrzebuje tylko tożsamości. Z klastra mają korzystać moduły funkcji, które nie mogą zależeć od siebie nawzajem.

## Decyzja

- Nowy moduł rdzenia `cluster` (`07_cluster`, pakiet `dev.coilix.cluster`, artefakt `coilix-cluster`) zależy od `concurrency`, bo heartbeat z kolejnego kroku korzysta z `TaskScheduler`. `boot` od niego nie zależy: aplikacja dostaje go z modułem funkcji, który dzieli pracę, albo dodaje sama. Moduł podłącza się przez `ClusterModule` (`CoilixModule`).
- `ReplicaIdentity` (`self: Member`, `replicaCount`) jest beanem `replicaIdentity`, a nie komponentem z `ConfigPath`. Tryb wybiera `coilix.cluster.identity` (enum `IdentityMode`: `single`, `static`, `stateful-set`), a własną tożsamość aplikacja daje beanem `@Primary`.
- `single` (domyślny) to replika 0 z 1. `static` bierze numer i liczbę z `coilix.cluster.replica-index` i `coilix.cluster.replicas` albo ze zmiennych `COILIX_REPLICA_INDEX` i `COILIX_REPLICAS`. `stateful-set` bierze numer z `coilix.cluster.replica-index`, ze zmiennej `COILIX_POD_INDEX` (etykieta `apps.kubernetes.io/pod-index` przez downward API) albo z końcówki nazwy poda, a liczbę jak w `static`.
- Brak numeru, liczba mniejsza niż 1 albo numer poza zakresem to `ReplicaIdentityException` przy starcie, z nazwą klucza albo zmiennej.
- Ustawienia są czytane kluczami przez `Environment.get`, a nie wiązaniem całej sekcji, bo `Option[Int]` w case class traci typ argumentu w sygnaturze JVM.

## Konsekwencje

- Rdzeń ma kolejny publikowany artefakt w grupie `core`, a `docs` zależy od niego dla przykładów.
- `Member.address` jest na razie zawsze `None`. Adresy, członkostwo i transport dojdą w kroku 2 COILIX-00027.
- Liczba replik przy `stateful-set` musi być w configu albo zmiennej, bo downward API jej nie udostępnia. Autoskalowanie bez stałej liczby da dopiero `cluster-kubernetes`.
