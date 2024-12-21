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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stream.core.component.ActivityRepository;
import org.stream.core.execution.DefaultEngine;
import org.stream.core.execution.Engine;
import org.stream.core.execution.GraphContext;
import org.stream.core.helper.GraphLoader;
import org.stream.core.helper.SmartLocalGraphLoader;

import java.io.IOException;

/**
 * Sample case for local workflow engine configuration.
 */
@Configuration
public class DefaultEngineConfiguration {

    @Bean
    public Engine engine() {
        return new DefaultEngine();
    }

    @Bean
    public ActivityRepository activityRepository() {
        return new ActivityRepository();
    }

    @Bean(initMethod = "init")
    public GraphLoader graphLoader() throws IOException {
        SmartLocalGraphLoader smartLocalGraphLoader = new SmartLocalGraphLoader();
        smartLocalGraphLoader.setGraphContext(graphContext());
        return smartLocalGraphLoader;
    }

    @Bean
    public GraphContext graphContext() {
        GraphContext graphContext = new GraphContext();
        graphContext.setActivityRepository(activityRepository());
        return graphContext;
    }

}
