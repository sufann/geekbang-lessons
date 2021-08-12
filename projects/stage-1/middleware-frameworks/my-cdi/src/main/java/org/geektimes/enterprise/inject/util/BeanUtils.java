/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.geektimes.enterprise.inject.util;

import org.geektimes.commons.reflect.util.ClassUtils;

import javax.decorator.Decorator;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

import static java.lang.Integer.compare;
import static java.lang.String.format;
import static java.util.stream.Stream.of;
import static org.geektimes.commons.util.AnnotationUtils.isAnnotated;

/**
 * Bean Utilities class
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public abstract class BeanUtils {

    /**
     * Bean Class - Constructor Cache
     * default access for testing
     */
    static final ConcurrentMap<Class<?>, Constructor> beanConstructorsCache = new ConcurrentHashMap<>();

    /**
     * Constructor's parameter count descent
     */
    private static final Comparator<Constructor> CONSTRUCTOR_PARAM_COUNT_COMPARATOR =
            (a, b) -> compare(b.getParameterCount(), a.getParameterCount());

    private BeanUtils() {
        throw new IllegalStateException("BeanUtils should not be instantiated!");
    }

    /**
     * A Java class is a managed bean if it meets all of the following conditions:
     * <p>
     * It is not an inner class.
     * <p>
     * It is a non-abstract class, or is annotated @Decorator.
     * <p>
     * It does not implement javax.enterprise.inject.spi.Extension.
     * <p>
     * It is not annotated @Vetoed or in a package annotated @Vetoed.
     * <p>
     * It has an appropriate constructor - either:
     * <p>
     * the class has a constructor with no parameters, or
     * <p>
     * the class declares a constructor annotated @Inject.
     *
     * @param beanClass the type of bean
     * @throws DefinitionException if the bean class does not meet above conditions
     */
    public static void validateManagedBean(Class<?> beanClass) throws DefinitionException {
        // It is not an inner class.
        validate(beanClass, ClassUtils::isTopLevelClass, "The Bean Class must not be an inner class!");
        // It is a non-abstract class, or is annotated @Decorator.
        validate(beanClass, ClassUtils::isConcreteClass, "The Bean Class must be a concrete class!");
        Predicate<Class<?>> validator = type -> !isAnnotatedDecorator(type);
        validate(beanClass, validator, "The Bean Class must not annotated @Decorator!");
        // It does not implement javax.enterprise.inject.spi.Extension
        validator = type -> !isExtensionClass(type);
        validate(beanClass, validator, "The Bean Class must not not implement javax.enterprise.inject.spi.Extension!");
        // It is not annotated @Vetoed or in a package annotated @Vetoed.
        validator = type -> !isAnnotatedVetoed(type) && !isAnnotatedVetoed(type.getPackage());
        validate(beanClass, validator, "The Bean Class must not annotated @Vetoed or in a package annotated @Vetoed!");
        // It has an appropriate constructor
        findAppropriateConstructor(beanClass);
    }

    public static Constructor<?> findAppropriateConstructor(Class<?> beanClass) {
        return beanConstructorsCache.computeIfAbsent(beanClass, type -> of(type.getConstructors())
                .sorted(CONSTRUCTOR_PARAM_COUNT_COMPARATOR) // parameter count descent
                .filter(c -> c.getParameterCount() == 0 || c.isAnnotationPresent(Inject.class))
                .findFirst()
                .orElseThrow(() -> new DefinitionException(
                        format("The beanClass[%s] does not have a constructor with no parameters, " +
                                        "or the class declares a constructor annotated @Inject",
                                beanClass.getName())))
        );
    }

    public static boolean isAnnotatedDecorator(Class<?> type) {
        return isAnnotated(type, Decorator.class);
    }

    public static boolean isAnnotatedVetoed(AnnotatedElement annotatedElement) {
        return isAnnotated(annotatedElement, Vetoed.class);
    }

    public static boolean isExtensionClass(Class<?> type) {
        return Extension.class.isAssignableFrom(type);
    }

    static <T> void validate(T target, Predicate<T> validator, String errorMessage) {
        if (!validator.test(target)) {
            throw new DefinitionException(errorMessage);
        }
    }

}
