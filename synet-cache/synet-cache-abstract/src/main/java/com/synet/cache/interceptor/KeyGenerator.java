package com.synet.cache.interceptor;

import java.lang.reflect.Method;

@FunctionalInterface
public interface KeyGenerator {

    /**
     * Generate a key for the given method and its parameters.
     * @param target the target instance
     * @param method the method being called
     * @param params the method parameters (with any var-args expanded)
     * @return a generated key
     */
    Object generate(Object target, Method method, Object... params);
}
