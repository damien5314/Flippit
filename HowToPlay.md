#How to Play

In Reversi there are two players, each represented by light or dark pieces on the game board.
Players take turns laying pieces adjacent to their opponent's pieces in order to capture spaces.
The objective of the game is to capture more spaces on the game board than your opponent.

The game board is an 8x8 square, giving a total of 64 spaces in the entire board.
The board is initialized with four pieces in the center four squares, alternating light and dark.

On each move, a player must claim an empty space, so that there is at least one of their opponent's spaces in between the claimed space and one of their own spaces.
After claiming a space, all of the opponent's spaces in between the claimed space and one of their own spaces are claimed by the player.

If at any point in the game a player has no valid moves, the turn will be passed back to the other player.
The game continues until neither player has any valid moves, or all spaces on the game board are claimed.

A good starting strategy for Reversi is to focus on claiming "stable" spaces, for which all of the adjacent spaces are of the same color, and therefore unable to be flanked for the rest of the game.
Because of this, the four corner spaces are considered to be the most valuable, followed by spaces along the edges.
Spaces immediately adjacent to the corners and edges should be avoided as these provide an opportunity for the opponent to claim stable spaces.

For more information on the history of the game and basic strategy, see the [Wikipedia](http://en.wikipedia.org/wiki/Reversi) page for Reversi.
