# COILIX-00016: Zdarzenia aplikacji

## Cel

Beany publikują i odbierają zdarzenia w obrębie kontekstu, a framework publikuje własne zdarzenia cyklu życia (kontekst gotowy, aplikacja uruchomiona, zamykanie). Pozwala to luźno łączyć beany i podpinać się pod start bez `ApplicationRunner`.

## Stan obecny

- Brak mechanizmu zdarzeń.
- Jedyny punkt po starcie to `ApplicationRunner`. Kod przy zamykaniu będzie miał `@PreDestroy` (COILIX-00004), ale nie wie, czy zamknięcie wynika z błędu startu, czy z Ctrl+C.

## Zakres

1. `EventPublisher` (trait) jako bean wbudowany, jak `applicationContext` i `environment`.
2. Odbiorcy:
   - bean implementujący `EventListener[E]`,
   - metoda z adnotacją `@EventListener` przyjmująca jeden parametr typu zdarzenia.
3. Dopasowanie po typie zdarzenia z dziedziczeniem: odbiorca `EventListener[ContextEvent]` dostaje wszystkie zdarzenia kontekstu.
4. Kolejność odbiorców według `@Order`.
5. Zdarzenia frameworka: `ContextReady`, `ApplicationStarted` (po runnerach), `ContextClosing`, `ApplicationFailed`.
6. Wyjątek w odbiorcy przerywa publikację i trafia do publikującego. Wyjątek przy `ContextClosing` jest logowany i nie przerywa zamykania.

## Proponowany projekt

- `05_context`: `EventPublisher`, `EventListener[E]`, `SynchronousEventPublisher` (`given ConfigPath` `coilix.context.event-publisher`, `default`), `EventListenerMethodScanner`.
- `09_boot`: publikacja `ApplicationStarted` i `ApplicationFailed` w `DefaultCoilixApplicationRunner`.
- Odbiorcy są ustalani przy budowie kontekstu, więc publikacja nie skanuje rejestru za każdym razem.

## Kryteria akceptacji

1. Bean publikuje zdarzenie, a dwa odbiorcy dostają je w kolejności `@Order`.
2. `@EventListener` na metodzie działa tak samo jak `EventListener[E]`.
3. `ContextReady` przychodzi przed pierwszym `ApplicationRunner`, a `ContextClosing` przed `@PreDestroy`.
4. Demo loguje zdarzenia startu i zamykania.

## Poza zakresem

- Zdarzenia asynchroniczne. Przyjdą po COILIX-00005 jako `@Async` na odbiorcy.
- Zdarzenia w transakcjach (moduł `data`).

## Ryzyka i otwarte pytania

- Zależy od COILIX-00004 (`close()`, `@PreDestroy`) dla `ContextClosing`.
- Odbiorca, który sam zależy od `EventPublisher`, nie tworzy cyklu, bo publisher jest beanem wbudowanym. Trzeba to potwierdzić testem.
