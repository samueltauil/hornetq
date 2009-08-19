/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005-2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.hornetq.tests.stress.journal.remote;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.journal.LoaderCallback;
import org.hornetq.core.journal.PreparedTransactionInfo;
import org.hornetq.core.journal.RecordInfo;
import org.hornetq.core.journal.SequentialFileFactory;
import org.hornetq.core.journal.impl.AIOSequentialFileFactory;
import org.hornetq.core.journal.impl.JournalImpl;
import org.hornetq.core.journal.impl.NIOSequentialFileFactory;

/**
 * 
 * @author <a href="mailto:clebert.suconic@jboss.com">Clebert Suconic</a>
 *
 */
public class RemoteJournalAppender
{

   // Constants -----------------------------------------------------

   public static final int OK = 10;

   // Attributes ----------------------------------------------------

   // Static --------------------------------------------------------

   public static void main(String args[]) throws Exception
   {

      if (args.length != 5)
      {
         System.err.println("Use: java -cp <classpath> " + RemoteJournalAppender.class.getCanonicalName() +
                            " aio|nio <journalDirectory> <NumberOfElements> <TransactionSize> <NumberOfThreads>");
         System.exit(-1);
      }
      System.out.println("Running");
      String journalType = args[0];
      String journalDir = args[1];
      long numberOfElements = Long.parseLong(args[2]);
      int transactionSize = Integer.parseInt(args[3]);
      int numberOfThreads = Integer.parseInt(args[4]);

      try
      {
         appendData(journalType, journalDir, numberOfElements, transactionSize, numberOfThreads);

      }
      catch (Exception e)
      {
         e.printStackTrace(System.out);
         System.exit(-1);
      }

      System.exit(OK);
   }

   public static JournalImpl appendData(String journalType,
                                        String journalDir,
                                        long numberOfElements,
                                        int transactionSize,
                                        int numberOfThreads) throws Exception
   {
      final JournalImpl journal = createJournal(journalType, journalDir);

      journal.start();
      journal.load(new LoaderCallback()
      {

         public void addPreparedTransaction(PreparedTransactionInfo preparedTransaction)
         {
         }

         public void addRecord(RecordInfo info)
         {
         }

         public void deleteRecord(long id)
         {
         }

         public void updateRecord(RecordInfo info)
         {
         }
      });

      LocalThreads threads[] = new LocalThreads[numberOfThreads];
      final AtomicLong sequenceTransaction = new AtomicLong();

      for (int i = 0; i < numberOfThreads; i++)
      {
         threads[i] = new LocalThreads(journal, numberOfElements, transactionSize, sequenceTransaction);
         threads[i].start();
      }

      Exception e = null;
      for (LocalThreads t : threads)
      {
         t.join();

         if (t.e != null)
         {
            e = t.e;
         }
      }

      if (e != null)
      {
         throw e;
      }

      return journal;
   }

   public static JournalImpl createJournal(String journalType, String journalDir)
   {
      JournalImpl journal = new JournalImpl(10485760,
                                            2,
                                            0,
                                            0,
                                            getFactory(journalType, journalDir),
                                            "journaltst",
                                            "tst",
                                            500);
      return journal;
   }

   public static SequentialFileFactory getFactory(String factoryType, String directory)
   {
      if (factoryType.equals("aio"))
      {
         return new AIOSequentialFileFactory(directory,
                                             ConfigurationImpl.DEFAULT_JOURNAL_AIO_BUFFER_SIZE,
                                             ConfigurationImpl.DEFAULT_JOURNAL_AIO_BUFFER_TIMEOUT,
                                             ConfigurationImpl.DEFAULT_JOURNAL_AIO_FLUSH_SYNC,
                                             false);
      }
      else
      {
         return new NIOSequentialFileFactory(directory);
      }
   }

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

   static class LocalThreads extends Thread
   {
      final JournalImpl journal;

      final long numberOfElements;

      final int transactionSize;

      final AtomicLong nextID;

      Exception e;

      public LocalThreads(JournalImpl journal, long numberOfElements, int transactionSize, AtomicLong nextID)
      {
         super();
         this.journal = journal;
         this.numberOfElements = numberOfElements;
         this.transactionSize = transactionSize;
         this.nextID = nextID;
      }

      public void run()
      {
         try
         {
            int transactionCounter = 0;

            long transactionId = nextID.incrementAndGet();

            for (long i = 0; i < numberOfElements; i++)
            {

               long id = nextID.incrementAndGet();

               ByteBuffer buffer = ByteBuffer.allocate(512 * 3);
               buffer.putLong(id);

               if (transactionSize != 0)
               {
                  journal.appendAddRecordTransactional(transactionId, id, (byte)99, buffer.array());

                  if (++transactionCounter == transactionSize)
                  {
                     System.out.println("Commit transaction " + transactionId);
                     journal.appendCommitRecord(transactionId, true);
                     transactionCounter = 0;
                     transactionId = nextID.incrementAndGet();
                  }
               }
               else
               {
                  journal.appendAddRecord(id, (byte)99, buffer.array(), false);
               }
            }

            if (transactionCounter != 0)
            {
               journal.appendCommitRecord(transactionId, true);
            }

            if (transactionSize == 0)
            {
               journal.debugWait();
            }
         }
         catch (Exception e)
         {
            this.e = e;
         }

      }
   }

}