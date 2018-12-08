package io.westerngun.spaceinvaderapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Area implements Serializable {
    private int x1;
    private int x2;
    private int y1;
    private int y2;

}
