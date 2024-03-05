package com.soulw.common.nameserver.domain.client;

import com.google.common.collect.Maps;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * Created by SoulW on 2024/3/5.
 *
 * @author SoulW
 * @since 2024/3/5 13:45
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class Cluster extends BaseModel {
    private String clusterName;
    private Map<String/** groupName **/, ClientGroup> groups = Maps.newConcurrentMap();
}
