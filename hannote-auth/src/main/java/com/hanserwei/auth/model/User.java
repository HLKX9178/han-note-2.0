package com.hanserwei.auth.model;

import com.hanserwei.framework.common.constant.DateConstants;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @NotBlank(message = "昵称不能为空")
    private String nickName;

    @JsonFormat(pattern = DateConstants.Y_M_D_H_M_S, timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
}
