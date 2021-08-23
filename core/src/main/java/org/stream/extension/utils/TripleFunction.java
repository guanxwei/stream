package org.stream.extension.utils;

/**
 * Function having three input argument.
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