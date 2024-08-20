package com.l30n4rd.intrack.utils

import android.hardware.SensorManager
import android.opengl.Matrix
import com.l30n4rd.intrack.model.ApriltagDetection
import com.l30n4rd.intrack.model.ApriltagPose
import kotlin.math.cos
import kotlin.math.sin

class PositionHelper {
    companion object {

        /**
         * Calculates the rotation matrix from a rotation vector.
         * @param rotationVector 3x1 vector containing the rotations.
         * For example from a Sensor.TYPE_GAME_ROTATION_VECTOR.
         * @return 4x4 rotation matrix in column-major order.
         */
        fun getRotationMatrixFromRotationVector(rotationVector: FloatArray): FloatArray {
            // Convert rotation vector to rotation matrix
            val rowMajorRotationMatrix = FloatArray(16)
            SensorManager.getRotationMatrixFromVector(
                rowMajorRotationMatrix,
                rotationVector
            )
            // Transpose the matrix to align with OpenGL ES column-major matrices
            val rotationMatrix = FloatArray(16)
            Matrix.transposeM(rotationMatrix, 0, rowMajorRotationMatrix, 0)
            return rotationMatrix
        }

        /**
         * Calculates the rotation matrix for the rotation about the z-axis by the angle azimuth.
         * @param azimuth The angle of the rotatoin about the z-axis in radian
         * @return A FloatArray containing the 4x4 column-major rotation matrix.
         */
        fun getRotationMatrixAboutZAxis(azimuth: Float): FloatArray {
            val cosAlpha = cos(azimuth)
            val sinAlpha = sin(azimuth)
            return floatArrayOf(
                cosAlpha,  sinAlpha, 0.0f, 0.0f,
                -sinAlpha, cosAlpha, 0.0f, 0.0f,
                0.0f,      0.0f,     1.0f, 0.0f,
                0.0f,      0.0f,     0.0f, 1.0f
            )
        }

        /**
         * Calculates azimuth, angle of rotation about the z axis.
         * @param matrix 3x3 or 4x4 column-major rotation matrix.
         * @return Azimuth, angle of rotation about the z axis. The range of values is -π to π.
         * @throws IllegalArgumentException if the size of the rotation matrix is invalid.
         */
        fun getAzimuthFromRotationMatrix(matrix: FloatArray): Float {
            require(matrix.size == 9 || matrix.size == 16) { "Rotation matrix must have 9 or 16 elements" }

            val rotationMatrix = matrix.clone()

            if (rotationMatrix.size == 16) {
                rotationMatrix[12] = 0.0f
                rotationMatrix[13] = 0.0f
                rotationMatrix[14] = 0.0f
                rotationMatrix[15] = 1.0f
            }

            /*
             * SensorManager.getOrientation expects a row-major matrix so normally we would transpose
             * the rotation matrix, but the returned azimuth is defined as this:
             * When facing north, this angle is 0, when facing south, this angle is π.
             * Likewise, when facing east, this angle is π/2, and when facing west, this angle is -π/2.
             *
             * But we would like that the angle increases when rotating counter clockwise, so we do
             * not transpose the rotation matrix, effectively describing a rotation in the opposite
             * direction.
             */
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            return orientation[0] // Rotation about the z-axis
        }

        /**
         * Combines the 3x3 rotation matrix and the 3x1 translation vector of an AprilTag detection
         * into a 4x4 transformation matrix.
         *
         * @param tag An AprilTag detection
         * @return A FloatArray containing the combined 4x4 column-major transformation matrix.
         */
        fun getTransformationMatrixFromTag(tag: ApriltagDetection): FloatArray {
            return combineTransformation(tag.pose.r, tag.pose.t)
        }

        /**
         * Combines a 3x3 rotation matrix and a 3x1 translation vector into a 4x4 transformation matrix.
         *
         * The rotation matrix is assumed to be in column-major order.
         *
         * @param rotationMatrix A List<Double> representing a 3x3 rotation matrix in column-major order.
         * @param translationVector A List<Double> representing a 3x1 translation vector.
         * @return A FloatArray representing the combined 4x4 column-major transformation matrix.
         * @throws IllegalArgumentException if the sizes of the rotation matrix or translation vector are invalid.
         */
        private fun combineTransformation(rotationMatrix: List<Double>, translationVector: List<Double>): FloatArray {
            require(rotationMatrix.size == 9) { "Rotation matrix must have 9 elements" }
            require(translationVector.size == 3) { "Translation vector must have 3 elements" }

            val transformationMatrix = FloatArray(16)

            // Copy rotation matrix values
            for (column in 0 .. 2) {
                for (row in 0 .. 2) {
                    // The rotation matrix of the AprilTag pose is in row-major order
                    transformationMatrix[column * 4 + row] = rotationMatrix[row * 3 + column].toFloat()
                }
            }

            // Copy translation vector to transformation matrix
            transformationMatrix[12] = translationVector[0].toFloat()
            transformationMatrix[13] = translationVector[1].toFloat()
            transformationMatrix[14] = translationVector[2].toFloat()

            // Set the rest of the transformation matrix elements
            transformationMatrix[15] = 1.0f

            return transformationMatrix
        }

        /**
         * Constructs an ApriltagPose from a transformation matrix.
         *
         * @param transformationMatrix 4x4 column-major transformation matrix.
         * @return A ApriltagPose containing the pose described by the transformation matrix.
         */
        fun getApriltagPoseFromMatrix(transformationMatrix: FloatArray): ApriltagPose {
            val r = DoubleArray(9)
            val t = DoubleArray(3)

            // Copy rotation matrix values
            for (column in 0..2) {
                for (row in 0..2) {
                    // The rotation matrix of the AprilTag pose is in row-major order
                    r[column * 3 + row] = transformationMatrix[row * 4 + column].toDouble()
                }
            }

            // Copy translation vector to transformation matrix
            t[0] = transformationMatrix[12].toDouble()
            t[1] = transformationMatrix[13].toDouble()
            t[2] = transformationMatrix[14].toDouble()

            return ApriltagPose(
                r = r.toList(),
                t = t.toList()
            )
        }

        /**
         * Generate a string in the format for wolfram "{ {1, 2, 3}, {4, 5, 6}, {7, 8, 9} }"
         * @param matrix containing the 3x3 rotation matrix.
         * @return String in the format "{ {1, 2, 3}, {4, 5, 6}, {7, 8, 9} }"
         */
        fun getWolfram3x3RotationString(matrix: List<Double>): String {
            var str = "{ {"
            for (column in 0..2) {
                for (row in 0..2) {
                    str += "${matrix[column * 3 + row]}, "
                }
                str = str.dropLast(2) // Remove the extra comma and space
                str += "}, {"
            }
            str = str.dropLast(3) // Remove the extra comma, space, and opening curly brace
            str += " }"

            return str
        }

        /**
         * Generate a string in the format for python.
         * @param matrix containing the 4x4 transformation matrix.
         * @return String in the following format:
         *```
         *     [0, 4,  8, 12],
         *     [1, 5,  9, 13],
         *     [2, 6, 10, 14],
         *     [3, 7, 11, 15]
         * ```
         */
        fun getPython4x4TransformationString(matrix: FloatArray): String {
            var str = "\t["
            for (row in 0..3) {
                for (column in 0..3) {
                    str += "${matrix[column * 4 + row]}, "
                }
                str = str.dropLast(2) // Remove the extra comma and space
                str += "],\n\t["
            }
            str = str.dropLast(4) // Remove the extra comma, new line, tab, and opening curly brace

            return str
        }

        /**
         * Generate a string in the format for json.
         * @param matrix containing the 4x4 transformation matrix.
         * @return String in the following format:
         *```
         *     0,
         *     1,
         *     2,
         *     3,
         *     4,
         *     5,
         *     6,
         *     7,
         *     8
         *     9,
         *     10,
         *     11,
         *     12,
         *     13,
         *     14,
         *     15
         * ```
         */
        fun getJson4x4TransformationString(matrix: FloatArray): String {
            var str = ""
            for (i in 0..15) {
                str += "${matrix[i]},\n"
            }
            str = str.dropLast(2) // Remove the extra comma and new line
            return str
        }

    }
}
