package org.stream.extension;

import static org.testng.Assert.assertEquals;

import org.stream.extension.io.HessianIOSerializer;
import org.stream.extension.io.StreamTransferData;
import org.testng.annotations.Test;

public class HessianIOSerializerTest {

    @Test
    public void test() {
        StreamTransferData data = StreamTransferData.succeed();
        data.add("primaryClass", String.class.getName());
        byte[] bytes = HessianIOSerializer.encode(data);
        StreamTransferData transferData = HessianIOSerializer.decode(bytes, StreamTransferData.class);
        assertEquals(data.getActivityResult(), transferData.getActivityResult());
        assertEquals(data.get("primaryClass"), transferData.get("primaryClass"));
    }
}
