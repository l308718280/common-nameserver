package com.soulw.common.nameserver.infrastructure.gateway.impl;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.soulw.common.nameserver.config.SystemConfig;
import com.soulw.common.nameserver.domain.client.ClientConfig;
import com.soulw.common.nameserver.domain.context.gateway.VoteGateway;
import com.soulw.common.nameserver.domain.context.model.Context;
import com.soulw.common.nameserver.domain.context.model.Heartbeat;
import com.soulw.common.nameserver.domain.context.model.Vote;
import com.soulw.common.nameserver.dto.QueryClients;
import com.soulw.common.nameserver.dto.Result;
import com.soulw.common.nameserver.sdk.VoteApi;
import feign.Feign;
import feign.FeignException;
import feign.Logger;
import feign.RequestTemplate;
import feign.Response;
import feign.Retryer;
import feign.Util;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Created by SoulW on 2024/3/5.
 *
 * @author SoulW
 * @since 2024/3/5 14:07
 */
@Component
@Slf4j
public class VoteGatewayImpl implements VoteGateway {

    private Map<String, VoteApi> apiCache = Maps.newConcurrentMap();

    @Override
    public void slaveHeartbeat(SystemConfig.Node master, Context context) {
        VoteApi api = getApi(master);
        Heartbeat request = new Heartbeat();
        request.setCluster(context.getClusterName());
        request.setGroupCode(context.getGroupName());
        SystemConfig config = context.getSystemConfig();
        request.setClientConfig(new ClientConfig().setIp(config.getIp())
                .setPort(config.getPort()));
        check(api.heartbeat(request));
    }

    private VoteApi getApi(SystemConfig.Node node) {
        String addr = "http://" + String.join(":", node.getIp(), String.valueOf(node.getPort()));

        return apiCache.computeIfAbsent(addr, e -> Feign.builder()
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .logger(new Logger() {
                    @Override
                    protected void log(String configKey, String format, Object... args) {
                        log.info(String.format("[" + configKey + "]" + format, args));
                    }
                })
                .retryer(new Retryer.Default(0, 0, 0))
                .logLevel(Logger.Level.NONE)
                .target(VoteApi.class, addr));
    }

    @Override
    public Map<String, ClientConfig> queryClients(SystemConfig.Node master) {
        Result<Map<String, ClientConfig>> resp = getApi(master).queryClusters(new QueryClients());
        check(resp);
        return resp.getData();
    }

    private void check(Result<?> resp) {
        Preconditions.checkNotNull(resp, "resp is null");
        Preconditions.checkState(resp.getSuccess(), "failed: " + resp.getMessage());
    }

    @Override
    public Boolean sendVoteRequest(SystemConfig.Node node, Vote vote) {
        Result<Boolean> resp = getApi(node).acceptVote(vote);
        check(resp);
        return resp.getData();
    }

    @Override
    public void sendMasterSync(SystemConfig.Node node, Vote vote) {
        Result<Void> resp = getApi(node).syncMaster(vote);
        check(resp);
    }
}
