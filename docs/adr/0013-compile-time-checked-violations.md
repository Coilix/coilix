# 0013: Naruszenia walidacji sprawdzane w czasie kompilacji

- Status: proponowana
- Data: 2026-11-16
- Task: COILIX-00022
- Zastępuje: punkt ADR 0012 o jawnym kodzie naruszeń

## Kontekst

ADR 0012 przyjął, że naruszenia walidacji mają jawny kod, bo rdzeń nie miał mieć makr. W implicits `Violation("…")` i `tvb[T]("… %s")(…)` są makrami: kod naruszenia powstaje z nazwy `val`, a szablon jest sprawdzany przy kompilacji (literał, tylko `%s`, liczba placeholderów równa liczbie argumentów). Opiekun chce zachować to zachowanie.

ADR 0002 wyklucza makra i generowany kod, które użytkownik musiałby pisać albo konfigurować. Makro `inline` w bibliotece, które użytkownik tylko wywołuje, tego nie narusza.

## Decyzja

- `Violation(message)` i `ViolationTemplate[T](template)(arguments*)` to makra `inline` w `01_common`. Kod naruszenia to nazwa `val`, do którego są przypisane, rozbita na małe słowa łączone kropką (`listTooLong` daje `list.too.long`).
- Kompilacja kończy się błędem, gdy komunikat albo szablon nie jest literałem, komunikat jest pusty albo zawiera `%`, szablon ma inny specyfikator niż `%s` albo inną liczbę placeholderów niż argumentów, albo wynik nie jest przypisany bezpośrednio do `val` lub `lazy val`.
- Jawny kod nadal jest dostępny: `Violation(code, message)` i konstruktor `ViolationTemplate`, który sprawdza szablon w runtime.
- Makra zostają wyjątkiem dla sprawdzeń, które bez nich przeszłyby do runtime. Wykrywanie komponentów i typów nadal działa przez refleksję (ADR 0002, ADR 0012).

## Konsekwencje

- Błąd w szablonie komunikatu widać w IDE i przy kompilacji, a nie dopiero przy pierwszym naruszeniu.
- Zmiana nazwy `val` zmienia kod naruszenia. Kod, na którym polegają klienci API albo tłumaczenia, trzeba wtedy podać jawnie.
- Kod makr nie jest wykonywany w runtime, więc pokrycie testami mierzy tylko kod przez nie wygenerowany. Błędy kompilacji sprawdzają testy przez `compileErrors`.
