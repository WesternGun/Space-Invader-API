package io.westerngun.spaceinvaderapi.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class Size implements Serializable {
    private int height;
    private int width;
}
