package org.stream.extension.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.stream.core.exception.WorkFlowExecutionExeception;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Hessian framework based serialize and de-serialize.
 */
@Slf4j
public final class HessianIOSerializer {

    private static final SerializerFactory SERIALIZER_FACTORY = new SerializerFactory();

    private HessianIOSerializer() { }

    /**
     * Encode the object to sessian style bytes.
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
            throw new WorkFlowExecutionExeception(e);
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
            throw new WorkFlowExecutionExeception(e);
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

