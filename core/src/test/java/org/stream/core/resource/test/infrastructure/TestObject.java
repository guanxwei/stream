package org.stream.core.resource.test.infrastructure;

import lombok.Getter;

public class TestObject {

    @Getter
    private String value;

    public TestObject(String value) {
        this.value = value;
    }
}
