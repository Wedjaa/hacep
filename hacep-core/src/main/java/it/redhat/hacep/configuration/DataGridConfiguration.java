/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.redhat.hacep.configuration;

import it.redhat.hacep.cache.session.HAKieSerializedSession;
import it.redhat.hacep.cache.session.HAKieSession;
import it.redhat.hacep.cache.session.HAKieSessionDeltaEmpty;
import it.redhat.hacep.cache.session.HAKieSessionDeltaFact;
import it.redhat.hacep.configuration.annotations.*;
import it.redhat.hacep.drools.KieSessionByteArraySerializer;
import it.redhat.hacep.model.Fact;
import it.redhat.hacep.model.Key;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class DataGridConfiguration {

    private final static Logger LOGGER = LoggerFactory.getLogger(DataGridConfiguration.class);

    private static final String FACT_CACHE_NAME = "fact";
    private static final String SESSION_CACHE_NAME = "session";

    private DefaultCacheManager manager;

    private ExecutorService executorService;

    private KieSessionByteArraySerializer serializer;

    @Inject
    private DroolsConfiguration droolsConfiguration;

    public DataGridConfiguration() {
    }

    @PostConstruct
    private void build() {
        executorService = Executors.newFixedThreadPool(4);
        serializer = new KieSessionByteArraySerializer(droolsConfiguration);

        GlobalConfiguration globalConfiguration = new GlobalConfigurationBuilder().clusteredDefault()
                .transport().addProperty("configurationFile", System.getProperty("jgroups.configuration", "jgroups-tcp.xml"))
                .clusterName("HACEP")
                .globalJmxStatistics().allowDuplicateDomains(true).enable()
                .serialization()
                .addAdvancedExternalizer(new HAKieSession.HASessionExternalizer(droolsConfiguration, serializer, executorService))
                .addAdvancedExternalizer(new HAKieSerializedSession.HASerializedSessionExternalizer(droolsConfiguration, serializer, executorService))
                .addAdvancedExternalizer(new HAKieSessionDeltaEmpty.HASessionDeltaEmptyExternalizer(droolsConfiguration, serializer, executorService))
                .addAdvancedExternalizer(new HAKieSessionDeltaFact.HASessionDeltaFactExternalizer())
                .build();

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        CacheMode cacheMode = getCacheMode();
        if (cacheMode.isDistributed()) {
            configurationBuilder
                    .clustering().cacheMode(cacheMode)
                    .hash().numOwners(getNumOwners())
                    .groups().enabled();
        } else {
            configurationBuilder.clustering().cacheMode(cacheMode);
        }

        Configuration defaultConfiguration = configurationBuilder.build();

        ConfigurationBuilder factCacheConfigurationBuilder = new ConfigurationBuilder().read(defaultConfiguration);
        factCacheConfigurationBuilder.expiration().maxIdle(500, TimeUnit.MILLISECONDS);

        this.manager = new DefaultCacheManager(globalConfiguration, defaultConfiguration, false);
        this.manager.defineConfiguration(FACT_CACHE_NAME, factCacheConfigurationBuilder.build());
    }

    @Produces
    @HACEPFactCache
    public Cache<Key, Fact> getFactCache() {
        return this.manager.getCache(FACT_CACHE_NAME, true);
    }

    @Produces
    @HACEPSessionCache
    public Cache<Key, Object> getSessionCache() {
        return this.manager.getCache(SESSION_CACHE_NAME, true);
    }

    @Produces
    @HACEPCacheManager
    public DefaultCacheManager getManager() {
        return manager;
    }

    @Produces
    @HACEPKieSessionSerializer
    public KieSessionByteArraySerializer getSerializer() {
        return serializer;
    }

    @Produces
    @HACEPExecutorService
    public ExecutorService getExecutorService() {
        return executorService;
    }

    private CacheMode getCacheMode() {
        try {
            return CacheMode.valueOf(System.getProperty("grid.mode", "DIST_SYNC"));
        } catch (IllegalArgumentException e) {
            return CacheMode.DIST_SYNC;
        }
    }

    private int getNumOwners() {
        try {
            return Integer.valueOf(System.getProperty("grid.owners", "2"));
        } catch (IllegalArgumentException e) {
            return 2;
        }
    }

}
