package yaga.extensions.aws.model.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface OutputSchema {
    String value();
}
