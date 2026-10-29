package com.bankquantityorder;

import lombok.RequiredArgsConstructor;

/**
 * Where divider lines go in the bank's bottom row.
 */
@RequiredArgsConstructor
public enum Dividers
{
	BETWEEN_SECTIONS("Between sections"),
	BETWEEN_BUTTONS("Between buttons"),
	NONE("None");

	private final String label;

	@Override
	public String toString()
	{
		return label;
	}
}
