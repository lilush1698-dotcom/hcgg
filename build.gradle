package kirill.jungleairship;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Клиент -> сервер: кнопки управления дирижаблем (битовая маска). */
public class MessageControls implements IMessage {
	public int flags;

	public MessageControls() {
	}

	public MessageControls(int flags) {
		this.flags = flags;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		flags = buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeByte(flags);
	}

	public static class Handler implements IMessageHandler<MessageControls, IMessage> {
		@Override
		public IMessage onMessage(final MessageControls m, final MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable() {
				@Override
				public void run() {
					EntityPlayerMP p = ctx.getServerHandler().player;
					Entity r = p.getRidingEntity();
					if (r instanceof EntityAirship) {
						((EntityAirship) r).setControls(m.flags, p);
					}
				}
			});
			return null;
		}
	}
}
