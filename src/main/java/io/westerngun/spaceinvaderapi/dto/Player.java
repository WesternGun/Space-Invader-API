package io.westerngun.spaceinvaderapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player implements Serializable {
    private String id;
    private String name;
    private Position position;
    private Area area;
    private Previous previous;

    private Boolean fire;
}
