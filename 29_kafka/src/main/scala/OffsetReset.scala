package dev.coilix.kafka

import dev.coilix.annotation.EnumName
import dev.coilix.annotation.Enumerated

/** Where a consumer group without a committed offset, or with an offset removed by retention, starts reading a partition. */
@Enumerated(ignoreCase = true)
enum OffsetReset(val kafkaName: String) {

  /** From the oldest record still kept, so nothing is skipped. */
  @EnumName("earliest") case Earliest extends OffsetReset("earliest")

  /** From the records published after the consumer joined. */
  @EnumName("latest") case Latest extends OffsetReset("latest")

  /** Nowhere: the consumer fails. */
  @EnumName("none") case Fail extends OffsetReset("none")
}
