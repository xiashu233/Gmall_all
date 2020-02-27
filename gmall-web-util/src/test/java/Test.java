import com.atguigu.gmall.util.JwtUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Test {

    public static void main(String[] args) {
        Map<String,Object> map = new HashMap<>();
        map.put("memeberId","1");
        String key = "";

        String ip = "127.0.0.1";
        String time = new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date());
        String salt = ip + time;
        String encode = JwtUtil.encode(key, map, salt);
        System.out.println("eyJhbGciOiJIUzI1NiJ9.eyJtZW1lYmVySWQiOiIxIn0.bFxO7Im0epLzSGPAIwooVrP9DnUiBacVqfaWd3lDvlI");
        System.out.println(encode);
    }
}
