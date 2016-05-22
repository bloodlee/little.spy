package org.yli.littlespy

import com.sun.jdi.Bootstrap
import com.sun.jdi.VirtualMachine
import com.sun.jdi.connect.AttachingConnector
import com.sun.jdi.connect.Connector
import org.slf4j.LoggerFactory
import org.yli.littlespy.exceptions.LittleSpyException

/**
 * Created by yli on 5/21/2016.
 */

class LittleSpy(val debugPort : Int, val config : LittleSpyConfig = LittleSpyConfig()) {

    val LOGGER = LoggerFactory.getLogger(javaClass)

    val host = "127.0.0.1"

    val TRANSPORT = "dt_socket"

    var attachConnector: AttachingConnector? = null

    var vm: VirtualMachine? = null

    init {
        LOGGER.debug("LittleSpy was just created.")
    }

    fun start() {
        if (vm != null || attachConnector != null) {
            LOGGER.debug("connector is attached.")
            return;
        }

        // attach to virtual machine
        attachToVm()
    }

    fun stop() {
        vm!!.dispose()

        LOGGER.debug("disconnect from port $debugPort on server $host")
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
        } catch (e: Throwable) {
            throw LittleSpyException("Couldn't attach to port $debugPort on server $host", e)
        }
    }

}
