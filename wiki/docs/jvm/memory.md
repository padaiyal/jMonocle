## JVM Memory [[3]](#references)
Java Virtual Machine or JVM is responsible for translating Java bytecode into native machine code and executing it. JVM is formed by three main components: ClassLoader, Runtime Data Area and the Execution Engine. The Class loader is responsible for loading the Java Classes into JVM. The Runtime Data Area stores all the data that will be used during the execution of the program. And finally, the Execution Engine deals mainly with running the application. In this article, we will focus on the Runtime Data Area or JVM Memory. The JVM memory has five sections that store different data: Method Area, Heap Area, Stack Memory, PC Register, Native Method Stack.

![JVM structure](images/jvm.jpg)

### Program Counter (PC) Register [[2]](#references)
It is used to store the address of the instruction being executed. Each JVM thread has its own PC Register. This section of the JVM memory does not contain any references to native methods. Instead, those are located in the Native Method Stacks.

### Stack Memory
When a new thread is created, a new Stack is created for that specific thread, meaning that no threads can share the same stack. <br> The stack datastructure uses a last-in-first-out (LIFO) approach. When a new method is called, a new stack frame is pushed onto the stack, and the JVM performs the defined operations. Once the function terminates, the stack frame is popped. The stack frame has three main parts: local variables, operand stack, and frame data.

#### Local Variables
Local variables are stored as an array whose size is determined at compile time. The data type determines the size of each entry in the collection. Each entry or slot has a size of 4 bytes.

| Types                      | Entry Size               |
|----------------------------|--------------------------|
| int, float, reference      | One                      |
| byte, short, char, boolean | One after parsing to int |
| long, double               | Two                      |

#### Operand Stack
The operand stack is a last-in-first-out (LIFO) stack. It stores the values needed for an intermediate operation, such as local variables and values. Take a look at the following example:
```
int a = 5;
int b = 4;
int c = a + b;
```
The above code generates the following bytecode:
```
0: iconst_1
1: istore_0
2: iconst_2
3: istore_1
4: iload_0
5: iload_1
6: iadd
7: istore_2
```
Lines 0 to 3 initializes `a` and `b`. In lines 4 and 5, the thread gets and pushs the values 5 and 4 into the operand stack, The `iadd` pops the two values, adds them together and pushes them back to the operand stack. Line 7 tells the thread to store the result in `c`.

#### Frame Data
The frame data contains symbolic references to the constant pool and the Exception table [[3]](#references). The exception table stores the information for each exception that the application throws, its type, a range that specifies lines where the program can throw an error, and the target (line) that can handle the exception [[6]](#references).

### Native Method Stacks [[3]](#references)
Stores Native method data. Native methods are methods that are written in a language other than Java, like C or C++. When a thread calls a Java method, it uses the Java stack, but if it invokes a native method, it uses the Native Method Stacks. The stacks behave differently depending on the language used. If the native method uses a `C-linkage model`, then C stacks are used.

### Method Area
This part of the JVM memory area stores all the class-level data (field types, modifiers, method code, etc.). It is created once the JVM starts and can be configured to be of dynamic or fixed size. It is shared by the same thread and stored in the heap [[2]](#references). For each loaded Class, the JVM stores the following information:
* The constant pool is an ordered set of constants used by the Class. A table represents it with three columns. The first column stores an index, the second column contains the type (e.g. Integer, Class, Utf8, MethodRef, FieldRef, etc.), and the third includes the value (String or numeric literal, or a reference).
* Class field information including name, type, modifiers.
* Method information including the method's name, return type, the number of parameters and their types, and the modifiers.
* Class variables [[3]](#references)

### Heap
It stores all the objects and instance variables. Unlike other parts of the memory, it is shared by the JVM threads of the same application. If a Class instance is changed by one thread, the change is viewed across the remaining threads. The size of the heap can be expanded or shrunk depending on the requirements of the computation. The garbage collector or GC generally works in this section by clearing unused objects [[2]](#references).

![Heap layout](images/heapLayout.PNG)

In Oracle Hotspot VM and OpenJDK VM, the heap is divided into three sections:
* **Young generation**: This is where the newly created objects are stored. It is divided into three other sections:
    * Eden: is the largest region, and it is where all newly created objects are stored.
      ** Survivor spaces: There are two survivor spaces. They store objects that survived the garbage collection in the `Eden` region. One of the survivor spaces is always empty. During each GC cycle all the surviving objects from the `young generation` are moved into the empty survivor space.
* **Old generation**: This is where objects which have survived several garbage collection cycles are stored [[4]](#references).
* **Permanent generation (<= JDK7) and metaspace (>=JDK8)**: This is where class information is stored. It is equal to the Method Area [[1]](#references). The main difference between `permanent generation` and `metaspace` is that `permanent generation` had a maximum fixed size which caused an `OutOfMemoryError`. So it was replaced with metaspace which can be resized during runtime.

Every time an object survives a GC iteration, the object has aged and based on its age, GC moves them up or promotes them to the next region or generation. From `Eden`, it moves to one of the survivor spaces, and from the survivor spaces the object can be transferred to the `old generation` where it will remain until it is no longer referenced [[4]](#references). Dividing the heap into generations comes from Ungar's statement: "Most objects die young" [[5]](#references). Therefore, having a section in the heap where most of the objects will need to be deallocated improves the GC's performance as it won't need to look into the entire heap to find objects to deallocate.
## References
[1] https://blogs.oracle.com/jonthecollector/presenting-the-permanent-generation <br>
[2] https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-2.html <br>
[3] https://www.artima.com/insidejvm/ed2/jvm.html <br>
[4] https://plumbr.io/handbook/garbage-collection-in-java <br>
[5] https://people.cs.umass.edu/~emery/classes/cmpsci691s-fall2004/papers/p157-ungar.pdf <br>
[6] https://www.overops.com/blog/the-surprising-truth-of-java-exceptions-what-is-really-going-on-under-the-hood/ <br>