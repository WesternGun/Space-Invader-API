package io.westerngun.spaceinvaderapi.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class Previous implements Serializable {
    private int x;
    private int y;
}