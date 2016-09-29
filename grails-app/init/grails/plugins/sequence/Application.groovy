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

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import grails.plugins.metadata.PluginSource
import org.gr8crm.sequence.SequenceGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@PluginSource
class Application extends GrailsAutoConfiguration {

    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    @Bean
    public GormSequenceRepository gormSequenceRepository() {
        new GormSequenceRepository()
    }

    @Bean
    @Primary
    public SequenceGenerator sequenceGenerator(GormSequenceRepository sequenceRepository) {
        new GormSequenceGenerator(sequenceRepository)
    }

    @Bean
    public GormSequencePersister gormSequencePersister(SequenceGenerator sequenceGenerator,
                                                       GormSequenceRepository sequenceRepository) {
        if (sequenceGenerator instanceof GormSequenceGenerator) {
            return new GormSequencePersister(sequenceGenerator, sequenceRepository)
        }
        throw new IllegalStateException("Expected GormSequenceGenerator but found ${sequenceGenerator.getClass().getName()}")
    }
}