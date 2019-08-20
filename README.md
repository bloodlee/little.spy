LittleSpy
=========
A utility to generate the runtime stack memory and heap memory of a Java application when exception happens.
It supports HotSpot VM and built upon JDI (Java Debug Interface). 

Stack Memory
------------
Usually, in a frame, there probably has local variables of primitive types. When exception is thrown, their status is important
for figuring out the root cause. But you can not see these variables in heap snapshot. `LittleSpy` uses JDI to get all their information,
including variable name, type and values.  

Heap Memory
-----------
It is relatively easier to get the snapshot of heap memory. `LittleSpy` uses `ManagementFactory` provided by Hotspot JVM 
to generate the heap memory file (*.hprof). The file format is standard and it can be investigated by many free tools, such as
 [VirtualVM](https://visualvm.java.net/) and [Eclipse Memory Analyzer](https://wiki.eclipse.org/MemoryAnalyzer).
  
*NOTE*: Due to the limit of `ManagementFactory`, to generate the heap memory, `LittleSpy` must be included into same JVM.

Exception Registry
------------------
For now, LittleSpy generates the memory snapshot when specific exception happens. So before using it, you need to finish
the exception registry. 

Following code is about how to generate `LittleSpyConfig` for `LittleSpy`

```
var config = LittleSpyConfig()

// monitor some exceptions
config.addException("java.lang.IllegalArgumentException")
config.addException("java.lang.UnsupportedOperationException")
config.addException("org.yli.learn.AException")

// set where to dump the files
config.folderPathForDumpFiles = args[0]
```

Start and Stop LittleSpy
------------------------
Since `LittleSpy` is built on JDI, the targeted Java application need to open debugging port.

When starting the Java application, please append following VM arguments.

```
-agentlib:jdwp=transport=dt_socket,server=y,address=9999,suspend=y
```

After initializing the `LittleSpyConfig`, we can start the `LittleSpy`.

```$xslt
val spy = LittleSpy(9999, config)

// start the LittleSpy
spy.start()

...

// stop the LittleSpy
spy.stop()
```

When registered exception is thrown, heap and stack memory will be saved into two files (*.json, *.hprof)
respectively.

![Alt text](screenshot/memory_files.png?raw=true "Memory files")

Stack Memory Viewer
-------------------
To make the stack memory viewing easier, `LittleSpy` provides a very simple viewer to load the stack memory file (*.json).
![Alt text](screenshot/stack_memory_viewer_screenshot.png?raw=true "Stack memory viewer")

License
=======
[Apache License Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)

