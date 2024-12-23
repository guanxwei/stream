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

package org.stream.extension.utils;

/**
 * Function having three input arguments.
 * @author guanxiongwei
 *
 * @param <T> The first function argument type.
 * @param <U> The second function argument type.
 * @param <R> The third function argument type.
 * @param <S> The function result type.
 */
@FunctionalInterface
public interface TripleFunction<T, U, R, S> {

    /**
     * Applies this function to the given arguments.
     *
     * @param t the first function argument
     * @param u the second function argument
     * @param r the third function argument
     * @return the function result
     */
    S apply(T t, U u, R r);
}