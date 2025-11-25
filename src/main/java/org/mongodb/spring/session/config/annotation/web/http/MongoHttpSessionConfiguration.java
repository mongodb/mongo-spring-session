/*
 * Copyright 2025-present MongoDB, Inc.
 * Copyright 2014-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.spring.session.config.annotation.web.http;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.mongodb.spring.session.AbstractMongoSessionConverter;
import org.mongodb.spring.session.JdkMongoSessionConverter;
import org.mongodb.spring.session.MongoIndexedSessionRepository;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.lang.Nullable;
import org.springframework.session.IndexResolver;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.session.SessionIdGenerator;
import org.springframework.session.UuidSessionIdGenerator;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * Configuration class registering {@code MongoSessionRepository} bean. To import this configuration use
 * {@link EnableMongoHttpSession} annotation.
 *
 * @author Jakub Kubrynski
 * @author Eddú Meléndez
 * @since 1.2
 */
@Configuration(proxyBeanMethods = false)
@Import(SpringHttpSessionConfiguration.class)
public class MongoHttpSessionConfiguration implements BeanClassLoaderAware, EmbeddedValueResolverAware, ImportAware {

    private AbstractMongoSessionConverter mongoSessionConverter;

    private Duration maxInactiveInterval = MapSession.DEFAULT_MAX_INACTIVE_INTERVAL;

    @Nullable private String collectionName;

    @Nullable private StringValueResolver embeddedValueResolver;

    @Nullable private List<SessionRepositoryCustomizer<MongoIndexedSessionRepository>> sessionRepositoryCustomizers;

    @Nullable private ClassLoader classLoader;

    @Nullable private IndexResolver<Session> indexResolver;

    private SessionIdGenerator sessionIdGenerator = UuidSessionIdGenerator.getInstance();

    /**
     * Create and configure the {@link MongoIndexedSessionRepository} bean.
     *
     * <p>The repository will be configured with the configured converter, index resolver, collection name, session id
     * generator and any registered repository customizers.
     *
     * @param mongoOperations the {@link MongoOperations} to use for repository persistence
     * @return a configured {@link MongoIndexedSessionRepository} instance
     */
    @Bean
    @SuppressWarnings("NullAway")
    public MongoIndexedSessionRepository mongoSessionRepository(MongoOperations mongoOperations) {

        MongoIndexedSessionRepository repository = new MongoIndexedSessionRepository(mongoOperations);
        repository.setDefaultMaxInactiveInterval(this.maxInactiveInterval);

        if (this.mongoSessionConverter != null) {
            repository.setMongoSessionConverter(this.mongoSessionConverter);

            if (this.indexResolver != null) {
                this.mongoSessionConverter.setIndexResolver(this.indexResolver);
            }
        } else {
            JdkMongoSessionConverter mongoSessionConverter = new JdkMongoSessionConverter(
                    new SerializingConverter(),
                    new DeserializingConverter(this.classLoader),
                    Duration.ofSeconds(MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS));

            if (this.indexResolver != null) {
                mongoSessionConverter.setIndexResolver(this.indexResolver);
            }

            repository.setMongoSessionConverter(mongoSessionConverter);
        }

        if (StringUtils.hasText(this.collectionName)) {
            repository.setCollectionName(this.collectionName);
        }
        repository.setSessionIdGenerator(this.sessionIdGenerator);

        Assert.notNull(this.sessionRepositoryCustomizers, "SessionRepositoryCustomizers not initialized.");
        this.sessionRepositoryCustomizers.forEach(
                (sessionRepositoryCustomizer) -> sessionRepositoryCustomizer.customize(repository));

        return repository;
    }

    /**
     * Set the collection name used to store sessions in MongoDB.
     *
     * @param collectionName the collection name to use
     */
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    /**
     * Set the default maximum inactive interval for newly created sessions.
     *
     * @param maxInactiveInterval the max inactive interval to use
     */
    public void setMaxInactiveInterval(Duration maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    /**
     * Set the default maximum inactive interval in seconds.
     *
     * @param maxInactiveIntervalInSeconds the max inactive interval in seconds
     * @deprecated use {@link #setMaxInactiveInterval(Duration)} instead
     */
    @Deprecated
    @SuppressWarnings("InlineMeSuggester")
    public void setMaxInactiveIntervalInSeconds(Integer maxInactiveIntervalInSeconds) {
        setMaxInactiveInterval(Duration.ofSeconds(maxInactiveIntervalInSeconds));
    }

    @Override
    @SuppressWarnings("NullAway")
    public void setImportMetadata(AnnotationMetadata importMetadata) {

        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                importMetadata.getAnnotationAttributes(EnableMongoHttpSession.class.getName()));

        if (attributes != null) {
            this.maxInactiveInterval =
                    Duration.ofSeconds(attributes.<Integer>getNumber("maxInactiveIntervalInSeconds"));
        }

        String collectionNameValue = (attributes != null) ? attributes.getString("collectionName") : "";
        if (StringUtils.hasText(collectionNameValue)) {
            Assert.notNull(this.embeddedValueResolver, "EmbeddedValueResolver not initialized.");
            this.collectionName = this.embeddedValueResolver.resolveStringValue(collectionNameValue);
        }
    }

    /**
     * Provide a custom {@link AbstractMongoSessionConverter} to use when serializing sessions.
     *
     * @param mongoSessionConverter the converter to use; may be {@code null} to use the default converter
     */
    @Autowired(required = false)
    public void setMongoSessionConverter(AbstractMongoSessionConverter mongoSessionConverter) {
        this.mongoSessionConverter = mongoSessionConverter;
    }

    /**
     * Set customizers to be applied to the created {@link MongoIndexedSessionRepository}.
     *
     * @param sessionRepositoryCustomizers provider of repository customizers
     */
    @Autowired(required = false)
    public void setSessionRepositoryCustomizers(
            ObjectProvider<SessionRepositoryCustomizer<MongoIndexedSessionRepository>> sessionRepositoryCustomizers) {
        this.sessionRepositoryCustomizers =
                sessionRepositoryCustomizers.orderedStream().collect(Collectors.toList());
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }

    /**
     * Set a custom {@link IndexResolver} to extract index values from sessions for indexing and querying.
     *
     * @param indexResolver the index resolver to set; may be {@code null} to use default behavior
     */
    @Autowired(required = false)
    public void setIndexResolver(IndexResolver<Session> indexResolver) {
        this.indexResolver = indexResolver;
    }

    /**
     * Set the {@link SessionIdGenerator} to use when creating sessions.
     *
     * @param sessionIdGenerator the session id generator to use; must not be {@code null}
     */
    @Autowired(required = false)
    public void setSessionIdGenerator(SessionIdGenerator sessionIdGenerator) {
        this.sessionIdGenerator = sessionIdGenerator;
    }
}
