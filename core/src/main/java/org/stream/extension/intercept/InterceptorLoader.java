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

package org.stream.extension.intercept;

import java.util.*;

import lombok.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.extern.slf4j.Slf4j;

/**
 * Interceptor loader.
 * @author guanxiongwei
 *
 */
@Slf4j
public class InterceptorLoader implements ApplicationContextAware {

    private static final Map<String, List<Interceptor>> INTERCEPTORS = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void setApplicationContext(final @NonNull ApplicationContext applicationContext) throws BeansException {
        Map<String, Interceptor> interceptors = applicationContext.getBeansOfType(Interceptor.class);
        if (interceptors.isEmpty()) {
            log.warn("No interceptors are configured for this application");
            return;
        }
        for (Interceptor interceptor : interceptors.values()) {
            List<Interceptor> target = INTERCEPTORS.getOrDefault(interceptor.targetGraph(), Collections.emptyList());
            target.add(interceptor);
        }
        Interceptors.merge(INTERCEPTORS);
    }

}