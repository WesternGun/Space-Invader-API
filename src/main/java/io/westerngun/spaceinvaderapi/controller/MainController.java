package io.westerngun.spaceinvaderapi.controller;

import io.westerngun.spaceinvaderapi.dto.Move;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {
    /**
     * up, down, left, right, fire-up, fire-down, fire-left or fire-right.
     */
    private static final String MU = "up";
    private static final String MD = "down";
    private static final String ML = "left";
    private static final String MR = "right";
    private static final String FU = "fire-up";
    private static final String FD = "fire-down";
    private static final String FL = "fire-left";
    private static final String FR = "fire-right";


    @RequestMapping(value = "/welcome", method = RequestMethod.GET)
    public String welcome() {
        return "Westerngun wants to get the prize!";
    }

    @RequestMapping(value = "/name", method = RequestMethod.GET)
    public String name() {
        return "{\"name\": \"WesternGun\", \"email\":\"westernguncyber@gmail.com\"}";
    }

    @RequestMapping(value = "/move", method = RequestMethod.GET)
    public Move move() {
        return new Move("something");
    }
}
