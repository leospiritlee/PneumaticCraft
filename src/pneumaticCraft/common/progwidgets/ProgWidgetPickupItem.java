package pneumaticCraft.common.progwidgets;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.ResourceLocation;
import pneumaticCraft.common.ai.DroneEntityAIPickupItems;
import pneumaticCraft.common.ai.IDroneBase;
import pneumaticCraft.common.item.ItemPlastic;
import pneumaticCraft.lib.Textures;

public class ProgWidgetPickupItem extends ProgWidgetAreaItemBase{

    @Override
    public String getWidgetString(){
        return "pickupItem";
    }

    @Override
    public ResourceLocation getTexture(){
        return Textures.PROG_WIDGET_PICK_ITEM;
    }

    @Override
    public EntityAIBase getWidgetAI(IDroneBase drone, IProgWidget widget){
        return new DroneEntityAIPickupItems(drone, (ProgWidgetAreaItemBase)widget);
    }

    @Override
    public int getCraftingColorIndex(){
        return ItemPlastic.POTION_PLANT_DAMAGE;
    }
}
