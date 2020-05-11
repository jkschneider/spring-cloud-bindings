/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.bindings.boot;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.cloud.bindings.Bindings;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.cloud.bindings.boot.PropertySourceContributor.contributePropertySource;

/**
 * An implementation of {@link EnvironmentPostProcessor} that generates properties from {@link Bindings} with a
 * flattened format: {@code cnb.bindings.{name}.{metadata,secret}.*}.
 */
public final class BindingFlattenedEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    public static final String BINDING_FLATTENED_PROPERTY_SOURCE_NAME = "cnbBindingFlattened";

    private final Log log = LogFactory.getLog(getClass());

    private final Bindings bindings;

    /**
     * Creates a new instance of {@code BindingFlattenedEnvironmentPostProcessor} using the {@link Bindings} available
     * in the environment.
     */
    public BindingFlattenedEnvironmentPostProcessor() {
        this(new Bindings());
    }

    BindingFlattenedEnvironmentPostProcessor(Bindings bindings) {
        this.bindings = bindings;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> properties = new HashMap<>();
        bindings.getBindings().forEach(binding -> {
            binding.getMetadata().forEach((key, value) -> {
                properties.put(String.format("cnb.bindings.%s.metadata.%s", binding.getName(), key), value);
            });
            binding.getSecret().forEach((key, value) -> {
                properties.put(String.format("cnb.bindings.%s.secret.%s", binding.getName(), key), value);
            });
        });

        if (properties.isEmpty()) {
            log.debug("No properties set from CNB Bindings. Skipping PropertySource creation.");
            return;
        }

        log.info("Creating flattened PropertySource from CNB Bindings");
        contributePropertySource(BINDING_FLATTENED_PROPERTY_SOURCE_NAME, properties, environment);
    }

    @Override
    public int getOrder() {
        // Before ConfigFileApplicationListener so values there can use values from {@link Bindings}.
        return ConfigFileApplicationListener.DEFAULT_ORDER - 1;
    }

}