package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    RedisUtil redisUtil;


    @Override
    public String genTradCode(String memberId) {
        Jedis jedis = null;
        String tradCode = "";
        try{
            jedis = redisUtil.getJedis();
            String tradKey = "user:" + memberId + ":tradeCode";
            tradCode = UUID.randomUUID().toString();
            jedis.setex(tradKey,60*15,tradCode);
        }catch (Exception e){

        }finally {
            jedis.close();
        }
        return tradCode;
    }

    @Override
    public String checkTradCode(String memberId, String tradeCode) {
        Jedis jedis = null;
        String success = "fail";
        try{
            jedis = redisUtil.getJedis();
            String tradKey = "user:" + memberId + ":tradeCode";
            String tradCodeFromCache = jedis.get(tradKey);
            if (StringUtils.isNotBlank(tradCodeFromCache) && tradCodeFromCache.equals(tradeCode)){
                // 使用 lua脚本在发现key同时删除该key 防止并发订单攻击
                String script = "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
                jedis.eval(script, Collections.singletonList(tradKey),Collections.singletonList(tradKey));
//                jedis.del(tradKey);
                success = "success";
            }
        }catch (Exception e){

        }finally {
            jedis.close();
        }
        return success;
    }
}
