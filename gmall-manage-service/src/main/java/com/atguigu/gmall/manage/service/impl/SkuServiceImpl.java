package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuImage;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

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

    @Override
    public PmsSkuInfo getSkuInfoBySkuId(String skuId) {
        PmsSkuInfo skuInfo = new PmsSkuInfo();
        skuInfo.setId(skuId);
//        PmsSkuInfo pmsSkuInfo = pmsSkuInfoMapper.select(skuInfo);
        return skuInfo;
    }
}
