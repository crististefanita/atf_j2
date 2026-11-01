package atf.sql.tests.support;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.annotation.*;

@AtfComponentTest
@ExtendWith(SqlComponentExtension.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SqlComponentTest { }
