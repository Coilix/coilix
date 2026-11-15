# 0012: Typy wartości i enumy przez adnotacje

- Status: proponowana
- Data: 2026-11-15
- Task: COILIX-00022

## Kontekst

Coilix przejmuje z biblioteki opiekuna `org.optimus:implicits` typy opakowujące jedną wartość (`AnyValObject`), enumy (`EnumProduct`, `EnumSettings`), walidację i `EditOption`. W implicits typ deklaruje się przez companion trait, a kodeki powstają makrami i zależą od circe, sangrii, enumeratum i pureconfig.

Reszta Coilixa opiera się na adnotacjach Javy czytanych refleksyjnie (ADR 0002), a rdzeń nie ma zależności od tych bibliotek.

## Decyzja

- Typ wartości oznacza się adnotacją `@ValueType`, enum (natywny `enum` Scali 3) adnotacją `@Enumerated`, a własną nazwę przypadku adnotacją `@EnumName`. Nie ma companion traitów ani nazw z przyrostkiem `Object`.
- Framework rozpoznaje te typy refleksyjnie przez wymienne traity `ValueTypeResolver` i `EnumResolver` w `01_common` (ADR 0003).
- Części ogólne (typy wartości, enumy, walidacja, `EditOption`) są w rdzeniu, bo potrzebuje ich konfiguracja i przyszłe moduły `web` i `data`. Mapowanie na kolumny bazy jest w `06_data`.
- Rdzeń nie dostaje makr po stronie użytkownika, derywacji `derives` ani zależności od cats, circe i sangrii. Integracje z tymi bibliotekami będą osobnymi modułami.

## Konsekwencje

- Użytkownik pisze zwykłą case class albo `enum` z jedną adnotacją, bez dodatkowego kodu.
- Błędy (zła klasa, zła wartość, nieznana nazwa) wychodzą przy starcie, a nie przy kompilacji, tak jak inne błędy powiązań.
- `extends AnyVal` może ukrywać typ w sygnaturach JVM. Dokumentacja musi to opisać, a jeśli się potwierdzi, zalecać typ wartości bez `AnyVal`.
- Naruszenia walidacji mają jawny kod zamiast nazwy wyprowadzanej makrem.
- Kodeki JSON i typy GraphQL dla tych typów pojawią się dopiero z modułami integracyjnymi.
