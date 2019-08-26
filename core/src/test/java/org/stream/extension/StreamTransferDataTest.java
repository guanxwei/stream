package org.stream.extension;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.stream.extension.io.HessianIOSerializer;
import org.stream.extension.io.StreamTransferData;
import org.testng.annotations.Test;

public class StreamTransferDataTest {

    @Test
    public void test() {
        StreamTransferData streamTransferData = StreamTransferData.succeed();
        byte[] bytes = HessianIOSerializer.encode(streamTransferData);

        StreamTransferData decoded = HessianIOSerializer.decode(bytes, StreamTransferData.class);

        assertEquals(decoded.getActivityResult(), streamTransferData.getActivityResult());
    }

    @Test
    public void testEmpty() {
        StreamTransferData streamTransferData = new StreamTransferData();
        byte[] bytes = HessianIOSerializer.encode(streamTransferData);

        StreamTransferData decoded = HessianIOSerializer.decode(bytes, StreamTransferData.class);

        assertNotNull(decoded);
    }
}
