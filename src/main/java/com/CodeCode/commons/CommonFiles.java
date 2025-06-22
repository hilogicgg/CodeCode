package com.CodeCode.commons;

/**
 * @author ysx
 * @create 2025-05-14 17:40
 */
public class CommonFiles {
    // zookeeper
    public static String ZOOKEEPER_SERVER = "hadoop202:2181,hadoop203:2181,hadoop204:2181";
    // zk的连接名(zk的操作目录)
    public static String ZOOKEEPER_NAMESPACE = "NodeTest"; // 后续操作都在该路径下, 注意: curator会自动给它加上/ , 即 /hbase.
    //

}
