package com.ddiehl.android.reversi.game;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class BoardIterator implements Iterator<BoardSpace> {
	private final Board b;
	private int x;
	private int y;

	public BoardIterator(Board b) {
		this.b = b;
		x = 0; y = 0;
	}

	@Override
	public boolean hasNext() {
		return y != b.height();
	}

	@Override
	public BoardSpace next() {
		if (!hasNext())
			throw new NoSuchElementException();

		BoardSpace s = b.getSpaceAt(x, y);
		if (++x == b.width()) {
			y++; x = 0;
		}

		return s;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
