package com.soulw.common.nameserver.domain.context.gateway;

import com.soulw.common.nameserver.config.SystemConfig;
import com.soulw.common.nameserver.domain.client.ClientConfig;
import com.soulw.common.nameserver.domain.context.model.Context;
import com.soulw.common.nameserver.domain.context.model.Vote;

import java.util.Map;

/**
 * Created by SoulW on 2024/3/5.
 *
 * @author SoulW
 */
public interface VoteGateway {
    /**
     * 发送心跳以维持与主服务器的连接
     *
     * @param master  主服务器的客户端配置
     * @param context 上下文环境
     */
    void slaveHeartbeat(SystemConfig.Node master, Context context);

    /**
     * 查询集群信息
     *
     * @param master 客户端配置
     * @return 包含集群信息的映射
     */
    Map<String, ClientConfig> queryClients(SystemConfig.Node master);

    /**
     * 发送投票请求
     *
     * @param node 节点信息
     * @param vote 投票信息
     * @return 是否成功发送投票请求
     */
    Boolean sendVoteRequest(SystemConfig.Node node, Vote vote);

    /**
     * 发送主节点同步
     *
     * @param node 节点信息
     * @param vote 投票信息
     */
    void sendMasterSync(SystemConfig.Node node, Vote vote);
}
