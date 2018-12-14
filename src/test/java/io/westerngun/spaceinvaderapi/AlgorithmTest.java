package io.westerngun.spaceinvaderapi;

import io.westerngun.spaceinvaderapi.controller.MainController;
import io.westerngun.spaceinvaderapi.dto.DeadEnd;
import io.westerngun.spaceinvaderapi.dto.Player;
import io.westerngun.spaceinvaderapi.dto.Position;
import io.westerngun.spaceinvaderapi.dto.Wall;
import javafx.geometry.Pos;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AlgorithmTest {
    @Autowired
    private MainController controller;

    @Test
    public void testOrdering() {
        int[] shortestDimension = new int[] {5, 2, 8, 6, 1};
        int another = 0;
        for (int i=shortestDimension.length-1; i>=1; i--) {
            if (shortestDimension[i] < shortestDimension[i-1]) {
                another = shortestDimension[i];
                shortestDimension[i] = shortestDimension[i-1];
                shortestDimension[i-1] = another;
            }
        }
        Arrays.stream(shortestDimension).forEach((x) -> System.out.println(x));
    }


    @Test
    public void testOrderNearestPlayer() {
        // when
        Position p1 = new Position(7, 1);
        Position p2= new Position(5, 2);
        Position p3 = new Position(4, 0);
        controller.setMe(new Position(4, 4));
        Position[] players = new Position[]{p1, p2, p3};
        Position nearest = controller.findNearestPlayer(players);

        Assert.assertEquals(p3, nearest);
    }

    @Test
    public void testNoWallBlocking() {
        // given
        Position p1 = new Position(4, 1);
        controller.setMe(new Position(4, 4));
        controller.addWalls(1, new Wall[]{new Wall(4, 2)});

        // when

        // then
        Assert.assertTrue(controller.someWallIsBlocking(p1));

        // given
        Position p2 = new Position(1, 3);
        controller.setMe(new Position(4, 3));
        controller.addWalls(2, new Wall[]{new Wall(2, 3), new Wall(3, 3)});

        Assert.assertTrue(controller.someWallIsBlocking(p2));
    }

    @Test
    public void testGetAroundCorner() {
        // given
        DeadEnd deadEnd = new DeadEnd(new Position(1, 2), "down");
        controller.setDeadEnd(deadEnd);
        List<String> moves = new ArrayList<>();
        moves.add("left");
        moves.add("up");

        // when
        String result = controller.getAroundCorner((ArrayList<String>)moves);

        // then
        Assert.assertEquals("left", result);

    }

    @Test
    public void testPositionEquals() {
        Position p1 = new Position(3, 2);
        Position p2 = new Position(3, 2);
        Assert.assertTrue(p1.equals(p2));
    }

    @Test
    public void testStreamArrayToList() {
        List<String> newList = Arrays.stream(new String[]{"one", "two", "three"}).collect(Collectors.toCollection(ArrayList::new));
        Assert.assertEquals("ArrayList", newList.getClass().getSimpleName());
    }
}
