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

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import rx.Producer;
import rx.Subscriber;
import rx.internal.operators.BackpressureUtils;
import rx.internal.util.unsafe.SpscLinkedQueue;

import static com.upday.Preconditions.checkNotNull;

/**
 * Producer that supplies values to RxProxy.
 *
 * The code is based on a non-blocking queue implementation from RxJava.
 */
final class ProxyProducer<T> extends AtomicLong implements Producer {

    private static final long serialVersionUID = 31845635407556628L;

    private final Subscriber<? super T> mSubscriber;

    private final Queue<T> mQueue = new SpscLinkedQueue<>();

    /**
     * Using the Integer as the possibility of overflow is really small and might
     * even indicate a bug in subscriber's implementation.
     */
    private final AtomicInteger mWip = new AtomicInteger();

    ProxyProducer(final Subscriber<? super T> subscriber) {
        checkNotNull(subscriber, "Subscriber cannot be null.");

        mSubscriber = subscriber;
    }

    ProxyProducer(final Subscriber<? super T> subscriber, final T initialValue) {
        this(subscriber);

        offer(checkNotNull(initialValue, "Initial value cannot be null."));
    }

    @Override
    public void request(final long n) {
        if (n < 0) {
            throw new IllegalArgumentException("Invalid requested amount.");
        }

        if (n > 0) {
            BackpressureUtils.getAndAddRequest(this, n);
            drain();
        }
    }

    void offer(final T value) {
        checkNotNull(value, "Value cannot be null.");

        mQueue.offer(value);
        drain();
    }

    /**
     * Drains and emits values from queue in a thread-safe non-blocking way.
     */
    @SuppressWarnings("NestedAssignment")
    private void drain() {
        if (mWip.getAndIncrement() == 0) {
            do {
                if (mSubscriber.isUnsubscribed()) {
                    return;
                }

                mWip.lazySet(1);

                long requested = get();
                long emitted = 0;
                T value;

                while (requested != 0 && (value = mQueue.poll()) != null) {
                    mSubscriber.onNext(value);
                    if (mSubscriber.isUnsubscribed()) {
                        return;
                    }
                    ++requested;
                    ++emitted;
                }
                if (emitted != 0) {
                    addAndGet(-emitted);
                }

            } while (mWip.decrementAndGet() != 0);
        }
    }

}
