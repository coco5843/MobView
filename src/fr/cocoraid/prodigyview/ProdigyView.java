package fr.cocoraid.prodigyview;

import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Created by cocoraid on 20/02/2017.
 */
public class ProdigyView extends JavaPlugin implements Listener {


    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this,this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();

        viewers.keySet().forEach(cur -> reset(Bukkit.getPlayer(cur)));
    }

    private List<String> names = new ArrayList<>(Arrays.asList("creeper","enderman","spider"));

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;

            if(command.getName().equalsIgnoreCase("prodigyview") || command.getName().equalsIgnoreCase("pv")) {



                if (args.length == 0) {
                    p.sendMessage("§b/pv <entitytype>");
                    p.sendMessage("§b/pv <entitytype> <playername>");
                    p.sendMessage("§b/pv reset");
                    p.sendMessage("§b/pv reset <playername>");
                    p.sendMessage("§bTypes: §2creeper §5enderman §4spider");

                } else if (args.length == 1) {
                    if(args[0].equalsIgnoreCase("reset")) {
                        if(viewers.containsKey(p.getUniqueId())) {
                            viewers.remove(p.getUniqueId());
                            reset(p);
                        } else {
                            p.sendMessage("§cYou do not have any view");
                        }
                    }
                   else if(names.stream().filter(n -> n.equals(args[0])).findFirst().isPresent()) {
                        if (p.hasPermission("prodigyview.play." + args[0])) {
                            playView(p, EntityType.valueOf(args[0].toUpperCase()));
                        } else {
                            p.sendMessage("§cYou do not have the permission to do this");
                        }
                    } else {
                        p.sendMessage("§b/pv <entitytype>");
                        p.sendMessage("§b/pv <entitytype> <playername>");
                        p.sendMessage("§b/pv reset");
                        p.sendMessage("§b/pv reset <playername>");
                        p.sendMessage("§bTypes: §2creeper §5enderman §4spider");
                    }
                } else if (args.length == 2) {
                    if(args[0].equalsIgnoreCase("reset")) {
                        if (p.hasPermission("prodigyview.reset.otherplayer")) {
                            if (Bukkit.getPlayer(args[1]) != null && Bukkit.getPlayer(args[1]).isOnline()) {
                                if (viewers.containsKey(Bukkit.getPlayer(args[1]).getUniqueId())) {
                                    viewers.remove(Bukkit.getPlayer(args[1]).getUniqueId());
                                    reset(Bukkit.getPlayer(args[1]));
                                } else {
                                    p.sendMessage("§cThis player does not have any view");
                                }
                            } else {
                                p.sendMessage("§cThe player§4 " + args[1] + " §cis not online");
                            }
                        } else {
                            p.sendMessage("§cYou do not have the permission to do this");
                        }
                    } else if(names.stream().filter(n -> n.equals(args[0])).findFirst().isPresent()) {
                        if (p.hasPermission("prodigyview.play.otherplayer." + args[0])) {
                            if (Bukkit.getPlayer(args[1]) != null && Bukkit.getPlayer(args[1]).isOnline()) {
                                playView(Bukkit.getPlayer(args[1]), EntityType.valueOf(args[0].toUpperCase()));
                            } else {
                                p.sendMessage("§cThe player§4 " + args[1] + " §cis not online");
                            }
                        } else {
                            p.sendMessage("§cYou do not have the permission to do this");
                        }
                    } else {
                        p.sendMessage("§b/pv <entitytype>");
                        p.sendMessage("§b/pv <entitytype> <playername>");
                        p.sendMessage("§b/pv reset");
                        p.sendMessage("§b/pv reset <playername>");
                        p.sendMessage("§bTypes: §2creeper §5enderman §4spider");
                    }
                } else {
                    p.sendMessage("§b/pv <entitytype>");
                    p.sendMessage("§b/pv <entitytype> <playername>");
                    p.sendMessage("§b/pv reset");
                    p.sendMessage("§b/pv reset <playername>");
                    p.sendMessage("§bTypes: §2creeper §5enderman §4spider");
                }
            }
        }
        return false;
    }


    private List<UUID> list = new ArrayList<>();
    private Map<UUID, EntityType> viewers = new HashMap<>();

    @EventHandler
    public void quitEvent(PlayerQuitEvent e) {
        if(viewers.containsKey(e.getPlayer().getUniqueId()))
            viewers.remove(e.getPlayer().getUniqueId());
    }


    @EventHandler
    public void deathEvent(PlayerDeathEvent e) {
        if(list.contains(e.getEntity().getUniqueId())) {
            e.setDeathMessage("");
            e.setKeepInventory(true);
            e.setKeepLevel(true);
            e.setDroppedExp(0);
            list.remove(e.getEntity().getUniqueId());
        }
    }

    private static Reflection.MethodInvoker getHandleMethod = Reflection.getMethod("{obc}.entity.CraftEntity", "getHandle");
    private static Class<?> packetCameraClass = Reflection.getMinecraftClass("PacketPlayOutCamera");


    private static Reflection.ConstructorInvoker cameraConstructor = Reflection.getConstructor(packetCameraClass, Reflection.getMinecraftClass("Entity"));
    private static Reflection.ConstructorInvoker cameravoidConstructor = Reflection.getConstructor(packetCameraClass);
    private static Reflection.FieldAccessor<?> entityID = Reflection.getField(packetCameraClass, int.class,0);
    private static Reflection.FieldAccessor<?> playerConnectionField = Reflection.getField("{nms}.EntityPlayer", "playerConnection", Object.class);
    private static Reflection.MethodInvoker getHandlePlayerMethod = Reflection.getMethod("{obc}.entity.CraftPlayer", "getHandle");
    private static Reflection.MethodInvoker sendPacket = Reflection.getMethod("{nms}.PlayerConnection","sendPacket", Reflection.getMinecraftClass("Packet"));

    private void playView(Player p, EntityType type) {
        if(viewers.containsKey(p.getUniqueId()) && type.equals(viewers.get(p.getUniqueId()))) {
            viewers.remove(p.getUniqueId());
            reset(p);
            return;
        }

        viewers.put(p.getUniqueId(),type);
        LivingEntity en = (LivingEntity) p.getWorld().spawnEntity(p.getLocation().clone().add(0, 20,0),type);
        en.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,20*5,1,false,false));
        Object packet = cameraConstructor.invoke(getHandleMethod.invoke(en));
        en.remove();
        sendPacket.invoke(playerConnectionField.get(getHandlePlayerMethod.invoke(p)), packet);
        Location l = p.getLocation();
        list.add(p.getUniqueId());
        p.setHealth(0);
        PlayerDeathEvent e = new PlayerDeathEvent(p,new ArrayList<>(),0,"");
        getServer().getPluginManager().callEvent(e);
        p.spigot().respawn();
        p.teleport(l);

    }

    private void reset(Player p) {

        LivingEntity en = (LivingEntity) p.getWorld().spawnEntity(p.getLocation().clone().add(0, 20,0),EntityType.PIG);
        en.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,20*5,1,false,false));
        Object first = cameraConstructor.invoke(getHandleMethod.invoke(en));
        sendPacket.invoke(playerConnectionField.get(getHandlePlayerMethod.invoke(p)), first);

        Object packet = cameravoidConstructor.invoke();
        entityID.set(packet, Reflection.getField(Reflection.getMinecraftClass("Entity"), "id", int.class).get(getHandlePlayerMethod.invoke(p)));
        sendPacket.invoke(playerConnectionField.get(getHandlePlayerMethod.invoke(p)), packet);

        en.remove();


    }



}
