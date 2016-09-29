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

import grails.core.GrailsApplication
import grails.gorm.multitenancy.Tenants
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gr8crm.sequence.SequenceConfiguration
import org.gr8crm.sequence.SequenceGenerator
import org.gr8crm.sequence.SequenceStatus
import org.springframework.beans.factory.annotation.Autowired

import static grails.gorm.multitenancy.Tenants.withId

/**
 * A service that provide sequence counters (for customer numbers, invoice numbers, etc)
 * This service has two primary methods: nextNumber() and nextNumberLong().
 */
@CompileStatic
class SequenceGeneratorService {

    @Autowired
    private SequenceGenerator sequenceGenerator

    @Autowired
    private GrailsApplication grailsApplication

    @CompileDynamic
    private String getApplicationName() {
        grailsApplication.config.grails.plugins.sequence.appname ?: grailsApplication.metadata.getApplicationName()
    }

    SequenceStatus initialize(Class clazz, String group = null, Long tenant = null, Long start = null, String format = null) {
        initialize(clazz.simpleName, group, tenant, start, format)
    }

    SequenceStatus initialize(String name, String group = null, Long tenant = null, Long start = null, String format = null) {
        Long t = tenant != null ? tenant : (Long)Tenants.currentId()
        final SequenceConfiguration sequenceConfig = SequenceConfiguration.builder()
                .withApp(getApplicationName())
                .withTenant(t)
                .withName(name)
                .withGroup(group)
                .withFormat(format)
                .withStart(start != null ? start.longValue() : 1L)
                .build()

        sequenceGenerator.create(sequenceConfig)
    }

    String nextNumber(Class clazz, String group = null, Long tenant = null) {
        nextNumber(clazz.simpleName, group, tenant)
    }

    String nextNumber(String name, String group = null, Long tenant = null) {
        Long t = tenant != null ? tenant : (Long)Tenants.currentId()
        sequenceGenerator.nextNumber(getApplicationName(), t, name, group)
    }

    Long nextNumberLong(Class clazz, String group = null, Long tenant = null) {
        nextNumberLong(clazz.simpleName, group, tenant)
    }

    Long nextNumberLong(String name, String group = null, Long tenant = null) {
        Long t = tenant != null ? tenant : (Long)Tenants.currentId()
        sequenceGenerator.nextNumberLong(getApplicationName(), t, name, group)
    }

    boolean setNextNumber(Long currentNumber, Long newNumber, String name, String group = null, Long tenant = null) {
        if (currentNumber != newNumber) {
            Long t = tenant != null ? tenant : (Long)Tenants.currentId()
            if (sequenceGenerator.update(getApplicationName(), t, name, group, currentNumber, newNumber)) {
                return true
            }
        }
        return false
    }

    SequenceStatus status(String name, String group = null, Long tenant = null) {
        Long t = tenant != null ? tenant : (Long)Tenants.currentId()
        sequenceGenerator.status(getApplicationName(), t, name, group)
    }

    @CompileDynamic
    Iterable<SequenceStatus> statistics(Long tenant = null) {
        Long t = tenant != null ? tenant : (Long)Tenants.currentId()
        sequenceGenerator.statistics(getApplicationName(), t)
    }

}
