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

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct

/**
 * Background thread that periodically flushes sequences to database.
 */
@Component
class GormSequencePersister implements ApplicationListener<ContextClosedEvent> {

    private static final Log log = LogFactory.getLog(GormSequencePersister)

    private boolean keepGoing
    private boolean persisterRunning
    private Thread persisterThread

    private final GormSequenceGenerator sequenceGenerator
    private final GormSequenceRepository sequenceRepository

    @Value('${sequence.flushInterval:60}')
    private int flushInterval

    @Autowired
    GormSequencePersister(GormSequenceGenerator sequenceGenerator,
                          GormSequenceRepository sequenceRepository) {
        this.sequenceGenerator = sequenceGenerator
        this.sequenceRepository = sequenceRepository
    }

    @PostConstruct
    protected void initPersister() {
        def interval = 1000 * flushInterval
        persisterThread = new Thread(getClass().getSimpleName())
        persisterThread.start {
            persisterRunning = true
            keepGoing = true
            log.info "Sequence persister thread started with [$interval ms] flush interval"
            while (keepGoing) {
                try {
                    Thread.currentThread().sleep(interval)
                    if(log.isTraceEnabled()) {
                        log.trace "Scheduled flush at ${new Date()}"
                    }
                    flush()
                } catch (InterruptedException e) {
                    log.debug "Sequence flusher thread interrupted"
                } catch (Exception e) {
                    log.error "Failed to flush sequences", e
                }
            }
            persisterRunning = false
            log.info "Sequence persister thread stopped"
        }
    }

    protected void flush() {
        sequenceGenerator.flush()
    }

    protected void terminate() {
        keepGoing = false
        if (persisterThread != null) {
            persisterThread.interrupt()
            persisterThread = null
        }
        sequenceGenerator.terminate()
    }

    @Override
    void onApplicationEvent(ContextClosedEvent event) {
        try {
            terminate()
        } catch (Throwable e) {
            log.error "Unclean shutdown of ${getClass().getName()}", e
        }
    }
}
