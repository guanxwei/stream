package org.stream.extension.intercept;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InterceptorLoader implements ApplicationContextAware {

    private static final Map<String, List<Interceptor>> INTERCEPTORS = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Interceptor> interceptors = applicationContext.getBeansOfType(Interceptor.class);
        if (interceptors.isEmpty()) {
            log.warn("No interceptors are configured for this application");
            return;
        }
        for (Interceptor interceptor : interceptors.values()) {
            if (!INTERCEPTORS.containsKey(interceptor.targetGraph())) {
                INTERCEPTORS.put(interceptor.targetGraph(), new LinkedList<>());
            }
            List<Interceptor> target = INTERCEPTORS.get(interceptor.targetGraph());
            target.add(interceptor);
        }
    }
    
}