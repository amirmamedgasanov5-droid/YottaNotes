package design.noodle.showcase

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import design.noodle.theme.NoodleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-port-xxhdpi")
class ShotTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val out = File("/tmp/claude-0/-home-user-YottaNotes/540b0b8a-3d4c-5206-996e-1bcd35c2d4fa/scratchpad/shots").also { it.mkdirs() }

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { NoodleTheme(darkTheme = dark) { Box(Modifier.fillMaxSize()) { content() } } }
        rule.mainClock.advanceTimeBy(2200)
        val view = rule.activity.window.decorView
        val w = view.width.takeIf { it > 0 } ?: 1179
        val h = view.height.takeIf { it > 0 } ?: 2556
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun home() = shot("home") { ShowcaseScaffold(ShowcaseState(), 0) }
    @Test fun controls() = shot("controls") { ShowcaseScaffold(ShowcaseState(), 1) }
    @Test fun lists() = shot("lists") { ShowcaseScaffold(ShowcaseState(), 2) }
    @Test fun motion() = shot("motion") { ShowcaseScaffold(ShowcaseState(), 3) }
    @Test fun style() = shot("style") { ShowcaseScaffold(ShowcaseState(), 4) }
    @Test fun homeDark() = shot("home_dark", true) { ShowcaseScaffold(ShowcaseState(), 0) }
    @Test fun controlsDark() = shot("controls_dark", true) { ShowcaseScaffold(ShowcaseState(), 1) }
    @Test fun sheet() = shot("sheet") { ShowcaseScaffold(ShowcaseState().apply { sheet = true }, 3) }
    @Test fun alert() = shot("alert") { ShowcaseScaffold(ShowcaseState().apply { alert = true }, 3) }
    @Test fun actionSheet() = shot("actionsheet") { ShowcaseScaffold(ShowcaseState().apply { actionSheet = true }, 3) }
}
