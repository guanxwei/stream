package org.stream.extension.executors;

import java.util.concurrent.Future;

import org.stream.core.execution.Engine;
import org.stream.core.resource.Resource;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.meta.Task;

/**
 * Virtual thread based implementation of task executor, all tasks will be executed in virtual thread.
 * Cases like RPC call can benefit from this implementation, uses less theads for tons of parallel requests.
 */
public class VirtualThreadTaskExecutor implements TaskExecutor {

    @Override
    public Future<?> submit(Resource primaryResource, Task task, StreamTransferData data, Engine engine) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'submit'");
    }

    @Override
    public Future<?> retry(String id, Engine engine) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'retry'");
    }

    @Override
    public int getActiveTasks() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getActiveTasks'");
    }

    @Override
    public int getQueuedTasks() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getQueuedTasks'");
    }

    @Override
    public int getPoolSize() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPoolSize'");
    }

    
    @Override
    public void shutDownHook() {
        throw new UnsupportedOperationException("Unimplemented method 'shutDownHook'");
    }
    
}
