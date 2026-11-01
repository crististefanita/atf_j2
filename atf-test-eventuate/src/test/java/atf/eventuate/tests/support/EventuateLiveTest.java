package atf.eventuate.tests.support;

import atf.testkit.AtfE2eTest;
import atf.testkit.reporting.AtfReportPath;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.annotation.*;

/** Explicit LIVE selection; ordinary component/unit runs never contact the laboratory. */
@AtfE2eTest
@AtfReportPath
@ExtendWith(EventuateComponentExtension.class)
@EnabledIfSystemProperty(named = "atf.eventuate.live.enabled", matches = "true")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventuateLiveTest { }
