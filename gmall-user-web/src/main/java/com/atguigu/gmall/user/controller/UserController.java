package com.atguigu.gmall.user.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class UserController {

    // 远程协议代理
    @Reference
    UserService userService;


    @RequestMapping("index")
    @ResponseBody
    public String index(){
        return "hello User";
    }

    @RequestMapping("getAllUser")
    @ResponseBody
    public List<UmsMember> getAllUser(){
        List<UmsMember> umsMembers = userService.getAllUser();
        return umsMembers;
    }

    @RequestMapping("getReceiveAddressMemberId")
    @ResponseBody
    public List<UmsMemberReceiveAddress> getReceiveAddressMemberId(String memberId){
        List<UmsMemberReceiveAddress> umsMemberReceiveAddressList = userService.getReceiveAddressMemberId(memberId);
//        System.out.println("233333333333333333333333");
        return umsMemberReceiveAddressList;
    }
}
