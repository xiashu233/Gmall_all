package com.atguigu.gmall.passport.cotroller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpclientUtil;
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
            token = addToken(umsMemberLogin,request);
            // 增加一个 cookie
            CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);

            // redis 里也要存放一份 token
            userService.addUserToken(token,umsMemberLogin.getId() );
            request.setAttribute("token",token);
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

    @GetMapping("/vlogin")
    public String vlogin(String code,HttpServletRequest request){

        // 授权码 交换 access_token
        Map<String,String> map = new HashMap<>();
        map.put("client_id","4140827957");
        map.put("client_secret","004e4ce4de694a4246055884b4054a83");
        map.put("grant_type","authorization_code");
        map.put("redirect_uri","http://passport.gmall.com:8085/vlogin");
        map.put("code",code);
        String access_tokenJson = HttpclientUtil.doPost("https://api.weibo.com/oauth2/access_token?client_id=4140827957&client_secret=004e4ce4de694a4246055884b4054a83&grant_type=authorization_code&redirect_uri=http://passport.gmall.com:8085/vlogin&code="+code, map);
        Map<String, String> accessMap = JSON.parseObject(access_tokenJson, Map.class);

        // access_token换 用户信息
        String user_Json = HttpclientUtil.doGet("https://api.weibo.com/2/users/show.json?access_token="+accessMap.get("access_token") + "&uid=" + accessMap.get("uid"));
        Map<String,Object> userInfo = JSON.parseObject(user_Json,Map.class);
        String access_token = accessMap.get("access_token");

        String nickname = (String) userInfo.get("screen_name");

        // 将用户信息保存到数据库
        UmsMember umsMember = new UmsMember();
        umsMember.setSourceType("2");
        umsMember.setAccessCode(code);
        umsMember.setAccessToken(access_token);
        umsMember.setSourceUid((String) userInfo.get("idstr"));
        umsMember.setCity((String) userInfo.get("location"));
        umsMember.setGender(((String)userInfo.get("gender")).equals("m")?"1":"0");
        umsMember.setNickname(nickname);

        UmsMember umsCheckUser = new UmsMember();
        umsCheckUser.setSourceUid((String) userInfo.get("idstr"));
        UmsMember umsMemberCheck = userService.checkOauthUser(umsCheckUser);
        if (umsMemberCheck == null){
            umsMember =  userService.addOauthUser(umsMember);
        }else {
            umsMember = umsMemberCheck;
        }

        // 生成 JWT的 token，并且重定向到首页，携带该token
        String token = addToken(umsMember, request);
        userService.addUserToken(token,umsMember.getId());
        return "redirect:///search.gmall.com:8083/index?token="+token;
    }

    private String addToken(UmsMember umsMemberLogin,HttpServletRequest request){
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

        String  token = JwtUtil.encode("2019gmall0105", userMap, ip);
        return token;
    }

}
