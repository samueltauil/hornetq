/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hornetq.tests.integration.stomp.v11;
import org.junit.Before;
import org.junit.After;

import java.util.HashMap;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.HornetQServers;
import org.hornetq.jms.client.HornetQJMSConnectionFactory;
import org.hornetq.jms.server.JMSServerManager;
import org.hornetq.jms.server.config.JMSConfiguration;
import org.hornetq.jms.server.config.impl.JMSConfigurationImpl;
import org.hornetq.jms.server.config.impl.JMSQueueConfigurationImpl;
import org.hornetq.jms.server.config.impl.TopicConfigurationImpl;
import org.hornetq.jms.server.impl.JMSServerManagerImpl;
import org.hornetq.spi.core.protocol.ProtocolType;
import org.hornetq.tests.unit.util.InVMContext;
import org.hornetq.tests.util.UnitTestCase;

public abstract class StompV11TestBase extends UnitTestCase
{
   protected String hostname = "127.0.0.1";

   protected int port = 61613;

   private ConnectionFactory connectionFactory;

   private Connection connection;

   protected Session session;

   protected Queue queue;

   protected Topic topic;

   protected JMSServerManager server;

   protected String defUser = "brianm";

   protected String defPass = "wombats";

   protected boolean persistenceEnabled = false;

   // Implementation methods
   // -------------------------------------------------------------------------
   @Override
   @Before
   public void setUp() throws Exception
   {
      super.setUp();

      server = createServer();
      server.start();
      connectionFactory = createConnectionFactory();

      connection = connectionFactory.createConnection();
      session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      queue = session.createQueue(getQueueName());
      topic = session.createTopic(getTopicName());
      connection.start();
   }

   /**
   * @return
   * @throws Exception
   */
   protected JMSServerManager createServer() throws Exception
   {
      Configuration config = createBasicConfig();
      config.setSecurityEnabled(false);
      config.setPersistenceEnabled(persistenceEnabled);

      Map<String, Object> params = new HashMap<String, Object>();
      params.put(TransportConstants.PROTOCOL_PROP_NAME, ProtocolType.STOMP.toString());
      params.put(TransportConstants.PORT_PROP_NAME, TransportConstants.DEFAULT_STOMP_PORT);
      params.put(TransportConstants.STOMP_CONSUMERS_CREDIT, "-1");
      TransportConfiguration stompTransport = new TransportConfiguration(NettyAcceptorFactory.class.getName(), params);
      config.getAcceptorConfigurations().add(stompTransport);
      config.getAcceptorConfigurations().add(new TransportConfiguration(InVMAcceptorFactory.class.getName()));
      HornetQServer hornetQServer = addServer(HornetQServers.newHornetQServer(config, defUser, defPass));

      JMSConfiguration jmsConfig = new JMSConfigurationImpl();
      jmsConfig.getQueueConfigurations()
               .add(new JMSQueueConfigurationImpl(getQueueName(), null, true, getQueueName()));
      jmsConfig.getTopicConfigurations().add(new TopicConfigurationImpl(getTopicName(), getTopicName()));
      server = new JMSServerManagerImpl(hornetQServer, jmsConfig);
      server.setContext(new InVMContext());
      return server;
   }

   @Override
   @After
   public void tearDown() throws Exception
   {
      try
      {
         if (connection != null)
            connection.close();
         if (server != null)
            server.stop();
      }
      finally
      {
         super.tearDown();
      }
   }

   protected ConnectionFactory createConnectionFactory()
   {
      return new HornetQJMSConnectionFactory(false, new TransportConfiguration(InVMConnectorFactory.class.getName()));
   }

   protected String getQueueName()
   {
      return "test";
   }

   protected String getQueuePrefix()
   {
      return "jms.queue.";
   }

   protected String getTopicName()
   {
      return "testtopic";
   }

   protected String getTopicPrefix()
   {
      return "jms.topic.";
   }

   public void sendMessage(String msg) throws Exception
   {
      sendMessage(msg, queue);
   }

   public void sendMessage(String msg, Destination destination) throws Exception
   {
      MessageProducer producer = session.createProducer(destination);
      TextMessage message = session.createTextMessage(msg);
      producer.send(message);
   }

   public void sendMessage(byte[] data, Destination destination) throws Exception
   {
      sendMessage(data, "foo", "xyz", destination);
   }

   public void sendMessage(String msg, String propertyName, String propertyValue) throws Exception
   {
      sendMessage(msg.getBytes("UTF-8"), propertyName, propertyValue, queue);
   }

   public void sendMessage(byte[] data, String propertyName, String propertyValue, Destination destination) throws Exception
   {
      MessageProducer producer = session.createProducer(destination);
      BytesMessage message = session.createBytesMessage();
      message.setStringProperty(propertyName, propertyValue);
      message.writeBytes(data);
      producer.send(message);
   }

}
