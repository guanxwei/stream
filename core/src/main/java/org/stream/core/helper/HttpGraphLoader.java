package org.stream.core.helper;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.stream.core.exception.GraphLoadException;

/**
 * A enhanced graph loader that be used to load or reload graphs that changes can be done in run-time.
 */
public class HttpGraphLoader extends AbstractGraphLoader {

    /**
     * {@inheritDoc}
     */
    @Override
    protected InputStream loadInputStream(final String sourcePath) throws GraphLoadException {
        try {
            URL url = new URL(sourcePath);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(3000);
            httpURLConnection.connect();
            int code = httpURLConnection.getResponseCode();
            if (code != 200) {
                throw new GraphLoadException("Fail to connection to remote server");
            }
            return httpURLConnection.getInputStream();
        } catch (MalformedURLException e) {
            throw new GraphLoadException("Unavaliable uri", e);
        } catch (IOException e) {
            throw new GraphLoadException("Fail to connection to remote server", e);
        }
    }

}
