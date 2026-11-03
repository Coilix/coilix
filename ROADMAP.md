# Roadmap

Coilix is in early development. This page lists what is planned and in which order. Each item links to a task in [tasks/](tasks/README.md), where the scope and acceptance criteria are described (in Polish). The order can change; the task index has the current status.

## 0.1: first release

The goal of the first release is a usable dependency injection container with configuration, startup and testing support, published to Maven Central.

| Task | Feature | Status |
|---|---|---|
| COILIX-00002 | application startup with `@CoilixApplication` and `ApplicationRunner` | done |
| COILIX-00003 | configuration files, `@Value`, `@ConfigurationProperties`, `@Configuration` and `@Bean` | done |
| COILIX-00012 | correct creation order for `Option[T]` and `List[T]` dependencies | planned |
| COILIX-00004 | lifecycle callbacks: `@PostConstruct` and `@PreDestroy` | planned |
| COILIX-00010 | optional modules discovered from the classpath | planned |
| COILIX-00014 | profiles and conditional beans | planned |
| COILIX-00013 | test support module (`coilix-testkit`) | planned |
| COILIX-00007 | versioned API documentation site | planned |
| COILIX-00009 | first release to Maven Central | in progress |

## 0.2 and later

| Task | Feature |
|---|---|
| COILIX-00005 | managed thread pools, `@Async` and `@Scheduled` |
| COILIX-00015 | prototype and lazy beans, `Provider[T]` |
| COILIX-00016 | application events |
| COILIX-00017 | startup benchmarks |
| COILIX-00018 | `sbt new` project template |
| COILIX-00020 | support for effect types `F[_]` (cats-effect, ZIO): analysis and ADR first |
| | `data`, `web` and `security` modules (not yet specified) |

## Known limitations

- Beans that depend on `Option[T]` or `List[T]` see only beans created before them, so an optional dependency can be `None` even though the bean exists (COILIX-00012).
- Wiring errors are reported at startup, not at compile time ([ADR 0002](docs/adr/0002-runtime-reflection-and-java-annotations.md)).
- Only constructor injection is supported, and dependency cycles are startup errors ([ADR 0004](docs/adr/0004-immutable-context-and-constructor-injection.md)).

## 1.0

Version 1.0 freezes the public API under [Semantic Versioning](docs/architecture/versioning.md). It requires the features above, at least one feature module (`web` or `data`), and feedback from real applications on the `0.x` releases.
