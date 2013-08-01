hbase-downstreamer
==================

Fake downstream project used figuring what is required when depending on hbase client and minicluster, etc.

To build against an hbase compiled against hadoop1 (1.1.2 as of this writing), do the below:

  $ mvn clean install

It runs a unit test that spins up an hdfs cluster.

To build against an hbase compiled against hadoop2 (2.0.5-alpha as of this writing), do the below:

  $ mvn clean install -f pom.xml.hadoop2

It runs the same test.

Read pom.xml.hadoop2 for list of dependencies needed for test and build.
