package net.mcreator.commandcookieselectronicmod.client;

/** Samsung presentation of the shared, tested phone applications. */
public final class SamsungScreen extends IphoneScreen {
    public SamsungScreen(String color,java.util.UUID device){super(color,true,PhoneAppState.key(net.minecraft.client.Minecraft.getInstance().player.getUUID(),device));}
    public SamsungScreen(String color) { super(color, true); }
}
