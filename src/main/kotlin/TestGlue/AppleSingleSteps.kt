package TestGlue

import appfile.AppleSingle
import appfile.DataForkEntry
import com.loomcom.symon.machines.Machine
import cucumber.api.Scenario
import cucumber.api.java.Before
import java.nio.file.Paths
import kotlin.io.path.readBytes

class AppleSingleSteps: StepLoader {
    @Before
    fun beforeHook(s: Scenario) {
        a2s = this
        scenario = s
    }

    override fun loadApp(file: String) {
        val cwd = Paths.get(".")
        val machine = Glue.getMachine()

        val asFile = AppleSingle(cwd.resolve(file).readBytes())
        // asFile.dump()
        copyToMachine(asFile, machine)
        machine.cpu.programCounter = asFile.loadAddress
    }

    private fun copyToMachine(appleSingle: AppleSingle, machine: Machine) {
        appleSingle.entries.filterIsInstance<DataForkEntry>().forEach { entry ->
            entry.data.forEachIndexed { i, b ->
                machine.bus.write(appleSingle.loadAddress + i, b.toUByte().toInt())
            }
        }
    }

    override fun extension() = "as"

    companion object {
        lateinit var a2s: AppleSingleSteps
        lateinit var scenario: Scenario
    }
}