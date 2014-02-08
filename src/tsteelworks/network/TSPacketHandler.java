package tsteelworks.network;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fluids.FluidStack;
import tsteelworks.TSteelworks;
import tsteelworks.blocks.logic.HighOvenLogic;
import tsteelworks.lib.Repo;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.relauncher.Side;

public class TSPacketHandler implements IPacketHandler
{
    @Override
    public void onPacketData (INetworkManager manager, Packet250CustomPayload packet, Player player)
    {
        final Side side = FMLCommonHandler.instance().getEffectiveSide();
        if (packet.channel.equals(Repo.modChan)) if (side == Side.SERVER)
        {
            handleServerPacket(packet, (EntityPlayerMP) player);
        }
        else
        {
            handleClientPacket(packet, (EntityPlayer) player);
        }
    }

    void handleClientPacket (Packet250CustomPayload packet, EntityPlayer player)
    {
        final DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(packet.data));
        try
        {
            inputStream.readByte();
        }
        catch (final Exception e)
        {
            TSteelworks.logger.warning("Failed at reading client packet for TSteelworks.");
            e.printStackTrace();
        }
    }

    void handleServerPacket (Packet250CustomPayload packet, EntityPlayerMP player)
    {
        final DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(packet.data));
        byte packetID;
        try
        {
            packetID = inputStream.readByte();
            TSteelworks.logger.info("Packet ID: " + packetID);
            if (packetID == 1) // High Oven
            {
                final int dimension = inputStream.readInt();
                final World world = DimensionManager.getWorld(dimension);
                final int x = inputStream.readInt();
                final int y = inputStream.readInt();
                final int z = inputStream.readInt();
                final boolean isShiftPressed = inputStream.readBoolean();
                final int fluidID = inputStream.readInt();
                final TileEntity te = world.getBlockTileEntity(x, y, z);
                if (te instanceof HighOvenLogic)
                {
                    FluidStack temp = null;
                    for (final FluidStack liquid : ((HighOvenLogic) te).moltenMetal)
                        if (liquid.fluidID == fluidID)
                        {
                            temp = liquid;
                        }
                    if (temp != null)
                    {
                        ((HighOvenLogic) te).moltenMetal.remove(temp);
                        if (isShiftPressed)
                        {
                            ((HighOvenLogic) te).moltenMetal.add(temp);
                        }
                        else
                        {
                            ((HighOvenLogic) te).moltenMetal.add(0, temp);
                        }
                    }
                    PacketDispatcher.sendPacketToAllInDimension(te.getDescriptionPacket(), dimension);
                }
            }
        }
        catch (final IOException e)
        {
            TSteelworks.logger.warning("Failed at reading server packet for TSteelworks.");
            e.printStackTrace();
        }
    }

    Entity getEntity (World world, int id)
    {
        for (final Object o : world.loadedEntityList)
            if (((Entity) o).entityId == id) return (Entity) o;
        return null;
    }
}