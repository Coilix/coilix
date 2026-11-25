package dev.coilix.warts

import java.io.File

/** [[dev.coilix.warts.ClassParameterCheck]] for the main sources of Coilix: files under `src/test` are left out. */
object ClassParameterInApi
    extends ClassParameterCheck(path => path.replace(File.separatorChar, '/').contains("/src/test/"))
