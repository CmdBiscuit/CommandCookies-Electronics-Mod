package net.mcreator.commandcookiesretroexpansion.retro;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.mcreator.commandcookieselectronicmod.nes.*;
import net.mcreator.commandcookieselectronicmod.power.PowerNetwork;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** The server owns reservations; local ROM paths and bytes never cross the network. */
@EventBusSubscriber(modid=RetroItems.MOD)
public final class RetroSessions {
 public record Session(UUID player,UUID token,ServerLevel level,BlockPos monitor,BlockPos console,UUID identity,String hash,RetroSystem system,long[] last){}
 private static final Map<MinecraftServer,Map<UUID,Session>> sessions=new WeakHashMap<>();
 private static Map<UUID,Session> map(MinecraftServer s){return sessions.computeIfAbsent(s,k->new HashMap<>());}
 private static void notice(ServerPlayer p,String s){p.displayClientMessage(Component.literal(s),true);}
 public static void configure(ServerPlayer p,RetroPackets.Configure m){
  var stack=p.getItemInHand(m.offhand()?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
  if(!p.isAlive()||p.isSpectator()||!RetroItems.is(stack)||!RetroItems.isGameHash(m.hash()))return;
  String title=m.title().replaceAll("[\\p{Cntrl}Â§]","").trim();if(title.isEmpty())return;if(title.length()>48)title=title.substring(0,48);
  RetroItems.configure(stack,m.hash(),title);p.getInventory().setChanged();notice(p,"Cartridge loaded: "+title);
 }
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void click(PlayerInteractEvent.RightClickBlock e){
  var p=e.getEntity();var held=p.getItemInHand(e.getHand());
  if(NesSystem.cable(held)||NesSystem.cable(p.getMainHandItem())||NesSystem.cable(p.getOffhandItem())||held.is(net.mcreator.commandcookieselectronicmod.init.CommandcookiesElectronicModModItems.WRENCH.get()))return;
  if(p.isShiftKeyDown()&&held.getItem() instanceof BlockItem)return;
  var c=RetroConsoles.at(e.getLevel(),e.getPos());boolean monitor=false;
  if(c==null&&e.getLevel().getBlockEntity(e.getPos()) instanceof NesDeviceBlockEntity m&&m.monitor()){monitor=true;c=RetroConsoles.forMonitor(m);}
  if(c==null)return;boolean inserted=RetroItems.is(c.cartridge()),incoming=RetroItems.is(held)||RetroItems.is(p.getOffhandItem());
  if(monitor&&!inserted||!monitor&&!inserted&&!incoming)return;
  e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
  if(!(p instanceof ServerPlayer player)||e.getLevel().isClientSide)return;
  if(p.isSpectator()||!p.isAlive()||p.distanceToSqr(e.getPos().getCenter())>64)return;
  if(!monitor){
   if(p.isShiftKeyDown()&&inserted){releaseConsole(player.getServer(),c.identity(),"Cartridge removed.");var drop=c.cartridge().copy();c.cartridge(ItemStack.EMPTY);if(!player.getInventory().add(drop))player.drop(drop,false);notice(player,"Cartridge ejected.");return;}
   if(incoming){var source=RetroItems.is(held)?held:p.getOffhandItem();
    if(RetroItems.system(source)!=c.system()){notice(player,"Use a "+c.system().label+" ROM cartridge for this console.");return;}
    if(!RetroItems.hasGame(source)){notice(player,"Choose a local "+c.system().label+" game first: right-click the ROM cartridge in air.");return;}
    if(!c.cartridge().isEmpty()){notice(player,"Crouch + right-click to eject the current cartridge.");return;}
    c.cartridge(source.copyWithCount(1));if(!p.getAbilities().instabuild)source.shrink(1);notice(player,RetroItems.title(c.cartridge())+" inserted. Hold the "+c.system().label+" controller and use the linked monitor.");return;
   }
   notice(player,RetroItems.title(c.cartridge())+" is loaded. Use the linked monitor while holding the "+c.system().label+" controller.");return;
  }
  if(RetroItems.system(c.cartridge())!=c.system()||!RetroItems.hasGame(c.cartridge())){notice(player,"This cartridge has no matching game selected. Eject it and choose a local "+c.system().label+" game.");return;}
  if(!PowerNetwork.powered(e.getLevel(),e.getPos())||!PowerNetwork.powered(c.block().getLevel(),c.pos())){notice(player,"The monitor needs wall power and coax; the "+c.system().label+" needs wall power.");return;}
  if(!c.system().controller(player)){notice(player,"Hold the "+c.system().label+" controller in either hand to play.");return;}
  var console=c;if(RetroConsoles.baseGameBusy(c)||map(player.getServer()).values().stream().anyMatch(s->s.identity.equals(console.identity())&&!s.player.equals(player.getUUID()))){notice(player,"This "+c.system().label+" is being used by another player.");return;}
  var existing=map(player.getServer()).get(player.getUUID());if(existing!=null&&existing.identity.equals(c.identity())&&existing.monitor.equals(e.getPos())&&existing.last[0]==player.getServer().getTickCount()&&valid(player,existing))return;
  close(player.getServer(),player.getUUID(),"");
  var s=new Session(player.getUUID(),UUID.randomUUID(),player.serverLevel(),e.getPos().immutable(),c.pos().immutable(),c.identity(),RetroItems.hash(c.cartridge()),c.system(),new long[]{player.getServer().getTickCount()});
  map(player.getServer()).put(player.getUUID(),s);
  PacketDistributor.sendToPlayer(player,new RetroPackets.Open(s.monitor,s.token,RetroWorld.get(player.getServer()).id,s.identity,s.hash,RetroItems.title(c.cartridge()),s.system));
 }
 public static boolean valid(ServerPlayer p,Session s){
  if(!p.isAlive()||p.isSpectator()||p.serverLevel()!=s.level||!s.system.controller(p)||p.distanceToSqr(s.monitor.getCenter())>64||!s.level.hasChunkAt(s.console)||!s.level.hasChunkAt(s.monitor))return false;
  var c=RetroConsoles.at(s.level,s.console);if(c==null||c.system()!=s.system||!Objects.equals(c.identity(),s.identity)||!RetroItems.hasGame(c.cartridge())||RetroItems.system(c.cartridge())!=s.system||!s.hash.equals(RetroItems.hash(c.cartridge())))return false;
  var m=RetroConsoles.monitor(c);return m!=null&&m.getBlockPos().equals(s.monitor)&&PowerNetwork.powered(s.level,s.console)&&PowerNetwork.powered(s.level,s.monitor)&&!RetroConsoles.baseGameBusy(c);
 }
 public static void pulse(ServerPlayer p,RetroPackets.Pulse m){var s=map(p.getServer()).get(p.getUUID());if(s==null||!s.token.equals(m.session()))return;if(m.close()||!valid(p,s)){close(p.getServer(),p.getUUID(),"");return;}s.last[0]=p.getServer().getTickCount();}
 public static void releaseConsole(MinecraftServer server,UUID id,String why){if(server==null||id==null)return;for(var s:List.copyOf(map(server).values()))if(s.identity.equals(id))close(server,s.player,why);}
 public static void close(MinecraftServer server,UUID player,String why){var s=map(server).remove(player);var p=server.getPlayerList().getPlayer(player);if(s!=null&&p!=null)PacketDistributor.sendToPlayer(p,new RetroPackets.Close(s.token,why));}
 @SubscribeEvent public static void tick(ServerTickEvent.Post e){var server=e.getServer();if(server.getTickCount()%10!=0)return;for(var s:List.copyOf(map(server).values())){var p=server.getPlayerList().getPlayer(s.player);if(p==null||!valid(p,s)||server.getTickCount()-s.last[0]>100)close(server,s.player,"Keep the matching controller in hand and stay near the powered monitor.");}}
 @SubscribeEvent public static void stopped(ServerStoppedEvent e){sessions.remove(e.getServer());}
}
