package com.soulw.common.nameserver.domain.context.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.soulw.common.nameserver.config.SystemConfig;
import com.soulw.common.nameserver.domain.client.ClientConfig;
import lombok.Data;

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
     * 从客户端列表中移除主客户端
     */
    public void removeMaster() {
        ClientConfig master = getMaster();
        clients.remove(master.getClientName());
    }

    /**
     * 判断系统是否健康
     *
     * @return 返回系统健康状态，true表示健康，false表示不健康
     * @throws NullPointerException 如果当前配置为空，则抛出空指针异常
     */
    public boolean isHealth() {
        ClientConfig config = getCurConfig();
        if (Objects.isNull(config)) {
            return false;
        }
        return (System.currentTimeMillis() - config.getHeartbeatTime()) < systemConfig.getHeartbeatTimeDelta();
    }

    /**
     * 查找主节点
     *
     * @return 返回包含主节点信息的Node对象
     */
    public SystemConfig.Node findMasterNode() {
        if (Objects.nonNull(vote)) {
            return convertToNode(vote);
        } else {
            return clients.values()
                    .stream().filter(ClientConfig::isMaster)
                    .map(this::convertToNode)
                    .findFirst().orElse(null);
        }
    }

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
        List<SystemConfig.Node> r = Lists.newArrayList();
        for (ClientConfig value : clients.values()) {
            SystemConfig.Node node = convertToNode(value);
            if (r.contains(node)) {
                continue;
            }
            r.add(node);
        }
        for (SystemConfig.Node node : systemConfig.getNodes()) {
            if (r.contains(node)) {
                continue;
            }
            r.add(node);
        }
        return r;
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
        return Objects.nonNull(vote) && (System.nanoTime() - vote.getVoteTime()) > systemConfig.getVoteTimeout();
    }

    /**
     * 获取当前节点
     *
     * @return 当前节点
     */
    public SystemConfig.Node getCurNode() {
        return new SystemConfig.Node().setIp(systemConfig.getIp())
                .setPort(systemConfig.getPort());
    }

    /**
     * 获取与客户端配置匹配的节点
     *
     * @param clientConfig 客户端配置
     * @return 匹配的节点，如果没有匹配的节点则返回null
     */
    public SystemConfig.Node convertToNode(ClientConfig clientConfig) {
        return new SystemConfig.Node().setIp(clientConfig.getIp())
                .setPort(clientConfig.getPort());
    }

    public boolean isAlreadyVoteTimeout() {
        return isTimeout(this.vote);
    }

    /**
     * 将投票对象转换为节点对象
     *
     * @param vote 要转换的投票对象
     * @return 转换后的节点对象
     */
    public SystemConfig.Node convertToNode(Vote vote) {
        return new SystemConfig.Node().setIp(vote.getIp())
                .setPort(vote.getPort());
    }
}
