hbase-downstreamer
==================

Fake downstream project used figuring what is required when depending on hbase client and minicluster, etc.

To point it at a repository, say a staging dir of the latest hbase release, do the following setting
the version you want to run this downstream 'app' against and the repository to get the jars from
(for good measure clear your local cache of jars under .m2/repository or pass -U to maven building):

diff --git a/pom.xml b/pom.xml
index 44d7f09..59b4c89 100644
--- a/pom.xml
+++ b/pom.xml
@@ -8,7 +8,7 @@
   &lt;name>hbase-downstreamer&lt;/name>
   &lt;url>https://github.com/saintstack/hbase-downstreamer&lt;/url>
   &lt;properties>
-    &lt;hbase.version>0.95.2-hadoop1-SNAPSHOT&lt;/hbase.version>
&%2B;    &lt;hbase.version>0.96.0-hadoop1-SNAPSHOT&lt;/hbase.version>
     &lt;hadoop.version>1.1.2&lt;/hadoop.version>
   &lt;/properties>
   &lt;dependencies>
@@ -87,4 +87,10 @@
       &lt;version>${hbase.version}&lt;/version>
     &lt;/dependency>
   &lt;/dependencies>
+  &lt;repositories>
+    &lt;repository>
+      &lt;id>Staging&lt;/id>
+      &lt;url>https://repository.apache.org/content/repositories/orgapachehbase-076/&lt;/url>
+    &lt;/repository>
+  &lt;/repositories>
 &lt;/project>

Then to build against an hbase compiled against hadoop1, do the below:

  $ mvn clean install

It runs a unit test that spins up an hdfs cluster.

To build against an hbase compiled against hadoop2, do the above edit to pom.xml.hadoop2 and then run:

  $ mvn clean install -f pom.xml.hadoop2

It runs the same test only depends on hadoop2 hbase
