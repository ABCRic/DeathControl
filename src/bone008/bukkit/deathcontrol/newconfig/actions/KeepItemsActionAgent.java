package bone008.bukkit.deathcontrol.newconfig.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import bone008.bukkit.deathcontrol.StoredItemStack;
import bone008.bukkit.deathcontrol.newconfig.ActionAgent;
import bone008.bukkit.deathcontrol.newconfig.ActionResult;
import bone008.bukkit.deathcontrol.newconfig.DeathContext;
import bone008.bukkit.deathcontrol.util.Util;

public class KeepItemsActionAgent extends ActionAgent {

	private final KeepItemsAction action;

	private List<StoredItemStack> keptItems = new ArrayList<StoredItemStack>();

	public KeepItemsActionAgent(DeathContext context, KeepItemsAction action) {
		super(context, action);
		this.action = action;
	}

	@Override
	public void preprocess() {
		Iterator<StoredItemStack> it = context.getItemDrops().iterator();

		while (it.hasNext()) {
			StoredItemStack item = it.next();

			if (!action.isValidItem(item.itemStack))
				continue;

			int keptAmount = calcLossAmount(item.itemStack.getAmount());
			if (keptAmount == item.itemStack.getAmount()) {
				keptItems.add(item);
				it.remove();
			}
			else if (keptAmount > 0) {
				StoredItemStack kept = item.clone();
				kept.itemStack.setAmount(keptAmount);
				keptItems.add(kept);

				item.itemStack.setAmount(item.itemStack.getAmount() - keptAmount);
			}
			// do nothing if kept amount is 0
		}
	}

	private int calcLossAmount(int oldAmount) {
		double newAmount = ((double) oldAmount) * action.keepPct;
		int intAmount = (int) newAmount;

		// got a floating result --> apply random
		if (newAmount > intAmount && Util.getRandom().nextDouble() < newAmount - intAmount) {
			intAmount++;
		}

		return intAmount;
	}

	@Override
	public ActionResult execute() {
		if (keptItems.isEmpty()) // nothing to keep
			return ActionResult.FAILED;

		PlayerInventory inv = context.getVictim().getInventory();

		System.out.println("give back " + keptItems.size());

		for (StoredItemStack stored : keptItems) {
			if (inv.getItem(stored.slot) == null) // slot is empty
				inv.setItem(stored.slot, stored.itemStack);

			else { // slot is occupied --> add it regularly and drop if necessary
				HashMap<Integer, ItemStack> leftovers = inv.addItem(stored.itemStack);
				if (leftovers.size() > 0)
					Util.dropItems(context.getVictim().getLocation(), leftovers, false);
			}
		}

		return null;
	}

	@Override
	public void cancel() {
		for (StoredItemStack stored : keptItems)
			Util.dropItem(context.getDeathLocation(), stored.itemStack, true);
	}

}
