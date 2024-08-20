package com.l30n4rd.intrack.utils

import android.content.Context
import com.l30n4rd.intrack.model.AccelerationSample
import com.l30n4rd.intrack.model.PositionSample
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

class FileHelper @Inject constructor (
    @ApplicationContext private val context: Context
) {
    @Throws(IOException::class)
    fun saveStringToFile(str: String, fileName: String): File {
        val file = File(context.filesDir, fileName)
        val fileWriter = FileWriter(file)
        fileWriter.write(str)
        fileWriter.close()
        return file
    }

    companion object {
        fun convertAccelerationDataToCsv(sampleList: List<AccelerationSample>): String {
            // Convert data to CSV format
            val csvHeader = "Timestamp,X,Y,Z\n"
            val csvData = sampleList.joinToString("\n") { sample ->
                "${sample.timestamp}," +
                        "${"%.9e".format(Locale.US, sample.data[0])}," +
                        "${"%.9e".format(Locale.US, sample.data[1])}," +
                        "%.9e".format(Locale.US, sample.data[2])
            }
            return csvHeader + csvData
        }

        /**
         * Encodes a list of PositionSamples into a JSON string.
         *
         * @throws SerializationException in case of any encoding-specific error
         * @throws IllegalArgumentException if the encoded input does not comply format's specification
         */
        fun convertPositionDataToJson(sampleList: List<PositionSample>): String {
            return Json.encodeToString(sampleList)
        }
    }

}
