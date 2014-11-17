package org.hbase.downstreamer;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.zookeeper.MiniZooKeeperCluster;
import org.junit.Test;

/**
 * Make sure downstream project can put up a minihbasecluster.
 */
public class TestHBaseMiniCluster {
  @Test
  public void testSpinUpMiniHBaseCluster() throws Exception {
    HBaseTestingUtility htu = new HBaseTestingUtility();
    try {
      htu.startMiniCluster();
      htu.getMetaTableRows();
    } finally {
      htu.shutdownMiniCluster();
    }
  }
}