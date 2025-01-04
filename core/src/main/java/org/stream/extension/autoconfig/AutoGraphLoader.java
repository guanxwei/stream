/*
 * Copyright (C) 2021 guanxiongwei
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.stream.extension.autoconfig;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.stream.core.exception.GraphLoadException;
import org.stream.core.runtime.AbstractGraphLoader;
import org.stream.core.runtime.GraphLoader;

import java.io.IOException;
import java.util.List;

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

    private void loadGraphsIfPossible(final ApplicationContext applicationContext) throws GraphLoadException, IOException {
        AbstractGraphLoader graphLoader = (AbstractGraphLoader) applicationContext.getBean(GraphLoader.class);
        if (applicationContext.getBean("graphs") instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<String> graphs = (List<String>) applicationContext.getBean("graphs");
            for (String path : graphs) {
                graphLoader.loadGraphFromSource(path);
            }
        } else {
            ClassLoader classLoader = getClass().getClassLoader();
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
            Resource[] resources = resolver.getResources("classpath:graph/*.graph");
            for (Resource resource : resources) {
                    graphLoader.loadGraphFromSource(resource.getFilename());
            }
        }
    }

    
}
