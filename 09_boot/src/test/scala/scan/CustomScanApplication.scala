package dev.coilix.boot.scan

import dev.coilix.annotation.CoilixApplication

@CoilixApplication(scanBasePackages = Array("dev.coilix.boot.fixture", "dev.coilix.boot.empty"))
object CustomScanApplication
