package dev.oxion;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.world.Heightmap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

/** Oxion DLC - personal Fabric client for Minecraft 1.21.4. RIGHT SHIFT = ClickGUI. */
public class OxionClient implements ClientModInitializer {
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final List<Module> MODULES = new ArrayList<>();
    private static KeyBinding menuKey;
    static int ACCENT = 0xFF9B5CFF;
    static double prevX, prevZ, bps;

    public enum Category { COMBAT, MOVEMENT, RENDER, PLAYER, MISC }

    // ---------------- Settings ----------------
    public static class Setting {
        final String name; double value; final double min, max; final boolean bool; final String[] opts;
        Setting(String name, double value, double min, double max) {
            this.name = name; this.value = value; this.min = min; this.max = max; this.bool = false; this.opts = null;
        }
        Setting(String name, boolean on) {
            this.name = name; this.value = on ? 1 : 0; this.min = 0; this.max = 1; this.bool = true; this.opts = null;
        }
        Setting(String name, int idx, String... opts) {
            this.name = name; this.value = idx; this.min = 0; this.max = opts.length - 1; this.bool = false; this.opts = opts;
        }
        boolean isOpt() { return opts != null; }
        String optName() { return opts[(int) Math.max(0, Math.min(opts.length - 1, Math.round(value)))]; }
        void cycle() { value = (((int) Math.round(value)) + 1) % opts.length; }
        double d() { return value; }
        float f() { return (float) value; }
        boolean on() { return value > 0.5; }
        void setFromRatio(double r) {
            double v = min + (max - min) * MathHelper.clamp(r, 0, 1);
            value = Math.round(v * 100.0) / 100.0;
        }
    }

    @Override
    public void onInitializeClient() {
        MODULES.add(new TriggerBot());
        MODULES.add(new AimAssist());
        MODULES.add(new KillAura());
        MODULES.add(new AutoClicker());
        MODULES.add(new Sprint());
        MODULES.add(new Speed());
        MODULES.add(new BHop());
        MODULES.add(new Fly());
        MODULES.add(new Glide());
        MODULES.add(new Spider());
        MODULES.add(new Jesus());
        MODULES.add(new NoFall());
        MODULES.add(new Step());
        MODULES.add(new Fullbright());
        MODULES.add(new ESP());
        MODULES.add(new BoxESP());
        MODULES.add(new Tracers());
        MODULES.add(new BlockESP());
        MODULES.add(new Rain());
        MODULES.add(new Snow());
        MODULES.add(new WetGround());
        MODULES.add(new Theme());
        MODULES.add(new HUD());
        MODULES.add(new AutoTotem());
        MODULES.add(new Scaffold());
        MODULES.add(new AirJump());
        MODULES.add(new AutoSneak());
        MODULES.add(new AntiAFK());
        MODULES.add(new AutoRespawn());
        MODULES.add(new AutoWalk());
        MODULES.add(new AutoGapple());
        MODULES.add(new AutoBlock());
        MODULES.add(new BowAimbot());
        MODULES.add(new AutoWeapon());
        MODULES.add(new FastBow());
        MODULES.add(new LongJump());
        MODULES.add(new HighJump());
        MODULES.add(new Strafe());
        MODULES.add(new Jetpack());
        MODULES.add(new AutoJump());
        MODULES.add(new SafeWalk());
        MODULES.add(new Parkour());
        MODULES.add(new AntiVoid());
        MODULES.add(new ElytraFly());
        MODULES.add(new FastClimb());
        MODULES.add(new Dolphin());
        MODULES.add(new Hover());
        MODULES.add(new ReverseStep());
        MODULES.add(new Zoom());
        MODULES.add(new FOVChanger());
        MODULES.add(new Crosshair());
        MODULES.add(new Radar());
        MODULES.add(new Keystrokes());
        MODULES.add(new Compass());
        MODULES.add(new Speedometer());
        MODULES.add(new AutoEat());
        MODULES.add(new AutoArmor());
        MODULES.add(new Derp());
        MODULES.add(new Spin());
        MODULES.add(new Freeze());
        MODULES.add(new ChestStealer());
        MODULES.add(new AutoFish());
        MODULES.add(new RightClicker());
        MODULES.add(new AutoMine());
        MODULES.add(new AutoLog());
        MODULES.add(new Panic());

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Oxion DLC Menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "Oxion DLC"));
        for (Module m : MODULES) {
            m.key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "Toggle " + m.name, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "Oxion DLC"));
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) if (client.player != null) client.setScreen(new ClickGui());
            if (client.player == null || client.world == null) return;
            tickCount++;
            if (client.targetedEntity instanceof LivingEntity te && te != client.player && te.isAlive()) { lastTarget = te; lastTargetTick = tickCount; }
            if (client.options.attackKey.isPressed()) {
                LivingEntity n = nearest(6, 60f, true, true);
                if (n != null) { lastTarget = n; lastTargetTick = tickCount; }
            }
            if (lastTarget != null && (tickCount - lastTargetTick > 60 || !lastTarget.isAlive() || client.player.distanceTo(lastTarget) > 12)) lastTarget = null;
            bps = Math.hypot(client.player.getX() - prevX, client.player.getZ() - prevZ) * 20;
            prevX = client.player.getX(); prevZ = client.player.getZ();
            for (Module m : MODULES) {
                while (m.key.wasPressed()) m.toggle();
                if (m.enabled) m.onTick();
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(OxionClient::renderWorld);

        loadConfig();
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> saveConfig());

        HudRenderCallback.EVENT.register((ctx, tick) -> {
            if (mc.player == null || mc.options.hudHidden) return;
            if (find("HUD") instanceof HUD h && h.enabled) renderHud(ctx, h);
            int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
            for (Module m : MODULES) if (m.enabled) m.onHud(ctx, sw, sh);
        });
    }

    static Module find(String n) {
        for (Module m : MODULES) if (m.name.equals(n)) return m;
        return null;
    }

    // ---------------- Helpers ----------------
    static boolean validTarget(Entity e, boolean players, boolean mobs) {
        if (!(e instanceof LivingEntity le) || e == mc.player || !le.isAlive()) return false;
        return le instanceof PlayerEntity ? players : mobs;
    }

    static LivingEntity nearest(double range, float fov, boolean players, boolean mobs) {
        if (mc.player == null || mc.world == null) return null;
        return mc.world.getEntitiesByClass(LivingEntity.class,
                        mc.player.getBoundingBox().expand(range), e -> validTarget(e, players, mobs)).stream()
                .filter(e -> mc.player.distanceTo(e) <= range)
                .filter(e -> Math.abs(yawDelta(e)) <= fov)
                .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
                .orElse(null);
    }

    static float[] rotsTo(Entity e) {
        Vec3d d = e.getEyePos().subtract(mc.player.getEyePos());
        float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90f);
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        return new float[]{yaw, MathHelper.clamp(pitch, -90f, 90f)};
    }

    static float[] rotsToPoint(Vec3d p) {
        Vec3d d = p.subtract(mc.player.getEyePos());
        float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90f);
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        return new float[]{yaw, MathHelper.clamp(pitch, -90f, 90f)};
    }

    static boolean holdingWeapon() {
        ItemStack st = mc.player.getMainHandStack();
        return st.isIn(ItemTags.SWORDS) || st.isIn(ItemTags.AXES);
    }

    static float yawDelta(Entity e) { return MathHelper.wrapDegrees(rotsTo(e)[0] - mc.player.getYaw()); }

    static void attack(Entity e) {
        mc.interactionManager.attackEntity(mc.player, e);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    static boolean moving() {
        return mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
    }

    // ---------------- Module base ----------------
    public static abstract class Module {
        public final String name; public final Category category;
        public boolean enabled, expanded;
        public KeyBinding key;
        public final List<Setting> settings = new ArrayList<>();
        Module(String name, Category c) { this.name = name; this.category = c; }
        <T extends Setting> T add(T s) { settings.add(s); return s; }
        void onTick() {}
        void onHud(DrawContext ctx, int sw, int sh) {}
        void onEnable() {}
        void onDisable() {}
        void toggle() {
            enabled = !enabled;
            if (enabled) onEnable(); else onDisable();
            if (mc.player != null)
                mc.player.sendMessage(Text.literal("§5Oxion §f" + name + (enabled ? " §aON" : " §cOFF")), true);
        }
    }

    // ---------------- Combat ----------------
    static class TriggerBot extends Module {
        Setting mode = add(new Setting("Mode", 0, "Legit", "Normal", "Rage"));
        Setting cd = add(new Setting("Cooldown", 0.95, 0.5, 1.0));
        Setting delay = add(new Setting("Delay", 2, 0, 10));
        Setting range = add(new Setting("Range", 3.0, 2, 6));
        Setting pl = add(new Setting("Players", true)), mb = add(new Setting("Mobs", true));
        Setting weapon = add(new Setting("OnlyWeapon", false));
        Setting crits = add(new Setting("OnlyCrits", false));
        Setting aim = add(new Setting("RageAutoAim", true));
        int wait;
        TriggerBot() { super("TriggerBot", Category.COMBAT); }
        @Override void onTick() {
            if (mc.currentScreen != null) return;
            int m = (int) Math.round(mode.d());
            if (weapon.on() && !holdingWeapon()) return;
            if (crits.on() && (mc.player.isOnGround() || mc.player.fallDistance <= 0f)) return;
            double r = m == 0 ? Math.min(range.d(), 3.0) : range.d();
            Entity target = null;
            if (m == 2 && aim.on()) {
                LivingEntity t = nearest(r, 360f, pl.on(), mb.on());
                if (t != null && mc.player.canSee(t)) {
                    float[] rot = rotsTo(t);
                    mc.player.setYaw(rot[0]);
                    mc.player.setPitch(rot[1]);
                    target = t;
                }
            } else if (mc.crosshairTarget instanceof EntityHitResult ehr && validTarget(ehr.getEntity(), pl.on(), mb.on())
                    && mc.player.distanceTo(ehr.getEntity()) <= r) {
                target = ehr.getEntity();
            }
            if (target == null) { wait = 0; return; }
            if (mc.player.getAttackCooldownProgress(0.5f) < cd.f()) return;
            if (wait < (int) delay.d()) { wait++; return; }
            wait = 0;
            attack(target);
        }
    }

    static class AimAssist extends Module {
        Setting range = add(new Setting("Range", 4.2, 1, 6));
        Setting speed = add(new Setting("Speed", 0.25, 0.05, 1));
        Setting fov = add(new Setting("FOV", 90, 10, 360));
        Setting part = add(new Setting("Target", 0, "Head", "Chest", "Legs"));
        Setting vertical = add(new Setting("Vertical", true));
        Setting visible = add(new Setting("VisibleOnly", true));
        Setting sticky = add(new Setting("Sticky", false));
        Setting needClick = add(new Setting("OnlyOnClick", true));
        Setting weapon = add(new Setting("OnlyWeapon", false));
        Setting pl = add(new Setting("Players", true)), mb = add(new Setting("Mobs", true));
        LivingEntity stuck;
        AimAssist() { super("AimAssist", Category.COMBAT); }
        @Override void onTick() {
            if (mc.currentScreen != null || (needClick.on() && !mc.options.attackKey.isPressed())) return;
            if (weapon.on() && !holdingWeapon()) return;
            LivingEntity t = null;
            if (sticky.on() && stuck != null && stuck.isAlive() && mc.player.distanceTo(stuck) <= range.d()) t = stuck;
            if (t == null) t = nearest(range.d(), fov.f(), pl.on(), mb.on());
            if (t == null) { stuck = null; return; }
            if (visible.on() && !mc.player.canSee(t)) return;
            stuck = t;
            double frac = switch ((int) Math.round(part.d())) { case 0 -> 0.92; case 1 -> 0.6; default -> 0.2; };
            float[] r = rotsToPoint(new Vec3d(t.getX(), t.getBodyY(frac), t.getZ()));
            float dy = MathHelper.wrapDegrees(r[0] - mc.player.getYaw()), dp = r[1] - mc.player.getPitch();
            if (Math.abs(dy) < 1.0f && Math.abs(dp) < 1.0f) return;
            mc.player.setYaw(mc.player.getYaw() + dy * speed.f());
            if (vertical.on()) mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + dp * speed.f(), -90f, 90f));
        }
    }

    static class KillAura extends Module {
        Setting range = add(new Setting("Range", 4.2, 1, 6));
        Setting cd = add(new Setting("Cooldown", 0.92, 0.5, 1.0));
        Setting pl = add(new Setting("Players", true)), mb = add(new Setting("Mobs", true));
        KillAura() { super("KillAura", Category.COMBAT); }
        @Override void onTick() {
            LivingEntity t = nearest(range.d(), 360f, pl.on(), mb.on());
            if (t == null || mc.player.getAttackCooldownProgress(0.5f) < cd.f()) return;
            float[] r = rotsTo(t);
            mc.player.setYaw(r[0]); mc.player.setPitch(r[1]);
            attack(t);
        }
    }

    static class AutoClicker extends Module {
        Setting cps = add(new Setting("CPS", 12, 1, 20));
        int tick;
        AutoClicker() { super("AutoClicker", Category.COMBAT); }
        @Override void onTick() {
            if (mc.currentScreen != null || !mc.options.attackKey.isPressed()) return;
            if (++tick >= Math.max(1, Math.round(20 / cps.f()))) {
                tick = 0;
                if (mc.crosshairTarget instanceof EntityHitResult e) attack(e.getEntity());
                else mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    // ---------------- Movement ----------------
    static class Sprint extends Module {
        Sprint() { super("Sprint", Category.MOVEMENT); }
        @Override void onTick() {
            if (mc.options.forwardKey.isPressed() && !mc.player.isSneaking()
                    && mc.player.getHungerManager().getFoodLevel() > 6) mc.player.setSprinting(true);
        }
    }

    static class Speed extends Module {
        Setting mult = add(new Setting("Multiplier", 1.12, 1.0, 1.6));
        Speed() { super("Speed", Category.MOVEMENT); }
        @Override void onTick() {
            if (!moving() || !mc.player.isOnGround()) return;
            Vec3d v = mc.player.getVelocity();
            if (Math.hypot(v.x, v.z) < 0.6) mc.player.setVelocity(v.x * mult.d(), v.y, v.z * mult.d());
        }
    }

    static class BHop extends Module {
        BHop() { super("BHop", Category.MOVEMENT); }
        @Override void onTick() { if (moving() && mc.player.isOnGround()) mc.player.jump(); }
    }

    static class Fly extends Module {
        Setting speed = add(new Setting("Speed", 0.6, 0.1, 3));
        Fly() { super("Fly", Category.MOVEMENT); }
        @Override void onTick() {
            double yaw = Math.toRadians(mc.player.getYaw()), sp = speed.d();
            double f = (mc.options.forwardKey.isPressed() ? 1 : 0) - (mc.options.backKey.isPressed() ? 1 : 0);
            double s = (mc.options.leftKey.isPressed() ? 1 : 0) - (mc.options.rightKey.isPressed() ? 1 : 0);
            double x = (-Math.sin(yaw) * f + Math.cos(yaw) * s) * sp;
            double z = (Math.cos(yaw) * f + Math.sin(yaw) * s) * sp;
            double y = mc.options.jumpKey.isPressed() ? sp : mc.options.sneakKey.isPressed() ? -sp : 0;
            mc.player.setVelocity(x, y, z);
            mc.player.fallDistance = 0;
        }
    }

    static class Glide extends Module {
        Setting fall = add(new Setting("FallSpeed", 0.05, 0.01, 0.3));
        Glide() { super("Glide", Category.MOVEMENT); }
        @Override void onTick() {
            Vec3d v = mc.player.getVelocity();
            if (!mc.player.isOnGround() && v.y < -fall.d()) mc.player.setVelocity(v.x, -fall.d(), v.z);
        }
    }

    static class Spider extends Module {
        Spider() { super("Spider", Category.MOVEMENT); }
        @Override void onTick() {
            if (mc.player.horizontalCollision) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0.2, v.z);
            }
        }
    }

    static class Jesus extends Module {
        Jesus() { super("Jesus", Category.MOVEMENT); }
        @Override void onTick() {
            if (mc.player.isTouchingWater() && !mc.player.isSneaking()) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0.11, v.z);
            }
        }
    }

    static class NoFall extends Module {
        NoFall() { super("NoFall", Category.MOVEMENT); }
        @Override void onTick() {
            if (mc.player.fallDistance > 2.5f && mc.getNetworkHandler() != null)
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
        }
    }

    static class Step extends Module {
        Setting h = add(new Setting("Height", 1.0, 0.6, 2.5));
        Step() { super("Step", Category.MOVEMENT); }
        @Override void onTick() {
            var a = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
            if (a != null) a.setBaseValue(h.d());
        }
        @Override void onDisable() {
            if (mc.player == null) return;
            var a = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
            if (a != null) a.setBaseValue(0.6);
        }
    }

    // ---------------- Render ----------------
    static class Fullbright extends Module {
        Fullbright() { super("Fullbright", Category.RENDER); }
        @Override void onTick() {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000, 0, false, false, false));
        }
        @Override void onDisable() { if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION); }
    }

    static class ESP extends Module {
        Setting pl = add(new Setting("Players", true)), mb = add(new Setting("Mobs", true)),
                items = add(new Setting("Items", false));
        ESP() { super("ESP", Category.RENDER); }
        @Override void onTick() {
            for (Entity e : mc.world.getEntities()) {
                if (validTarget(e, pl.on(), mb.on())) e.setGlowing(true);
                else if (items.on() && e instanceof ItemEntity) e.setGlowing(true);
            }
        }
        @Override void onDisable() {
            if (mc.world == null) return;
            for (Entity e : mc.world.getEntities())
                if ((e instanceof LivingEntity && e != mc.player) || e instanceof ItemEntity) e.setGlowing(false);
        }
    }

    static class BoxESP extends Module {
        Setting pl = add(new Setting("Players", true)), mb = add(new Setting("Mobs", true));
        Setting range = add(new Setting("Range", 64, 8, 128));
        BoxESP() { super("BoxESP", Category.RENDER); }
    }

    static class BlockESP extends Module {
        Setting radius = add(new Setting("Radius", 16, 6, 32));
        Setting scanTicks = add(new Setting("ScanTicks", 20, 5, 60));
        Setting diamond = add(new Setting("Diamond", true)), debris = add(new Setting("AncientDebris", true)),
                gold = add(new Setting("Gold", true)), iron = add(new Setting("Iron", false)),
                emerald = add(new Setting("Emerald", true)), chests = add(new Setting("Chests", true)),
                spawners = add(new Setting("Spawners", true));
        final List<BlockPos> found = new ArrayList<>();
        final List<float[]> colors = new ArrayList<>();
        int t;
        BlockESP() { super("BlockESP", Category.RENDER); }
        float[] colorFor(Block b) {
            if (diamond.on() && (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE)) return new float[]{0.3f, 0.9f, 1f};
            if (debris.on() && b == Blocks.ANCIENT_DEBRIS) return new float[]{0.6f, 0.3f, 0.2f};
            if (gold.on() && (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE || b == Blocks.NETHER_GOLD_ORE)) return new float[]{1f, 0.85f, 0.2f};
            if (iron.on() && (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE)) return new float[]{0.85f, 0.7f, 0.6f};
            if (emerald.on() && (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE)) return new float[]{0.2f, 1f, 0.4f};
            if (chests.on() && (b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST || b == Blocks.ENDER_CHEST || b == Blocks.BARREL)) return new float[]{1f, 0.6f, 0.2f};
            if (spawners.on() && b == Blocks.SPAWNER) return new float[]{0.9f, 0.2f, 0.9f};
            return null;
        }
        @Override void onTick() {
            if (++t < (int) scanTicks.d()) return;
            t = 0;
            found.clear();
            colors.clear();
            int r = (int) radius.d();
            BlockPos c = mc.player.getBlockPos();
            BlockPos.Mutable m = new BlockPos.Mutable();
            for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
                m.set(c.getX() + x, c.getY() + y, c.getZ() + z);
                float[] col = colorFor(mc.world.getBlockState(m).getBlock());
                if (col != null) {
                    found.add(m.toImmutable());
                    colors.add(col);
                    if (found.size() >= 400) return;
                }
            }
        }
        @Override void onDisable() { found.clear(); colors.clear(); }
    }

    /** Client-side rain made of particles. */
    static class Rain extends Module {
        Setting density = add(new Setting("Density", 20, 1, 80));
        Setting radius = add(new Setting("Radius", 14, 6, 32));
        Setting height = add(new Setting("Height", 8, 4, 16));
        Setting splash = add(new Setting("GroundSplash", true));
        Rain() { super("Rain", Category.RENDER); }
        @Override void onTick() {
            double r = radius.d();
            for (int i = 0; i < (int) density.d(); i++) {
                double x = mc.player.getX() + (Math.random() - 0.5) * 2 * r, z = mc.player.getZ() + (Math.random() - 0.5) * 2 * r;
                int gy = mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
                double y = Math.min(mc.player.getY() + height.d() + Math.random() * 3, gy + height.d() + Math.random() * 3);
                mc.world.addParticle(ParticleTypes.FALLING_WATER, x, y, z, 0, -0.3, 0);
                if (splash.on() && Math.random() < 0.5) mc.world.addParticle(ParticleTypes.RAIN, x, gy + 0.1, z, 0, 0, 0);
            }
        }
    }

    /** Client-side snowfall plus a sparkling snow layer on the ground (made of particles). */
    static class Snow extends Module {
        Setting density = add(new Setting("Density", 25, 1, 100));
        Setting radius = add(new Setting("Radius", 14, 6, 32));
        Setting height = add(new Setting("Height", 8, 4, 16));
        Setting wind = add(new Setting("Wind", 0.02, 0, 0.12));
        Setting ground = add(new Setting("GroundSnow", true));
        Snow() { super("Snow", Category.RENDER); }
        @Override void onTick() {
            double r = radius.d();
            for (int i = 0; i < (int) density.d(); i++) {
                double x = mc.player.getX() + (Math.random() - 0.5) * 2 * r, z = mc.player.getZ() + (Math.random() - 0.5) * 2 * r;
                int gy = mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
                double y = Math.min(mc.player.getY() + height.d() + Math.random() * 4, gy + height.d() + Math.random() * 4);
                mc.world.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, (Math.random() - 0.3) * wind.d(), -0.06 - Math.random() * 0.04, (Math.random() - 0.5) * wind.d());
                if (ground.on()) mc.world.addParticle(ParticleTypes.SNOWFLAKE, x, gy + 0.05, z, 0, 0, 0);
            }
        }
    }

    /** Wet-ground look: ripples and splashes on the ground around the player. */
    static class WetGround extends Module {
        Setting density = add(new Setting("Density", 14, 1, 60));
        Setting radius = add(new Setting("Radius", 12, 4, 28));
        WetGround() { super("WetGround", Category.RENDER); }
        @Override void onTick() {
            double r = radius.d();
            for (int i = 0; i < (int) density.d(); i++) {
                double x = mc.player.getX() + (Math.random() - 0.5) * 2 * r, z = mc.player.getZ() + (Math.random() - 0.5) * 2 * r;
                int gy = mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
                mc.world.addParticle(ParticleTypes.RAIN, x, gy + 0.05, z, 0, 0, 0);
            }
        }
    }

    static class Tracers extends Module {
        Setting pl = add(new Setting("Players", true)), mb = add(new Setting("Mobs", true));
        Setting range = add(new Setting("Range", 64, 8, 128));
        Tracers() { super("Tracers", Category.RENDER); }
    }

    /** Draws boxes and lines in the world (depth-tested). */
    static void renderWorld(WorldRenderContext ctx) {
        if (mc.player == null || mc.world == null) return;
        if (!(find("BoxESP") instanceof BoxESP box) || !(find("Tracers") instanceof Tracers tr)) return;
        BlockESP be = find("BlockESP") instanceof BlockESP bb ? bb : null;
        boolean blocks = be != null && be.enabled;
        if (!box.enabled && !tr.enabled && !blocks) return;
        MatrixStack ms = ctx.matrixStack();
        if (ms == null || ctx.consumers() == null) return;

        Vec3d cam = ctx.camera().getPos();
        Vec3d start = cam.add(Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw()).multiply(0.6));
        ms.push();
        ms.translate(-cam.x, -cam.y, -cam.z);
        VertexConsumer vc = ctx.consumers().getBuffer(RenderLayer.getLines());

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || e == mc.player || !le.isAlive()) continue;
            boolean isPlayer = e instanceof PlayerEntity;
            float r = isPlayer ? 0.61f : 1.0f, g = isPlayer ? 0.36f : 0.6f, b = isPlayer ? 1.0f : 0.2f;
            double dist = mc.player.distanceTo(e);

            if (box.enabled && (isPlayer ? box.pl.on() : box.mb.on()) && dist <= box.range.d()) {
                VertexRendering.drawBox(ms, vc, e.getBoundingBox(), r, g, b, 1.0f);
            }
            if (tr.enabled && (isPlayer ? tr.pl.on() : tr.mb.on()) && dist <= tr.range.d()) {
                Vec3d end = e.getBoundingBox().getCenter();
                Vec3d d = end.subtract(start).normalize();
                MatrixStack.Entry en = ms.peek();
                int ri = (int) (r * 255), gi = (int) (g * 255), bi = (int) (b * 255);
                vc.vertex(en, (float) start.x, (float) start.y, (float) start.z)
                        .color(ri, gi, bi, 255).normal(en, (float) d.x, (float) d.y, (float) d.z);
                vc.vertex(en, (float) end.x, (float) end.y, (float) end.z)
                        .color(ri, gi, bi, 255).normal(en, (float) d.x, (float) d.y, (float) d.z);
            }
        }
        if (blocks) {
            for (int k = 0; k < be.found.size(); k++) {
                float[] c = be.colors.get(k);
                VertexRendering.drawBox(ms, vc, new net.minecraft.util.math.Box(be.found.get(k)), c[0], c[1], c[2], 1.0f);
            }
        }
        ms.pop();
    }

    static class Theme extends Module {
        static final int[] PAL = {0xFF9B5CFF, 0xFF4DA6FF, 0xFF3DDC84, 0xFFFF5C7A, 0xFFFFB347, 0xFFFFFFFF};
        Setting color = add(new Setting("Color", 0, 0, 5));
        Setting rainbow = add(new Setting("Rainbow", false));
        Theme() { super("Theme", Category.RENDER); enabled = true; }
        @Override void onTick() {
            if (rainbow.on()) ACCENT = 0xFF000000 | MathHelper.hsvToRgb((System.currentTimeMillis() % 5000) / 5000f, 0.6f, 1f);
            else ACCENT = PAL[Math.min(5, (int) Math.floor(color.d() + 0.5))];
        }
    }

    static class AutoTotem extends Module {
        int cd;
        AutoTotem() { super("AutoTotem", Category.COMBAT); }
        @Override void onTick() {
            if (cd > 0) { cd--; return; }
            if (mc.currentScreen != null || mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
            var inv = mc.player.getInventory();
            for (int i = 0; i < 36; i++) {
                if (inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                    int slot = i < 9 ? 36 + i : i;
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, mc.player);
                    cd = 5;
                    return;
                }
            }
        }
    }

    static class Scaffold extends Module {
        Scaffold() { super("Scaffold", Category.MOVEMENT); }
        @Override void onTick() {
            if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)) return;
            BlockPos below = mc.player.getBlockPos().down();
            if (!mc.world.getBlockState(below).isReplaceable()) return;
            for (Direction d : Direction.values()) {
                if (d == Direction.UP) continue;
                BlockPos n = below.offset(d);
                if (!mc.world.getBlockState(n).isReplaceable()) {
                    Direction face = d.getOpposite();
                    Vec3d hit = Vec3d.ofCenter(n).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hit, face, n, false));
                    mc.player.swingHand(Hand.MAIN_HAND);
                    return;
                }
            }
        }
    }

    static class AirJump extends Module {
        int cd;
        AirJump() { super("AirJump", Category.MOVEMENT); }
        @Override void onTick() {
            if (cd > 0) cd--;
            if (mc.options.jumpKey.isPressed() && !mc.player.isOnGround() && cd == 0) { mc.player.jump(); cd = 6; }
        }
    }

    static class AutoSneak extends Module {
        AutoSneak() { super("AutoSneak", Category.PLAYER); }
        @Override void onTick() { mc.options.sneakKey.setPressed(true); }
        @Override void onDisable() { mc.options.sneakKey.setPressed(false); }
    }

    static class AntiAFK extends Module {
        int t;
        AntiAFK() { super("AntiAFK", Category.PLAYER); }
        @Override void onTick() {
            if (++t >= 600) { t = 0; mc.player.jump(); mc.player.swingHand(Hand.MAIN_HAND); }
        }
    }

    static class HUD extends Module {
        Setting opacity = add(new Setting("Opacity", 0.75, 0.2, 1.0));
        Setting scale = add(new Setting("Scale", 1.0, 0.6, 1.6));
        final List<HudElem> elems = new ArrayList<>();
        HudElem el(String name, boolean on, float fx, float fy) {
            HudElem e = new HudElem(name, add(new Setting(name, on)), fx, fy);
            elems.add(e);
            return e;
        }
        HUD() {
            super("HUD", Category.RENDER);
            enabled = true;
            el("Watermark", true, 0.40f, 0.01f);
            el("TargetHUD", true, 0.56f, 0.42f);
            el("Potions", true, 0.14f, 0.56f);
            el("Cooldowns", true, 0.70f, 0.56f);
            el("Keybinds", true, 0.43f, 0.62f);
            el("Coords", true, 0.005f, 0.94f);
            el("Armor", true, 0.95f, 0.15f);
            el("AttackBar", false, 0.45f, 0.53f);
            el("ArrayList", false, 0.005f, 0.01f);
        }
    }

    // ---------------- Player ----------------
    static class AutoRespawn extends Module {
        AutoRespawn() { super("AutoRespawn", Category.PLAYER); }
        @Override void onTick() { if (mc.player.isDead()) mc.player.requestRespawn(); }
    }

    static class AutoWalk extends Module {
        AutoWalk() { super("AutoWalk", Category.PLAYER); }
        @Override void onTick() { mc.options.forwardKey.setPressed(true); }
        @Override void onDisable() { mc.options.forwardKey.setPressed(false); }
    }



    // ================= EXTRA MODULES =================
    static void selectSlot(int i) { mc.player.getInventory().selectedSlot = i; }

    static int findHotbar(java.util.function.Predicate<ItemStack> pr) {
        for (int i = 0; i < 9; i++) if (pr.test(mc.player.getInventory().getStack(i))) return i;
        return -1;
    }

    static Vec3d moveDir() {
        double yaw = Math.toRadians(mc.player.getYaw());
        double f = (mc.options.forwardKey.isPressed() ? 1 : 0) - (mc.options.backKey.isPressed() ? 1 : 0);
        double s = (mc.options.leftKey.isPressed() ? 1 : 0) - (mc.options.rightKey.isPressed() ? 1 : 0);
        double x = -Math.sin(yaw) * f + Math.cos(yaw) * s, z = Math.cos(yaw) * f + Math.sin(yaw) * s;
        double len = Math.hypot(x, z);
        return len < 1e-4 ? Vec3d.ZERO : new Vec3d(x / len, 0, z / len);
    }

    // ---- Combat ----
    static class AutoGapple extends Module {
        Setting hp = add(new Setting("HP", 10, 1, 20));
        boolean eating;
        AutoGapple() { super("AutoGapple", Category.COMBAT); }
        @Override void onTick() {
            if (mc.player.getHealth() <= hp.f()) {
                int slot = findHotbar(st -> st.isOf(Items.GOLDEN_APPLE) || st.isOf(Items.ENCHANTED_GOLDEN_APPLE));
                if (slot >= 0) { selectSlot(slot); mc.options.useKey.setPressed(true); eating = true; return; }
            }
            if (eating) { mc.options.useKey.setPressed(false); eating = false; }
        }
        @Override void onDisable() { if (eating) mc.options.useKey.setPressed(false); eating = false; }
    }

    static class AutoBlock extends Module {
        Setting range = add(new Setting("Range", 4.5, 2, 6));
        boolean on;
        AutoBlock() { super("AutoBlock", Category.COMBAT); }
        @Override void onTick() {
            boolean shield = mc.player.getOffHandStack().isOf(Items.SHIELD) || mc.player.getMainHandStack().isOf(Items.SHIELD);
            if (shield && nearest(range.d(), 360f, true, true) != null) { mc.options.useKey.setPressed(true); on = true; }
            else if (on) { mc.options.useKey.setPressed(false); on = false; }
        }
        @Override void onDisable() { if (on) mc.options.useKey.setPressed(false); on = false; }
    }

    static class BowAimbot extends Module {
        Setting range = add(new Setting("Range", 40, 10, 80));
        Setting lead = add(new Setting("Drop", 0.08, 0, 0.3));
        BowAimbot() { super("BowAimbot", Category.COMBAT); }
        @Override void onTick() {
            if (!mc.player.isUsingItem() || !mc.player.getActiveItem().isOf(Items.BOW)) return;
            LivingEntity t = nearest(range.d(), 360f, true, true);
            if (t == null) return;
            float[] r = rotsTo(t);
            mc.player.setYaw(r[0]);
            mc.player.setPitch(MathHelper.clamp(r[1] - (float) (mc.player.distanceTo(t) * lead.d()), -90f, 90f));
        }
    }

    static class AutoWeapon extends Module {
        AutoWeapon() { super("AutoWeapon", Category.COMBAT); }
        @Override void onTick() {
            if (mc.currentScreen != null || !mc.options.attackKey.isPressed() || !(mc.crosshairTarget instanceof EntityHitResult)) return;
            int slot = findHotbar(st -> st.isIn(ItemTags.SWORDS));
            if (slot >= 0) selectSlot(slot);
        }
    }

    static class FastBow extends Module {
        Setting ticks = add(new Setting("Ticks", 3, 1, 20));
        FastBow() { super("FastBow", Category.COMBAT); }
        @Override void onTick() {
            if (mc.player.isUsingItem() && mc.player.getActiveItem().isOf(Items.BOW) && mc.player.getItemUseTime() >= ticks.f())
                mc.interactionManager.stopUsingItem(mc.player);
        }
    }

    // ---- Movement ----
    static class LongJump extends Module {
        Setting boost = add(new Setting("Boost", 1.6, 1.0, 3.0));
        LongJump() { super("LongJump", Category.MOVEMENT); }
        @Override void onTick() {
            if (mc.options.jumpKey.isPressed() && mc.player.isOnGround() && moving()) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x * boost.d(), v.y, v.z * boost.d());
            }
        }
    }

    static class HighJump extends Module {
        Setting power = add(new Setting("Power", 0.8, 0.45, 2.0));
        HighJump() { super("HighJump", Category.MOVEMENT); }
        @Override void onTick() {
            if (mc.options.jumpKey.isPressed() && mc.player.isOnGround()) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, power.d(), v.z);
            }
        }
    }

    static class Strafe extends Module {
        Strafe() { super("Strafe", Category.MOVEMENT); }
        @Override void onTick() {
            if (!moving() || mc.player.isOnGround()) return;
            Vec3d v = mc.player.getVelocity(), d = moveDir();
            double sp = Math.max(Math.hypot(v.x, v.z), 0.2);
            mc.player.setVelocity(d.x * sp, v.y, d.z * sp);
        }
    }

    static class Jetpack extends Module {
        Setting power = add(new Setting("Power", 0.35, 0.1, 1.0));
        Jetpack() { super("Jetpack", Category.MOVEMENT); }
        @Override void onTick() {
            if (mc.options.jumpKey.isPressed()) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, power.d(), v.z);
                mc.player.fallDistance = 0;
            }
        }
    }

    static class AutoJump extends Module {
        AutoJump() { super("AutoJump", Category.MOVEMENT); }
        @Override void onTick() { if (mc.player.horizontalCollision && mc.player.isOnGround() && moving()) mc.player.jump(); }
    }

    static class SafeWalk extends Module {
        boolean active;
        SafeWalk() { super("SafeWalk", Category.MOVEMENT); }
        @Override void onTick() {
            Vec3d v = mc.player.getVelocity();
            BlockPos ahead = BlockPos.ofFloored(mc.player.getX() + v.x * 4, mc.player.getY() - 1, mc.player.getZ() + v.z * 4);
            boolean edge = mc.player.isOnGround() && moving() && mc.world.getBlockState(ahead).isAir();
            if (edge) { mc.options.sneakKey.setPressed(true); active = true; }
            else if (active) { mc.options.sneakKey.setPressed(false); active = false; }
        }
        @Override void onDisable() { if (active) mc.options.sneakKey.setPressed(false); active = false; }
    }

    static class Parkour extends Module {
        Parkour() { super("Parkour", Category.MOVEMENT); }
        @Override void onTick() {
            if (!mc.player.isOnGround() || mc.player.isSneaking() || !moving()) return;
            Vec3d v = mc.player.getVelocity();
            if (Math.hypot(v.x, v.z) < 0.05) return;
            BlockPos ahead = BlockPos.ofFloored(mc.player.getX() + v.x * 4, mc.player.getY() - 1, mc.player.getZ() + v.z * 4);
            if (mc.world.getBlockState(ahead).isAir()) mc.player.jump();
        }
    }

    static class AntiVoid extends Module {
        AntiVoid() { super("AntiVoid", Category.MOVEMENT); }
        @Override void onTick() {
            if (mc.player.getY() < mc.world.getBottomY() + 2) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0.8, v.z);
            }
        }
    }

    static class ElytraFly extends Module {
        Setting speed = add(new Setting("Speed", 1.2, 0.5, 3.0));
        ElytraFly() { super("ElytraFly", Category.MOVEMENT); }
        @Override void onTick() {
            if (!mc.player.isGliding()) return;
            if (mc.options.forwardKey.isPressed()) mc.player.setVelocity(mc.player.getRotationVector().multiply(speed.d()));
            else if (mc.options.sneakKey.isPressed()) mc.player.setVelocity(Vec3d.ZERO);
        }
    }

    static class FastClimb extends Module {
        FastClimb() { super("FastClimb", Category.MOVEMENT); }
        @Override void onTick() {
            if (mc.player.isClimbing() && (mc.options.jumpKey.isPressed() || mc.player.horizontalCollision)) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0.3, v.z);
            }
        }
    }

    static class Dolphin extends Module {
        Dolphin() { super("Dolphin", Category.MOVEMENT); }
        @Override void onTick() {
            if (mc.player.isTouchingWater() && moving()) {
                Vec3d v = mc.player.getVelocity();
                if (Math.hypot(v.x, v.z) < 0.5) mc.player.setVelocity(v.x * 1.12, v.y, v.z * 1.12);
            }
        }
    }

    static class Hover extends Module {
        Hover() { super("Hover", Category.MOVEMENT); }
        @Override void onTick() {
            if (!mc.player.isOnGround() && !mc.options.jumpKey.isPressed() && !mc.options.sneakKey.isPressed()) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0, v.z);
                mc.player.fallDistance = 0;
            }
        }
    }

    static class ReverseStep extends Module {
        ReverseStep() { super("ReverseStep", Category.MOVEMENT); }
        @Override void onTick() {
            Vec3d v = mc.player.getVelocity();
            if (!mc.player.isOnGround() && v.y < 0 && v.y > -0.6
                    && !mc.world.getBlockState(BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 1.2, mc.player.getZ())).isAir())
                mc.player.setVelocity(v.x, -1.0, v.z);
        }
    }

    // ---- Render ----
    static int origFov = -1;

    static void setFov(int f) {
        if (origFov < 0) origFov = mc.options.getFov().getValue();
        mc.options.getFov().setValue(f);
    }

    static void restoreFov() {
        if (origFov >= 0) { mc.options.getFov().setValue(origFov); origFov = -1; }
    }

    static class Zoom extends Module {
        Setting fov = add(new Setting("FOV", 30, 30, 60));
        Zoom() { super("Zoom", Category.RENDER); }
        @Override void onTick() { setFov((int) fov.d()); }
        @Override void onDisable() { restoreFov(); }
    }

    static class FOVChanger extends Module {
        Setting fov = add(new Setting("FOV", 100, 30, 110));
        FOVChanger() { super("FOVChanger", Category.RENDER); }
        @Override void onTick() { setFov((int) fov.d()); }
        @Override void onDisable() { restoreFov(); }
    }

    static class Crosshair extends Module {
        Setting size = add(new Setting("Size", 6, 2, 16)), gap = add(new Setting("Gap", 3, 0, 10)),
                thick = add(new Setting("Thickness", 1, 1, 3)), rainbow = add(new Setting("Rainbow", false));
        Crosshair() { super("Crosshair", Category.RENDER); }
        @Override void onHud(DrawContext ctx, int sw, int sh) {
            int cx = sw / 2, cy = sh / 2, g = (int) gap.d(), l = (int) size.d(), t = (int) thick.d(), h1 = t / 2;
            int c = rainbow.on() ? 0xFF000000 | MathHelper.hsvToRgb((System.currentTimeMillis() % 3000) / 3000f, 0.7f, 1f) : ACCENT;
            ctx.fill(cx - g - l, cy - h1, cx - g, cy - h1 + t, c);
            ctx.fill(cx + g, cy - h1, cx + g + l, cy - h1 + t, c);
            ctx.fill(cx - h1, cy - g - l, cx - h1 + t, cy - g, c);
            ctx.fill(cx - h1, cy + g, cx - h1 + t, cy + g + l, c);
        }
    }

    static class Radar extends Module {
        Setting size = add(new Setting("Size", 90, 60, 150)), range = add(new Setting("Range", 40, 10, 100));
        Setting pl = add(new Setting("Players", true)), mb = add(new Setting("Mobs", true));
        Radar() { super("Radar", Category.RENDER); }
        @Override void onHud(DrawContext ctx, int sw, int sh) {
            int sz = (int) size.d(), x = 8, y = sh / 2 - sz / 2, c = sz / 2;
            ctx.fill(x, y, x + sz, y + sz, 0xAA0E0E14);
            ctx.fill(x, y + c, x + sz, y + c + 1, 0x33FFFFFF);
            ctx.fill(x + c, y, x + c + 1, y + sz, 0x33FFFFFF);
            ctx.fill(x + c - 1, y + c - 1, x + c + 2, y + c + 2, ACCENT);
            double yaw = Math.toRadians(mc.player.getYaw()), scale = (c - 2) / range.d();
            double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = -Math.cos(yaw), rz = -Math.sin(yaw);
            for (Entity e : mc.world.getEntities()) {
                if (!validTarget(e, pl.on(), mb.on())) continue;
                double dx = e.getX() - mc.player.getX(), dz = e.getZ() - mc.player.getZ();
                double px = (dx * rx + dz * rz) * scale, py = -(dx * fx + dz * fz) * scale;
                if (Math.abs(px) > c - 2 || Math.abs(py) > c - 2) continue;
                int col = e instanceof PlayerEntity ? 0xFFFF5C7A : 0xFFFFB347;
                ctx.fill(x + c + (int) px - 1, y + c + (int) py - 1, x + c + (int) px + 2, y + c + (int) py + 2, col);
            }
        }
    }

    static class Keystrokes extends Module {
        Keystrokes() { super("Keystrokes", Category.RENDER); }
        void key(DrawContext ctx, int x, int y, int w, int h, String t, boolean down) {
            ctx.fill(x, y, x + w, y + h, down ? (ACCENT & 0xFFFFFF) | 0xCC000000 : 0xAA0E0E14);
            ctx.drawCenteredTextWithShadow(mc.textRenderer, t, x + w / 2, y + h / 2 - 4, 0xFFFFFFFF);
        }
        @Override void onHud(DrawContext ctx, int sw, int sh) {
            int x = sw - 84, y = sh - 84;
            key(ctx, x + 27, y, 24, 24, "W", mc.options.forwardKey.isPressed());
            key(ctx, x, y + 26, 24, 24, "A", mc.options.leftKey.isPressed());
            key(ctx, x + 27, y + 26, 24, 24, "S", mc.options.backKey.isPressed());
            key(ctx, x + 54, y + 26, 24, 24, "D", mc.options.rightKey.isPressed());
            key(ctx, x, y + 52, 78, 16, "SPACE", mc.options.jumpKey.isPressed());
        }
    }

    static class Compass extends Module {
        Compass() { super("Compass", Category.RENDER); }
        @Override void onHud(DrawContext ctx, int sw, int sh) {
            String t = mc.player.getHorizontalFacing().asString().toUpperCase() + "  " + (int) MathHelper.wrapDegrees(mc.player.getYaw());
            ctx.drawCenteredTextWithShadow(mc.textRenderer, t, sw / 2, 26, ACCENT);
        }
    }

    static class Speedometer extends Module {
        Speedometer() { super("Speedometer", Category.RENDER); }
        @Override void onHud(DrawContext ctx, int sw, int sh) {
            ctx.drawCenteredTextWithShadow(mc.textRenderer, String.format("%.1f b/s", bps), sw / 2, sh - 62, 0xFFFFFFFF);
        }
    }

    // ---- Player ----
    static class AutoEat extends Module {
        Setting hunger = add(new Setting("Hunger", 14, 1, 19));
        boolean eating;
        AutoEat() { super("AutoEat", Category.PLAYER); }
        @Override void onTick() {
            if (mc.player.getHungerManager().getFoodLevel() <= hunger.f()) {
                int slot = findHotbar(st -> st.contains(DataComponentTypes.FOOD));
                if (slot >= 0) { selectSlot(slot); mc.options.useKey.setPressed(true); eating = true; return; }
            }
            if (eating) { mc.options.useKey.setPressed(false); eating = false; }
        }
        @Override void onDisable() { if (eating) mc.options.useKey.setPressed(false); eating = false; }
    }

    static class AutoArmor extends Module {
        int cd;
        AutoArmor() { super("AutoArmor", Category.PLAYER); }
        @Override void onTick() {
            if (cd > 0) { cd--; return; }
            var inv = mc.player.getInventory();
            for (int i = 0; i < 36; i++) {
                ItemStack st = inv.getStack(i);
                var eq = st.get(DataComponentTypes.EQUIPPABLE);
                if (eq == null) continue;
                EquipmentSlot es = eq.slot();
                if (es != EquipmentSlot.HEAD && es != EquipmentSlot.CHEST && es != EquipmentSlot.LEGS && es != EquipmentSlot.FEET) continue;
                if (mc.player.getEquippedStack(es).isEmpty()) {
                    int slotId = i < 9 ? 36 + i : i;
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slotId, 0, SlotActionType.QUICK_MOVE, mc.player);
                    cd = 6;
                    return;
                }
            }
        }
    }

    static class Derp extends Module {
        Derp() { super("Derp", Category.PLAYER); }
        @Override void onTick() {
            mc.player.setYaw((float) (Math.random() * 360 - 180));
            mc.player.setPitch((float) (Math.random() * 180 - 90));
        }
    }

    static class Spin extends Module {
        Setting speed = add(new Setting("Speed", 30, 5, 90));
        Spin() { super("Spin", Category.PLAYER); }
        @Override void onTick() { mc.player.setYaw(mc.player.getYaw() + speed.f()); }
    }

    static class Freeze extends Module {
        Freeze() { super("Freeze", Category.PLAYER); }
        @Override void onTick() { mc.player.setVelocity(Vec3d.ZERO); }
    }

    // ---- Misc ----
    static class ChestStealer extends Module {
        Setting delay = add(new Setting("Delay", 2, 0, 10));
        int cd;
        ChestStealer() { super("ChestStealer", Category.MISC); }
        @Override void onTick() {
            if (!(mc.currentScreen instanceof GenericContainerScreen gs)) return;
            if (cd-- > 0) return;
            var h = gs.getScreenHandler();
            int n = h.getRows() * 9;
            for (int i = 0; i < n; i++) {
                if (!h.getSlot(i).getStack().isEmpty()) {
                    mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                    cd = (int) delay.d();
                    return;
                }
            }
        }
    }

    static class AutoFish extends Module {
        int cd;
        AutoFish() { super("AutoFish", Category.MISC); }
        @Override void onTick() {
            if (!mc.player.getMainHandStack().isOf(Items.FISHING_ROD)) return;
            if (cd > 0) { cd--; return; }
            var hook = mc.player.fishHook;
            if (hook == null) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                cd = 30;
            } else if (hook.getVelocity().y < -0.08) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                cd = 25;
            }
        }
    }

    static class RightClicker extends Module {
        Setting cps = add(new Setting("CPS", 8, 1, 20));
        int t;
        RightClicker() { super("RightClicker", Category.MISC); }
        @Override void onTick() {
            if (mc.currentScreen != null || !mc.options.useKey.isPressed()) return;
            if (++t >= Math.max(1, Math.round(20 / cps.f()))) {
                t = 0;
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
        }
    }

    static class AutoMine extends Module {
        AutoMine() { super("AutoMine", Category.MISC); }
        @Override void onTick() { mc.options.attackKey.setPressed(true); }
        @Override void onDisable() { mc.options.attackKey.setPressed(false); }
    }

    static class AutoLog extends Module {
        Setting hp = add(new Setting("HP", 6, 1, 20));
        AutoLog() { super("AutoLog", Category.MISC); }
        @Override void onTick() {
            if (mc.player.getHealth() <= hp.f() && mc.getNetworkHandler() != null) {
                enabled = false;
                mc.getNetworkHandler().getConnection().disconnect(Text.literal("Oxion AutoLog: low HP"));
            }
        }
    }

    /** Turns every other module off at once. */
    static class Panic extends Module {
        Panic() { super("Panic", Category.MISC); }
        @Override void onEnable() {
            for (Module m : MODULES) if (m != this && m.enabled) { m.enabled = false; m.onDisable(); }
            enabled = false;
        }
    }

    // ---------------- HUD ----------------
    static class HudElem {
        final String name; final Setting on; float fx, fy; int w = 80, h = 16;
        HudElem(String n, Setting on, float fx, float fy) { this.name = n; this.on = on; this.fx = fx; this.fy = fy; }
    }

    static float hudAlpha = 0.75f, shownHp = -1;
    static LivingEntity lastTarget;
    static int lastTargetTick, tickCount;

    static int al(int c) {
        int a = Math.max(4, (int) (((c >>> 24) & 0xFF) * hudAlpha));
        return (a << 24) | (c & 0xFFFFFF);
    }

    static void box(DrawContext ctx, int w, int h, String title) {
        ctx.fill(0, 0, w, h, al(0xFF0E0E14));
        if (title != null) {
            ctx.fill(0, 0, w, 12, al(0xFF1B1B26));
            ctx.fill(0, 12, w, 13, ACCENT);
            ctx.drawCenteredTextWithShadow(mc.textRenderer, title, w / 2, 2, 0xFFFFFFFF);
        } else ctx.fill(0, h - 1, w, h, ACCENT);
    }

    static String fmt(int ticks) {
        if (ticks < 0) return "inf";
        int sec = ticks / 20;
        return String.format("%d:%02d", sec / 60, sec % 60);
    }

    static void renderHud(DrawContext ctx, HUD h) {
        hudAlpha = h.opacity.f();
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        for (HudElem e : h.elems) if (e.on.on()) drawElem(ctx, h, e, sw, sh, false);
    }

    static void drawElem(DrawContext ctx, HUD h, HudElem e, int sw, int sh, boolean preview) {
        if (mc.player == null) return;
        float sc = h.scale.f();
        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate((int) (e.fx * sw), (int) (e.fy * sh), 0);
        ms.scale(sc, sc, 1f);
        int[] size;
        switch (e.name) {
            case "Watermark" -> size = dWatermark(ctx);
            case "TargetHUD" -> size = dTarget(ctx, preview);
            case "Potions" -> size = dPotions(ctx, preview);
            case "Cooldowns" -> size = dCooldowns(ctx, preview);
            case "Keybinds" -> size = dKeybinds(ctx, h, preview);
            case "Coords" -> size = dCoords(ctx);
            case "Armor" -> size = dArmor(ctx, preview);
            case "AttackBar" -> size = dAttack(ctx);
            default -> size = dArray(ctx, h);
        }
        ms.pop();
        e.w = Math.max(20, (int) (size[0] * sc));
        e.h = Math.max(12, (int) (size[1] * sc));
    }

    static int[] dWatermark(DrawContext ctx) {
        var tr = mc.textRenderer;
        int ping = 0;
        if (mc.getNetworkHandler() != null) {
            var en = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (en != null) ping = en.getLatency();
        }
        String t = "Oxion  |  " + mc.getCurrentFps() + " fps  |  " + ping + " ms  |  " + String.format("%.1f", bps) + " bps";
        int w = tr.getWidth(t) + 12;
        box(ctx, w, 15, null);
        ctx.drawTextWithShadow(tr, t, 6, 4, 0xFFFFFFFF);
        return new int[]{w, 15};
    }

    static int[] dTarget(DrawContext ctx, boolean preview) {
        var tr = mc.textRenderer;
        LivingEntity t = lastTarget;
        if (t == null && !preview) { shownHp = -1; return new int[]{0, 0}; }
        String name = t != null ? t.getName().getString() : "Target";
        float hp = t != null ? t.getHealth() : 14f, max = t != null ? t.getMaxHealth() : 20f;
        float abs = t != null ? t.getAbsorptionAmount() : 4f;
        float dist = t != null ? mc.player.distanceTo(t) : 3.2f;
        int armor = t != null ? t.getArmor() : 12;
        if (shownHp < 0) shownHp = hp;
        shownHp += (hp - shownHp) * 0.15f;
        int w = 150, h = 46;
        box(ctx, w, h, null);
        ctx.drawTextWithShadow(tr, name, 6, 5, 0xFFFFFFFF);
        String ds = String.format("%.1fm", dist);
        ctx.drawTextWithShadow(tr, ds, w - 6 - tr.getWidth(ds), 5, 0xFF9B9BAA);
        float ratio = MathHelper.clamp(shownHp / Math.max(1f, max), 0f, 1f);
        int barW = w - 12;
        ctx.fill(6, 18, 6 + barW, 26, 0xFF2A2A33);
        int hpColor = 0xFF000000 | MathHelper.hsvToRgb(ratio * 0.33f, 0.8f, 1f);
        ctx.fill(6, 18, 6 + (int) (barW * ratio), 26, hpColor);
        if (abs > 0) ctx.fill(6, 18, 6 + (int) (barW * MathHelper.clamp(abs / Math.max(1f, max), 0f, 1f)), 20, 0xFFFFD84D);
        String hs = String.format("HP %.1f / %.0f", hp, max) + (abs > 0 ? String.format("  (+%.0f)", abs) : "");
        ctx.drawTextWithShadow(tr, hs, 6, 31, 0xFFDDDDDD);
        String as = "ARM " + armor;
        ctx.drawTextWithShadow(tr, as, w - 6 - tr.getWidth(as), 31, ACCENT);
        return new int[]{w, h};
    }

    static int[] dPotions(DrawContext ctx, boolean preview) {
        var tr = mc.textRenderer;
        List<String[]> rows = new ArrayList<>();
        for (StatusEffectInstance ei : mc.player.getStatusEffects())
            rows.add(new String[]{ei.getEffectType().value().getName().getString() + " " + (ei.getAmplifier() + 1), fmt(ei.getDuration())});
        if (rows.isEmpty() && preview) { rows.add(new String[]{"Speed 2", "0:45"}); rows.add(new String[]{"Strength 1", "1:20"}); }
        if (rows.isEmpty()) return new int[]{0, 0};
        int w = 120, h = 16 + rows.size() * 10;
        box(ctx, w, h, "Potions");
        int ry = 16;
        for (String[] r : rows) {
            ctx.drawTextWithShadow(tr, r[0], 4, ry, 0xFFDDDDDD);
            ctx.drawTextWithShadow(tr, r[1], w - 4 - tr.getWidth(r[1]), ry, 0xFF9B9BAA);
            ry += 10;
        }
        return new int[]{w, h};
    }

    static int[] dCooldowns(DrawContext ctx, boolean preview) {
        var tr = mc.textRenderer;
        ItemStack[] items = {new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.GOLDEN_APPLE),
                new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), new ItemStack(Items.CHORUS_FRUIT),
                new ItemStack(Items.SHIELD), new ItemStack(Items.WIND_CHARGE), new ItemStack(Items.ENDER_EYE),
                new ItemStack(Items.TOTEM_OF_UNDYING)};
        List<Object[]> rows = new ArrayList<>();
        for (ItemStack st : items) {
            if (mc.player.getItemCooldownManager().isCoolingDown(st))
                rows.add(new Object[]{st, mc.player.getItemCooldownManager().getCooldownProgress(st, 0f)});
        }
        if (rows.isEmpty() && preview) rows.add(new Object[]{new ItemStack(Items.ENDER_PEARL), 0.6f});
        if (rows.isEmpty()) return new int[]{0, 0};
        int w = 130, h = 16 + rows.size() * 18;
        box(ctx, w, h, "Cooldowns");
        int ry = 16;
        for (Object[] r : rows) {
            ItemStack st = (ItemStack) r[0]; float prog = (Float) r[1];
            ctx.drawItem(st, 2, ry);
            ctx.drawTextWithShadow(tr, st.getName().getString(), 22, ry, 0xFFDDDDDD);
            String pc = (int) (prog * 100) + "%";
            ctx.drawTextWithShadow(tr, pc, w - 4 - tr.getWidth(pc), ry, 0xFF9B9BAA);
            ctx.fill(22, ry + 11, w - 4, ry + 13, 0xFF2A2A33);
            ctx.fill(22, ry + 11, 22 + (int) ((w - 26) * prog), ry + 13, ACCENT);
            ry += 18;
        }
        return new int[]{w, h};
    }

    static int[] dKeybinds(DrawContext ctx, HUD h, boolean preview) {
        var tr = mc.textRenderer;
        List<String[]> rows = new ArrayList<>();
        for (Module m : MODULES) {
            if (!m.enabled || m == h) continue;
            rows.add(new String[]{m.name, m.key.isUnbound() ? "NONE" : m.key.getBoundKeyLocalizedText().getString().toUpperCase()});
        }
        if (rows.isEmpty() && preview) rows.add(new String[]{"TriggerBot", "R"});
        if (rows.isEmpty()) return new int[]{0, 0};
        int w = 110, hh = 16 + rows.size() * 10;
        box(ctx, w, hh, "Keybinds");
        int ry = 16;
        for (String[] r : rows) {
            ctx.drawTextWithShadow(tr, r[0], 4, ry, 0xFFDDDDDD);
            ctx.drawTextWithShadow(tr, r[1], w - 4 - tr.getWidth(r[1]), ry, ACCENT);
            ry += 10;
        }
        return new int[]{w, hh};
    }

    static int[] dCoords(DrawContext ctx) {
        var tr = mc.textRenderer;
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        String l1 = String.format("XYZ: %.1f %.1f %.1f", x, y, z);
        String dim = mc.world.getRegistryKey().getValue().getPath();
        String l2 = mc.player.getHorizontalFacing().asString().toUpperCase();
        if (dim.equals("overworld")) l2 += String.format("   Nether: %.0f %.0f", x / 8, z / 8);
        else if (dim.equals("the_nether")) l2 += String.format("   Overworld: %.0f %.0f", x * 8, z * 8);
        int w = Math.max(tr.getWidth(l1), tr.getWidth(l2)) + 10;
        box(ctx, w, 26, null);
        ctx.drawTextWithShadow(tr, l1, 5, 4, 0xFFFFFFFF);
        ctx.drawTextWithShadow(tr, l2, 5, 15, 0xFF9B9BAA);
        return new int[]{w, 26};
    }

    static int[] dArmor(DrawContext ctx, boolean preview) {
        var tr = mc.textRenderer;
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        int ay = 0, w = 22;
        for (EquipmentSlot es : slots) {
            ItemStack st = mc.player.getEquippedStack(es);
            if (st.isEmpty()) continue;
            ctx.drawItem(st, 0, ay);
            if (st.isDamageable()) {
                String left = String.valueOf(st.getMaxDamage() - st.getDamage());
                ctx.drawTextWithShadow(tr, left, 18, ay + 4, 0xFFDDDDDD);
                w = Math.max(w, 20 + tr.getWidth(left));
            }
            ay += 18;
        }
        if (ay == 0 && preview) return new int[]{40, 18};
        return new int[]{w, ay};
    }

    static int[] dAttack(DrawContext ctx) {
        float p = MathHelper.clamp(mc.player.getAttackCooldownProgress(0f), 0f, 1f);
        box(ctx, 60, 9, null);
        ctx.fill(2, 3, 2 + (int) (56 * p), 6, ACCENT);
        return new int[]{60, 9};
    }

    static int[] dArray(DrawContext ctx, HUD h) {
        var tr = mc.textRenderer;
        int y = 0, w = 40;
        for (Module m : MODULES) {
            if (!m.enabled || m == h) continue;
            ctx.drawTextWithShadow(tr, m.name, 0, y, ACCENT);
            w = Math.max(w, tr.getWidth(m.name));
            y += 10;
        }
        return new int[]{w, Math.max(10, y)};
    }

    // ---------------- Config ----------------
    static Path cfgPath() { return FabricLoader.getInstance().getConfigDir().resolve("oxion-dlc.properties"); }

    static void saveConfig() {
        try {
            Properties p = new Properties();
            for (Module m : MODULES) {
                p.setProperty("m." + m.name + ".on", String.valueOf(m.enabled));
                for (Setting st : m.settings) p.setProperty("m." + m.name + "." + st.name, String.valueOf(st.value));
            }
            if (find("HUD") instanceof HUD h) for (HudElem e : h.elems) {
                p.setProperty("h." + e.name + ".x", String.valueOf(e.fx));
                p.setProperty("h." + e.name + ".y", String.valueOf(e.fy));
            }
            try (var out = Files.newOutputStream(cfgPath())) { p.store(out, "Oxion DLC"); }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    static void loadConfig() {
        try {
            if (!Files.exists(cfgPath())) return;
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(cfgPath())) { p.load(in); }
            for (Module m : MODULES) {
                String on = p.getProperty("m." + m.name + ".on");
                if (on != null) m.enabled = Boolean.parseBoolean(on);
                for (Setting st : m.settings) {
                    String v = p.getProperty("m." + m.name + "." + st.name);
                    if (v != null) st.value = Double.parseDouble(v);
                }
            }
            if (find("HUD") instanceof HUD h) for (HudElem e : h.elems) {
                String x = p.getProperty("h." + e.name + ".x"), y = p.getProperty("h." + e.name + ".y");
                if (x != null) e.fx = Float.parseFloat(x);
                if (y != null) e.fy = Float.parseFloat(y);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ---------------- HUD editor ----------------
    static class HudEditor extends Screen {
        HudElem drag; int dx, dy;
        HudEditor() { super(Text.literal("HUD Editor")); }

        @Override public void render(DrawContext ctx, int mx, int my, float delta) {
            ctx.fill(0, 0, width, height, 0x77000000);
            if (find("HUD") instanceof HUD h) {
                hudAlpha = h.opacity.f();
                for (HudElem e : h.elems) {
                    if (!e.on.on()) continue;
                    drawElem(ctx, h, e, width, height, true);
                    int x = (int) (e.fx * width), y = (int) (e.fy * height);
                    ctx.drawBorder(x - 1, y - 1, e.w + 2, e.h + 2, e == drag ? 0xFFFFFFFF : ACCENT);
                    ctx.drawTextWithShadow(textRenderer, e.name, x, y - 10, 0xFFAAAAAA);
                }
            }
            ctx.drawCenteredTextWithShadow(textRenderer, "HUD Editor: перетаскивай блоки. Esc - сохранить и выйти", width / 2, height - 14, 0xFFFFFFFF);
        }

        @Override public boolean mouseClicked(double mx, double my, int button) {
            if (find("HUD") instanceof HUD h && button == 0) {
                for (int i = h.elems.size() - 1; i >= 0; i--) {
                    HudElem e = h.elems.get(i);
                    if (!e.on.on()) continue;
                    int x = (int) (e.fx * width), y = (int) (e.fy * height);
                    if (mx >= x && mx < x + e.w && my >= y && my < y + e.h) { drag = e; dx = (int) mx - x; dy = (int) my - y; return true; }
                }
            }
            return super.mouseClicked(mx, my, button);
        }

        @Override public boolean mouseDragged(double mx, double my, int button, double ddx, double ddy) {
            if (drag != null) {
                drag.fx = MathHelper.clamp((float) (mx - dx) / width, 0f, 0.98f);
                drag.fy = MathHelper.clamp((float) (my - dy) / height, 0f, 0.98f);
                return true;
            }
            return super.mouseDragged(mx, my, button, ddx, ddy);
        }

        @Override public boolean mouseReleased(double mx, double my, int button) { drag = null; return super.mouseReleased(mx, my, button); }
        @Override public void removed() { saveConfig(); }
        @Override public boolean shouldPause() { return false; }
    }

    // ---------------- ClickGUI (liquid glass, square) ----------------
    static class ClickGui extends Screen {
        static Category selCat = Category.COMBAT;
        static Module selMod;
        static boolean settingsOpen;
        static float scrollY, setScrollY;
        static final int PAD = 10, TABH = 18, CELLH = 34, GAP = 6;
        final long openT = System.currentTimeMillis();
        Setting dragSetting; int dragX, dragW;
        Module binding;
        boolean pressing, scrolled;
        double pressY; float pressScroll;
        Runnable pending;

        ClickGui() { super(Text.literal("Oxion DLC")); }

        int S() { return Math.max(240, Math.min(320, Math.min(width, height) - 12)); }
        float anim() { float t = Math.min(1f, (System.currentTimeMillis() - openT) / 240f); return 1 - (1 - t) * (1 - t); }
        int x0() { return (width - S()) / 2; }
        int y0() { return (height - S()) / 2 + (int) ((1 - anim()) * 18); }
        int cx() { return x0() + PAD; }
        int cy() { return y0() + 58; }
        int cw() { return S() - 2 * PAD; }
        int ch() { return S() - 58 - 22; }
        int cellW() { return (cw() - GAP) / 2; }

        static int a(int c, float m) { int al = Math.max(4, (int) (((c >>> 24) & 0xFF) * m)); return (al << 24) | (c & 0xFFFFFF); }
        static boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
        static void rrect(DrawContext ctx, int x, int y, int w, int h, int c) {
            ctx.fill(x + 2, y, x + w - 2, y + h, c);
            ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, c);
            ctx.fill(x, y + 2, x + w, y + h - 2, c);
        }
        static void glass(DrawContext ctx, int x, int y, int w, int h, int tint, float an) {
            rrect(ctx, x, y, w, h, a(0x22FFFFFF, an));
            ctx.fillGradient(x + 2, y + 1, x + w - 2, y + h / 2, a(0x3DFFFFFF, an), a(0x00FFFFFF, an));
            if ((tint >>> 24) != 0) rrect(ctx, x, y, w, h, a(tint, an));
            int b = a(0x50FFFFFF, an);
            ctx.fill(x + 2, y, x + w - 2, y + 1, b);
            ctx.fill(x + 2, y + h - 1, x + w - 2, y + h, a(0x25FFFFFF, an));
            ctx.fill(x, y + 2, x + 1, y + h - 2, a(0x30FFFFFF, an));
            ctx.fill(x + w - 1, y + 2, x + w, y + h - 2, a(0x30FFFFFF, an));
        }
        static String pretty(Category c) { return c.name().charAt(0) + c.name().substring(1).toLowerCase(); }
        static List<Module> mods() {
            List<Module> l = new ArrayList<>();
            for (Module m : MODULES) if (m.category == selCat) l.add(m);
            return l;
        }

        int gridMax(List<Module> L) {
            int rows = (L.size() + 1) / 2;
            return Math.max(0, rows * (CELLH + GAP) - GAP - ch());
        }
        int rowH(Setting st) { return st.isOpt() || st.bool ? 20 : 28; }
        int setTotal(Module m) {
            int t = 44;
            for (Setting st : m.settings) t += rowH(st) + 4;
            return t;
        }
        int setMax(Module m) { return Math.max(0, setTotal(m) - (ch() - 20)); }

        @Override public void render(DrawContext ctx, int mx, int my, float delta) {
            float an = anim();
            int S = S(), x = x0(), y = y0();
            ctx.fillGradient(0, 0, width, height, a(0x88000000, an), a(0x55000000, an));
            for (int i = 6; i >= 1; i--) rrect(ctx, x - i, y - i, S + 2 * i, S + 2 * i, a((ACCENT & 0xFFFFFF) | 0x0C000000, an));
            rrect(ctx, x, y, S, S, a(0x6612121C, an));
            ctx.fillGradient(x + 2, y + 1, x + S - 2, y + S / 3, a(0x30FFFFFF, an), a(0x00FFFFFF, an));
            ctx.fillGradient(x + 2, y + S * 2 / 3, x + S - 2, y + S - 1, a(0x00000000, an), a((ACCENT & 0xFFFFFF) | 0x22000000, an));
            ctx.fill(x + 2, y, x + S - 2, y + 1, a(0x70FFFFFF, an));
            ctx.fill(x + 2, y + S - 1, x + S - 2, y + S, a(0x30FFFFFF, an));
            ctx.fill(x, y + 2, x + 1, y + S - 2, a(0x45FFFFFF, an));
            ctx.fill(x + S - 1, y + 2, x + S, y + S - 2, a(0x25FFFFFF, an));

            // moving shine
            long now = System.currentTimeMillis();
            int bx = x + (int) ((now / 18) % (S + 80)) - 40;
            for (int k = -14; k <= 14; k++) {
                int px = bx + k;
                if (px <= x + 1 || px >= x + S - 1) continue;
                ctx.fill(px, y + 2, px + 1, y + 30, a(((int) (0x26 * (1 - Math.abs(k) / 14f)) << 24) | 0xFFFFFF, an));
            }

            // header
            ctx.drawTextWithShadow(textRenderer, "OXION", x + PAD, y + 13, a(ACCENT, an));
            ctx.drawTextWithShadow(textRenderer, "glass", x + PAD + textRenderer.getWidth("OXION "), y + 13, a(0xFFDDDDEE, an));
            glass(ctx, x + S - PAD - 74, y + 8, 74, 18, 0, an);
            ctx.drawCenteredTextWithShadow(textRenderer, "HUD Editor", x + S - PAD - 37, y + 13, a(0xFFFFFFFF, an));

            // tabs
            Category[] cats = Category.values();
            int tw = cw() / cats.length;
            for (int i = 0; i < cats.length; i++) {
                int tx = cx() + i * tw;
                boolean sel = cats[i] == selCat;
                glass(ctx, tx + 1, y + 34, tw - 2, TABH, sel ? (ACCENT & 0xFFFFFF) | 0x66000000 : 0, an);
                ctx.drawCenteredTextWithShadow(textRenderer, pretty(cats[i]), tx + tw / 2, y + 40, a(sel ? 0xFFFFFFFF : 0xFFB9B9C8, an));
            }

            if (!settingsOpen || selMod == null) renderGrid(ctx, an, mx, my);
            else renderSettings(ctx, an, mx, my);

            ctx.drawCenteredTextWithShadow(textRenderer, "тап - вкл/выкл  |  > настройки  |  тяни - прокрутка", x + S / 2, y + S - 13, a(0xFF8A8AA0, an));
        }

        void renderGrid(DrawContext ctx, float an, int mx, int my) {
            List<Module> L = mods();
            scrollY = MathHelper.clamp(scrollY, 0, gridMax(L));
            int cw = cellW();
            ctx.enableScissor(cx(), cy(), cx() + cw(), cy() + ch());
            for (int i = 0; i < L.size(); i++) {
                Module m = L.get(i);
                int tx = cx() + (i % 2) * (cw + GAP), ty = cy() + (i / 2) * (CELLH + GAP) - (int) scrollY;
                if (ty + CELLH < cy() || ty > cy() + ch()) continue;
                glass(ctx, tx, ty, cw, CELLH, m.enabled ? (ACCENT & 0xFFFFFF) | 0x66000000 : 0, an);
                ctx.drawTextWithShadow(textRenderer, m.name, tx + 8, ty + 7, a(0xFFFFFFFF, an));
                ctx.drawTextWithShadow(textRenderer, m.enabled ? "ON" : "OFF", tx + 8, ty + 20, a(m.enabled ? 0xFFFFFFFF : 0xFF8C8CA0, an));
                if (!m.settings.isEmpty()) {
                    ctx.fill(tx + cw - 22, ty + 6, tx + cw - 21, ty + CELLH - 6, a(0x33FFFFFF, an));
                    ctx.drawTextWithShadow(textRenderer, ">", tx + cw - 13, ty + 13, a(0xFFDDDDEE, an));
                }
            }
            ctx.disableScissor();
            int mxs = gridMax(L);
            if (mxs > 0) {
                int total = ch() + mxs, barH = Math.max(14, ch() * ch() / total);
                int barY = cy() + (int) ((ch() - barH) * (scrollY / mxs));
                ctx.fill(cx() + cw() + 1, barY, cx() + cw() + 3, barY + barH, a(ACCENT, an));
            }
        }

        void renderSettings(DrawContext ctx, float an, int mx, int my) {
            Module m = selMod;
            setScrollY = MathHelper.clamp(setScrollY, 0, setMax(m));
            int cw = cw();
            glass(ctx, cx(), cy(), 56, 16, 0, an);
            ctx.drawCenteredTextWithShadow(textRenderer, "< Back", cx() + 28, cy() + 4, a(0xFFFFFFFF, an));
            ctx.drawTextWithShadow(textRenderer, m.name, cx() + 64, cy() + 4, a(ACCENT, an));

            int top = cy() + 20;
            ctx.enableScissor(cx(), top, cx() + cw, cy() + ch());
            int py = top - (int) setScrollY;
            glass(ctx, cx(), py, cw, 18, m.enabled ? (ACCENT & 0xFFFFFF) | 0x66000000 : 0, an);
            ctx.drawTextWithShadow(textRenderer, "Enabled: " + (m.enabled ? "ON" : "OFF"), cx() + 8, py + 5, a(0xFFFFFFFF, an));
            py += 22;
            glass(ctx, cx(), py, cw, 18, 0, an);
            String bk = binding == m ? "..." : (m.key.isUnbound() ? "NONE" : m.key.getBoundKeyLocalizedText().getString().toUpperCase());
            ctx.drawTextWithShadow(textRenderer, "Bind: " + bk, cx() + 8, py + 5, a(0xFFFFFFFF, an));
            py += 22;
            for (Setting st : m.settings) {
                int h = rowH(st);
                if (st.bool) {
                    glass(ctx, cx(), py, cw, h, 0, an);
                    rrect(ctx, cx() + cw - 30, py + 4, 22, 12, a(st.on() ? (ACCENT & 0xFFFFFF) | 0xCC000000 : 0xFF2A2A36, an));
                    ctx.fill(cx() + cw - (st.on() ? 18 : 28), py + 6, cx() + cw - (st.on() ? 10 : 20), py + 14, a(0xFFFFFFFF, an));
                    ctx.drawTextWithShadow(textRenderer, st.name, cx() + 8, py + 6, a(0xFFE8E8F2, an));
                } else if (st.isOpt()) {
                    glass(ctx, cx(), py, cw, h, 0, an);
                    ctx.drawTextWithShadow(textRenderer, st.name, cx() + 8, py + 6, a(0xFFE8E8F2, an));
                    String v = "< " + st.optName() + " >";
                    ctx.drawTextWithShadow(textRenderer, v, cx() + cw - 8 - textRenderer.getWidth(v), py + 6, a(ACCENT, an));
                } else {
                    ctx.drawTextWithShadow(textRenderer, st.name, cx() + 4, py, a(0xFFE8E8F2, an));
                    String v = String.valueOf(st.value);
                    ctx.drawTextWithShadow(textRenderer, v, cx() + cw - 4 - textRenderer.getWidth(v), py, a(0xFFFFFFFF, an));
                    int by = py + 13;
                    rrect(ctx, cx(), by, cw, 12, a(0x40FFFFFF, an));
                    double r = (st.value - st.min) / (st.max - st.min);
                    rrect(ctx, cx(), by, Math.max(4, (int) (cw * r)), 12, a((ACCENT & 0xFFFFFF) | 0xAA000000, an));
                    ctx.fillGradient(cx() + 1, by + 1, cx() + Math.max(3, (int) (cw * r)) - 1, by + 6, a(0x40FFFFFF, an), a(0x00FFFFFF, an));
                }
                py += h + 4;
            }
            ctx.disableScissor();
        }

        @Override public boolean mouseClicked(double mx, double my, int button) {
            int x = x0(), y = y0(), S = S();
            if (in(mx, my, x + S - PAD - 74, y + 8, 74, 18)) { client.setScreen(new HudEditor()); return true; }
            Category[] cats = Category.values();
            int tw = cw() / cats.length;
            for (int i = 0; i < cats.length; i++) {
                if (in(mx, my, cx() + i * tw + 1, y + 34, tw - 2, TABH)) { selCat = cats[i]; scrollY = 0; settingsOpen = false; return true; }
            }
            scrolled = false; pending = null; pressing = false;
            if (!settingsOpen || selMod == null) {
                if (!in(mx, my, cx(), cy(), cw(), ch())) return super.mouseClicked(mx, my, button);
                pressing = true; pressY = my; pressScroll = scrollY;
                List<Module> L = mods();
                int cw = cellW();
                for (int i = 0; i < L.size(); i++) {
                    int tx = cx() + (i % 2) * (cw + GAP), ty = cy() + (i / 2) * (CELLH + GAP) - (int) scrollY;
                    if (in(mx, my, tx, ty, cw, CELLH)) {
                        Module m = L.get(i);
                        boolean gear = mx >= tx + cw - 22 && !m.settings.isEmpty();
                        final int btn = button;
                        pending = () -> {
                            if (btn == 1 || gear) { selMod = m; settingsOpen = true; setScrollY = 0; }
                            else m.toggle();
                        };
                        break;
                    }
                }
                return true;
            }
            Module m = selMod;
            if (in(mx, my, cx(), cy(), 56, 16)) { settingsOpen = false; return true; }
            int top = cy() + 20;
            if (!in(mx, my, cx(), top, cw(), ch() - 20)) return super.mouseClicked(mx, my, button);
            pressing = true; pressY = my; pressScroll = setScrollY;
            int py = top - (int) setScrollY;
            if (in(mx, my, cx(), py, cw(), 18)) { pending = m::toggle; return true; }
            py += 22;
            if (in(mx, my, cx(), py, cw(), 18)) { pending = () -> binding = m; return true; }
            py += 22;
            for (Setting st : m.settings) {
                int h = rowH(st);
                if (in(mx, my, cx(), py, cw(), h)) {
                    if (st.bool) pending = () -> st.value = st.on() ? 0 : 1;
                    else if (st.isOpt()) pending = st::cycle;
                    else if (my >= py + 13) {
                        dragSetting = st; dragX = cx(); dragW = cw();
                        st.setFromRatio((mx - dragX) / dragW);
                        pressing = false;
                    }
                    return true;
                }
                py += h + 4;
            }
            return true;
        }

        @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
            if (dragSetting != null) { dragSetting.setFromRatio((mx - dragX) / dragW); return true; }
            if (pressing) {
                double d = my - pressY;
                if (Math.abs(d) > 4) scrolled = true;
                if (scrolled) {
                    if (settingsOpen && selMod != null) setScrollY = MathHelper.clamp((float) (pressScroll - d), 0, setMax(selMod));
                    else scrollY = MathHelper.clamp((float) (pressScroll - d), 0, gridMax(mods()));
                }
                return true;
            }
            return super.mouseDragged(mx, my, button, dx, dy);
        }

        @Override public boolean mouseReleased(double mx, double my, int button) {
            if (pressing && !scrolled && pending != null) pending.run();
            pressing = false; scrolled = false; pending = null; dragSetting = null;
            return super.mouseReleased(mx, my, button);
        }

        @Override public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
            float d = (float) -vAmount * 22f;
            if (settingsOpen && selMod != null) setScrollY = MathHelper.clamp(setScrollY + d, 0, setMax(selMod));
            else scrollY = MathHelper.clamp(scrollY + d, 0, gridMax(mods()));
            return true;
        }

        @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (binding != null) {
                binding.key.setBoundKey(keyCode == GLFW.GLFW_KEY_ESCAPE ? InputUtil.UNKNOWN_KEY : InputUtil.fromKeyCode(keyCode, scanCode));
                KeyBinding.updateKeysByCode();
                binding = null;
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override public void removed() { saveConfig(); }
        @Override public boolean shouldPause() { return false; }
    }
                    }
