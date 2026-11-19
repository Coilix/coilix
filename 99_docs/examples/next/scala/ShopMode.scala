package com.example

import dev.coilix.annotation.EnumName
import dev.coilix.annotation.Enumerated

@Enumerated(ignoreCase = true)
enum ShopMode {
  case Open, Closed
  @EnumName(value = "read-only", aliases = Array("browse-only")) case ReadOnly
}
