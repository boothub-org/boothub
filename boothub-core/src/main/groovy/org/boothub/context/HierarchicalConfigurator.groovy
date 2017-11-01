/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.boothub.context

import org.boothub.Util

trait HierarchicalConfigurator<T> {
    void configureInheritedTraitsOfConfigurableTrait(T context) {
        configureInheritedTraits(context, context.getClass())
    }

    void configureInheritedTraits(T context, Class baseClass) {
        getConfigurableTraits(baseClass).each {cls -> configureTrait(context, cls)}
    }

    void configureInheritedTraitsOfConfigurableTrait(T context, Class baseTrait, List<Class> forbiddenTraits = []) {
        def concreteClass = getHighestConfigurable(context.getClass(), baseTrait, forbiddenTraits)
        if(concreteClass) {
            configureInheritedTraits(context, concreteClass)
        }
    }

    Class getHighestConfigurable(Class cls, Class baseTrait, List<Class> forbiddenTraits = []) {
        Class highestTrait = null
        Util.getAllInterfaces(cls).each { traitClass ->
            if(baseTrait.isAssignableFrom(traitClass)) {
                def forbidden = forbiddenTraits.any { forbiddenTrait -> forbiddenTrait.isAssignableFrom(traitClass)}
                if(!forbidden) {
                    def configuredBy = traitClass.getAnnotation(ConfiguredBy)
                    if(configuredBy) {
                        if(!highestTrait || highestTrait.isAssignableFrom(traitClass))
                            highestTrait = traitClass
                    }
                }
            }
        }
        highestTrait
    }

    void configureTrait(T context, Class<T> traitClass) {
        def ann = traitClass.getAnnotation(ConfiguredBy)
        if(ann) {
            def configurator = ann.value().newInstance()
            configurator.configure(context)
        }
    }

    List<Class> getConfigurableTraits(Class cls) {
        List<Class> configurableTraits = []
        collectConfigurableTraits(cls, configurableTraits)
        configurableTraits
    }

    void collectConfigurableTraits(Class cls, List<Class> configurableTraits) {
        if(cls.superclass) {
            collectConfigurableTraits(cls.superclass, configurableTraits)
        }
        Util.getConfiguredByInterfaces(cls).each { traitClass ->
            def configuredBy = traitClass.getAnnotation(ConfiguredBy)
            if(configuredBy && !configurableTraits.contains(traitClass)) {
                if(configuredBy.inheritConfigurators()) {
                    collectConfigurableTraits(traitClass, configurableTraits)
                } else {
                    configurableTraits << traitClass
                }
            }
        }
        def configuredBy = cls.getAnnotation(ConfiguredBy)
        if(configuredBy && !configurableTraits.contains(cls)) {
            configurableTraits << cls
        }
    }
}
