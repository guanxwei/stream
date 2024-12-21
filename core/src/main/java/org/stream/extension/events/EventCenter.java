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

import java.util.List;

/**
 * Event center to gather and dispatch events.
 * @author guanxiong wei
 *
 */
public interface EventCenter {

    /**
     * Fire an event asynchronously.
     * @param event Event to be fired.
     */
    void fireEvent(final Event event);

    /**
     * Fire an event synchronously.
     * @param event Event to be fired.
     */
    void fireSyncEvent(final Event event);

    /**
     * Register the event listener.
     * @param event Event type that the listener interested in.
     * @param listener Event listener.
     */
    void registerListener(final Class<? extends Event> event, final Listener listener);

    /**
     * Remove a listener from the event center.
     * @param listener Listener to be removed.
     */
    void removeListener(final Listener listener);

    /**
     * Register a listener interested in multi kinds of events.
     * @param events Event class list the listener is interested in.
     * @param listener Listener to be registered.
     */
    void registerMultiChannelListener(final List<Class<? extends Event>> events, final Listener listener);

    /**
     * Get a listener list by event type.
     * @param type Event type.
     * @return Listener list.
     */
    List<Listener> getListenerListByEventType(final Class<?> type);
}
