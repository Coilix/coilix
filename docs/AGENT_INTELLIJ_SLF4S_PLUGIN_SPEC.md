# Specyfikacja dla agenta: plugin IntelliJ z pelnym wsparciem `@Slf4s` (Scala)

> Zrealizowane w module `11_intellij_plugin` (task `tasks/COILIX-00001_intellij_slf4s_support.md`). Ścieżki i opis build-time rewrite poniżej są historyczne: kompilacją zajmuje się plugin kompilatora `03_slf4s_plugin`, a adnotacja to `io.github.coilix.annotation.Slf4s`.

## 1. Cel dokumentu
Ten dokument ma sluzyc jako kompletna instrukcja dla osobnego agenta, ktory zaprojektuje plugin IntelliJ (bez implementacji w tym zadaniu), zapewniajacy pelne wsparcie adnotacji `@Slf4s` w kodzie Scala.

Priorytet: w klasach oznaczonych `@Slf4s` symbol `log` ma byc traktowany przez IDE jak prawdziwy member typu `org.slf4j.Logger`.

## 2. Kontekst projektu
- Repo: `coilix`
- Przyklad problemu: `src/main/scala/demo/DemoBeans.scala`
- W kodzie jest:
  - adnotacja `@Slf4s`
  - odwolania `log.info(...)`
- Build-time rewrite (SBT) umie to skompilowac, ale IntelliJ nadal analizuje zrodlo i zglasza `Not found: log`.

Wniosek: potrzebny jest plugin IDE, ktory doda syntetyczny member na etapie PSI/resolve w IntelliJ.

## 3. Definicja "pelnego wsparcia"
Pelne wsparcie oznacza jednoczesnie:
1. Brak czerwonego bledu (`Unresolved reference`) dla `log` w klasie z `@Slf4s`.
2. `log` ma resolve do typu `org.slf4j.Logger`.
3. Dziala completion metod loggera (`info`, `debug`, `warn`, `error`, itd.).
4. Dziala type inference i szybkie akcje IDE (nawigacja, typ w tooltipie, ewentualnie find usages po API loggera z normalnym zachowaniem IntelliJ/Scala plugin).
5. Brak ingerencji w runtime aplikacji (to tylko wsparcie IDE).

## 4. Zakres funkcjonalny (MVP + rozszerzenia)

### 4.1 MVP (obowiazkowe)
- Dla `ScClass` z adnotacja `@Slf4s` dodaj syntetyczny member:
  - nazwa: `log`
  - typ: `org.slf4j.Logger`
  - semantyka: read-only field-like member
- Wspieraj co najmniej klasy (`class`).
- Jezeli klasa juz definiuje `log`, plugin nie moze dodawac drugiego.

### 4.2 Rozszerzenia (po MVP)
- Wsparcie `object` i `trait` z `@Slf4s`.
- Rozpoznawanie adnotacji w pelnej nazwie (`@io.github.coilix.annotation.Slf4s`) i po imporcie (`@Slf4s`).
- Konfigurowalna nazwa pola (domyslnie `log`) - opcjonalnie.

## 5. Oczekiwane zachowanie semantyczne
- `log` istnieje tylko logicznie w IDE (syntetycznie), bez modyfikacji plikow zrodlowych.
- Typ `org.slf4j.Logger` musi byc rozwiazywany przez PSI/type system Scala plugin.
- Jezeli biblioteka `slf4j-api` nie jest na classpath modułu, plugin powinien:
  - nie crashowac,
  - ewentualnie pokazywac controlled fallback/soft warning,
  - nie powodowac lawiny false positives.

## 6. Architektura pluginu (projekt)

### 6.1 Moduly
1. `core` - logika wykrywania `@Slf4s`.
2. `scala-integration` - integracja z PSI/resolve Scala plugin.
3. `tests` - testy funkcjonalne i regression.

### 6.2 Kluczowe komponenty
1. **AnnotationDetector**
   - Wejscie: element PSI klasy.
   - Wyjscie: czy element ma `@Slf4s`.
   - Musi obslugiwac rozne formy zapisu adnotacji.

2. **SyntheticLoggerMemberProvider**
   - Odpowiedzialny za dostarczenie syntetycznego membera `log`.
   - Zwraca member tylko gdy:
     - wykryto `@Slf4s`,
     - brak realnego membera `log` w klasie.

3. **LoggerTypeResolver**
   - Rozwiazuje `org.slf4j.Logger` do typu IDE.
   - Z fallbackiem bez wywalenia przy brakujacym `slf4j-api`.

4. **ConflictGuard**
   - Zapobiega konfliktom nazw i duplikatom.

## 7. Integracja z IntelliJ/Scala Plugin
Uwaga: API Scala plugin bywa niestabilne miedzy wersjami. Agent ma zweryfikowac konkretne klasy i extension pointy dla docelowej wersji IntelliJ + Scala plugin.

### 7.1 Docelowe punkty rozszerzen
Agent ma znalezc i wykorzystac mechanizm "synthetic members" dla Scala PSI/resolve (wlasciwy dla aktualnej wersji Scala plugin), zamiast hackow typu suppress-only.

Wymaganie projektowe:
- rozwiazanie ma byc semantyczne (resolve/type/completion),
- nie tylko ukrywanie inspection error.

### 7.2 Kompatybilnosc
- Zdefiniowac target matrix:
  - IntelliJ IDEA (minimum jedna LTS i jedna biezaca stable),
  - Scala plugin kompatybilny z projektem.
- Wymagac testow na co najmniej 2 kombinacjach wersji.

## 8. Algorytm dodawania `log`
1. Dla kazdej klasy PSI w module:
   - sprawdz adnotacje `@Slf4s`.
2. Jezeli brak `@Slf4s` -> nic nie rob.
3. Jezeli jest `@Slf4s`:
   - sprawdz, czy klasa ma realny member `log`.
   - jesli ma -> nie dodawaj syntetycznego.
   - jesli nie ma -> expose synthetic member `log: org.slf4j.Logger`.
4. Upewnij sie, ze member bierze udzial w:
   - resolve symbolu,
   - typowaniu,
   - completion.

## 9. UX i diagnostyka
- W IDE nie ma byc czerwonych bledow dla `log` w klasach z `@Slf4s`.
- W klasach bez `@Slf4s` odwolanie do `log` ma dalej dawac blad.
- Plugin nie moze psuc czasu odpowiedzi edytora:
  - cache wynikow detekcji,
  - invalidacja cache przy zmianie PSI/importow/adnotacji.

## 10. Test plan (obowiazkowy)

### 10.1 Testy pozytywne
1. `class` z `@Slf4s` i `log.info` -> brak bledu, completion pokazuje metody Logger.
2. `@io.github.coilix.annotation.Slf4s` (pelna nazwa) -> dziala tak samo.
3. `@Slf4s` + import aliasowany -> dziala (jezeli alias wspierany w MVP, w przeciwnym razie jawnie odlozyc do backlogu).

### 10.2 Testy negatywne
1. Klasa bez `@Slf4s` i `log.info` -> blad unresolved musi zostac.
2. Klasa z `@Slf4s`, ale z wlasnym `val log = ...` -> brak duplikatu syntetycznego.
3. Brak `slf4j-api` na classpath -> plugin stabilny, bez crash.

### 10.3 Testy regresyjne
1. Edycja pliku (dodanie/usuniecie `@Slf4s`) odswieza resolve bez restartu IDE.
2. Refactor/import optimize nie psuje wykrywania.
3. Wiecej niz jedna klasa z `@Slf4s` w pliku dziala poprawnie.

## 11. Kryteria akceptacji (Definition of Done)
Funkcja jest gotowa tylko jezeli wszystkie ponizsze punkty sa spelnione:
1. W `src/main/scala/demo/DemoBeans.scala` odwolania `log.info(...)` nie swieca sie na czerwono w IntelliJ.
2. IntelliJ pokazuje typ `log` jako `org.slf4j.Logger`.
3. Autocomplete dla `log.` podpowiada metody loggera.
4. Brak false positives w klasach bez `@Slf4s`.
5. Testy pluginu przechodza lokalnie i w CI.
6. Dokument kompatybilnosci wersji jest dolaczony.

## 12. Ryzyka i decyzje

### 12.1 Ryzyka
- API Scala plugin moze sie zmieniac miedzy wersjami.
- Integracja z resolverem moze byc wrazliwa na edge-case'y (nested classes, companions, alias imports).

### 12.2 Mitigacje
- Izolacja warstwy integracji (adapter),
- testy na kilku wersjach,
- fallback defensywny przy braku typu loggera.

## 13. Artefakty, ktore agent ma dostarczyc
1. Dokument projektowy techniczny (short ADR) z wyborem extension pointow.
2. Szkielet pluginu IntelliJ z modulami.
3. Implementacja synthetic member provider dla `@Slf4s`.
4. Testy funkcjonalne + regression.
5. Notatka kompatybilnosci wersji IntelliJ/Scala plugin.

## 14. Out of scope (w tym zadaniu)
- Zmiana kompilatora Scala.
- Runtime code generation.
- Modyfikacja kodu zrodlowego usera.
- Tylko suppress inspection bez resolve/type/completion.

## 15. Szybka checklista dla agenta wykonawczego
- [ ] Zidentyfikowano poprawny extension point Scala plugin dla synthetic members.
- [ ] `@Slf4s` jest poprawnie wykrywane w PSI.
- [ ] `log` jest syntetycznie dodawany jako `org.slf4j.Logger`.
- [ ] Resolve + type + completion dzialaja.
- [ ] Brak duplikatu gdy istnieje realny `log`.
- [ ] Testy pozytywne/negatywne/regresyjne przechodza.
- [ ] Potwierdzono kompatybilnosc na uzgodnionych wersjach IntelliJ/Scala plugin.

