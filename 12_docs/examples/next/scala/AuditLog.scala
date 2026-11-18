package com.example

import dev.coilix.annotation.Component
import dev.coilix.annotation.ConditionalOnProperty

@Component
@ConditionalOnProperty(name = "shop.audit.enabled", havingValue = "true")
final class AuditLog
