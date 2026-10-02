package mie.astronomy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 相机参数（增益、读出噪声、暗电流）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraParams {
    /** 增益 e-/ADU */
    private double gain;
    /** 读出噪声 e- */
    private double readNoise;
    /** 暗电流 e-/s */
    private double darkCurrent;
}
