package pneumaticCraft.common.progwidgets;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pneumaticCraft.client.gui.GuiProgrammer;
import pneumaticCraft.client.gui.programmer.GuiProgWidgetString;
import pneumaticCraft.common.item.ItemPlastic;
import pneumaticCraft.lib.Textures;

public class ProgWidgetString extends ProgWidget{
    public String string = "";

    @Override
    public void getTooltip(List<String> curTooltip){
        super.getTooltip(curTooltip);
        if(addToTooltip()) curTooltip.add("Value: \"" + string + "\"");
    }

    protected boolean addToTooltip(){
        return true;
    }

    @Override
    public String getExtraStringInfo(){
        return "\"" + string + "\"";
    }

    @Override
    public boolean hasStepInput(){
        return false;
    }

    @Override
    public Class<? extends IProgWidget> returnType(){
        return ProgWidgetString.class;
    }

    @Override
    public Class<? extends IProgWidget>[] getParameters(){
        return new Class[]{ProgWidgetString.class};
    }

    @Override
    public ResourceLocation getTexture(){
        return Textures.PROG_WIDGET_TEXT;
    }

    @Override
    public String getWidgetString(){
        return "text";
    }

    @Override
    public void writeToNBT(NBTTagCompound tag){
        super.writeToNBT(tag);
        tag.setString("string", string);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag){
        super.readFromNBT(tag);
        string = tag.getString("string");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen getOptionWindow(GuiProgrammer guiProgrammer){
        return new GuiProgWidgetString(this, guiProgrammer);
    }

    @Override
    public WidgetDifficulty getDifficulty(){
        return WidgetDifficulty.EASY;
    }

    @Override
    public int getCraftingColorIndex(){
        return ItemPlastic.CHOPPER_PLANT_DAMAGE;
    }

}
