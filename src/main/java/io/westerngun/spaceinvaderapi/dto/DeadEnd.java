package io.westerngun.spaceinvaderapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Class to help the ship to get out of dead end;
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeadEnd {
    private Position deadEndPosition;
    private String solution;
}
