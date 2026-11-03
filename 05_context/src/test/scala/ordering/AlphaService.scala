package dev.coilix.context.ordering

import dev.coilix.annotation.Service

@Service
final class AlphaService(val audit: Option[Audit])
