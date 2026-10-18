package net.mcreator.commandcookiesretroexpansion.retro;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;
import java.util.List;

@EventBusSubscriber(modid=RetroItems.MOD,bus=EventBusSubscriber.Bus.MOD)
public final class RetroItems {
 public static final String MOD="commandcookies_retro_expansion";
 public static final String REMOVED_GAME="3dddb174d69799157d2de69fd12b47b51f5285f50eabea1a3abd3921207363a3";
 public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(MOD);
 public static final DeferredItem<Item> CARTRIDGE=ITEMS.register("rom_cartridge",()->new Cartridge(RetroSystem.NES));
 public static final DeferredItem<Item> SNES_CARTRIDGE=ITEMS.register("snes_rom_cartridge",()->new Cartridge(RetroSystem.SNES));
 public static final DeferredItem<Item> N64_CARTRIDGE=ITEMS.register("n64_rom_cartridge",()->new Cartridge(RetroSystem.N64));
 public static void init(IEventBus bus){ITEMS.register(bus);}
 public static boolean is(ItemStack s){return s.getItem() instanceof Cartridge;}
 public static RetroSystem system(ItemStack s){return s.getItem() instanceof Cartridge c?c.system:RetroSystem.NES;}
 public static boolean isGameHash(String hash){return hash!=null&&hash.matches("[0-9a-f]{64}")&&!hash.equals(REMOVED_GAME);}
 public static String hash(ItemStack s){if(!is(s))return "";String hash=data(s).getString("RetroHash");return isGameHash(hash)?hash:"";}
 public static boolean hasGame(ItemStack s){return !hash(s).isEmpty();}
 public static String title(ItemStack s){if(!hasGame(s))return "Empty";String title=data(s).getString("RetroTitle");return title.isEmpty()?system(s).label+" game":title;}
 private static CompoundTag data(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
 public static void configure(ItemStack s,String hash,String title){if(!is(s)||!isGameHash(hash))return;var d=data(s);d.putString("RetroHash",hash);d.putString("RetroTitle",title);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));}
 @SubscribeEvent public static void tab(BuildCreativeModeTabContentsEvent e){if(e.getTab()==net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModTabs.GAMING_CHAIRS.get()){e.accept(CARTRIDGE);e.accept(SNES_CARTRIDGE);e.accept(N64_CARTRIDGE);}}
 private static final class Cartridge extends Item {
  final RetroSystem system;
  Cartridge(RetroSystem system){super(new Properties().stacksTo(1));this.system=system;}
  @Override public Component getName(ItemStack s){return Component.literal(hasGame(s)?system.label+" ROM Cartridge: "+title(s):"Empty "+system.label+" ROM Cartridge");}
  @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){if(l.isClientSide)Client.open(h,system);return InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);}
  @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> text,TooltipFlag f){text.add(Component.literal(title(s)));text.add(Component.literal("Right-click in air to choose a local "+system.label+" game."));text.add(Component.literal("Insert into a "+system.label+"; use its monitor with the matching controller."));}
 }
 private static final class Client {static void open(InteractionHand h,RetroSystem s){net.minecraft.client.Minecraft.getInstance().setScreen(new net.mcreator.commandcookiesretroexpansion.retro.client.RomLibraryScreen(h,s));}}
}
