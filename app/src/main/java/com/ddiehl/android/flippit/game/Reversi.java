package com.ddiehl.android.flippit.game;

public class Reversi {
	private static Player p1;
	private static Player p2;
	private static Board b;
	private static final int HEIGHT = 500;
	private static final int WIDTH = 500;
	private static Player currentPlayer;

	private Reversi() { }

	private static void start() {
		p1 = new Player(ReversiColor.White);
		p2 = new Player(ReversiColor.Black);
		b = Board.getInstance();
	}

	private static void newGame() {
		b.reset();
		currentPlayer = p1;
	}

}



