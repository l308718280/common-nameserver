package com.soulw.common.nameserver.domain.client;

import com.google.common.collect.Maps;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by SoulW on 2024/3/5.
 *
 * @author SoulW
 * @since 2024/3/5 13:45
 */
@Data
public abstract class BaseModel {
    private Map<String, Object> ext = Maps.newHashMap();
}
