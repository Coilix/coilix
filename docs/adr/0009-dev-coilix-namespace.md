# 0009: Przestrzeń nazw `dev.coilix`

- Status: proponowana
- Data: 2026-11-03
- Task: COILIX-00019

## Kontekst

Maven Central wymaga `groupId` w przestrzeni nazw, którą projekt kontroluje. Do tej pory był to `io.github.coilix`, weryfikowany przez repozytorium organizacji GitHub Coilix, bo projekt nie miał domeny.

2026-11-02 projekt kupił domenę `coilix.dev`. Działa na niej poczta projektu (`hello@`, `security@`), a organizacja na GitHubie jest nią zweryfikowana. Konwencja JVM zakłada pakiety z odwróconej domeny właściciela.

Nic nie zostało jeszcze wydane, więc zmiana nie dotyka żadnego użytkownika. Po pierwszym wydaniu byłaby zmianą niezgodną we wszystkich importach i kluczach `class` w configach.

## Decyzja

`groupId` to `dev.coilix`, a pakiety to `dev.coilix.*` (`dev.coilix.context`, `dev.coilix.boot` i tak dalej). Nazwy artefaktów (`coilix-boot`), klucze configu (`coilix.*`) i identyfikatory tasków się nie zmieniają.

Central Portal weryfikuje przestrzeń nazw rekordem DNS TXT w domenie `coilix.dev`.

## Konsekwencje

- Pakiet, strona, poczta i `groupId` mają jedną nazwę, niezależną od GitHuba. Przeniesienie repozytorium nie zmienia `groupId`.
- Domenę trzeba odnawiać. Jej utrata oznaczałaby utratę kontroli nad przestrzenią nazw, więc konto rejestratora ma 2FA i automatyczne odnowienie.
- Plugin IntelliJ dostaje identyfikator `dev.coilix.slf4s`. Nie był publikowany w Marketplace, więc zmiana nikogo nie dotyka.
