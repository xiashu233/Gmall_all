package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.SpuService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class SpuServiceImpl implements SpuService {

    @Autowired
    PmsProductInfoMapper pmsProductInfoMapper;
    @Autowired
    PmsBaseSaleAttrMapper pmsBaseSaleAttrMapper;
    @Autowired
    PmsProductSaleAttrValueMapper pmsProductSaleAttrValueMapper;
    @Autowired
    PmsProductImageMapper pmsProductImageMapper;
    @Autowired
    PmsProductSaleAttrMapper pmsProductSaleAttrMapper;

    @Override
    public List<PmsProductInfo> spuList(String catalog3Id) {
        PmsProductInfo pmsProductInfo = new PmsProductInfo();
        pmsProductInfo.setCatalog3Id(catalog3Id);
        List<PmsProductInfo> pmsProductInfos = pmsProductInfoMapper.select(pmsProductInfo);
        return pmsProductInfos;
    }

    @Override
    public List<PmsBaseSaleAttr> baseSaleAttrList() {
        List<PmsBaseSaleAttr> pmsBaseSaleAttrs = pmsBaseSaleAttrMapper.selectAll();
        return pmsBaseSaleAttrs;
    }

    @Override
    public String saveSpuInfo(PmsProductInfo pmsProductInfo) {
        try{
            pmsProductInfoMapper.insertSelective(pmsProductInfo);
            String id = pmsProductInfo.getId();
            List<PmsProductSaleAttr> pmsProductSaleAttrs = pmsProductInfo.getSpuSaleAttrList();
            for (PmsProductSaleAttr pmsProductSaleAttr : pmsProductSaleAttrs) {
                pmsProductSaleAttr.setProductId(id);
                pmsProductSaleAttrMapper.insertSelective(pmsProductSaleAttr);
                List<PmsProductSaleAttrValue> spuSaleAttrValueList = pmsProductSaleAttr.getSpuSaleAttrValueList();
                for (PmsProductSaleAttrValue pmsProductSaleAttrValue : spuSaleAttrValueList) {
                    pmsProductSaleAttrValue.setProductId(id);
                    pmsProductSaleAttrValueMapper.insertSelective(pmsProductSaleAttrValue);
                }
            }
            List<PmsProductImage> pmsProductImages = pmsProductInfo.getSpuImageList();
            for (PmsProductImage pmsProductImage : pmsProductImages) {
                pmsProductImage.setProductId(id);
                pmsProductImageMapper.insertSelective(pmsProductImage);
            }
        }catch (Exception e){
            e.printStackTrace();
            return "插入失败";
        }
        return "插入成功";
    }

    @Override
    public List<PmsProductImage> spuImageList(String spuId) {
        PmsProductImage pmsProductImage = new PmsProductImage();
        pmsProductImage.setProductId(spuId);
        List<PmsProductImage> pmsProductImages = pmsProductImageMapper.select(pmsProductImage);
        return pmsProductImages;
    }

    @Override
    public List<PmsProductSaleAttr> spuSaleAttrList(String spuId) {
        PmsProductSaleAttr pmsProductSaleAttr = new PmsProductSaleAttr();
        pmsProductSaleAttr.setProductId(spuId);
        List<PmsProductSaleAttr> pmsProductSaleAttrs = pmsProductSaleAttrMapper.select(pmsProductSaleAttr);

        for (PmsProductSaleAttr productSaleAttr : pmsProductSaleAttrs) {
            PmsProductSaleAttrValue pmsProductSaleAttrValue = new PmsProductSaleAttrValue();
            pmsProductSaleAttrValue.setProductId(spuId);
            pmsProductSaleAttrValue.setSaleAttrId(productSaleAttr.getSaleAttrId());
            List<PmsProductSaleAttrValue> pmsProductSaleAttrValueList = pmsProductSaleAttrValueMapper.select(pmsProductSaleAttrValue);
            productSaleAttr.setSpuSaleAttrValueList(pmsProductSaleAttrValueList);
        }
        return pmsProductSaleAttrs;
    }


}
