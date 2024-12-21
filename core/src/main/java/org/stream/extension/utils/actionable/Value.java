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

package org.stream.extension.utils.actionable;

/**
 * A value.
 * @author guanxiongwei
 *
 */
public final class Value {

    private Object value;

    /**
     * Constructor.
     * @param value Real value.
     */
    private Value(final Object value) {
        this.value = value;
    }

    /**
     * Create a new value instance.
     * @param value Real value object.
     * @return Value instance.
     */
    public static Value of(final Object value) {
        return new Value(value);
    }

    /**
     * Get the real value.
     * @param <T> Target class type.
     * @param clazz Real value's type
     * @return Real value.
     */
    public <T> T get(final Class<T> clazz) {
        return clazz.cast(this.value);
    }

    /**
     * Change the value.
     * @param value Value to replace the current one.
     * @return this value.
     */
    public Value change(final Object value) {
        this.value = value;
        return this;
    }
}
