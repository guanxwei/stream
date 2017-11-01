package org.stream.extension.reader;

import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceAuthority;
import org.stream.core.resource.ResourceReader;
import org.stream.core.resource.ResourceURL;
import org.stream.extension.meta.Task;

import lombok.Setter;
import redis.clients.jedis.Jedis;

/**
 * ROA bases framework used reader to read {@link Task} from somewhere.
 * @author hzweiguanxiong
 *
 */
public class TaskReader implements ResourceReader {

    @Setter
    private Jedis jedis;

    private ResourceAuthority taskResourceAuthority = new ResourceAuthority("TaskReader", Task.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource read(final ResourceURL resourceURL) {
        String key = resourceURL.getPath();
        String content = jedis.get(key);
        Resource taskResource = Resource.builder()
                .resourceReference(key)
                .resourceURL(resourceURL)
                .value(Task.parse(content))
                .build();
        return taskResource;
    }

    @Override
    public ResourceAuthority resolve() {
        return this.taskResourceAuthority;
    }

}
