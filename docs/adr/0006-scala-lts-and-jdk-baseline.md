# 0006: Scala z linii LTS i JDK 17 dla publikowanych modułów

- Status: przyjęta
- Data: 2026-10-30 (zapis wsteczny)
- Task: COILIX-00009

## Kontekst

Biblioteka skompilowana Scalą 3.N działa tylko w projektach na 3.N i nowszych. Kompilacja najnowszą Scalą wymusza ją na użytkownikach.

## Decyzja

Publikowane moduły kompiluje się najnowszą wersją z linii LTS (teraz 3.9.0), a bytecode generuje dla JDK 17. CI testuje na JDK 17 i najnowszym LTS. Szczegóły są w [versioning.md](../architecture/versioning.md#wersja-scali-i-jdk).

## Konsekwencje

- Coilix działa w każdym projekcie na Scali 3.9+ i JDK 17+.
- Funkcje języka nowsze niż linia LTS są niedostępne w kodzie frameworka.
- Scala Steward ma przypiętą Scalę do linii `3.9.` (`.scala-steward.conf`), żeby nie proponował wyjścia z LTS.
- Podniesienie minimalnej wersji Scali lub JDK od `1.0.0` to zmiana `MAJOR`.
