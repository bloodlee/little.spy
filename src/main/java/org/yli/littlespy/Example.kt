package org.yli.littlespy

/**
 * Created by yli on 5/21/2016.
 */

fun main(args: Array<String>) {
    var config = LittleSpyConfig()
    config.addException("java.lang.IllegalArgumentException")
    config.addException("java.lang.UnsupportedOperationException")
    config.folderPathForDumpFiles = "D:/"

    val spy = LittleSpy(9999, config)

    spy.start()

    Thread.sleep(50000)

    spy.stop()
}