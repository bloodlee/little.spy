package org.yli.littlespy.domain

/**
 * Memory information of stack.
 *
 * Created by yli on 5/22/2016.
 */
class StackMemory {
    val threads : MutableList<ThreadInfo> = mutableListOf()
    override fun toString(): String {
        return "StackMemory(threads=$threads)"
    }
}

/**
 * Information of a thread.
 */
class ThreadInfo(val name: String) {
    var frames : MutableList<FrameInfo> = mutableListOf()
    override fun toString(): String {
        return "ThreadInfo(name='$name', frames=$frames)"
    }
}

/**
 * Information of a frame of a thread.
 */
class FrameInfo(val sourcePath: String, val methodName: String, val lineNumber: Int) {
    var variables : MutableList<VariableInfo> = mutableListOf()

    override fun toString(): String{
        return "FrameInfo(sourcePath='$sourcePath', methodName='$methodName', lineNumber=$lineNumber, variables=$variables)"
    }

}

/**
 * Information of a variable of a frame of a thread.
 */
class VariableInfo(val name: String, val type: String, val value: String){
    override fun toString(): String {
        return "VariableInfo(name='$name', type='$type', value='$value')"
    }
}