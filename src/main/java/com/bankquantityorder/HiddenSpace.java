package com.bankquantityorder;

import lombok.RequiredArgsConstructor;

/**
 * What the bank's bottom row does with the space left by hidden buttons.
 */
@RequiredArgsConstructor
public enum HiddenSpace
{
	LEAVE_GAPS("Leave gaps"),
	COLLAPSE_LEFT("Collapse left"),
	COLLAPSE_RIGHT("Collapse right"),
	STRETCH("Stretch to fill");

	private final String label;

	@Override
	public String toString()
	{
		return label;
	}
}
