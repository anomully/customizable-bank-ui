package com.bankquantityorder;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(BankQuantityOrderConfig.GROUP)
public interface BankQuantityOrderConfig extends Config
{
	String GROUP = "bankquantityorder";
	String ORDER_KEY = "order";

	@ConfigSection(
		name = "Layout",
		description = "How the bank's bottom row is arranged",
		position = 0
	)
	String layoutSection = "layout";

	@ConfigSection(
		name = "Button visibility",
		description = "Buttons shown in the bank's bottom row",
		position = 10
	)
	String visibilitySection = "visibility";

	@ConfigItem(
		keyName = "dragToReorder",
		name = "Drag to reorder",
		description = "Drag the buttons in the bank's bottom row to change their order.",
		section = layoutSection,
		position = 1
	)
	default boolean dragToReorder()
	{
		return false;
	}

	@ConfigItem(
		keyName = ORDER_KEY,
		name = "",
		description = "",
		hidden = true
	)
	default String order()
	{
		return RowButton.serialize(RowButton.DEFAULT_ORDER);
	}

	@ConfigItem(keyName = "hiddenSpace", name = "Leftover space", description = "What the row does with the space left by hidden buttons.",
		section = layoutSection, position = 2)
	default HiddenSpace hiddenSpace()
	{
		return HiddenSpace.LEAVE_GAPS;
	}

	@ConfigItem(keyName = "uniformWidths", name = "Uniform button widths", description = "Make every button the same width as the others, including search.",
		section = layoutSection, position = 3)
	default boolean uniformWidths()
	{
		return false;
	}

	@ConfigItem(keyName = "dividers", name = "Dividers", description = "Where divider lines go between buttons.",
		section = layoutSection, position = 4)
	default Dividers dividers()
	{
		return Dividers.BETWEEN_SECTIONS;
	}

	@ConfigItem(keyName = "showRearrange", name = "Rearrange mode", description = "Show the Swap/Insert button.",
		section = visibilitySection, position = 11)
	default boolean showRearrange()
	{
		return true;
	}

	@ConfigItem(keyName = "showWithdrawAs", name = "Withdraw as note", description = "Show the Item/Note button.",
		section = visibilitySection, position = 12)
	default boolean showWithdrawAs()
	{
		return true;
	}

	@ConfigItem(keyName = "showQuantity1", name = "Quantity 1", description = "Show the 1 button.",
		section = visibilitySection, position = 13)
	default boolean showQuantity1()
	{
		return true;
	}

	@ConfigItem(keyName = "showQuantity5", name = "Quantity 5", description = "Show the 5 button.",
		section = visibilitySection, position = 14)
	default boolean showQuantity5()
	{
		return true;
	}

	@ConfigItem(keyName = "showQuantity10", name = "Quantity 10", description = "Show the 10 button.",
		section = visibilitySection, position = 15)
	default boolean showQuantity10()
	{
		return true;
	}

	@ConfigItem(keyName = "showQuantityX", name = "Quantity X", description = "Show the X button.",
		section = visibilitySection, position = 16)
	default boolean showQuantityX()
	{
		return true;
	}

	@ConfigItem(keyName = "showQuantityAll", name = "Quantity All", description = "Show the All button.",
		section = visibilitySection, position = 17)
	default boolean showQuantityAll()
	{
		return true;
	}

	@ConfigItem(keyName = "showPlaceholders", name = "Placeholders", description = "Show the placeholders button.",
		section = visibilitySection, position = 18)
	default boolean showPlaceholders()
	{
		return true;
	}

	@ConfigItem(keyName = "showSearch", name = "Search", description = "Show the search button.",
		section = visibilitySection, position = 19)
	default boolean showSearch()
	{
		return true;
	}

	@ConfigItem(keyName = "showEmptyContainers", name = "Empty containers", description = "Show the empty containers button.",
		section = visibilitySection, position = 20)
	default boolean showEmptyContainers()
	{
		return true;
	}

	@ConfigItem(keyName = "showDepositInventory", name = "Deposit inventory", description = "Show the deposit inventory button.",
		section = visibilitySection, position = 21)
	default boolean showDepositInventory()
	{
		return true;
	}

	@ConfigItem(keyName = "showDepositWorn", name = "Deposit worn items", description = "Show the deposit worn items button.",
		section = visibilitySection, position = 22)
	default boolean showDepositWorn()
	{
		return true;
	}
}
