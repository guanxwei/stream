package org.stream.extension.clients;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import lombok.Getter;
import lombok.Setter;

/**
 * Default implementation of {@linkplain MangoClient}
 * @author 魏冠雄
 *
 */
@Getter
@Setter
public class MangoClientImpl implements MangoClient {

    private MongoClient mango;
    private MongoDatabase db;
    private String dbName;

    private List<ServerAddress> addrs;
    private MongoCredential credential;

    private boolean autoRetry = true;
    private int connections = 50;
    private int threads = 50;
    private int waitTime = 1000 * 60 * 2;
    private int timeOut = 1000 * 60 * 1;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean save(final String key, final Object object, final String collectionName) {
        MongoCollection<Document> collection = getCollection(collectionName);
        Document document = new Document();
        document.put(key, object);
        try {
            collection.insertOne(document);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final Map<String, Integer> servers, final String username, final String dbName, final String password) throws UnknownHostException {
        List<ServerAddress> addresses = new ArrayList<ServerAddress>();
        for (Entry<String, Integer> entry : servers.entrySet()) {
            ServerAddress serverAddress = new ServerAddress(entry.getKey(), entry.getValue());
            addresses.add(serverAddress);
        }
        this.dbName = dbName;
        this.addrs = addresses;

        MongoCredential credential = MongoCredential.createCredential(username, dbName, password.toCharArray());
        this.credential = credential;

        mango = new MongoClient(addresses, credential, option());
        this.db = mango.getDatabase(dbName);
    }

    private MongoClientOptions option() {
        MongoClientOptions.Builder build = new MongoClientOptions.Builder();
        build.connectionsPerHost(connections);
        build.threadsAllowedToBlockForConnectionMultiplier(threads);
        build.maxWaitTime(waitTime);
        build.connectTimeout(timeOut);

        return build.build();
    }

    private MongoCollection<Document> getCollection(final String collectionName) {
        return db.getCollection(collectionName);
    }
}
