package org.stream.extension.io;

/**
 * Utility class providing standard Stream Transfer Data Status.
 * @author guanxiong wei
 *
 */
public final class StreamTransferDataStatus {

    private StreamTransferDataStatus() { }

    /**
     * Success result.
     */
    public static final String SUCCESS = "SUCCESS";

    /**
     * Fail result.
     */
    public static final String FAIL = "FAIL";

    /**
     * Result unknown after the node is executed, sometimes extra effort need to be taken to check if everything is okay.
     */
    public static final String UNKNOWN = "UNKNOWN";

    /**
     * Unknown result, maybe the current work flow should be suspended and tried again later.
     */
    public static final String SUSPEND = "SUSPEND";

    
}
