package org.sluck.arch.stream.network.message;

/**
 * 消费失败请求结果响应
 *
 * author: sunxy
 * createTime: 2019/6/20:15:47
 * since: 1.0.0
 */
public class ConsumeFailResponseMsg implements ResponseMessage{

    private boolean persistSuccess;//是否持久化成功

    private String remark;// 备注

    private ConsumeFailResponseMsg() {}

    public ConsumeFailResponseMsg(boolean persistSuccess, String remark) {
        this.persistSuccess = persistSuccess;
        this.remark = remark;
    }

    public boolean isPersistSuccess() {
        return persistSuccess;
    }

    public void setPersistSuccess(boolean persistSuccess) {
        this.persistSuccess = persistSuccess;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
