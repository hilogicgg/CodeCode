package com.CodeCode;

import com.CodeCode.utils.ZkCuratorUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;

/**
 * TODO
 *
 * @Description
 * @Author Administrator
 * @Date 2025-06-18 21:51
 **/
public class MainZK {
    public static void main(String[] args) throws Exception {

        CuratorFramework cClient = ZkCuratorUtil.getzkClient();

        NodeCache nodeCache = ZkCuratorUtil.watchNode(cClient, "/flag");

        Thread.sleep(Long.MAX_VALUE);
    }
}
