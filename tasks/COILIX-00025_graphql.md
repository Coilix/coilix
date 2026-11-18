# COILIX-00025: GraphQL na Sangrii (`sangria`, `web-graphql`)

> Szkic do doszlifowania. Kierunek uzgodniony z opiekunem 2026-11-18, zakres trzeba doprecyzować po COILIX-00023 i COILIX-00024.

## Cel

Aplikacja wystawia API GraphQL nad modułem HTTP. Typy GraphQL powstają z tego samego deklaratywnego opisu typu co kodeki JSON (COILIX-00024), więc pole usunięte z JSON-a nie pojawia się w schemacie, a interfejsy mają ten sam kształt w obu formatach.

## Stan obecny

- Biblioteka implicits ma makra Sangrii: `OutputOps`, `InputOps`, `ValueObject`, `InterfaceOutputObject` (typy `ObjectType`, `InputObjectType`, `InterfaceType` z case class i traitów), skalary dla typów wartości i kodeki `EditOption` jako input object.
- Projekt bulldozer2 (`graphql`) ma działającą warstwę GraphQL na Pekko: executory query, mutation i subscription (`Source`), relay, operacje bulk, klienta, obsługę błędów i budowę schematu z dostawców pól.
- W implicits zmiany pól opisuje się osobno dla JSON (`EncodingSupport`) i dla Sangrii (`DeriveObjectSetting`).

## Zakres (wstępny)

1. Moduł funkcji `sangria`: interpretacja opisu z `common` (COILIX-00024) na typy Sangrii; skalary dla `@ValueType`, enumy dla `@Enumerated`, input object dla `EditOption`.
2. Interfejsy: `TypeEncoding.Wrapped` jako `@oneOf` input object po stronie wejścia i `InterfaceType`/union po stronie wyjścia.
3. Moduł integracyjny `web-graphql` (`web`, `sangria`, `circe`): endpoint `/graphql` (query i mutation przez POST), subskrypcje przez WebSocket (graphql-ws) albo SSE.
4. Schemat składany z beanów dostarczających pola (wzór `GraphQLOperationFieldsProvider` z bulldozer2), kontekst GraphQL budowany z kontekstu żądania i uwierzytelnienia (`security-web`).
5. Mapowanie błędów na odpowiedź GraphQL, w tym naruszeń `Validator` z kodami z ADR 0013.

## Kryteria akceptacji

Do ustalenia po prototypie. Minimum:

1. Typ z `EncodingSupport` ma w schemacie GraphQL te same pola co w JSON.
2. Query, mutation i subskrypcja działają w demo.

## Poza zakresem

- Relay i operacje bulk z bulldozer2 (osobne taski, jeśli będą potrzebne).
- Klient GraphQL.

## Ryzyka i otwarte pytania

- Licencja i aktywność Sangrii oraz jej zgodność ze Scalą 3.9 LTS.
- Strumienie subskrypcji: Pekko `Source`, `fs2.Stream` czy `ZStream`; zależy od COILIX-00020 i kontraktu strumieni w COILIX-00023.
- Które makra z implicits i jaki kod z bulldozer2 przenieść, a co napisać od nowa. Oba projekty są opiekuna, więc przeniesienie jest dozwolone, ale kod musi spełniać zasady z `CLAUDE.md`.
