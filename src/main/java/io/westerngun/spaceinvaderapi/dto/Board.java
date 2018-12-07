package io.westerngun.spaceinvaderapi.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class Board implements Serializable {
    private Size size;
    private Wall[] walls;

}
