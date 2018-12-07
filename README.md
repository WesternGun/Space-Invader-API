# Space Invaders Tribute: 2018 Privalia

The 2018 event of the Privalia Code Challenge is called Space Invaders Tribute! The goal is easy: You have to create a REST API to kill the invaders (and the other players too). Your API will move a starship competing with the other player APIs in a real time challenge. All the starships will start in a different random position. The winner will be the starship with the highest score.

 - 100 points to kill another player's starship.
 - 50 points to kill a space invader.
 - -25 points when killed by another player or invader.

# Create a game to test:
[Click here to create a game](http://code-challenge-2018.privalia.com/game/create)


# Rules
The idea is quite simple: You have to create a REST API to move a starship across a random generated board. In this board there are also a number of space invaders and the other players' starships. You could move or fire at any direction (up, down, left or right), but you will have limited shots.

You will know the size of the board, the position of your character and some other data, but you won't see the entire board. The game engine will call your API, sending the current conditions, and your API will have to response with your next move: up, down, left, right, fire-up, fire-down, fire-left or fire-right.


The Player's Starship is the character moved by the player's REST API. The game engine can manage up to 8 differenct starships concurrently.


The Space Invaders will kill a you if they catch you. The invaders move randomly across the board. The number of invaders in the game and the spawn frequency can be configured and it changes from game to game.


When a new space invader is born, he is neutral for some movements. A neutral invader cannot kill a player, but players can kill neutral space invaders by touching them. After a few movements a neutral invader becomes a regular space invader.


When a player is killed, he explodes. A killed player will remain dead for some movements and then he respaws in the same place where he was killed.


This is a shot. A player fired. If a shot hunts a player or an invader, it kills him.

The game engine processes all the movements from the players and then the shots. So a player can dodge a shot. When all the movements and the shots are processed the game engine moves the invaders. So the invaders cannot dodge a shot.

The shots are instantaneous, but limited to the visible area. If two players shot each other at the same time, the both die. After shoting, a player must reload and he cannot shot again until after a few movements.

Rules
Basics
The Code Challenge 2018 is a programming competition.
It's opened to all Privalia employees particularly those ones in the IT department.
The required programming level to participate is high.
 

 

Space Invaders tribute

 - Join
     - To join the challenge you'll need to upload your API to your own Internet server.
     - Your API has to be accessible from the game server through an URL or IP address.
     - No support will be given to create the API or upload it to a server.

 - Dates
     - November 30th to December 13rd: Develop your API.
     - December 14th at 13:00h: Competition at Privalia training room.

 - Prizes
   - A t-shirt for all participants.
   - Three special prizes for the bests.

 - Competition format
     - The Competition format will depend on the number of participants.
     - The idea is to do some semifinals to discard non-optimized APIs and then a big final.
     - The game parameters (size of the board, number of invaders, etc) will be revealed the day of the competition.


# API Documentation
Players are simple REST APIs with two endpoints:

 - `/name`: should provide basic information on a player. See Name Endpoint for more information.
 - `/move`: will receive map information, and needs to reply with the next move for the player. See the Move Endpoint for more information.

## Name Endpoint
Your `/name` URL API will receive a POST request without body. Reply with JSON format indicating the name of the player or team and the contact email.

Request format
```
POST /name HTTP/1.1
Host: your-api.com
Response format
HTTP/1.1 200 OK
```
```json
{
    "name": string,        // The player or team name
    "email": string        // The contact email
}
```
## Move Endpoint
Your `/move` URL API will receive a POST request with the information about the visible part of the map in JSON format, and you need to reply with the next movement also in JSON format. See visible area section for more information.

Request format
```
POST /move HTTP/1.1
Host: your-api.com
```
```
{
    "game": {                  // Object - game data
        "id": uuid             // Unified unique ID of the game
    },
    "player": {                // Object - player data
        "id": uuid,            // Unified unique ID of the player
        "name": string,        // The name of the player
        "position": {          // Object - current position of the player
            "y": int,
            "x": int
        },
        "previous": {          // Object - previous positions of the player
            "y": int,
            "x": int
        },
        "area": {              // Object - visible area for the player
            "y1": int,
            "x1": int,
            "y2": int,
            "x2": int
        },
        "fire ": "bool"        // If the player can fire this round
    },
    "board": {                 // Object - board data
        "size": {              // Object - Total size of the maze
            "height": int,
            "width": int
        },
        "walls": [             // Array - visible walls
            {                  // Object - wall position
                "y": int,
                "x": int
            },
        ]
    },
    "players": [               // Array - other players positions
        {                      // Object - other players position
            "y": int,
            "x": int
        }
    ],
    "invaders": [              // Array - invaders positions
        {                      // Object - invader position
            "y": int,
            "x": int,
            "neutral": "bool"  // If the invader can be killed by touching
        }
    ]
}
```
Response format
```
HTTP/1.1 200 OK

{
    "move": string         // up, down, left or right, fire-up, fire-down, fire-right or fire-left
}
```

## Visible area
The board is a 0-based matrix, where [0, 0] is the upper left corner. The height and width are sent in the board.size.height and board.size.width vars of the request body in the Move endpoint.

Each player has its own visible area based on its current position (see Figure 1). The visible area is sent in the player.area var which is an object with four vars y1, x1, y2 and x2.

The information sent in board.walls, invaders and players vars depends on the visible area.

The fire range also depends on the visible area. You can shot at any straight direction (up, down, left or right) and the fire range is limited to what you can see.


Figure 1: Visible area
Example of request
```
POST /move HTTP/1.1
Host: your-api.com

{
    "game": {
        "id": "78df6fe1-4ba4-408a-ab99-b7122967214f"
    },
    "player": {
        "id": "78df6fe1-4ba4-408a-ab99-b7122967214f",
        "name": "Test player",
        "position": {
            "y": 3,
            "x": 4
        },
        "previous": {
            "y": 2,
            "x": 4
        },
        "area": {
            "y1": 0,
            "x1": 0,
            "y2": 8,
            "x2": 9
        },
        "fire": false
    },
    "board": {
        "size": {
            "height": 15,
            "width": 15
        },
        "walls": [ {
            "y": 0,
            "x": 1
        }, {
            "y": 0,
            "x": 2
        }, {
            "y": 0,
            "x": 3
        }, {
            "y": 0,
            "x": 4
        }, {
            "y": 0,
            "x": 5
        }, {
            "y": 0,
            "x": 6
        }, {
            "y": 0,
            "x": 7
        }, {
            "y": 0,
            "x": 8
        }, {
            "y": 0,
            "x": 9
        }, {
            "y": 1,
            "x": 0
        }, {
            "y": 2,
            "x": 0
        }, {
            "y": 3,
            "x": 0
        }, {
            "y": 4,
            "x": 0
        }, {
            "y": 5,
            "x": 0
        }, {
            "y": 5,
            "x": 1
        }, {
            "y": 5,
            "x": 2
        }, {
            "y": 5,
            "x": 3
        }, {
            "y": 5,
            "x": 4
        }, {
            "y": 5,
            "x": 5
        }, {
            "y": 5,
            "x": 6
        }, {
            "y": 5,
            "x": 7
        }, {
            "y": 5,
            "x": 9
        }, {
            "y": 6,
            "x": 0
        }, {
            "y": 6,
            "x": 4
        }, {
            "y": 7,
            "x": 0
        }, {
            "y": 8,
            "x": 0
        }, {
            "y": 8,
            "x": 4
        } ]
    },
    "players": [ {
        "y": 1,
        "x": 1
    } ],
    "invaders": [ {
        "y": 2,
        "x": 2,
        "neutral": false
    }, {
        "y": 8,
        "x": 3,
        "neutral": true
    } ]
}
```
Example of response
```
HTTP/1.1 200 OK

{
    "move": "fire-up"
}
```