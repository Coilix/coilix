# Podobieństwo do Springa, znaki towarowe i nazwa

## Kod

- Spring Framework i Spring Boot są w Javie, Scaling w Scali 3. Architektura jest inna: traity z implementacją w `default` obiektu towarzyszącego, `ConfigPath` i `ComponentProvider` do podmiany komponentów, niemutowalne rejestry, plugin kompilatora `@Slf4s` zamiast Lomboka. Dosłowne skopiowanie kodu jest w tej sytuacji praktycznie wykluczone.
- Porównanie narzędziem typu PMD CPD między Javą a Scalą nie da wiarygodnego wyniku. Zamiast tego sprawdziliśmy teksty, które dałoby się przenieść bez zmian między językami: komunikaty wyjątków, logi, banner, pliki zasobów i dokumentację.

### Teksty

| Miejsce | Tekst | Ocena |
|---|---|---|
| `DefaultStartupMessageFormatter.started` | `Started <app> in <n> seconds` | Prawie tak samo jak log Spring Boota `Started <app> in <n> seconds (process running for <m>)`. Krótka fraza opisująca fakt, raczej niechroniona, ale rozpoznawalna. **Do przeredagowania**, np. `<app> ready in <n> s`. |
| `DefaultStartupMessageFormatter.starting` | `Starting <app>` | Fraza ogólna, bez zmian. |
| `10_demo/.../banner.txt` | ASCII-art „Scaling” | Własny napis wygenerowany fontem FIGlet „standard”. Mechanizm `banner.txt` jest funkcją, a nie utworem. Bez zmian. |
| Komunikaty wyjątków (`No bean of type ...`, `Circular dependency detected among ...`, `Multiple beans of type ...`) | własne sformułowania | Opisują ten sam problem co Spring, ale innymi słowami. Bez zmian. |
| Klucze configu (`scaling.boot.*`, `scaling.context.*`) | własna przestrzeń `scaling.*` | Bez zmian. |
| Scaladoc i przewodniki | jeszcze nie istnieją | Zasada: pisać własnymi słowami, nie tłumaczyć ani nie parafrazować dokumentacji Springa. |

## API

Nazwy adnotacji odpowiadają Springowi: `@Component`, `@Service`, `@Repository`, `@Configuration`, `@Bean`, `@Autowired`, `@Qualifier`, `@Primary`, `@Order`, `@Value`, `@ConfigurationProperties`. Do tego pojęcia `ApplicationContext` i `ApplicationRunner`.

- **UE:** TSUE (C-406/10 *SAS Institute v World Programming*, 2012) nie uznaje funkcjonalności, języka programowania ani formatu plików za chronione wyrażenie programu komputerowego. Dyrektywa 2009/24/WE chroni wyrażenie (kod), a nie idee i zasady leżące u podstaw interfejsów.
- **USA:** Sąd Najwyższy (*Google LLC v. Oracle America, Inc.*, 2021) uznał ponowną implementację deklaracji API Javy przez Google za dozwolony użytek, nie rozstrzygając, czy API w ogóle podlega ochronie.
- **Licencja Springa:** Spring jest na Apache-2.0, która pozwala tworzyć utwory zależne. Nawet przy najszerszej interpretacji ochrony API odtworzenie nazw jest dozwolone.
- Same nazwy (`Component`, `Service`, `Bean`) to krótkie, ogólne słowa, używane też w Jakarta EE, Micronaut, Quarkus i Guice.

**Wniosek:** nazwy adnotacji i pojęć mogą zostać. Mają też wartość praktyczną, bo programista Springa od razu rozumie framework.

## Znaki towarowe

„Spring”, „Spring Boot”, „Spring Cloud” i logo Spring to znaki towarowe Broadcom Inc. Zasady są na [spring.io/trademarks](https://spring.io/trademarks).

**Zabronione:**

- „Spring” w nazwie produktu, firmy, artefaktu, pakietu, domeny albo repozytorium (np. „Spring Scala”, „ScalaSpring”, `spring-scala.dev`),
- łączenie „Spring” z innymi słowami albo znakami i warianty typu „SpringDB”,
- logo Spring i elementy wizualne Springa (zielony liść) w materiałach projektu,
- sugerowanie powiązania z Broadcom lub poparcia przez Broadcom albo zespół Springa,
- „Spring” w tytułach stron i meta tagach w celu pozycjonowania strony z dokumentacją.

**Dozwolone przy rzetelnym, opisowym użyciu:**

- zdanie w README i dokumentacji w rodzaju: *„Scaling is an IoC framework for Scala 3 with an API modeled on the Spring Framework. It is not affiliated with or endorsed by Broadcom or the Spring team.”*,
- przewodnik „Coming from Spring” porównujący pojęcia,
- atrybucja: *„Spring is a trademark of Broadcom Inc. and/or its subsidiaries.”*

Zasady Springa nie mówią wprost o sformułowaniu „inspired by”. Bezpieczniejsze jest opisowe „API modeled on” albo „familiar to Spring developers” razem z zastrzeżeniem o braku powiązania.

**Stan obecny:** `CLAUDE.md`, taski i `.gitignore` (`.springBeans`) wspominają Springa wewnętrznie. To nie jest użycie znaku w obrocie, ale `CLAUDE.md` będzie publiczny, więc opis projektu warto zmienić na opisowy („framework IoC z API wzorowanym na Springu”). `@ScalingBootApplication`: słowo „Boot” samo nie jest znakiem, a konstrukcja „<nazwa> Boot” jest szeroko używana. Ryzyko jest niskie, ale przy zmianie nazwy adnotacja traci „Boot” i staje się `@CoilixApplication` (SCALING-00008).

## Nazwa projektu

**Decyzja (2026-10-29): projekt zmienia nazwę ze „Scaling” na „Coilix”.**

Powody rezygnacji ze „Scaling”:

- to zwykłe angielskie słowo („skalowanie”), więc daje słabą ochronę jako znak towarowy i trudno je wyszukać („scaling scala” zwraca wyniki o skalowaniu aplikacji),
- **domena `scaling.org` jest zajęta** (zwraca adresy IP), więc `org.scaling` odpada jako `groupId` na Maven Central.

Odrzucone kandydatury:

- „Mortise”: zajęte `mortise.dev`, `mortise.io`, `mortise.org` i konto `mortise` na GitHubie,
- „Nimbo”: zajęte domeny i konto GitHub, 474 repozytoria o tej nazwie, w tym narzędzie ofensywne Nimbo-C2,
- krótkie angielskie słowa ze stolarstwa (`tenon`, `dowel`, `dovetail`, `joinery` i inne): wszystkie domeny `.dev` i konta GitHub zajęte.

„Coilix” to nazwa wymyślona od *coil*: sprężyny i spirali. Spirala to motyw logo Scali, a zwinięta sprężyna oddaje energię komponentów gotowych do użycia. Sprawdzenie z 2026-10-29:

- nie zawiera znaku Spring ani nie nawiązuje do pór roku, więc nie sugeruje związku ze Springiem,
- wyszukiwarka nie zwraca produktu, firmy ani znaku o tej nazwie (najbliższa jest firma „Coelix”, inna pisownia i inna branża),
- Maven Central: brak artefaktów `coilix*`,
- `coilix.dev` i `coilix.io` niezarejestrowane (RDAP zwraca 404),
- organizacja GitHub [Coilix](https://github.com/Coilix) założona przez autora. Istnieje nieaktywne konto użytkownika `Coillix` (podwójne „l”, założone w 2014, 0 repozytoriów), co nie koliduje z przestrzenią nazw `io.github.coilix`.

Przed publikacją: przeszukać EUIPO (TMview) i USPTO pod kątem znaków „Coilix” w klasach 9 i 42 Nicei (wymaga ręcznego wyszukania w formularzach rejestrów). Warto też kupić `coilix.dev`, żeby nikt jej nie zajął, nawet jeśli `groupId` zostaje `io.github.coilix`.

## Działania

1. Zmienić `Started <app> in <n> seconds` na własne sformułowanie (SCALING-00008).
2. Dodać do README zastrzeżenie o braku powiązania i atrybucję znaku (SCALING-00008).
3. Dodać do `CLAUDE.md` zasady: bez „Spring” w nazwach i bez kopiowania tekstów ani kodu Springa (zrobione).
4. Sprawdzić nazwę w rejestrach znaków przed pierwszą publikacją (SCALING-00008).
