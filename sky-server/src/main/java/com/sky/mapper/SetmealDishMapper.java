package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
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

    /**
     * 批量保存套餐和菜品的关联关系
     * * @param setmealDishes 套餐菜品关系列表
     */
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据套餐id删除套餐和菜品的关联关系
     * @param setmealId
     */
    @Delete("delete from setmeal_dish where id = #{setmealId}")
    void deleteBySetmealId(Long setmealId);
}
