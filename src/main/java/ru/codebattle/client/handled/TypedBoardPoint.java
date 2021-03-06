package ru.codebattle.client.handled;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.codebattle.client.api.BoardElement;
import ru.codebattle.client.handled.comparator.ExplosionStatusComparator;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@AllArgsConstructor
@Getter
public class TypedBoardPoint {
	private static final int RANGE_OF_EXPLOSION = 4;
	private static final Set<BoardElement> NOT_PASSED_FOR_EXPLOSION = Set.of(BoardElement.WALL, BoardElement.DESTROY_WALL);
	private static final Set<BoardElement> BOMBS = Set.of(BoardElement.BOMB_TIMER_1, BoardElement.BOMB_TIMER_2, BoardElement.BOMB_TIMER_3, BoardElement.BOMB_TIMER_4, BoardElement.BOMB_BOMBERMAN, BoardElement.OTHER_BOMB_BOMBERMAN);

	private final int x;
	private final int y;

	private final BoardElement boardElement;

	@Getter(AccessLevel.NONE)
	private final HandledGameBoard gameBoard;

	public Optional<TypedBoardPoint> shiftTop() {
		return shiftTop(1);
	}

	public Optional<TypedBoardPoint> shiftRight() {
		return shiftRight(1);
	}

	public Optional<TypedBoardPoint> shiftBottom() {
		return shiftBottom(1);
	}

	public Optional<TypedBoardPoint> shiftLeft() {
		return shiftLeft(1);
	}

	public Optional<TypedBoardPoint> shiftTop(int delta) {
		return gameBoard.getPoint(x, y - delta);
	}

	public Optional<TypedBoardPoint> shiftRight(int delta) {
		return gameBoard.getPoint(x + delta, y);
	}

	public Optional<TypedBoardPoint> shiftBottom(int delta) {
		return gameBoard.getPoint(x, y + delta);
	}

	public Optional<TypedBoardPoint> shiftLeft(int delta) {
		return gameBoard.getPoint(x - delta, y);
	}

	public ExplosionInfo getExplosionInfo() {
		TreeSet<TypedBoardPoint> foundedBombs = new TreeSet<>(new ExplosionStatusComparator());
		fillConnectedBombs(foundedBombs);
		if (foundedBombs.isEmpty()) {
			return new ExplosionInfo(null, ExplosionStatus.NEVER);
		}
		TypedBoardPoint earliestBomb = foundedBombs.first();

		return new ExplosionInfo(earliestBomb, earliestBomb.getMyExplosionStatus());
	}

	public boolean isNotPassForExplosion() {
		return NOT_PASSED_FOR_EXPLOSION.contains(boardElement);
	}

	public boolean isBomb() {
		return BOMBS.contains(boardElement);
	}

	public double calculateDistance(TypedBoardPoint anotherPoint) {
		return Math.sqrt(Math.pow(this.getX() - anotherPoint.getX(), 2) + Math.pow(this.getY() - anotherPoint.getY(), 2));
	}

	public ExplosionStatus getMyExplosionStatus() {
		switch (boardElement) {
			// TODO add BOMB under Bomberman (take a look at others bombs)
			case BOMB_BOMBERMAN:
			case OTHER_BOMB_BOMBERMAN:
			case BOMB_TIMER_4:
				return ExplosionStatus.AFTER_4_TICKS;
			case BOMB_TIMER_3:
				return ExplosionStatus.AFTER_3_TICKS;
			case BOMB_TIMER_2:
				return ExplosionStatus.AFTER_2_TICKS;
			case BOMB_TIMER_1:
				return ExplosionStatus.NEXT_TICK;
			default:
				return ExplosionStatus.NEVER;
		}
	}

	public boolean isNeighbour(TypedBoardPoint anotherPoint) {
		return (this.getX() - 1 == anotherPoint.getX() && this.getY() == anotherPoint.getY())
				|| (this.getX() + 1 == anotherPoint.getX() && this.getY() == anotherPoint.getY())
				|| (this.getX() == anotherPoint.getX() && this.getY() - 1 == anotherPoint.getY())
				|| (this.getX() == anotherPoint.getX() && this.getY() + 1 == anotherPoint.getY());
	}

	public boolean canBeDestroyedFrom(TypedBoardPoint anotherPoint) {
		if (this.getX() != anotherPoint.getX() && this.getY() != anotherPoint.getY()) {
			return false;
		}
		if (this.getX() == anotherPoint.getX()) {
			if (this.getY() < anotherPoint.getY() && this.getY() + RANGE_OF_EXPLOSION > anotherPoint.getY()) {
				ResearchDirection direction = ResearchDirection.BOTTOM;
				return isFreeForExplosion(direction, anotherPoint);
			}
			if (this.getY() > anotherPoint.getY() && this.getY() - RANGE_OF_EXPLOSION < anotherPoint.getY()) {
				ResearchDirection direction = ResearchDirection.TOP;
				return isFreeForExplosion(direction, anotherPoint);
			}
		} else {
			if (this.getX() < anotherPoint.getX() && this.getX() + RANGE_OF_EXPLOSION > anotherPoint.getX()) {
				ResearchDirection direction = ResearchDirection.RIGHT;
				return isFreeForExplosion(direction, anotherPoint);
			}
			if (this.getX() > anotherPoint.getX() && this.getX() - RANGE_OF_EXPLOSION < anotherPoint.getX()) {
				ResearchDirection direction = ResearchDirection.LEFT;
				return isFreeForExplosion(direction, anotherPoint);
			}
		}
		return false;
	}

	private boolean isFreeForExplosion(ResearchDirection direction, TypedBoardPoint lastPoint) {
		for (int offset = 1; offset < RANGE_OF_EXPLOSION; offset++) {
			Optional<TypedBoardPoint> isPointToResearch = getNextPointToResearch(direction, offset);
			if (isPointToResearch.isEmpty()) {
				throw new IllegalArgumentException("Something goes wrong");
			}

			TypedBoardPoint pointToResearch = isPointToResearch.get();

			if (lastPoint.equals(pointToResearch)) {
				return true;
			}
			if (pointToResearch.isNotPassForExplosion()) {
				return false;
			}
		}
		return false;
	}


	private void fillConnectedBombs(Set<TypedBoardPoint> foundedBombs) {
		for (ResearchDirection direction : ResearchDirection.values()) {
			for (int offset = 1; offset < RANGE_OF_EXPLOSION; offset++) {
				Optional<TypedBoardPoint> isPointToResearch = getNextPointToResearch(direction, offset);
				if (isPointToResearch.isEmpty()) {
					break;
				}
				TypedBoardPoint pointToResearch = isPointToResearch.get();
				if (pointToResearch.isNotPassForExplosion()) {
					break;
				}
				if (pointToResearch.isBomb() && !foundedBombs.contains(pointToResearch)) {
					foundedBombs.add(pointToResearch);
					pointToResearch.fillConnectedBombs(foundedBombs);
				}
			}
		}
	}

	private Optional<TypedBoardPoint> getNextPointToResearch(ResearchDirection direction, int offset) {
		switch (direction) {
			case TOP:
				return shiftTop(offset);
			case RIGHT:
				return shiftRight(offset);
			case BOTTOM:
				return shiftBottom(offset);
			case LEFT:
				return shiftLeft(offset);
			default:
				throw new UnsupportedOperationException("unsupported Direction " + direction);
		}
	}

	public boolean isWillBeDestoyed() {
		for (ResearchDirection direction : ResearchDirection.values()) {
			for (int offset = 1; offset < RANGE_OF_EXPLOSION; offset++) {
				Optional<TypedBoardPoint> isPointToResearch = getNextPointToResearch(direction, offset);
				if (isPointToResearch.isEmpty()) {
					break;
				}
				TypedBoardPoint pointToResearch = isPointToResearch.get();
				if (pointToResearch.isNotPassForExplosion()) {
					break;
				}
				if (pointToResearch.isBomb()) {
					return true;
				}
			}
		}
		return false;
	}

	private enum ResearchDirection {
		TOP, RIGHT, BOTTOM, LEFT
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TypedBoardPoint that = (TypedBoardPoint) o;
		return x == that.x && y == that.y;
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y);
	}

	@Override
	public String toString() {
		return String.format("[%s,%s]", x, y);
	}
}
