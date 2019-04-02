package org.stream.core.helper.test;

import javax.annotation.Resource;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.stream.core.execution.Engine;
import org.stream.core.execution.GraphContext;
import org.stream.core.resource.ResourceTank;
import org.testng.Assert;
import org.testng.annotations.Test;

@ContextConfiguration(classes = GraphLoaderWithSpringTestConfiguration.class)
public class GraphLoaderWithSpringTest extends AbstractTestNGSpringContextTests {

    @Resource
    private Engine engine;

    @Resource
    private GraphContext graphContext;

    @Test
    public void test() {
        String graphName = "springCase";

        ResourceTank resourceTank = engine.execute(graphContext, graphName, false);

        org.stream.core.resource.Resource resource = resourceTank.resolve("springModule");
        SpringModule springModule = (SpringModule) resource.getValue();

        Assert.assertNotNull(springModule);
        Assert.assertEquals(springModule.getName(), "springModule");
    }
}
