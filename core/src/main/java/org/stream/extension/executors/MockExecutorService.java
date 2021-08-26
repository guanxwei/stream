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

package org.stream.extension.executors;


import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MockExecutorService implements ExecutorService {

    @Override
    public void execute(Runnable command) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void shutdown() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<Runnable> shutdownNow() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isShutdown() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isTerminated() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        try {
            task.call();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<?> submit(Runnable task) {
        task.run();
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        // TODO Auto-generated method stub
        return null;
    }

}
