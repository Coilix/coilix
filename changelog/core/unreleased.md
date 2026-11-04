### Added

- IoC container with constructor injection, `@Component`, `@Service`, `@Repository`, `@Qualifier`, `@Primary`, `@Order`, and `Option[T]` and `List[T]` dependencies (COILIX-00000).
- `@Slf4s` compiler plugin that adds an SLF4J `log` field (COILIX-00000).
- Application startup with `@CoilixApplication`, `Coilix.run`, `ApplicationRunner`, command-line arguments, a banner and a shutdown hook (COILIX-00002).
- Configuration from HOCON, YAML and properties files, command-line options and system properties, `@Value`, `@ConfigurationProperties`, `@Configuration` with `@Bean`, and replacement of framework components through a `class` key under their `coilix.*` path (COILIX-00003).

### Changed

- Startup log messages are now `Launching <app>` and `<app> is up after <n> s` (COILIX-00008).
- Artifacts are compiled with Scala 3.9 LTS for JDK 17, so they work with Scala 3.9 or newer and JDK 17 or newer (COILIX-00009).

### Fixed

- A bean with an `Option[T]` or `List[T]` dependency now receives every matching bean, not only the beans created before it. A cycle through an `Option[T]` dependency is now reported as `CircularDependencyException` (COILIX-00012).
