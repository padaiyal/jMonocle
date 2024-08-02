## Garbage Collection

The Garbage Collection is an automatic process which main task is to free up the memory used by objects no longer used by an application. It also may move the instance to reduce heap fragmentation. **Heap fragmentation** occurs when the GC deallocates non-contiguous memory within the heap, which means free memory "sprinkled" around. Heap fragmentation prevents some objects from being allocated even though there is enough total space to store them. That is because there is not enough contiguous memory to hold the new instance. Preventing GC is done by compacting the used memory. Compacting is triggered differently depending on the GC running in the JVM. Some GC compresses the used memory after every garbage collection, but others compact whenever is needed [[6]](#references).

![Memory Fragmentation](images/memory_fragmentation.jpg)

In general, every GC triggers a **stop-the-world** pause. Stop-the World pause occurs when the GC freezes all the running Java applications in the JVM so the GC can perform its tasks. The length of this pause depends on the GC algorithm. A longer delay decreases the algorithm's overall performance and increases its latency, so it is essential to reduce the wait time as much as possible. A way to minimize the `stop-the-world` pause is by using Concurrency, which performs some GC tasks while the application threads are running.

GC has two general steps that it follows to free up unnecessary memory:
* **Marking**: In this phase the GC algorithm identifies all the reachable objects starting from the GC (e.g. local variables, input parameters of the method being executed, static fields, etc.). It traverses all the referenced objects and mark them as "live" [[3]](#references). Depending on the algorithm used, the marking can be stored within the object as a mark bit, or it can be stored in a MarkBitMap [[5]](#references)

* **Collecting**: Once marking is done, the GC needs to collect the garbage objects. The approaches vary by GC, but there are three ways they handle the unreferenced data:
    * **Mark and sweep** is the simplest way. After marking the object, it deletes them from memory. Its main disadvantage is that it causes heap fragmentation.
    * **Mark-sweep-compact**, which solves the issue of heap fragmentation but compacting used memory together. A downside of this is an increase in `stop-the-world` pause length and thus the Java application latency.
    * **Mark and copy** can be used to reduce the delay. `Mark and copy` work by marking and copying the live objects into another section of the heap. Because the GC can do copying and marking simultaneously, it reduces the `stop-the-world` pause, but it requires more heap memory [[3]](#references).

A garbage collector can be named differently depending on the location of the heap it processes:
* **Minor GC**: it occurs in the `young generation`. It is triggered when `Eden` region is almost full. It generates a `stop-the-world` pause so the GC can `mark and copy` the referenced objects from `Eden` to one of the survivor spaces.
* **Major GC**: it occurs in the `old generation`. It is triggered when the `old generation/permanent generation` is full, but can also be started by a minor GC [[2]](#references). It may also generate a `stop-the-world` pause but the length will depend on the GC algorithm implemented.

It is important to mention that there exist situations in which objects inside the `old generation` reference an object inside `Eden`. The GC deals with this scenario by using remembered sets that keep track of incoming pointers to `Eden`. Each region in the heap stores a remembered set. A card table is a type of remembered set, an array of bytes with each element or "card" pointing to an address in the heap. [[1]](#references)

### Garbage Collector Algorithms
The following lists show some of the most popular Garbage Collector Algorithms used:

#### Serial Garbage Collector [[6]](#references)
It is one of the simplest GCs. It is a single-threaded GC, and it will trigger the `stop-the-world` event when it processes the heap. In `young generations`, it uses `mark and copy`, and in `old generations`, it uses mark-sweep-compact. It is useful for single-processor systems, like Docker containers and virtual machines. Add the VM option `-XX:+UseSerialGC` to use this GC .

#### Parallel Garbage Collector [[6]](#references)
It is also known as throughput GC is a multithreaded GC, and like the Serial GC, it also triggers a `stop-the-world` event when it processes the heap. During minor GC, it marks and copies the referenced objects from one region to another. In Major GC, it uses mark-sweep-compact with multiple threads. It reduces `stop-the-world` pauses compared to Serial GC, though there is no guarantee that the pauses will be very short compared to the algorithms described below. Add the VM option `-XX:+UseParallelGC` to use this GC.

#### Concurrent Mark Sweep (CMS) Garbage Collector
It is a concurrent multithreaded GC. It uses `mark and copy` in the `young generation` and `mark and sweep` in the `old generation`. A major flaw of this algorithm is that when the `old generation` is fragmented, it needs to trigger a `stop-the-world` event to compact the `old generation` region, defeating the purpose of concurrent `mark and sweep` [[6]](#references).

Concurrent mark and sweep have six phases:
1. **Initial Mark**: One of the `stop-the-world` events. During this phase, the GC identifies the objects referenced from the `young generation` and the `GC roots`, which are objects that are being referenced by local variables of the currently running method or also static fields.
2. **Concurrent Mark**: During this phase, the algorithm finds all the referenced objects in the `young generation`, starting from the objects identified in the previous step.
3. **Concurrent Preclean**: Whenever the references (unreferenced, referenced by another object, new object,  etc.) were modified by the application, the JVM  mark that area of the heap or "card" as dirty, which lets the GC know that it has to rescan the object and clean the "card." A card can be referenced as dirty if it has a modified bit in the remembered set.
4. **Concurrent Abortable Preclean**: The algorithm continues doing the same thing as the previous phase until an abortion condition, like a specific number of iterations or elapsed time,  has been met. The main objective of this phase is to take as much of the work from the next stage.
5. **Final Remark**: One of the `stop-the-world` events. Since the previous phases were concurrent, the GC may not have detected some changes, so it tries to identify the remaining referenced objects. It usually runs when the `young generation` is as empty as possible.
6. **Concurrent Sweep**: Removes unused objects from the heap [[4]](#references).

CMS has been deprecated since JDK 11 and replaced by G1 as the default GC in OpenJDK JVM and HotspotVM. Its main advantage is that it improves latency, but it has worse throughput than Parallel GC with CPU-bound applications .

Add the VM option `-XX:+UseParNewGC` to use this GC [[6]](#references).

#### G1 or Garbage First Garbage Collector
It is a concurrent multithreaded GC. It is the default collector since JDK 11 replacing CMS, but it has been available since JDK 8 [[6]](#references). It divides the heap into sections of the same size but maintains the generational concept without a contiguous generation. It makes use of semi concurrent marking, which has five phases:

1. **Initial mark phase**: This is a `stop-the-world` event, where it identifies the root objects. The `young garbage collector` trigger this phase.
2. **Root region scanning phase**: Scans survivor regions from the initial mark to references to `old generation` objects. This step runs concurrently, but the `young garbage collector` can terminate with its `stop-the-world` event.
Concurrent marking: Tries to identify the reachable objects in the heap. This phase runs concurrently.
3. **Remark phase**:  Final identification of the reachable objects. It is a `stop-the-world` event.
4. **Cleanup phase**:  This is a partial `stop-the-world` event. It freezes the running java thread applications when it identifies free regions and regions with garbage objects. This phase runs concurrently after identifying the free regions and adding them to the free list (list to keep track of the areas in memory that are free) [[7]](#references).

In `young generation` regions, it marks the referenced objects concurrently. It moves them up to the survivor space or the `old generation` by copying them from one region to another (with `stop-the-world` event) [[4]](#references). Unlike the previously described GCs, it does not have a full major GC, rather it collects from both `young and old generation`. The mixed garbage collection, triggered after the marking phase, will collect the `young generation` and a certain number of `old regions`. It has a default value of 8, but you can use the flag `-XX:G1MixedGCCountTarget` to change its value. The chosen regions will be collected based on priority [[7]](#references). A section with a higher number of unreferenced data has a higher priority than another area with less garbage, and that is why it is called Garbage First [[8]](#references). Remembered sets are very useful in this situation where there are more regions to monitor. Due to the importance of reducing the `stop-the-world` event's length, G1 sets its maximum pause time to 200 ms, but this value can be modified by adding `-XX:MaxGCPauseMillis=<newPauseLength>`

Add the VM option `-XX:+UseG1GC` to use this GC [[7]](#references).

## References
[1] https://docs.oracle.com/javase/8/docs/technotes/guides/vm/gctuning/g1_gc.html <br>
[2] https://plumbr.io/blog/garbage-collection/minor-gc-vs-major-gc-vs-full-gc <br>
[3] https://plumbr.io/handbook/garbage-collection-algorithms <br>
[4] https://plumbr.io/handbook/garbage-collection-algorithms-implementations <br>
[5] https://shipilev.net/jvm/diy-gc/ <br>
[6] http://ommolketab.ir/aaf-lib/dzdtg33tuzo4y4bd35xg2hmfg3hknl.pdf <br>
[7] https://www.oracle.com/technical-resources/articles/java/g1gc.html <br>
[8] https://www.youtube.com/watch?v=trkYGEGT6_w <br>
