package com.atguigu.gmall.interceptors;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpclientUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.HttpClientUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 拦截代码

        // 判断被拦截的方法是否带有 @LoginRequired 注解
        // handler 请求方法所携带的信息
        HandlerMethod hm = (HandlerMethod) handler;
        LoginRequired methodAnnotation = hm.getMethodAnnotation(LoginRequired.class);


        // 是否需要拦截
        //请求没有带 @LoginRequired 注解 则开始直接跳过
        if (methodAnnotation == null) {
            return true;
        }

        String token = "";
        String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);
        String newToken = request.getParameter("token");
        if (StringUtils.isNotBlank(oldToken)) {
            token = oldToken;
        }
        if (StringUtils.isNotBlank(newToken)) {
            token = newToken;
        }
        // 是否必须登录
        boolean loginSuccess = methodAnnotation.loginSuccess();

        String success = "fail";
        Map<String,String> successMap = new HashMap<>();
        if (StringUtils.isNotBlank(token)){

            String successJson = HttpclientUtil.doGet("http://passport.gmall.com:8085/verify?token=" + token + "&currentIp=127.0.0.1" );

            successMap  = JSON.parseObject(successJson, Map.class);
            // 返回状态
            success = successMap.get("status");

        }



        if (loginSuccess) {
            // 必须登录成功
            if (StringUtils.isBlank(token)) {
                // 验证 token 失败 回到认证中心
                if (!success.equals("success")) {
                    // 重定向回 passport 登录
                    StringBuffer requestURL = request.getRequestURL();
                    response.sendRedirect("http://passport.gmall.com:8085/index?ReturnUrl=" + requestURL);
                    return false;
                }
                // 验证通过，覆盖 cookie 中的 token
                request.setAttribute("memberId", successMap.get("memberId"));
                request.setAttribute("nickname", successMap.get("nickname"));
                return true;


            } else {
                // 验证 token 成功
                if (success.equals("success")) {
                    // 不登录时验证成功 需要将token携带的用户信息 写入到cookie中
                    request.setAttribute("memberId", "1");
                    request.setAttribute("nickname", "nickname");

                    // 验证通过 覆盖 cookie 中的token
                    if (StringUtils.isNotBlank(token)){
                        CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
                    }

                }
            }


            return true;
        }
        return true;
    }
}