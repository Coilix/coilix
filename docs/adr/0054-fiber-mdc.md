# 0054: MDC fibera na `IOLocal`

- Status: proponowana
- Data: 2026-11-27
- Task: COILIX-00031 (przed krokiem 4)

## Kontekst

Krok 4 COILIX-00031 przenosi szynę zdarzeń na `IO`: asynchroniczni odbiorcy staną się fiberami pod `Supervisor`. MDC z SLF4J jest przypięte do wątku, a fiber zmienia wątki przy każdym `IO.blocking`, `IO.sleep` i przejściu asynchronicznym. Dziś MDC publikującego przenosi do odbiorcy `MdcContextPropagator` przy zadaniu `TaskExecutor`. Po przejściu na fibery to przestanie działać, a demo ma test, który sprawdza klucz `job` w powiadomieniu wysłanym przez asynchronicznego odbiorcę.

cats-effect 3.6 dodał `IOLocal#unsafeThreadLocal`: widok wartości `IOLocal` jako `ThreadLocal`, czytany z wątku, który właśnie wykonuje fiber. Działa przy włączonym śledzeniu kontekstu fiberów (`cats.effect.trackFiberContext=true`), które trzeba ustawić, zanim cats-effect uruchomi pierwszy fiber.

## Decyzja

- **MDC dostawcy SLF4J w `logging` należy do fibera.** `FiberMdcAdapter` trzyma MDC kodu w fiberze w `IOLocal[Map[String, String]]` (`FiberContext`), a MDC kodu poza fiberem w zwykłym `BasicMDCAdapter`. Fibery dziedziczą `IOLocal` przy starcie, więc fiber uruchomiony przez inny zaczyna z kopią jego MDC, a zmiany w nim nie wracają do rodzica. Stosy wartości (`pushByKey`) zostają przy wątku.
- **Rozpoznanie fibera przez `IOFiber.currentIOFiber`.** Bez tego adapter nie odróżni wątku bez fibera od fibera z pustym MDC, a zapis do `IOLocal` poza fiberem kończy się błędem. cats-effect nie udostępnia tej metody w Scali, więc adapter woła ją przez refleksję, a testy modułu sprawdzają to zachowanie przy każdej aktualizacji cats-effect. Bez śledzenia kontekstu albo bez tej metody adapter działa jak zwykłe MDC wątku.
- **`CoilixIOApp` włącza śledzenie.** Ustawia `cats.effect.trackFiberContext=true` w inicjalizacji traitu, przed startem runtime'u, chyba że właściwość jest już ustawiona. Testy poza `CoilixIOApp` ustawiają ją w `Test / javaOptions`, co opisuje przewodnik.

## Konsekwencje

- `MDC.put` w `IO` działa jak w kodzie synchronicznym, także po `IO.blocking`, a asynchroniczni odbiorcy i procesory dostaną MDC tego, kto je uruchomił, bez osobnego przenoszenia.
- Śledzenie kontekstu kosztuje trochę przy każdym przełączeniu fibera. Wydajność sprawdzi COILIX-00017.
- Zależność od wewnętrznej metody cats-effect. Przy zmianie jej nazwy adapter przechodzi na MDC wątku, a test `FiberMdcAdapterTest` przestaje przechodzić, więc zmiana nie przejdzie niezauważona.
- Bez `coilix-logging` (Logback, Log4j 2) MDC zostaje przy wątku. Most dla innych backendów to osobna sprawa.
