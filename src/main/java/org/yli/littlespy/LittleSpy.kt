package org.yli.littlespy

import com.sun.jdi.Bootstrap
import com.sun.jdi.VirtualMachine
import com.sun.jdi.connect.AttachingConnector
import com.sun.jdi.event.EventSet
import com.sun.jdi.event.ExceptionEvent
import com.sun.jdi.event.VMDisconnectEvent
import com.sun.jdi.request.EventRequest
import com.sun.jdi.request.ExceptionRequest
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.slf4j.LoggerFactory
import org.yli.littlespy.exceptions.LittleSpyException
import org.yli.littlespy.utilities.Utilities
import org.yli.littlespy.utilities.dumpHeap
import org.yli.littlespy.utilities.dumpStack

/**
 * Created by yli on 5/21/2016.
 */

class LittleSpy(val debugPort : Int, val config : LittleSpyConfig = LittleSpyConfig()) {

    val LOGGER = LoggerFactory.getLogger(javaClass)

    val host = "127.0.0.1"

    val TRANSPORT = "dt_socket"

    var attachConnector: AttachingConnector? = null

    var vm: VirtualMachine? = null

    var listeningThread : Thread? = null

    val forPattern = DateTimeFormat.forPattern("yyyy_MM_dd_H_m_s")

    var extraClassLoader : ClassLoader? = null

    init {
        LOGGER.debug("LittleSpy was just created.")
    }

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

        // dump memory when exception throws
        // NOTE: apy attention, only loaded exception could be detected by VirtualMachine
        for (exceptionClassName in config.getExceptionList()) {
            var refTypes = vm!!.classesByName(exceptionClassName)

            if (refTypes.isEmpty() && extraClassLoader != null) {
                extraClassLoader!!.loadClass(exceptionClassName)
                refTypes = vm!!.classesByName(exceptionClassName)
            }

            for (refType in refTypes) {
                val er = vm!!.eventRequestManager().createExceptionRequest(refType, true, true)
                er.setSuspendPolicy(EventRequest.SUSPEND_ALL)
                er.enable()

                LOGGER.debug("exception $exceptionClassName request is enabled.")
            }
        }

        val eq = vm!!.eventQueue()

        val runnable = Runnable {
            LOGGER.debug("event listening thread is started.")

            while (!Thread.interrupted()) {
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
                        // vm!!.suspend()
                        // suspendAllThreads()

                        var name = (event.request() as ExceptionRequest)!!.exception().name()
                        LOGGER.debug("exception $name is caught!")
                        LOGGER.debug("catch location is ${event.catchLocation()}")

                        dumpHeapAndStack()

                        // resumeAllThreads()
                        // vm!!.resume()
                    }
                }

                // if (resume) {
                eventSet.resume()
                // }
            }
        }

        listeningThread = Thread(runnable)
        listeningThread!!.name = "event listening"
        listeningThread!!.start()

        Thread.sleep(1000)
    }

    private fun dumpHeapAndStack() {
        val now = DateTime.now()
        LOGGER.debug("dump heap file")
        Utilities.dumpHeap(config.folderPathForDumpFiles, forPattern.print(now))

        LOGGER.debug("dump stack file")
        Utilities.dumpStack(vm, config.folderPathForDumpFiles, forPattern.print(now))
    }

    private fun resumeAllThreads() {
        for (thread in vm!!.allThreads()) {
            thread.resume()
        }
    }

    private fun suspendAllThreads() {
        for (thread in vm!!.allThreads()) {
            thread.suspend()
        }
    }

}
