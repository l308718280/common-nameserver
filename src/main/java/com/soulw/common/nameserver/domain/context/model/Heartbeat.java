package com.soulw.common.nameserver.domain.context.model;

import com.soulw.common.nameserver.domain.client.BaseModel;
import com.soulw.common.nameserver.domain.client.ClientConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by SoulW on 2024/3/5.
 *
 * @author SoulW
 * @since 2024/3/5 14:25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Heartbeat extends BaseModel {
    private String cluster;
    private String groupCode;
    private ClientConfig clientConfig;
}
