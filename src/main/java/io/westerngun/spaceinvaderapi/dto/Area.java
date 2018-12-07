package io.westerngun.spaceinvaderapi.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class Area implements Serializable {
    private int x1;
    private int x2;
    private int y1;
    private int y2;

}
