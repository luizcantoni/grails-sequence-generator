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
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.gr8crm.sequence.SequenceConfiguration
import org.gr8crm.sequence.SequenceGenerator
import org.gr8crm.sequence.SequenceStatus
import org.springframework.stereotype.Component

import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Stream

import static grails.gorm.multitenancy.Tenants.withId

/**
 * A sequence generator that persist sequences in database using GORM.
 */
@Component
@CompileStatic
class GormSequenceGenerator implements SequenceGenerator {

    private static final Log log = LogFactory.getLog(GormSequenceGenerator)

    private final Map<SequenceKey, Sequence> sequences = new ConcurrentHashMap<>()
    private final Set<SequenceKey> dirtySequences = new ConcurrentHashMap<>().newKeySet()

    private final GormSequenceRepository sequenceRepository

    GormSequenceGenerator(GormSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository
    }

    private SequenceKey key(String app, long tenant, String name, String group) {
        Objects.requireNonNull(app, "application name must be specified")
        Objects.requireNonNull(name, "sequence name must be specified")
        new SequenceKey(app, tenant, name, group)
    }

    private Sequence getSequence(SequenceKey key) {
        Sequence sequence = sequences[key]
        if (sequence == null) {
            final SequenceNumber number = sequenceRepository.getNumber(key.app, key.tenant, key.name, key.group)
            if (number == null) {
                throw new IllegalArgumentException("No such sequence: $key")
            }
            final SequenceDefinition definition = number.definition
            sequence = new Sequence(number.number, definition.increment, definition.format)
            sequences[key] = sequence
        }
        sequence
    }

    private SequenceConfiguration getConfiguration(String app, long tenant, String name, String group) {
        def definition = sequenceRepository.getDefinition(app, tenant, name, group)
        if (definition == null) {
            throw new IllegalArgumentException("No such sequence: $app, $tenant, $name, $group")
        }
        definition.toConfiguration()
    }

    public boolean flush() {
        Set<SequenceKey> copy
        synchronized (dirtySequences) {
            copy = new HashSet<>(dirtySequences)
            dirtySequences.clear()
        }

        if(copy.isEmpty()) {
            return false
        }

        if(log.isDebugEnabled()) {
            log.debug "Flushing ${copy.size()} dirty sequences"
        }

        for (SequenceKey key in copy) {
            Sequence sequence = sequences[key]
            if (sequence != null) {
                sequenceRepository.save(key.app, key.tenant, key.name, key.group, sequence.snapshot())
                if(log.isTraceEnabled()) {
                    log.trace "Successfully flushed sequence $key"
                }
            }
        }
        true
    }

    public void terminate() {
        log.info "Terminating ${getClass().getName()}"
        flush()
        clear()
    }

    public void clear() {
        dirtySequences.clear()
        sequences.clear()
    }

    @Override
    SequenceStatus create(SequenceConfiguration configuration) {
        final SequenceKey key = key(configuration.getApp(), configuration.getTenant(),
                configuration.getName(), configuration.getGroup())
        def definition = sequenceRepository.getDefinition(configuration.getApp(), configuration.getTenant(),
                configuration.getName(), configuration.getGroup())
        if (definition == null) {
            sequenceRepository.createDefinition(
                    configuration.getApp(),
                    configuration.getTenant(),
                    configuration.getName(),
                    configuration.getGroup(),
                    configuration.getFormat(),
                    configuration.getStart(),
                    configuration.getIncrement()
            )
        }

        final Sequence sequence = getSequence(key)
        new SequenceStatus(configuration, sequence.getNumber())
    }

    @Override
    boolean delete(String app, long tenant, String name, String group) {
        sequenceRepository.delete(app, tenant, name, group)
        true
    }

    @Override
    String nextNumber(String app, long tenant, String name, String group) {
        final SequenceKey key = key(app, tenant, name, group)
        final Sequence sequence = getSequence(key)
        String result = sequence.nextFormatted()
        dirtySequences.add(key)
        return result
    }

    @Override
    long nextNumberLong(String app, long tenant, String name, String group) {
        final SequenceKey key = key(app, tenant, name, group)
        final Sequence sequence = getSequence(key)
        long result = sequence.next()
        dirtySequences.add(key)
        return result
    }

    @Override
    SequenceStatus update(String app, long tenant, String name, String group, long before, long after) {
        final SequenceKey key = key(app, tenant, name, group)
        final Sequence sequence = getSequence(key)
        if (sequence.getNumber() == before) {
            sequence.setNumber(after)
            if(log.isDebugEnabled()) {
                log.debug "Sequence $key updated from $before to $after"
            }
            dirtySequences.add(key)
        }
        new SequenceStatus(getConfiguration(app, tenant, name, group), sequence.getNumber())
    }

    @Override
    SequenceStatus status(String app, long tenant, String name, String group) {
        final SequenceKey key = key(app, tenant, name, group)
        final Sequence sequence = getSequence(key)
        new SequenceStatus(getConfiguration(app, tenant, name, group), sequence.getNumber())
    }

    @Override
    Stream<SequenceStatus> statistics(String app, long tenant) {
        withId(tenant) {
            List<SequenceDefinition> result = SequenceDefinition.createCriteria().list() {
                eq('app', app)
                order('name', 'asc')
                order('group', 'asc')
            } as List<SequenceDefinition>
            result.collect { it.toStatus() }.stream()
        }
    }

    @Override
    void shutdown() {
        // The persister thread handle flushing of sequences at application context shutdown.
    }

}
