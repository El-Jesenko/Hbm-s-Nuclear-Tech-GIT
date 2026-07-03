package com.hbm.inventory.gui;

import java.util.Arrays;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerPneumoTube;
import com.hbm.inventory.gui.element.GUIElements;
import com.hbm.lib.RefStrings;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube.PneumaticChannel;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube;
import com.hbm.uninos.networkproviders.PneumaticNetwork;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

public class GUIPneumoTube extends GuiInfoContainer {

	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/storage/gui_pneumatic_pipe.png");
	private static ResourceLocation texture_endpoint = new ResourceLocation(RefStrings.MODID + ":textures/gui/storage/gui_pneumatic_endpoint.png");
	private static final int GUI_Y_OFFSET = 2;
	private static final int ICON_SIZE = 18;
	private static final int PIPE_CHANNEL_X = 151;
	private static final int PIPE_CHANNEL_GREEN_Y = 34 + GUI_Y_OFFSET;
	private static final int ENDPOINT_CHANNEL_X = 126;
	private static final int ENDPOINT_CHANNEL_GREEN_Y = 8 + GUI_Y_OFFSET;
	private static final int CHANNEL_BUTTON_SIZE = 18;
	private static final int RECEIVE_MODE_X = 151;
	private static final int RECEIVE_MODE_Y = 18;
	private static final int RECEIVE_MODE_U = 197;
	private static final int RECEIVE_MODE_V = 85;
	private static final int REDSTONE_U = 179;
	private static final int REDSTONE_V = 85;
	private static final int WHITELIST_ARROW_U = 176;
	private static final int WHITELIST_ARROW_V = 85;
	private static final int ROUND_ROBIN_FIELD_X = 130;
	private static final int ROUND_ROBIN_FIELD_Y = 17 + GUI_Y_OFFSET;
	public TileEntityPneumoTube tube;
	public boolean endpointOnly;
	private GuiTextField roundRobinField;

	public GUIPneumoTube(InventoryPlayer invPlayer, TileEntityPneumoTube tedf, boolean endpointOnly) {
		super(new ContainerPneumoTube(invPlayer, tedf));
		this.tube = tedf;
		this.endpointOnly = endpointOnly;

		this.xSize = 176;
		this.ySize = 185;
	}

	@Override
	public void initGui() {
		super.initGui();
		Keyboard.enableRepeatEvents(true);

		if(!endpointOnly) {
			this.roundRobinField = new GuiTextField(this.fontRendererObj, guiLeft + ROUND_ROBIN_FIELD_X, guiTop + ROUND_ROBIN_FIELD_Y, 18, 10);
			this.roundRobinField.setEnableBackgroundDrawing(false);
			this.roundRobinField.setTextColor(-1);
			this.roundRobinField.setDisabledTextColour(-1);
			this.roundRobinField.setMaxStringLength(2);
			this.roundRobinField.setText(String.valueOf(tube.roundRobinAmount));
		}
	}

	@Override
	public void drawScreen(int x, int y, float interp) {
		super.drawScreen(x, y, interp);

		if(!endpointOnly) {
			tube.compair.renderTankInfo(this, x, y, guiLeft + 7, guiTop + 16 + GUI_Y_OFFSET, 18, 18);

			this.drawCustomInfoStat(x, y, guiLeft + 7, guiTop + 54, 17, 17, x, y, (tube.redstone ? (EnumChatFormatting.GREEN + "ON ") : (EnumChatFormatting.RED + "OFF ")) + EnumChatFormatting.RESET + "with Redstone");
			this.drawCustomInfoStat(x, y, guiLeft + 6, guiTop + 36 + GUI_Y_OFFSET, 20, 8, x, y, "Compressor: " + tube.compair.getPressure() + " PU", "Max range: " + tube.getRangeFromPressure(tube.compair.getPressure()) + "m");

			this.drawCustomInfoStat(x, y, guiLeft + RECEIVE_MODE_X, guiTop + RECEIVE_MODE_Y, 17, 17, x, y, EnumChatFormatting.YELLOW + "Receiver order:", getReceiveModeLabel());
			this.drawCustomInfoStat(x, y, guiLeft + PIPE_CHANNEL_X, guiTop + PIPE_CHANNEL_GREEN_Y, CHANNEL_BUTTON_SIZE, CHANNEL_BUTTON_SIZE, x, y, EnumChatFormatting.YELLOW + "Send channel:", PneumaticChannel.fromId(tube.sendChannel).label, EnumChatFormatting.RED + "Click to change");
			this.drawCustomInfoStat(x, y, guiLeft + ROUND_ROBIN_FIELD_X, guiTop + ROUND_ROBIN_FIELD_Y, 18, 10, x, y, EnumChatFormatting.YELLOW + "Round robin amount:", tube.roundRobinAmount + " / 64", tube.receiveOrder == PneumaticNetwork.RECEIVE_ROBIN ? "Used by round robin" : EnumChatFormatting.GRAY + "Only used by round robin");
		} else {
			this.drawCustomInfoStat(x, y, guiLeft + ENDPOINT_CHANNEL_X, guiTop + ENDPOINT_CHANNEL_GREEN_Y, CHANNEL_BUTTON_SIZE, CHANNEL_BUTTON_SIZE, x, y, EnumChatFormatting.YELLOW + "Receive channel:", PneumaticChannel.fromId(tube.receiveChannel).label, EnumChatFormatting.RED + "Click to change");
		}

		if(this.mc.thePlayer.inventory.getItemStack() == null) {
			for(int i = 0; i < 15; ++i) {
				Slot slot = (Slot) this.inventorySlots.inventorySlots.get(i);

				if(this.isMouseOverSlot(slot, x, y) && tube.pattern.modes[i] != null) {
					this.func_146283_a(Arrays.asList(new String[] { EnumChatFormatting.RED + "Right click to change", ModulePatternMatcher.getLabel(tube.pattern.modes[i]) }), x, y - 30);
				}
			}
		}
	}

	@Override
	protected void mouseClicked(int x, int y, int i) {
		super.mouseClicked(x, y, i);
		if(this.roundRobinField != null) {
			this.roundRobinField.mouseClicked(x, y, i);
			if(!this.roundRobinField.isFocused()) {
				normalizeRoundRobinField(true);
			}
		}

		if(!endpointOnly) {
			click(x, y, 7, 54, 17, 17, "redstone");
			click(x, y, 6, 36 + GUI_Y_OFFSET, 20, 8, "pressure");
			click(x, y, RECEIVE_MODE_X, RECEIVE_MODE_Y, 17, 17, "receive");
			click(x, y, PIPE_CHANNEL_X, PIPE_CHANNEL_GREEN_Y, CHANNEL_BUTTON_SIZE, CHANNEL_BUTTON_SIZE, "sendChannel");
		} else {
			click(x, y, ENDPOINT_CHANNEL_X, ENDPOINT_CHANNEL_GREEN_Y, CHANNEL_BUTTON_SIZE, CHANNEL_BUTTON_SIZE, "receiveChannel");
		}

		click(x, y, 128, 30 + GUI_Y_OFFSET, 14, 26, "whitelist");
	}

	public void click(int x, int y, int left, int top, int sizeX, int sizeY, String name) {
		if(checkClick(x, y, left, top, sizeX, sizeY)) {
			mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
			NBTTagCompound data = new NBTTagCompound();
			data.setBoolean(name, true);
			PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, tube.xCoord, tube.yCoord, tube.zCoord));
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = this.tube.hasCustomInventoryName() ? this.tube.getInventoryName() : I18n.format(this.tube.getInventoryName());

		this.fontRendererObj.drawString(name, this.xSize / 2 - this.fontRendererObj.getStringWidth(name) / 2, 5, 4210752);
		this.fontRendererObj.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(endpointOnly ? texture_endpoint : texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		if(tube.whitelist) {
			drawTexturedModalRect(guiLeft + 139, guiTop + 33 + GUI_Y_OFFSET, WHITELIST_ARROW_U, WHITELIST_ARROW_V, 3, 6);
		} else {
			drawTexturedModalRect(guiLeft + 139, guiTop + 47 + GUI_Y_OFFSET, WHITELIST_ARROW_U, WHITELIST_ARROW_V, 3, 6);
		}

		if(!endpointOnly) {
			if(tube.redstone) drawTexturedModalRect(guiLeft + 7, guiTop + 54, REDSTONE_U, REDSTONE_V, ICON_SIZE, ICON_SIZE);
			drawTexturedModalRect(guiLeft + RECEIVE_MODE_X, guiTop + RECEIVE_MODE_Y, RECEIVE_MODE_U, RECEIVE_MODE_V + ICON_SIZE * tube.receiveOrder, ICON_SIZE, ICON_SIZE);
			drawTexturedModalRect(guiLeft + PIPE_CHANNEL_X, guiTop + PIPE_CHANNEL_GREEN_Y, 233, tube.sendChannel == PneumaticChannel.RED.id ? 18 : 0, 18, 18);
			drawChannelSwatch(guiLeft + 154, guiTop + 37 + GUI_Y_OFFSET, tube.sendChannel);

			drawTexturedModalRect(guiLeft + 6 + 4 * (tube.compair.getPressure() - 1), guiTop + 36 + GUI_Y_OFFSET, 179, 18, 4, 8);
			GUIElements.drawSmoothGauge(guiLeft + 16, guiTop + 25 + GUI_Y_OFFSET, this.zLevel, (double) tube.compair.getFill() / (double) tube.compair.getMaxFill(), 5, 2, 1, 0xCA6C43, 0xAB4223);
			if(this.roundRobinField != null) {
				if(this.roundRobinField.isFocused()) {
					drawRect(guiLeft + ROUND_ROBIN_FIELD_X - 1, guiTop + ROUND_ROBIN_FIELD_Y - 1, guiLeft + ROUND_ROBIN_FIELD_X + 19, guiTop + ROUND_ROBIN_FIELD_Y + 11, 0x66FFFFFF);
				}
				this.roundRobinField.drawTextBox();
			}
		} else {
			drawTexturedModalRect(guiLeft + ENDPOINT_CHANNEL_X, guiTop + ENDPOINT_CHANNEL_GREEN_Y, 178, tube.receiveChannel == PneumaticChannel.RED.id ? 18 : 0, 18, 18);
		}
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		if(this.roundRobinField != null) {
			this.roundRobinField.updateCursorCounter();
			if(!this.roundRobinField.isFocused() && !this.roundRobinField.getText().equals(String.valueOf(tube.roundRobinAmount))) {
				this.roundRobinField.setText(String.valueOf(tube.roundRobinAmount));
			}
		}
	}

	@Override
	protected void keyTyped(char c, int key) {
		if(this.roundRobinField != null && this.roundRobinField.isFocused()) {
			if(key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
				normalizeRoundRobinField(true);
				this.roundRobinField.setFocused(false);
				return;
			}

			String before = this.roundRobinField.getText();
			if(this.roundRobinField.textboxKeyTyped(c, key)) {
				String filtered = this.roundRobinField.getText().replaceAll("[^0-9]", "");
				if(!filtered.equals(this.roundRobinField.getText())) {
					this.roundRobinField.setText(filtered);
				}
				if(!before.equals(this.roundRobinField.getText())) {
					normalizeRoundRobinField(false);
				}
				return;
			}
		}

		super.keyTyped(c, key);
	}

	@Override
	public void onGuiClosed() {
		if(this.roundRobinField != null) {
			normalizeRoundRobinField(true);
		}
		Keyboard.enableRepeatEvents(false);
		super.onGuiClosed();
	}

	protected void normalizeRoundRobinField(boolean snapEmpty) {
		if(this.roundRobinField == null) return;

		String text = this.roundRobinField.getText().replaceAll("[^0-9]", "");
		if(text.isEmpty()) {
			if(snapEmpty) {
				this.roundRobinField.setText(String.valueOf(tube.roundRobinAmount));
			}
			return;
		}

		int amount = MathHelper.clamp_int(parseInt(text, tube.roundRobinAmount), 0, PneumaticNetwork.ITEMS_PER_TRANSFER);
		String normalized = String.valueOf(amount);
		if(!normalized.equals(this.roundRobinField.getText())) {
			this.roundRobinField.setText(normalized);
		}
		if(amount != tube.roundRobinAmount) {
			tube.roundRobinAmount = amount;
			NBTTagCompound data = new NBTTagCompound();
			data.setInteger("roundRobinAmount", amount);
			PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, tube.xCoord, tube.yCoord, tube.zCoord));
		}
	}

	protected int parseInt(String text, int fallback) {
		try {
			return Integer.parseInt(text);
		} catch(Exception ex) {
			return fallback;
		}
	}

	protected String getReceiveModeLabel() {
		if(tube.receiveOrder == PneumaticNetwork.RECEIVE_ROBIN) return "Round robin";
		if(tube.receiveOrder == PneumaticNetwork.RECEIVE_EVEN) return "Evenly split";
		return "Random";
	}

	protected void drawChannelSwatch(int x, int y, byte channelId) {
		int fill = channelId == PneumaticChannel.RED.id ? 0xFFFF3A2E : 0xFF2EEA5C;
		int dark = channelId == PneumaticChannel.RED.id ? 0xFF7A1A16 : 0xFF166F31;
		drawRect(x, y, x + 12, y + 12, dark);
		drawRect(x + 1, y + 1, x + 11, y + 11, fill);
	}
}
