package com.jtcsm.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

/**
 * 会员套餐实体，对应表 membership_plan
 */
@TableName("membership_plan")
public class MembershipPlan extends BaseEntity {

    /** 套餐名称 */
    private String name;

    /** 售价 */
    private BigDecimal price;

    /** 原价 */
    @TableField("original_price")
    private BigDecimal originalPrice;

    /** 有效期天数 */
    private Integer days;

    /** 套餐说明 */
    private String description;

    /** 是否启用 0禁用 1启用 */
    @TableField("is_enabled")
    private Integer isEnabled;

    // ==================== Getter / Setter ====================

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Integer isEnabled) { this.isEnabled = isEnabled; }
}
