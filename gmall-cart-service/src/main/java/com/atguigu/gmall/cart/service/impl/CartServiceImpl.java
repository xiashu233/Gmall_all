package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.cart.mapper.OmsCartItemMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.internal.cglib.asm.$MethodVisitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    OmsCartItemMapper omsCartItemMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public List<OmsCartItem> getCartsByUser(String memberId) {
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        List<OmsCartItem> omsCartItems = omsCartItemMapper.select(omsCartItem);
        return omsCartItems;
    }

    @Override
    public void updateCart(OmsCartItem omsCartItemFromDb) {
        Example example = new Example(OmsCartItem.class);
        example.createCriteria().andEqualTo("id",omsCartItemFromDb.getId());
        omsCartItemMapper.updateByExampleSelective(omsCartItemFromDb,example);
    }

    @Override
    public void addCart(OmsCartItem omsCartItem) {
        if (StringUtils.isNotBlank(omsCartItem.getMemberId())) {
            omsCartItemMapper.insertSelective(omsCartItem);//避免添加空值
        }
    }

    @Override
    public OmsCartItem ifCartExistByUser(String memberId, String skuId) {
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);
        OmsCartItem cartItem = new OmsCartItem();
        try{
            cartItem = omsCartItemMapper.selectOne(omsCartItem);
        }catch (Exception e){
            e.printStackTrace();
        }

        return cartItem;
    }

    @Override
    public void flushCartCache(String memberId) {
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        List<OmsCartItem> omsCartItems = omsCartItemMapper.select(omsCartItem);

        // 同步到 redis缓存
        Jedis jedis = redisUtil.getJedis();

        try{
            Map<String,String> map = new HashMap<>();

            for (OmsCartItem cartItem : omsCartItems) {
                map.put(cartItem.getProductSkuId(), JSON.toJSONString(cartItem));
            }
            // 对象名：id：关系名 user:id:cart
            // Redis 存放 hash结构的 kv
            jedis.del("user:"+memberId+":cart");
            jedis.hmset("user:" + memberId + ":cart",map);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            jedis.close();
        }

    }

    @Override
    public List<OmsCartItem> cartList(String memberId) {
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        Jedis jedis = redisUtil.getJedis();
        try{
            List<String> hvals = jedis.hvals("user:" + memberId + ":cart");

            if (hvals != null){
                for (String hval : hvals) {
                    OmsCartItem omsCartItem = JSON.parseObject(hval, OmsCartItem.class);
                    omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));
                    omsCartItems.add(omsCartItem);
                }
                // 设置总价方法 setTotalPrice(omsCartItems);


            }

        }catch (Exception e){
            // 处理异常 记录系统日志
            e.printStackTrace();
            return null;

            // 严谨的话可以有一个 日志服务 记录错误日志 比如
            // String message = e.getMessage();
            // logService.addError(message)
        }finally {
            jedis.close();
        }

        return omsCartItems;
    }



    @Override
    public void checkCart(OmsCartItem omsCartItem) {
        // 修改数据库购物车状态
        Example example = new Example(OmsCartItem.class);
        example.createCriteria().andEqualTo("memberId",omsCartItem.getMemberId()).andEqualTo("productSkuId",omsCartItem.getProductSkuId());
        omsCartItemMapper.updateByExampleSelective(omsCartItem,example);
        // 缓存同步
        flushCartCache(omsCartItem.getMemberId());
    }

    @Override
    public BigDecimal getAllTotalPrice(List<OmsCartItem> omsCartItems) {
        BigDecimal allTotalPrice = new BigDecimal("0.0");
        for (OmsCartItem omsCartItemd : omsCartItems) {
            if (omsCartItemd.getIsChecked().equals("1")){
                allTotalPrice = allTotalPrice.add(omsCartItemd.getTotalPrice());
            }
        }
        return allTotalPrice;
    }

    @Override
    public void delCartByProcId(String productId) {
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setProductId(productId);
        omsCartItemMapper.delete(omsCartItem);
    }

    // 获取单个商品总价
    private void setTotalPrice(List<OmsCartItem> omsCartItems) {
        for (OmsCartItem omsCartItem : omsCartItems) {
            omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));
        }
    }
}
