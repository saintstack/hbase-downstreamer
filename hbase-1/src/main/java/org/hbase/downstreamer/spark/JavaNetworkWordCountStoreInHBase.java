/*
 * Changes Copyright 2016,2019 hbase-downstreamer contributor(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Original code covered under:
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * derived from:
 * https://raw.githubusercontent.com/apache/spark/v1.5.0/examples/src/main/java/org/apache/spark/examples/streaming/JavaNetworkWordCount.java
 * https://raw.githubusercontent.com/apache/spark/v2.4.0/examples/src/main/java/org/apache/spark/examples/streaming/JavaNetworkWordCount.java
 *
 */

package org.hbase.downstreamer.spark;

import scala.Tuple2;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.hbase.ClusterStatus;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.api.java.StorageLevels;
import org.apache.spark.deploy.SparkHadoopUtil;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.Time;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Counts words in UTF8 encoded, '\n' delimited text received from the network every second. Then
 * stores the counts in HBase using a table layout of:
 *
 *  RowID          |     CF          |   CQ     | value
 * time in millis  |  "word_counts"  |  <word>  |  <count for period>
 *
 * Works on secure clusters for an indefinite period via keytab login. NOTE: copies the given
 * keytab to the working directory of executors.
 *
 * Usage: JavaNetworkWordCountStoreInHBase <hostname> <port>
 * <hostname> and <port> describe the TCP server that Spark Streaming would connect to receive data.
 *
 * To run this on your local machine, you need to first run a Netcat server
 *    `$ nc -lk 9999`
 * and then run the example
 *    `$ bin/run-example org.apache.spark.examples.streaming.JavaNetworkWordCount localhost 9999`
 */
public final class JavaNetworkWordCountStoreInHBase {

  private static final Logger LOG = LoggerFactory.getLogger(JavaNetworkWordCountStoreInHBase.class);

  /**
   * Write each word:count pair into hbase, in a row for the given time period.
   */
  public static final class StoreCountsToHBase implements VoidFunction<Iterator<Tuple2<String,Integer>>> {

    private static final TableName COUNTS = TableName.valueOf("counts");
    private static final byte[] WORD_COUNTS = Bytes.toBytes("word_counts");

    private static final ConcurrentHashMap<Tuple2<String,String>, Tuple2<UserGroupInformation, Connection>>
        connections = new ConcurrentHashMap<Tuple2<String,String>, Tuple2<UserGroupInformation, Connection>>();

    private byte[] ROW_ID;
    /** (principal, keytab) */
    private final Tuple2<String, String> auth;

    public StoreCountsToHBase(final SparkConf sparkConf) {
      auth = new Tuple2<String,String>(sparkConf.get("spark.yarn.principal"), sparkConf.get("spark.yarn.keytab"));
    }

    public void setTime(Time time) {
      ROW_ID = Bytes.toBytes(time.toString());
    }

    /**
     * Rely on a map of (principal,keytab) => connection to ensure we only keep around one per
     * Classloader.
     */
    private static Tuple2<UserGroupInformation, Connection> ensureConnection(final Tuple2<String, String> auth)
        throws IOException, InterruptedException {
      Tuple2<UserGroupInformation, Connection> result = connections.get(auth);
      if (result == null) {
        LOG.info("Setting up HBase connection.");
        try {
          final SparkHadoopUtil util = SparkHadoopUtil.get();
          final Configuration conf = HBaseConfiguration.create(util.newConfiguration(new SparkConf()));
          // This work-around for getting hbase client configs requires that you deploy an HBase GATEWAY
          // role on each node that can run a spark executor.
          final File clientConfigs = new File("/etc/hbase/conf");
          for (File siteConfig : clientConfigs.listFiles()) {
            if (siteConfig.getName().endsWith(".xml")) {
              LOG.debug("Adding config resource: {}", siteConfig);
              conf.addResource(siteConfig.toURI().toURL());
            }
          }
          UserGroupInformation ugi = null;
          Connection connection = null;
          synchronized(UserGroupInformation.class) {
            LOG.debug("setting UGI conf");
            UserGroupInformation.setConfiguration(conf);
            LOG.debug("Ability to read keytab '{}' : {}", auth._2(), (new File(auth._2())).canRead());
            LOG.debug("logging in via UGI and keytab.");
            ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(auth._1(), auth._2());
            LOG.info("finished login attempt via UGI and keytab. security set? {}", ugi.isSecurityEnabled());
          }
          connection = ugi.doAs(new PrivilegedExceptionAction<Connection>() {
            @Override
            public Connection run() throws IOException {
              return ConnectionFactory.createConnection(conf);
            }
          });
          // Do something with the connection now, to ensure we have valid credentials.
          final Admin admin = connection.getAdmin();
          try {
            final ClusterStatus status = admin.getClusterStatus();
            LOG.info("connection successful: {}", status);
            final Tuple2<UserGroupInformation, Connection> proposed = new Tuple2<UserGroupInformation, Connection>(ugi, connection);
            final Tuple2<UserGroupInformation, Connection> prior = connections.putIfAbsent(auth, proposed);
            if (prior == null) {
              // our proposed connection is valid.
              result = proposed;
            } else {
              // parallel instantiation beat us to completion
              LOG.warn("Discarding extra connection. No need to be concerned, unless this message happens dozens of times.");
              result = prior;
              connection.close();
            }
          } finally {
            admin.close();
          }
        } catch (IOException exception) {
          LOG.error("Failed to connect to hbase. rethrowing; application should fail.", exception);
          throw exception;
        }
      }
      return result;
    }

    @Override
    public void call(Iterator<Tuple2<String, Integer>> iterator) throws IOException, InterruptedException {
      try {
        Tuple2<UserGroupInformation, Connection> hbase = ensureConnection(auth);
        // This presumes we can complete our Put calculation before the TGT expires.
        // At worst after this check we will have 20% of the ticket window left; for 24hr tickets that means
        // just under 5 hours.
        hbase._1().checkTGTAndReloginFromKeytab();
        final Table table = hbase._2().getTable(COUNTS);
        Put put = new Put(ROW_ID);
        while (iterator.hasNext()) {
          final Tuple2<String, Integer> wordCount = iterator.next();
          put.addColumn(WORD_COUNTS, Bytes.toBytes(wordCount._1), Bytes.toBytes(wordCount._2));
        }
        if (!put.isEmpty()) {
          LOG.debug("Putting " + put.size() + " cells.");
          table.put(put);
        }
      } catch (IOException exception) {
        LOG.error("Failed to put cells. rethrowing; application should fail.", exception);
        throw exception;
      }
    }
  }

  private static final Pattern SPACE = Pattern.compile(" ");

  public static void main(String[] args) throws InterruptedException {
    if (args.length < 2) {
      System.err.println("Usage: JavaNetworkWordCountStoreInHBase <hostname> <port>");
      System.exit(1);
    }

    // Create the context with a 1 second batch size
    SparkConf sparkConf = new SparkConf().setAppName("JavaNetworkWordCountStoreInHBase");
    JavaStreamingContext ssc = new JavaStreamingContext(sparkConf, Durations.seconds(1));

    // Copy the keytab to our executors
    ssc.sparkContext().addFile(sparkConf.get("spark.yarn.keytab"));

    // Create a JavaReceiverInputDStream on target ip:port and count the
    // words in input stream of \n delimited text (eg. generated by 'nc')
    // Note that no duplication in storage level only for running locally.
    // Replication necessary in distributed scenario for fault tolerance.
    JavaReceiverInputDStream<String> lines = ssc.socketTextStream(
            args[0], Integer.parseInt(args[1]), StorageLevels.MEMORY_AND_DISK_SER);
    JavaDStream<String> words = lines.flatMap(x -> Arrays.asList(SPACE.split(x)).iterator());
    JavaPairDStream<String, Integer> wordCounts = words.mapToPair(s -> new Tuple2<>(s,1))
        .reduceByKey((i1, i2) -> i1 + i2);

    final StoreCountsToHBase store = new StoreCountsToHBase(sparkConf);

    wordCounts.foreachRDD((rdd, time) -> {
      store.setTime(time);
      rdd.foreachPartition(store);
    });

    ssc.start();
    ssc.awaitTermination();
  }
}
