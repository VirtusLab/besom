package yaga.extensions.aws.model.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface InputSchema {
    String value();
}
