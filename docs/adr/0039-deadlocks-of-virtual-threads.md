# 0039: Wykrywanie zakleszczeń przy wątkach wirtualnych

- Status: proponowana
- Data: 2026-11-25
- Task: COILIX-00030 (krok 2)

## Kontekst

Monitor wątków z COILIX-00030 miał wykrywać zakleszczenia przez `ThreadMXBean.findDeadlockedThreads`. Coilix uruchamia jednak pracę blokującą na wątkach wirtualnych (`BlockingExecutor`, `@Async(blocking = true)`, odbiorcy zdarzeń `blocking`, silnik procesorów), a task zostawił otwarte pytanie, co z ich zakleszczeń widać przez API JVM. Rozstrzyga to eksperyment.

### Eksperyment

Program w jednym pliku Java tworzył zakleszczenie dwóch wątków, każdy w osobnej JVM, w sześciu wariantach: wątki platformowe, wirtualne i mieszane (jeden platformowy, jeden wirtualny), na monitorach (`synchronized`) i na `ReentrantLock`. Po 500 ms od zakleszczenia sprawdzał `ThreadMXBean` (`getAllThreadIds`, `findDeadlockedThreads`, `findMonitorDeadlockedThreads`, `getThreadInfo`), `Thread.getAllStackTraces`, `Thread.getState` i `getStackTrace` wątków, stan wątków nośnych `ForkJoinPool` oraz zrzut `HotSpotDiagnosticMXBean.dumpThreads` w formacie JSON i tekstowym (to samo daje `jcmd <pid> Thread.dump_to_file`). Wątki wirtualne powstawały przez refleksję, żeby ten sam plik działał na JDK 17. Uruchomienia: Temurin 17.0.17, 21.0.7, 25.0.3 i 26.0.2.

### Wyniki

| Wariant | `findDeadlockedThreads` | `getThreadInfo` | Zrzut `dumpThreads` JSON |
|---|---|---|---|
| platformowe, monitory (17, 21, 25, 26) | oba wątki | `BLOCKED`, blokada i właściciel | 21: nazwy i stosy; 25 i 26: `state`, `blockedOn`, `monitorsOwned` |
| platformowe, `ReentrantLock` (17, 21, 25, 26) | oba wątki | `WAITING`, blokada i właściciel | 21: nazwy i stosy; 25: `parkBlocker` bez właściciela; 26: `parkBlocker` z `owner` |
| wirtualne, monitory (21, 25, 26) | brak | `null` | 21: nazwy i stosy; 25 i 26: `state`, `blockedOn`, `monitorsOwned` |
| wirtualne, `ReentrantLock` (21, 25, 26) | brak | `null` | 21: nazwy i stosy; 25: `parkBlocker` bez właściciela; 26: `parkBlocker` z `owner` |
| mieszane, monitory (21, 25, 26) | brak | 21: wątek platformowy `BLOCKED` z właścicielem `ForkJoinPool-1-worker-1` (nośnik, nie wątek wirtualny); 25 i 26: wątek platformowy `RUNNABLE` bez blokady, choć `Thread.getState` mówi `BLOCKED` | jak przy monitorach |
| mieszane, `ReentrantLock` (21, 25, 26) | brak | wątek platformowy `WAITING` z właścicielem, wątek wirtualny `null` | jak przy `ReentrantLock` |

Wnioski:

- `ThreadMXBean` nie widzi wątków wirtualnych na żadnej wersji: nie ma ich w `getAllThreadIds` ani w `Thread.getAllStackTraces`, `getThreadInfo` zwraca `null`, a `findDeadlockedThreads` nie znajduje żadnego cyklu z ich udziałem, także mieszanego.
- Na JDK 21 `synchronized` przypina wątek wirtualny do nośnika: przy zakleszczeniu dwa nośniki `ForkJoinPool` są `BLOCKED`, więc pula traci wątki, ale `findDeadlockedThreads` i tak nic nie zgłasza. Od JDK 24 wątek wirtualny zwalnia nośnik także na monitorze.
- Na JDK 25 i 26 wątek platformowy czekający na monitor wątku wirtualnego jest w `ThreadMXBean` `RUNNABLE` bez blokady, więc kontrola „długie `BLOCKED`” też go nie zobaczy. Wiarygodny jest `Thread.getState`.
- `Thread.getState` i `Thread.getStackTrace` działają dla wątków wirtualnych na każdej wersji, ale tylko dla wątków, do których ma się referencję.
- `dumpThreads` w JSON obejmuje wszystkie wątki, także wirtualne. Od JDK 25 podaje stan, monitor, na który wątek czeka (`blockedOn`), i trzymane monitory (`monitorsOwned`), więc cykl na monitorach da się ustalić z samego zrzutu. Właściciela `ReentrantLock` i innych synchronizatorów (`parkBlocker.owner`) podaje dopiero JDK 26. Format tekstowy ma te same informacje (`locked`, `waiting to lock`, `parking to wait for <...>, owner #46`), ale bez schematu.
- Koszt `dumpThreads` JSON na JDK 25 przy zaparkowanych wątkach wirtualnych: 16 ms i 1,1 MB dla 1 000, 105 ms i 11 MB dla 10 000, 0,9 s i 110 MB dla 100 000. To za dużo na kontrolę co 5 s, ale wystarczająco mało na zrzut przy podejrzeniu.

## Decyzja

- **Wątki platformowe:** kontrola zakleszczeń przez `findDeadlockedThreads` na każdej wersji JDK, tak jak w tasku. Kontrola „długie `BLOCKED`” bierze stan z `Thread.getState`, a nie z `ThreadInfo`, i opisuje w przewodniku, że wątek czekający na monitor wątku wirtualnego może wyglądać w narzędziach JMX na `RUNNABLE`.
- **Wątki wirtualne, sygnał:** podstawą jest licznik zadań działających dłużej niż próg w `TaskExecutor` i `BlockingExecutor` (`coilix.diagnostics.threads.long-task`). Wykonawcy Coilix znają swoje zadania i ich wątki, więc `Finding` `Warning` ma nazwę zadania, czas i stos z `Thread.getStackTrace`. Działa na JDK 17, 21, 25 i 26.
- **Wątki wirtualne, potwierdzenie:** gdy licznik zgłasza długie zadania, a JVM ma `HotSpotDiagnosticMXBean.dumpThreads` z informacją o blokadach (JDK 25 i nowsze), monitor robi zrzut JSON do pliku tymczasowego, najwyżej raz na `coilix.diagnostics.threads.dump-interval` (domyślnie 1 min), i szuka w nim cykli: na JDK 25 po `blockedOn` i `monitorsOwned`, a od JDK 26 także po `parkBlocker.owner`. Znaleziony cykl daje `Finding` `Critical` i reakcję z `on-deadlock`, tak jak zakleszczenie wątków platformowych. Plik zrzutu jest usuwany po analizie, chyba że `coilix.diagnostics.threads.keep-dumps = true`.
- **Odczyt zrzutu:** moduł `diagnostics` czyta JSON własnym małym parserem (obiekty, tablice, napisy, liczby, wartości logiczne), bo rdzeń nie ma zależności od biblioteki JSON. Wybiera format JSON, a nie tekstowy, bo nazwy pól są stałe, a tekst jest przeznaczony dla ludzi.
- **JDK 17 i 21:** zostaje tylko sygnał długich zadań. Na JDK 21 monitor dodatkowo zgłasza `Warning`, gdy wszystkie nośniki `ForkJoinPool-*-worker-*` są `BLOCKED` (przypięte wątki wirtualne). Przewodnik opisuje `jcmd <pid> Thread.dump_to_file -format=json <plik>` jako ręczną diagnozę.

## Konsekwencje

- Zakleszczenie wątków platformowych jest wykrywane na każdej wersji. Zakleszczenie z udziałem wątków wirtualnych jest wykrywane automatycznie na JDK 25 dla monitorów i od JDK 26 dla monitorów i `java.util.concurrent`. Na starszych wersjach aplikacja dostaje ostrzeżenie o długich zadaniach ze stosami, ale nie wyłącza się sama.
- Pełny zrzut przy dziesiątkach tysięcy wątków wirtualnych zajmuje około sekundy i pisze dziesiątki MB, dlatego jest robiony tylko przy podejrzeniu i z limitem częstotliwości, na wątku `coilix-diagnostics`.
- Format JSON zrzutu może się zmieniać między wersjami JDK. Test kontraktowy modułu tworzy zakleszczenie wątków wirtualnych i sprawdza wykrycie na wersjach z macierzy CI, a parser ignoruje nieznane pola.
