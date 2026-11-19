# COILIX-00027: Klaster replik (`cluster`)

> Szkic do doszlifowania, wydzielony z COILIX-00026 na prośbę opiekuna z 2026-11-19: replika wie o pozostałych replikach i potrafi się z nimi komunikować przez API.

## Cel

Każda replika aplikacji (pod w Kubernetesie) wie:

- ile jest replik i którą z nich jest,
- które repliki żyją i pod jakim adresem są dostępne,
- która replika jest koordynatorem.

Repliki komunikują się przez typowane API: zapytanie i odpowiedź do jednej repliki, rozgłoszenie do wszystkich, powiadomienie bez odpowiedzi. Na tym budują moduły frameworka: procesory tła (COILIX-00026) do sterowania w całym klastrze, zmiany liczby shardów i sprawdzania zgodności configu, a `@Scheduled` (COILIX-00005) do zadań na jednej replice. Aplikacja może dodawać własne endpointy.

## Stan obecny

- Aplikacja nie zna liczby replik, swojego numeru ani adresów pozostałych replik.
- Bulldozer opiera podział pracy wyłącznie na grupach konsumentów Kafki. Pody nie rozmawiają ze sobą bezpośrednio.
- Moduł `web` (COILIX-00023) i serializacja (COILIX-00024) są w szkicu. API klastra nie może od nich zależeć w rdzeniu, ale transport HTTP może z nich korzystać w module integracyjnym.

## Zakres

### 1. Tożsamość repliki

```scala
final case class Member(index: Int, id: String, address: Option[MemberAddress], startedAt: Instant, version: String)

trait ReplicaIdentity {
  def self: Member

  def replicaCount: Int
}
```

`ReplicaIdentity` (komponent z `given ConfigPath`, klucz `coilix.cluster.identity`) ma implementacje:

- `single` (domyślna): replika 0 z 1, czyli lokalne uruchomienie i testy,
- `static`: `coilix.cluster.replica-index` i `coilix.cluster.replicas` z configu albo zmiennych środowiskowych,
- `stateful-set`: numer z etykiety `apps.kubernetes.io/pod-index` przekazanej przez downward API (Kubernetes 1.28+) albo z końcówki nazwy poda (`app-3` → 3). Liczbę replik podaje się w `coilix.cluster.replicas` z tej samej wartości co `replicas` StatefulSetu (np. w Helmie), bo downward API jej nie udostępnia.

Start bez poprawnej tożsamości to błąd startu z podpowiedzią. Przykłady: indeks poza `0 until replicas`, `stateful-set` bez etykiety i bez numeru w nazwie poda.

### 2. Członkostwo i wykrywanie

```scala
trait Cluster {
  def self: Member

  def members: List[MemberState]

  def coordinator: Option[Member]

  def subscribe(listener: MembershipListener): Subscription

  def client: ClusterClient
}
```

- `MemberState` to `Member` ze stanem `Joining`, `Up`, `Suspect`, `Leaving` albo `Down` i czasem ostatniego kontaktu.
- `MemberDiscovery` (SPI) mówi, gdzie szukać replik:
  - `static`: lista adresów w configu,
  - `stateful-set`: adresy z headless service, `<pod>-<i>.<service>.<namespace>.svc.cluster.local` dla `i < replicas`,
  - `kubernetes-api` (moduł integracyjny, później): EndpointSlices usługi, bez stałej liczby replik, także dla Deploymentu,
  - `in-memory`: kilka kontekstów w jednym JVM, do testów.
- Wykrywanie awarii: heartbeat przez transport co `heartbeat-interval`. Replika bez odpowiedzi przez `suspect-after` przechodzi w `Suspect`, a po `down-after` w `Down`. Zamykana replika ogłasza `Leaving`, więc pozostałe nie czekają na limit czasu.
- `MembershipListener` dostaje zmiany jako zdarzenia: `MemberJoined`, `MemberUp`, `MemberSuspected`, `MemberLeft`, `MemberDown`, `CoordinatorChanged`.

### 3. Koordynator

- Przy członkostwie statycznym koordynatorem jest żywa replika o najniższym numerze.
- Przy dynamicznym (później) koordynatora wybiera dzierżawa `coordination.k8s.io/v1 Lease` w module `cluster-kubernetes`.
- Każda zmiana koordynatora podnosi **epokę klastra**. Polecenia koordynatora niosą epokę, a replika odrzuca polecenia ze starszą epoką, więc stary koordynator po podziale sieci nie nadpisze decyzji nowego.
- `ClusterCoordination` daje prosty protokół dwufazowy, z którego korzysta np. `reshard` procesorów: przygotowanie na wszystkich żywych replikach, potem zatwierdzenie albo wycofanie. Brak odpowiedzi którejkolwiek repliki w fazie przygotowania wycofuje operację.

### 4. API między replikami

```scala
trait ClusterEndpoint[Req, Res] {
  def name: String
}

trait ClusterHandler[Req, Res] {
  def endpoint: ClusterEndpoint[Req, Res]

  def handle(request: Req, from: Member): Res
}

trait ClusterClient {
  def ask[Req: ClusterCodec, Res: ClusterCodec](
    member: Member,
    endpoint: ClusterEndpoint[Req, Res],
    request: Req,
    timeout: FiniteDuration
  ): Either[ClusterError, Res]

  def broadcast[Req: ClusterCodec, Res: ClusterCodec](
    endpoint: ClusterEndpoint[Req, Res],
    request: Req,
    timeout: FiniteDuration
  ): Map[Member, Either[ClusterError, Res]]

  def tell[Req: ClusterCodec](member: Member, endpoint: ClusterEndpoint[Req, Unit], request: Req): Either[ClusterError, Unit]

  def askCoordinator[Req: ClusterCodec, Res: ClusterCodec](
    endpoint: ClusterEndpoint[Req, Res],
    request: Req,
    timeout: FiniteDuration
  ): Either[ClusterError, Res]
}
```

- Endpoint to obiekt z nazwą i typami. Handler to bean `ClusterHandler`, zebrany przy starcie. Dwa handlery tego samego endpointu to błąd startu.
- `ClusterError` (sealed): `Unreachable`, `Timeout`, `Rejected` (np. stara epoka), `NoHandler`, `HandlerFailed` z opisem błędu, `Unauthorized`. Błędy są wartościami, a nie wyjątkami, bo w klastrze częściowa porażka jest normalna.
- `broadcast` zwraca wynik każdej repliki osobno, łącznie z samą sobą (wywołanie lokalne bez transportu).
- `ClusterCodec[T]` to typeclass serializacji do bajtów. W rdzeniu są instancje dla typów prostych i `derives` dla case class. Most do kodeków z COILIX-00024 jest w module integracyjnym.
- Wywołania z jednego endpointu do jednej repliki są wykonywane po kolei, a różne endpointy równolegle, każdy na wątkach wirtualnych.

### 5. Transport

`ClusterTransport` (SPI):

- `in-memory` w rdzeniu do testów: kilka kontekstów w jednym JVM rozmawia bez sieci,
- `http` w module integracyjnym `cluster-http`: serwer na osobnym porcie `coilix.cluster.port` (nie na porcie publicznego API), klient `java.net.http.HttpClient`. Działa bez modułu `web`, żeby aplikacja bez HTTP mogła mieć klaster.

Bezpieczeństwo:

- każde wywołanie jest podpisane wspólnym sekretem (`coilix.cluster.secret` jako `Secret` z COILIX-00022, HMAC z epoką i znacznikiem czasu przeciw powtórzeniom),
- opcjonalnie mTLS,
- przewodnik zaleca NetworkPolicy, która wpuszcza na port klastra tylko pody tej samej aplikacji.

### 6. Zgodność configu między replikami

- Moduły publikują odcisk ustawień, które muszą być zgodne na wszystkich replikach. Na przykład `processing` publikuje `shards`, `slots` i `assignment` każdego procesora.
- Repliki wymieniają odciski przy dołączeniu i w heartbeacie. Rozjazd daje zdarzenie `ConfigMismatch` z modułem, kluczem i wartościami na każdej replice. Moduł sam decyduje, co z tym zrobić: `processing` przechodzi w `Degraded` i wstrzymuje przejmowanie shardów.
- Odcisk zawiera też wersję aplikacji, więc w trakcie rolling update widać, które repliki mają nową wersję.

### 7. Konfiguracja

```hocon
coilix.cluster {
  identity = static
  replicas = 1
  replicas = ${?COILIX_REPLICAS}
  replica-index = 0
  replica-index = ${?COILIX_REPLICA_INDEX}
  replica-id = ${?HOSTNAME}
  discovery = static
  members = []
  service = ${?COILIX_CLUSTER_SERVICE}
  port = 7700
  transport = none
  secret = ${?COILIX_CLUSTER_SECRET}
  heartbeat-interval = 2s
  suspect-after = 6s
  down-after = 20s
  request-timeout = 5s
}
```

`transport = none` (domyślnie) oznacza, że replika zna swoją tożsamość, ale nie rozmawia z innymi. `Cluster.members` zawiera wtedy tylko ją samą, a moduły zgłaszają operacje klastrowe jako lokalne.

## Proponowany projekt

| Moduł | Rodzaj | Zależności | Zawartość |
|---|---|---|---|
| `cluster` | rdzeń | `context` | `Member`, `ReplicaIdentity`, `Cluster`, `MemberDiscovery` (`static`, `stateful-set`, `in-memory`), koordynator i epoka, `ClusterClient`, `ClusterEndpoint`, `ClusterHandler`, `ClusterCodec`, transport `in-memory`, odciski configu |
| `cluster-http` | funkcja | `cluster` | transport HTTP na osobnym porcie, podpisy HMAC, opcjonalny mTLS |
| `cluster-kubernetes` | funkcja (później) | `cluster`, klient Kubernetesa | wykrywanie przez EndpointSlices, koordynator na `Lease`, liczba replik z API |

`cluster` jest w rdzeniu, bo korzystają z niego `processing` (moduł funkcji) i `@Scheduled` (COILIX-00005), a moduły funkcji nie mogą zależeć od siebie. Nie ma ciężkich zależności: transport sieciowy jest w osobnych modułach. Katalog `06_cluster`, pierwszy wolny numer bibliotek rdzenia (ADR 0018).

Przykład własnego endpointu aplikacji:

```scala
object CacheEndpoints {
  val Invalidate: ClusterEndpoint[InvalidateCache, Unit] = ClusterEndpoint("cache.invalidate")
}

@Component
final class InvalidateCacheHandler(cache: TokenCache) extends ClusterHandler[InvalidateCache, Unit] {
  override def endpoint: ClusterEndpoint[InvalidateCache, Unit] = CacheEndpoints.Invalidate

  override def handle(request: InvalidateCache, from: Member): Unit =
    cache.invalidate(request.userId)
}

@Component
final class UserLogout(cluster: Cluster) {
  def onLogout(userId: UserId): Map[Member, Either[ClusterError, Unit]] =
    cluster.client.broadcast(CacheEndpoints.Invalidate, InvalidateCache(userId), 2.seconds)
}
```

### Kroki (osobne PR-y)

1. Tożsamość repliki (`single`, `static`, `stateful-set`) i statyczne członkostwo bez transportu. Tego potrzebuje pierwsza wersja COILIX-00026.
2. `ClusterClient`, endpointy, handlery, kodeki, transport `in-memory`, heartbeat, stany członków, koordynator i epoka.
3. Protokół dwufazowy i odciski configu.
4. `cluster-http` z podpisami.
5. Przewodnik w obu językach z manifestem StatefulSetu, headless service i NetworkPolicy.
6. `cluster-kubernetes`.

## Kryteria akceptacji

1. `stateful-set` czyta numer repliki z etykiety pod-index i z nazwy poda `app-2`. Brak obu albo numer poza zakresem to błąd startu z podpowiedzią.
2. Trzy konteksty z transportem `in-memory` widzą się nawzajem jako `Up`, a koordynatorem jest replika 0.
3. Po zamknięciu repliki 0 pozostałe widzą `Leaving`, a koordynatorem zostaje replika 1 z wyższą epoką. Polecenie ze starą epoką jest odrzucane jako `Rejected`.
4. Replika, która przestała odpowiadać, przechodzi w `Suspect`, a potem w `Down` według limitów z configu. Test na kontrolowanym zegarze.
5. `broadcast` zwraca wynik każdej repliki, a niedostępna replika daje `Unreachable` albo `Timeout` bez wpływu na pozostałe.
6. Dwa handlery tego samego endpointu to błąd startu, a wywołanie endpointu bez handlera daje `NoHandler`.
7. Protokół dwufazowy wycofuje operację, gdy jedna replika nie odpowie w fazie przygotowania.
8. Różne odciski configu na dwóch replikach dają zdarzenie `ConfigMismatch` z wartościami z obu replik.
9. `cluster-http` odrzuca wywołanie bez poprawnego podpisu jako `Unauthorized` i wywołanie z przeterminowanym znacznikiem czasu.
10. `cluster` nie ma zależności zewnętrznych, co sprawdza `checkModuleLayers`.

## Poza zakresem

- Konsensus (Raft, Paxos) i replikowany stan. Koordynator z epoką wystarcza do przydziału pracy.
- Klaster między centrami danych i między różnymi aplikacjami.
- Gwarancja dostarczenia wiadomości (kolejki, ponowienia po stronie klastra). Wywołujący sam decyduje o ponowieniu po `ClusterError`.
- Rozproszone blokady dla kodu aplikacji. Mogą powstać na koordynatorze w osobnym tasku.

## Ryzyka i otwarte pytania

1. Wykrywanie awarii przez heartbeat może przy podziale sieci uznać za martwą replikę, która nadal pracuje. Epoka chroni decyzje koordynatora, a moduły z trwałym stanem (procesory stanowe) muszą mieć własną ochronę przy zapisie (epoka w `StateStore`, COILIX-00026).
2. Członkostwo statyczne wymaga stałej liczby replik w configu, więc HorizontalPodAutoscaler na StatefulSecie wymaga też zmiany tej wartości. Pełne autoskalowanie daje dopiero `cluster-kubernetes`.
3. Osobny port i sekret to dodatkowa konfiguracja wdrożenia. Trzeba ją dobrze opisać w przewodniku i dać czytelne błędy przy starcie (brak sekretu przy `transport = http` to błąd startu).
4. `ClusterCodec` w rdzeniu dubluje część COILIX-00024. Trzeba to uzgodnić z projektem serializacji, żeby nie było dwóch mechanizmów `derives`.
