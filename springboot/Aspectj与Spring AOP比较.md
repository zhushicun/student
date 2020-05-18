# Aspectj与Spring AOP比较

[![img](https://cdn2.jianshu.io/assets/default_avatar/14-0651acff782e7a18653d7530d6b27661.jpg)](https://www.jianshu.com/u/2ac682c76713)

[沈渊](https://www.jianshu.com/u/2ac682c76713)关注

0.7832018.04.18 23:49:14字数 2,218阅读 10,591

## 1、简介

今天有多个可用的 AOP 库, 它们需要能够回答许多问题:

1. 是否与用户现有的或新的应用程序兼容？
2. 在哪里可以实现 AOP？
3. 与自己的应用程序集成多快？
4. 性能开销是多少？
   在本文中, 我们将研究如何回答这些问题, 并介绍 Spring aop 和 AspectJ, 这是 Java 的两个最受欢迎的 aop 框架。

## 2、AOP概念

在开始之前, 让我们对术语和核心概念进行快速、高层次的审查:

- **Aspect** —— 一种标准代码/功能, 分散在应用程序中的多个位置, 通常与实际的业务逻辑不同 (例如, 事务管理)。每个方面都侧重于特定的跨裁剪功能
- **Joinpoint** —— 它是执行程序 (如方法执行、构造函数调用或字段分配) 期间的特定点
- **Advice** —— 特定 joinpoint 中的方面所采取的行动
- **Pointcut** —— 与 joinpoint 匹配的正则表达式。每次连接点与切入点匹配时, 都将执行与该切入点关联的指定建议。
- **Weaving** —— 将各方面与目标对象链接起来以创建建议对象的过程

## 3、Spring AOP 和 AspectJ

现在, 让我们在一些维度上讨论 Spring AOP 和 AspectJ —— 例如功能、目标、Weaving（织入）、内部结构、joinpoints 和简单性。

#### 3.1、能力和目标

简单地说, Spring AOP 和 AspectJ 有不同的目标。

Spring aop 旨在提供一个跨 Spring IoC 的简单的 aop 实现, 以解决程序员面临的最常见问题。**它不打算作为一个完整的 AOP 解决方案** —— 它只能应用于由 Spring 容器管理的 bean。

另一方面, AspectJ 是原始的 aop 技术, 目的是提供完整的 aop 解决方案。它更健壮, 但也比 Spring AOP 复杂得多。还值得注意的是, AspectJ 可以在所有域对象中应用。

#### 3.2、Weaving（织入）

AspectJ 和 Spring AOP 都使用不同类型的编织, 这会影响它们在性能和易用性方面的行为。

AspectJ 使用三种不同类型的Weaving:

1. **编译时 Weaving**: AspectJ 编译器作为输入我们的方面的源代码和我们的应用, 并产生一个织入类文件作为输出；
2. **编译后 Weaving**: 这也称为二进制织入。它是用来织入现有的类文件和 JAR 文件与我们的方面；
3. **加载时间 Weaving**: 这就像前二进制织入, 不同的是织入被推迟, 直到类加载程序加载类文件到 JVM。

要了解更多关于 AspectJ 本身的详细信息, 请 [阅读此文](https://link.jianshu.com/?t=http%3A%2F%2Fwww.baeldung.com%2Faspectj)。

当 AspectJ 使用编译时和class文件加载时织入时，Spring AOP 利用运行时织入。

使用运行时编织, 这些方面在使用目标对象的代理执行应用程序时被编织-使用 JDK 动态代理或 CGLIB 代理 (在下一点讨论):



![img](https://upload-images.jianshu.io/upload_images/7179784-5d499156cfbeba32.png?imageMogr2/auto-orient/strip|imageView2/2/w/1200/format/webp)

image

#### 3.3、内部结构与应用

Spring aop 是基于代理的 aop 框架。这意味着, 要实现目标对象的各个方面, 它将创建该对象的代理。使用以下两种方法之一实现:

1. JDK 动态代理 —— Spring AOP 的首选方式。只要目标对象实现甚至一个接口, 就会使用 JDK 动态代理；
2. CGLIB 代理 —— 如果目标对象没有实现接口, 则可以使用 CGLIB 代理。

我们可以从 [官方文档](https://link.jianshu.com/?t=https%3A%2F%2Fdocs.spring.io%2Fspring%2Fdocs%2Fcurrent%2Fspring-framework-reference%2Fcore.html%23aop-proxying) 中了解有关 Spring AOP 代理机制的更多信息。

另一方面, AspectJ 在运行时不做任何事情, 因为类是直接用方面进行编译的。

与 Spring AOP 不同, 它不需要任何设计模式。为了编织代码的各个方面, 它引入了称为 AspectJ 编译器 (ajc) 的编译器, 通过它编译我们的程序, 然后通过提供一个小型 (100K) 运行时库来运行它。

#### 3.4、Joinpoints

在3.3 节中, 我们显示了 Spring AOP 是基于代理模式的。因此, 它需要将目标 Java 类分类, 并相应地应用交叉问题。

但这是有限制的。我们不能在 "最终" 类中应用交叉问题 (或方面), 因为它们不能被重写, 因此会导致运行时异常。

同样适用于静态和最终方法。不能将 Spring 方面应用于它们, 因为它们不能被覆盖。因此, 由于这些限制, Spring AOP 只支持方法执行连接点。

然而, AspectJ 在运行前直接将横切关注点编织到实际代码中。与 Spring AOP 不同, 它不需要对目标对象进行子类, 因此也支持许多其他 joinpoints。以下是支持的 joinpoints 的摘要:

| Joinpoint                    | Spring AOP Supported | AspectJ Supported |
| :--------------------------- | :------------------: | :---------------: |
| Method Call                  |          No          |        Yes        |
| Method Execution             |         Yes          |        Yes        |
| Constructor Call             |          No          |        Yes        |
| Constructor Execution        |          No          |        Yes        |
| Static initializer execution |          No          |        Yes        |
| Object initialization        |          No          |        Yes        |
| Field reference              |          No          |        Yes        |
| Field assignment             |          No          |        Yes        |
| Handler execution            |          No          |        Yes        |
| Advice execution             |          No          |        Yes        |

还值得注意的是, 在 Spring AOP 中, aspects不应用于在同一个类中相互调用的方法。

这显然是因为当我们调用同一类中的方法时, 我们就不会调用 Spring AOP 提供的代理的方法。如果我们需要这个功能, 那么我们必须在不同的 bean 中定义一个单独的方法, 或者使用 AspectJ。

#### 3.5、简单性

Spring AOP 显然更简单, 因为它不会在我们的构建过程中引入任何额外的编译器或织入。它使用运行时编织, 因此它与我们通常的构建过程无缝集成。虽然它看起来很简单, 但它只适用于由 Spring 管理的 bean。

但是, 要使用 AspectJ, 我们需要引入 AspectJ 编译器 (ajc) 并重新打包所有的库 (除非我们切换到编译后或加载时间的织入)。

当然, 这比前者更复杂, 因为它引入了 AspectJ Java 工具 (包括编译器 (ajc)、调试器 (ajdb)、文档生成器 (ajdoc)、程序结构浏览器 (ajbrowser)), 我们需要将它们与我们的 IDE 或生成工具。

#### 3.6、性能

就性能而言, 编译时织入比运行时织入快得多。Spring AOP 是基于代理的框架, 因此在应用程序启动时会创建代理。另外, 每个方面还有一些方法调用, 这会对性能产生负面影响。

另一方面, AspectJ 在应用程序执行之前将这些方面编织到主代码中, 因此没有额外的运行时开销, 与 Spring AOP 不同。

基于这些原因, 基准表明 AspectJ 的速度几乎比 Spring AOP 快8到35倍。

## 4、总结

此快速表总结了 Spring AOP 和 AspectJ 之间的关键区别:

| Spring AOP                                       | AspectJ                                                      |
| :----------------------------------------------- | :----------------------------------------------------------- |
| 在纯 Java 中实现                                 | 使用 Java 编程语言的扩展实现                                 |
| 不需要单独的编译过程                             | 除非设置 LTW，否则需要 AspectJ 编译器 (ajc)                  |
| 只能使用运行时织入                               | 运行时织入不可用。支持编译时、编译后和加载时织入             |
| 功能不强-仅支持方法级编织                        | 更强大 - 可以编织字段、方法、构造函数、静态初始值设定项、最终类/方法等......。 |
| 只能在由 Spring 容器管理的 bean 上实现           | 可以在所有域对象上实现                                       |
| 仅支持方法执行切入点                             | 支持所有切入点                                               |
| 代理是由目标对象创建的, 并且切面应用在这些代理上 | 在执行应用程序之前 (在运行时) 前, 各方面直接在代码中进行织入 |
| 比 AspectJ 慢多了                                | 更好的性能                                                   |
| 易于学习和应用                                   | 相对于 Spring AOP 来说更复杂                                 |

## 5、选择正确的框架

如果我们分析了本节中提出的所有论点, 我们就会开始理解, 一个框架比另一个架构更好。
简单地说, 选择很大程度上取决于我们的要求:

- **框架**: 如果应用程序没有使用 spring 框架, 那么我们就别无选择, 只能放弃使用 spring AOP 的想法, 因为它无法管理任何超出 spring 容器范围的东西。但是, 如果我们的应用程序是完全使用 spring 框架创建的, 那么我们可以使用 spring AOP, 因为它是简单的学习和应用
- **灵活性**: 由于有限的 joinpoint 支持, Spring aop 不是一个完整的 aop 解决方案, 但它解决了程序员面临的最常见的问题。尽管如果我们想深入挖掘和开发 AOP 以达到其最大能力, 并希望得到广泛的可用 joinpoints 的支持, 那么最好选择 AspectJ
- **性能**: 如果我们使用的是有限的切面, 那么就会有细微的性能差异。但有时, 应用程序有成千上万个切面的情况。我们不想在这样的情况下使用运行时编织, 所以最好选择 AspectJ。AspectJ 已知的速度比 Spring AOP 快8到35倍
- **两者的最佳之处**: 这两个框架都是完全兼容的。我们总是可以利用 Spring AOP； 只要有可能, 仍然可以在不支持前者的地方使用 AspectJ 获得支持

## 6、结论

在本文中, 我们分析了 Spring AOP 和 AspectJ 的几个关键领域。

我们比较了两种 AOP 方法的灵活性, 以及它们将如何轻松地适应我们的应用程序