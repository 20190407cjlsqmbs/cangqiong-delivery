package com.sky.service;

import com.sky.dto.SetmealDTO;

public interface SetmealService {
    /**
     * 新增套餐
     * @param setmealDTO 套餐数据传输对象
     */
    void saveWithDish(SetmealDTO setmealDTO);

}
