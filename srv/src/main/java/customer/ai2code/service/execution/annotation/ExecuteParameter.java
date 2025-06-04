// code\srv\src\main\java\customer\ai2code\service\execution\Parameter.java
package customer.ai2code.service.execution.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExecuteParameter {
    // String name();
    boolean required() default false;
    String description() ;
}