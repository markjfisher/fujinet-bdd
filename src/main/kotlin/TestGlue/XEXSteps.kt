package TestGlue

import appfile.AtariBinary
import appfile.DataSection
import com.loomcom.symon.machines.Machine
import cucumber.api.Scenario
import cucumber.api.java.Before
import java.nio.file.Paths
import kotlin.io.path.readBytes

class XEXSteps: StepLoader {

    @Before
    fun beforeHook(s: Scenario) {
        xexSteps = this
        scenario = s
    }

    override fun extension() = "xex"

    override fun loadApp(file: String) {
        val cwd = Paths.get(".")
        val machine = Glue.getMachine()

        // load all the DataSections of the binary into memory into their respective load locations
        val atariBinary = AtariBinary(cwd.resolve(file).readBytes())
        // abFile.dump()
        copyToMachine(atariBinary, machine)
        machine.cpu.programCounter = atariBinary.runAddress
    }

    private fun copyToMachine(atariBinary: AtariBinary, machine: Machine) {
        atariBinary.sections.filterIsInstance<DataSection>().forEach { ds ->
            ds.data.forEachIndexed { i, b ->
                machine.bus.write(ds.startAddress + i, b.toUByte().toInt())
            }
        }
    }

    companion object {
        lateinit var xexSteps: XEXSteps
        lateinit var scenario: Scenario
    }

}