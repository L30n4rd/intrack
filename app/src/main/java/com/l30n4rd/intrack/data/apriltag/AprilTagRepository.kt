package com.l30n4rd.intrack.data.apriltag

import android.os.SystemClock
import com.l30n4rd.intrack.model.ApriltagDetection
import com.l30n4rd.intrack.model.TagWithGlobalPose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AprilTagRepository @Inject constructor(
) {
    // In memory data source for tags TODO: consider replacing it with a persistant data source
    private val tags: MutableList<TagWithGlobalPose> = mutableListOf()

    /**
     * Tag with a known global pose. It is used to set the initial position of the navigator.
     */
    private val anchorTag: TagWithGlobalPose

    private val _detectedTags: MutableStateFlow<List<ApriltagDetection>> = MutableStateFlow(
        emptyList()
    )
    val detectedTags: StateFlow<List<ApriltagDetection>> = _detectedTags.asStateFlow()

    init {
        // Add anchor tag with known global pose
        anchorTag = TagWithGlobalPose(
            tag = ApriltagDetection(id = 0),
            globalPose = floatArrayOf(
                 0.58283234f, 0.81259245f, 0.0f,  0.0f,
                -0.81259245f, 0.58283234f, 0.0f,  0.0f,
                 0.0f,        0.0f,        1.0f,  0.0f,
                26.832071f,  18.586119f,   3.0f,  1.0f

            ),
            lastUpdateTimestampNanos = SystemClock.elapsedRealtimeNanos()
        )
        val fixedTags = listOf(
            anchorTag,
            TagWithGlobalPose(
                tag = ApriltagDetection(id = 1),
                globalPose = floatArrayOf(
                     0.6422526f,  0.766493f,  0.0f,  0.0f,
                    -0.766493f,   0.6422526f, 0.0f,  0.0f,
                     0.0f,        0.0f,       1.0f,  0.0f,
                     23.832926f, 24.57647f,   3.0f,  1.0f
                ),
                lastUpdateTimestampNanos = SystemClock.elapsedRealtimeNanos()
            ),
            TagWithGlobalPose(
                tag = ApriltagDetection(id = 2),
                globalPose = floatArrayOf(
                    0.7169106f, 0.69716513f, 0.0f,  0.0f,
                    -0.69716513f, 0.7169106f, 0.0f,  0.0f,
                    0.0f,        0.0f,        1.0f,  0.0f,
                    19.7f,  31.6f,   3.0f,  1.0f
                ),
                lastUpdateTimestampNanos = SystemClock.elapsedRealtimeNanos()
            ),
            TagWithGlobalPose(
                tag = ApriltagDetection(id = 3),
                globalPose = floatArrayOf(
                    0.5120429f, 0.8589599f, 0.0f,  0.0f,
                    -0.8589599f, 0.5120429f, 0.0f,  0.0f,
                    0.0f,        0.0f,        1.0f,  0.0f,
                    33.4f,  17.0f,   3.0f,  1.0f
                ),
                lastUpdateTimestampNanos = SystemClock.elapsedRealtimeNanos()
            ),
            TagWithGlobalPose(
                tag = ApriltagDetection(id = 4),
                globalPose = floatArrayOf(
                    0.38187703f, 0.9242131f, 0.0f,  0.0f,
                    -0.9242131f, 0.38187703f, 0.0f,  0.0f,
                    0.0f,        0.0f,        1.0f,  0.0f,
                    43.1f, 11.6f,   3.0f,  1.0f
                ),
                lastUpdateTimestampNanos = SystemClock.elapsedRealtimeNanos()
            )
        )

        for (tag in fixedTags) {
            tags.add(tag)
        }
    }

    /**
     * This method is called from AprilTagHandler when it detects tags.
     */
    fun onTagsDetected(tags: List<ApriltagDetection>) {
        _detectedTags.value = tags
    }

    /**
     * Add a tag to the repository or update an existing one.
     */
    fun addOrUpdateTag(tag: TagWithGlobalPose) {
        tag.lastUpdateTimestampNanos = SystemClock.elapsedRealtimeNanos()
        val existingTagIndex = tags.indexOfFirst { it.tag.id == tag.tag.id }
        if (existingTagIndex != -1) {
            // Update existing tag
            tags[existingTagIndex] = tag
        } else {
            // Add new tag
            tags.add(tag)
        }
    }

    /**
     * Retrieves the TagWithGlobalPose object corresponding to the provided tag ID from the 'tags' list.
     *
     * @param tagId The ID of the tag to retrieve.
     * @return The TagWithGlobalPose object corresponding to the provided tag ID, or null if no such tag exists.
     */
    fun getTag(tagId: Int): TagWithGlobalPose? {
        val existingTagIndex = tags.indexOfFirst { it.tag.id == tagId }
        if (existingTagIndex == -1) {
            return null
        }
        return tags[existingTagIndex]
    }

    fun getAnchorTag(): TagWithGlobalPose {
        return anchorTag
    }

    fun getAllTags(): List<TagWithGlobalPose> {
        return tags.toList()
    }

    companion object {
        private const val TAG = "AprilTagRepository"
    }

}
