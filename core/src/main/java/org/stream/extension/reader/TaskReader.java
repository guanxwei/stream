package org.stream.extension.reader;

import org.stream.core.resource.AbstractResourceReader;
import org.stream.core.resource.ResourceAuthority;
import org.stream.core.resource.ResourceURL;
import org.stream.extension.meta.Task;
import org.stream.extension.persist.TaskStorage;

import lombok.Setter;

/**
 * ROA bases framework used reader to read {@link Task} from somewhere.
 * @author guanxiong wei
 *
 */
public class TaskReader extends AbstractResourceReader {

    @Setter
    private TaskStorage taskStorage;

    private ResourceAuthority taskResourceAuthority = new ResourceAuthority("TaskReader", Task.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceAuthority resolve() {
        return this.taskResourceAuthority;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object doRead(final ResourceURL resourceURL) {
        String key = resourceURL.getPath();
        Task task = taskStorage.query(key);
        return task;
    }

}
