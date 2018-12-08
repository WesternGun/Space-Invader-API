package io.westerngun.spaceinvaderapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Map implements Serializable {
    private Player player;
    private Board board;
    private Player[] players;
    private Invader[] invaders;

}
