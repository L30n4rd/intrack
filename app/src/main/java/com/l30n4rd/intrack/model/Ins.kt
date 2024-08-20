package com.l30n4rd.intrack.model

/**
 * The InsLocation contains a single transformation matrix, because the location is
 * always in reference to a known absolute location.
 * <p>
 * To get the current absolute location you multiply the last known position as 4 x 1 column vector
 * with the transformation matrix of this InsLocation.
 */
data class InsPosition(
    val transformationMatrix: FloatArray, // 4 x 4 column-vector matrix
    val timestamp: Long = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InsPosition

        return transformationMatrix.contentEquals(other.transformationMatrix)
    }

    override fun hashCode(): Int {
        return transformationMatrix.contentHashCode()
    }
}

data class AccelerationSample(
    val timestamp: Long,
    val data: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AccelerationSample

        if (timestamp != other.timestamp) return false
        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
