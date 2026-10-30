# COILIX-00002: Uruchamianie aplikacji w stylu Spring Boot

## Cel

Start aplikacji ma przypominać Spring Boot: adnotacja na klasie startowej, jedna linia w `main`, pakiet bazowy wyznaczany z położenia klasy startowej, logika startowa w beanach-runnerach zamiast w nadpisanej metodzie.

Docelowo:

```scala
@CoilixApplication
object DemoApplication {
  def main(args: Array[String]): Unit = {
    val _ = Coilix.run[DemoApplication.type](args)
  }
}
```

## Stan obecny

- `09_boot` ma trait aplikacji, który aplikacja rozszerza. Trzeba w nim ręcznie nadpisać `basePackage` (string) i `run(context)`.
- Logika demo (`DemoRunner`, `ContextInspector`) jest wywoływana ręcznie z `Main.run`.
- Kontekstu nie da się zamknąć, nie ma shutdown hooka ani logu z czasem startu.

## Zakres

1. Adnotacja `@CoilixApplication` w `02_annotations` z opcjonalnym `scanBasePackages`.
2. `Coilix.run[T: ClassTag](args)` w `09_boot`:
   - pakiet bazowy domyślnie to pakiet klasy `T`, jak w Springu,
   - zwraca działający kontekst.
3. `ApplicationArguments`, czyli bean z argumentami wywołania. Na początek parsowanie `--klucz=wartość` i argumentów pozycyjnych.
4. Runnery jako beany, odpowiedniki `CommandLineRunner` i `ApplicationRunner`:
   - po zbudowaniu kontekstu framework uruchamia wszystkie beany implementujące `ApplicationRunner`,
   - kolejność przez adnotację `@Order` (domyślnie najniższy priorytet).
5. Logi startu:
   - banner (z `banner.txt` z zasobów, jeśli istnieje),
   - `Starting DemoApplication`,
   - `Started DemoApplication in 0.123 seconds`.
6. Zamykalny kontekst:
   - `ConfigurableApplicationContext` (albo `ClosableApplicationContext`) z `close()`,
   - shutdown hook JVM, który zamyka kontekst.
7. Migracja `10_demo` na nowy sposób uruchamiania. `DemoRunner` i `ContextInspector` stają się runnerami.
8. Usunięcie starego traitu aplikacji po migracji.

## Proponowany projekt

- `02_annotations`: `CoilixApplication.java`, `Order.java`.
- `05_context`:
  - `ConfigurableApplicationContext` (trait) rozszerzający `ApplicationContext` o `close()`,
  - `DefaultApplicationContext` implementuje nowy trait.
- `09_boot`:
  - `Coilix`: obiekt z `run[T: ClassTag](args)` delegujący do `CoilixApplicationRunner`,
  - `CoilixApplicationRunner` (trait) i `DefaultCoilixApplicationRunner`, które składają kroki startu,
  - `BasePackageResolver` (trait) i `AnnotationBasePackageResolver`, które czytają `scanBasePackages` albo pakiet klasy startowej,
  - `ApplicationArguments`, `ArgumentsParser` (trait) i `SimpleArgumentsParser`,
  - `ApplicationRunner` (trait dla runnerów użytkownika) i `RunnerInvoker`, który sortuje po `@Order` i wywołuje,
  - `StartupLogger` (trait) i `Slf4jStartupLogger` z bannerem i czasem startu,
  - `ShutdownHookRegistrar` (trait) i `RuntimeShutdownHookRegistrar`.
- Argumenty wywołania rejestrowane jako bean przez `extraDefinitions` albo przez gotowy `RegisteredBean`, podobnie jak dziś sam kontekst.

## Kryteria akceptacji

1. `10_demo` startuje przez `@CoilixApplication` i `Coilix.run[...]`, bez nadpisywania `basePackage`.
2. Runnery demo wykonują się automatycznie po starcie, w kolejności `@Order`.
3. Log zawiera banner, `Starting ...` i `Started ... in X seconds`.
4. `ApplicationArguments` da się wstrzyknąć do beana, a `--name=Ann` jest dostępne jako opcja.
5. Zakończenie JVM (Ctrl+C) zamyka kontekst, co widać w logu.
6. Testy pokrywają: wyznaczanie pakietu bazowego, `scanBasePackages`, kolejność runnerów, parsowanie argumentów i zamykanie kontekstu.

## Poza zakresem

- Profile i pliki `application.conf` / `application.properties` (osobny task).
- Eventy aplikacji (`ApplicationStartedEvent` itd.).
- Auto-konfiguracja modułów `data`, `web` i `security`.

## Ryzyka i otwarte pytania

- Czy Java-adnotacja na `object` Scali ląduje na klasie `DemoApplication$`, tak że `ClassTag[DemoApplication.type]` ją widzi? Trzeba to sprawdzić testem na początku taska. Jeśli nie, klasą startową będzie `class` z obiektem towarzyszącym.
- Czy `Coilix.run` ma zwracać kontekst, czy `Unit` jak `main`? Proponowane: zwraca kontekst, bo to ułatwia testy.
