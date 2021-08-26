package org.stream.extension.clients;

import java.net.UnknownHostException;
import java.util.Map;

/**
 * Abstract of MongoDB client.
 * @author guanxiongwei
 *
 */
public interface MongoClient {

    /**
     * Initiate the MongoDB client.
     * @param servers MongoDB servers.
     * @param username MongoDB login user name.
     * @param db Database instance name.
     * @param password Password.
     * @throws UnknownHostException UnknownHostException
     */
    void init(final Map<String, Integer> servers, final String username, final String db, final String password) throws UnknownHostException;

    /**
     * Save an new entity in MongoDB.
     * @param key MongoDB key.
     * @param object Object to be saved.
     * @param collectionName Collection's name.
     * @return Manipulation result.
     */
    boolean save(final String key, final Object object, final String collectionName);
}
