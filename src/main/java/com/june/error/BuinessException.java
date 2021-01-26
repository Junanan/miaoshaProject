package com.june.error;

//包装器业务异常类实现
public class BuinessException extends Exception implements CommonError{

    private CommonError commonError;

    //直接接收EmBuinessError的传参用于构造业务异常
    public BuinessException(CommonError commonError) {
        super();
        this.commonError = commonError;
    }

    //接收自定义ErrMsg的方式构造业务异常
    public BuinessException(CommonError commonError, String errMsg) {
        super();
        this.commonError = commonError;
        this.commonError.setErrMsg(errMsg);
    }

    @Override
    public int getErrCode() {
        return this.commonError.getErrCode();
    }

    @Override
    public String getErrMsg() {
        return this.commonError.getErrMsg();
    }

    @Override
    public CommonError setErrMsg(String errMsg) {
        this.commonError.setErrMsg(errMsg);
        return this;
    }
}
