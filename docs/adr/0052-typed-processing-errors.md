# 0052: Błędy typowane procesorów tła i ustawień logowania

- Status: proponowana
- Data: 2026-11-27
- Task: COILIX-00031 (krok 10)

## Kontekst

Po ADR 0050 i 0051 kontener, start i konfiguracja zwracają przewidywalne błędy jako typ. Moduł `processing` dalej rzucał wyjątki: `ProcessorSettings.resolve` i `ShardAssignment.named` rzucały `ConfigurationException`, `ProcessorAssembler` `InvalidProcessorException`, a operacje `ProcessorManager` kończyły `IO` wyjątkami `NoSuchProcessorException` i `ShardNotOwnedException`. Bean, który steruje procesorami na żądanie z zewnątrz (na przykład z HTTP), musiał łapać klasy wyjątków, żeby odróżnić nieznany procesor od błędu w kodzie. `LoggingSettings` też rzucał wyjątek dla nieznanego poziomu, a `CoilixLogging` łapał go przez `Try`.

## Decyzja

- **Dwa typy błędów w `processing`, bo mają różnych odbiorców.** `ProcessingSetupError` (`InvalidSettings`, `UnknownAssignment`, `DuplicateIdentifier`, `NoEngine`, `SeveralEngines`) opisuje, dlaczego procesorów nie da się przygotować. Zwracają go `ProcessorSettings.resolve`, `ShardAssignment.named` i `ProcessorAssembler.assemble`. `ProcessorError` (`NoSuchProcessor`, `ShardNotOwned`) opisuje, dlaczego operacji nie da się wykonać. Jeden typ dla obu kazałby wywołującemu operację obsługiwać przypadki, które nie mogą się zdarzyć po starcie.
- **Operacje menedżera w `EitherT`.** `status`, `start`, `stop` i `restart` zwracają `EitherT[IO, ProcessorError, A]`. `stopAll`, `processors` i `topology` nie mają przewidywalnych błędów i zostają bez zmian. `ProcessorError.raised` zamienia błąd na wyjątek dla wywołującego, który nie chce go obsługiwać, a `fold` i `exists` z `EitherT` wystarczają do sprawdzenia stanu.
- **Bean `processorManager` jako warstwa najwyższa przygotowania.** Jego `Resource` kończy się wyjątkiem błędu (`ConfigurationException` dla ustawień, `InvalidProcessorException` dla procesorów), który kontener zgłasza jako `ContextError.CreationFailed`, więc start kończy się z tym samym komunikatem co wcześniej. Wyjątek z tworzenia klasy procesora dalej jest defektem.
- **Ustawienia logowania w dwóch formach.** `LoggingSettings.read` zwraca `Either[ConfigError, LoggingSettings]` przez `Environment.lookup`, a nieznany poziom to `ConfigError.InvalidValue`. `LoggingSettings.from` rzuca `exception` błędu dla beanów. `CoilixLogging` używa `read` zamiast `Try`.

## Konsekwencje

- Kod sterujący procesorami widzi w typie, że procesor może nie istnieć, a shard może działać na innej replice, i sam decyduje o odpowiedzi.
- Zmiana API `ProcessorManager`, `ProcessorSettings.resolve` i `ShardAssignment.named`. Moduły `processing` i `logging` nie miały jeszcze wydania, więc changelog opisuje nowe API we wpisach `Added`.
- Ustawienia innych modułów (`ConcurrencySettings`, `ClusterSettings`, `EventsSettings`) dalej czytają konfigurację formami rzucającymi, bo czytają je tylko beany w czasie tworzenia. Ta sama zasada obejmie `events` przy jego przepisaniu.
