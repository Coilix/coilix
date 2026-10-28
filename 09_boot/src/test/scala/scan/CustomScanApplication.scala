package org.scaling.boot.scan

import org.scaling.annotation.ScalingBootApplication

@ScalingBootApplication(scanBasePackages = Array("org.scaling.boot.fixture", "org.scaling.boot.empty"))
object CustomScanApplication
