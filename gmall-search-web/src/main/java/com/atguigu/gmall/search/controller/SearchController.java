package com.atguigu.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.AttrService;
import com.atguigu.gmall.service.SearchService;
import com.atguigu.gmall.service.SkuService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.*;


@Controller
public class SearchController {

    @Reference
    SearchService searchService;

    @Reference
    AttrService attrService;


    @RequestMapping("index")
    public String index(){
        return "index";
    }

    @RequestMapping("list.html")
    public String list(PmsSerachParam pmsSerachParam, Model model) throws IOException {
        //调用搜索服务 返回搜索结果
        List<PmsSearchSkuInfo> pmsSearchSkuInfos = searchService.list(pmsSerachParam);

        // 抽取检索结果所包含的平台属性集合
        Set<Object> valueIdSet = new HashSet<>();
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
            List<PmsSkuAttrValue> skuAttrValueList = pmsSearchSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                String valueId = pmsSkuAttrValue.getValueId();
                valueIdSet.add(valueId);

            }
        }

        // 根据 valueId 将属性列表查询出来
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = attrService.getAttrValueListByValueId(valueIdSet);

        String[] delValueId = pmsSerachParam.getValueId();
        // 对平台属性进行处理 筛选掉已经选择过的 属性项
        if (delValueId != null){
            // 面包屑
            List<PmsSearchCrumb> pmsSearchCrumbs = new ArrayList<>();

            for (String delVal : delValueId) {
                Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfos.iterator();
                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
                pmsSearchCrumb.setValueId(delVal);

                pmsSearchCrumb.setUrlParam(getUrlParam(pmsSerachParam,delVal));


                while (iterator.hasNext()){
                    PmsBaseAttrInfo pmsBaseAttrInfo = iterator.next();
                    List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
                    for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                        String id = pmsBaseAttrValue.getId();

                        if (id.equals(delVal)){
                            pmsSearchCrumb.setValueName(pmsBaseAttrValue.getValueName());
                            iterator.remove();
                            break;
                        }
                    }
                }
                pmsSearchCrumbs.add(pmsSearchCrumb);
            }
            if (pmsBaseAttrInfos!=null){
                model.addAttribute("attrValueSelectedList",pmsSearchCrumbs);
            }

        }


        model.addAttribute("skuLsInfoList",pmsSearchSkuInfos);

        model.addAttribute("attrList",pmsBaseAttrInfos);
        
        String urlParam = getUrlParam(pmsSerachParam);
        model.addAttribute("urlParam",urlParam);
        if (pmsSerachParam.getKeyword()!=null){
            model.addAttribute("keyword",pmsSerachParam.getKeyword());
        }


        return "list";
    }


    private String getUrlParam(PmsSerachParam pmsSerachParam,String...delValueId) {

        String delVal = delValueId.length<=0?"":delValueId[0];

        String keyword = pmsSerachParam.getKeyword();
        String catalog3Id = pmsSerachParam.getCatalog3Id();
        String[] valueIds = pmsSerachParam.getValueId();
        String urlParam = "";
        if (StringUtils.isNotBlank(keyword)){
            urlParam += ((urlParam.indexOf('&')==-1?"":"&") + "keyword="+ keyword);
        }
        if (StringUtils.isNotBlank(catalog3Id)){
            urlParam += ((urlParam.indexOf('&')==-1?"":"&") + "catalog3Id="+ catalog3Id);
        }
        if (valueIds != null){
            for (String valueId : valueIds) {
                if (!valueId.equals(delVal) ){
                    urlParam += "&valueId="+ valueId;
                }
            }
        }

//        System.out.println(urlParam);
        return urlParam;
    }
}
