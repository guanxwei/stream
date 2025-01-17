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

package org.stream.extension.events;

import org.stream.core.component.Node;
import org.stream.extension.utils.actionable.Tellme;
import org.stream.extension.utils.actionable.Value;

import lombok.Data;

/**
 * Base class to abstract different kinds of asynchronous events.
 *
 */
@Data
public abstract class Event {

    // Object that triggered this event.
    private Object trigger;

    // The time this event was triggered
    private long time;

    // Graph name this event belongs to.
    private String graph;

    // Current node.
    private String node;

    /**
     * Return the type of the event.
     * @return Event type.
     */
    public abstract String type();

    @SuppressWarnings("unchecked")
    public <T> T getTrigger() {
        return (T) this.trigger;
    }

    /**
     * Construct a new instance of event. Concrete class type is specified by the input parameter clazz.
     * @param clazz Concrete event subclass type.
     * @param trigger Object that triggers this event.
     * @param node Current node.
     * @return New instance of the target event subclass.
     */
    @SuppressWarnings("unchecked")
    public static Event of(final Class<? extends Event> clazz, final Object trigger, final Node node) {
        Value value = Value.of(null);
        Tellme.tryIt(() -> {
                    value.change(clazz.getDeclaredConstructor().newInstance());
                    Event event = value.get(clazz);
                    event.setGraph(node.getGraph().getGraphName());
                    event.setNode(node.getNodeName());
                    event.setTime(System.currentTimeMillis());
                    event.setTrigger(trigger);
                })
                .incase(Exception.class)
                .thenFix(e -> {
                    Event event = new Event() {
                        @Override
                        public String type() {
                            return "Anony";
                        }
                    };
                    event.setTime(System.currentTimeMillis());
                    event.setTrigger(trigger);
                    Tellme.when(node != null).then(() -> {
                        assert node != null;
                        event.setGraph(node.getGraph().getGraphName());
                        event.setNode(node.getNodeName());
                    });
                    value.change(event);
                });
        return value.get(clazz);
    }
}
