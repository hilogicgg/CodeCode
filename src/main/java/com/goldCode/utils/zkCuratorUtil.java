package com.goldCode.utils;

import com.goldCode.commons.CommonFiles;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.curator.framework.listen.Listenable;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;

import java.util.List;

/**
 * TODO
 *
 * @Description
 * @Author ysx zk工具类: 实现 子节点、数据查询 和 节点监听
 * @Date 2025-05-14 21:27
 **/
public class zkCuratorUtil {

    public static void main(String[] args) throws Exception {

        CuratorFramework cClient = getzkClient(); // 创建Curator客户端
        cClient.start();                          // 连接服务端 (异步连接zk服务器获取会话、激活监听器).

//        createZkNode(zkClient , "/test" , CreateMode.PERSISTENT);

        watchNode(cClient , "/DEL", PathChildrenCache.StartMode.NORMAL);

/*        // 获取子节点信息
        GetChildrenBuilder children = zkClient.getChildren();
        List<String> nodes = children.forPath("/");
        for (String node : nodes) { // node /hbase下一级的的文件或目录名
            System.out.println("/hbase 下的子节点为:" + node);
            if(node.equals("master")){
                byte[] bytes = zkClient.getData().forPath("/master");
                System.out.println("/hbase/master 挂载的数据为: " + new String(bytes));
                continue;
            }
        }

        // 获取节点数据
        byte[] bytes = zkClient.getData().forPath("/");
        if (bytes != null) {
            String data = new String(bytes);
            System.out.println("/hbase 挂载的数据为: " + data);
        }*/


        cClient.close(); // 关闭连接
    }


    /**
     * @param
     * @return
     * @Description 创建curator客户端
     */
    private static CuratorFramework getzkClient() {

        // 构造者模式: 先通过builder()获取内部类Build对象,其作为client的完整构造器  再通过.build()获取CuratorFramework.
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(CommonFiles.ZOOKEEPER_SERVER)    // zk服务器地址  (必选,其余则是生产建议配置)
                .namespace(CommonFiles.ZOOKEEPER_NAMESPACE)     // 路径前缀: 后续所有操作都在该路径下(在该路径下创建节点或创建子路径)
                .connectionTimeoutMs(5000)                      // 连接超时时间
                .retryPolicy(new RetryNTimes(3, 2000)) // 重试策略
                .sessionTimeoutMs(5000)                         // 会话超时时间: 客户端会周期发送心跳至服务端, 若5s内服务器没有收到心跳, 则会删除该会话及其相关的临时节点
                .build();
        return client;
    }

    // TODO 监控节点
    public static void watchNode( CuratorFramework client, String path , PathChildrenCache.StartMode startMode) throws Exception {

        PathChildrenCache pathCache = new PathChildrenCache(client, path, true);
        pathCache.start( startMode );

        // 获取监听器管理接口 : 用于添加、移除监听器;  要监听该path的一级子节点变化, 则必须用PathChildrenCache , 再获取监听器接口去添加监听器.
        Listenable<PathChildrenCacheListener> listenable = pathCache.getListenable();

        // 监听器触发后执行的逻辑: 只要该子节点产生变化,就会调用该方法体;  event是变化的事件
        PathChildrenCacheListener cacheListener = (client1, event) -> {
            System.out.println("监测到一个操作 =====================");
            System.out.println("监测到 事件类型：" + event.getType());
            ChildData data = event.getData();
            if (null != data) { //
                System.out.println("监测到 节点数据：" + data.getPath() + " = " + new String(data.getData()));
            }
        };

//        PathChildrenCacheListener cacheListener = new PathChildrenCacheListener() {
//            @Override
//            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
//                PathChildrenCacheEvent.Type type = event.getType();
//                ChildData data = event.getData();
//                List<ChildData> initialData = event.getInitialData();
//
//            }
//        };

        // 添加监听器操作: 形参是具体的监听器接口. Listener: PathChildrenCacheListener
        listenable.addListener( cacheListener );

        // 模拟对节点操作: 新增子节点和数据
        client.create().creatingParentsIfNeeded().forPath(path + "/test01", "01".getBytes());
        Thread.sleep(10);
        client.create().creatingParentsIfNeeded().forPath(path + "/test02", "02".getBytes());
        Thread.sleep(10);
        client.setData().forPath(path + "/test01", "01_V2".getBytes());
        Thread.sleep(10);
        for (ChildData data : pathCache.getCurrentData()) {
            System.out.println("getCurrentData:" + data.getPath() + " = " + new String(data.getData()));
        }
//        client.delete().forPath(path + "/test01");
//        Thread.sleep(10);
//        client.delete().forPath(path + "/test02");
//        Thread.sleep(1000 * 5);

        pathCache.close();  // 关闭缓存
        client.close();
        System.out.println("OK!");
    }

    // TODO 创建节点
    public static String createZkNode( CuratorFramework zkClient , String path , CreateMode nodeMode ) throws Exception {
        CreateBuilder createBuilder = zkClient.create();

        createBuilder.withMode( nodeMode );
        String flag = createBuilder.forPath(path);

        return flag;
    }


}
