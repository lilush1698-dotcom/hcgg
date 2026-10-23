package kirill.jungleairship;

import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Сервер -> клиент: список блоков, поставленных на дирижабль. */
public class MessageBlocks implements IMessage {
	public int entityId;
	public List<EntityAirship.Cell> cells;

	public MessageBlocks() {
	}

	public MessageBlocks(EntityAirship ship) {
		this.entityId = ship.getEntityId();
		this.cells = ship.placed;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		entityId = buf.readInt();
		cells = EntityAirship.readCells(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(entityId);
		EntityAirship.writeCells(buf, cells);
	}

	public static class Handler implements IMessageHandler<MessageBlocks, IMessage> {
		@Override
		public IMessage onMessage(MessageBlocks m, MessageContext ctx) {
			JungleAirshipMod.proxy.handleBlocks(m);
			return null;
		}
	}
}
