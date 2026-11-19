# COILIX-00023: Moduł HTTP (`web`)

> Szkic do doszlifowania. Kierunek uzgodniony z opiekunem 2026-11-18, szczegóły API mogą się jeszcze zmienić.

## Cel

Aplikacja wystawia endpointy HTTP przez trait z adnotacjami, który jest jednocześnie kontraktem serwera i klienta. Błędy, które da się wykryć w czasie kompilacji (brak deserializatora ciała, brak dekodera parametru, niezgodność ścieżki z parametrami), kończą kompilację, a nie start albo obsługę żądania.

## Stan obecny

- `21_web` jest pusty, a moduł jest opcjonalny i podłącza się przez `CoilixModule` (COILIX-00010).
- Rdzeń działa przez refleksję w runtime (ADR 0002). Makra są wyjątkiem dla sprawdzeń, które bez nich przeszłyby do runtime (ADR 0013).
- Są typy wartości (`@ValueType`), enumy (`@Enumerated`), `Validator`, `EditOption` i `Secret` (COILIX-00022).
- Kontener jest synchroniczny. Efekty `F[_]` są w analizie (COILIX-00020).

## Zakres

1. Adnotacje w nowym module Java `web-annotations`: `@HttpApi(path)`, `@Get`, `@Post`, `@Put`, `@Patch`, `@Delete`, `@Path`, `@Query`, `@Header`, `@Body`.
2. Kontrakt jako trait:

   ```scala
   @HttpApi("/vehicles")
   trait VehicleApi {
     @Get("/{vin}")
     def find(@Path vin: Vin): IO[Response[Vehicle]]

     @Post("")
     def create(@Body command: CreateVehicle): IO[Response[Vehicle]]

     @Delete("/{vin}")
     def delete(@Path vin: Vin): IO[Response[Nothing]]
   }

   object VehicleApi {
     given HttpEndpoints[VehicleApi] = HttpEndpoints.derived
   }
   ```

   Kontrolerem jest bean implementujący trait. Kontekst znajduje go przy starcie i pobiera `given HttpEndpoints` z obiektu towarzyszącego traitu. Brak `given` to błąd startu z podpowiedzią.
3. `HttpEndpoints.derived` (makro) sprawdza przy kompilacji:
   - dla każdego `@Body T` istnieje `given BodyReader[T]`,
   - dla każdego `@Path`, `@Query`, `@Header T` istnieje `given ParamDecoder[T]`; dla `@ValueType` i `@Enumerated` makro tworzy go samo,
   - dla typu zwracanego `F[Response[A]]` istnieje `given ResponseEffect[F]`,
   - zmienne ścieżki (`{vin}`) odpowiadają parametrom `@Path`, a trasy w traicie się nie dublują.

   Wynikiem jest dispatcher bez refleksji przy obsłudze żądania.
4. `Response[+A]`: sealed trait ze statusem i nagłówkami, warianty bez ciała (`Response[Nothing]`) i z ciałem. Konstruktory z ciałem (`Response.ok`, `Response.created`) wymagają `using BodyWriter[A]` i zapamiętują writer w odpowiedzi. Endpoint zawsze zwraca `Response`, a nie gołą wartość ani `Option`.
5. `BodyReader[A]` i `BodyWriter[A]`: lista obsługiwanych media types, odczyt i zapis. Writery łączy się (`json.orElse(xml)`), a wybór robi negocjacja `Accept` (406, gdy nic nie pasuje). Moduł `web` nie zależy od żadnej biblioteki JSON; mosty są w modułach integracyjnych (COILIX-00024).
6. `ResponseEffect[F]`: instancje dla `Response[A]` (styl blokujący) i `Future[Response[A]]` w `web`. `IO` i `ZIO` w modułach integracyjnych z COILIX-00020.
7. Błędy: 400 dla błędów deserializacji i naruszeń `Validator`, w formacie Problem Details (RFC 9457), z kodami naruszeń z ADR 0013. Typ błędu nie jest w sygnaturze (`Response[A]`, a nie `Response[E, A]`).
8. Klient z tego samego traitu: `HttpClient.of[VehicleApi](baseUri)`, przez `java.lang.reflect.Proxy` i `java.net.http.HttpClient`, z tymi samymi `BodyReader`/`BodyWriter`.
9. Silnik za SPI `HttpServerEngine`, domyślnie Jetty 12 w module integracyjnym `web-jetty`. Kontrakt SPI od początku obejmuje ciało strumieniowe i WebSocket, bo potrzebuje ich GraphQL (COILIX-00025); implementacja strumieni może przyjść w osobnym kroku.
10. Wykonanie blokujące, wątek na żądanie: wątki wirtualne na JDK 21+, wykryte w runtime, zwykła pula na JDK 17. Spięcie z `TaskExecutor`, gdy powstanie COILIX-00005.
11. `HttpFilter` jako beany, uporządkowane; przez nie wchodzi `security-web`. Kontekst żądania (np. zalogowany użytkownik) dostępny dla handlerów.
12. Cykl życia: serwer startuje po zbudowaniu kontekstu i zamyka się z kontekstem z limitem czasu. Klucze `coilix.web.host`, `coilix.web.port`, `coilix.web.shutdown-timeout`.

## Proponowany projekt

| Moduł | Poziom | Zawartość |
|---|---|---|
| `web-annotations` | adnotacje funkcji (Java) | adnotacje z pkt 1 |
| `web` | funkcja | `Response`, `HttpEndpoints`, `BodyReader`/`BodyWriter`, `ParamDecoder`, `ResponseEffect`, routing, filtry, SPI silnika, klient |
| `web-jetty` | integracja | silnik Jetty 12 |

Numery katalogów: kolejne wolne od `20` (ADR 0018). Decyzja o makrach jest w [ADR 0015](../docs/adr/0015-http-and-serialization-by-macros-and-givens.md).

## Kryteria akceptacji

1. Demo wystawia endpointy z kontraktu-traitu i woła je klientem z tego samego traitu.
2. Brak `BodyReader` dla `@Body`, brak `ParamDecoder` dla parametru, niezgodna zmienna ścieżki i podwójna trasa dają błąd kompilacji (testy przez `compileErrors`).
3. Bean kontrolera bez `given HttpEndpoints` w obiekcie towarzyszącym traitu daje błąd startu z podpowiedzią.
4. Negocjacja `Accept`: dwa writery, wybór właściwego, 406 bez dopasowania.
5. Błąd deserializacji i naruszenia walidacji dają 400 w formacie Problem Details.
6. Serwer zamyka się z kontekstem i czeka na trwające żądania do limitu czasu.
7. `web` jest wyłączalny kluczem `coilix.modules.web.enabled` i nie łamie `checkModuleLayers`.

## Poza zakresem

- Serializacja JSON i mosty do bibliotek (COILIX-00024).
- GraphQL (COILIX-00025).
- Instancje `ResponseEffect` dla `IO` i `ZIO` (COILIX-00020).
- Zasięgi request i session (COILIX-00015).
- OpenAPI (osobny task po ustabilizowaniu API).

## Ryzyka i otwarte pytania

- Makro czytające adnotacje Javy z traitu: trzeba potwierdzić w prototypie, że quotes widzą adnotacje parametrów metod traitu.
- Kontrakt tagless final (`VehicleApi[F[_]]`): makro widzi konkretny typ przy `HttpEndpoints.derived[VehicleApi[IO]]`, ale kontekst musi dopasować bean `VehicleApi[IO]` mimo wymazywania typów.
- Jawny `given` w obiekcie towarzyszącym czy `derives HttpEndpoints`: do decyzji przy prototypie.
- Mapowanie błędów domenowych na status (np. 409) bez typu błędu w sygnaturze.
- Silnik: Jetty 12 jest propozycją, alternatywą jest Netty.
