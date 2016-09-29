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

import grails.plugins.Plugin

class SequenceGeneratorGrailsPlugin extends Plugin {

    def grailsVersion = "3.2.0 > *"
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "Sequence Number Generator"
    def author = "Göran Ehrsson"
    def authorEmail = "goran@technipelago.se"
    def description = '''\
A Grails service that generate sequence numbers from different sequences, formats, etc.
You can control the starting number, the format and you can have different sequences based on application logic.
The method getNextSequenceNumber() is injected into all domain classes annotated with @SequenceEntity.
It returns the next number for the sequence defined for the domain class.
'''
    def profiles = ['web']
    def documentation = "https://github.com/technipelago/grails-sequence-generator"
    def license = "APACHE"
    def organization = [name: "Technipelago AB", url: "http://www.technipelago.se/"]
    def issueManagement = [system: "github", url: "https://github.com/technipelago/grails-sequence-generator/issues"]
    def scm = [url: "https://github.com/technipelago/grails-sequence-generator"]
}
