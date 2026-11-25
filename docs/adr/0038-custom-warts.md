# 0038: Własne warty w module narzędziowym `tools/warts`

- Status: proponowana
- Data: 2026-11-25
- Task: COILIX-00028 (krok 2)

## Kontekst

Zakaz wypisywania na konsolę pilnowało zadanie `checkForbiddenCalls`, czyli wyrażenie regularne `\b(println|print|printf)\s*\(` po liniach źródeł. Zgłaszało tekst w stringach i w Scaladocu, a nie łapało `System.out`, `System.err` ani `Console.err`. Zasadę „`asInstanceOf` tylko tam, gdzie jest uzasadnione” sprawdzało tylko review, a w `src/main` było już 11 rzutowań zamiast 3 opisanych w szkicu taska.

Kierunek z COILIX-00028: kontrole semantyczne jako warty wartremovera na drzewie z typami, składniowe jako reguły scalafix.

## Decyzja

- **Moduł narzędziowy `tools/warts`** (projekt sbt `warts`, pakiet `dev.coilix.warts`), w Scali 3 i niepublikowany. Nie ma numeru poziomu, bo nie jest częścią frameworka, i nie podlega `checkModuleLayers`. Zależy od biblioteki `wartremover` w wersji pluginu `sbt-wartremover` i dla dokładnej wersji Scali, bo warty działają w kompilatorze.
- **Podłączenie:** każdy moduł Scali (`scalaModule`, więc też `slf4sModule`, demo i docs) dostaje klasy wartów przez `WartRemover.dependsOnLocalProjectWarts(warts)` i błędy `coilixWarts` obok `forbiddenWarts`. Warty kompilują się przed modułami, które sprawdzają. Moduł `warts` sam ich nie używa.
- **Wart `ConsoleOutput`** zastępuje `checkForbiddenCalls` i `project/ForbiddenCalls.scala`. Rozpoznaje wywołania po symbolu: `println`, `print` i `printf` z `scala.Predef`, każdy element `scala.Console` oraz `System.out` i `System.err`. Tekst w stringach i Scaladocu nie jest zgłaszany.
- **`Wart.AsInstanceOf`** jest w `forbiddenWarts`. Rzutowania, które nie były potrzebne (tablica z refleksji), zastępuje dopasowanie wzorca. Pozostałe są w małych metodach z `@SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))`, a Scaladoc metody mówi, dlaczego rzutowanie nie zawiedzie albo dlaczego jest bezpieczne mimo wymazania typu. Takie metody łatwo znaleźć po adnotacji.
- **Wart `ClassParameterInApi`** (krok 3) zgłasza w `src/main` publiczną albo chronioną metodę, konstruktor lub metodę rozszerzającą z parametrem `Class[T]` albo `Class[? <: T]` obok parametru typu `T`. Nie zgłasza `Class[?]` bez związku z parametrem typu, elementów prywatnych, elementów typów prywatnych, nadpisań i definicji lokalnych. Reguła jest w klasie `ClassParameterCheck` z predykatem pomijanych plików, bo sbt-wartremover dodaje warty jednocześnie do `Compile` i `Test`: `ClassParameterInApi` pomija pliki z `src/test`, a test wartu sprawdza wszystkie. Przegląd 199 wystąpień `Class[...]` w `src/main` znalazł jedno naruszenie, publiczny konstruktor `ConfigPath(path, componentType)`, który stał się prywatny.
- **Testy wartów** to testy munit z `WartTestTraverser` z biblioteki wartremover: przypadek zgłaszany, niezgłaszany i wyłączony adnotacją.

## Konsekwencje

- Wypisywanie na konsolę i nowe rzutowania kończą kompilację błędem także w IntelliJ przy budowaniu przez sbt, z komunikatem, czego użyć zamiast nich.
- Czas pełnej kompilacji (`clean Test/compile` z pustym cache sbt) nie zmienił się w granicach pomiaru: 17–20 s przed zmianą i 18–20 s po niej, razem z kompilacją modułu `warts`.
- Kolejne warty z COILIX-00028 (`ClassParameterInApi`, `MissingScaladoc`, `ComponentCompanion`) trafiają do tego samego modułu. Reguły scalafix dostaną osobne moduły w `tools/`.
