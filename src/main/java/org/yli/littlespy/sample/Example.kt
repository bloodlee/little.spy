package org.yli.littlespy.sample

import org.yli.littlespy.LittleSpy
import org.yli.littlespy.LittleSpyConfig

/**
 * Created by yli on 5/21/2016.
 */

fun main(args: Array<String>) {
    var config = LittleSpyConfig()

    // monitor some exceptions
    config.addException("java.lang.IllegalArgumentException")
    config.addException("java.lang.UnsupportedOperationException")
    config.addException("org.yli.learn.AException")

    // set where to dump the files
    config.folderPathForDumpFiles = args[0]

    val spy = LittleSpy(9999, config)

    spy.start()

    Thread.sleep(500000)

    spy.stop()
}