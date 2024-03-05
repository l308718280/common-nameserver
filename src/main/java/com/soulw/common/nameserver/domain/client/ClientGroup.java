package com.soulw.common.nameserver.domain.client;

import com.google.common.collect.Maps;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by SoulW on 2024/3/5.
 *
 * @author SoulW
 * @since 2024/3/5 13:46
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class ClientGroup extends BaseModel {
    /**
     * 全局唯一标识
     */
    private String groupName;
    private Map<String/** clientName **/, ClientConfig> clients = Maps.newConcurrentMap();

    /**
     * 获取主节点客户端配置
     *
     * @return 主节点客户端配置，如果不存在则返回null
     */
    public ClientConfig getMaster() {
        return CollectionUtils.emptyIfNull(clients.values()).stream()
                .filter(ClientConfig::isMaster)
                .findFirst().orElse(null);
    }
}
