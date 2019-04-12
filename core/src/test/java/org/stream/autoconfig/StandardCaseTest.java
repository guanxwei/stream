package org.stream.autoconfig;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.stream.extension.autoconfig.StandardAutoScheduleEngineConfiguration;

@ContextConfiguration(classes = StandardAutoScheduleEngineConfiguration.class)
public class StandardCaseTest extends AbstractTestNGSpringContextTests {

}
