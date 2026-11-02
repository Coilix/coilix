# COILIX-00018: Szablon nowego projektu

## Cel

Nowy użytkownik zaczyna od `sbt new Coilix/coilix.g8` i dostaje działającą aplikację z jednym serwisem, configiem, testem i pluginem `@Slf4s`, bez kopiowania ustawień z README.

## Stan obecny

- Jedyny przykład to `10_demo`, który jest częścią buildu frameworka i korzysta z ustawień `build.sbt` (`slf4sModule`), niedostępnych dla użytkownika.
- README pokazuje kod, ale nie pokazuje `build.sbt` aplikacji ani podpięcia pluginu kompilatora.

## Zakres

1. Repozytorium `Coilix/coilix.g8` z szablonem giter8:
   - `build.sbt` z `coilix-boot`, pluginem `coilix-slf4s-plugin` (`addCompilerPlugin`) i `slf4j-simple`,
   - `@CoilixApplication`, jeden `@Service`, `ApplicationRunner` i `application.yaml`,
   - test munit (po COILIX-00013 na testkicie),
   - `.scalafmt.conf` i `.gitignore`.
2. Parametry: nazwa, pakiet, wersja Coilix (domyślnie najnowsza z Maven Central).
3. CI szablonu: wygenerowanie projektu i `sbt test` na każdym wydaniu Coilix.
4. Sekcja „Getting started” w README i na stronie dokumentacji (COILIX-00007).

## Proponowany projekt

- Osobne repozytorium, bo `sbt new` wymaga repozytorium z sufiksem `.g8`.
- Wersja Coilix w szablonie aktualizowana przez Scala Steward.

## Kryteria akceptacji

1. `sbt new Coilix/coilix.g8` tworzy projekt, który przechodzi `sbt run` i `sbt test`.
2. Logger `@Slf4s` działa w wygenerowanym projekcie bez dodatkowej konfiguracji.

## Poza zakresem

- Szablony z modułami `web` i `data`. Przyjdą razem z nimi.

## Ryzyka i otwarte pytania

- Wymaga pierwszego wydania na Maven Central (COILIX-00009), więc realizacja dopiero po nim.
- Jak użytkownik ma podpinać plugin kompilatora w sbt 2: `addCompilerPlugin` czy `autoCompilerPlugins`? Trzeba sprawdzić na wydanym artefakcie.
