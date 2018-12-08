package io.westerngun.spaceinvaderapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Board implements Serializable {
    private Size size;
    private Wall[] walls;

}
