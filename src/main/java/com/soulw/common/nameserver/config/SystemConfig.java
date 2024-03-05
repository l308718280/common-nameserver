package com.soulw.common.nameserver.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Objects;

/**
 * Created by SoulW on 2024/3/5.
 *
 * @author SoulW
 * @since 2024/3/5 13:44
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ns")
public class SystemConfig {
    /**
     * 强一致性
     */
    private static final int TYPE_CP = 2;
    /**
     * 当前集群
     */
    private String cluster = "defaultCluster";
    /**
     * 当前分组
     */
    private String clientGroup = "defaultGroup";
    /**
     * 类型1-高可用，2-cp强一致
     */
    private Integer type = 2;
    /**
     * 选举超时时间
     */
    private long voteTimeout = 60_000;
    /**
     * 所有的节点
     */
    private List<Node> nodes;
    /**
     * 最小选举赞同节点
     */
    private Integer minNodeLen = 2;
    /**
     * 当前端口
     */
    @Value("${server.port}")
    private int port;
    /**
     * 当前ip
     */
    private String ip = "127.0.0.1";
    /**
     * 心跳时间
     */
    private Long heartbeatTime = 10_000L;
    /**
     * 集群更新时间
     */
    private Long clusterSyncTime = 3_000L;
    /**
     * 启动心跳
     */
    private Boolean heartbeat = true;

    /**
     * 判断是否为CP类型
     *
     * @return 如果是CP类型则返回true，否则返回false
     */
    public boolean isCp() {
        return Objects.equals(TYPE_CP, type);
    }

    /**
     * 获取心跳时间差
     *
     * @return 心跳时间差加上1000的结果
     */
    public Long getHeartbeatTimeDelta() {
        return heartbeatTime + 1000;
    }

    /**
     * 节点
     *
     * @author Soulw
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode
    public static class Node {
        private String ip;
        private Integer port;
    }

}
