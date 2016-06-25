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

import rx.Observable;
import rx.Scheduler;

/**
 * Should be instead of {@link rx.subjects.Subject} when one wants to use
 * different {@link Scheduler} while receiving the events.
 * The implementation of the interface has to support back-pressure.
 */
public interface RxProxy<T> {

    /**
     * Publishes next value to the proxy.
     *
     * @param value non null value that will be published
     */
    void publish(T value);

    /**
     * Returns a stream of proxied values on the {@link Scheduler}.
     * The subscription itself happens on the scheduler
     * defined in {@link Observable#onSubscribe} but the
     * notification from {@link this#publish(Object)} is
     * moved to the provided {@link Scheduler}.
     *
     * @return a non null stream of published values on the {@link Scheduler}
     */
    Observable<T> asObservable(Scheduler scheduler);

}
