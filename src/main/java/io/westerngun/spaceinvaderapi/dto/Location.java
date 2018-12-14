package io.westerngun.spaceinvaderapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Location implements Serializable {
    private Position position;
    private List<Location> possibilities;

    private boolean visited;

    public void tryLocation() {
        
    }
}
