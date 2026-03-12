package com.hmdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryAll() {
        // 1.从redis中进行查询
        Set<String> shopType = stringRedisTemplate.opsForZSet().range("cache:shopType", 0, -1);


        // 2.命中，直接返回
        if (!CollectionUtil.isEmpty(shopType)) {
            List<ShopType> list = new ArrayList<>();
            for (String s : shopType) {
                ShopType bean = JSONUtil.toBean(s, ShopType.class);
                list.add(bean);
            }
            return Result.ok(list);
        }

        // 3.未命中，查找数据库
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        // 4.未找到
        if (CollectionUtil.isEmpty(shopTypes)) {
            // 5.返回错误信息
            return Result.fail("没有相关信息");
        }


        Set<ZSetOperations.TypedTuple<String>> set = new HashSet<>();
        for (ShopType type : shopTypes) {
            String typeJson = JSONUtil.toJsonStr(type);
            Double score = type.getSort().doubleValue();
            set.add(new DefaultTypedTuple<>(typeJson, score));
        }

        // 6.数据库中找到，存入redis
        stringRedisTemplate.opsForZSet().add("cache:shopType", set);

        // 8.返回
        return Result.ok(shopTypes);
    }
}
