package com.atguigu.gmall.passport.cotroller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassPortController {

    @Reference
    UserService userService;

    @GetMapping("index")
    public String index(String ReturnUrl, ModelMap modelMap){
        modelMap.put("ReturnUrl",ReturnUrl);
        return "index";
    }

    @PostMapping("/login")
    @ResponseBody
    public String login(HttpServletRequest request, HttpServletResponse response, UmsMember umsMember){
        // 调用用户服务验证用户名和密码
        UmsMember umsMemberLogin = userService.login(umsMember);
        String token = "";
        if (umsMemberLogin != null){
            // 登录成功 用jwt制作一个 token
            Map<String,Object> userMap = new HashMap<>();
            userMap.put("memberId",umsMemberLogin.getId());
            userMap.put("nickname",umsMemberLogin.getNickname());
            // 盐值
            String ip = request.getHeader("x-forwarded-for"); // 根据Nginx转发的客户端ip
            if (StringUtils.isBlank(ip)){
                ip = request.getRemoteAddr();
            }
            if (StringUtils.isBlank(ip)){
                ip = "127.0.0.1";
                // 不携带ip的请求 是非法请求
                return null;
            }

            token = JwtUtil.encode("2019gmall0105", userMap, ip);
            // redis 里也要存放一份 token
            userService.addUserToken(token,umsMemberLogin.getId() );
        }else{
            // 登录失败 返回认证中心
            token = "fail";
        }
        return token;
    }

    @GetMapping("/verify")
    @ResponseBody
    public String verify(String token,String currentIp){
        // 通过jwt进行校验 token 的真假
        Map<String ,String> map = new HashMap<>();


        Map<String, Object> decode = JwtUtil.decode(token, "2019gmall0105", currentIp);

        if (decode != null){
            map.put("memberId",(String) decode.get("memberId"));
            map.put("nickname",(String) decode.get("nickname"));
            map.put("status","success");
        }else{
            map.put("status","fail");
        }

        String success = JSON.toJSONString(map);
        return success;
    }
}
