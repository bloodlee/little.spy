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

/**
 * Created by yli on 5/22/2016.
 */
class Utilities {
    companion object {
        val LOGGER = LoggerFactory.getLogger(Utilities::class.java)
    }
}

fun Utilities.Companion.dumpHeap(folderPath: String, filePrefix: String) {
    val platformMBeanServer = ManagementFactory.getPlatformMBeanServer()
    val bean = ManagementFactory.newPlatformMXBeanProxy(platformMBeanServer,
            "com.sun.management:type=HotSpotDiagnostic",
            HotSpotDiagnosticMXBean::class.java)

    val heapFile = File(folderPath, filePrefix + "_heap.hprof")
    LOGGER.debug("heap file is dump to ${heapFile.absoluteFile}")
    bean.dumpHeap(heapFile.absolutePath, true)
}

fun Utilities.Companion.dumpStack(vm: VirtualMachine?, folderPath: String, filePrefix: String) {
    val stackMemory = StackMemory()
    vm!!.allThreads().forEach { th -> th.suspend() }

    LOGGER.debug("Threads count ${vm!!.allThreads().size}")
    for (aThread in vm!!.allThreads()) {
        LOGGER.debug("Thread ${aThread.name()} is ${aThread.isSuspended}.")

        val thread = ThreadInfo(aThread.name())

        try {
            LOGGER.debug("frame count ${aThread.frames().size}")
            for (aFrame in aThread.frames()) {
                val location = aFrame.location()
                val frame = FrameInfo(location.sourcePath(), location.method().name(), location.lineNumber())

                LOGGER.debug("var count ${frame.variables.size}")
                for (aVar in aFrame.visibleVariables()) {
                    val variable = VariableInfo(aVar.name(), aVar.typeName(), aFrame.getValue(aVar).toString())

                    frame.variables.add(variable)
                }

                thread.frames.add(frame)
            }
        } catch (e: AbsentInformationException) {
            continue
        }

        stackMemory.threads.add(thread)
    }

    vm!!.allThreads().forEach { th -> th.resume() }

    val heapFile = File(folderPath, filePrefix + "_stack.json")
    LOGGER.debug("stack file is dump to ${heapFile.absoluteFile}")
    FileUtils.writeStringToFile(heapFile, Gson().toJson(stackMemory))
}