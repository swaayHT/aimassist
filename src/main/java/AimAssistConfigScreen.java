import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class AimAssistConfigScreen extends Screen {

    protected AimAssistConfigScreen() {
        super(Text.literal("AimAssist Settings"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 4 - 20;

        // Toggle Main Aim
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("AimAssist: " + (AimAssistEngine.ENABLED ? "ON" : "OFF")),
                button -> {
                    AimAssistEngine.ENABLED = !AimAssistEngine.ENABLED;
                    button.setMessage(Text.literal("AimAssist: " + (AimAssistEngine.ENABLED ? "ON" : "OFF")));
                }
        ).dimensions(centerX - 100, startY, 200, 20).build());

        // Toggle Wall Check
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Wall Check: " + (AimAssistEngine.CHECK_WALLS ? "ON" : "OFF")),
                button -> {
                    AimAssistEngine.CHECK_WALLS = !AimAssistEngine.CHECK_WALLS;
                    button.setMessage(Text.literal("Wall Check: " + (AimAssistEngine.CHECK_WALLS ? "ON" : "OFF")));
                }
        ).dimensions(centerX - 100, startY + 25, 200, 20).build());

        // Slider FOV
        this.addDrawableChild(new SliderWidget(centerX - 100, startY + 50, 200, 20, 
                Text.literal("FOV: " + (int) AimAssistEngine.FOV), (AimAssistEngine.FOV - 10) / 170.0) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("FOV: " + (int) AimAssistEngine.FOV + "°"));
            }

            @Override
            protected void applyValue() {
                AimAssistEngine.FOV = 10 + (this.value * 170);
            }
        });

        // Slider Aim Speed
        this.addDrawableChild(new SliderWidget(centerX - 100, startY + 75, 200, 20, 
                Text.literal("Speed: " + String.format("%.2f", AimAssistEngine.AIM_SPEED)), AimAssistEngine.AIM_SPEED) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Speed: " + String.format("%.2f", AimAssistEngine.AIM_SPEED)));
            }

            @Override
            protected void applyValue() {
                AimAssistEngine.AIM_SPEED = Math.max(0.01, this.value);
            }
        });

        // Slider Prediction Offset (Налаштування зміщення/випередження)
        this.addDrawableChild(new SliderWidget(centerX - 100, startY + 100, 200, 20, 
                Text.literal("Prediction: " + String.format("%.2f", AimAssistEngine.PREDICTION_OFFSET)), AimAssistEngine.PREDICTION_OFFSET / 3.0) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Prediction: " + String.format("%.2f", AimAssistEngine.PREDICTION_OFFSET)));
            }

            @Override
            protected void applyValue() {
                AimAssistEngine.PREDICTION_OFFSET = this.value * 3.0; // від 0 до 3 блоків зміщення
            }
        });

        // Slider Distance
        this.addDrawableChild(new SliderWidget(centerX - 100, startY + 125, 200, 20, 
                Text.literal("Max Range: " + (int) AimAssistEngine.MAX_DISTANCE), (AimAssistEngine.MAX_DISTANCE - 5) / 45.0) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Max Range: " + (int) AimAssistEngine.MAX_DISTANCE + " blocks"));
            }

            @Override
            protected void applyValue() {
                AimAssistEngine.MAX_DISTANCE = 5 + (this.value * 45);
            }
        });

        // Close Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> this.close())
                .dimensions(centerX - 100, startY + 155, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
}
