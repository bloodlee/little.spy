package org.yli.littlespy

import com.google.common.collect.Lists
import com.sun.jdi.*
import com.sun.jdi.connect.AttachingConnector
import com.sun.jdi.event.*
import com.sun.jdi.request.ClassPrepareRequest
import com.sun.jdi.request.EventRequest
import com.sun.jdi.request.ExceptionRequest
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.slf4j.LoggerFactory
import org.yli.littlespy.exceptions.LittleSpyException
import org.yli.littlespy.utilities.Utilities
import org.yli.littlespy.utilities.dumpHeap
import org.yli.littlespy.utilities.dumpStack
import java.util.*

/**
 * LittleSpy is used to dump the stack and memory status to files.
 *
 * @param debugPort JDPA debugging port.
 * @param config the configuration of LittleSpy.
 *
 * @see LittleSpyConfig
 *
 * Created by yli on 5/21/2016.
 */
class LittleSpy(val debugPort : Int, val config : LittleSpyConfig = LittleSpyConfig()) {

    val LOGGER = LoggerFactory.getLogger(javaClass)

    val host = "127.0.0.1"

    val TRANSPORT = "dt_socket"

    var attachConnector: AttachingConnector? = null

    var vm: VirtualMachine? = null

    var listeningThread : Thread? = null

    val forPattern = DateTimeFormat.forPattern("yyyy_MM_dd_H_m_s_S")

    val exceptionRequestMap : MutableMap<String, ExceptionRequest> = mutableMapOf()

    init {
        LOGGER.debug("LittleSpy was just created.")
    }

    /**
     * Start the spy.
     */
    @Throws(LittleSpyException::class)
    fun start() {
        if (vm != null || attachConnector != null) {
            LOGGER.debug("connector is attached.")
            return;
        }

        // attach to virtual machine
        attachToVm()

        setupEvent()

        vm!!.resume()
    }

    /**
     * Stop the spy.
     */
    @Throws(LittleSpyException::class)
    fun stop() {
        try {
            listeningThread!!.interrupt()
            vm!!.dispose()
            LOGGER.debug("disconnect from port $debugPort on server $host")
        } catch (e : Throwable) {
            when (e) {
                is VMDisconnectEvent -> LOGGER.debug("Target VM is disconnected.")
                else -> throw LittleSpyException("Detach from virtual machine failed.", e)
            }
        }
    }

    private fun attachToVm() {
        val vmm = Bootstrap.virtualMachineManager()

        var connections = vmm.attachingConnectors()

        for (conn in connections) {
            if (TRANSPORT.equals(conn.transport().name(), true)) {
                attachConnector = conn
                break
            }
        }

        if (attachConnector == null) {
            throw LittleSpyException("Couldn't initalize the attaching connector")
        }

        val attachingConnector = attachConnector as AttachingConnector
        var defaultArguments = attachingConnector.defaultArguments()
        defaultArguments.get("port")!!.setValue(Integer.toString(debugPort))
        defaultArguments.get("hostname")!!.setValue(host);

        try {
            vm = attachingConnector.attach(defaultArguments)
            LOGGER.debug("port $debugPort on server $host is attached!")
            LOGGER.debug("attached virtual machine is ${vm!!.name()}")
            LOGGER.debug("attached virtual machine's version is ${vm!!.version()}")
        } catch (e: Throwable) {
            throw LittleSpyException("Couldn't attach to port $debugPort on server $host", e)
        }
    }

    private fun setupEvent() {
        // set default event listener
        if (vm == null) {
            throw LittleSpyException("VirtualMachine is not attached. Stop the event setup")
        }

        // the exception class may not be loaded
        for (exceptionClassName in config.getExceptionList()) {
            val cpr = vm!!.eventRequestManager().createClassPrepareRequest()
            cpr.addClassFilter(exceptionClassName)
            cpr.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD)
            cpr.isEnabled = true
        }

        for (exceptionClass in config.getExceptionList()) {
            val types = vm!!.classesByName(exceptionClass)
            for (type in types) {
                val er = createExceptionRequest(vm, type)
                exceptionRequestMap.put(exceptionClass, er)
            }
        }

        val eq = vm!!.eventQueue()

        val runnable = Runnable {
            LOGGER.debug("event listening thread is started.")

            var stopped = false
            while (!stopped || !Thread.interrupted()) {
                var eventSet : EventSet? = null

                try {
                    eventSet = eq.remove(500)
                } catch (ex : InterruptedException) {
                    Thread.currentThread().interrupt()
                    continue
                } catch (e : Throwable) {
                    LOGGER.debug("Unexpected exception.", e)
                    break
                }

                if (eventSet == null) {
                    continue
                }

                var resume = false
                for (event in eventSet) {
                    val request = event.request()

                    if (request != null) {
                        var eventPolicy = request.suspendPolicy()
                        resume = resume || (eventPolicy != EventRequest.SUSPEND_NONE)
                    }

                    if (event is ExceptionEvent) {
                        LOGGER.debug("Caught ExceptionEvent " + event)
                        val request = event.request()
                        when (request) {
                            is ExceptionRequest -> {
                                LOGGER.debug("Exception class is " + request.exception().name())
                                dumpHeapAndStack(request.exception().name())
                            }
                            else -> LOGGER.debug("not exception request")
                        }
                    } else if (event is ClassPrepareEvent) {
                        LOGGER.debug("Caught ClassPrepareEvent for " + event.referenceType())
                        val rt = event.referenceType()
                        createExceptionRequest(vm, rt)
                    } else if (event is VMDisconnectEvent || event is VMDeathEvent) {
                        stopped = true
                    }
                }

                if (resume) {
                    eventSet.resume()
                }
            }
        }

        listeningThread = Thread(runnable)
        listeningThread!!.name = "littlespy event listening"
        listeningThread!!.start()

        Thread.sleep(1000)
    }

    private fun dumpHeapAndStack(exception: String = "") {
        val now = DateTime.now()
        LOGGER.debug("dump heap for exception: " + exception)
        Utilities.dumpHeap(config.folderPathForDumpFiles, exception + "_" + forPattern.print(now))

        LOGGER.debug("dump stack for exception: " + exception)
        Utilities.dumpStack(vm, config.folderPathForDumpFiles, exception + "_" + forPattern.print(now))
    }

    private fun createExceptionRequest(vm: VirtualMachine?, refType: ReferenceType): ExceptionRequest {
        LOGGER.debug("Create exception request for " + refType)
        val er = vm!!.eventRequestManager().createExceptionRequest(refType, true, true)
        er.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD)
        er.isEnabled = true

        return er
    }
}