package io.westerngun.spaceinvaderapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Body implements Serializable {
    private Game game;
    private Player player;
    private Board board;
    private Position[] players;
    private Invader[] invaders;
}
