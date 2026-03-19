package com.rcs.system.wes;

public class WesSocketMessage {
    private String source;
    private String sceneCode;
    private String sceneDescription;
    private String inbound;
    private String productId;
    private String productName;
    private Integer quantity;
    private String normalCompleteCode;
    private Integer normalCompleteQty;
    private String abnormalCompleteCode;
    private String abnormalReasonCode;
    private Integer abnormalCompleteQty;
    private String rawMessage;
    private String remoteAddress;

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSceneCode() { return sceneCode; }
    public void setSceneCode(String sceneCode) { this.sceneCode = sceneCode; }
    public String getSceneDescription() { return sceneDescription; }
    public void setSceneDescription(String sceneDescription) { this.sceneDescription = sceneDescription; }
    public String getInbound() { return inbound; }
    public void setInbound(String inbound) { this.inbound = inbound; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getNormalCompleteCode() { return normalCompleteCode; }
    public void setNormalCompleteCode(String normalCompleteCode) { this.normalCompleteCode = normalCompleteCode; }
    public Integer getNormalCompleteQty() { return normalCompleteQty; }
    public void setNormalCompleteQty(Integer normalCompleteQty) { this.normalCompleteQty = normalCompleteQty; }
    public String getAbnormalCompleteCode() { return abnormalCompleteCode; }
    public void setAbnormalCompleteCode(String abnormalCompleteCode) { this.abnormalCompleteCode = abnormalCompleteCode; }
    public String getAbnormalReasonCode() { return abnormalReasonCode; }
    public void setAbnormalReasonCode(String abnormalReasonCode) { this.abnormalReasonCode = abnormalReasonCode; }
    public Integer getAbnormalCompleteQty() { return abnormalCompleteQty; }
    public void setAbnormalCompleteQty(Integer abnormalCompleteQty) { this.abnormalCompleteQty = abnormalCompleteQty; }
    public String getRawMessage() { return rawMessage; }
    public void setRawMessage(String rawMessage) { this.rawMessage = rawMessage; }
    public String getRemoteAddress() { return remoteAddress; }
    public void setRemoteAddress(String remoteAddress) { this.remoteAddress = remoteAddress; }
}
