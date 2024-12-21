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

package org.stream.extension.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.stream.core.exception.WorkFlowExecutionException;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Hessian-framework-based serializes and deserializes.
 * @author guanxiongwei
 */
@Slf4j
public final class HessianIOSerializer {

    private static final SerializerFactory SERIALIZER_FACTORY = new SerializerFactory();

    private HessianIOSerializer() { }

    /**
     * Encode the object to session style bytes.
     * @param obj Target object.
     * @return Hessian framework encoded bytes.
     */
    public static byte[] encode(final Object obj) {
        Hessian2Output out = null;
        try {
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            out = new Hessian2Output(byteArray);
            out.setSerializerFactory(SERIALIZER_FACTORY);
            out.writeObject(obj);
            out.flush();
            return byteArray.toByteArray();
        } catch (Exception e) {
            throw new WorkFlowExecutionException(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.warn("close object output stream error! {}", e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Decode object from the input bytes.
     * @param <T> Target type.
     * @param dat Input bytes.
     * @param type Target type parameter.
     * @return Decoded value from the input bytes.
     */
    public static <T> T decode(final byte[] dat, final Class<T> type) {
        Hessian2Input input = null;
        try {
            input = new Hessian2Input(new ByteArrayInputStream(dat));
            return type.cast(input.readObject());
        } catch (Exception e) {
            throw new WorkFlowExecutionException(e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    log.warn("close object output stream error! {}", e.getMessage(), e);
                }
            }
        }
    }
}

