package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.order.mapper.OmsOrderItemMapper;
import com.atguigu.gmall.order.mapper.OmsOrderMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import sun.rmi.runtime.Log;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    OmsOrderMapper omsOrderMapper;
    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;
    @Reference
    CartService cartService;

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
            //String tradCodeFromCache = jedis.get(tradKey);
            // 使用 lua脚本在发现key同时删除该key 防止并发订单攻击
             String script = "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
             Long evel = (Long) jedis.eval(script, Collections.singletonList(tradKey),Collections.singletonList(tradeCode));
            if (evel != null && evel != 0){
                //jedis.del(tradKey);
                success = "success";
            }
        }catch (Exception e){

        }finally {
            jedis.close();
        }
        return success;
    }


    @Override
    public void saveOrder(OmsOrder omsOrder) {
        // 保存订单表
        omsOrderMapper.insertSelective(omsOrder);
        String orderId =  omsOrder.getId();
        // 保存订单详情表
        List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();
        if (omsOrderItems != null){
            for (OmsOrderItem omsOrderItem : omsOrderItems) {
                omsOrderItem.setOrderId(orderId);
                omsOrderItemMapper.insertSelective(omsOrderItem);
                // 删除购物车里的数据
//                cartService.delCartByProcId(omsOrderItem.getProductId());
            }
        }


        

    }
}
