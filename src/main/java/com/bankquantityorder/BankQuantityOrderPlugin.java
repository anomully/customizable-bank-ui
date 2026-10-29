package com.bankquantityorder;

import com.google.inject.Provides;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetDrag;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetConfig;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

/**
 * Customizes the buttons in the bank's bottom row: their order (dragged in place), which ones
 * show, how the row fills the space hidden buttons leave, button widths and divider lines.
 * <p>
 * The game's own widgets are moved (not redrawn), so their click handlers,
 * right-click options, hover effects and selected-state highlight are untouched
 * and travel with them. The selected quantity (a varbit) is never written.
 */
@PluginDescriptor(
	name = "Customizable Bank UI",
	description = "Reorder, hide, resize and divide the buttons in the bank's bottom row",
	tags = {"bank", "quantity", "withdraw", "buttons", "reorder", "hide", "layout"}
)
public class BankQuantityOrderPlugin extends Plugin
{
	private static final int DRAG_FLAGS = WidgetConfig.DRAG | WidgetConfig.DRAG_ON;
	// Same thresholds Bank Tags uses for its draggable tabs, so a normal click never starts a drag
	private static final int DRAG_DEAD_ZONE = 5;
	private static final int DRAG_DEAD_TIME = 5;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private BankQuantityOrderConfig config;

	private boolean active;

	private BottomRowLayout layout;

	// Buttons whose drag flags we turned on, so turning dragging off only clears what we set
	private final Set<RowButton> dragEnabled = EnumSet.noneOf(RowButton.class);

	// Widgets this plugin hid, by identity, so showing them again never unhides something the game hid
	private final Set<Widget> hiddenByUs = Collections.newSetFromMap(new IdentityHashMap<>());

	@Provides
	BankQuantityOrderConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankQuantityOrderConfig.class);
	}

	@Override
	protected void startUp()
	{
		active = true;
		layout = new BottomRowLayout(client);
		// Handles enabling the plugin while the bank is already open
		clientThread.invokeLater(this::apply);
	}

	@Override
	protected void shutDown()
	{
		active = false;
		// Handles disabling the plugin while the bank is open
		clientThread.invokeLater(() ->
		{
			showAll();
			setDraggable(false);
			layout.restore();
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (active && BankQuantityOrderConfig.GROUP.equals(event.getGroup()))
		{
			clientThread.invokeLater(this::apply);
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		// Runs after every bank open and bank rebuild (tab switch, search, resize, etc).
		// Event handlers run on the client thread.
		if (active && event.getScriptId() == ScriptID.BANKMAIN_FINISHBUILDING)
		{
			apply();
		}
	}

	@Subscribe
	public void onWidgetDrag(WidgetDrag event)
	{
		// Fires every frame of any widget drag; only a release over another row button matters
		if (!active || !config.dragToReorder() || client.getMouseCurrentButton() != 0)
		{
			return;
		}

		Widget dragged = client.getDraggedWidget();
		Widget draggedOn = client.getDraggedOnWidget();
		if (dragged == null || draggedOn == null)
		{
			return;
		}

		RowButton moved = RowButton.forButtonId(dragged.getId());
		if (moved == null)
		{
			return;
		}

		// The game would send a drag packet for this release; the buttons are only draggable
		// because of this plugin, so the reorder must stay client-side (as core's prayer reorder does)
		client.setDraggedOnWidget(null);

		RowButton target = RowButton.forButtonId(draggedOn.getId());
		if (target == null || moved == target)
		{
			return;
		}

		List<RowButton> order = RowButton.move(RowButton.parse(config.order()), moved, target);
		// Saving fires ConfigChanged, which re-lays out the row
		configManager.setConfiguration(BankQuantityOrderConfig.GROUP, BankQuantityOrderConfig.ORDER_KEY,
			RowButton.serialize(order));
	}

	private void apply()
	{
		applyVisibility();
		setDraggable(config.dragToReorder());
		layout.apply(RowButton.parse(config.order()), config.hiddenSpace(), config.uniformWidths(), config.dividers());
	}

	private void applyVisibility()
	{
		// Drop widgets from a previous bank session; a reopened bank has new widget objects
		hiddenByUs.removeIf(w -> client.getWidget(w.getId()) != w);

		for (RowButton b : RowButton.values())
		{
			setShown(isShown(b), b.getButtonId(), b.getAttachedId());
		}
	}

	/**
	 * Hides the widgets, or shows again only the ones this plugin hid. Something the game itself
	 * hid is never unhidden.
	 */
	private void setShown(boolean shown, int... widgetIds)
	{
		for (int id : widgetIds)
		{
			Widget w = client.getWidget(id);
			if (w == null)
			{
				continue;
			}
			if (!shown && !w.isSelfHidden())
			{
				w.setHidden(true);
				hiddenByUs.add(w);
			}
			else if (shown && hiddenByUs.remove(w))
			{
				w.setHidden(false);
			}
		}
	}

	private void showAll()
	{
		for (Widget w : hiddenByUs)
		{
			if (client.getWidget(w.getId()) == w)
			{
				w.setHidden(false);
			}
		}
		hiddenByUs.clear();
	}

	private boolean isShown(RowButton b)
	{
		switch (b)
		{
			case REARRANGE:
				return config.showRearrange();
			case WITHDRAW_AS:
				return config.showWithdrawAs();
			case ONE:
				return config.showQuantity1();
			case FIVE:
				return config.showQuantity5();
			case TEN:
				return config.showQuantity10();
			case X:
				return config.showQuantityX();
			case ALL:
				return config.showQuantityAll();
			case SEARCH:
				return config.showSearch();
			case PLACEHOLDERS:
				return config.showPlaceholders();
			case EMPTY_CONTAINERS:
				return config.showEmptyContainers();
			case DEPOSIT_INVENTORY:
				return config.showDepositInventory();
			default:
				return config.showDepositWorn();
		}
	}

	private void setDraggable(boolean draggable)
	{
		for (RowButton b : RowButton.values())
		{
			Widget button = client.getWidget(b.getButtonId());
			if (button == null)
			{
				dragEnabled.clear(); // bank closed; reopened widgets start from the game's own flags
				return;
			}

			int mask = button.getClickMask();
			if (draggable && (mask & DRAG_FLAGS) != DRAG_FLAGS)
			{
				button.setClickMask(mask | DRAG_FLAGS);
				button.setDragDeadZone(DRAG_DEAD_ZONE);
				button.setDragDeadTime(DRAG_DEAD_TIME);
				dragEnabled.add(b);
			}
			else if (!draggable && dragEnabled.remove(b))
			{
				button.setClickMask(mask & ~DRAG_FLAGS);
			}
		}
	}
}
