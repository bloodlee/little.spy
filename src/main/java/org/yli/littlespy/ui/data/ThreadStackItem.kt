package org.yli.littlespy.ui.data

import org.yli.littlespy.domain.FrameInfo
import org.yli.littlespy.domain.ThreadInfo

/**
 * Created by yli on 12/26/2016.
 */
class ThreadStackItem(val label: String, val hasVariables: Boolean = false, val frameInfo: FrameInfo? = null) {
    override fun toString(): String {
        return label
    }
}