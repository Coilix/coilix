# Plugin IntelliJ dla `@Slf4s`

Dodaje w IDE syntetyczny member `log: org.slf4j.Logger` do klas, traitów i obiektów Scali oznaczonych `@Slf4s`, tak samo jak robi to plugin kompilatora `03_slf4s_plugin`. Dzięki temu IntelliJ nie podkreśla `log.info(...)`, podpowiada metody loggera i pokazuje typ `Logger`.

Działa przez extension point `org.intellij.scala.syntheticMemberInjector` pluginu Scala.

## Instalacja

```
sbt intellij-plugin/installIntellijPlugin
```

Task kopiuje jar pluginu do `<katalog konfiguracji IDE>/plugins/scaling-slf4s.jar`. Po instalacji trzeba zrestartować IntelliJ.

Ręczna instalacja: `sbt intellij-plugin/packageBin`, a potem w IntelliJ *Settings → Plugins → ⚙ → Install Plugin from Disk...* i wybór jara z `target/out/jvm/scala-2.13.18/intellij-plugin/`.

## Ścieżki do IDE

Moduł kompiluje się do jarów lokalnej instalacji IntelliJ i pluginu Scala. Domyślne ścieżki można nadpisać zmiennymi środowiskowymi:

| Zmienna | Znaczenie | Domyślnie |
|---|---|---|
| `IDEA_HOME` | katalog instalacji IntelliJ | `C:/Program Files/JetBrains/IntelliJ IDEA 262.4852.50` |
| `IDEA_CONFIG` | katalog konfiguracji IntelliJ (z `plugins/Scala`) | `~/AppData/Roaming/JetBrains/IntelliJIdea2026.2` |

Gdy którejś ścieżki brakuje, moduł nie jest dołączany do agregatu roota, więc reszta projektu buduje się bez IDE.

## Dlaczego Scala 2.13

Plugin Scala dla IntelliJ jest napisany w Scali 2.13.18 i ładuje jej bibliotekę standardową. Ten plugin działa w tym samym classloaderze, więc musi być skompilowany tą samą wersją i nie może dołączać własnej biblioteki Scali. To jedyny moduł projektu w Scali 2.13. Zasady z `CLAUDE.md` obowiązują w nim tak samo, poza flagami `-no-indent -old-syntax`, których 2.13 nie zna. Styl z klamrami jest w 2.13 jedynym dostępnym, więc nic się nie zmienia.

## Zgodność

Zbudowany na IntelliJ IDEA 2026.2.3 (build 262) z pluginem Scala 2026.2.669. `since-build` w `plugin.xml` to `262`.
