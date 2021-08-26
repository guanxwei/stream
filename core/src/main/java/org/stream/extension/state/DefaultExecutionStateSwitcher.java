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

package org.stream.extension.state;

import java.util.HashSet;
import java.util.Set;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;

/**
 * Default implement of {@link ExecutionStateSwitcher}.
 *
 * DefaultExecutionStateSwitcher will try to trace the execution step by saving execution pair information in the thread local,
 * where a execution pair is constructed by a previous node's name and a next node's name.
 * If the breaker find that a execution pair equals with the incoming pair, true will be returned by method {@link #isOpen(Node, Node, ActivityResult)}
 * otherwise false will be returned.
 * @author weiguanxiong
 *
 */
public class DefaultExecutionStateSwitcher implements ExecutionStateSwitcher {

    private ThreadLocal<Set<String>> pairs = new ThreadLocal<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen(final Node previous, final Node next, final ActivityResult activityResult) {
        String first = previous == null ? "NULL" : previous.getNodeName();
        String second = next == null ? "NULL" : next.getNodeName();
        String pair = first + second + activityResult.name();
        if (pairs.get() == null) {
            pairs.set(new HashSet<>());
        }
        if (pairs.get().contains(pair)) {
            pairs.get().clear();
            return true;
        }

        pairs.get().add(pair);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Node open(final Graph graph, final Node previous) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        pairs.get().clear();
    }

}
