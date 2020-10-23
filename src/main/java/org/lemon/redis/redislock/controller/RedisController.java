package org.lemon.redis.redislock.controller;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedisController {

    @AutoConfigureOrder
    private StringRedisTemplate stringRedisTemplate;

    @RequestMapping(value = "/sale")
    public String sale() {
        int count = Integer.parseInt(stringRedisTemplate.opsForValue().get("count"));
        if (count > 1) {
            count--;
            stringRedisTemplate.opsForValue().set("count", count + "");
            System.out.println("库存扣减成功,剩余库存:" + count);
        } else {
            System.out.println("库存扣减失败");
        }
        return "end";
    }
}
