package com.bankquantityorder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.InterfaceID.Bankmain;

/**
 * The buttons in the bank's bottom row, declared in the game's native left-to-right order.
 * {@code group} is the section the game puts each button in, between its divider lines.
 */
@Getter
@RequiredArgsConstructor
enum RowButton
{
	REARRANGE(Bankmain.SWAP_INSERT, Bankmain.SWAP_INSERT_GRAPHIC, 0),
	WITHDRAW_AS(Bankmain.NOTE, Bankmain.NOTE_GRAPHIC, 0),
	ONE(Bankmain.QUANTITY1, Bankmain.QUANTITY1_TEXT, 1),
	FIVE(Bankmain.QUANTITY5, Bankmain.QUANTITY5_TEXT, 1),
	TEN(Bankmain.QUANTITY10, Bankmain.QUANTITY10_TEXT, 1),
	X(Bankmain.QUANTITYX, Bankmain.QUANTITYX_TEXT, 1),
	ALL(Bankmain.QUANTITYALL, Bankmain.QUANTITYALL_TEXT, 1),
	SEARCH(Bankmain.SEARCH, Bankmain.SEARCH_GRAPHIC, 2),
	PLACEHOLDERS(Bankmain.PLACEHOLDER, Bankmain.PLACEHOLDER_GRAPHIC, 2),
	EMPTY_CONTAINERS(Bankmain.DEPOSITCONTAINERS, Bankmain.DEPOSITCONTAINERS_GRAPHIC, 3),
	DEPOSIT_INVENTORY(Bankmain.DEPOSITINV, Bankmain.DEPOSITINV_GRAPHIC, 3),
	DEPOSIT_WORN(Bankmain.DEPOSITWORN, Bankmain.DEPOSITWORN_GRAPHIC, 3);

	static final List<RowButton> DEFAULT_ORDER = Arrays.asList(
		REARRANGE, WITHDRAW_AS, TEN, FIVE, ONE, X, ALL, SEARCH, PLACEHOLDERS, EMPTY_CONTAINERS, DEPOSIT_INVENTORY, DEPOSIT_WORN);

	private static final List<RowButton> QUANTITIES = Arrays.asList(ONE, FIVE, TEN, X, ALL);

	private final int buttonId;
	// The icon or label that sits on the button and moves with it
	private final int attachedId;
	private final int group;

	boolean isQuantity()
	{
		return QUANTITIES.contains(this);
	}

	static RowButton forButtonId(int widgetId)
	{
		for (RowButton b : values())
		{
			if (b.buttonId == widgetId)
			{
				return b;
			}
		}
		return null;
	}

	static String serialize(List<RowButton> order)
	{
		return order.stream().map(Enum::name).collect(Collectors.joining(","));
	}

	/**
	 * Reads a stored order. An older value holding only the 5 quantity buttons keeps that quantity
	 * order inside the default row; anything else incomplete falls back to the default.
	 */
	static List<RowButton> parse(String stored)
	{
		List<RowButton> order = new ArrayList<>();
		Set<RowButton> seen = EnumSet.noneOf(RowButton.class);
		for (String name : (stored == null ? "" : stored).split(","))
		{
			for (RowButton b : values())
			{
				if (b.name().equals(name.trim()) && seen.add(b))
				{
					order.add(b);
				}
			}
		}

		if (order.size() == values().length)
		{
			return order;
		}
		if (order.size() == QUANTITIES.size() && QUANTITIES.containsAll(order))
		{
			List<RowButton> migrated = new ArrayList<>();
			int q = 0;
			for (RowButton b : DEFAULT_ORDER)
			{
				migrated.add(b.isQuantity() ? order.get(q++) : b);
			}
			return migrated;
		}
		return DEFAULT_ORDER;
	}

	/**
	 * The order after dropping {@code moved} onto {@code target}: {@code moved} takes the target's
	 * position and the buttons in between shift one place toward where it came from.
	 */
	static List<RowButton> move(List<RowButton> order, RowButton moved, RowButton target)
	{
		List<RowButton> result = new ArrayList<>(order);
		int targetIndex = result.indexOf(target);
		result.remove(moved);
		result.add(targetIndex, moved);
		return result;
	}
}
