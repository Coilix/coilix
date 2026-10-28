# SCALING-00001: Wsparcie `@Slf4s` w IntelliJ

## Cel

W klasach i obiektach oznaczonych `@Slf4s` IntelliJ ma traktować `log` jak prawdziwe pole typu `org.slf4j.Logger`:

- brak czerwonego podkreślenia `log` i `log.info(...)`,
- podpowiadanie metod loggera po `log.`,
- typ `org.slf4j.Logger` w podglądzie typu i nawigacja do `Logger`.

To wyłącznie wsparcie IDE. Kompilacją zajmuje się już `03_slf4s_plugin`.

## Stan obecny

- Plugin kompilatora `03_slf4s_plugin` dodaje przed typerem pole `private final val log = org.slf4j.LoggerFactory.getLogger("<pełna.nazwa>")`. Kod się kompiluje i działa.
- IntelliJ nie uruchamia pluginów kompilatora podczas analizy kodu, więc `log` świeci się na czerwono, np. w `10_demo/src/main/scala/DemoRunner.scala`, `10_demo/src/main/scala/ContextInspector.scala`, `09_boot/src/main/scala/DefaultScalingBootstrap.scala` i `05_context/src/main/scala/ReflectiveBeanInstantiator.scala`.
- Szczegółowa specyfikacja jest w [docs/AGENT_INTELLIJ_SLF4S_PLUGIN_SPEC.md](../docs/AGENT_INTELLIJ_SLF4S_PLUGIN_SPEC.md). Część jej treści jest nieaktualna:
  - ścieżka `src/main/scala/demo/DemoBeans.scala` już nie istnieje,
  - build-time rewrite w sbt został zastąpiony pluginem kompilatora,
  - adnotacja ma teraz pełną nazwę `org.scaling.annotation.Slf4s`.

## Zakres

- Syntetyczny member `log: org.slf4j.Logger` dla `class` i `object` z `@Slf4s`.
- Taka sama semantyka jak w pluginie kompilatora, żeby IDE i kompilator się nie rozjeżdżały:
  - adnotacja rozpoznawana po prostej nazwie `Slf4s` (także pełna nazwa i import),
  - brak syntetycznego `log`, gdy definicja ma już własny member `log`,
  - działa dla klas zagnieżdżonych w obiektach i pakietach.
- Odświeżanie bez restartu IDE po dodaniu lub usunięciu adnotacji.

## Proponowany projekt

Mechanizm: extension point pluginu Scala `SyntheticMembersInjector` (`org.intellij.scala.syntheticMemberInjector`). Injector dostaje `ScTypeDefinition` i zwraca tekst źródłowy membera do wstrzyknięcia, np. `private val log: _root_.org.slf4j.Logger = ???`. Plugin Scala sam go parsuje i włącza w resolve, typowanie i completion. Dokładne sygnatury metod (`injectFunctions`, `injectMembers` itd.) trzeba sprawdzić dla aktualnej wersji pluginu Scala.

Dwie drogi dostarczenia, do wyboru na początku taska:

1. **Osobny plugin IntelliJ (zalecane na start).** Instalowany ręcznie z dysku, działa niezależnie od tego, jak IDE widzi moduły projektu. Build przez `sbt-idea-plugin` od JetBrains, żeby zostać przy sbt. Trzeba sprawdzić, czy `sbt-idea-plugin` działa z sbt 2. Jeśli nie, plugin dostaje osobny build w podkatalogu z własnym `project/`.
2. **Library extension pluginu Scala.** Jar biblioteki zawiera `META-INF/intellij-compat.json` i klasy injectora, a IntelliJ sam proponuje włączenie rozszerzenia. Wygodne dla użytkowników frameworka, ale działa tylko dla zależności w postaci jara. W obrębie tego repo moduły są widziane jako moduły, nie jary, więc to rozwiązanie na później, przy publikacji artefaktów.

Komponenty (zgodnie z zasadami projektu, każdy jako trait i implementacja):

- `Slf4sAnnotationDetector` dla PSI, czyli odpowiednik `NameBasedSlf4sAnnotationDetector` z pluginu kompilatora,
- `LoggerMemberInjector`, czyli implementacja `SyntheticMembersInjector`,
- `ExistingLoggerGuard`, który sprawdza, czy definicja ma już własny `log`.

## Kryteria akceptacji

1. W `DemoRunner`, `ContextInspector`, `DefaultScalingBootstrap` i `ReflectiveBeanInstantiator` `log.info(...)` nie jest podkreślane.
2. Podgląd typu `log` pokazuje `org.slf4j.Logger`, a `log.` podpowiada `info`, `debug`, `warn`, `error`.
3. W klasie bez `@Slf4s` odwołanie do `log` dalej jest błędem.
4. Klasa z `@Slf4s` i własnym `val log` nie dostaje drugiego `log`.
5. Brak `slf4j-api` na classpath modułu nie powoduje wyjątków w IDE.
6. Testy pluginu (fixture'y IntelliJ) przechodzą.
7. Instrukcja instalacji pluginu jest w README pluginu, a specyfikacja w `docs/` jest zaktualizowana.

## Poza zakresem

- Zmiany w pluginie kompilatora, poza ewentualnym wydzieleniem wspólnej reguły rozpoznawania adnotacji.
- Samo wyciszanie inspekcji bez prawdziwego resolve.

## Ryzyka i otwarte pytania

- API pluginu Scala zmienia się między wersjami, więc trzeba ustalić docelowe wersje IntelliJ i pluginu Scala.
- Czy kod pluginu IDE podlega wartremoverowi i zasadom z `CLAUDE.md`? Proponowane: tak, z wyjątkami tylko tam, gdzie wymusza je API IntelliJ.
