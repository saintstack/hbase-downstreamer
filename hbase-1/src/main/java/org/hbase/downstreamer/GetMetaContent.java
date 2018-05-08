package org.hbase.downstreamer;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;
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
    try (final Connection connection = ConnectionFactory.createConnection(configuration);
         final Table t = connection.getTable(TableName.META_TABLE_NAME);
         final ResultScanner scanner = t.getScanner(new Scan())) {
      for (Result r = null; (r = scanner.next()) != null;) {
        System.out.println(r);
      }
    }
  }
}
