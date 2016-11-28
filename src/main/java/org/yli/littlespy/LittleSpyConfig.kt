package org.yli.littlespy

/**
 * Configuration of {@link LittleSpy}.
 *
 * @param dumpStack to dump stack information or not
 * @param autoDumpWhenExceptionHappen to automatically dump when exception or not.
 * @param exceptionClassList the list of monitored exception class.
 *
 * Created by yli on 5/21/2016.
 */
class LittleSpyConfig(val dumpStack : Boolean = true,
                      val autoDumpWhenExceptionHappen : Boolean = true,
                      private val exceptionClassList : MutableList<String> = mutableListOf()) {

    var folderPathForDumpFiles : String = ""

    /**
     * Add exception, which will be added into {@link LittleSpy} attentation list.
     * When it's thrown, LittleSpy will be activated.
     *
     * @param className the full exception class name, such as "java.lang.IllegalArgumentException".
     */
    fun addException(className: String) {
        exceptionClassList.add(className)
    }

    /**
     * Remove the given exception
     *
     * @param className the full exception class name.
     */
    fun removeException(className: String) {
        exceptionClassList.remove(className)
    }

    /**
     * get the list of exception names.
     */
    fun getExceptionList(): List<String> {
        val results : List<String> = exceptionClassList
        return results
    }
}
