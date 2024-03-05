package com.soulw.common.nameserver.domain.context.model;

import com.google.common.collect.Maps;
import com.soulw.common.nameserver.config.SystemConfig;
import com.soulw.common.nameserver.domain.client.ClientConfig;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by SoulW on 2024/3/5.
 *
 * @author SoulW
 * @since 2024/3/5 13:52
 */
@Data
public class Context {
    private SystemConfig systemConfig;
    private Map<String/** clientName */, ClientConfig> clients = Maps.newConcurrentMap();
    private String clusterName;
    private String groupName;
    private String clientName;
    /**
     * 是否选举中
     */
    private AtomicBoolean votingFlag = new AtomicBoolean(false);
    /**
     * 发起的选举对象
     */
    private Vote vote;
    /**
     * 属性
     */
    private Map<String, Object> attributes = Maps.newHashMap();

    /**
     * 获取主客户端配置
     *
     * @return 返回主客户端配置，如果clientGroup不为null则返回clientGroup的主客户端配置，否则返回null
     */
    public ClientConfig getMaster() {
        for (ClientConfig value : clients.values()) {
            if (value.isMaster()) {
                return value;
            }
        }
        return null;
    }

    /**
     * 获取当前客户端配置
     *
     * @return 当前客户端配置
     */
    public ClientConfig getCurConfig() {
        return clients.get(clientName);
    }

    /**
     * 判断当前配置是否为主节点
     *
     * @return 返回true表示当前节点为主节点，返回false表示当前节点不是主节点
     */
    public boolean isCurMaster() {
        ClientConfig master = getMaster();
        return Objects.nonNull(master) && master.isEquals(clientName);
    }

    /**
     * 获取所有节点的方法
     *
     * @return 返回系统配置中的所有节点列表
     */
    public List<SystemConfig.Node> getAllNodes() {
        return systemConfig.getNodes();
    }

    /**
     * 判断是否正在投票
     *
     * @return 返回是否正在投票的布尔值
     */
    public boolean isVoting() {
        return votingFlag.get();
    }

    /**
     * 比较并替换投票
     *
     * @param vote 投票对象
     * @return 如果投票已经过期或者当前投票为空，返回false；否则返回true
     */
    public boolean compareToReplace(Vote vote) {
        if (isTimeout(vote)) {
            return false;
        }
        if (Objects.isNull(this.vote) || this.vote.getVoteId().equals(vote.getVoteId()) || isTimeout(this.vote)) {
            return true;
        }
        return vote.getVoteTime() < this.vote.getVoteTime();
    }

    private boolean isTimeout(Vote vote) {
        return Objects.nonNull(vote) && (System.currentTimeMillis() - vote.getVoteTime()) > systemConfig.getVoteTimeout();
    }

    /**
     * 获取当前节点
     *
     * @return 当前节点
     */
    public SystemConfig.Node getCurNode() {
        for (SystemConfig.Node node : systemConfig.getNodes()) {
            if (StringUtils.equalsIgnoreCase(node.getIp(), systemConfig.getIp()) &&
                    Objects.equals(node.getPort(), systemConfig.getPort())) {
                return node;
            }
        }
        return null;
    }

    public boolean isAlreadyVoteTimeout() {
        return isTimeout(this.vote);
    }
}
