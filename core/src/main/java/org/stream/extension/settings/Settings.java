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
     *  Lock expire time in milliseconds.
     */
    public static final int LOCK_EXPIRE_TIME = 6000;

    /**
     *  Randomly assigned host name for the running JVM.
     */
    public static final String HOST_NAME = RandomStringUtils.randomAlphabetic(32);

}
