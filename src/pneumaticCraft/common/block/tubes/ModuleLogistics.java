package pneumaticCraft.common.block.tubes;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

import org.lwjgl.opengl.GL11;

import pneumaticCraft.client.model.IBaseModel;
import pneumaticCraft.client.model.tubemodules.ModelLogisticsModule;
import pneumaticCraft.client.util.RenderUtils;
import pneumaticCraft.common.ai.LogisticsManager;
import pneumaticCraft.common.ai.LogisticsManager.LogisticsTask;
import pneumaticCraft.common.network.NetworkHandler;
import pneumaticCraft.common.network.PacketUpdateLogisticModule;
import pneumaticCraft.common.semiblock.ISemiBlock;
import pneumaticCraft.common.semiblock.SemiBlockLogistics;
import pneumaticCraft.common.semiblock.SemiBlockManager;
import pneumaticCraft.common.tileentity.TileEntityPlasticMixer;
import pneumaticCraft.common.util.IOHelper;
import pneumaticCraft.common.util.PneumaticCraftUtils;
import pneumaticCraft.lib.Names;
import pneumaticCraft.proxy.CommonProxy.EnumGuiId;

public class ModuleLogistics extends TubeModule{
    private static final ModelLogisticsModule model = new ModelLogisticsModule();
    private SemiBlockLogistics cachedFrame;
    private boolean ticked;
    private int colorChannel;
    private int ticksSinceAction = -1;//client sided timer used to display the blue color when doing a logistic task.
    private int ticksSinceNotEnoughAir = -1;
    private boolean powered;
    private static final double MIN_PRESSURE = 3;
    private static final double ITEM_TRANSPORT_COST = 0.1;
    private static final double FLUID_TRANSPORT_COST = 0.001;

    @Override
    public double getWidth(){
        return 13 / 16D;
    }

    @Override
    protected double getHeight(){
        return 4.5D / 16D;
    }

    @Override
    public String getType(){
        return Names.MODULE_LOGISTICS;
    }

    @Override
    public IBaseModel getModel(){
        if(ticksSinceAction >= 0) {
            model.base1 = model.action;
        } else if(ticksSinceNotEnoughAir >= 0) {
            model.base1 = model.notEnoughAir;
        } else {
            model.base1 = hasPower() ? model.powered : model.notPowered;
        }
        return model;
    }

    @Override
    protected void renderModule(){
        super.renderModule();
        RenderUtils.glColorHex(0xFF000000 | ItemDye.field_150922_c[getColorChannel()]);
        model.renderChannelColorFrame(1 / 16F);
        GL11.glColor4d(1, 1, 1, 1);
    }

    @Override
    protected EnumGuiId getGuiId(){
        return null;
    }

    public int getColorChannel(){
        return colorChannel;
    }

    public void setColorChannel(int colorChannel){
        this.colorChannel = colorChannel;
    }

    public boolean hasPower(){
        return powered;
    }

    public void onUpdatePacket(int status, int colorChannel){
        powered = status > 0;
        if(status == 2) ticksSinceAction = 0;
        if(status == 3) ticksSinceNotEnoughAir = 0;
        this.colorChannel = colorChannel;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt){
        super.writeToNBT(nbt);
        nbt.setBoolean("powered", powered);
        nbt.setByte("colorChannel", (byte)colorChannel);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt){
        super.readFromNBT(nbt);
        powered = nbt.getBoolean("powered");
        colorChannel = nbt.getByte("colorChannel");
    }

    public SemiBlockLogistics getFrame(){
        if(cachedFrame == null) {
            ISemiBlock semiBlock = SemiBlockManager.getInstance(getTube().world()).getSemiBlock(getTube().world(), getTube().x() + dir.offsetX, getTube().y() + dir.offsetY, getTube().z() + dir.offsetZ);
            if(semiBlock instanceof SemiBlockLogistics) cachedFrame = (SemiBlockLogistics)semiBlock;
        }
        return cachedFrame;
    }

    @Override
    public boolean onActivated(EntityPlayer player){
        if(player.getCurrentEquippedItem() != null) {
            int colorIndex = TileEntityPlasticMixer.getDyeIndex(player.getCurrentEquippedItem());
            if(colorIndex >= 0) {
                if(!player.worldObj.isRemote) {
                    colorChannel = colorIndex;
                    NetworkHandler.sendToAllAround(new PacketUpdateLogisticModule(this, 0), getTube().world());
                }
                return true;
            }
        }
        return super.onActivated(player);
    }

    @Override
    public void update(){
        super.update();
        if(cachedFrame != null && cachedFrame.isInvalid()) cachedFrame = null;
        if(!getTube().world().isRemote) {
            if(powered != getTube().getAirHandler().getPressure(null) >= MIN_PRESSURE) {
                powered = !powered;
                NetworkHandler.sendToAllAround(new PacketUpdateLogisticModule(this, 0), getTube().world());
            }
            if(getTube().world().getWorldTime() % 20 == 0 && !ticked) {
                // Log.info(ModuleNetworkManager.getInstance().getConnectedModules(this).size() + "");
                LogisticsManager manager = new LogisticsManager();
                Map<SemiBlockLogistics, ModuleLogistics> frameToModuleMap = new HashMap<SemiBlockLogistics, ModuleLogistics>();
                for(TubeModule module : ModuleNetworkManager.getInstance().getConnectedModules(this)) {
                    if(module instanceof ModuleLogistics) {
                        ModuleLogistics logistics = (ModuleLogistics)module;
                        if(logistics.getColorChannel() == getColorChannel() && logistics.getFrame() != null) {
                            logistics.ticked = true;
                            if(logistics.hasPower()) {
                                frameToModuleMap.put(logistics.getFrame(), logistics);
                                manager.addLogisticFrame(logistics.getFrame());
                            }
                        }
                    }
                }
                PriorityQueue<LogisticsTask> tasks = manager.getTasks(null);
                for(LogisticsTask task : tasks) {
                    if(task.isStillValid(task.transportingItem != null ? task.transportingItem : task.transportingFluid.stack)) {
                        if(task.transportingItem != null) {
                            ItemStack remainder = IOHelper.insert(task.requester.getTileEntity(), task.transportingItem.copy(), true);
                            if(remainder == null || remainder.stackSize != task.transportingItem.stackSize) {
                                ItemStack toBeExtracted = task.transportingItem.copy();
                                if(remainder != null) toBeExtracted.stackSize -= remainder.stackSize;
                                ItemStack extractedStack = IOHelper.extract(task.provider.getTileEntity(), toBeExtracted, true);
                                if(extractedStack != null) {
                                    ModuleLogistics provider = frameToModuleMap.get(task.provider);
                                    ModuleLogistics requester = frameToModuleMap.get(task.requester);
                                    int airUsed = (int)(ITEM_TRANSPORT_COST * extractedStack.stackSize * PneumaticCraftUtils.distBetweenSq(provider.getTube().x(), provider.getTube().y(), provider.getTube().z(), requester.getTube().x(), requester.getTube().y(), requester.getTube().z()));
                                    if(provider.getTube().getAirHandler().getCurrentAir(null) >= airUsed && requester.getTube().getAirHandler().getCurrentAir(null) > airUsed) {
                                        sendModuleUpdate(provider, true);
                                        sendModuleUpdate(requester, true);
                                        provider.getTube().getAirHandler().addAir(-airUsed, null);
                                        requester.getTube().getAirHandler().addAir(-airUsed, null);
                                        IOHelper.insert(task.requester.getTileEntity(), extractedStack, false);
                                    } else {
                                        sendModuleUpdate(provider, false);
                                        sendModuleUpdate(requester, false);
                                    }
                                }
                            }
                        } else {
                            TileEntity providingTE = task.provider.getTileEntity();
                            TileEntity requestingTE = task.requester.getTileEntity();
                            if(providingTE instanceof IFluidHandler && requestingTE instanceof IFluidHandler) {
                                IFluidHandler provider = (IFluidHandler)task.provider.getTileEntity();
                                IFluidHandler requester = (IFluidHandler)task.requester.getTileEntity();

                                for(ForgeDirection di : ForgeDirection.VALID_DIRECTIONS) {
                                    int amountFilled = requester.fill(di, task.transportingFluid.stack, false);
                                    if(amountFilled > 0) {
                                        FluidStack drainingFluid = task.transportingFluid.stack.copy();
                                        drainingFluid.amount = amountFilled;
                                        FluidStack extractedFluid = null;
                                        ModuleLogistics p = frameToModuleMap.get(task.provider);
                                        ModuleLogistics r = frameToModuleMap.get(task.requester);
                                        int airUsed = 0;
                                        for(ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
                                            extractedFluid = provider.drain(d, drainingFluid, false);
                                            if(extractedFluid != null) {
                                                airUsed = (int)(FLUID_TRANSPORT_COST * extractedFluid.amount * PneumaticCraftUtils.distBetweenSq(p.getTube().x(), p.getTube().y(), p.getTube().z(), r.getTube().x(), r.getTube().y(), r.getTube().z()));
                                                if(p.getTube().getAirHandler().getCurrentAir(null) >= airUsed && r.getTube().getAirHandler().getCurrentAir(null) > airUsed) {
                                                    extractedFluid = provider.drain(d, drainingFluid, true);
                                                    break;
                                                } else {
                                                    sendModuleUpdate(p, false);
                                                    sendModuleUpdate(r, false);
                                                    extractedFluid = null;
                                                    break;
                                                }
                                            }
                                        }
                                        if(extractedFluid != null) {
                                            sendModuleUpdate(p, true);
                                            sendModuleUpdate(r, true);
                                            p.getTube().getAirHandler().addAir(-airUsed, null);
                                            r.getTube().getAirHandler().addAir(-airUsed, null);
                                            requester.fill(di, extractedFluid, true);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if(ticksSinceAction >= 0) {
                ticksSinceAction++;
                if(ticksSinceAction > 3) ticksSinceAction = -1;
            }
            if(ticksSinceNotEnoughAir >= 0) {
                ticksSinceNotEnoughAir++;
                if(ticksSinceNotEnoughAir > 20) ticksSinceNotEnoughAir = -1;
            }
        }
        ticked = false;
    }

    private void sendModuleUpdate(ModuleLogistics module, boolean enoughAir){
        NetworkHandler.sendToAllAround(new PacketUpdateLogisticModule(module, enoughAir ? 2 : 1), module.getTube().world());
    }
}
