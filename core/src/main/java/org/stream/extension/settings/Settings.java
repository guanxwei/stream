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

package org.stream.extension.settings;

import java.net.InetAddress;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.stream.extension.utils.actionable.Tellme;

import lombok.extern.slf4j.Slf4j;

/**
 * A setting property only class. Holding all the well-designed settings here.
 * @author guanxiongwei
 *
 */
@SuppressWarnings("unchecked")
@Slf4j
public final class Settings {

    private Settings() {}

    /**
     * Flag indicating if lua script is enabled in the redis server end.
     */
    public static final boolean LUA_SUPPORTED = Boolean.parseBoolean(System.getProperty("RedisCluster.Rua.Enabled", "true"));

    /**
     *  Lock expire time in milliseconds.
     */
    public static final int LOCK_EXPIRE_TIME = 6000;

    /**
     *  Randomly assigned host name for the running JVM.
     */
    public static final String HOST_NAME = RandomStringUtils.randomAlphabetic(32);

    /**
     * Don't have redis cluster installed in my local machine, the lua script is not verified yet, so give
     * a chance to the users to fix the script if it is not correct. Will try to test the script later, once it
     * is done will remove this tricky code.
     */
    public static final String UPDATE_EXPIRE_TIME_LUA_SCRIPT = System.getProperty("Rua.Script.Update.Expire.Time");

    /**
     * Error message when primary resource missed.
     */
    public static final String PRIMARY_MISSING_ERROR = "Auto scheduled engine does not support cases without primary resource!";

    /**
     * Preserved resource reference for AutoScheduledEngine's primary resource.
     */
    public static final String ORIGINAL_RESOURCE_REFERENCE = "stream::scheduled::workflow::primary";

    /**
     * Task reference.
     */
    public static final String TASK_REFERENCE = "stream::autoschedule::task::reference";

    /**
     * A-Synchronized executor pool size setting name.
     */
    public static final String STREAM_POOL_SIZE = "stream.async.pool.size";

    /**
     * Work flow close error message.
     */
    public static final String WORK_FLOW_CLOSE_ERROR_MESSAGE = "The work-flow instance has been closed!";

    /**
     * Auto schedule engine instance name.
     */
    public static String instance;

    static {
        Tellme.tryIt(() -> instance = InetAddress.getLocalHost().getHostName())
        .incase(Exception.class)
        .fix(e -> {
            log.warn("Failed to get machine name, will generate one for this engine instance");
            instance = UUID.randomUUID().toString();
            log.warn("Engine instance name {}", instance);
        });
    }
}
