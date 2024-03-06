package com.soulw.common.nameserver.infrastructure.gateway.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.soulw.common.nameserver.config.SystemConfig;
import com.soulw.common.nameserver.domain.client.ClientConfig;
import com.soulw.common.nameserver.domain.context.gateway.VoteGateway;
import com.soulw.common.nameserver.domain.context.model.Context;
import com.soulw.common.nameserver.domain.context.model.Heartbeat;
import com.soulw.common.nameserver.domain.context.model.Vote;
import com.soulw.common.nameserver.dto.QueryClients;
import com.soulw.common.nameserver.dto.Result;
import com.soulw.common.nameserver.sdk.VoteApi;
import feign.Client;
import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.Response;
import feign.Retryer;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Dispatcher;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by SoulW on 2024/3/5.
 *
 * @author SoulW
 * @since 2024/3/5 14:07
 */
@Component
@Slf4j
public class VoteGatewayImpl implements VoteGateway {

    private static final OkHttpClient okHttpClient = getOkHttpClient();
    private Map<String, VoteApi> apiCache = Maps.newConcurrentMap();

    public static OkHttpClient getOkHttpClient() {
        try {
            SSLContext sslContext = SSLContext.getInstance("ssl");
            X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            sslContext.init(new KeyManager[0], new TrustManager[]{trustManager}, new SecureRandom());

            Duration timeout = Duration.ofSeconds(3);
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(timeout)
                    .callTimeout(timeout)
                    .readTimeout(timeout)
                    .writeTimeout(timeout)
                    .followSslRedirects(true)
                    .followRedirects(true)
                    .hostnameVerifier((hostname, session) -> true)
                    .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                    .dispatcher(new Dispatcher(new ThreadPoolExecutor(20, 20, 30, TimeUnit.MINUTES,
                            new SynchronousQueue<Runnable>(false), new ThreadFactoryBuilder()
                            .setNameFormat("httpPool-%s")
                            .setPriority(Thread.NORM_PRIORITY)
                            .setUncaughtExceptionHandler((t, e) -> log.error("uncaught exception, thread={}", t, e))
                            .setDaemon(true)
                            .build())))
                    .build();
            return client;
        } catch (Exception e) {
            log.error("init okHttpClient failed", e);
            throw new RuntimeException("initOkHttpClient failed", e);
        }
    }

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
                .client(new Client() {
                    @Override
                    public Response execute(Request request, Request.Options options) throws IOException {
                        okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder()
                                .url(request.url());
                        MapUtils.emptyIfNull(request.headers()).forEach((k, v) -> {
                            if (CollectionUtils.isEmpty(v)) {
                                return;
                            }
                            requestBuilder.header(k, v.iterator().next());
                        });

                        byte[] requestContent = request.body();
                        requestBuilder.post(RequestBody.create(MediaType.get("application/json;charset=UTF-8"),
                                requestContent));

                        Call call = okHttpClient.newCall(requestBuilder.build());
                        okhttp3.Response okResp = null;
                        try {
                            okResp = call.execute();
                        } catch (ConnectException ce) {
                            log.error("connect failed, url={}", request.url());
                        }

                        ResponseBody body = Objects.nonNull(okResp) && okResp.isSuccessful() ? okResp.body() : null;
                        String responseContent = Objects.nonNull(body) ? body.string() : null;

                        log.info("Send Http Request, url={}, requestBody={}, response={}",
                                request.url(), new String(requestContent, StandardCharsets.UTF_8), responseContent);
                        return Response.builder()
                                .request(request)
                                .status(Objects.nonNull(okResp) ? okResp.code() : -1)
                                .body(responseContent, StandardCharsets.UTF_8)
                                .build();
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
    public void sendVoteRequest(SystemConfig.Node node, Vote vote) {
        Result<Void> resp = getApi(node).acceptVote(vote);
        try {
            check(resp);
        } catch (Exception e) {
            log.error("Send Vote Request failed, msg={}, vote={}", e.getMessage(), vote);
            throw new RuntimeException("Send Vote Request failed, msg=" + e.getMessage());
        }
    }

    @Override
    public void sendMasterSync(SystemConfig.Node node, Vote vote) {
        Result<Void> resp = getApi(node).syncMaster(vote);
        check(resp);
    }
}
