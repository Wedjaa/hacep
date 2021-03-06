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

package it.redhat.hacep.playground.event.send;

import it.redhat.hacep.model.Fact;

import javax.jms.*;

public class Sender {

    private final Session session;
    private String queueName;

    public Sender(ConnectionFactory connectionFactory, String queueName) {
        this.queueName = queueName;
        try {
            Connection connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(Fact fact) {
        try {
            Queue destination = session.createQueue(queueName);
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

            ObjectMessage message = session.createObjectMessage(fact);
            //message.setStringProperty("JMSXGroupID", String.format("P%05d", playerId));
            producer.send(message);
        } catch (Exception e) {
            System.out.println("Caught: " + e);
        }
    }

}