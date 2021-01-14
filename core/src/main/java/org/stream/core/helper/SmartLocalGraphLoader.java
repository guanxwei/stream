package org.stream.core.helper;

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
        if (resources != null) {
            for (Resource resource : resources) {
                String name = resource.getFilename();
                graphFilePaths.add(name);
            }
        }
    }
}
