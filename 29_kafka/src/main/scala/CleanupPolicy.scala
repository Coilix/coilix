package dev.coilix.kafka

/** What Kafka does with old records of a topic. */
enum CleanupPolicy(val kafkaName: String) {

  /** Removes records older than the retention. */
  case Delete extends CleanupPolicy("delete")

  /** Keeps the latest record of each key. */
  case Compact extends CleanupPolicy("compact")

  /** Keeps the latest record of each key and removes records older than the retention. */
  case CompactAndDelete extends CleanupPolicy("compact,delete")
}
