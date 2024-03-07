package com.soulw.common.nameserver.sdk;

import com.soulw.common.nameserver.domain.client.ClientConfig;
import com.soulw.common.nameserver.domain.context.model.Heartbeat;
import com.soulw.common.nameserver.domain.context.model.Vote;
import com.soulw.common.nameserver.dto.QueryClients;
import com.soulw.common.nameserver.dto.Result;
import feign.Headers;
import feign.RequestLine;

import java.util.Map;

/**
 * Created by SoulW on 2024/3/5.
 *
 * @author SoulW
 */
@Headers({"Content-Type:application/json;charset=UTF-8", "Accept: */*"})
public interface VoteApi {

    @RequestLine("POST /vote/heartbeat")
    Result<Void> heartbeat(Heartbeat request);

    @RequestLine("POST /vote/clusters")
    Result<Map<String, ClientConfig>> queryClusters(QueryClients queryClients);

    @RequestLine("POST /vote/master/sync")
    Result<Void> syncMaster(Vote vote);

    @RequestLine("POST /vote/accept")
    Result<Void> acceptVote(Vote vote);

}
