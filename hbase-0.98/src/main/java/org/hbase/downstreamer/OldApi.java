package org.hbase.downstreamer;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.util.Bytes;
import java.io.IOException;

/**
 * Makes use of pre-1.0 APIs.
 * Specifically these classes:
 * <ul>
 *   <li>org.apache.hadoop.hbase.client.HTableInterface 
 *   <li>org.apache.hadoop.hbase.client.Put 
 *   <li>org.apache.hadoop.hbase.client.Result 
 *   <li>org.apache.hadoop.hbase.client.ResultScanner 
 *   <li>org.apache.hadoop.hbase.client.Scan 
 *   <li>org.apache.hadoop.hbase.client.Delete 
 *   <li>org.apache.hadoop.hbase.client.Get 
 *   <li>org.apache.hadoop.hbase.client.HConnection 
 *   <li>org.apache.hadoop.hbase.client.HConnectionManager 
 *   <li>org.apache.hadoop.hbase.client.HBaseAdmin
 * </ul>
 */
public class OldApi {

  private static final byte[] ROW = Bytes.toBytes("example_row");
  private static final byte[] FAMILY = Bytes.toBytes("example_family");
  private static final byte[] QUALIFIER = Bytes.toBytes("example_qualifier");
  private static final byte[] VALUE = Bytes.toBytes("example_value");

  public static void main(String[] args) throws IOException {
    Configuration configuration = HBaseConfiguration.create();
    // first arg is table name
    final String tableName = args[0];
    // second arg is zookeeper quorum, if any.
    if (args.length > 1) {
      configuration.set(HConstants.ZOOKEEPER_QUORUM, args[1]);
    }

    final HConnection connection = HConnectionManager.createConnection(configuration);
    try {
      final HBaseAdmin admin = new HBaseAdmin(connection);
      try {
        final HTableDescriptor descriptor = new HTableDescriptor(tableName);
        final HColumnDescriptor family = new HColumnDescriptor(FAMILY);
        descriptor.addFamily(family);
        admin.createTable(descriptor);
        final HTableInterface table = connection.getTable(tableName);
        try {
          System.out.println("Put.");
          final Put put = new Put(ROW);
          put.add(FAMILY, QUALIFIER, VALUE);
          table.put(put);
          table.flushCommits();
          final Get get = new Get(ROW);
          final Result getResult = table.get(get);
          System.out.println("Get:");
          System.out.println(getResult);
          System.out.println("Scan:");
          final Scan all = new Scan();
          final ResultScanner scanner = table.getScanner(all);
          try {
            for (Result r = null; (r = scanner.next()) != null;) {
              System.out.println(r);
            }
          } finally {
            scanner.close();
          }
          System.out.println("Delete.");
          final Delete del = new Delete(ROW);
          table.delete(del);
          table.flushCommits();
        } finally {
          try {
            table.close();
          } finally {
            admin.disableTable(tableName);
            admin.deleteTable(tableName);
          }
        }
      } finally {
        admin.close();
      }
    } finally {
      connection.close();
    }
  }
}
