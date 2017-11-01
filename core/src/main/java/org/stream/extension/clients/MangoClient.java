package org.stream.extension.clients;

import java.net.UnknownHostException;
import java.util.Map;

/**
 * Abstract of MangoDB client.
 * @author 魏冠雄
 *
 */
public interface MangoClient {

    /**
     * Initiate the MangoDB client.
     * @param servers MangoDB servers.
     * @param username MangoDB login user name.
     * @param db Database instance name.
     * @param password Password.
     * @throws UnknownHostException UnknownHostException
     */
    void init(final Map<String, Integer> servers, final String username, final String db, final String password) throws UnknownHostException;

    /**
     * Save an new entity in MangoDB.
     * @param key MangoDB key.
     * @param object Object to be saved.
     * @param collectionName Collection's name.
     * @return Manipulation result.
     */
    boolean save(final String key, final Object object, final String collectionName);
}
