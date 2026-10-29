package com.bankquantityorder;

import static com.bankquantityorder.RowButton.ALL;
import static com.bankquantityorder.RowButton.DEPOSIT_INVENTORY;
import static com.bankquantityorder.RowButton.DEPOSIT_WORN;
import static com.bankquantityorder.RowButton.EMPTY_CONTAINERS;
import static com.bankquantityorder.RowButton.FIVE;
import static com.bankquantityorder.RowButton.ONE;
import static com.bankquantityorder.RowButton.PLACEHOLDERS;
import static com.bankquantityorder.RowButton.REARRANGE;
import static com.bankquantityorder.RowButton.SEARCH;
import static com.bankquantityorder.RowButton.TEN;
import static com.bankquantityorder.RowButton.WITHDRAW_AS;
import static com.bankquantityorder.RowButton.X;
import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class RowButtonTest
{
	@Test
	public void roundTrips()
	{
		List<RowButton> order = Arrays.asList(
			SEARCH, REARRANGE, WITHDRAW_AS, ALL, X, ONE, FIVE, TEN, PLACEHOLDERS, EMPTY_CONTAINERS, DEPOSIT_INVENTORY, DEPOSIT_WORN);
		assertEquals(order, RowButton.parse(RowButton.serialize(order)));
	}

	@Test
	public void migratesQuantityOnlyOrder()
	{
		assertEquals(Arrays.asList(
				REARRANGE, WITHDRAW_AS, ALL, X, ONE, FIVE, TEN, SEARCH, PLACEHOLDERS, EMPTY_CONTAINERS, DEPOSIT_INVENTORY, DEPOSIT_WORN),
			RowButton.parse("ALL,X,ONE,FIVE,TEN"));
	}

	@Test
	public void incompleteOrCorruptFallsBackToDefault()
	{
		assertEquals(RowButton.DEFAULT_ORDER, RowButton.parse("ONE,ONE,FIVE"));
		assertEquals(RowButton.DEFAULT_ORDER, RowButton.parse(null));
	}

	@Test
	public void movesAcrossSections()
	{
		// drag Search onto 5
		assertEquals(Arrays.asList(
				REARRANGE, WITHDRAW_AS, TEN, SEARCH, FIVE, ONE, X, ALL, PLACEHOLDERS, EMPTY_CONTAINERS, DEPOSIT_INVENTORY, DEPOSIT_WORN),
			RowButton.move(RowButton.DEFAULT_ORDER, SEARCH, FIVE));
	}
}
