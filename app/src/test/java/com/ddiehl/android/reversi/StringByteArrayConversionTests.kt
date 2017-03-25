package com.ddiehl.android.reversi
import junit.framework.Assert.assertEquals
import org.junit.Test

class StringByteArrayConversionTests {

    @Test
    fun stringToByteArrayConversion() {
        val string = "0123456789"

        val byteArray = string.toByteArray()

        assertEquals(10, byteArray.size)

        byteArray.indices.forEach { i ->
            assertEquals(string[i], byteArray[i].toChar())
        }
    }

    @Test
    fun byteArrayToStringConversion() {
        val arrayLength = 10
        val byteArray = ByteArray(arrayLength, Int::toByte)

        val string = byteArrayToString(byteArray)

        assertEquals(arrayLength, string.length)

        byteArray.indices.forEach { i ->
            assertEquals(byteArray[i], string[i].toString().toByte())
        }
    }
}
