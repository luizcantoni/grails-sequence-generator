/*
 * Copyright (c) 2016 Goran Ehrsson.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.plugins.sequence

import groovy.transform.CompileStatic

import java.util.concurrent.atomic.AtomicLong

/**
 * Created by goran on 2016-09-18.
 */
@CompileStatic
class Sequence {

    private final AtomicLong number

    private boolean dirty
    private int increment
    private String format

    Sequence(long start, int increment, String format) {
        this.number = new AtomicLong(start)
        this.increment = increment
        this.format = format
    }

    public long getNumber() {
        this.@number.get()
    }

    public void setNumber(long n) {
        this.@number.set(n)
        dirty = true
    }

    public long next() {
        long n = this.@number.getAndAdd(increment);
        dirty = true
        return n
    }

    public String nextFormatted() {
        String.format(format ?: '%d', next())
    }

    public String toString() {
        String.format(format ?: '%d', getNumber())
    }

    public boolean isDirty() {
        dirty
    }

    public long snapshot() {
        dirty = false
        getNumber()
    }
}
