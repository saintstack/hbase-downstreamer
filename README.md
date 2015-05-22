hbase-downstreamer
==================

Fake downstream project used figuring what is required when depending on hbase client and minicluster, etc.

To build against the default of the current HBase 1.y release line and the last stable Hadoop
version, simply run package.

    $ mvn clean package

It runs a unit test that spins up an hdfs cluster.

The generated application also includes an example command to verify connectivity to a real cluster.
To test it copy the artifact out of target/ and then run using the hbase command. It will need to know
your ZooKeeper quorum (defaults to localhost).

    $ scp target/hbase-downstreamer-1.0-SNAPSHOT-1.1.0_2.6.0.jar edge-node.example.com:
    hbase-downstreamer-1.0-SNAPSHOT-1.1.0_2.6.0.jar                                                                                                                           100%   11KB  10.8KB/s   00:00
    $ ssh edge-node.example.com
    $ HBASE_CLASSPATH=hbase-downstreamer-1.0-SNAPSHOT-1.1.0_2.6.0.jar hbase org.hbase.downstreamer.GetMetaContent zoo1.example.com:2181,zoo2.example.com:2181,zoo3.example.com:2181
    ...SNIP...
    $ echo $?
    0


The application should print out many entries from an hbase internal table and then exit without error.

To build against an older version of HBase 1.y, specify hbase.version property on the maven
command line. Similarly, you can use hadoop.version to specify a different release of Hadoop 2.
For example, in this example we build against HBase 1.0.1 and Hadoop 2.7.0 (an unstable release of
Hadoop currently available for developer testing).

    $ mvn -Dhbase.version=1.0.1 -Dhadoop.version=2.7.0 clean package

To build against older release lines of HBase, you'll need to activate the hbase-0.98 profile.

    $ mvn -Phbase-0.98 clean package

By default, this profile should use the latest version of HBase 0.98 and Hadoop 2. If you want to specify
versions you can do it just like in the HBase 1 case.

    $ mvn -Phbase-0.98 -Dhbase-version=0.98.4 -Dhadoop.version=2.7.0 clean package

If you want to use HBase 0.98 with Hadoop 1, you'll need to use the hbase-0.98-hadoop1 profile.
Specifying an HBase 0.98 or Hadoop 1 version other than the latest works similarly to the
hadoop 2 version.

    $ mvn -Phbase-0.98-hadoop1 clean package

To test building against a repository, e.g. the staging repo for an HBase release candidate, you can
set use the hbase.staging.repository property. Note that you'll also have to specify the appropriate
upstream release version. (for good measure clear your local cache of jars under .m2/repository or
pass -U to maven building):

    $ mvn -Dhbase.version=1.1.1 \
        -Dhbase.staging.repository='https://repository.apache.org/content/repositories/orgapachehbase-1001' \
        clean package

Finally, if you wish to verify client-server wire compatibility between HBase & Hadoop versions you can
build a standalone jar using the 'client-standalone' profile. This profile can be combined with any of the
above options for choosing an HBase and Hadoop version, but you must always specify the HBase profile
you want.

    $ mvn -Pclient-standalone -Phbase-1 clean package

The generated artifact should be used on a live cluster with the java command. It should only need
access to HBase client configuration files and the location of your zookeeper quorum.

    $ scp target/hbase-downstreamer-1.0-SNAPSHOT-1.1.0_2.6.0-standalone.jar edge-node.example.com:
    hbase-downstreamer-1.0-SNAPSHOT-1.1.0_2.6.0-standalone.jar                                                                                                                100%   31MB 378.3KB/s   01:24
    $ ssh edge-node.example.com
    $ java -cp /etc/hbase/conf:hbase-downstream-1.0-SNAPSHOT-1.1.0_2.6.0-standalone.java org.hbase.downstreamer.GetMetaContent zoo1.example.com:2181,zoo2.example.com:2181,zoo3.example.com:2181
    ...SNIP...
    $ echo $?
    0

The application should print out many entries from an hbase internal table and then exit without error.

