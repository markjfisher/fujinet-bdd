package TestGlue

interface StepLoader {
    fun loadApp(file: String)
    fun extension(): String
}