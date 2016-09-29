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

import grails.gorm.MultiTenant
import groovy.transform.CompileStatic
import org.gr8crm.sequence.SequenceConfiguration
import org.gr8crm.sequence.SequenceStatus

/**
 * Domain class for persisting sequence definitions.
 */
class SequenceDefinition implements MultiTenant {
    Long tenantId
    String app
    String name
    String group
    String format
    Long start
    Integer increment
    static hasMany = [numbers: SequenceNumber]
    static constraints = {
        app(blank: false, maxSize: 100)
        name(blank: false, maxSize: 100)
        group(nullable: true, blank: false, maxSize: 40, unique: ['app', 'tenantId', 'name'])
        format(nullable: true, maxSize: 100)
        increment(notEqual: 0)
    }
    static transients = ['oneNumber']
    static mapping = {
        //cache true
        group column: 'sequence_group'
    }

    transient SequenceNumber getOneNumber() {
        numbers.find { it }
    }

    SequenceConfiguration toConfiguration() {
        SequenceConfiguration.builder()
                .withApp(app)
                .withTenant(tenantId)
                .withName(name)
                .withGroup(group)
                .withFormat(format)
                .withStart(start)
                //.withStart(getOneNumber()?.number ?: (start ?: 0L))
                .withIncrement(increment)
                .build()
    }

    SequenceStatus toStatus() {
        new SequenceStatus(toConfiguration(), getOneNumber()?.number ?: 0L)
    }

    @Override
    String toString() {
        StringBuilder s = new StringBuilder()
        if(tenantId != null) {
            s.append(String.valueOf(tenantId))
        }
        if(name != null) {
            s.append('.')
            s.append(name)
        }
        if(group != null) {
            s.append('.')
            s.append(group)
        }
        s.toString()
    }

    @Override
    int hashCode() {
        int hash = 17
        if (id != null) hash = hash * 17 + id * 17
        if (version != null) hash = hash * 17 + version * 17
        if (app != null) hash = hash * 17 + app.hashCode()
        if (name != null) hash = hash * 17 + name.hashCode()
        return hash
    }

    @Override
    boolean equals(other) {
        if (this.is(other)) {
            return true
        }
        if (other == null) {
            return false
        }
        if (!(other.instanceOf(SequenceDefinition))) {
            return false
        }
        if (!(this.id != null ? this.id.equals(other.id) : other.id == null)) {
            return false
        }
        if (!(this.version != null ? this.version.equals(other.version) : other.version == null)) {
            return false
        }
        if (!(this.app != null ? this.app.equals(other.app) : other.app == null)) {
            return false
        }
        if (!(this.name != null ? this.name.equals(other.name) : other.name == null)) {
            return false
        }
        return true
    }
}
