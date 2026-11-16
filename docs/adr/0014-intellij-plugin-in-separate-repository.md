# 0014: Plugin IntelliJ w osobnym repozytorium

- Status: przyjęta
- Data: 2026-11-16
- Task: COILIX-00001
- Zastępuje: punkt ADR 0005 o module `11_intellij_plugin` i grupę wydań `intellij` z ADR 0011

## Kontekst

Plugin IntelliJ dla `@Slf4s` był modułem sbt `11_intellij_plugin`. Kompilował się do jarów lokalnej instalacji IDE, musiał używać tej samej wersji Scali co plugin Scala dla IntelliJ (do 2026.2 była to 2.13, od 2026.3 jest 3.9.0) i nie miał weryfikacji zgodności, podpisu ani publikacji w JetBrains Marketplace. Oficjalne narzędzia JetBrains do tego (IntelliJ Platform Gradle Plugin: `verifyPlugin`, `signPlugin`, `publishPlugin`) działają w Gradle.

Równolegle powstał plugin w Kotlinie z buildem Gradle, który ma to samo zachowanie oraz aliasy importów, pomijanie komentarzy i typ zapasowy bez `slf4j-api`. Plugin ma też obsługiwać kolejne funkcje frameworka, nie tylko `@Slf4s`.

## Decyzja

- Plugin IntelliJ dla frameworka żyje w osobnym repozytorium [`Coilix/coilix-intellij`](https://github.com/Coilix/coilix-intellij) z buildem Gradle. To repo zostaje przy sbt i Scali.
- Plugin ma id `dev.coilix.intellij` i nazwę „Coilix Framework”, bo id w Marketplace jest niezmienne, a plugin obejmie kolejne funkcje frameworka. `@Slf4s` jest pierwszą z nich.
- Moduł `11_intellij_plugin`, `project/IntellijPlugin.scala` i grupa wydań `intellij` znikają z tego repo. Plugin ma własne wersje, changelog i wydania w swoim repo.
- Coilix AI to osobny produkt z własnym repo i nie jest częścią tej decyzji.

## Konsekwencje

- W buildzie frameworka nie ma Gradle ani zależności od lokalnej instalacji IDE, a numer `11` jest wolny.
- Plugin wydaje się w rytmie wersji IDE, niezależnie od wydań rdzenia.
- Zmiana semantyki `@Slf4s` w `03_slf4s_plugin` (np. rozpoznawania adnotacji) wymaga odpowiedniej zmiany w `coilix-intellij`, bo kod nie jest współdzielony.
