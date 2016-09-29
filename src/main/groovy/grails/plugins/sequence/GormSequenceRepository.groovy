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

import org.springframework.stereotype.Component

import static grails.gorm.multitenancy.Tenants.withId
import static grails.gorm.multitenancy.Tenants.withoutId

/**
 * A repository responsible for loading and saving sequences from database.
 */
@Component
class GormSequenceRepository {

    SequenceDefinition createDefinition(final String app, final long tenant, final String name, final String group,
                                        String format, long start, int increment) {
        withId(tenant) {
            SequenceDefinition definition = new SequenceDefinition(app: app, tenantId: tenant, name: name, group: group,
                    format: format, start: start, increment: increment)
            definition.addToNumbers(new SequenceNumber(definition: definition, number: start))
            definition.save(flush: true, failOnError: true)
        }
    }

    SequenceDefinition getDefinition(final String app, final long tenant, final String name, final String group) {
        withId(tenant) {
            SequenceDefinition.createCriteria().get() {
                eq('app', app)
                eq('name', name)
                if (group != null) {
                    eq('group', group)
                } else {
                    isNull('group');
                }
            }
        } as SequenceDefinition
    }

    List<SequenceDefinition> getDefinitions(final String app, final Long tenant) {
        List<SequenceDefinition> result = []
        if (tenant != null) {
            withId(tenant) {
                result.addAll(SequenceDefinition.findAllByApp(app))
            }
        } else {
            withoutId {
                result.addAll(SequenceDefinition.findAllByApp(app, [sort: 'tenantId', order: 'asc']))
            }
        }
        result
    }

    SequenceNumber getNumber(final String app, final long tenant, final String name, final String group) {
        withId(tenant) {
            SequenceNumber.createCriteria().get() {
                definition {
                    eq('app', app)
                    eq('tenantId', tenant)
                    eq('name', name)
                    if (group != null) {
                        eq('group', group)
                    } else {
                        isNull('group');
                    }
                }
            }
        } as SequenceNumber
    }

    SequenceNumber save(final String app, final long tenant, final String name, final String group, final long number) {
        withId(tenant) {
            final SequenceNumber sequenceNumber = getNumber(app, tenant, name, group)
            if (sequenceNumber == null) {
                throw new IllegalArgumentException("No such sequence: $app, $tenant, $name, $group")
            }
            sequenceNumber.number = number
            sequenceNumber.save(flush: true, failOnError: true)
        }
    }

    void delete(final String app, final long tenant, final String name, final String group) {
        withId(tenant) {
            final SequenceNumber sequenceNumber = getNumber(app, tenant, name, group)
            if (sequenceNumber == null) {
                throw new IllegalArgumentException("No such sequence: $app, $tenant, $name, $group")
            }
            sequenceNumber.delete(flush: true, failOnError: true)
        }
    }
}
