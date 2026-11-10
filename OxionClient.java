package dev.oxion;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Oxion DLC - personal Fabric client for Minecraft 1.21.4. RIGHT SHIFT = ClickGUI. */
public class OxionClient implements ClientModInitializer {
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final List<Module> MODULES = new ArrayList<>();
    private static KeyBinding menuKey;
    static final int ACCENT = 0xFF9B5CFF;
    static double prevX, prevZ, bps;

    public enum Category { COMBAT, MOVEMENT, RENDER, PLAYER }

    // ---------------- Settings ----------------
    public static class Setting {
        final String name; double value; final double min, max; final boolean bool;
        Setting(String name, double value, double min, double max) {
            this.name = name; this.value = value; this.min = min; this.max = max; this.bool = false;
        }
        Setting(String name, boolean on) {
            this.name = name; this.value = on ? 1 : 0; this.min = 0; this.max = 1; this.bool = true;
        }
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
        MODULES.add(new HUD());
        MODULES.add(new AutoRespawn());
        MODULES.add(new AutoWalk());

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Oxion DLC Menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "Oxion DLC"));
        for (Module m : MODULES) {
            m.key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "Toggle " + m.name, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "Oxion DLC"));
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) client.setScreen(new ClickGui());
            if (client.player == null || client.world == null) return;
            bps = Math.hypot(client.player.getX() - prevX, client.player.getZ() - prevZ) * 20;
            prevX = client.player.getX(); prevZ = client.player.getZ();
            for (Module m : MODULES) {
                while (m.key.wasPressed()) m.toggle();
                if (m.enabled) m.onTick();
            }
        });

        HudRenderCallback.EVENT.register((ctx, tick) -> {
            if (mc.player == null || mc.options.hudHidden || mc.options.debugEnabled) return;
            if (find("HUD") instanceof HUD h && h.enabled) renderHud(ctx, h);
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
        Setting cd = add(new Setting("Cooldown", 0.92, 0.5, 1.0));
        Setting pl = add(new Setting("Players", true)), mb = add(new Setting("Mobs", true));
        TriggerBot() { super("TriggerBot", Category.COMBAT); }
        @Override void onTick() {
            if (mc.currentScreen != null) return;
            HitResult hr = mc.crosshairTarget;
            if (hr instanceof EntityHitResult ehr && validTarget(ehr.getEntity(), pl.on(), mb.on())
                    && mc.player.getAttackCooldownProgress(0.5f) >= cd.f()) attack(ehr.getEntity());
        }
    }

    static class AimAssist extends Module {
        Setting range = add(new Setting("Range", 4.2, 1, 6));
        Setting speed = add(new Setting("Speed", 0.25, 0.05, 1));
        Setting fov = add(new Setting("FOV", 90, 10, 360));
        Setting pl = add(new Setting("Players", true)), mb = add(new Setting("Mobs", true));
        Setting needClick = add(new Setting("OnlyOnClick", true));
        AimAssist() { super("AimAssist", Category.COMBAT); }
        @Override void onTick() {
            if (mc.currentScreen != null || (needClick.on() && !mc.options.attackKey.isPressed())) return;
            LivingEntity t = nearest(range.d(), fov.f(), pl.on(), mb.on());
            if (t == null) return;
            float[] r = rotsTo(t);
            mc.player.setYaw(mc.player.getYaw() + MathHelper.wrapDegrees(r[0] - mc.player.getYaw()) * speed.f());
            mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + (r[1] - mc.player.getPitch()) * speed.f(), -90f, 90f));
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
        Setting pl = add(new Setting("Players", true)), mb = add(new Setting("Mobs", true));
        ESP() { super("ESP", Category.RENDER); }
        @Override void onTick() {
            for (Entity e : mc.world.getEntities()) if (validTarget(e, pl.on(), mb.on())) e.setGlowing(true);
        }
        @Override void onDisable() {
            if (mc.world == null) return;
            for (Entity e : mc.world.getEntities()) if (e instanceof LivingEntity && e != mc.player) e.setGlowing(false);
        }
    }

    static class HUD extends Module {
        Setting wm = add(new Setting("Watermark", true)), pots = add(new Setting("Potions", true)),
                keys = add(new Setting("Keybinds", true)), cds = add(new Setting("Cooldowns", true)),
                list = add(new Setting("ArrayList", false));
        HUD() { super("HUD", Category.RENDER); enabled = true; }
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


    // ---------------- HUD ----------------
    static void panel(DrawContext ctx, int x, int y, int w, int h, String title) {
        ctx.fill(x, y, x + w, y + h, 0xAA0E0E14);
        ctx.fill(x, y, x + w, y + 12, 0xCC1B1B26);
        ctx.fill(x, y + 12, x + w, y + 13, ACCENT);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, title, x + w / 2, y + 2, 0xFFFFFFFF);
    }

    static String fmt(int ticks) {
        if (ticks < 0) return "inf";
        int sec = ticks / 20;
        return String.format("%d:%02d", sec / 60, sec % 60);
    }

    static void renderHud(DrawContext ctx, HUD h) {
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        var tr = mc.textRenderer;

        // Watermark: Oxion | fps | ping | bps
        if (h.wm.on()) {
            int ping = 0;
            if (mc.getNetworkHandler() != null) {
                var e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (e != null) ping = e.getLatency();
            }
            String t = "Oxion  |  " + mc.getCurrentFps() + " fps  |  " + ping + " ms  |  " + String.format("%.1f", bps) + " bps";
            int w = tr.getWidth(t) + 12, x = (sw - w) / 2;
            ctx.fill(x, 6, x + w, 20, 0xAA0E0E14);
            ctx.fill(x, 19, x + w, 20, ACCENT);
            ctx.drawTextWithShadow(tr, t, x + 6, 10, 0xFFFFFFFF);
        }

        // Potions (left of crosshair)
        if (h.pots.on() && !mc.player.getStatusEffects().isEmpty()) {
            var list = new ArrayList<>(mc.player.getStatusEffects());
            int w = 120, x = sw / 2 - 190, y = sh / 2 + 20;
            panel(ctx, x, y, w, 15 + list.size() * 10, "Potions");
            int ry = y + 16;
            for (StatusEffectInstance ei : list) {
                String n = ei.getEffectType().value().getName().getString() + " " + (ei.getAmplifier() + 1);
                ctx.drawTextWithShadow(tr, n, x + 4, ry, 0xFFDDDDDD);
                String d = fmt(ei.getDuration());
                ctx.drawTextWithS
