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
public class TaskReader extends AbstractResourceReader<Task> {

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
    protected Task doRead(final ResourceURL resourceURL) {
        String key = resourceURL.getPath();
        return taskStorage.query(key);
    }

    @Override
    protected String constructResourceReference(ResourceURL resourceURL) {
        return "Workflow::Task::" + resourceURL.getPath();
    }

}
