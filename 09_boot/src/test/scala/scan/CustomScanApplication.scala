package dev.coilix.boot
package scan

import dev.coilix.annotation.CoilixApplication

@CoilixApplication(scanBasePackages = Array("dev.coilix.boot.fixture", "dev.coilix.boot.empty"))
object CustomScanApplication
