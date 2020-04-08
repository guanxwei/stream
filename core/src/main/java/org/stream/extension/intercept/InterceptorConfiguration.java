package org.stream.extension.intercept;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InterceptorConfiguration {

    @Bean
    public InterceptorLoader interceptorLoader() {
        return new InterceptorLoader();
    }
}