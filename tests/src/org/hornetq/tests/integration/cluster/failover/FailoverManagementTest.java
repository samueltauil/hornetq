/*
 * JBoss, Home of Professional Open Source Copyright 2005-2008, Red Hat
 * Middleware LLC, and individual contributors by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of individual
 * contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package org.hornetq.tests.integration.cluster.failover;

import static org.hornetq.core.config.impl.ConfigurationImpl.DEFAULT_MANAGEMENT_ADDRESS;

import java.util.HashMap;
import java.util.Map;

import org.hornetq.core.client.ClientConsumer;
import org.hornetq.core.client.ClientMessage;
import org.hornetq.core.client.ClientProducer;
import org.hornetq.core.client.ClientSession;
import org.hornetq.core.client.impl.ClientMessageImpl;
import org.hornetq.core.client.impl.ClientSessionFactoryImpl;
import org.hornetq.core.client.impl.ClientSessionFactoryInternal;
import org.hornetq.core.client.impl.ClientSessionImpl;
import org.hornetq.core.client.impl.ClientSessionInternal;
import org.hornetq.core.client.management.impl.ManagementHelper;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.TransportConfiguration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.exception.MessagingException;
import org.hornetq.core.logging.Logger;
import org.hornetq.core.management.ResourceNames;
import org.hornetq.core.remoting.RemotingConnection;
import org.hornetq.core.remoting.impl.invm.InVMRegistry;
import org.hornetq.core.remoting.impl.invm.TransportConstants;
import org.hornetq.core.server.Messaging;
import org.hornetq.core.server.MessagingServer;
import org.hornetq.tests.util.UnitTestCase;
import org.hornetq.utils.SimpleString;

/**
 * 
 * A FailoverManagementTest
 *
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * 
 * Created 5 Nov 2008 15:05:14
 *
 *
 */
public class FailoverManagementTest extends UnitTestCase
{
   private static final Logger log = Logger.getLogger(FailoverManagementTest.class);

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private static final SimpleString ADDRESS = new SimpleString("FailoverTestAddress");

   private MessagingServer liveService;

   private MessagingServer backupService;

   private Map<String, Object> backupParams = new HashMap<String, Object>();

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public void testManagementMessages() throws Exception
   {            
      ClientSessionFactoryInternal sf1 = new ClientSessionFactoryImpl(new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory"),
                                                                      new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory",
                                                                                                 backupParams));
      
      sf1.setProducerWindowSize(32 * 1024);
  
      ClientSession session1 = sf1.createSession(false, true, true);

      session1.createQueue(ADDRESS, ADDRESS, null, false);
      
      SimpleString replyTo = new SimpleString("replyto");
      
      session1.createQueue(replyTo, new SimpleString("replyto"), null, false);
      
      ClientProducer producer = session1.createProducer(ADDRESS);
      
      final int numMessages = 10;
      
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage msg  = session1.createClientMessage(false);
         
         producer.send(msg);
      }
      
      for (int i = 0; i < numMessages / 2; i++)
      {
         ClientMessage managementMessage  = session1.createClientMessage(false);
         
         ManagementHelper.putAttribute(managementMessage,
                                        ResourceNames.CORE_QUEUE + ADDRESS,
                                        "messageCount");
         managementMessage.putStringProperty(ClientMessageImpl.REPLYTO_HEADER_NAME, replyTo);
         
         producer.send(DEFAULT_MANAGEMENT_ADDRESS, managementMessage);
      }
                            
      ClientConsumer consumer1 = session1.createConsumer(replyTo);
                 
      final RemotingConnection conn1 = ((ClientSessionInternal)session1).getConnection();
 
      conn1.fail(new MessagingException(MessagingException.NOT_CONNECTED));
      
      //Send the other half
      for (int i = 0; i < numMessages / 2; i++)
      {
         ClientMessage managementMessage  = session1.createClientMessage(false);
         
         ManagementHelper.putAttribute(managementMessage,
                                        ResourceNames.CORE_QUEUE + ADDRESS,
                                        "messageCount");
         managementMessage.putStringProperty(ClientMessageImpl.REPLYTO_HEADER_NAME, replyTo);
         
         producer.send(DEFAULT_MANAGEMENT_ADDRESS, managementMessage);
      }
            
      session1.start();
                   
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer1.receive(1000);
         
         assertNotNull(message);
                        
         message.acknowledge();
         
         assertTrue(ManagementHelper.isAttributesResult(message));
         
         assertEquals(numMessages, ManagementHelper.getResult(message));
      }
      
      session1.close();
      
      //Make sure no more messages
      ClientSession session2 = sf1.createSession(false, true, true);
      
      session2.start();
      
      ClientConsumer consumer2 = session2.createConsumer(replyTo);
      
      ClientMessage message = consumer2.receive(1000);
      
      assertNull(message);
      
      session2.close();      
   }
   
   public void testManagementMessages2() throws Exception
   {            
      ClientSessionFactoryInternal sf1 = new ClientSessionFactoryImpl(new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory"),
                                                                      new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory",
                                                                                                 backupParams));
      
      sf1.setProducerWindowSize(32 * 1024);
  
      ClientSession session1 = sf1.createSession(false, true, true);

      session1.createQueue(ADDRESS, ADDRESS, null, false);
      
      SimpleString replyTo = new SimpleString("replyto");
      
      session1.createQueue(replyTo, new SimpleString("replyto"), null, false);
      
      ClientProducer producer = session1.createProducer(ADDRESS);
      
      final int numMessages = 10;
      
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage msg  = session1.createClientMessage(false);
         
         producer.send(msg);
      }
      
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage managementMessage  = session1.createClientMessage(false);
         
         ManagementHelper.putAttribute(managementMessage,
                                        ResourceNames.CORE_QUEUE + ADDRESS,
                                        "messageCount");
         managementMessage.putStringProperty(ClientMessageImpl.REPLYTO_HEADER_NAME, replyTo);
         
         producer.send(DEFAULT_MANAGEMENT_ADDRESS, managementMessage);
      }
                            
      ClientConsumer consumer1 = session1.createConsumer(replyTo);
                       
                      
      session1.start();
                   
      for (int i = 0; i < numMessages; i++)
      {
         ClientMessage message = consumer1.receive(1000);
         
         assertNotNull(message);
         
         if (i == 0)
         {
            //Fail after receipt but before ack
            final RemotingConnection conn1 = ((ClientSessionInternal)session1).getConnection();
            
            conn1.fail(new MessagingException(MessagingException.NOT_CONNECTED));
         }
                        
         message.acknowledge();
         
         assertTrue(ManagementHelper.isAttributesResult(message));
         
         assertEquals(numMessages, ManagementHelper.getResult(message));
      }
      
      session1.close();
      
      //Make sure no more messages
      ClientSession session2 = sf1.createSession(false, true, true);
      
      session2.start();
      
      ClientConsumer consumer2 = session2.createConsumer(replyTo);
      
      ClientMessage message = consumer2.receive(1000);
      
      assertNull(message);
      
      session2.close();      
   }
   
   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      
      Configuration backupConf = new ConfigurationImpl();
      backupConf.setSecurityEnabled(false);
      backupParams.put(TransportConstants.SERVER_ID_PROP_NAME, 1);
      backupConf.getAcceptorConfigurations()
                .add(new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory",
                                                backupParams));
      backupConf.setBackup(true);
      backupService = Messaging.newMessagingServer(backupConf, false);
      backupService.start();

      Configuration liveConf = new ConfigurationImpl();
      liveConf.setSecurityEnabled(false);
      liveConf.getAcceptorConfigurations()
              .add(new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory"));
      Map<String, TransportConfiguration> connectors = new HashMap<String, TransportConfiguration>();
      TransportConfiguration backupTC = new TransportConfiguration("org.hornetq.core.remoting.impl.invm.InVMConnectorFactory",
                                                                   backupParams, "backup-connector");
      connectors.put(backupTC.getName(), backupTC);
      liveConf.setConnectorConfigurations(connectors);
      liveConf.setBackupConnectorName(backupTC.getName());
      liveService = Messaging.newMessagingServer(liveConf, false);
      liveService.start();
   }

   @Override
   protected void tearDown() throws Exception
   {
      backupService.stop();

      liveService.stop();

      assertEquals(0, InVMRegistry.instance.size());
      
      backupService = null;
      
      liveService = null;
      
      backupParams = null;
      
      super.tearDown();
   }

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}

