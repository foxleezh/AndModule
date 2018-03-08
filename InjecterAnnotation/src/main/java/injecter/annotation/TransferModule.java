package injecter.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by foxleezh on 18-3-8.
 */

@Retention(RUNTIME) @Target(FIELD)
public @interface TransferModule {
    String[] params();
    String[] types();
    int index();
}
