# 0016: Moduł testkit w rdzeniu

- Status: proponowana
- Data: 2026-11-19
- Task: COILIX-00013

## Kontekst

Testy aplikacji, które chcą sprawdzić współpracę beanów, wołały `Coilix.run` z argumentami albo składały kontekst ręcznie z `StartupComponents`. Nie było sposobu, żeby dać testowi właściwości bez plików w `src/test/resources`, włączyć profil tylko dla testu ani zastąpić bean atrapą bez nowej klasy z `@Primary`. Kontekst trzeba było zamykać ręcznie, a każdy test odpalał baner, shutdown hook i runnery.

Te same potrzeby mają kolejne moduły frameworka: testy klastra (COILIX-00027) budują kilka kontekstów naraz z różnymi właściwościami, a testy procesorów (COILIX-00026) podmieniają źródła i wyjścia.

## Decyzja

- Nowy publikowany moduł `testkit` (`coilix-testkit`, pakiet `dev.coilix.testkit`) w katalogu `10_testkit`, zależny od `boot`. Należy do grupy wydań `core`, bo jego API zmienia się razem z `boot` i powinno wychodzić w tej samej wersji. Demo przechodzi do `11_demo`, bo jego testy korzystają z testkitu, a numer `11` zwolnił się po ADR 0014.
- munit jest zależnością `Provided`. Aplikacja wybiera wersję munit sama.
- Rdzeń testkitu nie zależy od munit: niemutowalny `CoilixTestSetup` (pakiety, właściwości, profile, beany, argumenty, runnery) i `TestContextFactory` z `DefaultTestContextFactory`. Z munit korzystają tylko traity `CoilixFixtures` i `CoilixSuite` (bo `FunFixture` w munit jest klasą wewnętrzną suite'u) oraz `SuiteContextFixture`. Integracja z inną biblioteką testów może powstać później bez zmian w rdzeniu testkitu.
- `TestContextFactory` nie ma `given ConfigPath`, tak jak `CoilixApplicationRunner`: działa przed wczytaniem konfiguracji. Komponenty startu (np. `CoilixBootstrap`) są brane z wczytanej konfiguracji, więc ich podmiana działa też w testach.
- Podmiana beanów jest w `boot`, a nie w testkicie: nowa przeciążona metoda `CoilixBootstrap.start(..., replacements: BeanReplacements)`. `DefaultCoilixBootstrap` usuwa definicje ze skanowania i z modułów, których klasa jest przypisywalna do podmienianego typu, a dodatkowe definicje zostawia. Podmiana, która nie pasuje do żadnej definicji, kończy się `UnmatchedBeanReplacementException`. Dotychczasowa metoda `start` z trzema parametrami zostaje jako `final` i deleguje z `BeanReplacements.none`.
- Dwa poziomy API. Zwięzły: `setup.use { ... }` i trait `CoilixSuite` (`coilixTest(name) { ... }` z setupem z `given CoilixTestSetup`, `shared.test(name) { ... }` dla kontekstu suite'u) uruchamiają ciało jako funkcję kontekstową z kontekstem jako `given`, a `CoilixContextOps` daje helpery `bean[T]`, `beanOption[T]`, `beans[T]`, `property[T]`, `arguments` i `context`, które biorą kontekst z `using`. Jawny: `TestContextFactory.create` i fixture z `CoilixFixtures`, które przekazują kontekst jako parametr. Styl zwięzły wzoruje się na testkicie bulldozera (ciało scenariusza jako `TestCtx ?=> ...`, helpery na `using`), bez jego silnika planu na `IO`, bo kontener Coilix jest synchroniczny.
- Testkit rozróżnia `replaceBean[T]` (musi coś zastąpić, chroni przed pomyłką w typie) i `addBean[T]` (dodaje bean, którego aplikacja nie ma, np. dla zależności `Option[T]`).
- Właściwości i profile z testu wygrywają z każdym innym źródłem configu. Kontekst testowy nie ma banera, logu startu ani shutdown hooka, a runnery działają tylko po `withRunners`.

## Konsekwencje

- Implementacje `CoilixBootstrap` spoza frameworka muszą obsłużyć nową metodę. Rdzeń nie ma jeszcze wydania, więc nie jest to zmiana niezgodna wobec użytkowników.
- Grupa `core` ma siedem modułów. Testkit wychodzi z każdym wydaniem rdzenia, także gdy się nie zmienił.
- Przewodniki mogą pokazywać testy: `12_docs/examples/next/test` jest uruchamiane przez `sbt docs/testFull`.
- Testy wycinków (tylko warstwa web albo tylko data) przyjdą razem z tymi modułami i będą rozszerzać `CoilixTestSetup`, a nie tworzyć osobnego mechanizmu.
