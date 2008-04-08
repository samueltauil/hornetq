/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.messaging.core.remoting.impl.wireformat;

import static org.jboss.messaging.core.remoting.impl.wireformat.PacketType.SESS_BROWSER_NEXTMESSAGEBLOCK_RESP;

import java.util.Arrays;

import org.jboss.messaging.core.message.Message;

/**
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 * 
 * @version <tt>$Revision$</tt>
 */
public class SessionBrowserNextMessageBlockResponseMessage extends PacketImpl
{
   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private final Message[] messages;

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   public SessionBrowserNextMessageBlockResponseMessage(final Message[] messages)
   {
      super(SESS_BROWSER_NEXTMESSAGEBLOCK_RESP);

      assert messages != null;

      this.messages = messages;
   }

   // Public --------------------------------------------------------

   public Message[] getMessages()
   {
      return messages;
   }

   @Override
   public String toString()
   {
      return getParentString() + ", messages=" + Arrays.asList(messages) + "]";
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}
