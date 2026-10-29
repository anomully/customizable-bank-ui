package com.bankquantityorder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID.Bankmain;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetType;

/**
 * Lays out the bank's bottom row: buttons in the chosen order, the space left by hidden buttons
 * handled per {@link HiddenSpace}, and divider lines per {@link Dividers}.
 * <p>
 * Every position and width this class writes on the game's widgets is recorded. Each layout first
 * puts back what it wrote, unless the game has since written its own value (it repositions the
 * deposit buttons itself), so layouts always start from the game's current arrangement and never
 * drift. Must be used on the client thread.
 */
@Slf4j
class BottomRowLayout
{
	private static final int[] NATIVE_DIVIDERS = {Bankmain.BOTTOM_LINE4, Bankmain.BOTTOM_LINE6, Bankmain.DEPOSIT_LINE};

	// Used only if the game's own spacing can't be read: 1px between buttons, a divider 3px after
	// one button and 4px before the next
	private static final int DEFAULT_BUTTON_GAP = 1;
	private static final int DEFAULT_BEFORE_DIVIDER = 3;
	private static final int DEFAULT_AFTER_DIVIDER = 4;

	/**
	 * A button's widgets and its geometry in the game's arrangement, in row coordinates: quantity
	 * widgets live inside the quantity layer, so their row position is the layer's x plus their own.
	 */
	private static final class Button
	{
		RowButton id;
		Widget main;
		Widget attached;
		int baseX;
		int baseW;
		int attachedBaseX;
		boolean visible;
		int newX;
		int newW;
	}

	private final Client client;
	// {value before we wrote, value we wrote}
	private final Map<Widget, int[]> xWrites = new IdentityHashMap<>();
	private final Map<Widget, int[]> widthWrites = new IdentityHashMap<>();
	private final Set<Widget> hiddenDividers = Collections.newSetFromMap(new IdentityHashMap<>());
	// Extra divider lines this class created for "between buttons"; they live on the row widget
	private final List<Widget> createdDividers = new ArrayList<>();

	BottomRowLayout(Client client)
	{
		this.client = client;
	}

	/**
	 * Puts back everything this class changed, as long as the game hasn't overwritten it since.
	 */
	void restore()
	{
		List<Widget> touched = new ArrayList<>();
		putBack(xWrites, true, touched);
		putBack(widthWrites, false, touched);
		for (Widget w : hiddenDividers)
		{
			if (isCurrent(w))
			{
				w.setHidden(false);
			}
		}
		hiddenDividers.clear();
		for (Widget w : createdDividers)
		{
			w.setHidden(true);
		}
		revalidate(touched);
	}

	void apply(List<RowButton> order, HiddenSpace mode, boolean uniformWidths, Dividers dividers)
	{
		Widget row = client.getWidget(Bankmain.BOTTOM);
		Widget layer = client.getWidget(Bankmain.QUANTITY_LAYER);
		if (row == null || layer == null)
		{
			return; // bank not open
		}

		restore();

		Map<RowButton, Button> buttons = collect(layer);
		List<Widget> nativeDividers = new ArrayList<>();
		for (int id : NATIVE_DIVIDERS)
		{
			Widget w = client.getWidget(id);
			if (w != null && isAbsolute(w))
			{
				nativeDividers.add(w);
			}
		}
		if (buttons == null || nativeDividers.size() != NATIVE_DIVIDERS.length || !isAbsolute(layer))
		{
			log.debug("Bank bottom row not in an expected layout; leaving it alone");
			return;
		}

		// The game's own spacing, read from its arrangement around the first divider
		Button left = buttons.get(RowButton.REARRANGE);
		Button beforeDivider = buttons.get(RowButton.WITHDRAW_AS);
		int firstQuantityX = Integer.MAX_VALUE;
		int rowStart = Integer.MAX_VALUE;
		int rowEnd = Integer.MIN_VALUE;
		for (Button b : buttons.values())
		{
			if (b.id.isQuantity())
			{
				firstQuantityX = Math.min(firstQuantityX, b.baseX);
			}
			rowStart = Math.min(rowStart, b.baseX);
			rowEnd = Math.max(rowEnd, b.baseX + b.baseW);
		}
		int dividerX = nativeDividers.get(0).getOriginalX();
		int buttonGap = sane(beforeDivider.baseX - (left.baseX + left.baseW), DEFAULT_BUTTON_GAP);
		int gapBeforeDivider = sane(dividerX - (beforeDivider.baseX + beforeDivider.baseW), DEFAULT_BEFORE_DIVIDER);
		int gapAfterDivider = sane(firstQuantityX - dividerX, DEFAULT_AFTER_DIVIDER);

		// "Leave gaps" lays out every button, so hidden ones keep their space; the other modes lay
		// out only what's showing
		List<Button> placed = new ArrayList<>();
		for (RowButton id : order)
		{
			Button b = buttons.get(id);
			if (mode == HiddenSpace.LEAVE_GAPS || b.visible)
			{
				placed.add(b);
			}
		}

		int uniformW = commonWidth(buttons.values());
		int n = placed.size();
		boolean[] dividerAfter = new boolean[Math.max(n - 1, 0)];
		int span = 0;
		for (int i = 0; i < n; i++)
		{
			Button b = placed.get(i);
			b.newW = uniformWidths ? uniformW : b.baseW;
			span += b.newW;
			if (i < n - 1)
			{
				dividerAfter[i] = dividers == Dividers.BETWEEN_BUTTONS
					|| (dividers == Dividers.BETWEEN_SECTIONS && b.id.getGroup() != placed.get(i + 1).id.getGroup());
				span += dividerAfter[i] ? gapBeforeDivider + gapAfterDivider : buttonGap;
			}
		}

		int extra = rowEnd - rowStart - span;
		if (mode == HiddenSpace.STRETCH && extra > 0 && n > 0)
		{
			int each = extra / n;
			int remainder = extra % n;
			for (Button b : placed)
			{
				b.newW += each + (remainder-- > 0 ? 1 : 0);
			}
			extra = 0;
		}

		// "Leave gaps" keeps the row full width: whatever space is left over (from dividers removed or
		// narrower buttons) is shared out evenly between the buttons
		int[] extraGap = new int[Math.max(n - 1, 0)];
		if (mode == HiddenSpace.LEAVE_GAPS && extra > 0 && n > 1)
		{
			for (int i = 0; i < n - 1; i++)
			{
				extraGap[i] = extra / (n - 1) + (i < extra % (n - 1) ? 1 : 0);
			}
			extra = 0;
		}

		int x = mode == HiddenSpace.COLLAPSE_RIGHT ? rowStart + Math.max(extra, 0) : rowStart;
		for (int i = 0; i < n; i++)
		{
			Button b = placed.get(i);
			b.newX = x;
			x += b.newW;
			if (i < n - 1)
			{
				x += (dividerAfter[i] ? gapBeforeDivider + gapAfterDivider : buttonGap) + extraGap[i];
			}
		}

		// Dividers go only between two buttons that are showing (with "Leave gaps", hidden buttons
		// still hold space), centred in the space between them
		List<Integer> dividerPositions = new ArrayList<>();
		Button previous = null;
		for (Button b : placed)
		{
			if (!b.visible)
			{
				continue;
			}
			if (previous != null && (dividers == Dividers.BETWEEN_BUTTONS
				|| (dividers == Dividers.BETWEEN_SECTIONS && previous.id.getGroup() != b.id.getGroup())))
			{
				dividerPositions.add((previous.newX + previous.newW + b.newX) / 2);
			}
			previous = b;
		}

		List<Widget> touched = new ArrayList<>();
		// The quantity layer spans the whole row from x 0, so row coordinates work inside it too and
		// its buttons can sit anywhere along the row
		setX(layer, 0);
		setWidth(layer, rowEnd);
		touched.add(layer);
		for (Button b : placed)
		{
			setX(b.main, b.newX);
			setWidth(b.main, b.newW);
			touched.add(b.main);
			if (b.attached != null)
			{
				// Keep the icon or label centred on the button, however wide the button now is
				setX(b.attached, b.newX + (b.attachedBaseX - b.baseX) + (b.newW - b.baseW) / 2);
				touched.add(b.attached);
			}
		}
		placeDividers(row, nativeDividers, dividerPositions, touched);
		revalidate(touched);
	}

	private void placeDividers(Widget row, List<Widget> nativeDividers, List<Integer> positions, List<Widget> touched)
	{
		// A divider from an earlier bank session is gone with that session; hide it anyway before
		// forgetting it, so a line can never be left behind on screen
		createdDividers.removeIf(w ->
		{
			if (isChildOf(w, row))
			{
				return false;
			}
			w.setHidden(true);
			return true;
		});
		Widget template = nativeDividers.get(0);
		for (int i = 0; i < Math.max(positions.size(), nativeDividers.size()); i++)
		{
			Widget divider;
			if (i < nativeDividers.size())
			{
				divider = nativeDividers.get(i);
				if (i >= positions.size())
				{
					if (!divider.isSelfHidden())
					{
						divider.setHidden(true);
						hiddenDividers.add(divider);
					}
					continue;
				}
				setX(divider, positions.get(i));
			}
			else
			{
				int extraIndex = i - nativeDividers.size();
				if (extraIndex >= createdDividers.size())
				{
					createdDividers.add(createDivider(row, template));
				}
				divider = createdDividers.get(extraIndex);
				divider.setHidden(false);
				divider.setOriginalX(positions.get(i));
			}
			touched.add(divider);
		}
		for (int i = Math.max(positions.size() - nativeDividers.size(), 0); i < createdDividers.size(); i++)
		{
			createdDividers.get(i).setHidden(true);
		}
	}

	/**
	 * A new divider line drawn exactly like the game's own.
	 */
	private static Widget createDivider(Widget row, Widget template)
	{
		Widget w = row.createChild(-1, WidgetType.LINE);
		w.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		w.setYPositionMode(template.getYPositionMode());
		w.setWidthMode(WidgetSizeMode.ABSOLUTE);
		w.setHeightMode(template.getHeightMode());
		w.setOriginalY(template.getOriginalY());
		w.setOriginalWidth(template.getOriginalWidth());
		w.setOriginalHeight(template.getOriginalHeight());
		w.setTextColor(template.getTextColor());
		return w;
	}

	private static boolean isChildOf(Widget w, Widget parent)
	{
		Widget[] children = parent.getDynamicChildren();
		return children != null && Arrays.asList(children).contains(w);
	}

	private Map<RowButton, Button> collect(Widget layer)
	{
		Map<RowButton, Button> buttons = new HashMap<>();
		for (RowButton id : RowButton.values())
		{
			Widget main = client.getWidget(id.getButtonId());
			Widget attached = client.getWidget(id.getAttachedId());
			if (main == null || !isAbsolute(main) || (attached != null && !isAbsolute(attached)))
			{
				return null;
			}
			int offset = id.isQuantity() ? layer.getOriginalX() : 0;
			Button b = new Button();
			b.id = id;
			b.main = main;
			b.attached = attached;
			b.baseX = offset + main.getOriginalX();
			b.baseW = main.getOriginalWidth();
			b.attachedBaseX = attached == null ? 0 : offset + attached.getOriginalX();
			b.visible = !main.isSelfHidden();
			buttons.put(id, b);
		}
		return buttons;
	}

	/**
	 * The width most of the row's buttons share (search is the one wider exception).
	 */
	private static int commonWidth(Iterable<Button> buttons)
	{
		Map<Integer, Integer> counts = new HashMap<>();
		int best = 0;
		int bestCount = 0;
		for (Button b : buttons)
		{
			int count = counts.merge(b.baseW, 1, Integer::sum);
			if (count > bestCount)
			{
				best = b.baseW;
				bestCount = count;
			}
		}
		return best;
	}

	private static int sane(int measured, int fallback)
	{
		return measured >= 0 && measured <= 20 ? measured : fallback;
	}

	private void setX(Widget w, int x)
	{
		if (w.getOriginalX() != x)
		{
			xWrites.computeIfAbsent(w, k -> new int[]{w.getOriginalX(), 0})[1] = x;
			w.setOriginalX(x);
		}
	}

	private void setWidth(Widget w, int width)
	{
		if (w.getOriginalWidth() != width)
		{
			widthWrites.computeIfAbsent(w, k -> new int[]{w.getOriginalWidth(), 0})[1] = width;
			w.setOriginalWidth(width);
		}
	}

	private void putBack(Map<Widget, int[]> writes, boolean isX, List<Widget> touched)
	{
		for (Map.Entry<Widget, int[]> e : writes.entrySet())
		{
			Widget w = e.getKey();
			int[] rec = e.getValue();
			int current = isX ? w.getOriginalX() : w.getOriginalWidth();
			// If the value isn't the one we wrote, the game has repositioned it; its value wins
			if (isCurrent(w) && current == rec[1])
			{
				if (isX)
				{
					w.setOriginalX(rec[0]);
				}
				else
				{
					w.setOriginalWidth(rec[0]);
				}
				touched.add(w);
			}
		}
		writes.clear();
	}

	private boolean isCurrent(Widget w)
	{
		return client.getWidget(w.getId()) == w;
	}

	private static void revalidate(List<Widget> widgets)
	{
		for (Widget w : widgets)
		{
			revalidateTree(w);
		}
	}

	/**
	 * Revalidates a widget and everything inside it. The search button's bevel is nine pieces sized
	 * from the button's width, so they have to be recomputed whenever the button is resized.
	 */
	private static void revalidateTree(Widget w)
	{
		w.revalidate();
		for (Widget[] children : new Widget[][]{w.getStaticChildren(), w.getDynamicChildren(), w.getNestedChildren()})
		{
			if (children != null)
			{
				for (Widget child : children)
				{
					if (child != null)
					{
						revalidateTree(child);
					}
				}
			}
		}
	}

	private static boolean isAbsolute(Widget w)
	{
		return w.getXPositionMode() == WidgetPositionMode.ABSOLUTE_LEFT
			&& w.getWidthMode() == WidgetSizeMode.ABSOLUTE;
	}
}
