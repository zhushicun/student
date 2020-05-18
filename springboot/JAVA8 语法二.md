# JAVA8 语法二
## 概述
对于一些常用java8四大语法的基本使用
### Function&lt;T,R&gt;
```
import java.util.function.Function;
/**
* TODO Function<T,R> 接收一个参数，返回一个结果，调用方法apply()
 * andThen先执行function逻辑，将执行结果放入andthen指定的function1中作为参数，执行function1
 * compose先执行compose指定的funciton2函数，然后将function2函数执行出来的结果，放入function中作为参数执行得到结果
* @datetime 2019/12/23
* @author shawn
**/
public class TestFunction {
    public static void main(String[] args) {
        Function<Integer,Integer> function=s->s*10+2;
        Function<Integer,Integer> function1=x->x*10;
        Function<Integer,Integer> function2=s->s*2;
        System.out.println(function.apply(10));
        System.out.println(function.andThen(function1).apply(2));
        System.out.println(function.compose(function2).apply(2));
    }
}
```

### Consumer&lt;T&gt;
```
import java.util.function.Consumer;
/**
* TODO Consumer<T> 接收一个参数，没有返回结果，调用函数accept()
* @datetime 2019/12/23
* @author shawn
**/
public class TestConsumer {
    public static void main(String[] args) {
        Consumer<Integer> integerConsumer=x-> System.out.println(x*10);
        integerConsumer.accept(10);
    }
}
```

### Predicate&lt;T&gt;

```
import java.util.function.Predicate;
/**
* TODO Predicate<T> 传入一个参数，返回一个boolean类型，调用函数test
* @datetime 2019/12/23
* @author shawn
**/
public class TestPredicate {
    public static void main(String[] args) {
        Predicate<Integer> integerPredicate=t->t%2==0;
        System.out.println(integerPredicate.test(2));
        System.out.println(integerPredicate.test(3));
    }
}
```

### Supplier&lt;T&gt;

```
/**
* TODO Supplier<T> 无传入参数，返回一个值，调用方法get()
* @datetime 2019/12/23
* @author shawn
**/
public class TestSupplier {
    public static void main(String[] args) {
        Supplier<Integer> integerSupplier=()-> {System.out.println(100);return 100;};
        Integer integer = integerSupplier.get();
        System.out.println("接收到的参数结果："+integer);
    }
}
```

## 扩展

### 非空操作的判断 Optional&lt;T&gt;
```
import org.shawn.tuia.model.User;

import java.util.Optional;
/**
* TODO
 * 1.声明一个空的Optional
 * Optional<Car> optCar = Optional.empty();
 * 2.依据一个非空值创建Optional
 * Optional<Car> optCar = Optional.of(car);
 * 3.可接受null的Optional
 * Optional<Car> optCar = Optional.ofNullable(car);
* @datetime 2019/12/27
* @author shawn
**/
public class TestOptional {
    public static void main(String[] args){
        User user=null;
        //user.setName("11321");
//        String sdffs = Optional.ofNullable(user).map(User::getName).orElse("sdffs");
//        String s="1111";
//        String s1 = Optional.ofNullable(s).map(String::toString).orElse("123");
        String ss=null;
        String s2 = Optional.ofNullable(ss).map(String::toString).orElseThrow(NullPointerException::new);
//        System.out.println(s1);
//        System.out.println(sdffs);
        System.out.println(s2);
    }
}
```