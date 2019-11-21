package org.stream.extension.events;

import lombok.extern.slf4j.Slf4j;

/**
 * Events helper.
 * @author weiguanxiong
 *
 */
@Slf4j
public final class EventsHelper {

    /**
     * Fire an event through the event center. If the event center is not configure, nothing will happen except one
     * warn log.
     * @param eventCenter Target event center.
     * @param event Event to be fired.
     * @param sync Flag indicates if the event should be sent immediately.
     */
    public static void fireEvent(final EventCenter eventCenter, final Event event, final boolean sync) {
        if (eventCenter != null) {
            if (sync) {
                eventCenter.fireSyncEvent(event);
            } else {
                eventCenter.fireEvent(event);
            }
        } else {
            log.warn("Event center is not configured");
        }
    }
}
