package pneumaticCraft.common.progwidgets;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pneumaticCraft.client.gui.GuiProgrammer;
import pneumaticCraft.client.gui.programmer.GuiProgWidgetAreaShow;
import pneumaticCraft.common.ai.DroneAIAttackEntity;
import pneumaticCraft.common.ai.DroneAINearestAttackableTarget;
import pneumaticCraft.common.ai.IDroneBase;
import pneumaticCraft.common.ai.StringFilterEntitySelector;
import pneumaticCraft.common.entity.living.EntityDrone;
import pneumaticCraft.common.item.ItemPlastic;
import pneumaticCraft.lib.Textures;

public class ProgWidgetEntityAttack extends ProgWidget implements IAreaProvider, IEntityProvider{

    @Override
    public void addErrors(List<String> curInfo, List<IProgWidget> widgets){
        super.addErrors(curInfo, widgets);
        if(getConnectedParameters()[0] == null) {
            curInfo.add("gui.progWidget.area.error.noArea");
        }
    }

    @Override
    public boolean hasStepInput(){
        return true;
    }

    @Override
    public EntityAIBase getWidgetAI(IDroneBase drone, IProgWidget widget){
        return new DroneAIAttackEntity((EntityDrone)drone, 0.1D, false);
    }

    @Override
    public EntityAIBase getWidgetTargetAI(IDroneBase drone, IProgWidget widget){
        return new DroneAINearestAttackableTarget((EntityDrone)drone, 0, false, (ProgWidget)widget);
    }

    @Override
    public Class<? extends IProgWidget> returnType(){
        return null;
    }

    @Override
    public Class<? extends IProgWidget>[] getParameters(){
        return new Class[]{ProgWidgetArea.class, ProgWidgetString.class};
    }

    @Override
    public ResourceLocation getTexture(){
        return Textures.PROG_WIDGET_ATTACK;
    }

    @Override
    public String getWidgetString(){
        return "entityAttack";
    }

    @Override
    public List<Entity> getValidEntities(World world){
        StringFilterEntitySelector whitelistFilter = ProgWidgetAreaItemBase.getEntityFilter((ProgWidgetString)getConnectedParameters()[1], true);
        StringFilterEntitySelector blacklistFilter = ProgWidgetAreaItemBase.getEntityFilter((ProgWidgetString)getConnectedParameters()[3], false);
        return ProgWidgetAreaItemBase.getEntitiesInArea((ProgWidgetArea)getConnectedParameters()[0], (ProgWidgetArea)getConnectedParameters()[2], world, whitelistFilter, blacklistFilter);
    }

    @Override
    public boolean isEntityValid(Entity entity){
        StringFilterEntitySelector whitelistFilter = ProgWidgetAreaItemBase.getEntityFilter((ProgWidgetString)getConnectedParameters()[1], true);
        StringFilterEntitySelector blacklistFilter = ProgWidgetAreaItemBase.getEntityFilter((ProgWidgetString)getConnectedParameters()[3], false);
        return whitelistFilter.apply(entity) && !blacklistFilter.apply(entity);
    }

    @Override
    public void getArea(Set<BlockPos> area){
        getArea(area, (ProgWidgetArea)getConnectedParameters()[0], (ProgWidgetArea)getConnectedParameters()[2]);
    }

    public static void getArea(Set<BlockPos> area, ProgWidgetArea whitelistWidget, ProgWidgetArea blacklistWidget){
        if(whitelistWidget == null) return;
        ProgWidgetArea widget = whitelistWidget;
        while(widget != null) {
            ProgWidgetArea.EnumAreaType oldAreaType = widget.type;
            widget.type = ProgWidgetArea.EnumAreaType.FILL;
            widget.getArea(area);
            widget.type = oldAreaType;
            widget = (ProgWidgetArea)widget.getConnectedParameters()[0];
        }
        widget = blacklistWidget;
        while(widget != null) {
            ProgWidgetArea.EnumAreaType oldAreaType = widget.type;
            widget.type = ProgWidgetArea.EnumAreaType.FILL;
            Set<BlockPos> blacklistedArea = new HashSet<BlockPos>();
            widget.getArea(area);
            area.removeAll(blacklistedArea);
            widget.type = oldAreaType;
            widget = (ProgWidgetArea)widget.getConnectedParameters()[0];
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen getOptionWindow(GuiProgrammer guiProgrammer){
        return new GuiProgWidgetAreaShow(this, guiProgrammer);
    }

    @Override
    public WidgetDifficulty getDifficulty(){
        return WidgetDifficulty.EASY;
    }

    @Override
    public int getCraftingColorIndex(){
        return ItemPlastic.FIRE_FLOWER_DAMAGE;
    }
}
