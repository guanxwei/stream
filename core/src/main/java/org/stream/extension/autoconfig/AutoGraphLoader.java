package org.stream.extension.autoconfig;

import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.stream.core.helper.AbstractGraphLoader;
import org.stream.core.helper.GraphLoader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AutoGraphLoader implements ApplicationListener<ApplicationContextEvent> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onApplicationEvent(final ApplicationContextEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            try {
                loadGraphsIfPossible(event.getApplicationContext());
            } catch (Exception e) {
                log.error("Fail to register strategy config reload listener, please restart the app if possible");
                throw new RuntimeException(e);
            }
        }
    }

    private void loadGraphsIfPossible(final ApplicationContext applicationContext) throws Exception {
        AbstractGraphLoader graphLoader = (AbstractGraphLoader) applicationContext.getBean(GraphLoader.class);
        if (applicationContext.getBean("graphs") != null && applicationContext.getBean("graphs") instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<String> graphs = (List<String>) applicationContext.getBean("graphs");
            for (String path : graphs) {
                graphLoader.loadGraphFromSource(path);
            }
        } else {
            ClassLoader classLoader = getClass().getClassLoader();
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
            Resource[] resources = resolver.getResources("classpath:graph/*.graph");
            if (resources != null) {
                for (Resource resource : resources) {
                    graphLoader.loadGraphFromSource(resource.getFilename());
                }
            }
        }
    }

    
}
