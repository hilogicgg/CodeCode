package com.goldCode.utils;

import com.goldCode.commons.CommonFiles;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.listen.Listenable;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.io.IOException;

/**
 * TODO
 *
 * @Description zk客户端Curator操作zk : 监视节点、查子节点、查节点挂载数据等
 * @Author ysx
 * @Date 2025-05-19 21:34
 **/
public class zkCuratorUtil2 {
    public static void main(String[] args) throws Exception {
        CuratorFramework client = getzkConn( CommonFiles.ZOOKEEPER_SERVER , CommonFiles.ZOOKEEPER_NAMESPACE);
        client.start(); // 此处的start()是 CuratorFramework 的实现类 CuratorFrameworkImpl的, 只是用它接收而已.
        zkCuratorUtil2.monitorNode( client , "/DEL" );
    }

    public static void monitorNode(CuratorFramework zkClient, String path ) throws Exception {
        System.out.println("进入monitor逻辑");
        PathChildrenCache cache = new PathChildrenCache(zkClient, path, true);
        Listenable<PathChildrenCacheListener> listenable = cache.getListenable();
        listenable.addListener( new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                System.out.println("监测到子节点数据变化: ===========================");
                PathChildrenCacheEvent.Type opType = event.getType();
                ChildData data = event.getData();
                String childData = new String(data.getData());
                String childPath = data.getPath();
                if( data != null ){
                    System.out.println("操作: opType=" + opType + ", childPath=" + childPath + ", childData=" + childData);
                }
            }
        });

        // 操作
        zkClient.create().creatingParentsIfNeeded().forPath(path + "/test01", "01".getBytes());
        Thread.sleep(10);
        zkClient.create().creatingParentsIfNeeded().forPath(path + "/test02", "02".getBytes());
        Thread.sleep(10);
        zkClient.setData().forPath(path + "/test01", "01_V2".getBytes());
        Thread.sleep(10);
        for (ChildData data : cache.getCurrentData()) {
            System.out.println("getCurrentData:" + data.getPath() + " = " + new String(data.getData()));
        }

        cache.close();
        zkClient.close();
    }

    public static CuratorFramework getzkConn( String zkServer , String namespace ){
        // 客户端必填参数: 服务器、节点、重试机制
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString( zkServer )
                .namespace( namespace )
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();

        return client;
    }
}
