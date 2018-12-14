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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

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

    private int maxNumInvader;
    private int maxNumPlayer;
    private boolean stayInArea;

    private boolean invaderNumIncrease;
    private boolean searchNextArea;

    private Set<Position> pathHistory = new HashSet<>();
    private Set<Position[]> allDeadends = new HashSet<>();

    private Set<String> evadeAlignedEnemy = new HashSet<>();
    private Set<String> evadeCornerEnemy = new HashSet<>();
    private Set<String> evadeInvaderEnemyAxis = new HashSet<>();
    private Set<String> evadeInvader = new HashSet<>();
    private Set<String> evadeWalls = new HashSet<>();


    @Value("${crowd.threshold}")
    private double crowdedThreshold;

    @Value("${invader.density.threshold}")
    private int invaderDensity;

    private int fireCount;
    private boolean mustMove; // when you may be dead at the same location as other players
    private String[] path;
    private String moveToCenterX; // left or right
    private String moveToCenterY; // up or down
    private Set<String> allMoveToCenter = new HashSet<>();
    private Position centerPoint;
    private int reloadCounter; // 0-7
    private Set<Position> allWalls;

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
        // reset moveToCenter, getOut TODO
        game = body.getGame();
        player = body.getPlayer();
        fire = player.getFire();

        me = player.getPosition();
        pathHistory.add(me); // go back: remove last element, me TODO

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

        allMoveToCenter.add(moveToCenterX);
        allMoveToCenter.add(moveToCenterY);

        walls = board.getWalls(); // visible area walls, not all the walls
        allWalls.addAll(Arrays.stream(walls).collect(Collectors.toCollection(HashSet::new)));
        allDeadends.add(calculateDeadends(allWalls));


        if (areaSize < 81) {
            // we are not in the center of map
            goToCenter = true;
        } else {
            goToCenter = false;
        }
        if (walls.length / 81 >= crowdedThreshold) {
            // too many walls, we get out
            getOut = true;
        } else {
            getOut = false;
        }


        invaders = body.getInvaders();
        for (Invader i: invaders) {
            i.setPosition(new Position(i.getX(), i.getY()));
        }
        if (invaders.length >= maxNumInvader) {
            maxNumInvader = invaders.length;
            invaderNumIncrease = true; // we can stay here
        } else {
            invaderNumIncrease = false; // we may back up to last area, searchNextArea may be false; TODO
        }
        if (invaders.length >= invaderDensity) {
            stayInArea = true;
        } else {
            stayInArea = false;
        }


        players = body.getPlayers();
        nearestEnemy = findNearestPlayer(players);
        nearestInvader = findNearestInvader(invaders);




        // now we are good to make decisions.
        // if we must move because of collision: we move!
        // if not: we evade enemy, we never shoot until they are aligned with another invader!
        //     if aligned with enemy and at least one invader:
        //          yes: check if we can kill more than one thing
        //              yes: if we can fire:
        //                  yes: fire!
        //                  no: we evade axis
    //                  no: we evade
        //          no: check if we have corner enemy:
        //              yes: (wait or escapeEnemy)
        //              no: we check aligned invader:
        //                  yes: if neighbor and neutral:
        //                      yes: check if oneShotKillMore
        //                          yes: if we can fire:
        //                              yes: we fire
        //                              no: we evadeAxis
        //                          no: check if it is aligned with another player(L letter align)
        //                              yes: we evadeInvader
        //                              no: we crash
        //                      no: if we can fire:
        //                          yes: we fire
        //                          no: check if it is neutral
        //                              yes: we move towards
        //                              no: we move towards nearest axis
        //                  no: we check corner invader, check all; and collect possible evade steps, or just wait

        // after all, move:
        //      towards farest wall
        //      don't repeat yourself, don't go towards deadend
        //      go to center, go to where most invaders are
        //

        // if we may be dead by collision, we must move!
        if (previous.equals(me) && deadByCollision()) {
            mustMove = true; // we must get moving!
        } else {
            mustMove = false;
        }


        if (oneShotKillMore(nearestEnemy, nearestInvader)) {
            if (fire && !someWallIsBlocking(me, nearestEnemy) && !someWallIsBlocking(nearestEnemy, nearestInvader.getPosition())) {
                return new Move(fireAt(nearestEnemy));
            } else {
                evadeAlignedEnemy.addAll(evadeAxis(nearestEnemy, nearestInvader.getPosition()));
            }
        } else {
            List<Position> cornerEnemies = getCornerEnemies();
            if (cornerEnemies.size() > 0) {
                evadeCornerEnemy.addAll(calculateEvadePositions(cornerEnemies)); // cross get all possible evade path
            }
        }

        List<Invader> alignedInvaders = getAlignedInvaders();
        if (alignedInvaders.size() > 0) {
            Invader aligned = alignedInvaders.get(0);
            if (isNeighbor(aligned.getPosition())) {
                if (aligned.getNeutral()) {
                    Position anotherToEvade = invaderPossibleTarget(aligned);
                    if (anotherToEvade != null) {
                        evadeInvaderEnemyAxis.addAll(evadeAxis(aligned.getPosition(), anotherToEvade));
                    } else {
                        return new Move(moveTowards(aligned.getPosition()));
                    }
                } else if (fire) {
                    return new Move(fireAt(aligned.getPosition()));
                } else {
                    evadeInvader.addAll(evadePosition(aligned.getPosition()));
                }
            } else { // aligned invader is not neighbor
                if (fire) {
                    if (!someWallIsBlocking(me, aligned.getPosition())) {
                        return new Move(fireAt(aligned.getPosition()));
                    }
                }
            }
        } else {
            // not aligned invader, check corner
            List<Position> cornerInvaders = getCornerInvaders();
            if (cornerInvaders.size() > 0) {
                evadeInvader.addAll(calculateEvadePositions(cornerInvaders));
            }

        }


        // remember:
        // death penalty: 6 rounds death, -25p, revive first round cannot fire
        // you could revive and dead again, when others collide with you, so when you revive, first thing is to run!
        // reload: 7 rounds (round 1 fire -> round 8 fire again)
        // neutral invader: 5 rounds (round 1 yes, round 6 no) only kill by touching(?)
        // bullet: extends 4 blocks, instant kill; can evade; invader cannot
        // if we stick to one place to kill one, we get 75p every 2 rounds; but at first we should move to get in touch with others
        //

        return moveWithinBoundaries();
    }

    private Move moveWithinBoundaries() {
        Set<String> possibleMoves = new HashSet<>();
        if (!mustMove) {
            possibleMoves.add(WAIT);
        }
        if (goToCenter) { // if at last we are in center, we can also move to them
            possibleMoves.add(moveToCenterX);
            possibleMoves.add(moveToCenterY);
        }

        if (evadeInvaderEnemyAxis.size() > 0) {
            possibleMoves.remove(WAIT);
            possibleMoves.addAll(evadeAlignedEnemy.stream().filter(checkFarthestWalls()::contains).filter(allMoveToCenter::contains).collect(Collectors.toSet()));
            if (possibleMoves.size() > 0) {
                return new Move(pickRandomMoveFromSet(possibleMoves));
            } else {
                return new Move(pickRandomMoveFromSet(evadeInvaderEnemyAxis));
            }
        } else if (evadeAlignedEnemy.size() > 0) {
            possibleMoves.remove(WAIT); // TODO test when absent
            possibleMoves.addAll(evadeAlignedEnemy.stream().filter(checkFarthestWalls()::contains).filter(allMoveToCenter::contains).collect(Collectors.toSet()));
            if (possibleMoves.size() > 0) {
                return new Move(pickRandomMoveFromSet(possibleMoves));
            } else {
                return new Move(pickRandomMoveFromSet(evadeAlignedEnemy));
            }
        } else if (evadeCornerEnemy.size() > 0) {
            possibleMoves.addAll(evadeCornerEnemy.stream().filter(checkFarthestWalls()::contains).filter(allMoveToCenter::contains).collect(Collectors.toSet()));
            if (possibleMoves.size() > 0) {
                return new Move(pickRandomMoveFromSet(possibleMoves));
            } else {
                return new Move(pickRandomMoveFromSet(evadeCornerEnemy));
            }
        } else if (evadeInvader.size() > 0) {
            possibleMoves.addAll(evadeInvader.stream().filter(checkFarthestWalls()::contains).filter(allMoveToCenter::contains).collect(Collectors.toSet()));
            if (possibleMoves.size() > 0) {
                return new Move(pickRandomMoveFromSet(possibleMoves));
            } else {
                return new Move(pickRandomMoveFromSet(evadeInvader));
            }
        } else {
            possibleMoves.addAll(checkFarthestWalls().stream().filter(allMoveToCenter::contains).collect(Collectors.toSet()));
            if (possibleMoves.size() > 0) {
                return new Move(pickRandomMoveFromSet(possibleMoves));
            } else {
                List<String> goFarther = checkFarthestWalls();
                return new Move(goFarther.get(randomNumber(0, goFarther.size())));
            }
        }

    }


    private String pickRandomMoveFromSet(Set<String> set) {
        int index = 0;
        int target = randomNumber(0, set.size());
        Iterator<String> it = set.iterator();
        String move = "";
        while (it.hasNext()) {
            move = it.next();
            if (index == target) {
                return move;
            } else {
                index++;
            }
        }
        return move;

    }

    private List<String> calculateEvadePositions(List<Position> positions) {
        if (positions.size() == 1) {
            return evadePosition(positions.get(0));
        } else {
            List<String> initDirs = evadePosition(positions.get(0));

            for (int i=1; i<positions.size(); i++) {
                List<String> thisDirs = evadePosition(positions.get(i));
                initDirs = initDirs.stream().filter(thisDirs::contains).collect(Collectors.toList());
            }
            return initDirs;
        }
    }


    private List<Position> getCornerInvaders() {
        return Arrays.stream(invaders).filter(i -> isAtCornerOf(me, i.getPosition())).map(Invader::getPosition).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Cannot wait!
     * @param p
     * @return
     */
    private List<String> evadePosition(Position p) {
        List<String> directions = new ArrayList<String>();
        if (isHigherThan(me, p)) {
            directions.add(MU);
        } else if (isLowerThan(me, p)) {
            directions.add(MD);
        }
        if (isAtLeft(me, p)) {
            directions.add(ML);
        } else if (isAtRight(me, p)) {
            directions.add(MR);
        }
        return directions;
    }
    private Position invaderPossibleTarget(Invader invader) {
        for (Position p: players) {
            if (isHorizontalAligned(invader.getPosition(), p) || isVerticalAligned(invader.getPosition(), p)) {
                return p;
            }
        }
        return null;
    }

    private List<Position> getCornerEnemies() {
        List<Position> corners = new ArrayList<>();
        for (Position p: players) {
            if (isAtCornerOf(me, p)) {
                corners.add(p);
            }
        }
        return corners;
    }

    private List<Invader> getAlignedInvaders() {
        List<Invader> aligned = new ArrayList<>();
        for (Invader i: invaders) {
            if (isHorizontalAligned(me, i.getPosition()) || isVerticalAligned(me, i.getPosition())) {
                aligned.add(i);
            }
        }
        return aligned;
    }
    private List<String> getCornerEscapePath(Position corner) {
        ArrayList<String> directions = new ArrayList<>();
        if (isHigherThan(me, corner) && isAtLeft(me, corner)) {
            directions.add(MU); directions.add(ML);
        } else if (isHigherThan(me, corner) && isAtRight(me, corner)) {
            directions.add(MU); directions.add(MR);
        } else if (isLowerThan(me, corner) && isAtLeft(me, corner)) {
            directions.add(MD); directions.add(ML);
        } else if (isLowerThan(me, corner) && isAtRight(me, corner)) {
            directions.add(MD); directions.add(MR);
        }
        directions.add(WAIT);
        return directions;
    }

    private boolean isAtCornerOf(Position p1, Position p2) {
        return Math.abs(p1.getX() - p2.getX()) == 1 && Math.abs(p1.getY() - p2.getY()) == 1;
    }

    /**
     * Evade one axis marked by two positions.
     * Cannot wait!!
     * @param p1
     * @param p2
     * @return
     */
    private Set<String> evadeAxis(Position p1, Position p2) {
        Set<String> directions = new HashSet<>();

        if (p1.getX() == p2.getX()) {
            directions.add(MR);
            directions.add(ML);
        } else if (p1.getY() == p2.getY()) {
            directions.add(MU);
            directions.add(MD);
        }
        return directions;
    }
    private Position[] calculateDeadends(Set<Position> allWalls) {
        return new Position[]{}; // TODO
    }

    private boolean isXAxisNeighbor(Position p1, Position p2) {
        return Math.abs(p1.getX() - p2.getX()) == 1;
    }

    private boolean isYAxisNeighbor(Position p1, Position p2) {
        return Math.abs(p1.getY() - p2.getY()) == 1;
    }

    private boolean oneShotKillMore(Position nearestEnemy, Invader nearestInvader) {
        if (isVerticalAligned(me, nearestEnemy) && isVerticalAligned(me, nearestInvader.getPosition())) {
            if (isHigherThan(me, nearestEnemy) && isHigherThan(nearestEnemy, nearestInvader.getPosition())
                || isLowerThan(me, nearestEnemy) && isLowerThan(nearestEnemy, nearestInvader.getPosition()))  {
                return true;
            }
        } else if (isHorizontalAligned(me, nearestEnemy) && isHorizontalAligned(me, nearestInvader.getPosition())) {
            if (isAtLeft(me, nearestEnemy) && isAtLeft(nearestEnemy, nearestInvader.getPosition())
                || isAtRight(me, nearestEnemy) && isAtRight(nearestEnemy, nearestInvader.getPosition())) {
                return true;
            }
        }
        return false;
    }

    private boolean isHorizontalAligned(Position p1, Position p2) {
        return p1.getY() == p2.getY();
    }

    private boolean isVerticalAligned(Position p1, Position p2) {
        return p1.getX() == p2.getX();
    }

    /**
     * Check if me and some player is at same location.
     * Same time dead, same time revive, so when I revive, another too.
     * @return
     */
    private boolean deadByCollision() {
        boolean byCollision = false;
        for (Position p: players) {
            if (p.equals(me)) {
                byCollision = true;
            }
        }
        return byCollision;
    }

    private Move shootOrCrashInvader() {
        if (isAligned(nearestInvader.getPosition())) {
            if (nearestInvader.getNeutral()) {
                if (isNeighbor(nearestInvader.getPosition())) {
                    return new Move(moveTowards(nearestInvader.getPosition()));
                } else if (!someWallIsBlocking(me, nearestInvader.getPosition())) {
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
            int shortest = 0;
            for (int i=shortestDimension.length-1; i>=1; i--) {
                if (shortestDimension[i] < shortestDimension[i-1]) {
                    shortest = shortestDimension[i];
                    shortestDimension[i] = shortestDimension[i - 1];
                    shortestDimension[i-1] = shortest;
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
        ArrayList<String> possibleMoves = allFourDirections.stream().filter(possibleMovesInWall::contains).collect(Collectors.toCollection(ArrayList::new));

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

    private List<String> checkFarthestWalls() {
        List<Position> leftRow = new ArrayList<>();
        Position newPosition = null;
        int leftDis = me.getX() - area.getX1();
        int rightDis = area.getX2() - me.getX();
        int topDis = me.getY() - area.getY1();
        int botDis = area.getY2() - me.getY();
        Position leftLimit = new Position(area.getX1(), me.getY());
        Position rightLimit = new Position(area.getX2(), me.getY());
        Position topLimit = new Position(me.getX(), area.getY1());
        Position botLimit = new Position(me.getX(), area.getY2());
        for (int i=me.getX()-1; i>=area.getX1(); i--) {
            newPosition = new Position(i, me.getY());
            if (Arrays.asList(walls).contains(newPosition)) {
                leftDis = me.getX() - i;
                leftLimit = newPosition;
                break;
            }
        }
        for (int i=me.getX()+1; i<=area.getX2(); i++) {
            newPosition = new Position(i, me.getY());
            if (Arrays.asList(walls).contains(newPosition)) {
                rightDis = i - me.getX();
                rightLimit = newPosition;
                break;
            }
        }

        List<Position> topCol = new ArrayList<>();
        for (int i=me.getY()-1; i>=area.getY1(); i--) {
            newPosition = new Position(me.getX(), i);
            if (Arrays.asList(walls).contains(newPosition)) {
                topDis = me.getY() - i;
                topLimit = newPosition;
                break;
            }
        }

        List<Position> botCol = new ArrayList<>();
        for (int i=me.getY()+1; i<=area.getY2(); i++) {
            newPosition = new Position(me.getX(), i);
            if (Arrays.asList(walls).contains(newPosition)) {
                botDis = i - me.getY();
                botLimit = newPosition;
                break;
            }
        }

        // decreasing order
        int[] allDistances = new int[]{leftDis, rightDis, topDis, botDis};
        Arrays.asList(allDistances).sort(Collections.reverseOrder());
        List<String> results = new ArrayList<>();
        if (allDistances[0] == allDistances[1]) { // we at most take two
            if (allDistances[0] == leftDis) {
                results.add(ML);
            } else if (allDistances[0] == rightDis) {
                results.add(MR);
            } else if (allDistances[0] == topDis) {

                results.add(MU);
            } else if (allDistances[0] == botDis) {
                results.add(MD);
            }
            if (allDistances[1] == leftDis) {
                results.add(ML);
            } else if (allDistances[1] == rightDis) {
                results.add(MR);
            } else if (allDistances[1] == topDis) {

                results.add(MU);
            } else if (allDistances[1] == botDis) {
                results.add(MD);
            }
        } else {
            if (allDistances[0] == leftDis) {
                results.add(ML);
            } else if (allDistances[0] == rightDis) {
                results.add(MR);
            } else if (allDistances[0] == topDis) {

                results.add(MU);
            } else if (allDistances[0] == botDis) {
                results.add(MD);
            }
        }

        return results;
    }
    /**
     * Check if any wall is blocking me and the other position.
     * Only check when is aligned; if not, make no sense.
     */
    public boolean someWallIsBlocking(Position p1, Position p2) {
        if (p1.getX() == p2.getX()) {
            for (Position w: walls) {
                if (isVerticalAligned(w, p1) && between(w.getY(), p1.getY(), p2.getY())) {
                    return true;
                }
            }
            return false;
        } else if (p1.getY() == p2.getY()) {
            for (Position w: walls) {
                if (isHorizontalAligned(w, p1) && between(w.getX(), p1.getX(), p2.getX())) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }


    /**
     * If two positions are at distance 1
     * @param other the seconds position
     * @return if they are neighbor
     */
    private boolean isNeighbor(Position other) {
        return (me.getX() - other.getX() == 1 || me.getX() - other.getX() == -1 || me.getY() - other.getY() == 1 || me.getY() - other.getY() == -1);
    }

    private boolean between(int i, int a, int b) {
        if (a < b) {
            return i > a && i < b;
        } else if (a==b) {
            return false;
        } else {
            return i < a && i > b;
        }
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
        return (isHorizontalAligned(me, other) || isVerticalAligned(me, other));
    }

    /**
     * Calculate the relative position of the enemy to me; USE ONLY WHEN IS ALIGNED.
     * @param enemy the positon of the enemy
     * @return at which position I should fire
     */
    private String fireAt(Position enemy) {
        if (isHorizontalAligned(me, enemy)) {
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
        if (isHorizontalAligned(me, neutral_invader)) {
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
                if (isAtLeft(me, position)) {
                    return MR;
                } else {
                    return ML;
                }
            } else {
                return WAIT;
            }
        } else { // move on y axis
            if (yDistance != 1) {
                if (isHigherThan(me, position)) {
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

    private boolean isHigherThan(Position p1, Position p2) {
        return p1.getY() < p2.getY();
    }

    private boolean isLowerThan(Position p1, Position p2) {
        return p1.getY() > p2.getY();
    }

    /**
     * If p1 is at left of p2
     */
    private boolean isAtLeft(Position p1, Position p2) {
        return p1.getX() < p2.getX();
    }

    /**
     * If p1 is at right of p2
     */
    private boolean isAtRight(Position p1, Position p2) {
        return p1.getX() > p2.getX();
    }
}
