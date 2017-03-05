
import com.ddiehl.android.reversi.utils.Utils
import junit.framework.Assert.assertEquals
import org.junit.Test

class StringByteArrayConversionTests {

    @Test
    fun stringToByteArrayConversion() {
        val string = "0123456789"

        val byteArray = string.toByteArray()

        assertEquals(10, byteArray.size)

        for (i in string.indices) {
            assertEquals(string[i], byteArray[i].toChar())
        }
    }

    @Test
    fun byteArrayToStringConversion() {
        val arrayLength = 10
        val byteArray = ByteArray(arrayLength, Int::toByte)

        val string = Utils.byteArrayToString(byteArray)

        assertEquals(arrayLength, string.length)

        for (i in byteArray.indices) {
            assertEquals(byteArray[i], string[i].toString().toByte())
        }
    }
}
