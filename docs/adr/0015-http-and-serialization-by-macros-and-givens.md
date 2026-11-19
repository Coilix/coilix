# 0015: HTTP i serializacja przez makra i givens

- Status: proponowana (szkic do doszlifowania)
- Data: 2026-11-18
- Task: COILIX-00023, COILIX-00024, COILIX-00025
- Zawęża: ADR 0002 dla modułów HTTP i serializacji

## Kontekst

Rdzeń wykrywa komponenty i wiąże je refleksją w runtime (ADR 0002), więc błędy powiązań wychodzą przy starcie. ADR 0013 dopuścił makra `inline` dla sprawdzeń, które bez nich przeszłyby do runtime.

Endpoint HTTP wymaga deserializatora dla ciała żądania, dekoderów parametrów i serializatora odpowiedzi. Przy wywołaniu metody kontrolera refleksją kompilator nie ma gdzie wymusić tych instancji, więc brak deserializatora wychodziłby dopiero przy starcie albo przy pierwszym żądaniu. Biblioteki JSON w Scali 3 (circe, jsoniter-scala) i Sangria opierają się na givens i derywacji w czasie kompilacji.

Opiekun chce, żeby endpoint zawsze zwracał `Response`, żeby odpowiedź z ciałem wymagała serializatora w kontekście givens, żeby `@Body` bez deserializatora nie kompilowało się, żeby obsługiwać `F[Response[A]]` i żeby zmiany kształtu obiektu przy serializacji były możliwe i sprawdzane.

## Decyzja

1. Moduły HTTP (`web` i integracje) oraz serializacji (`circe`, `sangria` i ich mosty) wiążą typy przez givens i makra w czasie kompilacji, a nie refleksją.
2. Adnotacje Javy (`web-annotations`) zostają nośnikiem opisu kontraktu HTTP. Czyta je makro `HttpEndpoints.derived`, które użytkownik wywołuje w obiekcie towarzyszącym traitu kontraktu.
3. Kontener nadal znajduje beany refleksją. Dla kontrolera pobiera z obiektu towarzyszącego jego traitu gotowy `given HttpEndpoints`. Jego brak to błąd startu.
4. `Response` z ciałem zapamiętuje `BodyWriter` w miejscu utworzenia, więc obsługa odpowiedzi nie szuka serializatora w runtime.
5. Moduł `web` nie zależy od bibliotek JSON ani efektów. Biblioteki podłącza się przez mosty givens w modułach integracyjnych, a efekty przez instancje `ResponseEffect[F]`.
6. Zmiany kształtu obiektu przy serializacji są deklaratywnym opisem w `common` (selektory pól sprawdzane makrem), interpretowanym przez moduły bibliotek. Jeden opis obsługuje JSON i GraphQL.
7. Wykrywanie komponentów, konfiguracja i typy wartości w rdzeniu nadal działają przez refleksję (ADR 0002, ADR 0012).

## Konsekwencje

- Brak serializatora, deserializatora albo dekodera parametru to błąd kompilacji z podpowiedzią z `@implicitNotFound`.
- Użytkownik pisze jedną linię `given` w obiekcie towarzyszącym kontraktu, czego ADR 0002 dotąd unikał.
- Obsługa żądania nie używa refleksji, co pomaga wydajności i przyszłemu GraalVM native-image.
- Ten sam trait daje serwer i klienta, a kontrakt tagless final (`Api[F[_]]`) jest możliwy, bo makro widzi konkretny typ.
- Kod makr nie jest mierzony przez pokrycie; błędy kompilacji sprawdzają testy przez `compileErrors` (jak w ADR 0013).
- Każda biblioteka JSON wymaga pary modułów (`coilix-<biblioteka>`, `web-<biblioteka>`).
- Decyzja jest szkicem: nazwy, sposób podpięcia `given` (jawny czy `derives`) i podział modułów doprecyzuje prototyp.
