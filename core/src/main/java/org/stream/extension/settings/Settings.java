package org.stream.extension.settings;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * A setting property only class. Holding all the well-designed settings here.
 * @author guanxiongwei
 *
 */
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
}
