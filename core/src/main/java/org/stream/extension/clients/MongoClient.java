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
