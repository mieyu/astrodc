package mie.astronomy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("site_paper")
public class SitePaper {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("title")
    private String title;

    @TableField("paper_link")
    private String paperLink;

    @TableField("date")
    private String date;

    @TableField("galaxy")
    private String galaxy;
}
