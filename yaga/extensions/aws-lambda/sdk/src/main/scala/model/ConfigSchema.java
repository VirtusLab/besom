package yaga.extensions.aws.model.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigSchema {
    String value();
}
