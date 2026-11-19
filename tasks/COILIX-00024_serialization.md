# COILIX-00024: Serializacja: opis typu, circe i most do HTTP

> Szkic do doszlifowania. Kierunek uzgodniony z opiekunem 2026-11-18, szczegóły API mogą się jeszcze zmienić.

## Cel

Typy aplikacji serializuje się do JSON przez givens biblioteki JSON (na start circe), z pełnym wsparciem typów wartości, enumów, `EditOption` i `Secret`. Gdy trzeba zmienić kształt obiektu (usunąć, dodać albo przemianować pole), opisuje się to deklaratywnie i sprawdza w czasie kompilacji. Ten sam opis wykorzystuje później GraphQL (COILIX-00025).

## Stan obecny

- `@ValueType`, `@Enumerated`, `EditOption` i `Secret` są w rdzeniu (COILIX-00022), ale bez integracji z żadną biblioteką JSON.
- Biblioteka implicits opiekuna (`org.optimus:implicits`, Apache-2.0) ma:
  - `AnyValOps.derive(to, from)`: kodek circe typu wartości jako surowej wartości,
  - `EncoderOps`/`DecoderOps.derive(support)` z `EncodingSupport`/`DecodingSupport` (`Json => Json`, operacje na polach po nazwie jako stringu),
  - kodeki `EditOption`: `null` = bez zmiany, `{"value": null}` = wyczyść, `{"value": x}` = ustaw,
  - `InterfaceDecoderObject`/`InterfaceEncoderObject`: trait serializowany jako `{"<nazwa implementacji>": {...}}`,
  - `OutputOps.derive(encodingSupport)(sangriaSettings*)`, gdzie zmiany pól trzeba opisać osobno dla JSON i dla Sangrii.

## Zakres

1. Dwie drogi, ten sam wynik (givens `Encoder`/`Decoder`):
   - bez zmian kształtu: natywne `derives Codec.AsObject` z circe,
   - ze zmianami: `given` w obiekcie towarzyszącym zbudowany przez `CirceCodec.derive[A](encoding, decoding)`.
2. Opis zmian jako dane, a nie funkcja `Json => Json`:

   ```scala
   object Vehicle {
     given Codec.AsObject[Vehicle] = CirceCodec.derive[Vehicle](
       encoding = EncodingSupport[Vehicle]
         .removeField(_.internalId)
         .renameField(_.createdAt, "registeredAt")
         .addField("kind", "vehicle")
         .addField("ownerKnown", _.owner.isDefined),
       decoding = DecodingSupport[Vehicle]
         .renameField("registeredAt", _.createdAt)
         .addField(_.internalId, 0L)
     )
   }
   ```

   - pola wskazuje się selektorem `_.field`, sprawdzanym makrem; string tylko dla pól, których klasa nie ma (nowe pole, stara nazwa przy dekodowaniu),
   - `addField` przyjmuje stałą albo funkcję `A => V`; wartość przechodzi przez `Encoder[V]`,
   - `beforeEncode(A => A)` i `afterDecode(A => A)` dla zmian wartości bez zmiany kształtu.
3. Twarde sprawdzanie symetrii: gdy kodek dekoduje, a pole zostało usunięte albo przemianowane przy kodowaniu, makro wymaga wartości domyślnej, typu `Option`/`EditOption` albo uzupełnienia w `DecodingSupport`. Inaczej błąd kompilacji.
4. Givens circe dla typów z rdzenia:
   - `@ValueType`: kodek z jedynego pola (wzór `AnyValOps.derive`), bez obiektu towarzyszącego, wyzwalany adnotacją,
   - `@Enumerated`: kodek po `@EnumName`,
   - `EditOption[T]`: format z implicits; brak pola też znaczy „bez zmiany”,
   - `Secret`: tylko `Decoder` (decyzja do potwierdzenia, zob. pytania).
5. Interfejsy (sealed trait): dwie strategie, domyślnie `TypeEncoding.Wrapped`:
   - `Wrapped`: `{"Electric": {"kw": 150}}`, odpowiada `@oneOf` input object w GraphQL,
   - `Discriminator("type")`: `{"type": "Electric", "kw": 150}`, dla klientów REST i OpenAPI.

   Implementacje z `Mirror.SumOf`; nazwa domyślnie prosta nazwa klasy, nadpisywana w builderze. Trait niezapieczętowany z jawną listą implementacji.
6. Widoki: ta sama klasa może mieć dodatkowy, nazwany encoder (np. `adminView`), przekazywany jawnie do `Response.ok` (COILIX-00023).
7. Most `web-circe`: `given [A](using Decoder[A]): BodyReader[A]` i `given [A](using Encoder[A]): BodyWriter[A]` dla `application/json`; błędy circe zamieniane na 400 Problem Details ze ścieżką pola. `@implicitNotFound` na `BodyReader`/`BodyWriter` podpowiada `derives Codec.AsObject` albo import mostu.

## Proponowany projekt

| Moduł | Poziom | Zawartość |
|---|---|---|
| `common` | rdzeń | makro selektora pól, `EncodingSupport`/`DecodingSupport` jako opis, `TypeEncoding`, bez zależności zewnętrznych |
| `circe` | funkcja | interpretacja opisu na kodeki circe, givens z pkt 4, interfejsy z pkt 5 |
| `web-circe` | integracja (`web`, `circe`) | most z pkt 7 |

Opis zmian leży w `common`, bo `circe` i `sangria` to moduły funkcji i nie mogą zależeć od siebie ani od wspólnego modułu funkcji ([modularity.md](../docs/architecture/modularity.md)). Kolejne biblioteki (np. jsoniter, XML) dostają pary `coilix-<biblioteka>` i `web-<biblioteka>` według tego samego wzoru. Decyzja o givens i makrach jest w [ADR 0015](../docs/adr/0015-http-and-serialization-by-macros-and-givens.md).

## Kryteria akceptacji

1. Case class z `derives Codec.AsObject` z polami `@ValueType`, `@Enumerated`, `EditOption` koduje się i dekoduje bez dodatkowego kodu poza importem givens.
2. `removeField`, `renameField`, `addField` (stała i funkcja), `beforeEncode`, `afterDecode` działają i mają testy.
3. Selektor nieistniejącego pola i niesymetryczny kodek dają błąd kompilacji (`compileErrors`).
4. Sealed trait koduje się i dekoduje w obu strategiach z pkt 5.
5. Endpoint z COILIX-00023 przyjmuje i zwraca JSON przez `web-circe`.
6. Licencje nowych zależności przechodzą `sbt checkLicenses`.

## Poza zakresem

- GraphQL i interpretacja opisu przez Sangrię (COILIX-00025).
- Biblioteki JSON inne niż circe.
- Mapowanie kolumn bazy danych dla typów wartości (moduł `data`, COILIX-00022).

## Ryzyka i otwarte pytania

- Inline given „dla każdego typu z `@ValueType`”: trzeba potwierdzić, że kompilator po cichu odrzuca kandydata, gdy makro nie znajdzie adnotacji. Inaczej potrzebny jest węższy typ albo znacznik.
- Import mostów: jawny `import dev.coilix.web.circe.given` czy `-Yimports` jak w implicits.
- `Secret`: brak `Encoder` (błąd kompilacji przy próbie wysłania) czy `Encoder` maskujący (`"***"`).
- Zakres przeniesienia makr z implicits (`OxMacro`): które części są ogólne, a które związane z Sangrią.
- Nazwy implementacji interfejsu: tylko builder czy też adnotacja; adnotacja wymagałaby modułu adnotacji serializacji.
