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

package org.stream.extension.persist;

/**
 * Utility class to help deduce queue name based on input information.
 * @author guanxiong wei
 *
 */
public final class QueueHelper {

    public static final String RETRY_KEY = "stream_auto_scheduled_retry_set_";
    public static final String BACKUP_KEY = "stream_auto_scheduled_backup_set_";
    public static final int DEFAULT_QUEUES = 8;

    /**
     * Find the target queue that the current task should be pushed.
     * @param prefix Pre-set prefix.
     * @param application Application name.
     * @param taskID Task id.
     * @return Queue name.
     */
    public static String getQueueNameFromTaskID(final String prefix, final String application, final String taskID) {
        int hashcode = taskID.hashCode();
        int queue = hashcode % DEFAULT_QUEUES;
        if (queue < 0) {
            queue *= -1;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append(application).append("_").append(queue);
        return sb.toString();
    }

    /**
     * Find the target queue based on the index.
    * @param prefix Pre-set prefix.
     * @param application Application name.
     * @param queue Queue index.
     * @return Queue name.
     */
    public static String getQueueNameFromIndex(final String prefix, final String application, final int queue) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append(application).append("_").append(queue);
        return sb.toString();
    }

    /**
     * Get prefix based on input query type.
     * @param type query type.
     * @return queue name prefix.
     */
    public static String getPrefix(final int type) {
        switch (type) {
            case 1 :
                return RETRY_KEY;
            case 2 :
                return BACKUP_KEY;
            default :
                return "";
        }
    }
}
