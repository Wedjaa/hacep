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

import it.redhat.hacep.cache.listeners.FactListenerPost;
import it.redhat.hacep.cache.listeners.SessionListenerPost;
import it.redhat.hacep.cache.listeners.SessionListenerPre;
import it.redhat.hacep.cache.session.KieSessionSaver;
import it.redhat.hacep.configuration.annotations.HACEPCacheManager;
import it.redhat.hacep.configuration.annotations.HACEPFactCache;
import it.redhat.hacep.configuration.annotations.HACEPSessionCache;
import it.redhat.hacep.model.Fact;
import it.redhat.hacep.model.Key;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class HACEPApplication {

    @Inject
    @HACEPCacheManager
    private DefaultCacheManager manager;

    @Inject
    @HACEPFactCache
    private Cache<Key, Fact> factCache;

    @Inject
    @HACEPSessionCache
    private Cache<Key, Object> sessionCache;

    @Inject
    private Router router;

    @Inject
    private KieSessionSaver kieSessionSaver;

    public HACEPApplication() {
    }

    public void start() {
        try {
            this.factCache.addListener(new FactListenerPost(this.kieSessionSaver));
            this.sessionCache.addListener(new SessionListenerPre(this.router));
            this.sessionCache.addListener(new SessionListenerPost(this.router));

            this.router.start();
            this.manager.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            this.router.stop();
            this.manager.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void suspend() {
        this.router.suspend();
    }

    public void resume() {
        this.router.resume();
    }

    public Cache<Key, Fact> getFactCache() {
        return factCache;
    }

    public DefaultCacheManager getCacheManager() {
        return manager;
    }

}
