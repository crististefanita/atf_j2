package atf.eventuate.tests.support;
import atf.testkit.AtfComponentTest;
import atf.testkit.reporting.AtfReportPath;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.annotation.*;
@AtfComponentTest
@AtfReportPath
@ExtendWith(EventuateComponentExtension.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventuateComponentTest { }
