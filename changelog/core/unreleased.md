### Added

- IoC container with constructor injection, `@Component`, `@Service`, `@Repository`, `@Qualifier`, `@Primary`, `@Order`, and `Option[T]` and `List[T]` dependencies (COILIX-00000).
- `@Slf4s` compiler plugin that adds an SLF4J `log` field (COILIX-00000).
- Application startup with `@CoilixApplication`, `Coilix.run`, `ApplicationRunner`, command-line arguments, a banner and a shutdown hook (COILIX-00002).
- Configuration from HOCON, YAML and properties files, command-line options and system properties, `@Value`, `@ConfigurationProperties`, `@Configuration` with `@Bean`, and replacement of framework components through a `class` key under their `coilix.*` path (COILIX-00003).
- Bean lifecycle callbacks: `@PostConstruct` methods run after a bean is created and before it is injected, and `@PreDestroy` methods run in reverse creation order when the context closes. Both may be private (COILIX-00004).
- Optional modules: a `CoilixModule` listed in `META-INF/services/dev.coilix.context.CoilixModule` adds its components to the context when it is on the classpath, and `coilix.modules.<name>.enabled = false` disables it (COILIX-00010).
- Profiles: `coilix.profiles.active` (also `--coilix.profiles.active=dev,local`), `@Profile` with `!` negation, and `application-<profile>.*` files that override `application.*` (COILIX-00014).
- Conditional beans: `@ConditionalOnProperty`, `@ConditionalOnClass` and `@ConditionalOnMissingBean`, checked before dependencies are resolved, so a left-out bean causes no ambiguous or missing dependency (COILIX-00014).
- `EditOption[T]` for partial updates, which tells a field that stays unchanged (`NoEdit`) apart from a field that is set (`SetTo(Some(value))`) or cleared (`SetTo(None)`) (COILIX-00022).
- `Validator[T]` with `Violation`, `ViolationTemplate` and `ValidationException`, which checks every rule of a value and reports all broken rules at once. `Violation("...")` and `ViolationTemplate("... %s")(...)` take their code from the name of the `val` and fail compilation for a malformed message or template (COILIX-00022).
- `@ValueType` for classes that wrap one value, with `ValueTypeResolver` to create them from their inner value, and `Secret`, a value type for passwords and tokens that `toString` hides (COILIX-00022).
- `@Enumerated` for Scala 3 enums read and written by name, `@EnumName` for a custom case name with older aliases, and `EnumResolver`, which looks up cases by name and lists the allowed names when a name is unknown (COILIX-00022).
- `@Value` and `@ConfigurationProperties` accept `@ValueType` classes, `Secret` and `@Enumerated` enums, also inside `Option` and collections. An invalid value stops the startup with the configuration path and the reason (COILIX-00022).
- `coilix-testkit` for munit tests: `CoilixTestSetup` with properties and profiles that win over every other source, `replaceBean` and `addBean`, command-line arguments and optional runners, `setup.use { ... }` and the `CoilixSuite` trait that run test bodies with the context given to them, helpers such as `bean[T]`, `beans[T]` and `property[T](path)` that take the context from `using`, and the explicit `CoilixFixtures` with a context per test or per suite that is closed afterwards (COILIX-00013).
- A `CoilixModule` can collect classes marked with its own annotations through `collects`. Such classes are not beans: the module gets them from the `CollectedComponents` bean, creates them with their dependencies, `@Value` parameters and `@PostConstruct` methods, and closes them to run `@PreDestroy`. A class with a component annotation and a collected one fails the startup with `CollectedComponentConflictException`, and a bean that injects a collected class fails with a `NoSuchBeanException` that says why (COILIX-00026).
- Beans are matched by their full type with type arguments, so `Repository[IO]` and `Repository[Option]` are different beans for constructor parameters, `Option[T]` and `List[T]` dependencies, `getBean`, `BeanReplacements` and the testkit. `TypeInfo[T]` carries the full static type to runtime, and `RegisteredBean.of[T]` registers a ready instance with it. A dependency with an unbound type variable or a type lambda fails with `UnsupportedDependencyTypeException` (COILIX-00020).
- Bean creation is planned before any bean is created: `ContextPlanner` returns a `ContextPlan` with the ready instances and the beans in dependency order, so another interpreter can run the same plan. A `@Bean` method returning `cats.effect.IO[A]` or `cats.effect.kernel.Resource[F, A]` gives a bean of type `A`, recognized by name through the replaceable `EffectfulResultRecognizer`. The synchronous builder rejects such beans with `EffectfulBeanException` until an effect module runs them (COILIX-00020).
- New core module `coilix-concurrency`, included with `coilix-boot`: a `TaskExecutor` bean that runs each task on a new virtual thread (a platform thread on JDK 17, or a fixed pool with `coilix.concurrency.executor.virtual-threads = false`) and a `TaskScheduler` bean for delayed, fixed-rate and fixed-delay tasks. Both are closed with the context, wait up to `shutdown-timeout` for running tasks and then interrupt them. An application `TaskExecutor` marked `@Primary` replaces the default one (COILIX-00005).
- `CoilixBootstrap.start` with `BeanReplacements` leaves out discovered beans of the replaced types, and fails with `UnmatchedBeanReplacementException` when a replaced type matches no bean (COILIX-00013).

### Changed

- Startup log messages are now `Launching <app>` and `<app> is up after <n> s` (COILIX-00008).
- Artifacts are compiled with Scala 3.9 LTS for JDK 17, so they work with Scala 3.9 or newer and JDK 17 or newer (COILIX-00009).

### Fixed

- A bean with an `Option[T]` or `List[T]` dependency now receives every matching bean, not only the beans created before it. A cycle through an `Option[T]` dependency is now reported as `CircularDependencyException` (COILIX-00012).
