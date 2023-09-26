package TestGlue

import cucumber.api.Scenario
import cucumber.api.java.Before
import cucumber.api.java.en.Given
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.readLines
import kotlin.io.path.writeText

class CA65Steps {
    private val compileFiles: MutableList<String> = mutableListOf()
    private val compilerOptions: MutableList<String> = mutableListOf()
    private var target: String = ""
    private var workDir: String = ""
    private var config: String = ""

    @Before
    fun beforeHook(s: Scenario) {
        ca65Glue = this
        scenario = s
    }

    /*
    Example input

al 000082 .__ZP_START__
al 003086 ._siov
al 003007 ._fn_io_init
al 00302E .setup_screen
al 003000 .start

     */

    @Given("^I convert vice-labels file \"([^\"]*)\" to acme labels file \"([^\"]*)\"$")
    @Throws(Exception::class)
    fun `i convert vice-labels to acme file`(viceLabs: String, acmeLabs: String) {
        val cwd = Paths.get(".")
        val viceFile = cwd.resolve(viceLabs)

        val lines = viceFile.readLines()
        val outputString = lines.fold("") { s, line ->
            val parts = line.split("\\s+".toRegex())
            // There's a bug currently in Glue.java that RHS must be trimmed
            if (parts[0] == "al") {
                s + "${parts[2].lowercase().substring(1)} =0x${parts[1].substring(2)}\n"
            } else {
                s
            }
        }
        val acmeFile = cwd.resolve(acmeLabs)
        acmeFile.writeText(outputString)
    }

    @Given("^I start compiling for (.*) in \"([^\"]*)\" with config \"([^\"]*)\"$")
    @Throws(Exception::class)
    fun `i start compiling for target T in W with config C`(t: String, w: String, c: String) {
        target = t
        compileFiles.clear()
        compilerOptions.clear()
        workDir = w
        config = c
    }

    @Given("^I add file for compiling \"([^\"]*)\"$")
    @Throws(Exception::class)
    fun `i add file for compiling`(fileToCompile: String) {
        compileFiles.add(fileToCompile.trim())
    }

    @Given("^I add compiler option \"([^\"]*)\"$")
    @Throws(Exception::class)
    fun `i add compiler option`(option: String) {
        compilerOptions.add(option.trim())
    }

    @Given("^I create and load atari application$")
    @Throws(Exception::class)
    fun `I create and load atari application`() {
        createAndLoadApplication(crt0App, XEXSteps.xexSteps)
    }

    @Given("^I create and load apple-single application$")
    @Throws(Exception::class)
    fun `I create and load apple-single application`() {
        createAndLoadApplication(crt0App, AppleSingleSteps.a2s)
    }

    @Given("^I create and load simple atari application$")
    @Throws(Exception::class)
    fun `I create and load simple atari application`() {
        createAndLoadApplication(simpleApp, XEXSteps.xexSteps)
    }

    private fun createAndLoadApplication(stubApp: String, loader: StepLoader) {
        val cwd = Paths.get(".")
        val wd = cwd.resolve(workDir)
        val crt0 = Files.createFile(wd.resolve("crt0-test.s"))
        crt0.writeText(stubApp)

        val options = compilerOptions.joinToString(" ")

        // compile each file
        val glue = Glue.getGlue()
        (compileFiles + "$workDir/crt0-test.s").forEach { f ->
            val justName = f.substringAfterLast('/').substringBeforeLast('.')
            val cmd = "cl65 -t $target -c --create-dep $workDir/${justName}.d $options -l $workDir/${justName}.lst -o $workDir/${justName}.o $f"
            println("running cl65 for $f with cmd: >$cmd<")
            glue.i_run_the_command_line(cmd)
        }
        // create the app
        val ext = loader.extension()
        var appCmd = "cl65 -t $target -vm --mapfile $workDir/app.map $options -l $workDir/app.lst -Ln $workDir/app.lbl -o $workDir/app.$ext -C $config $workDir/crt0-test.o "
        appCmd += compileFiles.joinToString(" ") { f ->
            val justName = f.substringAfterLast('/').substringBeforeLast('.')
            "$workDir/${justName}.o"
        }
        println("running cl65 for app with cmd: >$appCmd<")
        glue.i_run_the_command_line(appCmd)

        loader.loadApp("$workDir/app.$ext")
        `i convert vice-labels to acme file`("$workDir/app.lbl", "$workDir/app.al")
        glue.i_load_labels("$workDir/app.al")
    }

    companion object {
        lateinit var ca65Glue: CA65Steps
        lateinit var scenario: Scenario
    }

    // Test is required to specify a _main method to call.
    private val crt0App = """
                ; setup basic crt0 code for testing
                    .export _init
                    .import _main, initlib
                    
                    .import __MAIN_START__, __MAIN_SIZE__, __STACKSIZE__
                    .include "zeropage.inc"
                    .segment "STARTUP"
    
                _init:
                    ; setup stack pointer
                    ldx #${"$"}ff
                    txs
                    cld
                    
                    ; software stack starts on top of main, but works downwards, so needs to be at the end.
                    lda #<(__MAIN_START__ + __MAIN_SIZE__ + __STACKSIZE__)
                    sta sp
                    lda #>(__MAIN_START__ + __MAIN_SIZE__ + __STACKSIZE__)
                    sta sp+1

                    jsr _main
                    brk
    
            """.trimIndent()

    private val simpleApp = """
                ; just a simple start, test will call directly to target function
                    .export start
                .proc start
                    rts
                .endproc
            """.trimIndent()

}