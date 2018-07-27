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

import grails.gorm.multitenancy.Tenants
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.ConcurrentHashMap

/**
 * Test spec for SequenceGeneratorService.
 */
//@Rollback
@Integration(applicationClass = Application)
class SequenceGeneratorServiceSpec extends Specification {

    @Shared
    private SequenceGeneratorService sequenceGeneratorService

    @Shared
    GormSequenceGenerator gormSequenceGenerator

    @Autowired
    void injectSequenceGenerator(GormSequenceGenerator bean) {
        this.gormSequenceGenerator = bean
    }

    @Autowired
    void injectSequenceGeneratorService(SequenceGeneratorService bean) {
        this.sequenceGeneratorService = bean
    }

    def cleanup() {
        gormSequenceGenerator.flush()
    }

    void "non-existing sequence"() {
        when:
        sequenceGeneratorService.nextNumber('notfound', 'foo', 0)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.startsWith('No such sequence')
    }

    void "test single sequence"() {
        when:
        sequenceGeneratorService.initialize('single', null, 0)

        then:
        sequenceGeneratorService.nextNumber('single', null, 0) == '1'
        sequenceGeneratorService.nextNumber('single', null, 0) == '2'
        sequenceGeneratorService.nextNumber('single', null, 0) == '3'
    }

    void "grouped sequences"() {
        given:
        sequenceGeneratorService.initialize('grouped', 'A', 0)
        sequenceGeneratorService.initialize('grouped', 'B', 0)

        when:
        sequenceGeneratorService.nextNumber('grouped', 'C', 0)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.startsWith('No such sequence')

        and:
        sequenceGeneratorService.nextNumber('grouped', 'A', 0) == '1'
        sequenceGeneratorService.nextNumber('grouped', 'A', 0) == '2'
        sequenceGeneratorService.nextNumber('grouped', 'A', 0) == '3'

        and:
        sequenceGeneratorService.nextNumber('grouped', 'B', 0) == '1'
        sequenceGeneratorService.nextNumber('grouped', 'B', 0) == '2'
        sequenceGeneratorService.nextNumber('grouped', 'B', 0) == '3'

        and:
        sequenceGeneratorService.nextNumber('grouped', 'A', 0) == '4'
        sequenceGeneratorService.nextNumber('grouped', 'B', 0) == '4'
    }

    void "multi-tenant sequences"() {
        when:
        sequenceGeneratorService.initialize('test', 'group', 1)
        sequenceGeneratorService.initialize('test', 'group', 2)
        sequenceGeneratorService.initialize('test', 'group', 3)

        then:
        sequenceGeneratorService.nextNumber('test', 'group', 1) == '1'
        sequenceGeneratorService.nextNumber('test', 'group', 1) == '2'
        sequenceGeneratorService.nextNumber('test', 'group', 1) == '3'

        and:
        sequenceGeneratorService.nextNumber('test', 'group', 2) == '1'
        sequenceGeneratorService.nextNumber('test', 'group', 2) == '2'
        sequenceGeneratorService.nextNumber('test', 'group', 2) == '3'

        and:
        sequenceGeneratorService.nextNumber('test', 'group', 3) == '1'
        sequenceGeneratorService.nextNumber('test', 'group', 3) == '2'
        sequenceGeneratorService.nextNumber('test', 'group', 3) == '3'

        and:
        sequenceGeneratorService.nextNumber('test', 'group', 1) == '4'
        sequenceGeneratorService.nextNumber('test', 'group', 2) == '4'
        sequenceGeneratorService.nextNumber('test', 'group', 3) == '4'
    }

    void "left padding number format"() {
        when:
        sequenceGeneratorService.initialize('test', 'group', 4, 1, '%04d')

        then:
        sequenceGeneratorService.nextNumber('test', 'group', 4) == '0001'
        sequenceGeneratorService.nextNumber('test', 'group', 4) == '0002'
        sequenceGeneratorService.nextNumber('test', 'group', 4) == '0003'
    }

    void "start at 0"() {
        when:
        sequenceGeneratorService.initialize('test', 'group', 5, 0, '%d')

        then:
        sequenceGeneratorService.nextNumber('test', 'group', 5) == '0'
        sequenceGeneratorService.nextNumber('test', 'group', 5) == '1'
        sequenceGeneratorService.nextNumber('test', 'group', 5) == '2'
    }

    void "start at 1000"() {
        when:
        sequenceGeneratorService.initialize('test', 'group', 6, 1000, '%d')

        then:
        sequenceGeneratorService.nextNumber('test', 'group', 6) == '1000'
        sequenceGeneratorService.nextNumber('test', 'group', 6) == '1001'
        sequenceGeneratorService.nextNumber('test', 'group', 6) == '1002'
    }

    void "mized raw and formatted numbers"() {
        when:
        sequenceGeneratorService.initialize('test', 'group', 7, 10000, '%d')

        then:
        sequenceGeneratorService.nextNumberLong('test', 'group', 7) == 10000
        sequenceGeneratorService.nextNumber('test', 'group', 7) == '10001'
        sequenceGeneratorService.nextNumberLong('test', 'group', 7) == 10002
        sequenceGeneratorService.nextNumber('test', 'group', 7) == '10003'
    }

    void "flush sequences to disc"() {
        when:
        sequenceGeneratorService.initialize('test', 'flush', 8, 1, '%d')

        then: "nothing to flush"
        gormSequenceGenerator.flush() == false

        when:
        sequenceGeneratorService.nextNumberLong('test', 'flush', 8) == 1
        sequenceGeneratorService.nextNumberLong('test', 'flush', 8) == 2
        sequenceGeneratorService.nextNumberLong('test', 'flush', 8) == 3

        then: "one sequence to flush"
        gormSequenceGenerator.flush() == true
        gormSequenceGenerator.flush() == false // A second flush will not do anything

        when: "clear all in-memory sequences to force reload from disc"
        gormSequenceGenerator.clear()

        then:
        sequenceGeneratorService.nextNumberLong('test', 'flush', 8) == 4
        sequenceGeneratorService.nextNumberLong('test', 'flush', 8) == 5
    }

    void "test SequenceEntity annotation"() {
        given:
        def entity = new SequenceTestEntity()

        when:
        entity.validate()

        then:
        entity.number == '1'
    }

    void "test performance"() {
        given:
        sequenceGeneratorService.initialize('test', 'perf', 888)

        when:
        def startTime = System.currentTimeMillis()
        1000000.times {
            sequenceGeneratorService.nextNumber('test', 'perf', 888)
        }
        println "100000 calls in ${System.currentTimeMillis() - startTime} ms"

        then:
        sequenceGeneratorService.status('test', 'perf', 888).number == 1000001
    }

    void "test multi-threading"() {
        given:
        sequenceGeneratorService.initialize("test", "threads", 888)
        final Integer cores = Runtime.getRuntime().availableProcessors()
        final Integer numberOfThreads = cores * 4
        final Integer numberOfRequests = 10000
        final Set<String> numbers = ConcurrentHashMap.newKeySet()
        final List<Thread> threads = new ArrayList<>(numberOfThreads)
        final Runnable runnable = {
            for (int i = 0; i < numberOfRequests; i++) {
                Tenants.withId(0L) {
                    numbers.add(sequenceGeneratorService.nextNumber("test", "threads", 888))
                }
            }
        } as Runnable

        when:
        println("Using " + numberOfThreads + " threads that do " + numberOfRequests + " requests each...")

        numberOfThreads.times { n ->
            threads << new Thread(runnable, "Test-${n + 1}")
        }

        threads*.start()

        threads*.join()

        then:
        numbers.size() == (numberOfThreads * numberOfRequests)
    }

    void "lots of sequences"() {
        given:
        for (t in 0..9999) {
            sequenceGeneratorService.initialize("test", "crowd", t, 0, "%d")
        }

        when:
        long total = 0L
        for (t in 3000..7999) {
            10.times {
                sequenceGeneratorService.nextNumberLong("test", "crowd", t)
            }
        }
        for (t in 0..9999) {
            total += sequenceGeneratorService.status("test", "crowd", t).number
        }

        then:
        total == 50000

    }
}
