hbase-downstreamer
==================

Fake downstream project used figuring what is required when depending on hbase client and minicluster, etc.

To build against the last three major HBase release lines, using a stable Hadoop version, just run package.

    $ mvn clean package

It runs a unit test in each major release line that spins up an HBase cluster.

The generated application also includes an example command to verify connectivity to a real cluster.
To test it copy the artifact out of the appropriate major version's target/ and then run using the hbase command. It will need to know
your ZooKeeper quorum (defaults to localhost).

For example, to use the HBase 1.y API to test against an HBase 1.y cluster:

    $ scp hbase-1/target/hbase-downstreamer-api-1.y-2.0-SNAPSHOT-1.4.10_2.7.1.jar edge-node.example.com:
    hbase-downstreamer-api-1.y-2.0-SNAPSHOT-1.4.10_2.7.1.jar                                                             100%   11KB  10.8KB/s   00:00
    $ ssh edge-node.example.com
    $ HBASE_CLASSPATH=hbase-downstreamer-api-1.y-2.0-SNAPSHOT-1.4.10_2.7.1.jar hbase org.hbase.downstreamer.GetMetaContent zoo1.example.com:2181,zoo2.example.com:2181,zoo3.example.com:2181
    ...SNIP...
    $ echo $?
    0


The application should print out many entries from an hbase internal table and then exit without error.

building against specific HBase versions
===============================

To build against a version of HBase 1.y other than the latest stable, specify `hbase.1.version`
property on the maven command line. Similarly, you can use `hadoop.version` to specify a different
release of Hadoop 2.

For example, below we'll build our HBase 1.y API example against HBase 1.3.4 and Hadoop 2.9.2.

    $ mvn -Dhbase.1.version=1.3.4 -Dhadoop.version=2.9.2 -pl hbase-1 -am clean package

To test building against a repository, e.g. the staging repo for an HBase release candidate, you can
set use the hbase.staging.repository property. Note that you'll also have to specify the appropriate
upstream release version. (for good measure clear your local cache of jars under .m2/repository or
pass -U to maven building):

    $ mvn -Dhbase.1.version=1.4.11 \
        -Dhbase.staging.repository='https://repository.apache.org/content/repositories/orgapachehbase-1234' \
        clean package

testing source compatibility
===========================

Each of the major version specific modules should be relying on the supported APIs for that particular major release. As such, we expect them to work with the following major release. For example, the `hbase-1` module should still build fine if we try to compile it against HBase 2.y releases.

You can test the above assertions by passing in a suitable next-major-release version number for the api-specific parameters. E.g. to use the 1.x API with upstream HBase 2.1.5 you would do:

    $ mvn -Dhbase.version=2.1.5 -pl hbase-1 -am clean package

To make this easier, there are profiles that will pick an appropraite next-major-version for you.

Ensure HBase 1 API use works with HBase 2:

    $ mvn -Pbuild-1.y-against-hbase-2 -pl hbase-1 -am clean package

You should not expect the reverse to work. That is, building the HBase 2.y API module with an HBase 1.x version will fail. Similarly, you should not expect using a version that is more than one major version ahead to work.

testing client-server compatibility with a standalone client
============================================================

If you wish to verify client-server wire compatibility between HBase & Hadoop versions you can
build a standalone jar using the 'client-standalone' profile. This profile can be combined with any of the
above options for choosing an HBase and Hadoop version.

    $ mvn -Pclient-standalone -Phadoop-2 clean package

The generated artifact should be used on a live cluster with the java command. It should only need
access to HBase client configuration files and the location of your zookeeper quorum.

    $ scp hbase-1/target/hbase-downstreamer-api-1.y-2.0-SNAPSHOT-1.4.10_2.7.1-standalone.jar edge-node.example.com:
    hbase-downstreamer-api-1.y-2.0-SNAPSHOT-1.4.10_2.7.1-standalone.jar                                                  100%   31MB 378.3KB/s   01:24
    $ ssh edge-node.example.com
    $ java -cp /etc/hbase/conf:hbase-downstreamer-api-1.y-2.0-SNAPSHOT-1.4.10_2.7.1-standalone.jar org.hbase.downstreamer.GetMetaContent zoo1.example.com:2181,zoo2.example.com:2181,zoo3.example.com:2181
    ...SNIP...
    $ echo $?
    0

The application should print out many entries from an hbase internal table and then exit without error.

spark streaming test application
================================

This project includes a Spark Streaming application with renewed tokens for long-running against a kerberos-enabled hbase cluster.

Prerequisites:
    
  - hbase client configs deployed to all yarn worker nodes (in /etc/hbase/conf)
  - have a keytab and associated principal with run access for yarn and write access to hbase (replace 'auser@EXAMPLE.COM' with a real principal for your KDC)

Build as described above. Optionally choose Scala and Spark versions appropriate for your distribution via `scala.version` and `spark.version`. The defaults are the equivalent to
    
        $ mvn -Dscala.version=2.11 -Dspark.version=2.4.0
    
Run a netcat instance generating data (below examples presume this host is named _netcat.running.host.example.com_
    
        $ yes | nc -lk 1772

Create needed test table in hbase and grant access to 'auser' (presumes hbase user has stored kerberos tickets)

        $ sudo -u hbase hbase shell
        hbase(main):001:0> create 'counts', 'word_counts'
        0 row(s) in 0.6740 seconds

        => Hbase::Table - counts
        hbase(main):002:0>  grant 'auser', 'RW', 'counts'
        0 row(s) in 0.7300 seconds

        hbase(main):003:0>

Test run with writes to hbase on the cluster (no need to kinit, presuming default hbase, hadoop, and slf4j versions):

        $ spark-submit --master yarn --deploy-mode cluster --keytab auser.keytab --principal 'auser@EXAMPLE.COM' --class org.hbase.downstreamer.spark.JavaNetworkWordCountStoreInHBase --packages org.slf4j:slf4j-api:1.7.5 hbase-downstreamer-api-1.y-2.0-SNAPSHOT-1.4.10_2.7.1.jar netcat.running.host.example.com 1772 2>spark.log | tee spark.out

Alternative, you can use the standalone client jar. In addition to relying on the HBase client jars you package, this will let you skip including the slf4j-api jars:

        $ spark-submit --master yarn --deploy-mode cluster --keytab auser.keytab --principal 'auser@EXAMPLE.COM' --class org.hbase.downstreamer.spark.JavaNetworkWordCountStoreInHBase hbase-downstreamer-api-1.y-2.0-SNAPSHOT-1.4.10_2.7.1-standalone.jar netcat.running.host.example.com 1772 2>spark.log | tee spark.out

Verify results in hbase (presumes you have stored kerberos tickets)

        $ echo "count \"counts\"; scan \"counts\", {REVERSED => true, LIMIT => 10}" | hbase shell --noninteractive 2>/dev/null

        Current count: 1000, row: 1458924355000 ms
        1944 row(s) in 0.5740 seconds

        ROW  COLUMN+CELL
         1458925431000 ms column=word_counts:y, timestamp=1458925432081, value=\x00\x14@\xFC
         1458925430000 ms column=word_counts:y, timestamp=1458925430537, value=\x00$\xE1\xF2
         1458925429000 ms column=word_counts:y, timestamp=1458925429676, value=\x00\x13\xFC\xA2
         1458925428000 ms column=word_counts:y, timestamp=1458925428673, value=\x00\x11\xCC\x9C
         1458925427000 ms column=word_counts:y, timestamp=1458925428189, value=\x00\x07\xCF\xE5
         1458925426000 ms column=word_counts:y, timestamp=1458925427861, value=\x00/\x18\x0C
         1458925425000 ms column=word_counts:y, timestamp=1458925425436, value=\x00\x0A\xC4\x0D
         1458925424000 ms column=word_counts:y, timestamp=1458925424717, value=\x00\x1E}%
         1458925423000 ms column=word_counts:y, timestamp=1458925423821, value=\x00\x1B&\x1D
         1458925422000 ms column=word_counts:y, timestamp=1458925422677, value=\x00\x06jt
        10 row(s) in 0.0400 seconds

        nil

