package com.soulw.common.nameserver.domain.client;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by SoulW on 2024/3/5.
 *
 * @author SoulW
 * @since 2024/3/5 13:47
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ClientConfig extends BaseModel {
    /**
     * 客户端唯一标识
     */
    private String clientName;
    private String ip;
    private Integer port;
    /**
     * 角色
     *
     * @see Role
     */
    private String role;
    /**
     * 心跳时间
     */
    private long heartbeatTime = -1;

    /**
     * 获取客户端名称
     *
     * @return 客户端名称
     */
    public String getClientName() {
        if (StringUtils.isBlank(clientName)) {
            clientName = calculateClientName(ip, port);
        }
        return clientName;
    }

    /**
     * 计算客户端名称
     *
     * @param ip   客户端IP地址
     * @param port 客户端端口号
     * @return 返回拼接后的客户端名称
     * @throws NullPointerException 如果端口号为空则抛出空指针异常
     */
    public static String calculateClientName(String ip, Integer port) {
        Preconditions.checkNotNull(port, "port is null");
        return String.join("_", "client", ip, String.valueOf(port));
    }

    /**
     * 判断当前角色是否为Master
     *
     * @return 如果当前角色为Master则返回true，否则返回false
     */
    public boolean isMaster() {
        return StringUtils.equalsIgnoreCase(Role.MASTER.name(), role);
    }

    /**
     * 判断当前时间是否超时
     *
     * @param timeout 超时时间戳
     * @return 如果当前时间减去超时时间大于超时时间，则返回true；否则返回false
     */
    public boolean isTimeout(Long timeout) {
        return System.currentTimeMillis() - heartbeatTime > timeout;
    }

    /**
     * 判断输入的客户端名称是否与获取的客户端名称相等
     *
     * @param clientName 客户端名称
     * @return 如果相等返回true，否则返回false
     */
    public boolean isEquals(String clientName) {
        return StringUtils.equalsIgnoreCase(clientName, getClientName());
    }
}
