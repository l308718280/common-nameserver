package com.soulw.common.nameserver.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.soulw.common.nameserver.domain.client.ClientConfig;
import com.soulw.common.nameserver.domain.context.model.Heartbeat;
import com.soulw.common.nameserver.domain.context.model.Vote;
import com.soulw.common.nameserver.domain.context.service.VoteService;
import com.soulw.common.nameserver.dto.QueryClients;
import com.soulw.common.nameserver.dto.Result;
import com.soulw.common.nameserver.sdk.VoteApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Created by SoulW on 2024/3/5.
 *
 * @author SoulW
 * @since 2024/3/5 14:14
 */
@Slf4j
@RestController
@RequestMapping("/vote")
public class VoteController implements VoteApi {

    @Resource
    private VoteService voteService;

    /**
     * 处理心跳请求
     *
     * @param heartbeat 心跳对象
     * @return 返回心跳处理结果
     */
    @PostMapping("/heartbeat")
    @Override
    public Result<Void> heartbeat(@RequestBody Heartbeat heartbeat) {
        return call(heartbeat, () -> {
            voteService.heartbeat(heartbeat);
            return Result.success(null);
        });
    }

    private <T> Result<T> call(Object request, Supplier<Result<T>> supplier) {
        Result<T> r;
        try {
            r = supplier.get();
        } catch (Exception e) {
            log.error("call() error", e);
            r = Result.failed(e.getMessage());
        }
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        log.info("Receive Http Request [{}()] >>> request={}, response={}", trace[2].getMethodName(), JSON.toJSONString(request),
                JSON.toJSONString(r, SerializerFeature.PrettyFormat));
        return r;
    }

    /**
     * 查询集群
     *
     * @param queryClients 查询集群的请求体
     * @return 查询到的集群信息
     */
    @PostMapping("/clusters")
    @Override
    public Result<Map<String, ClientConfig>> queryClusters(@RequestBody QueryClients queryClients) {
        return call(queryClients, () -> Result.success(voteService.queryClients()));
    }

    /**
     * 对主数据进行同步操作
     *
     * @param vote 传入的投票对象
     * @return 返回空值
     */
    @PostMapping("/master/sync")
    @Override
    public Result<Void> syncMaster(@RequestBody Vote vote) {
        return call(vote, () -> {
            voteService.masterSync(vote);
            return Result.success(null);
        });
    }

    /**
     * 接受投票
     *
     * @param vote 投票对象
     * @return 无返回结果
     */
    @PostMapping("/accept")
    @Override
    public Result<Void> acceptVote(@RequestBody Vote vote) {
        return call(vote, () -> {
            voteService.accept(vote);
            return Result.success(null);
        });
    }
}
