# 0003: Komponenty frameworka podmieniane w configu

- Status: przyjęta
- Data: 2026-10-30 (zapis wsteczny)
- Task: COILIX-00003

## Kontekst

Użytkownik powinien móc zmienić zachowanie frameworka (generowanie nazw beanów, wybór konstruktora, logowanie startu) bez forka i bez dziedziczenia po klasach wewnętrznych. Komponenty frameworka powstają przed kontekstem, więc nie mogą być zwykłymi beanami.

## Decyzja

- Każdy komponent to trait z implementacją domyślną.
- Obiekt towarzyszący traitu ma `given ConfigPath[T]` ze ścieżką `coilix.<moduł>.<komponent>` i `def default(using ComponentProvider): T`.
- `ConfigComponentProvider` tworzy klasę z klucza `class` pod tą ścieżką, a gdy go nie ma, używa implementacji domyślnej.
- Lista punktów rozszerzeń jest w [overview.md](../architecture/overview.md#punkty-rozszerzeń).

## Konsekwencje

- Każdy komponent da się podmienić jedną linią configu, także w testach.
- Ścieżki `coilix.*` i publiczne klasy `Default*` są częścią publicznego API. Ich zmiana jest niezgodna według [versioning.md](../architecture/versioning.md).
- Więcej typów w kodzie (trait, implementacja, obiekt towarzyszący) i więcej Scaladoca do utrzymania.
