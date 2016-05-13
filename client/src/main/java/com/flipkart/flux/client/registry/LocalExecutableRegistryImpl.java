/*
 * Copyright 2012-2016, the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.flipkart.flux.client.registry;

import com.flipkart.flux.client.intercept.MethodId;
import com.flipkart.flux.client.intercept.UnknownIdentifierException;
import com.google.inject.Injector;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is used to retrieve executables stored locally in the same JVM
 * @author yogesh.nachnani
 */
@Singleton
public class LocalExecutableRegistryImpl implements ExecutableRegistry {
    private final Map<String,Method> identifierToMethodMap;
    private final Injector injector;

    @Inject
    public LocalExecutableRegistryImpl(Injector injector) {
        this(new ConcurrentHashMap<>(),injector);
    }

    public LocalExecutableRegistryImpl(Map<String, Method> identifierToMethodMap,Injector injector) {
        this.identifierToMethodMap = identifierToMethodMap  ;
        this.injector = injector;
    }

    @Override
    public Method getTask(String taskIdentifier) {
        final Method cachedMethod = this.identifierToMethodMap.get(taskIdentifier);
        if (cachedMethod == null) {
            try {
                final MethodId methodId = MethodId.fromIdentifier(taskIdentifier);
                final Object classInstance = this.injector.getInstance(Class.forName(methodId.getClassName()));
                return classInstance.getClass().getDeclaredMethod(methodId.getMethodName(), methodId.getParameterTypes());
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                throw new UnknownIdentifierException("Could not load method corresponding to the given task identifier:" + taskIdentifier);
            }
        }
        return cachedMethod;
    }

    @Override
    public Method getHook(String hookIdentifier) {
        return null;
    }

    @Override
    public void registerTask(String taskIdentifier, Method method) {
        this.identifierToMethodMap.put(taskIdentifier,method);
    }
}