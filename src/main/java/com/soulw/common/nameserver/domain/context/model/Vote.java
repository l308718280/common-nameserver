package com.soulw.common.nameserver.domain.context.model;

import com.soulw.common.nameserver.domain.client.BaseModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by SoulW on 2024/3/5.
 *
 * @author SoulW
 * @since 2024/3/5 13:53
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Vote extends BaseModel {
    private String voteId;
    private String beginClientName;
    private Long voteTime;
    private String ip;
    private Integer port;
}
