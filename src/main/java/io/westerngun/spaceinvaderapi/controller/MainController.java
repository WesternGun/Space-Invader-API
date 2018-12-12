package io.westerngun.spaceinvaderapi.controller;

import io.westerngun.spaceinvaderapi.dto.Area;
import io.westerngun.spaceinvaderapi.dto.Board;
import io.westerngun.spaceinvaderapi.dto.Body;
import io.westerngun.spaceinvaderapi.dto.DeadEnd;
import io.westerngun.spaceinvaderapi.dto.Game;
import io.westerngun.spaceinvaderapi.dto.Invader;
import io.westerngun.spaceinvaderapi.dto.Move;
import io.westerngun.spaceinvaderapi.dto.Name;
import io.westerngun.spaceinvaderapi.dto.Player;
import io.westerngun.spaceinvaderapi.dto.Position;
import io.westerngun.spaceinvaderapi.dto.Size;
import io.westerngun.spaceinvaderapi.dto.Wall;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
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

    private List<String> allFourDirections;

    private static final String WAIT = "wait";

    private Game game;
    private Player player;
    private Position me;
    private Position previous;
    private Area area;
    private boolean fire;

    private Board board;
    private Size size;
    private Position[] walls;
    private Position[] players;
    private Invader[] invaders;

    private int stopWatch = 0;
    private Position nearestEnemy;
    private Invader nearestInvader;
    private DeadEnd deadEnd;

    private int areaSize;
    private boolean goToCenter;
    private boolean getOut; // if we are surrounded by too many walls and needs to get out

    @Value("${crowd.threshold}")
    private double crowdedThreshold;

    private String[] path;
    private String moveToCenterX; // left or right
    private String moveToCenterY; // up or down
    private Position centerPoint;

    public void setMe(Position p) {
        this.me = p;
    }

    public void addWalls(int capicity, Wall[] wallsToAdd) {
        walls = new Wall[capicity];
        for (int i=0; i<capicity; i++) {
            walls[i] = wallsToAdd[i];
        }
    }

    public void setDeadEnd(DeadEnd deadEnd) {
        this.deadEnd = deadEnd;
    }
    @Value("{$move.strategy}")
    private String strategy;

    // players ordering from the most threatening to the least threatening
    private List<Player> threats = new ArrayList<>();

    @RequestMapping(value = "/welcome", method = RequestMethod.GET)
    public String welcome() {
        return "Westerngun wants to get the prize!";
    }

    @RequestMapping(value = "/name", method = RequestMethod.POST)
    public Name name() {
        return new Name("WesternGun", "yangliang.ding@ext.privalia.com");
    }

    @RequestMapping(value = "/move", method = RequestMethod.POST)
    public Move move(@RequestBody Body body) {
        log.info("Received JSON: {}", body);
        //log.info("We are performing: {}", MR);
        allFourDirections = new ArrayList<>();
        allFourDirections.add(MR);
        allFourDirections.add(ML);
        allFourDirections.add(MU);
        allFourDirections.add(MD);

        game = body.getGame();
        player = body.getPlayer();
        fire = player.getFire();
        me = player.getPosition();
        previous = player.getPrevious();
        area = player.getArea();
        areaSize = (area.getX2()-area.getX1() + 1) * (area.getY2() - area.getY1() + 1);
        board = body.getBoard();
        if (size == null) {
            size = board.getSize(); // this won't change
            centerPoint = new Position(size.getWidth() / 2, size.getHeight() / 2);
        }
        if (me.getX() <= centerPoint.getX()) {
            moveToCenterX = MR;
        } else {
            moveToCenterX = ML;
        }
        if (me.getY() <= centerPoint.getY()) {
            moveToCenterY = MD;
        } else {
            moveToCenterY = MU;
        }

        walls = board.getWalls(); // visible area walls, not all the walls
        if (areaSize < 81) {
            // we are not in the center of map
            goToCenter = true;
        }
        if (walls.length / 81 >= crowdedThreshold) {
            // too many walls, we get out
            getOut = true;
        }


        invaders = body.getInvaders();
        players = body.getPlayers();
        for (Invader i: invaders) {
            i.setPosition(new Position(i.getX(), i.getY()));
        }

        nearestEnemy = findNearestPlayer(players);
        nearestInvader = findNearestInvader(invaders);

        //info("In the visible area we have {} invaders. ", invaders.length);
        //log.info("In the visible area we have {} players. ", players.length);

        // calculate if we are dead/blocked. (necessary?)
        if (me.equals(previous)) {
            stopWatch ++; // we have stopped for 1 round;
            //log.info("We may be shot, do something!");
            doSomething();
        } else {
            stopWatch = 0; // clear
        }

        // remember:
        // death penalty: 6 rounds death, -25p, revive first round cannot fire
        // you could revive and dead again, when others collide with you, so when you revive, first thing is to run!
        // reload: 7 rounds (round 1 fire -> round 8 fire again)
        // neutral invader: 5 rounds (round 1 yes, round 6 no) only kill by touching(?)
        // bullet: extends 4 blocks, instant kill; can evade; invader cannot
        // if we stick to one place to kill one, we get 75p every 2 rounds; but at first we should move to get in touch with others
        //
        if (fire) { // we can fire!
            // killing players first
            if (nearestEnemy != null) {
                if (isAligned(nearestEnemy)) { // cannot shoot enemy, we choose to move to gain point
                    if (!someWallIsBlocking(nearestEnemy)) {
                        return new Move(fireAt(nearestEnemy));
                    } else { // cannot fire enemy, so we search invader
                        if (nearestInvader != null) {
                            return shootOrCrashInvader();
                        } else {
                            return new Move(moveTowardsPlayer(new Position(nearestEnemy.getX(), nearestEnemy.getY())));
                        }
                    }
                } else {
                    if (nearestInvader != null) {
                        return shootOrCrashInvader();
                    } else {
                        String howToMove = moveTowardsPlayer(nearestEnemy);
                        if (howToMove.contains(WAIT)) {
                            // we cannot move towards one direction
                            String dontGo = howToMove.split(" ")[1];
                            allFourDirections.remove(dontGo);
                            return moveToCenter();
                        } else {
                            return new Move(howToMove);
                        }
                    }

                }
            } else if (nearestInvader != null) {
                return shootOrCrashInvader();
            } else {
                return moveToCenter();
            }
        } else {
            return moveToCenter();
        }
    }



    private Move shootOrCrashInvader() {
        if (isAligned(nearestInvader.getPosition())) {
            if (nearestInvader.getNeutral()) {
                if (isNeighbor(nearestInvader.getPosition())) {
                    return new Move(moveTowards(nearestInvader.getPosition()));
                } else if (!someWallIsBlocking(nearestInvader.getPosition())) {
                    return new Move(fireAt(nearestInvader.getPosition())); // NEUTRAL can fire too
                } else {
                    // cannot fire because wall is blocking, so we begin to move
                    return moveToCenter();
                }
            } else {
                return new Move(fireAt(nearestInvader.getPosition()));
            }
        } else {
            // invader not aligned, move at will
            return moveToCenter();
        }
    }


    public Position findNearestPlayer(Position[] visiblePlayers) {
        if (visiblePlayers.length == 0) {
            return null;
        } else if (visiblePlayers.length == 1) {
            return visiblePlayers[0];
        } else {
            int[] shortestDimension = new int[visiblePlayers.length];
            Position pp = null;
            for (int i=0; i<visiblePlayers.length; i++) {
                pp = visiblePlayers[i];
                shortestDimension[i] = Math.min(Math.abs(pp.getX()-me.getX()), Math.abs(pp.getY() - me.getY()));
            }

            // get the shortest of x and y axis distance, and order again; get the shortest one as [0]
            Position nearest = null;
            int shortest = 0;
            for (int i=shortestDimension.length-1; i>=1; i--) {
                if (shortestDimension[i] < shortestDimension[i-1]) {
                    shortest = shortestDimension[i];
                    shortestDimension[i] = shortestDimension[i - 1];
                    shortestDimension[i-1] = shortest;

                    nearest = visiblePlayers[i];
                    visiblePlayers[i] = visiblePlayers[i - 1];
                    visiblePlayers[i - 1] = nearest;
                }
            }
            Position firstNearest = visiblePlayers[0];
            int shortestDistance = shortestDimension[0];
            // if first 2 have same distance: choose to move depending on moveToCenter true/false
            Position[] restVisibles = Arrays.copyOfRange(visiblePlayers, 1, visiblePlayers.length);
            int[] shortestDimension2 = Arrays.copyOfRange(shortestDimension, 1, shortestDimension.length);
            nearest = null;
            shortest = 0;
            for (int i=restVisibles.length-1; i>=1; i--) {
                if (shortestDimension2[i] < shortestDimension2[i-1]) {
                    shortest = shortestDimension2[i];
                    shortestDimension2[i] = shortestDimension2[i - 1];
                    shortestDimension2[i-1] = shortest;
                    nearest = restVisibles[i];
                    restVisibles[i] = restVisibles[i - 1];
                    restVisibles[i - 1] = nearest;
                }
            }
            if (shortestDistance == shortestDimension2[0]) {
                Position secondsNearest = restVisibles[0];
                // TODO coordinate moveToCenter and moveTosecondNearest
            }
            return visiblePlayers[0];
        }
    }
    private Invader findNearestInvader(Invader[] visibleInvaders) {
        if (visibleInvaders.length == 0) {
            return null;
        } else if (visibleInvaders.length == 1) {
            return visibleInvaders[0];
        } else {
            int[] shortestDimension = new int[visibleInvaders.length];
            Position pp = null;
            for (int i=0; i<visibleInvaders.length; i++) {
                pp = visibleInvaders[i].getPosition();
                shortestDimension[i] = Math.min(Math.abs(pp.getX()-me.getX()), Math.abs(pp.getY() - me.getY()));
            }

            // get the shortest of x and y axis distance, and order again; get the shortest one as [0]
            Invader nearest = null;
            for (int i=shortestDimension.length-1; i>=1; i--) {
                if (shortestDimension[i] < shortestDimension[i-1]) {
                    nearest = visibleInvaders[i];
                    visibleInvaders[i] = visibleInvaders[i - 1];
                    visibleInvaders[i - 1] = nearest;
                }
            }
            return visibleInvaders[0];
        }

    }

    /**
     * Move at will(but preferablly go to the center and avoid walls.
     */
    public Move moveToCenter() {
        // 1. don't move towards wall, and pay attention to the possible direction
        // cross-join these two lists
        ArrayList<String> possibleMovesInWall = checkNearWalls();
        ArrayList<String> possibleMoves = new ArrayList<>();

        for (String d: allFourDirections) {
            for (String ds: possibleMovesInWall)
            if (d.equals(ds)) {
                possibleMoves.add(d);
            }
        }

        if (possibleMoves.size() == 1) {
            return new Move(deadEnd.getSolution());
        } else if (possibleMoves.size() == 2) {
            if (possibleMovesCountered(possibleMoves)) {
                if (deadEnd != null) {
                    return new Move(deadEnd.getSolution());
                } else {
                    return new Move(possibleMoves.get(0)); // TODO
                }
            } else if (deadEnd != null){
                return new Move(getAroundCorner(possibleMoves));
            } else {
                return new Move(possibleMoves.get(0)); // TODO
            }
        } else if (possibleMoves.size() == 3 || possibleMoves.size() == 4) { // free to move
            if (deadEnd != null) {
                deadEnd = null;
            }
            // not aligned with the nearestPlayer enemy; depending on the x/y axis shortest distance we move;

            // TODO: preserve the last move to not to going back
            return new Move(possibleMoves.get(randomNumber(0, possibleMoves.size())));
        }
        return new Move(possibleMoves.get(randomNumber(0, possibleMoves.size())));
    }

    /**
     * To get around deadend corner; don't go back into tunnel!
     * Only when we have found a dead end.
     * @param possibleMove
     * @return
     */
    public String getAroundCorner(ArrayList<String> possibleMove) {
        possibleMove.remove(getCounter(deadEnd.getSolution()));
        return possibleMove.get(0);
    }

    private String getCounter(String direction) {
        switch (direction) {
            case ML:
                return MR;
            case MR:
                return ML;
            case MU:
                return MD;
            case MD:
                return MU;
        }
        return "";
    }

    /**
     * Get a random number from begin to begin+range, upbound exclusive.
     * @param begin
     * @param range
     * @return
     */
    public int randomNumber(int begin, int range) {
        return ThreadLocalRandom.current().nextInt(range) + begin;
    }
    private boolean possibleMovesCountered(List<String> possibleMoves) {
        if (possibleMoves.size() != 2) {
            return false;
        } else {
            return (possibleMoves.get(0).equals(ML) && possibleMoves.get(1).equals(MR)
                || possibleMoves.get(0).equals(MR) && possibleMoves.get(1).equals(ML)
                || possibleMoves.get(0).equals(MU) && possibleMoves.get(1).equals(MD)
                || possibleMoves.get(0).equals(MD) && possibleMoves.get(1).equals(MU)
            );
        }
    }

    private ArrayList<String> checkNearWalls() {
        int x = me.getX();
        int y = me.getY();
        Position l = new Position(x-1, y);
        Position r = new Position(x+1, y);
        Position u = new Position(x, y-1);
        Position d = new Position(x, y+1);
        Position[] fourDirections = new Position[]{l, r, u, d};
        List<String> possibleMoves = new ArrayList<>();
        if (!Arrays.asList(walls).contains(l)) {
            possibleMoves.add(ML);
        }
        if (!Arrays.asList(walls).contains(r)) {
            possibleMoves.add(MR);
        }
        if (!Arrays.asList(walls).contains(u)) {
            possibleMoves.add(MU);
        }
        if (!Arrays.asList(walls).contains(d)) {
            possibleMoves.add(MD);
        }

        if (possibleMoves.size() == 1) {
            switch (possibleMoves.get(0)) {
                case ML:
                    deadEnd = new DeadEnd(new Position(x+1, y), ML);
                    break;
                case MR:
                    deadEnd = new DeadEnd(new Position(x-1, y), MR);
                    break;
                case MU:
                    deadEnd = new DeadEnd(new Position(x, y+1), MU);
                    break;
                case MD:
                    deadEnd = new DeadEnd(new Position(x, y-1), MD);
                    break;
            }
        }
        return (ArrayList)possibleMoves;
    }

    /**
     * Check if any wall is blocking me and the other position.
     * Only check when is aligned; if not, make no sense.
     * @param position
     * @return
     */
    public boolean someWallIsBlocking(Position position) {
        if (me.getY() == position.getY()) { // horizontally aligned
            Position[] xFromMeToOther = new Position[Math.abs(position.getX() - me.getX())-1];
            for (int i=0; i<xFromMeToOther.length; i++) {
                xFromMeToOther[i] = new Position(Math.min(position.getX(), me.getX()) + 1 + i, me.getY());
            }
            for (Position w: walls) {
                if (Arrays.asList(xFromMeToOther).contains(w)) {
                    return true;
                }
            }
            return false;
        } else if (me.getX() == position.getX()) { // vertically aligned
            Position[] yFromMeToOther = new Position[Math.abs(position.getY() - me.getY())-1];
            for (int i=0; i<yFromMeToOther.length; i++) {
                yFromMeToOther[i] = new Position(me.getX(), Math.min(position.getY(), me.getY()) + 1 + i);
            }
            for (Position w: walls) {
                if (Arrays.asList(yFromMeToOther).contains(w)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }


    /**
     * If two positions are at distance 1
     * @param other the seconds position
     * @return if they are neighbor
     */
    private boolean isNeighbor(Position other) {
        return (me.getX() - other.getX() == 1 || me.getX() - other.getX() == -1 || me.getY() - other.getY() == 1 || me.getY() - other.getY() == -1);
    }

    /**
     * If two positions are at diagonally distance of squr(2).
     * @param other the second position
     * @return if they are near
     */
    private boolean isNear(Position other) {
        return ((me.getX() - other.getX() == 1 || me.getX() - other.getX() == -1) && (me.getY() - other.getY() == 1 || me.getY() - other.getY() == -1));
    }

    /**
     * If two positions are aligned vertically/horizontally.
     * @param other the second position
     * @return if they are aligned
     */
    private boolean isAligned(Position other) {
        return (me.getX() == other.getX() || me.getY() == other.getY());
    }

    /**
     * Calculate the relative position of the enemy to me; USE ONLY WHEN IS ALIGNED.
     * @param enemy the positon of the enemy
     * @return at which position I should fire
     */
    private String fireAt(Position enemy) {
        if (me.getY() == enemy.getY()) {
            if (me.getX() < enemy.getX()) {
                return FR;
            } else {
                return FL;
            }
        } else {
            if (me.getY() < enemy.getY()) {
                return FD;
            } else {
                return FU;
            }
        }

    }

    /**
     * Move towards another position USE ONLY WHEN IF ALIGHED, IS INVADER AND ISNEIGNBER
     * @param neutral_invader the positon of the enemy
     * @return at which position I should fire
     */
    private String moveTowards(Position neutral_invader) {
        if (me.getY() == neutral_invader.getY()) {
            if (me.getX() < neutral_invader.getX()) {
                return MR;
            } else {
                return ML;
            }
        } else {
            if (me.getY() < neutral_invader.getY()) {
                return MD;
            } else {
                return MU;
            }
        }

    }

    /**
     * Move towards a player to shot, on the axis where me and the other has the shortest distance.
     * If distance is even number, save to move; if is 1, just wait; if is 3: move too;
     * @param position the other player
     * @return
     */
    private String moveTowardsPlayer(Position position) {
        int xDistance = Math.abs(me.getX() - position.getX());
        int yDistance = Math.abs(me.getY() - position.getY());
        if (xDistance < yDistance) {
            // move on x axis
            if (xDistance != 1) {
                // we are save to move
                if (isAtLeft(position)) {
                    return MR;
                } else {
                    return ML;
                }
            } else {
                return WAIT;
            }
        } else { // move on y axis
            if (yDistance != 1) {
                if (isHigherThan(position)) {
                    return MD;
                } else {
                    return MU;
                }
            } else {
                return WAIT;
            }
        }
    }


    /**
     * Don't panic, just relax.
     */
    private void doSomething() {

    }

    private boolean isHigherThan(Position player) {
        return me.getY() < player.getY();
    }

    private boolean isLowerThen(Position player) {
        return me.getY() > player.getY();
    }

    /**
     * If my position is at left of enemy
     * @param player
     * @return
     */
    private boolean isAtLeft(Position player) {
        return me.getX() < player.getX();
    }

    /**
     * If my position is at right of enemy
     * @param player
     * @return
     */
    private boolean isAtRight(Position player) {
        return me.getX() > player.getX();
    }
}
