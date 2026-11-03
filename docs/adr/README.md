# Decyzje architektoniczne (ADR)

Krótkie zapisy decyzji, które trudno odwrócić: kontekst, decyzja i konsekwencje. Format według [Michaela Nygarda](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions).

Decyzje 0002–0007 zapadły przed wprowadzeniem ADR i zostały zapisane wstecz 2026-10-30 na podstawie kodu, tasków i dokumentów w `docs/`.

| Nr | Decyzja | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Zapisywanie decyzji jako ADR | przyjęta |
| [0002](0002-runtime-reflection-and-java-annotations.md) | Refleksja w runtime i adnotacje w Javie | przyjęta |
| [0003](0003-replaceable-framework-components.md) | Komponenty frameworka podmieniane w configu | przyjęta |
| [0004](0004-immutable-context-and-constructor-injection.md) | Niemutowalny kontekst i wstrzykiwanie przez konstruktor | przyjęta |
| [0005](0005-slf4s-compiler-plugin.md) | Logger `@Slf4s` przez plugin kompilatora | przyjęta |
| [0006](0006-scala-lts-and-jdk-baseline.md) | Scala z linii LTS i JDK 17 dla publikowanych modułów | przyjęta |
| [0007](0007-apache-license-and-dco.md) | Apache-2.0 i DCO zamiast CLA | przyjęta |
| [0008](0008-squash-merge.md) | Scalanie przez squash, jeden PR to jedna zmiana | przyjęta |
| [0009](0009-dev-coilix-namespace.md) | Przestrzeń nazw `dev.coilix` | przyjęta |
| [0010](0010-docusaurus-documentation-site.md) | Strona dokumentacji w Docusaurusie pod `docs.coilix.dev` | przyjęta |

## Nowa decyzja

1. Plik `NNNN-tytul-w-kebab-case.md` z kolejnym numerem, według szablonu niżej.
2. Status `proponowana` w PR, `przyjęta` po scaleniu.
3. Decyzji się nie edytuje. Zmiana kierunku to nowy ADR ze statusem `zastępuje NNNN`, a stary dostaje status `zastąpiona przez NNNN`.

```markdown
# NNNN: Tytuł

- Status: proponowana | przyjęta | zastąpiona przez NNNN
- Data: RRRR-MM-DD
- Task: COILIX-NNNNN

## Kontekst
## Decyzja
## Konsekwencje
```
