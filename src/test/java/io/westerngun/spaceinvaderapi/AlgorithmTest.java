package io.westerngun.spaceinvaderapi;

import io.westerngun.spaceinvaderapi.dto.Position;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
public class AlgorithmTest {

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


//    @Test
//    public void testOrderNearestPlayer() {
//        // when
//        Position p1 = new Position(7, 1);
//        Position p2= new Position(5, 2);
//        Position p3 = new Position(4, 0);
//        controller.setMe(new Position(4, 4));
//        Position[] players = new Position[]{p1, p2, p3};
//        Position nearest = controller.findNearestPlayer(players);
//
//        Assert.assertEquals(p3, nearest);
//    }
//
//    @Test
//    public void testNoWallBlocking() {
//        // given
//        Position me = new Position(4, 4);
//        Position p1 = new Position(4, 1);
//        controller.addWalls(1, new Wall[]{new Wall(4, 2)});
//
//        // when
//
//        // then
//        Assert.assertTrue(controller.someWallIsBlocking(, p1));
//
//        // given
//        Position p2 = new Position(1, 3);
//        controller.setMe(new Position(4, 3));
//        controller.addWalls(2, new Wall[]{new Wall(2, 3), new Wall(3, 3)});
//
//        Assert.assertTrue(controller.someWallIsBlocking(me, p2));
//    }
//
//    @Test
//    public void testGetAroundCorner() {
//        // given
//        DeadEnd deadEnd = new DeadEnd(new Position(1, 2), "down");
//        controller.setDeadEnd(deadEnd);
//        List<String> moves = new ArrayList<>();
//        moves.add("left");
//        moves.add("up");
//
//        // when
//        String result = controller.getAroundCorner((ArrayList<String>)moves);
//
//        // then
//        Assert.assertEquals("left", result);
//
//    }

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

    @Test
    public void testStreamArrayToSet() {

        Set<String> newSet = new HashSet<>();
        newSet.add("one");
        newSet.add("four");
        newSet.addAll(Arrays.stream(new String[]{"one", "two", "three"}).collect(Collectors.toCollection(HashSet::new)));
        Assert.assertEquals("HashSet", newSet.getClass().getSimpleName());
        Assert.assertTrue(newSet.contains("one"));
        Assert.assertTrue(newSet.contains("two"));
        Assert.assertTrue(newSet.contains("three"));
        Assert.assertTrue(newSet.contains("four"));
        Assert.assertEquals(4, newSet.size());
    }
    @Test
    public void testAddListToSet() {
        List<String> newList = new ArrayList<>();
        newList.add("one");
        newList.add("two");

        Set<String> newSet = new HashSet<>();
        newSet.addAll(newList);

        System.out.println(newSet);
    }

    @Test
    public void testRetainTwoList() {
        List<String> list1 = new ArrayList<>();
        list1.add("one");
        list1.add("two");

        List<String> list2 = new ArrayList<>();
        //list2.add("two");
        //list2.add("three");

        ArrayList retain = list1.stream().filter(list2::contains).collect(Collectors.toCollection(ArrayList::new));

        Assert.assertEquals(0, retain.size());
        //Assert.assertEquals("two", retain.get(0));
    }

    @Test
    public void testRemoveNotPresent() {
        List<String> list1 = new ArrayList<>();
        list1.add("one");
        list1.add("two");
        list1.remove("three");

        Assert.assertEquals(2, list1.size());
    }
}
