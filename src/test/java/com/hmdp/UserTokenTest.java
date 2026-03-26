package com.hmdp;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class UserTokenTest {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Test
    void generateTokenForUsers() throws IOException {
        List<User> userList = userService.list();

        BufferedWriter writer = new BufferedWriter(new FileWriter("tokens.txt"));


        for (User user : userList) {
            String token = UUID.randomUUID().toString(true);
            String tokenKey = RedisConstants.LOGIN_USER_KEY + token; // 请极其仔细地核对这里是否和你常量类里的 LOGIN_USER_KEY 一致
            UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
            Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                    CopyOptions.create().setIgnoreNullValue(true)
                            .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
            stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
            stringRedisTemplate.expire(tokenKey, 30, TimeUnit.MINUTES);
            writer.write(token + "\n");
        }
        // 第六步：彻底关闭物理 I/O 通道，防止极其低级的内存泄漏
        writer.close();
        System.out.println("1000 个物理克隆 Token 已极其暴力地注入完毕！");
    }
}
