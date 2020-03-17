package com.atguigu.gmall.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderServiceMqListener {

    @Autowired
    OrderService orderService;

    @JmsListener(destination = "PAYMENT_SUCCESS_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePaymentResult(MapMessage mapMessage){
        try {
            String out_trad_no = mapMessage.getString("out_trad_no");
            // 更新订单信息的状态
            OmsOrder order = new OmsOrder();
            order.setOrderSn(out_trad_no);
            orderService.updateOrder(order);

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
