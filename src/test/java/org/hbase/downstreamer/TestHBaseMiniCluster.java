package org.hbase.downstreamer;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.junit.Test;

/**
 * Make sure downstream project can put up a minihbasecluster.
 */
public class TestHBaseMiniCluster {
  @Test
  public void testSpinUpMiniHBaseCluster() throws Exception {
    HBaseTestingUtility htu = new HBaseTestingUtility();
    /*
    System.out.println(FileSystem.FS_DEFAULT_NAME_KEY);
    System.out.println(htu.getConfiguration().get(FileSystem.FS_DEFAULT_NAME_KEY));
    htu.getConfiguration().set(FileSystem.FS_DEFAULT_NAME_KEY, "file://tmp/");
    System.out.println(htu.getConfiguration().get(FileSystem.FS_DEFAULT_NAME_KEY));
    System.out.println(FileSystem.class.getClassLoader());
    FileSystem fs = FileSystem.get(htu.getConfiguration());
    System.out.println(fs.toString());*/
    try {
      htu.startMiniCluster();
      htu.getMetaTableRows();
    } finally {
      htu.shutdownMiniCluster();
    }
  }
}