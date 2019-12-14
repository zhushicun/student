查询、监控、分析Java应用程序的时候，我们关注的点主要集中在资源利用，比如CPU，内存，IO，线程等。其中内存以及线程的分析尤为常见。内存，是否有内存泄漏，各个区块内存分配是否大小合适。线程，数目是否合理，有没有死锁，运行状态如何。

jps -vl
jps是JDK提供的一个小工具，上面的命令会把操作系统里面的java应用都展示出来，显示PID，启动类或者JAR，VM参数。可以通过jps -help进一步了解详细信息。当然也可以用操作系统的netsat查询PID。下文提到的PID均为jps中得到的PID。

jinfo -flags PID
显示JVM的参数，包括显示设置的和系统默认的。比如所用的垃圾回收器，堆的最大值等。也可以用jinfo -sysprops PID来显示System.getProperties()的内容。

jstat -gc PID
显示JVM的各个内存区使用情况（容量和使用量），GC的次数和耗时。可以通过命令jstat -class PID查看class的加载情况。

jmap -dump:file=data.hprof PID
把JVM的堆dump出来，用更高级的分析工具进行分析。命令jmap -heap PID可以查看堆的配置信息和使用情况，也很有用。

jstack PID
查看线程运行情况，检测是否有死锁。

jconsole
JDK提供的一个可视化资源查看，监控工具。

jvisualvm
JDK提供的另外一个一站式资源查看，监控，管理工具。支持插件机制，可以自己安装插件，定制jvisualvm。常用的是Visual GC插件。也可以通过该工具dump JVM的堆。也可以导入已经dump出来的堆信息进行分析。
————————————————
版权声明：本文为CSDN博主「kimy」的原创文章，遵循 CC 4.0 BY-SA 版权协议，转载请附上原文出处链接及本声明。
原文链接：https://blog.csdn.net/kimylrong/article/details/50970493



重点是后面两个，然后eclipse中有mat插件，idea也有**JProfilerl**插件