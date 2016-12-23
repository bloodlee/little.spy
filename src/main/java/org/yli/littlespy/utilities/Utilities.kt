package org.yli.littlespy.utilities

import com.google.gson.Gson
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.VirtualMachine
import com.sun.management.HotSpotDiagnosticMXBean
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.yli.littlespy.domain.FrameInfo
import org.yli.littlespy.domain.StackMemory
import org.yli.littlespy.domain.ThreadInfo
import org.yli.littlespy.domain.VariableInfo
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.charset.Charset

/**
 * {@link Utilities} to help to dumy the heap and the stack.
 *
 * Created by yli on 5/22/2016.
 */
class Utilities {
    companion object {
        val LOGGER = LoggerFactory.getLogger(Utilities::class.java)
    }
}

/**
 * Dump heap. <br\>
 *
 * It depends on Hotspot VM's API.
 *
 * @param folderPath the folder's path.
 * @param filePrefix the file's prefix.
 */
fun Utilities.Companion.dumpHeap(folderPath: String, filePrefix: String) {
    val platformMBeanServer = ManagementFactory.getPlatformMBeanServer()
    val bean = ManagementFactory.newPlatformMXBeanProxy(platformMBeanServer,
            "com.sun.management:type=HotSpotDiagnostic",
            HotSpotDiagnosticMXBean::class.java)

    val heapFile = File(folderPath, filePrefix + "_heap.hprof")
    LOGGER.debug("heap file is dump to ${heapFile.absoluteFile}")
    bean.dumpHeap(heapFile.absolutePath, true)
}

/**
 * Dump the stack. <br/>
 *
 * @param vm the virtual machine.
 * @param folderPath the folder's path.
 * @param filePrefix the file's prefix.
 *
 * @see VirtualMachine
 */
fun Utilities.Companion.dumpStack(vm: VirtualMachine?, folderPath: String, filePrefix: String) {
    val stackMemory = StackMemory()

    LOGGER.debug("Threads count ${vm!!.allThreads().size}")
    for (aThread in vm!!.allThreads()) {
        LOGGER.debug("Thread ${aThread.name()} is ${aThread.isSuspended}.")

        if (!aThread.isSuspended) {
            continue
        }

        val thread = ThreadInfo(aThread.name())

        try {
            LOGGER.debug("frame count ${aThread.frames().size}")
            for (aFrame in aThread.frames()) {
                val location = aFrame.location()

                var frame: FrameInfo? = null
                try {
                    frame = FrameInfo(location.sourcePath(), location.method().name(), location.lineNumber())

                    LOGGER.debug("frame ${frame}")

                    if (aFrame.visibleVariables() == null) {
                        continue
                    }

                    LOGGER.debug("var count ${aFrame.visibleVariables().size}")
                    for (aVar in aFrame.visibleVariables()) {
                        val varValue = aFrame.getValue(aVar)

                        val variable = VariableInfo(aVar.name(), aVar.typeName(),
                                varValue?.toString() ?: "null")

                        frame.variables.add(variable)
                    }
                } catch (e: AbsentInformationException) {
                    LOGGER.debug("skip this frame and go the next one.")
                }

                if (frame != null) {
                    thread.frames.add(frame)
                }
            }
        } catch (e: AbsentInformationException) {
            LOGGER.debug(e.message, e)
            continue
        }

        stackMemory.threads.add(thread)
    }

    val heapFile = File(folderPath, filePrefix + "_stack.json")
    LOGGER.debug("stack file is dump to ${heapFile.absoluteFile}")
    FileUtils.writeStringToFile(heapFile, Gson().toJson(stackMemory), Charset.defaultCharset())
}