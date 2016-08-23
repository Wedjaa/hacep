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

package it.redhat.hacep.cache.listeners;

import it.redhat.hacep.configuration.Router;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.DataRehashed;
import org.infinispan.notifications.cachelistener.event.DataRehashedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Listener(primaryOnly = true, observation = Listener.Observation.POST)
public class SessionListenerPost {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionListenerPost.class);

    private final Router router;

    public SessionListenerPost(Router router) {
        this.router = router;
    }

    @DataRehashed
    public void rehash(DataRehashedEvent event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Rehashing FINISHED for cache " + event.getCache());
        }
        this.router.resume();
    }

}