package com.soulw.common.nameserver.domain.context.service;

import com.soulw.common.nameserver.domain.client.ClientConfig;
import com.soulw.common.nameserver.domain.context.model.Heartbeat;
import com.soulw.common.nameserver.domain.context.model.Vote;

import java.util.Map;

/**
 * Created by SoulW on 2024/3/5.
 *
 * @author SoulW
 */
public interface VoteService {
    /**
     * 接受投票并返回投票是否被接受
     *
     * @param vote 表示投票的对象
     * @return 如果投票被接受，则返回true；否则返回false
     */
    boolean accept(Vote vote);

    /**
     * 心跳检测方法
     *
     * @param heartbeat 心跳对象
     * @return 结果对象
     */
    boolean heartbeat(Heartbeat heartbeat);

    /**
     * 主同步方法，用于同步投票信息
     *
     * @param vote 要同步的投票信息
     */
    void masterSync(Vote vote);

    /**
     * 查询集群信息
     *
     * @return 返回包含集群信息的Map对象
     */
    Map<String, ClientConfig> queryClients();
}
