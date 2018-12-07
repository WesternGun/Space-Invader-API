package io.westerngun.spaceinvaderapi.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {
    @RequestMapping(value = "/welcome", method = RequestMethod.GET)
    public String root() {
        return "Westerngun wants to get the prize!";
    }
}
