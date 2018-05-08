package org.hbase.downstreamer;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.zookeeper.MiniZooKeeperCluster;
import org.junit.Test;

/**
 * Make sure downstream project can put up a minihbasecluster.
 */
public class TestHBaseMiniCluster {
  @Test
  public void testSpinUpMiniHBaseCluster() throws Exception {
    Configuration config = HBaseConfiguration.create();
    // work around HBASE-20544
    config.setBoolean("hbase.localcluster.assign.random.ports", true);
    HBaseTestingUtility htu = new HBaseTestingUtility(config);
    try {
      htu.startMiniCluster();
      htu.getMetaTableRows();
    } finally {
      htu.shutdownMiniCluster();
    }
  }
}
