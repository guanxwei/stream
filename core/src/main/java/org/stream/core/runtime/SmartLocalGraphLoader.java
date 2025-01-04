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
package org.stream.core.runtime;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * A smart local graph loader. Will automatically load all the graphs when
 * initiating an instance. If not all the graphs are needed in runtime,
 * developers can use {@link LocalGraphLoader} directly and specify which graphs
 * should be parsed and will be used in the runtime.
 * 
 * @author weiguanxiong
 *
 */
public class SmartLocalGraphLoader extends LocalGraphLoader {

    public SmartLocalGraphLoader() throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:*graph/*.graph");
        for (Resource resource : resources) {
                String name = resource.getFilename();
                graphFilePaths.add(name);
        }
    }
}
