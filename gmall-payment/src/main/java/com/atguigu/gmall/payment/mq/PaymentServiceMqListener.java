package com.atguigu.gmall.payment.mq;

import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Date;
import java.util.Map;

@Component
public class PaymentServiceMqListener {

    @Autowired
    PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePaymentCheckResult(MapMessage mapMessage) throws JMSException {
        String out_trad_no = mapMessage.getString("out_trad_no");
        Integer count = 0;
        if (mapMessage.getInt("count") != 0){
            count = mapMessage.getInt("count");
        }


        // 调用 paymentService 的接口进行延迟检查
        System.out.println("进行延迟检查，调用支付检查的接口服务");

        Map<String,Object> resultMap = paymentService.checkAlipayPayment(out_trad_no);
        // 检测是否支付
        if (resultMap != null && !resultMap.isEmpty()){
            String trade_status = (String) resultMap.get("trade_status");

            // 根据查询的支付结果，判断是否进行下一次延迟任务还是支付成功更新数据和后续任务
            if (trade_status.equals("TRADE_SUCCESS")){

                System.out.println("支付成功，修改支付信息发送支付成功队列");
                // 进行支付更新的幂等性检查

                PaymentInfo paymentInfo = new PaymentInfo();

                paymentInfo.setOrderSn((String) resultMap.get("out_trade_no"));
                paymentInfo.setPaymentStatus("已支付");
                // 支付宝的交易凭证号
                paymentInfo.setAlipayTradeNo((String) resultMap.get("trade_no"));
                // 回调请求字符串
                paymentInfo.setCallbackContent((String) resultMap.get("call_back_content"));
                paymentInfo.setCallbackTime(new Date());
                paymentService.updatePaymentInfo(paymentInfo);
                return;
            }
        }

        if (count > 0){
            // 继续发送延迟检查任务
            System.out.println("没有支付成功，继续发送延迟检查队列，此次是已经发送的第" + count + "次");
            count--;
            paymentService.sendDelayPaymentResult(out_trad_no,count);
        }else{
            System.out.println("检查次数用尽，结束检查");
            return;
        }


    }
}
