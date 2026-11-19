# 0019: Model efektów i beany rozróżniane po argumentach typów

- Status: proponowana
- Data: 2026-11-19
- Task: COILIX-00020 (kroki 1 i 2)

## Kontekst

COILIX-00020 wpina `cats.effect.IO` w Coilix. Opiekun ustalił kierunek: rdzeń zostaje w stylu bezpośrednim (kod blokujący na wątkach wirtualnych), a efekty wchodzą w wyznaczonych punktach. Wspieramy tylko `IO`. ZIO i fs2 są poza planem, a Pekko Streams to osobny task (COILIX-00029).

Aplikacja na `IO` ma zależności typu `Repository[IO]` i `Sync[IO]`. Kontener dopasowywał beany po klasie, więc `Repository[IO]` i `Repository[Option]` były dla niego tym samym typem. Eksperyment ze szkicu COILIX-00020 pokazał, że sygnatury generyczne JVM zachowują argumenty typów wyższego rzędu w parametrach konstruktorów, typach zwracanych metod `@Bean` i nadtypach klas, a tracą częściowe zastosowanie w lambdach typów.

## Decyzja

Model efektów:

- Rdzeń nie zna `IO` ani żadnej biblioteki efektów. Kontener jest synchroniczny, a moduł `cats-effect` dostanie interpreter planu kontekstu w `IO` (COILIX-00020, krok 3 i 5).
- `IORuntime` będzie beanem modułu `cats-effect`, zbudowanym z pul Coilix. `IO` nie jest przekładane na `Future` poza mostem do Pekko (COILIX-00029).

Dopasowanie po typach:

- `dev.coilix.common.GenericType` opisuje typ z argumentami: `Concrete(klasa, argumenty)`, `Variable(nazwa)` i `Wildcard`. `GenericType.of` czyta go z refleksji Javy, a `isAssignableFrom` sprawdza, czy kandydat rozszerza typ z równymi argumentami (przez nadtypy z podstawieniem zmiennych). Wildcard w oczekiwanym typie pasuje do każdego argumentu, a zmienna w kandydacie do żadnego. Typ bez argumentów porównuje tylko klasy, więc zależności niegeneryczne działają jak wcześniej.
- `dev.coilix.common.TypeInfo[T]` niesie pełny typ statyczny do runtime. Wyprowadza go makro: konstruktor typu po eta-ekspansji (`Repo[IO]`) jest zapisywany jako sama klasa, prymitywne argumenty jako `Object` (tak jak w sygnaturach), a dla typu abstrakcyjnego makro bierze `ClassTag`. Lambda typu to błąd kompilacji.
- `BeanDefinition`, `RegisteredBean` i `Dependency` niosą `beanType: GenericType` zamiast samej klasy (`beanClass` zostaje jako wyliczona klasa). `CandidateSelector`, `BeanRegistry` i `DependencyResolver` dopasowują po `GenericType`. `ApplicationContext.getBean`, `getBeanOption`, `getBeansOfType`, `BeanReplacements` i testkit (`bean`, `replaceBean`, `addBean`) przyjmują `[T: TypeInfo]` zamiast `[T: ClassTag]`. Dotychczasowe wywołania się kompilują, bo `TypeInfo` jest wyprowadzane automatycznie.
- `RegisteredBean.of[T: TypeInfo]` rejestruje gotową instancję z pełnym typem, a `RegisteredBean.ofClass` tylko z klasą.
- Zależność ze zmienną typu, której nic nie wiąże (`SqlRepo[F[_]](repo: Repo[F])`), i z argumentem będącym klasą z kilkoma parametrami typu (tak JVM zapisuje lambdę typu, np. `[A] =>> Either[String, A]`) to `UnsupportedDependencyTypeException` przy tworzeniu definicji, z nazwą parametru.

## Konsekwencje

- Zmienia się publiczne API rdzenia (`BeanDefinition`, `RegisteredBean`, `Dependency`, `CandidateSelector`, `BeanRegistry`, `DependencyResolver`, `TestBean`, `BeanReplacements`). Rdzeń nie ma jeszcze wydania, więc nie jest to zmiana niezgodna wobec użytkowników.
- Klasa generyczna skanowana bez wiązania zmiennej nie może mieć zależności od tej zmiennej. Wiąże ją metoda `@Bean` z konkretnym typem. Wiązanie efektu dla całej aplikacji (`coilix.effect.type`) przyjdzie z modułem `cats-effect`.
- Heurystyka dla lambd typów odrzuca też poprawny konstruktor typu z dwoma parametrami użyty jako argument (np. dla traitu z `F[_, _]`). To rzadkie, a komunikat wyjaśnia powód. Dokładne sprawdzenie wymagałoby czytania TASTy.
- Wariancja argumentów (`Repo[+A]`) nie jest brana pod uwagę: argumenty muszą być równe.
