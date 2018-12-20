package org.stream.extension.io;

import org.stream.core.component.ActorActivity;
import org.stream.core.execution.AutoScheduledEngine;

/**
 * Abstract of "actors" to be used to communicate with the remote services by {@link AutoScheduledEngine}. Every actor should be
 * placed in a {@link ActorActivity} instance so that {@link AutoScheduledEngine} can invoke them.
 *
 * When using {@link AutoScheduledEngine}, planty of services will be arranged cooperating doing one thing in the single procedure.
 * Normally one RPC framework or something like that will be used, clients use the predefine interface to communicate with the remove service,
 * which role will be placed by {@link AutoScheduledEngine}. In each step, remote service's response status will be used to determine what to do
 * at the next step. To eliminate the effort writing the patterned code, user can use this interface as their own base interface and the Herd
 * framework will do some well designed work, for example merge the response into the work-flow level {@link StreamTransferData} so that successors
 * can use them freely.
 * 
 *
 * @author hzweiguanxiong
 *
 */
public interface Actor<T> {

    /**
     * Call the remote service to complete the sub-task. The herd framework will use the response to decide what to do in the next step, and help
     * merge the return value in to the work-flow branch.
     * @param request Request to be sent to the remote service.
     * @return Herd framework defined transfer data.
     */
    StreamTransferData call(final T request);
}
