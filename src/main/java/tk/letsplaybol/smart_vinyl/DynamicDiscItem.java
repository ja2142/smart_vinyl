package tk.letsplaybol.smart_vinyl;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;

public class DynamicDiscItem extends Item {

    public static final String name = "dynamic_disc";

    public DynamicDiscItem(Properties props) {
        super(props.stacksTo(1).tab(ItemGroup.TAB_MISC));
		setRegistryName(name);
    }

}
