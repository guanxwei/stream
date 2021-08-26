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
