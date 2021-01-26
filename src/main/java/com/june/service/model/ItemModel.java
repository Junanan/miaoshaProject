package com.june.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemModel implements Serializable {
    private Integer id;
    //商品名称
    @NotBlank(message = "商品不能为空")
    private String title;
    //商品价格
    @NotNull(message = "商品价格不能为空")
    @Min(value = 0, message = "商品价格必须大于0")
    private BigDecimal price;
    //商品库存
    @NotNull(message = "库存不能不填")
    private Integer stock;
    //商品销量
    private Integer sales;
    //商品描述
    @NotBlank(message = "商品描述信息不能为空")
    private String description;
    //商品描述图片的url;
    @NotBlank(message = "图片信息不能为空")
    private String imgUrl;

    //使用聚合模型,如果其不为空，则还有未结束的活动
    private PromoModel promoModel;
}
