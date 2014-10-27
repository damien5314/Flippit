package com.ddiehl.android.flippit.utils;

import com.ddiehl.android.flippit.game.Board;
import com.ddiehl.android.flippit.game.BoardSpace;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class BoardIterator implements Iterator<BoardSpace> {
	Board b;
	int x, y;

	public BoardIterator(Board b) {
		this.b = b;
		x = 0; y = 0;
	}

	@Override
	public boolean hasNext() {
		if (y == b.height())
			return false;

		return true;
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
