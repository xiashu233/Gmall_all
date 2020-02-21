package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.bean.PmsBaseAttrValue;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsProductInfoMapper;
import com.atguigu.gmall.service.AttrService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
import java.util.Set;

@Service
public class AttrServiceImpl implements AttrService {

    @Autowired
    PmsBaseAttrInfoMapper pmsBaseAttrInfoMapper;
    @Autowired
    PmsBaseAttrValueMapper pmsBaseAttrValueMapper;
    @Autowired
    PmsProductInfoMapper pmsProductInfoMapper;


    @Override
    public List<PmsBaseAttrInfo> attrInfoList(String catalog3Id) {
        PmsBaseAttrInfo pmsBaseAttrInfo = new PmsBaseAttrInfo();
        pmsBaseAttrInfo.setCatalog3Id(catalog3Id);
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = pmsBaseAttrInfoMapper.select(pmsBaseAttrInfo);
        for (int i = 0; i < pmsBaseAttrInfos.size(); i++) {
            PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();
            pmsBaseAttrValue.setAttrId(pmsBaseAttrInfos.get(i).getId());
            List<PmsBaseAttrValue> pmsBaseAttrValues = pmsBaseAttrValueMapper.select(pmsBaseAttrValue);
            pmsBaseAttrInfos.get(i).setAttrValueList(pmsBaseAttrValues);
        }

        return pmsBaseAttrInfos;
    }

    @Override
    public String saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo) {
        try{
            String attrId = pmsBaseAttrInfo.getId();
            List<PmsBaseAttrValue> pmsBaseAttrValues = pmsBaseAttrInfo.getAttrValueList();
            if (StringUtils.isBlank(attrId)){
                // id为空 保存
                pmsBaseAttrInfoMapper.insertSelective(pmsBaseAttrInfo);
                for (PmsBaseAttrValue pmsBaseAttrValue:pmsBaseAttrValues){
                    pmsBaseAttrValue.setAttrId(pmsBaseAttrInfo.getId());
                    pmsBaseAttrValueMapper.insertSelective(pmsBaseAttrValue);
                }
            }else{
                // 修改属性名
                Example example = new Example(PmsBaseAttrInfo.class);
                example.createCriteria().andEqualTo("id",attrId);
                pmsBaseAttrInfoMapper.updateByExampleSelective(pmsBaseAttrInfo,example);

                // 按照属性 id  删除所有属性值
                PmsBaseAttrValue delPmsBaseAttrValue = new PmsBaseAttrValue();
                delPmsBaseAttrValue.setAttrId(attrId);
                pmsBaseAttrValueMapper.delete(delPmsBaseAttrValue);

                for (PmsBaseAttrValue pmsBaseAttrValue : pmsBaseAttrValues) {
                    pmsBaseAttrValue.setAttrId(attrId);
                    pmsBaseAttrValueMapper.insertSelective(pmsBaseAttrValue);
                }

            }
        }catch (Exception e){

            return "插入失败";

        }
        return "插入成功";
    }

    @Override
    public List<PmsBaseAttrValue> getAttrValueList(String attrId) {
        PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();
        pmsBaseAttrValue.setAttrId(attrId);
        List<PmsBaseAttrValue> pmsBaseAttrValues = pmsBaseAttrValueMapper.select(pmsBaseAttrValue);
        return pmsBaseAttrValues;
    }

    @Override
    public List<PmsBaseAttrInfo> getAttrValueListByValueId(Set<Object> valueIdSet) {
        String join = StringUtils.join(valueIdSet,',');
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = pmsBaseAttrValueMapper.selectAttrValueListByValueId(join);
        return pmsBaseAttrInfos;
    }


}
