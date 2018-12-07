package io.westerngun.spaceinvaderapi.dto;

import lombok.Data;

@Data
public class Map {
    private Player player;
    private Board board;
    private Player[] players;
    private Invader[] invaders;

}
