package io.westerngun.spaceinvaderapi.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
public class Wall extends Position implements Serializable {
    public Wall(int x, int y) {
        super(x, y);
    }
}
