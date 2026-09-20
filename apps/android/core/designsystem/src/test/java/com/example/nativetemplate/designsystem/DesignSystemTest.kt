package com.example.nativetemplate.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.nativetemplate.designsystem.components.*
import com.example.nativetemplate.designsystem.theme.DSTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en")
class DesignSystemTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allButtonIntentsActivate() {
        var calls = 0
        val intent = mutableStateOf(DSButtonIntent.Primary)
        compose.setContent {
            DSTheme(dynamicColor = false) {
                DSButton("Save", { calls++ }, intent = intent.value)
            }
        }
        for (variant in DSButtonIntent.entries) {
            compose.runOnIdle { intent.value = variant }
            compose.onNodeWithText("Save").performTouchInput { click() }
        }
        compose.runOnIdle { assertEquals(3, calls) }
    }

    @Test fun disabledButtonsDoNotActivate() {
        var calls = 0
        val intent = mutableStateOf(DSButtonIntent.Primary)
        compose.setContent {
            DSTheme(dynamicColor = false) {
                DSButton("Save", { calls++ }, intent = intent.value, enabled = false)
            }
        }
        for (variant in DSButtonIntent.entries) {
            compose.runOnIdle { intent.value = variant }
            compose.onNodeWithText("Save").assertIsNotEnabled().performTouchInput { click() }
        }
        compose.runOnIdle { assertEquals(0, calls) }
    }

    @Test fun loadingRetainsAccessibleNameAndBlocksActivation() {
        var calls = 0
        compose.setContent {
            DSTheme(dynamicColor = false) { DSButton("Save", { calls++ }, loading = true) }
        }
        compose.onNodeWithText("Save").assertIsNotEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Loading"))
            .performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, calls) }
    }

    @Test fun callerCanEndLoadingAndEnableTheAction() {
        var calls = 0
        val loading = mutableStateOf(true)
        compose.setContent {
            DSTheme(dynamicColor = false) { DSButton("Save", { calls++ }, loading = loading.value) }
        }
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.runOnIdle { loading.value = false }
        compose.onNodeWithText("Save").assertIsEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test fun fieldReflectsCallerOwnedState() {
        val value = mutableStateOf("")
        compose.setContent {
            DSTheme(dynamicColor = false) {
                DSTextField(value.value, { value.value = it }, label = "Name")
            }
        }
        compose.onNode(hasSetTextAction()).performTextInput("Ada")
        compose.runOnIdle { assertEquals("Ada", value.value); value.value = "Grace" }
        compose.onNode(hasSetTextAction()).assertTextContains("Grace")
    }

    @Test fun errorReplacesSupportingTextAndIsAccessible() {
        compose.setContent {
            DSTheme(dynamicColor = false) {
                DSTextField("", {}, "Name", supportingText = "Helpful text", errorText = "Required")
            }
        }
        compose.onNode(hasSetTextAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Required"))
        compose.onNodeWithText("Helpful text").assertDoesNotExist()
    }

    @Test fun disabledFieldExposesItsState() {
        compose.setContent {
            DSTheme(dynamicColor = false) { DSTextField("Read only", {}, "Name", enabled = false) }
        }
        compose.onNodeWithText("Read only").assertIsNotEnabled()
    }

    @Test fun statusActionIsOwnedByTheCaller() {
        var retries = 0
        compose.setContent {
            DSTheme(dynamicColor = false) {
                DSStatusView("Could not load", description = "Try again", kind = DSStatusKind.Error) {
                    DSButton("Retry", { retries++ }, intent = DSButtonIntent.Secondary)
                }
            }
        }
        compose.onNodeWithText("Could not load")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit))
        compose.onNodeWithText("Try again").assertIsDisplayed()
        compose.onNodeWithText("Retry").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun longLabelFitsAtLargeTextSizeInRtl() {
        val label = "A longer action label that needs to wrap"
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                DSTheme(dynamicColor = false) {
                    Box(Modifier.width(220.dp)) {
                        DSButton(label, {}, modifier = Modifier.testTag("action"))
                    }
                }
            }
        }
        val button = compose.onNodeWithTag("action").assertHeightIsAtLeast(48.dp).fetchSemanticsNode().boundsInRoot
        val text = compose.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(text.left >= button.left && text.right <= button.right)
        assertTrue(text.top >= button.top && text.bottom <= button.bottom)
    }
}
