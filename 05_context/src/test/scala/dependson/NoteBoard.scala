package dev.coilix.context
package dependson

import dev.coilix.annotation.Component
import dev.coilix.annotation.DependsOn

@Component
@DependsOn(Array("draftNote"))
final class NoteBoard
