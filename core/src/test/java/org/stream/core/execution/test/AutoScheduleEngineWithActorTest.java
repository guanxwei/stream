package org.stream.core.execution.test;

import javax.annotation.Resource;

import org.apache.commons.lang3.RandomStringUtils;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.stream.core.execution.AutoScheduledEngine;
import org.stream.core.execution.GraphContext;
import org.stream.core.helper.test.GraphLoaderWithSpringActorTestConfiguration;
import org.stream.core.resource.ResourceTank;
import org.stream.core.resource.ResourceType;
import org.stream.extension.persist.TaskPersister;
import org.testng.Assert;
import org.testng.annotations.Test;

@ContextConfiguration(classes = GraphLoaderWithSpringActorTestConfiguration.class)
public class AutoScheduleEngineWithActorTest extends AbstractTestNGSpringContextTests {

    @Resource
    private AutoScheduledEngine engine;

    @Resource
    private GraphContext graphContext;

    @Resource    
    private TaskPersister taskPersister;

    @Test
    public void test() throws Throwable {
        String graphName = "springCase";

        org.stream.core.resource.Resource primary = org.stream.core.resource.Resource.builder()
                .value("testValue")
                .resourceReference(RandomStringUtils.randomAlphabetic(32))
                .build();
        Mockito.when(taskPersister.tryLock(Mockito.anyString())).thenReturn(true);
        ResourceTank resourceTank = engine.execute(graphContext, graphName, primary, false, ResourceType.OBJECT);

        org.stream.core.resource.Resource resource = resourceTank.resolve("stream::autoschedule::task::reference");

        Assert.assertNotNull(resource);
    }
}
