package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.mq.ActiveMQUtil;
import com.atguigu.gmall.order.mapper.OmsOrderItemMapper;
import com.atguigu.gmall.order.mapper.OmsOrderMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import sun.rmi.runtime.Log;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
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
    @Autowired
    ActiveMQUtil activeMQUtil;

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

    @Override
    public OmsOrder getOrderByOutTradeNo(String outTradeNo) {
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(outTradeNo);
        OmsOrder order = omsOrderMapper.selectOne(omsOrder);
        return order;
    }

    @Override
    public void updateOrder(OmsOrder order) {
        OmsOrder updateOrder = new OmsOrder();
        updateOrder.setStatus(1);
        updateOrder.setOrderSn(order.getOrderSn());

        Example example = new Example(OmsOrder.class);
        example.createCriteria().andEqualTo("orderSn",order.getOrderSn());


        // 发送订单已支付的队列
        Connection connection = null;
        Session session = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);

        } catch (JMSException ex) {
            ex.printStackTrace();
        }

        try{
            // 支付成功后 引起后台服务，订单服务的更新，库存服务，物流服务（消息队列）
            omsOrderMapper.updateByExampleSelective(updateOrder,example);
            Queue payhment_success_queue = session.createQueue("ORDER_PAY_QUEUE");
            MessageProducer producer = session.createProducer(payhment_success_queue);
            // message 是个接口
            // TextMessage textMessage = new ActiveMQTextMessage(); // 字符串文本
            MapMessage mapMessage = new ActiveMQMapMessage(); // hash结构

            producer.send(mapMessage);
            session.commit();

        }catch (Exception e1){
            try {
                session.rollback();
            } catch (JMSException ex) {
                ex.printStackTrace();
            }
        }finally {
            try {
                session.close();
            } catch (JMSException ex) {
                ex.printStackTrace();
            }
        }


    }
}
