package dev.coilix.warts

/** [[dev.coilix.warts.ClassParameterCheck]] that checks every file, so that it sees the code of the tests. */
object EveryFileClassParameterCheck extends ClassParameterCheck(_ => false)
