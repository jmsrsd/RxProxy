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

import org.junit.Test;

import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;

public class RxCacheProxyTest {

    @Test
    public void testContainsInitialValue_WhenCreatedWithValue() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        RxCacheProxy<Integer> proxy = RxCacheProxy.create(1);

        proxy.asObservable(Schedulers.computation())
             .subscribe(ts);

        ts.assertValue(1);
    }

    @Test
    public void testContainsNoInitialValue_WhenCreatedWithoutValue() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        RxCacheProxy<Integer> proxy = RxCacheProxy.create();

        proxy.asObservable(Schedulers.computation())
             .subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
    }

    @Test
    public void testUpdatesValueOfNextSubscriber() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        RxCacheProxy<Integer> proxy = RxCacheProxy.create(1);
        proxy.publish(2);

        proxy.asObservable(Schedulers.computation())
             .subscribe(ts);

        ts.assertValue(2);
    }

    @Test
    public void testPublishRespectsBackPressure_AndEmitsJustOneRequestedItem() {
        RxCacheProxy<Integer> proxy = RxCacheProxy.create(1);
        TestSubscriber<Integer> ts = new TestSubscriber<>(1);
        proxy.asObservable(Schedulers.computation()).subscribe(ts);

        proxy.publish(1);
        proxy.publish(2);

        ts.assertValueCount(1);
    }

    @Test
    public void testHasNoStoredValue_WhenCreatedWithoutOne() {
        RxCacheProxy<Integer> proxy = RxCacheProxy.create();

        assertThat(proxy.hasValue()).isFalse();
    }

    @Test
    public void testGetValue_ReturnsNone_WhenCreatedWithoutValue() {
        RxCacheProxy<Integer> proxy = RxCacheProxy.create();

        assertThat(proxy.getValue()).isEqualTo(null);
    }

    @Test
    public void testHasStoredValue_WhenCreatedWithOne() {
        RxCacheProxy<Integer> proxy = RxCacheProxy.create(1);

        assertThat(proxy.hasValue()).isTrue();
    }

    @Test
    public void testGetValue_ReturnsLastValue_WhenCreatedWithOne() {
        RxCacheProxy<Integer> proxy = RxCacheProxy.create(1);

        assertThat(proxy.getValue()).isEqualTo(1);
    }

    @Test
    public void testGetValue_ReturnsLastValue_WhenPublished() {
        RxCacheProxy<Integer> proxy = RxCacheProxy.create();

        proxy.publish(2);

        assertThat(proxy.getValue()).isEqualTo(2);
    }

    @Test
    public void testHasStoredValue_WhenPublished() {
        RxCacheProxy<Integer> proxy = RxCacheProxy.create();

        proxy.publish(2);

        assertThat(proxy.hasValue()).isTrue();
    }

    @Test(expected = NullPointerException.class)
    public void testCreate_DoesNotAcceptNullAsDefaultValue() {
        RxCacheProxy.create(null);
    }

}
