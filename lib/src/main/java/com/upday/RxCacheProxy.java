/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 upday GmbH & Co. KG
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.upday;

import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Scheduler;

import static com.upday.Preconditions.checkNotNull;

/**
 * Implementation of IRxProxy that must have initial value.
 */
public final class RxCacheProxy<T> extends RxPublishProxy<T> {

    private final AtomicReference<T> mCachedValue = new AtomicReference<>();

    public static <T> RxCacheProxy<T> create(final T defaultValue) {
        checkNotNull(defaultValue, "Default value cannot be null.");

        return new RxCacheProxy<>(defaultValue);
    }

    public static <T> RxCacheProxy<T> create() {
        return new RxCacheProxy<>(null);
    }

    private RxCacheProxy(final T value) {
        mCachedValue.set(value);
    }

    @Override
    public void publish(final T value) {
        checkNotNull(value, "Value cannot be null.");

        mCachedValue.set(value);
        super.publish(value);
    }

    @Override
    public Observable<T> asObservable(final Scheduler scheduler) {
        checkNotNull(scheduler, "Scheduler cannot be null.");

        return Observable.create(new OnSubscribePublisher(scheduler, mCachedValue));
    }

    /**
     * Returns {@code true} when the proxy already has a stored value.
     *
     * @return {@code true} when the proxy already has a stored value
     */
    public boolean hasValue() {
        return mCachedValue.get() != null;
    }

    /**
     * Returns the last value.
     * Will return {@code null} if there is no value yet
     *
     * @return the last value that can be {@code null}
     */
    public T getValue() {
        return mCachedValue.get();
    }
}
