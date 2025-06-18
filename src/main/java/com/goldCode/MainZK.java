package com.goldCode;

import com.goldCode.utils.ZkCuratorUtil;
import org.apache.curator.framework.CuratorFramework;

/**
 * TODO
 *
 * @Description
 * @Author Administrator
 * @Date 2025-06-18 21:51
 **/
public class MainZK {
    public static void main(String[] args) throws Exception {
        CuratorFramework client = ZkCuratorUtil.getzkClient();

        ZkCuratorUtil.watchNode( client , "/flag"  );



        Thread.sleep(Long.MAX_VALUE);

        client.close(); // 在线程睡眠完成之后再执行
    }
}
