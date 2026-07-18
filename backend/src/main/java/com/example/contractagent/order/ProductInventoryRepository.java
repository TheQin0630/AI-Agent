package com.example.contractagent.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductInventoryRepository extends BaseMapper<ProductInventory> {
    @Select("SELECT * FROM product_inventory WHERE item_name = #{itemName} AND item_model_key = #{itemModelKey} AND unit = #{unit} FOR UPDATE")
    ProductInventory findForUpdate(@Param("itemName") String itemName,
                                   @Param("itemModelKey") String itemModelKey,
                                   @Param("unit") String unit);
}
