package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuImage;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;
    @Autowired
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;
    @Autowired
    PmsSkuImageMapper pmsSkuImageMapper;
    @Autowired
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;
    @Autowired
    RedisUtil redisUtil;

    @Override
    public String saveSkuInfo(PmsSkuInfo pmsSkuInfo) {
        try{
            // 插入 skuInfo
            int key = pmsSkuInfoMapper.insertSelective(pmsSkuInfo);
            String skuId = pmsSkuInfo.getId();
            // 插入平台属性关联
            List<PmsSkuAttrValue> skuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                pmsSkuAttrValue.setSkuId(skuId);
                pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
            }
            // 插入属性销售关联
            List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
                pmsSkuSaleAttrValue.setSkuId(skuId);
                pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
            }

            // 插入图片信息
            List<PmsSkuImage> skuImageList = pmsSkuInfo.getSkuImageList();
            for (PmsSkuImage pmsSkuImage : skuImageList) {
                pmsSkuImage.setSkuId(skuId);
                pmsSkuImage.setProductImgId(pmsSkuImage.getSpuImgId());
                pmsSkuImageMapper.insertSelective(pmsSkuImage);
            }
        }catch (Exception e){
            e.printStackTrace();
            return "插入失败";
        }
        return "插入成功";
    }

    public PmsSkuInfo getSkuInfoBySkuIdFromDB(String skuId) {
        PmsSkuInfo skuInfo = new PmsSkuInfo();
        skuInfo.setId(skuId);
        PmsSkuInfo pmsSkuInfo = pmsSkuInfoMapper.selectOne(skuInfo);
        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> skuImages = pmsSkuImageMapper.select(pmsSkuImage);
        pmsSkuInfo.setSkuImageList(skuImages);

        return pmsSkuInfo;
    }

    @Override
    public PmsSkuInfo getSkuInfoBySkuId(String skuId,String ip) {
        System.out.println("ip为：" + ip + "的同学：" + Thread.currentThread().getName()+"进入了商品详情的请求");
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();

        // 链接缓存 Redis
        Jedis jedis = redisUtil.getJedis();
        // 查询缓存
        String skuKey = "sku:" + skuId + ":info";
        String skuJson = jedis.get(skuKey);
        // if(skuJson != null && !skuJson.equals(""))
        if (StringUtils.isNotBlank(skuJson)){
            System.out.println("ip为：" + ip + "的同学：" + Thread.currentThread().getName()+"从缓存中获取了商品详情");
            pmsSkuInfo = JSON.parseObject(skuJson, PmsSkuInfo.class);
        }else{
            System.out.println("ip为：" + ip + "的同学：" + Thread.currentThread().getName()+"发现缓存没有 申请缓存的分布式锁：" + "sku:"+skuId+":lock");
            // 如果缓存没有 再去查询MySQL
            // 设置分布式锁 设置10秒过期时间
            String token = UUID.randomUUID().toString();
            String OK = jedis.set("sku:" + skuId + ":lock",token,"nx","px",10*1000);

            if (StringUtils.isNotBlank(OK) && OK.equals("OK")){
                System.out.println("ip为：" + ip + "的同学：" + Thread.currentThread().getName()+"成功拿到锁 有权在10秒之内访问数据库");
                // 设置分布式锁成功 有权利访问数据库 10秒内（这样的目的就是让他们排队访问数据库）
                pmsSkuInfo = getSkuInfoBySkuIdFromDB(skuId);

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (pmsSkuInfo != null){
                    // MySql查询结果 存入 Redis
                    jedis.set(skuKey,JSON.toJSONString(pmsSkuInfo));
                }else{
                    // 数据库中不存在该数据
                    // 为了防止缓存穿透，将 null 值给 redis
                    jedis.setex(skuKey,60*3,JSON.toJSONString(pmsSkuInfo));
                }



                // 在访问 MySQL后，将 MySQL的分布式锁释放
                System.out.println("ip为：" + ip + "的同学：" + Thread.currentThread().getName()+"用完锁 将锁归还"+ "sku:"+skuId+":lock");
                // 删锁之前 校验一下是不是删的是自己的锁
                String lockToken = jedis.get("sku:" + skuId + ":lock");

                if (StringUtils.isNotBlank(lockToken) && token.equals(lockToken)){
                    String script = "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
//                    jedis.del("sku:" + skuId + ":lock");
                    jedis.eval(script, Collections.singletonList("sku:" + skuId + ":lock"),Collections.singletonList(lockToken));
                }

            }else{
                System.out.println("ip为：" + ip + "的同学：" + Thread.currentThread().getName()+"没有拿到分布式锁 开始自旋");

                // 设置失败 自旋（线程睡眠几秒后 调用自己）
                try{
                    Thread.sleep(3000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                return getSkuInfoBySkuId(skuId,ip);
            }

        }
        // 释放资源
        jedis.close();
        return pmsSkuInfo;
    }

    @Override
    public List<PmsSkuInfo> getAllSku() {
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectAll();
        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos) {
            String skuId = pmsSkuInfo.getId();
            PmsSkuAttrValue pmsSkuAttrValue = new PmsSkuAttrValue();
            pmsSkuAttrValue.setSkuId(skuId);
            List<PmsSkuAttrValue> pmsSkuAttrValues = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);
            pmsSkuInfo.setSkuAttrValueList(pmsSkuAttrValues);
        }
        return pmsSkuInfos;
    }
}
