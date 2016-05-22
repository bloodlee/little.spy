package org.yli.littlespy.domain

/**
 * Created by yli on 5/22/2016.
 */
class StackMemory {
    val threads : MutableList<ThreadInfo> = mutableListOf()
}

class ThreadInfo(val name: String) {
    var frames : MutableList<FrameInfo> = mutableListOf()
}

class FrameInfo(val sourcePath: String, val methodName: String, val lineNumber: Int) {
    var variables : MutableList<VariableInfo> = mutableListOf()
}

class VariableInfo(val name: String, val type: String, val value: String){
}