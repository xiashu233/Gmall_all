package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.mq.ActiveMQUtil;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentInfoMapper paymentInfoMapper;
    @Autowired
    ActiveMQUtil activeMQUtil;
    @Autowired
    AlipayClient alipayClient;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insert(paymentInfo);
    }

    @Override
    public void updatePaymentInfo(PaymentInfo paymentInfo) {
        // 幂等性检查
        PaymentInfo paymentInfoParm = new PaymentInfo();
        paymentInfoParm.setOrderSn(paymentInfo.getOrderSn());
        PaymentInfo paymentInfoResult = paymentInfoMapper.selectOne(paymentInfoParm);
        if (StringUtils.isNotBlank(paymentInfoResult.getPaymentStatus()) && paymentInfoResult.getPaymentStatus().equals("已支付")){
            return;
        }

        String orderSn = paymentInfo.getOrderSn();
        Example e = new Example(PaymentInfo.class);
        e.createCriteria().andEqualTo("orderSn",orderSn);


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
            paymentInfoMapper.updateByExample(paymentInfo,e);
            Queue payhment_success_queue = session.createQueue("payhment_success_queue");
            MessageProducer producer = session.createProducer(payhment_success_queue);
            // message 是个接口
            // TextMessage textMessage = new ActiveMQTextMessage();
            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("out_trad_no",paymentInfo.getOrderSn());
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

    @Override
    public void sendDelayPaymentResult(String outTradeNo,int count) {

        Connection connection = null;
        Session session = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);


        } catch (JMSException ex) {
            ex.printStackTrace();
        }

        try{

            Queue payment_check_queue = session.createQueue("PAYMENT_CHECK_QUEUE");
            MessageProducer producer = session.createProducer(payment_check_queue);
            // message 是个接口
            // TextMessage textMessage = new ActiveMQTextMessage();
            MapMessage mapMessage = new ActiveMQMapMessage();

            mapMessage.setString("out_trad_no",outTradeNo);
            mapMessage.setInt("count",count);
            // 加入延迟时间
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,1000 * 10);
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

    @Override
    public Map<String, Object> checkAlipayPayment(String out_trad_no) {
        // 创建返回值Map
        Map<String,Object> resultMap = new HashMap<>();

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("out_trade_no",out_trad_no);
        request.setBizContent(JSON.toJSONString(requestMap));
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()){
            System.out.println("已创建交易，调用成功");
            resultMap.put("out_trade_no",response.getOutTradeNo());
            resultMap.put("trade_no",response.getTradeNo());
            resultMap.put("buyer_logon_id",response.getBuyerLogonId());
            resultMap.put("trade_status",response.getTradeStatus());
            resultMap.put("call_back_content",response.getMsg());
        }else{
            System.out.println("有可能交易未创建，调用失败");
        }

        return resultMap;
    }
}
