package com.soulw.common.nameserver.domain.context.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.soulw.common.nameserver.config.SystemConfig;
import com.soulw.common.nameserver.domain.client.ClientConfig;
import com.soulw.common.nameserver.domain.client.Role;
import com.soulw.common.nameserver.domain.context.gateway.VoteGateway;
import com.soulw.common.nameserver.domain.context.model.Context;
import com.soulw.common.nameserver.domain.context.model.Heartbeat;
import com.soulw.common.nameserver.domain.context.model.Vote;
import com.soulw.common.nameserver.domain.context.service.VoteService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by SoulW on 2024/3/5.
 *
 * @author SoulW
 * @since 2024/3/5 13:55
 */
@Component
@Slf4j
@Data
public class VoteServiceImpl implements VoteService {

    @Resource
    private SystemConfig systemConfig;
    @Resource
    private VoteGateway voteGateway;
    /**
     * 当前上下文
     */
    private Context context;
    private ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(2, buildThreadFactory("voteScheduler-%s"));
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 30, TimeUnit.MINUTES, new LinkedBlockingQueue<>(),
            buildThreadFactory("voteThreadPool-%s"), new ThreadPoolExecutor.CallerRunsPolicy());
    private HeartbeatWorker heartbeatWorker = new HeartbeatWorker();
    private ClusterWorker clusterWorker = new ClusterWorker();

    /**
     * 构建一个线程工厂
     *
     * @param namePattern 线程名称的格式模式
     * @return 返回一个线程工厂
     */
    public static ThreadFactory buildThreadFactory(String namePattern) {
        return new ThreadFactoryBuilder()
                .setDaemon(true)
                .setPriority(Thread.NORM_PRIORITY)
                .setNameFormat(namePattern)
                .setUncaughtExceptionHandler((t, e) -> log.error(e.getMessage(), e))
                .build();
    }

    /**
     * 应用初始化
     */
    @PostConstruct
    public void init() {
        context = new Context();
        context.setSystemConfig(systemConfig);
        context.setClusterName(systemConfig.getCluster());
        context.setGroupName(systemConfig.getClientGroup());
        context.setClientName(ClientConfig.calculateClientName(systemConfig.getIp(), systemConfig.getPort()));

        if (systemConfig.isCp()) {
            scheduler.scheduleAtFixedRate(heartbeatWorker, 0,
                    systemConfig.getHeartbeatTime(), TimeUnit.MILLISECONDS);
            scheduler.scheduleAtFixedRate(clusterWorker, systemConfig.getClusterSyncTime(),
                    systemConfig.getClusterSyncTime(), TimeUnit.MILLISECONDS);
        }
    }

    private Heartbeat newHeartbeat(String ip, Integer port, Role role) {
        Preconditions.checkNotNull(role, "role is null");
        Heartbeat r = new Heartbeat();
        r.setClientConfig(new ClientConfig().setIp(ip)
                .setPort(port)
                .setRole(role.name()));
        r.setCluster(systemConfig.getCluster());
        r.setGroupCode(systemConfig.getClientGroup());
        return r;

    }

    private Heartbeat newHeartbeat() {
        return newHeartbeat(systemConfig.getIp(), systemConfig.getPort(), Role.MASTER);
    }

    @Override
    public boolean heartbeat(Heartbeat heartbeat) {
        Preconditions.checkNotNull(heartbeat, "heartbeat is null");
        ClientConfig requestClient = heartbeat.getClientConfig();
        ClientConfig storageClient = context.getClients().computeIfAbsent(heartbeat.getClientConfig().getClientName(),
                e -> new ClientConfig()
                        .setClientName(e)
                        .setRole(StringUtils.defaultIfBlank(requestClient.getRole(), Role.SLAVE.name()))
                        .setClientName(ClientConfig.calculateClientName(requestClient.getIp(), requestClient.getPort()))
                        .setIp(requestClient.getIp())
                        .setPort(requestClient.getPort()));
        storageClient.setHeartbeatTime(System.currentTimeMillis());
        return true;
    }

    @Override
    public Map<String, ClientConfig> queryClients() {
        return context.getClients();
    }

    @Override
    public boolean accept(Vote vote) {
        // step1. 心跳正常不接受投票
        ClientConfig curConfig = context.getCurConfig();
        if (Objects.nonNull(curConfig) && !curConfig.isTimeout(systemConfig.getHeartbeatTime())) {
            return false;
        }

        if (context.compareToReplace(vote)) {
            if ((context.getVotingFlag().get() && context.isAlreadyVoteTimeout()) ||
                    context.getVotingFlag().compareAndSet(false, true)) {
                log.info("accept() accepted, vote={}", vote);
                context.setVote(vote);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void masterSync(Vote vote) {
        if (context.compareToReplace(vote)) {
            context.getVotingFlag().set(false);
            context.setVote(vote);
            SystemConfig.Node master = getNode(vote);
            voteGateway.slaveHeartbeat(master, context);
            Map<String, ClientConfig> clients = voteGateway.queryClients(master);
            context.setClients(clients);
            log.info("masterSync() success, vote={}, clients={}", vote, clients);
        }
    }

    private SystemConfig.Node getNode(Vote vote) {
        for (SystemConfig.Node node : systemConfig.getNodes()) {
            if (StringUtils.equalsIgnoreCase(node.getIp(), vote.getIp()) &&
                    Objects.equals(node.getPort(), vote.getPort())) {
                return node;
            }
        }
        return null;
    }

    private Boolean sendVoteRequest(SystemConfig.Node node, Vote vote) {
        try {
            return voteGateway.sendVoteRequest(node, vote);
        } catch (Exception e) {
            log.error("sendVoteRequest() failed", e);
            return false;
        }
    }

    private void sendMasterSync(SystemConfig.Node node, Vote vote) {
        try {
            voteGateway.sendMasterSync(node, vote);
        } catch (Exception e) {
            log.error("sendMasterSync() failed, node={}, vote={}", node, vote, e);
        }
    }

    /**
     * 执行选举
     */
    private void doVote() {
        if (context.isVoting() || context.isCurMaster()) {
            return;
        }
        if (!context.getVotingFlag().compareAndSet(false, true)) {
            return;
        }
        try {
            // step1. 发起第一轮选举
            Vote vote = newVote();
            context.setVote(vote);
            // step2. 并发请求
            ExecutorCompletionService<Boolean> service = new ExecutorCompletionService<>(executor);
            int count = 0;
            SystemConfig.Node curNode = context.getCurNode();
            for (SystemConfig.Node node : context.getAllNodes()) {
                count++;
                if (curNode == node) {
                    continue;
                }
                service.submit(() -> sendVoteRequest(node, vote));
            }
            // step3. 等待结果
            long startTime = System.currentTimeMillis(), remainTime;
            int len = (count / 2) + 1;
            int acceptTimes = 1;
            boolean isOk = false;
            for (int i = 0; i < count - 1; i++) {
                remainTime = systemConfig.getVoteTimeout() - (System.currentTimeMillis() - startTime);
                if (remainTime <= 0) {
                    log.error("accept() remainTimes<=0");
                    break;
                }
                try {
                    Future<Boolean> future = service.poll(remainTime, TimeUnit.MILLISECONDS);
                    if (Objects.isNull(future)) {
                        continue;
                    }
                    if (Objects.equals(Boolean.TRUE, future.get())) {
                        acceptTimes++;
                    }
                    if (acceptTimes >= len) {
                        isOk = true;
                        break;
                    }
                } catch (Exception e) {
                    log.error("accept(vote) failed", e);
                }
            }
            if (!isOk) {
                log.error("doVote() vote failed, acceptTimes={}", acceptTimes);
                return;
            }

            // step4. 同步成功
            context.getClients().clear();
            for (SystemConfig.Node node : context.getAllNodes()) {
                if (curNode == node) {
                    heartbeat(newHeartbeat(node.getIp(), node.getPort(), Role.MASTER));
                    continue;
                }
                executor.submit(() -> {
                    sendMasterSync(node, vote);
                });
            }
        } catch (Exception e) {
            log.error("doVote() failed", e);
        } finally {
            context.setVote(null);
            context.getVotingFlag().set(false);
        }
    }

    private Vote newVote() {
        Vote vote = new Vote();
        vote.setVoteId(UUID.randomUUID().toString().replace("-", ""));
        vote.setVoteTime(System.nanoTime());
        vote.setBeginClientName(context.getClientName());
        vote.setIp(systemConfig.getIp());
        vote.setPort(systemConfig.getPort());
        return vote;
    }

    /**
     * 集群worker
     *
     * @author Soulw
     */
    public class ClusterWorker implements Runnable {
        @Override
        public void run() {
            if (!systemConfig.isCp()) {
                return;
            }
            if (context.isVoting()) {
                log.info("ClusterWorker.run() is voting, skip...");
                return;
            }
            if (context.isCurMaster()) {
                log.info("ClusterWorker.run() is master, skip...");
                return;
            }
            if (Objects.nonNull(context.getVote())) {
                SystemConfig.Node masterNode = getNode(context.getVote());
                context.setClients(voteGateway.queryClients(masterNode));
            }
        }
    }

    /**
     * 心跳执行器
     *
     * @author Soulw
     */
    public class HeartbeatWorker implements Runnable {
        private long heartbeatTime = -1;

        @Override
        public void run() {
            // step1. 检测是CP类型
            if (!systemConfig.isCp() || !systemConfig.getHeartbeat()) {
                return;
            }
            // step2. 检测在选举中
            if (context.isVoting()) {
                log.info("heartbeatWorker.run() voting, skip...");
                return;
            }
            // step3. 检测不是master
            if (context.isCurMaster()) {
                heartbeat(newHeartbeat());
                heartbeatTime = System.currentTimeMillis();
                log.info("heartbeatWorker.run() master, skip...");
                return;
            }
            // step4. 发送心跳
            try {
                if (Objects.nonNull(context.getVote())) {
                    voteGateway.slaveHeartbeat(getNode(context.getVote()), context);
                    heartbeatTime = System.currentTimeMillis();
                }
            } catch (Exception e) {
                log.error("heartbeat failed", e);
            }
            // step5. 检测是否发起选举
            if ((System.currentTimeMillis() - heartbeatTime) >= systemConfig.getHeartbeatTime()) {
                try {
                    Thread.sleep((long) (500 * Math.random()));
                } catch (InterruptedException e) {
                    log.error("sleep failed", e);
                }
                doVote();
                if (context.isCurMaster()) {
                    heartbeatTime = System.currentTimeMillis();
                }
            }
        }
    }

}
