# RxProxy

RxProxy is a simple way of creating Observables that can be fed values in a non-reactive way.

### Build Status
[![Build Status](https://travis-ci.org/upday/RxProxy.svg?branch=master)](https://travis-ci.org/upday/RxProxy) [![codecov.io](http://codecov.io/github/upday/RxProxy/coverage.svg?branch=master)](http://codecov.io/github/upday/RxProxy?branch=master)

## Usage
Declare `RxProxy` as a member variable:

    private final RxProxy<String> mTextStream = RxPublishProxy.create();
    

Publish on the proxy:

    private void changeText(final String text) {
        mTextStream.publish(text);
    }

Subscribe for the changes:

    mTextStream.asObservable(Schedulers.computation())
               .filter(this::isValidText)
               .observeOn(AndroidSchedulers.mainThread())
               .subscribe(mTextView::setText);
    
## Issues with Subjects
Subjects are really useful, but we have noticed two issues with them:

* Publication and subscription do happen on the same thread. It does not matter if you are using `subscribeOn`. The subscription will be executed on the same thread from which `onNext` was called. This can be mitigated with `observeOn` but it changes the original pattern of the monad. `RxProxy` requires subscribers to define which `Scheduler` is used when receiving events from the proxy.
* One can get confused when using `onComplete` and `onError`. Those can leave Subjects in a unusable state without making the user aware. In `RxProxy` a stream cannot be finished or report an error on its own. `RxProxy` just contains a `publish` method that is the equivalent of `onNext`.
* Most Subjects do not support back-pressure. They immediately report an error whenever more items are delivered than requested. In `RxProxy`, each implementation has a small dedicated buffer that in the future we can control.

## Credits
The idea for the project was inspired by reading a great series of blogs on [RxJava](http://akarnokd.blogspot.de/) by  [Dávid Karnok](https://plus.google.com/113316559156085910174/posts).

License
-------

    The MIT License (MIT)
    
    Copyright (c) 2016 upday GmbH & Co. KG

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
