# 0007: Apache-2.0 i DCO zamiast CLA

- Status: przyjęta
- Data: 2026-10-30 (zapis wsteczny)
- Task: COILIX-00006, COILIX-00008

## Kontekst

Framework ma być używany komercyjnie i przyjmować kontrybucje z zewnątrz. Analiza licencji jest w [docs/legal/license-decision.md](../legal/license-decision.md).

## Decyzja

- Licencja Apache-2.0.
- Kontrybucje przez Developer Certificate of Origin (`Signed-off-by`, `git commit -s`), bez CLA.
- Zależności tylko na licencjach zgodnych z Apache-2.0, sprawdzane przez `sbt checkLicenses`.

## Konsekwencje

- Licencja patentowa od kontrybutorów i brak copyleftu.
- Każdy commit musi mieć `Signed-off-by`, także commity botów aktualizujących zależności.
- Brak CLA oznacza, że późniejsza zmiana licencji wymagałaby zgody wszystkich kontrybutorów.
