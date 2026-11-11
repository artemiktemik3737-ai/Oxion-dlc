package net.onion.client;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.onion.client.manager.ModuleManager;
import net.onion.client.ui.gui.ClickGuiScreen;
import org.lwjgl.glfw.GLFW;

public class OnionClient implements ModInitializer {
    public static final String MOD_ID = "onion-client";
    private static KeyBinding guiKeyBind;

    @Override
    public void onInitialize() {
        ModuleManager.init();

        guiKeyBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.onion.gui",
                InputUtil.Type.KEY_SYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.onion.client"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKeyBind.wasPressed()) {
                client.setScreen(new ClickGuiScreen());
            }
        });
    }
}
