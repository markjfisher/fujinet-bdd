@file:Suppress("JoinDeclarationAndAssignment")

package appfile

/*
 https://nulib.com/library/AppleSingle_AppleDouble.pdf

 Table 2-1 AppleSingle file header

    Field                   Length
    Magic number            4 bytes
    Version number          4 bytes
    Filler                  16 bytes
    Number of entries       2 bytes

 Entry descriptor for each entry:

    Entry ID                4 bytes
    Offset                  4 bytes
    Length                  4 bytes

 Byte ordering in the file header fields follows MC68000 conventions, most significant byte first
 Apple has defined the magic number for the AppleSingle format as $00051600

 Apple has defined a set of entry IDs and their values as follows:

  * Data Fork                1   Data fork
    Resource Fork            2   Resource fork
    Real Name                3   File’s name as created on home file system
    Comment                  4   Standard Macintosh comment
    Icon, B&W                5   Standard Macintosh black and white icon
    Icon, Color              6   Macintosh color icon
    File Dates Info          8   File creation date, modification date, and so on
    Finder Info              9   Standard Macintosh Finder information
    Macintosh File Info     10   Macintosh file information, attributes, and so on
  * ProDOS File Info        11   ProDOS file information, attributes, and so on
    MS-DOS File Info        12   MS-DOS file information, attributes, and so on
    Short Name              13   AFP short name
    AFP File Info           14   AFP file information, attributes, and so on
    Directory ID            15   AFP directory ID

    * Implemented below
*/

data class AppleSingle(val bytes: ByteArray) {
    private var magicNumber: String
    private var versionNumber: String
    // DEFAULT for cc65, see https://cc65.github.io/doc/apple2.html
    val loadAddress = 0x803
    val entries = mutableListOf<ASEntry>()
    init {
        // validate the magic number is correct
        magicNumber = bytes.sliceArray(0..3).joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
        if (magicNumber != "00051600") {
            throw Exception("Magic Number unrecognised: 0x$magicNumber")
        }

        // validate the version
        versionNumber = bytes.sliceArray(4..7).joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
        if (versionNumber != "00020000") {
            throw Exception("Version Number unrecognised: 0x$versionNumber")
        }

        // validate we then get 16 x 00 from 8-23
        bytes.sliceArray(8 .. 23).forEachIndexed { i, b ->
            if (b.toInt() != 0) throw Exception("Zero Filler incorrect at byte $i, found: $b")
        }

        // number of entries to read
        val numEntries = bytes[24].toUInt().toInt() * 256 + bytes[25].toUInt().toInt()

        val headerBytes = 26
        (0 until numEntries).forEach { n ->
            // get the entry ID
            val entryStartOffset = headerBytes + n * 12
            val entry = when(val entryID = getNumberFrom(entryStartOffset, 4)) {
                1u  -> createDataForkEntry(entryStartOffset)
                11u -> createProDosEntry(entryStartOffset)
                else -> throw Exception("Unknown entryID: $entryID")
            }
            entries.add(entry)
        }
    }

    private fun createDataForkEntry(entryStartOffset: Int): DataForkEntry {
        val offset = getNumberFrom(entryStartOffset + 4, 4)
        val len = getNumberFrom(entryStartOffset + 8, 4)
        return DataForkEntry(bytes.sliceArray(offset.toInt() until (offset + len).toInt()))
    }

    private fun createProDosEntry(entryStartOffset: Int): ProDosEntry {
        val offset = getNumberFrom(entryStartOffset + 4, 4)
        val len = getNumberFrom(entryStartOffset + 8, 4)
        if (len != 8u) throw Exception("Incorrect entry length for ProDos type, expected 8, found: ${len.toInt()}")

        val access = getNumberFrom(offset.toInt(), 2)
        val fileType = getNumberFrom(offset.toInt() + 2, 2)
        val auxiliaryType = getNumberFrom(offset.toInt() + 4, 4)
        return ProDosEntry(access.toUShort(), fileType.toUShort(), auxiliaryType)
    }

    private fun getNumberFrom(start: Int, length: Int): UInt {
        return bytes.sliceArray(start until start + length).fold(0u) { acc, b ->
            acc * 256u + b.toUInt()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppleSingle

        if (!bytes.contentEquals(other.bytes)) return false
        if (magicNumber != other.magicNumber) return false
        if (versionNumber != other.versionNumber) return false
        if (entries != other.entries) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + magicNumber.hashCode()
        result = 31 * result + versionNumber.hashCode()
        result = 31 * result + entries.hashCode()
        return result
    }

    override fun toString(): String {
        val dataSize = entries.filterIsInstance<DataForkEntry>().firstOrNull()?.data?.size ?: 0
        return "AppleSingle[entries: ${entries.count()}, dataLen: $dataSize]"
    }
}

sealed class ASEntry

data class DataForkEntry(
    val data: ByteArray
): ASEntry() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataForkEntry

        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}

data class ProDosEntry(
    val access: UShort,     // 16 bits
    val fileType: UShort,   // 16 bits
    val auxiliaryType: UInt // 32 bits
): ASEntry()