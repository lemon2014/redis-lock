package org.lemon.redis.redislock.controller;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
public class RedisController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private Redisson redisson;
    /**
     * 这里由于获取库存，扣减，更新到缓存中 三个操作不是原子性的，所以并发情况下存在问题
     *
     * 直接再开启一个服务，nginx做负载，做压力测试，就可以看到效果
     */
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

    @RequestMapping(value = "/sale/lock1")
    public String saleLock1() {

        //获取lock
        boolean locked = stringRedisTemplate.boundValueOps("lock").setIfAbsent("1");
        try {
            //获取到lock继续执行
            if(!locked) {
                int count = Integer.parseInt(stringRedisTemplate.opsForValue().get("count"));
                if (count > 1) {
                   count--;
                   stringRedisTemplate.opsForValue().set("count", count + "");
                   System.out.println("库存扣减成功,剩余库存:" + count);
                } else {
                   System.out.println("库存扣减失败");
                }
            }
        } finally {
            //释放锁  如果执行到这里down机了,导致其他请求都无法执行, 问题很大
            stringRedisTemplate.delete("lock");
        }
        return "end";
    }

    @RequestMapping(value = "/sale/lock2")
    public String saleLock2() {

        //获取lock, 为了防止释放锁的时候down机, 这里加上过期时间, 自动释放锁
        //但是由于不确认业务处理的时间, 这里使用10秒释放锁, 高并发的情况下还是会有问题,具体问题见下图
        boolean locked = stringRedisTemplate.boundValueOps("lock").setIfAbsent("1", 10, TimeUnit.SECONDS);
        try {
            //获取到lock继续执行
            if(!locked) {
                int count = Integer.parseInt(stringRedisTemplate.opsForValue().get("count"));
                if (count > 1) {
                    count--;
                    stringRedisTemplate.opsForValue().set("count", count + "");
                    System.out.println("库存扣减成功,剩余库存:" + count);
                } else {
                    System.out.println("库存扣减失败");
                }
            }
        } finally {
            // 不等待时间到期，提前主动释放锁
            stringRedisTemplate.delete("lock");
        }
        return "end";
    }

    @RequestMapping(value = "/sale/lock3")
    public String saleLock3() {

        //获取lock, 为了防止释放锁的时候down机, 这里加上过期时间, 自动释放锁
        //但是由于不确认业务处理的时间, 这里使用10秒释放锁, 高并发的情况下还是会有问题,具体问题见下图
        String clientId = UUID.randomUUID().toString();
        boolean locked = stringRedisTemplate.boundValueOps("lock").setIfAbsent(clientId, 10, TimeUnit.SECONDS);
        try {
            //获取到lock继续执行
            if(!locked) {
                int count = Integer.parseInt(stringRedisTemplate.opsForValue().get("count"));
                if (count > 1) {
                    count--;
                    stringRedisTemplate.opsForValue().set("count", count + "");
                    System.out.println("库存扣减成功,剩余库存:" + count);
                } else {
                    System.out.println("库存扣减失败");
                }
            }
        } finally {
            // 不等待时间到期，提前主动释放锁
            if(clientId.equals(stringRedisTemplate.boundValueOps("lock").get())){
                stringRedisTemplate.delete("lock");
            }
        }
        return "end";
    }

    @RequestMapping(value = "/sale/lock4")
    public String saleLock4() {
        String lockKey = "lock";
        RLock redissonLock = redisson.getLock(lockKey);
        try {
            redissonLock.lock();
            //执行相关扣减和更新缓存逻辑
        }finally {
            redissonLock.unlock();
        }
        return "end";
    }


}
