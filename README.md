hbase-downstreamer
==================

Fake downstream project used figuring what is required when depending on hbase client and minicluster, etc.

To point it at a repository, say a staging dir of the latest hbase release, do the following setting
the version you want to run this downstream 'app' against and the repository to get the jars from
(for good measure clear your local cache of jars under .m2/repository or pass -U to maven building):

    diff --git a/pom.xml b/pom.xml
    index 3762c82..ece9b71 100644
    --- a/pom.xml
    +++ b/pom.xml
    @@ -8,7 +8,7 @@
       <name>hbase-downstreamer</name>
       <url>https://github.com/saintstack/hbase-downstreamer</url>
       <properties>
    -    <hbase.version>0.96.0-hadoop1</hbase.version>
    +    <hbase.version>0.98.0-hadoop1</hbase.version>
         <hadoop.version>1.1.2</hadoop.version>
       </properties>
       <dependencies>
    @@ -90,7 +90,7 @@
       <repositories>
         <repository>
           <id>Staging</id>
    -      <url>https://repository.apache.org/content/repositories/orgapachehbase-076/</url>
    +      <url>https://repository.apache.org/content/repositories/orgapachehbase-1001/</url>
         </repository>
       </repositories>
     </project>

Then to build against an hbase compiled against hadoop1, do the below:

  $ mvn clean install

It runs a unit test that spins up an hdfs cluster.

To build against an hbase compiled against hadoop2, do the above edit to pom.xml.hadoop2 and then run:

  $ mvn clean install -f pom.xml.hadoop2

It runs the same test only depends on hadoop2 hbase
