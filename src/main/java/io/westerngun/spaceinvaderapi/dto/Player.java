package io.westerngun.spaceinvaderapi.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class Player implements Serializable {
    private String id;
    private String name;
    private Position position;
    private Area area;
    private Previous previous;

    private Boolean fire;
}
