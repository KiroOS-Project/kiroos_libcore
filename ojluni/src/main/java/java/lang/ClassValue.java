/*
 * Copyright (C) 2026 The KiroOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.lang;

import java.util.WeakHashMap;

/**
 * Lazily associates a computed value with (potentially) every type.
 *
 * KiroOS: part of the platform since Android 14 (API 34). Libraries built for
 * current Java (kotlinx.coroutines' exception constructor cache, for one) touch
 * it on any Android version; on Android 7 the missing class threw
 * NoClassDefFoundError during app startup (3DMark stuck on its splash screen).
 * This is a small, thread-safe implementation of the same contract: one value
 * per class, computed once by {@link #computeValue}, cached until
 * {@link #remove}. Classes are held weakly. Hidden from the SDK API.
 *
 * @param <T> the type of the derived value
 * @since 1.7
 * @hide
 */
public abstract class ClassValue<T> {
    /** Stands for a computed null, so "absent" and "null" stay apart. */
    private static final Object NULL_VALUE = new Object();

    /** Guarded by itself. */
    private final WeakHashMap<Class<?>, Object> values = new WeakHashMap<Class<?>, Object>();

    /**
     * Sole constructor. (For invocation by subclass constructors, typically
     * implicit.)
     */
    protected ClassValue() {
    }

    /**
     * Computes the given class's derived value for this {@code ClassValue}.
     *
     * @param type the type whose class value must be computed
     * @return the newly computed value associated with this {@code ClassValue}, for the given
     *         class or interface
     */
    protected abstract T computeValue(Class<?> type);

    /**
     * Returns the value for the given class. If no value has yet been computed, it is obtained
     * by an invocation of {@link #computeValue}. When two threads compute a value for the same
     * class at the same time, both get the value that was stored first.
     *
     * @param type the type whose class value must be computed or retrieved
     * @return the current value associated with this {@code ClassValue}, for the given class
     *         or interface
     * @throws NullPointerException if the argument is null
     */
    public T get(Class<?> type) {
        if (type == null) {
            throw new NullPointerException();
        }
        synchronized (values) {
            Object v = values.get(type);
            if (v != null) {
                return unwrap(v);
            }
        }
        // Outside the lock: computeValue may ask this ClassValue for other classes.
        T computed = computeValue(type);
        synchronized (values) {
            Object v = values.get(type);
            if (v != null) {
                return unwrap(v);
            }
            values.put(type, computed == null ? NULL_VALUE : computed);
            return computed;
        }
    }

    /**
     * Removes the associated value for the given class. A later {@link #get} for the same class
     * computes it again.
     *
     * @param type the type whose class value must be removed
     * @throws NullPointerException if the argument is null
     */
    public void remove(Class<?> type) {
        if (type == null) {
            throw new NullPointerException();
        }
        synchronized (values) {
            values.remove(type);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T unwrap(Object v) {
        return v == NULL_VALUE ? null : (T) v;
    }
}
