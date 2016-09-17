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


import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import static com.upday.Preconditions.checkNotNull;


/**
 * Implementation of RxProxy that forwards a value.
 */
public class RxPublishProxy<T> implements RxProxy<T> {

    private final Collection<Callback<T>> mCallbacks = new CopyOnWriteArrayList<Callback<T>>();

    public static <T> RxPublishProxy<T> create() {
        return new RxPublishProxy<T>();
    }

    RxPublishProxy() {
    }

    @Override
    public void publish(final T value) {
        checkNotNull(value, "Value cannot be null.");

        for (Callback<T> callback : mCallbacks) {
            callback.notify(value);
        }
    }

    @Override
    public Observable<T> asObservable(final Scheduler scheduler) {
        checkNotNull(scheduler, "Scheduler cannot be null.");

        return Observable.create(new OnSubscribePublisher(scheduler, new AtomicReference<T>()));
    }

    void addCallback(final Callback<T> callback) {
        mCallbacks.add(callback);
    }

    void removeCallback(final Callback<T> callback) {
        mCallbacks.remove(callback);
    }

    final class OnSubscribePublisher implements Observable.OnSubscribe<T> {

        private final Scheduler mScheduler;

        private final AtomicReference<T> mCachedValue;

        OnSubscribePublisher(final Scheduler scheduler,
                             final AtomicReference<T> cachedValue) {
            mScheduler = checkNotNull(scheduler, "Scheduler cannot be null.");
            mCachedValue = checkNotNull(cachedValue, "Cached Value cannot be null.");
        }

        @Override
        public void call(final Subscriber<? super T> subscriber) {
            if (!subscriber.isUnsubscribed()) {
                try {
                    final ProxyProducer<T> producer = getProducer(subscriber, mCachedValue.get());
                    final Worker worker = mScheduler.createWorker();
                    subscriber.add(worker);
                    subscriber.setProducer(producer);
                    final Callback<T> listener = new Callback<T>() {
                        @Override
                        public void notify(final T value) {
                            if (!subscriber.isUnsubscribed()) {
                                worker.schedule(new Action0() {
                                    @Override
                                    public void call() {
                                        producer.offer(value);
                                    }
                                });
                            }
                        }
                    };

                    addCallback(listener);

                    subscriber.add(Subscriptions.create(new Action0() {
                        @Override
                        public void call() {
                            removeCallback(listener);
                        }
                    }));
                } catch (RuntimeException e) {
                    subscriber.onError(e);
                }
            }
        }

        private ProxyProducer<T> getProducer(final Subscriber<? super T> subscriber,
                                             final T value) {

            if (value != null) {
                return new ProxyProducer<T>(subscriber, value);
            }
            return new ProxyProducer<T>(subscriber);
        }

    }

    protected interface Callback<T> {

        void notify(T value);
    }

}
