package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品id查询套餐id
     * @param dishIds 菜品id列表
     * @return 套餐id列表
     */
    List<Long> getSetmealIdsByDishId(List<Long> dishIds);
}
