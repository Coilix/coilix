package dev.coilix.demo
package vehicle

import dev.coilix.annotation.EnumName
import dev.coilix.annotation.Enumerated

/** What a diagnostic tool may do with a control module. The configuration writes the names in kebab case. */
@Enumerated(ignoreCase = true)
enum Action {

  /** Reads the stored fault codes. */
  @EnumName("read-faults") case ReadFaults

  /** Reads live sensor values. */
  @EnumName("read-live-data") case ReadLiveData

  /** Runs an actuator test, such as the ABS pump test. */
  @EnumName("run-test") case RunTest

  /** Resets learned values after a repair. */
  case Adapt

  /** Changes the coding of the module, such as programming a new key. */
  case Code
}
