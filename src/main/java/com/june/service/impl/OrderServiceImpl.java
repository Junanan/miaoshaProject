package com.june.service.impl;

import com.june.dao.OrderDOMapper;
import com.june.dao.SequenceDOMapper;
import com.june.dao.StockLogDOMapper;
import com.june.dataobject.OrderDO;
import com.june.dataobject.SequenceDO;
import com.june.dataobject.StockLogDO;
import com.june.error.BuinessException;
import com.june.error.EmBusinessError;
import com.june.service.ItemService;
import com.june.service.OrderService;
import com.june.service.UserService;
import com.june.service.model.ItemModel;
import com.june.service.model.OrderModel;
import com.june.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sun.awt.windows.ThemeReader;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private ItemService itemService;
    @Autowired
    private UserService  userService;
    @Autowired
    private OrderDOMapper orderDOMapper;
    @Autowired
    private SequenceDOMapper sequenceDOMapper;
    @Autowired
    private StockLogDOMapper stockLogDOMapper;
    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer itemId,Integer promoId, Integer amount,String stockLogId) throws BuinessException {
        //1校验下单状态，下单的商品是否存在，用户是否合法，购买数量是否正确
//        ItemModel itemModel = itemService.getItemById(itemId); // 体现了service 独立的好处---
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        // service之间可以相互调用 所有逻辑都是在service层运行
        if (itemModel == null) {
            throw new BuinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "商品信息不存在");
        }
        if (amount <= 0 || amount > 99) {
            throw new BuinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "数量信息不正确");
        }
        //2.落单减库存
        boolean result = itemService.decreaseStock(itemId, amount);
        if (!result){
            throw  new BuinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }
        //3.订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        orderModel.setPromoId(promoId);
        if (promoId != null){
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        }else{
            orderModel.setItemPrice(itemModel.getPrice());
        }
        orderModel.setOrderPrice(itemModel.getPrice().multiply(new BigDecimal(amount)));

        //生成交易流水号，订单号
        orderModel.setId(generateOrderNo());
        OrderDO orderDO = convertFromOrderModel(orderModel);
        orderDOMapper.insertSelective(orderDO);

        //加上商品销量
        itemService.increaseSales(itemId,amount);

        //设置库存流水为成功
        StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
        if (stockLogDO == null){
            throw new BuinessException(EmBusinessError.UNKNOWN_ERROR);
        }
        //1 为unknown 2 为成功 3为回滚
        stockLogDO.setStatus(2);
        stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);

        //返回给前端

        return orderModel;
    }
//    propagation = Propagation.REQUIRES_NEW
//    标志REQUIRES_NEW会新开启事务，外层事务不会影响内部事务的提交/回滚
//    标志REQUIRES_NEW的内部事务的异常，会影响外部事务的回滚
//     --> 避免createOrder 创建失败事务回滚 造成订单号事务也回滚  丧失了全局唯一性
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateOrderNo() {
        StringBuilder stringBuilder = new StringBuilder();
        //订单号有16位 前8位为时间信息，年月日，中间6位为自增序列，
        LocalDateTime now = LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        stringBuilder.append(nowDate);
        //获取当前sequence
        int sequence = 0;
        SequenceDO sequenceDO = sequenceDOMapper.getSequenceByName("order_info");
        sequence = sequenceDO.getCurrentValue();
        sequenceDO.setCurrentValue(sequenceDO.getCurrentValue() + sequenceDO.getStep());
        sequenceDOMapper.updateByPrimaryKey(sequenceDO);
        String sequenceStr = String.valueOf(sequence);
        for (int i = 0; i < 6 - sequenceStr.length(); i ++) {
            stringBuilder.append(0);
        }
        stringBuilder.append(sequenceStr);
//        最后2位为分库分表位暂时写死
        stringBuilder.append("00");
        return stringBuilder.toString();
    }
    private OrderDO convertFromOrderModel(OrderModel orderModel){
        if (orderModel == null){
            return null;
        }
        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel,orderDO);
        return orderDO;
    }
}
