package com.CodeCode.utils;

import com.CodeCode.commons.CommonFiles;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.listen.Listenable;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * TODO zk工具类: 实现 连接 、节点的增删改查 和 监听
 *
 * @Description
 * @Author ysx
 * @Date 2025-05-14 21:27
 **/
public class ZkCuratorUtil {
    static final Logger logger = LoggerFactory.getLogger(ZkCuratorUtil.class);

    /**
     * @param
     * @return
     * @Description 创建curator客户端
     */
    public static CuratorFramework getzkClient() {
        // 构造者模式: 先通过builder()获取内部类Build对象,其作为client的完整构造器  再通过.build()获取CuratorFramework.
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(CommonFiles.ZOOKEEPER_SERVER)    // zk服务器地址  (必选,其余则是生产建议配置)
                .namespace(CommonFiles.ZOOKEEPER_NAMESPACE)     // 路径前缀: 后续所有操作都在该路径下(在该路径下创建节点或创建子路径)
                .connectionTimeoutMs(15000)                      // 初始化时的连接超时时间
                .retryPolicy(new RetryNTimes(3, 5000)) // 重试策略: 正常会话中断后 触发重试机制
                .sessionTimeoutMs(60000)                         // 会话超时时间: 客户端会周期发送心跳至服务端, 若5s内服务器没有收到心跳, 则会删除该会话及其相关的临时节点
                .build();

        client.start(); // 连接zk服务器, 开始会话  没有该操作client则无法后续操作

        // 判断连接会话是否OK
        if (client.getState() == CuratorFrameworkState.STARTED) {
            logger.info("zk client started");
            return client;
        } else {
            logger.info("客户端会话初始化异常,等待10秒");
            try { // 等待10秒
                client.blockUntilConnected(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("zk client error", e);
            }
            return client;
        }

    }

    /**
     * TODO 监控当前节点
     *
     * @param cClient curator客户端
     * @param path    监控节点路径
     * @throws Exception
     */
    public static NodeCache watchNode(CuratorFramework cClient, String path) throws Exception {
        NodeCache nodeCache = new NodeCache(cClient, path); // 该监视器类型 只有两种构造器, 另一种是追加 true, 即解压缩(如节点挂载的数据经过压缩则用这种构造器)
        nodeCache.start(true); // 注: 需先start,再添加监听器,否则会遗漏事件;  默认false , 若true则立即获取当前节点数据.

        nodeCache.getListenable().addListener(new NodeCacheListener() {
            @Override
            public void nodeChanged() throws Exception {
                // ChildData{path='/flag', stat=210453397509,210453397516,1750255745401,1750255882103,2,0,0,0,1,0,210453397509, data=[98]}
                ChildData currentData = nodeCache.getCurrentData();
                if (currentData != null) {
                    byte[] data = currentData.getData();
                    String record = new String(data);
                    if (!record.equals("SUCCESS")) {
                        cClient.setData().forPath(path, "SUCCESS".getBytes());
//                        logger.info("当前" + path + "节点数据: " + new String(currentData.getData()) + ", 已被修正为: " + "SUCCESS");
                    }
                }
            }
        });

        return nodeCache;
    }


    /***
     *  TODO 监控子节点
     * @param client
     * @param path
     * @param startMode
     * @throws Exception
     */
    public static void watchPathChildrenNode(CuratorFramework client, String path, PathChildrenCache.StartMode startMode) throws Exception {

        PathChildrenCache pathCache = new PathChildrenCache(client, path, true);
        pathCache.start(startMode);

        // 获取监听器管理接口 : 用于添加、移除监听器;  要监听该path的一级子节点变化, 则必须用PathChildrenCache , 再获取监听器接口去添加监听器.
        Listenable<PathChildrenCacheListener> listenable = pathCache.getListenable();

        // 监听器触发后执行的逻辑: 只要该子节点产生变化,就会调用该方法体;  event是变化的事件
        PathChildrenCacheListener cacheListener = new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                PathChildrenCacheEvent.Type type = event.getType();
                System.out.println("监测到一个操作 --->  type");

                ChildData data = event.getData();
                List<ChildData> initialData = event.getInitialData();
                if (data != null) {
                    System.out.println("监测到 节点数据：" + data.getPath() + " = " + new String(data.getData()));
                }

            }
        };

        // 添加监听器操作: 形参是具体的监听器接口. Listener: PathChildrenCacheListener
        listenable.addListener(cacheListener);

        pathCache.close();  // 关闭缓存
        client.close();
    }

    // TODO 创建节点
    public static String createZkNode(CuratorFramework zkClient, String path, CreateMode nodeMode) throws Exception {
        CreateBuilder createBuilder = zkClient.create();

        createBuilder.withMode(nodeMode);
        String flag = createBuilder.forPath(path);

        return flag;
    }


//    // 关闭所有资源
//    public void shutdown() {
//        System.out.println("开始关闭 Curator 资源...");
//
//        // 1. 关闭监听器（按创建的逆序关闭）
//        closeTreeCache();
//        closePathChildrenCache();
//        closeNodeCache();
//
//        // 2. 最后关闭客户端
//        closeClient();
//
//        System.out.println("Curator 资源关闭完成");
//    }

    public static void closeNodeCache(NodeCache nodeCache) {
        if (nodeCache != null) {
            try {
                nodeCache.close();
                logger.info("NodeCache 已关闭");
            } catch (IOException e) {
                logger.error("关闭 NodeCache 失败: " + e.getMessage());
            }
        }
    }

    public void closePathChildrenCache(PathChildrenCache pathChildrenCache) {
        if (pathChildrenCache != null) {
            try {
                pathChildrenCache.close();
                logger.info("PathChildrenCache 已关闭");
            } catch (IOException e) {
                logger.error("关闭 PathChildrenCache 失败: " + e.getMessage());
            }
        }
    }

    public void closeTreeCache(TreeCache treeCache) {
        if (treeCache != null) {
            treeCache.close();
            logger.info("TreeCache 已关闭");
        }
    }

    public static void closeClient(CuratorFramework cClient) {
        if (cClient != null) {
            try {
                cClient.close();
                logger.info("CuratorFramework 客户端已关闭");
            } catch (Exception e) {
                logger.error("关闭 CuratorFramework 失败: " + e.getMessage());
            }
        }
    }


}
