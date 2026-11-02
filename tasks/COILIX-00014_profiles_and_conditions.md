# COILIX-00014: Profile i warunkowe beany

## Cel

Bean może trafić do kontekstu tylko pod warunkiem: aktywny profil, wartość w configu, obecność klasy na classpath albo brak innego beana danego typu. Pozwala to mieć różne beany dla `dev` i `prod` oraz domyślne beany modułów funkcji, które aplikacja może zastąpić własnymi.

## Stan obecny

- Każda klasa ze skanowania i każda metoda `@Bean` trafia do kontekstu bezwarunkowo.
- Jedyny sposób wyboru implementacji to `@Primary`, który działa w kodzie, a nie w configu.
- COILIX-00010 planuje wyłączanie całych modułów (`coilix.modules.<nazwa>.enabled`), ale nie pojedynczych beanów.

## Zakres

1. Profile:
   - adnotacja `@Profile("dev")` na klasie i metodzie `@Bean`, z negacją `!prod`,
   - aktywne profile z `coilix.profiles.active` (lista), więc także z `--coilix.profiles.active=dev`,
   - dodatkowy plik `application-<profil>.{properties,yaml,conf}` ładowany nad `application.*`.
2. Warunki:
   - `@ConditionalOnProperty(name, havingValue, matchIfMissing)`,
   - `@ConditionalOnClass(name)` (nazwa jako string, żeby brak klasy nie psuł ładowania adnotacji),
   - `@ConditionalOnMissingBean` na metodzie `@Bean`: bean powstaje, gdy nikt inny nie dostarcza tego typu.
3. Warunki są sprawdzane przed `DependencyResolver`, więc odrzucony bean nie wpływa na kolejność ani na wybór kandydata.
4. Log na poziomie `debug` z powodem odrzucenia każdej definicji.

## Proponowany projekt

- `02_annotations`: `Profile.java`, `ConditionalOnProperty.java`, `ConditionalOnClass.java`, `ConditionalOnMissingBean.java`.
- `04_config`: profile w `ClasspathEnvironmentLoader` (warstwy `application-<profil>.*`).
- `05_context`:
  - `BeanCondition` (trait) z `matches(definition, environment)`,
  - `BeanDefinition` dostaje listę warunków, wypełnianą w `ReflectiveBeanDefinitionFactory` i `ReflectiveBeanMethodScanner`,
  - `DefinitionFilter` (trait, `given ConfigPath` `coilix.context.definition-filter`, `default`) i `ConditionalDefinitionFilter`,
  - `@ConditionalOnMissingBean` w drugim przebiegu, po odrzuceniu pozostałych warunków.
- Dopisanie `coilix.context.definition-filter` do tabeli w `docs/architecture/overview.md`.

## Kryteria akceptacji

1. Bean z `@Profile("dev")` jest w kontekście tylko przy aktywnym `dev`.
2. `application-dev.yaml` nadpisuje `application.yaml` przy aktywnym `dev`.
3. `@ConditionalOnMissingBean` ustępuje beanowi aplikacji tego samego typu.
4. Odrzucona definicja nie powoduje `AmbiguousBeanException` ani `NoSuchBeanException`.
5. Demo ma bean zależny od profilu i pokazuje różnicę przy `--coilix.profiles.active=dev`.

## Poza zakresem

- Własne warunki użytkownika przez adnotację na adnotacji (meta-adnotacje). Można dodać później przez `BeanCondition`.
- Wyrażenia logiczne na profilach (`dev & cloud`).

## Ryzyka i otwarte pytania

- `@ConditionalOnMissingBean` zależy od kolejności oceny. Proponowane: najpierw beany aplikacji, potem beany z modułów (COILIX-00010), i tylko moduły mogą używać tego warunku.
- Czy profile mają być w `config` (wpływają na pliki), czy w `context` (wpływają na beany)? Proponowane: lista aktywnych profili w `Environment`, a reszta w `context`.
