# Coilix

[![CI](https://github.com/Coilix/coilix/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/Coilix/coilix/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
[![Scala](https://img.shields.io/badge/scala-3.9%2B-red.svg)](https://www.scala-lang.org/)
[![JDK](https://img.shields.io/badge/jdk-17%2B-orange.svg)](https://adoptium.net/)

Coilix is a dependency injection and application framework for Scala 3. Its API is modeled on the Spring Framework, so annotations such as `@Component`, `@Service` and `@Value` work the way Spring developers expect, while the implementation is written from scratch in idiomatic Scala: immutable registries, traits with replaceable default implementations and a compiler plugin for logging.

> Coilix is in early development (`0.x`). The API can change between minor versions.

## Example

```scala
import dev.coilix.annotation.{CoilixApplication, Component, Service, Slf4s}
import dev.coilix.boot.{ApplicationArguments, ApplicationRunner, Coilix}

trait UserService {
  def listUsers(): List[String]
}

@Service
final class FixedUserService extends UserService {
  override def listUsers(): List[String] = List("Alice", "Bob")
}

@Component
@Slf4s
final class Greeter(users: UserService) extends ApplicationRunner {
  override def run(arguments: ApplicationArguments): Unit =
    users.listUsers().foreach(user => log.info(s"Hello, $user"))
}

@CoilixApplication
object App {
  def main(args: Array[String]): Unit = {
    val _ = Coilix.run[App.type](args)
  }
}
```

`Coilix.run` scans the package of the application object, creates the beans, injects them through constructors, loads `application.yaml` (or `.conf`, `.properties`) from the classpath and calls every `ApplicationRunner`.

## Features

- constructor injection with `@Component`, `@Service`, `@Repository`, `@Qualifier`, `@Primary` and `@Order`, including `Option[T]` and `List[T]` dependencies,
- `@Configuration` classes with `@Bean` methods,
- configuration from HOCON, YAML and properties files, command-line options and system properties, bound with `@Value` and `@ConfigurationProperties` to scalars, collections, options and case classes,
- every framework component can be replaced in configuration with a `class` key under its `coilix.*` path,
- `@Slf4s` compiler plugin that adds an SLF4J `log` field, with an IntelliJ IDEA plugin that makes the field visible in the editor.

## Modules

| Artifact | Contents |
|---|---|
| `coilix-common` | shared types |
| `coilix-annotations` | runtime annotations (Java) |
| `coilix-slf4s-plugin` | `@Slf4s` compiler plugin |
| `coilix-config` | configuration loading and component overrides |
| `coilix-context` | the IoC container |
| `coilix-boot` | application startup |

`data`, `web` and `security` are planned as optional modules.

## Installation

Coilix has not been released yet. The first release will be published to Maven Central under the `dev.coilix` group:

```scala
libraryDependencies += "dev.coilix" %% "coilix-boot" % "<version>"
```

Requirements: Scala 3.9 or newer and JDK 17 or newer.

## Building

```sh
sbt testFull      # all tests
sbt testCoverage  # tests with a coverage report
sbt demo/run      # the demo application
sbt doc           # API documentation
```

## Roadmap

Planned features and their order are in [ROADMAP.md](ROADMAP.md).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Security issues: see [SECURITY.md](SECURITY.md). This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md).

## License

Coilix is licensed under the [Apache License, Version 2.0](LICENSE).

## Trademarks

Coilix is an independent project. It is not affiliated with, sponsored by or endorsed by Broadcom Inc. or the Spring team. Spring is a trademark of Broadcom Inc. and/or its subsidiaries.
