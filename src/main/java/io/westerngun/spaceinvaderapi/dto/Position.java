package io.westerngun.spaceinvaderapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Position implements Serializable {
    private int x;
    private int y;
}
