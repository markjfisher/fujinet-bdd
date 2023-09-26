package appfile

import appfile.AtariBinary
import org.junit.jupiter.api.Test
import util.resourceStream

class AtariBinaryTest {
    @Test
    fun `can read defender bat shit xex`() {
        val stream = resourceStream("/Defender.xex")
        val xex = AtariBinary(stream.readAllBytes())
        xex.dump()
    }
}