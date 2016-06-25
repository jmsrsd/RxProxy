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

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import rx.observers.TestSubscriber;

import static java.lang.Thread.currentThread;
import static org.assertj.core.api.Assertions.assertThat;
import static rx.schedulers.Schedulers.computation;
import static rx.schedulers.Schedulers.immediate;

public class RxPublishProxyTest {

    private RxPublishProxy<Integer> mProxy;

    @Before
    public void setUp() {
        mProxy = RxPublishProxy.create();
    }

    @Test
    public void publish_IsCalledOnDifferentThread() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        mProxy.asObservable(computation())
              .first()
              .subscribe(ts);

        mProxy.publish(1);

        ts.awaitTerminalEvent(100, TimeUnit.MILLISECONDS);
        ts.assertTerminalEvent();
        assertThat(ts.getLastSeenThread()).isNotEqualTo(currentThread());
    }

    @Test
    public void publish_NotifiesSubscriber() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        mProxy.asObservable(computation())
              .first()
              .subscribe(ts);

        mProxy.publish(1);

        ts.awaitTerminalEvent(100, TimeUnit.MILLISECONDS);
        ts.assertTerminalEvent();
        ts.assertValue(1);
    }

    @Test
    public void publish_NotifiesAllValues() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        mProxy.asObservable(computation())
              .take(3)
              .subscribe(ts);

        mProxy.publish(1);
        mProxy.publish(2);
        mProxy.publish(3);

        ts.awaitTerminalEvent(100, TimeUnit.MILLISECONDS);
        ts.assertTerminalEvent();
        ts.assertValueCount(3);
    }

    @Test
    public void monadCallsArePerformedOnScheduledThread() {
        final AtomicReference<Thread> onNextThread = new AtomicReference<>();
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        mProxy.asObservable(computation())
              .doOnNext(__ -> onNextThread
                      .set(currentThread()))
              .first()
              .subscribe(ts);

        mProxy.publish(1);

        ts.awaitTerminalEvent(100, TimeUnit.MILLISECONDS);
        ts.assertTerminalEvent();
        assertThat(onNextThread.get()).isNotEqualTo(currentThread());
    }

    @Test
    public void publishRespectsBackPressure_AndEmitsJustOneRequestedItem() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(1);
        mProxy.asObservable(immediate()).subscribe(ts);

        mProxy.publish(1);
        mProxy.publish(2);

        ts.assertValueCount(1);
    }

    @Test
    public void publishRespectsBackPressure_DoesNotEmmitValues_WhenNotRequested() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        mProxy.asObservable(immediate()).subscribe(ts);

        ts.assertNoValues();
    }

    @Test
    public void publishRespectsBackPressure_DoesNotComplete_WhenNotRequested() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();

        mProxy.asObservable(immediate()).subscribe(ts);

        ts.assertNoTerminalEvent();
    }

    @Test
    public void proxy_DoesNotReceiveValues_WhenZeroValuesRequested() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        mProxy.asObservable(immediate()).subscribe(ts);

        ts.requestMore(0);

        ts.assertValueCount(0);
    }

    @Test
    public void proxy_DoesNotReceiveValues_WhenUnsubscribed() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        mProxy.asObservable(immediate()).subscribe(ts).unsubscribe();

        ts.requestMore(1);

        ts.assertValueCount(0);
    }

}
