package ru.codebattle.client.handled.strategy.move;

import ru.codebattle.client.handled.HandledGameBoard;
import ru.codebattle.client.handled.TypedBoardPoint;

import java.util.List;
import java.util.Optional;

public class StrategyManager {

	private final List<Strategy> strategies;

	public StrategyManager(List<Strategy> strategies) {
		this.strategies = strategies;
	}

	public TypedBoardPoint getDestinationPoint(HandledGameBoard gameBoard) {
		if (strategies.isEmpty()) {
			return getDefaultDestinationPoint(gameBoard);
		}
		Optional<TypedBoardPoint> optionalPoint = chooseStrategy().getDestinationBoardPoint(gameBoard);
		return optionalPoint.orElseGet(() -> getDefaultDestinationPoint(gameBoard));
	}

	private Strategy chooseStrategy() {
		return strategies.get(0);
	}

	private TypedBoardPoint getDefaultDestinationPoint(HandledGameBoard gameBoard) {
		int size = gameBoard.size();
		return gameBoard.getPoint(size / 2, size / 2).get();
	}
}
