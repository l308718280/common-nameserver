package com.soulw.common.nameserver.controller;

import com.alibaba.fastjson.JSON;
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
    public Result<Boolean> heartbeat(@RequestBody Heartbeat heartbeat) {
        printInputParam(heartbeat);
        return call(() -> Result.success(voteService.heartbeat(heartbeat)));
    }

    private static void printInputParam(Object heartbeat) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        log.info("call() [{}()] >>> request={}", trace[2].getMethodName(), JSON.toJSONString(heartbeat));
    }

    private <T> Result<T> call(Supplier<Result<T>> supplier) {
        Result<T> r;
        try {
            r = supplier.get();
        } catch (Exception e) {
            log.error("call() error", e);
            r = Result.failed(e.getMessage());
        }
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        log.info("call() [{}()] <<< response={}", trace[2].getMethodName(), JSON.toJSONString(r));
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
        printInputParam(queryClients);
        return call(() -> Result.success(voteService.queryClients()));
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
        printInputParam(vote);
        return call(() -> {
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
    public Result<Boolean> acceptVote(@RequestBody Vote vote) {
        printInputParam(vote);
        return call(() -> Result.success(voteService.accept(vote)));
    }
}
