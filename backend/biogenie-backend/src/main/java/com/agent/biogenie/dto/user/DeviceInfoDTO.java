package com.agent.biogenie.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 设备信息
 */
@Data
@Builder
@Schema(description = "设备信息")
public class DeviceInfoDTO {

    @Schema(description = "设备类型: iOS/Android/Web/Windows/Mac/Unknown")
    private String deviceType;

    @Schema(description = "设备名称")
    private String deviceName;
}
