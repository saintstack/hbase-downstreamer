package org.hbase.downstreamer;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import java.io.IOException;


public class GetMetaContent {
  /**
   * Lets first argument be the zk ensemble if any.
   */
  public static void main(String[] args) throws IOException {
    // Just go and scan .META.  Let that be success.
    Configuration configuration = HBaseConfiguration.create();
    if (args.length > 0) configuration.set(HConstants.ZOOKEEPER_QUORUM, args[0]);
    HTable t = new HTable(configuration, ".META.");
    try {
      ResultScanner scanner = t.getScanner(new Scan());
      for (Result r = null; (r = scanner.next()) != null;) {
        System.out.println(r);
      }
    } finally {
      t.close();
    }
  }
}
