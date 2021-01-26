package com.june.service;

import com.june.error.BuinessException;
import com.june.service.model.ItemModel;

import java.util.List;

public interface ItemService {
    //创建商品
    ItemModel createItem(ItemModel itemModel) throws BuinessException;

    //商品列表浏览
    List<ItemModel> listItem();

    //商品详情浏览
    ItemModel getItemById(Integer id);

    //库存扣减
    boolean decreaseStock(Integer itemId,Integer amount)throws BuinessException;
    //库存回补
    boolean increaseStock(Integer itemId,Integer amount)throws BuinessException;

    //异步更新库存
    boolean asyncDecreaseStock(Integer itemId,Integer amount);

    //item及promo model 缓存模型
    ItemModel getItemByIdInCache(Integer id);

    //商品销售量增加
    void increaseSales(Integer itemId,Integer amount)throws BuinessException;

    //初始化库存流水
    String initStockLog(Integer itemId,Integer amount);
}
