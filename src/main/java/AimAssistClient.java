import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class AimAssistClient implements ClientModInitializer {

    private static KeyBinding configKeyBinding;

    @Override
    public void onInitializeClient() {
        // Біндимо клавішу R для відкриття меню
        configKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Open AimAssist Menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "AimAssist Mod"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (configKeyBinding.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new AimAssistConfigScreen());
                }
            }
            AimAssistEngine.onClientTick(client);
        });
    }
}
