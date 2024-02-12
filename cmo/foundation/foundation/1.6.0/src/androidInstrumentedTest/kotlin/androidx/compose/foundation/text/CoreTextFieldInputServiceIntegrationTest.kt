/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation.text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toOffset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class CoreTextFieldInputServiceIntegrationTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var focusManager: FocusManager
    private val platformTextInputService = FakePlatformTextInputService()
    private val textInputService = TextInputService(platformTextInputService)

    @Test
    fun textField_ImeOptions_isPassedTo_platformTextInputService() {
        val testTag = "KeyboardOption"
        val value = TextFieldValue("abc")
        val imeOptions = ImeOptions(
            singleLine = true,
            capitalization = KeyboardCapitalization.Words,
            autoCorrect = false,
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Search
        )

        var focused = false

        setContent {
            CoreTextField(
                value = value,
                imeOptions = imeOptions,
                modifier = Modifier
                    .testTag(testTag)
                    .onFocusChanged { focused = it.isFocused },
                onValueChange = {}
            )
        }

        rule.onNodeWithTag(testTag).performClick()

        rule.runOnIdle {
            assertThat(focused).isTrue()

            assertThat(platformTextInputService.inputStarted).isTrue()
            assertThat(platformTextInputService.lastInputValue).isEqualTo(value)
            assertThat(platformTextInputService.lastInputImeOptions).isEqualTo(imeOptions)
        }
    }

    @Test
    fun textField_stopsThenStartsInput_whenFocusMovesBetweenTextFields() {
        val value = TextFieldValue("abc")
        val focusRequester1 = FocusRequester()
        val focusRequester2 = FocusRequester()

        setContent {
            Column {
                CoreTextField(
                    value = value,
                    onValueChange = {},
                    modifier = Modifier.focusRequester(focusRequester1)
                )
                CoreTextField(
                    value = value,
                    onValueChange = {},
                    modifier = Modifier.focusRequester(focusRequester2)
                )
            }
        }
        rule.runOnIdle {
            focusRequester1.requestFocus()
        }

        // Focus the other field. The IME connection should restart only once.
        rule.runOnIdle {
            focusRequester2.requestFocus()
        }

        rule.runOnIdle {
            assertThat(platformTextInputService.startInputCalls).isEqualTo(2)
            assertThat(platformTextInputService.stopInputCalls).isEqualTo(1)
            assertThat(platformTextInputService.inputStarted).isTrue()
        }
    }

    @Test
    fun keyboardShownOnInitialClick() {
        // Arrange.
        setContent {
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.testTag("TextField1")
            )
        }

        // Act.
        rule.onNodeWithTag("TextField1").performClick()

        // Assert.
        rule.runOnIdle { assertThat(platformTextInputService.keyboardShown).isTrue() }
    }

    @Test
    fun keyboardShownOnInitialFocus() {
        // Arrange.
        val focusRequester = FocusRequester()
        setContent {
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.focusRequester(focusRequester)
            )
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(platformTextInputService.keyboardShown).isTrue() }
    }

    @Test
    fun keyboardHiddenWhenFocusIsLost() {
        // Arrange.
        val focusRequester = FocusRequester()
        setContent {
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.focusRequester(focusRequester)
            )
        }
        // Request focus and wait for keyboard.
        rule.runOnIdle { focusRequester.requestFocus() }
        rule.runOnIdle { assertThat(platformTextInputService.keyboardShown).isTrue() }

        // Act.
        rule.runOnIdle { focusManager.clearFocus() }

        // Assert.
        rule.runOnIdle { assertThat(platformTextInputService.keyboardShown).isFalse() }
    }

    @Test
    fun keyboardShownAfterDismissingKeyboardAndClickingAgain() {
        var keyboardShown = false
        val fakeKeyboardController = object : SoftwareKeyboardController {
            override fun show() {
                keyboardShown = true
            }

            override fun hide() {
                keyboardShown = false
            }
        }

        // Arrange.
        setContent {
            CompositionLocalProvider(
                LocalSoftwareKeyboardController provides fakeKeyboardController
            ) {
                CoreTextField(
                    value = TextFieldValue("Hello"),
                    onValueChange = {},
                    modifier = Modifier.testTag("TextField1")
                )
            }
        }
        rule.onNodeWithTag("TextField1").requestFocus()

        // Act.
        rule.onNodeWithTag("TextField1").performClick()

        // Assert.
        rule.runOnIdle { assertThat(keyboardShown).isTrue() }
    }

    @Test
    fun keyboardStaysVisibleWhenMovingFromOneTextFieldToAnother() {
        // Arrange.
        val (focusRequester1, focusRequester2) = FocusRequester.createRefs()
        setContent {
            Column {
                CoreTextField(
                    value = TextFieldValue("Hello"),
                    onValueChange = {},
                    modifier = Modifier.focusRequester(focusRequester1)
                )
                CoreTextField(
                    value = TextFieldValue("Hello"),
                    onValueChange = {},
                    modifier = Modifier.focusRequester(focusRequester2)
                )
            }
        }
        rule.runOnIdle { focusRequester1.requestFocus() }
        rule.runOnIdle { assertThat(platformTextInputService.keyboardShown).isTrue() }

        // Act.
        rule.runOnIdle { focusRequester2.requestFocus() }

        // Assert.
        rule.runOnIdle { assertThat(platformTextInputService.keyboardShown).isTrue() }
    }

    @Test
    fun keyboardHiddenWhenFieldRemovedFromComposition() {
        // Arrange.
        val focusRequester = FocusRequester()
        var composeField by mutableStateOf(true)
        setContent {
            if (composeField) {
                CoreTextField(
                    value = TextFieldValue("Hello"),
                    onValueChange = {},
                    modifier = Modifier.focusRequester(focusRequester)
                )
            }
        }
        // Request focus and wait for keyboard.
        rule.runOnIdle { focusRequester.requestFocus() }
        rule.runOnIdle { assertThat(platformTextInputService.keyboardShown).isTrue() }

        // Act.
        composeField = false

        // Assert.
        rule.runOnIdle { assertThat(platformTextInputService.keyboardShown).isFalse() }
    }

    @Test
    fun keyboardHiddenWhenFieldChangedToDisabled() {
        // Arrange.
        val focusRequester = FocusRequester()
        var enabled by mutableStateOf(true)
        setContent {
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.focusRequester(focusRequester),
                enabled = enabled
            )
        }
        // Request focus and wait for keyboard.
        rule.runOnIdle { focusRequester.requestFocus() }
        rule.runOnIdle { assertThat(platformTextInputService.keyboardShown).isTrue() }

        // Act.
        enabled = false

        // Assert.
        rule.runOnIdle { assertThat(platformTextInputService.keyboardShown).isFalse() }
    }

    @Test
    fun keyboardHiddenWhenFieldChangedToReadOnly() {
        // Arrange.
        val focusRequester = FocusRequester()
        var readOnly by mutableStateOf(false)
        setContent {
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.focusRequester(focusRequester),
                readOnly = readOnly
            )
        }
        // Request focus and wait for keyboard.
        rule.runOnIdle { focusRequester.requestFocus() }
        rule.runOnIdle { assertThat(platformTextInputService.keyboardShown).isTrue() }

        // Act.
        readOnly = true

        // Assert.
        rule.runOnIdle { assertThat(platformTextInputService.keyboardShown).isFalse() }
    }

    @Test
    fun keyboardShownWhenFieldChangedToWritableWhileFocused() {
        // Arrange.
        val focusRequester = FocusRequester()
        var readOnly by mutableStateOf(true)
        setContent {
            CoreTextField(
                value = TextFieldValue("Hello"),
                onValueChange = {},
                modifier = Modifier.focusRequester(focusRequester),
                readOnly = readOnly
            )
        }
        // Request focus and wait for keyboard.
        rule.runOnIdle { focusRequester.requestFocus() }
        rule.runOnIdle { assertThat(platformTextInputService.keyboardShown).isFalse() }

        // Act.
        readOnly = false

        // Assert.
        rule.runOnIdle { assertThat(platformTextInputService.keyboardShown).isTrue() }
    }

    @Test
    fun focusedRectIsPassedOnFocus() {
        val value = TextFieldValue("abc\nefg", TextRange(6))
        lateinit var textLayoutResult: TextLayoutResult
        val focusRequester = FocusRequester()

        setContent {
            CoreTextField(
                value = value,
                modifier = Modifier.focusRequester(focusRequester),
                onValueChange = { },
                onTextLayout = { textLayoutResult = it }
            )
        }

        rule.runOnIdle {
            assertThat(platformTextInputService.focusedRect).isEqualTo(null)
        }

        rule.runOnUiThread {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(platformTextInputService.focusedRect)
                .isEqualTo(textLayoutResult.getBoundingBox(6))
        }
    }

    @Test
    fun focusedRectIsPassedOnGlobalPositionChanged() {
        var offset by mutableStateOf(IntOffset(0, 10))
        val value = TextFieldValue("abc\nefg", TextRange(6))
        lateinit var textLayoutResult: TextLayoutResult
        val focusRequester = FocusRequester()

        setContent {
            Box(Modifier.offset { offset }) {
                CoreTextField(
                    value = value,
                    modifier = Modifier.focusRequester(focusRequester),
                    onValueChange = { },
                    onTextLayout = { textLayoutResult = it }
                )
            }
        }

        rule.runOnIdle {
            assertThat(platformTextInputService.focusedRect).isEqualTo(null)
        }

        rule.runOnUiThread {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(platformTextInputService.focusedRect).isEqualTo(
                textLayoutResult.getBoundingBox(6).translate(offset.toOffset())
            )
        }

        offset = IntOffset(10, 20)

        rule.runOnIdle {
            assertThat(platformTextInputService.focusedRect).isEqualTo(
                textLayoutResult.getBoundingBox(6).translate(offset.toOffset())
            )
        }
    }

    @Test
    fun focusedRectIsPassedOnValueChange() {
        val tag = "TextField1"
        var value by mutableStateOf(TextFieldValue(""))
        lateinit var textLayoutResult: TextLayoutResult

        setContent {
            CoreTextField(
                value = value,
                modifier = Modifier.testTag(tag),
                onValueChange = { value = it },
                onTextLayout = { textLayoutResult = it }
            )
        }

        rule.onNodeWithTag(tag).performClick()
        rule.runOnIdle {
            assertThat(platformTextInputService.focusedRect?.topLeft)
                .isEqualTo(Offset.Zero)
        }

        value = TextFieldValue("a", TextRange(1))
        rule.runOnIdle {
            assertThat(platformTextInputService.focusedRect)
                .isEqualTo(textLayoutResult.getBoundingBox(0))
        }

        value = TextFieldValue("a\nbc", TextRange(4))
        rule.runOnIdle {
            assertThat(platformTextInputService.focusedRect)
                .isEqualTo(textLayoutResult.getBoundingBox(3))
        }

        value = TextFieldValue("a\nbc", TextRange(3))
        rule.runOnIdle {
            assertThat(platformTextInputService.focusedRect)
                .isEqualTo(textLayoutResult.getBoundingBox(3))
        }

        value = TextFieldValue("a\nbc", TextRange(2))
        rule.runOnIdle {
            assertThat(platformTextInputService.focusedRect)
                .isEqualTo(textLayoutResult.getBoundingBox(2))
        }

        value = TextFieldValue("a\nbc", TextRange(0))
        rule.runOnIdle {
            assertThat(platformTextInputService.focusedRect)
                .isEqualTo(textLayoutResult.getBoundingBox(0))
        }
    }

    @Test
    fun updateTextLayoutResultCalledOnGlobalPositionChanged() {
        var offset by mutableStateOf(IntOffset(0, 10))
        val value = TextFieldValue("abc\nefg", TextRange(6))
        lateinit var textLayoutResult: TextLayoutResult
        val focusRequester = FocusRequester()
        val matrix = Matrix()

        setContent {
            Box(Modifier.offset { offset }) {
                CoreTextField(value = value,
                    modifier = Modifier.focusRequester(focusRequester),
                    onValueChange = { },
                    onTextLayout = { textLayoutResult = it })
            }
        }

        rule.runOnIdle {
            assertThat(platformTextInputService.lastInputValue).isNull()
            assertThat(platformTextInputService.offsetMapping).isNull()
            assertThat(platformTextInputService.textLayoutResult).isNull()
            assertThat(platformTextInputService.textFieldToRootTransform).isNull()
            assertThat(platformTextInputService.innerTextFieldBounds).isNull()
            assertThat(platformTextInputService.decorationBoxBounds).isNull()
        }

        rule.runOnUiThread {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(platformTextInputService.lastInputValue).isEqualTo(value)
            assertThat(platformTextInputService.offsetMapping).isNotNull()
            assertThat(platformTextInputService.textLayoutResult).isNotNull()
            assertThat(platformTextInputService.textLayoutResult).isEqualTo(textLayoutResult)
            platformTextInputService.textFieldToRootTransform!!.invoke(matrix)
            assertThat(matrix.values).isEqualTo(Matrix().apply {
                translate(offset.x.toFloat(), offset.y.toFloat())
            }.values)
            assertThat(platformTextInputService.innerTextFieldBounds).isNotNull()
            assertThat(platformTextInputService.decorationBoxBounds).isNotNull()
        }

        offset = IntOffset(10, 20)

        rule.runOnIdle {
            platformTextInputService.textFieldToRootTransform!!.invoke(matrix)
            assertThat(matrix.values).isEqualTo(Matrix().apply {
                translate(offset.x.toFloat(), offset.y.toFloat())
            }.values)
        }
    }

    private fun setContent(content: @Composable () -> Unit) {
        rule.setContent {
            focusManager = LocalFocusManager.current
            CompositionLocalProvider(
                LocalTextInputService provides textInputService,
                content = content
            )
        }
    }

    private class FakePlatformTextInputService : PlatformTextInputService {
        var startInputCalls = 0
        var stopInputCalls = 0
        var inputStarted = false
        var keyboardShown = false
        var focusedRect: Rect? = null

        var lastInputValue: TextFieldValue? = null
        var lastInputImeOptions: ImeOptions? = null

        var offsetMapping: OffsetMapping? = null
        var textLayoutResult: TextLayoutResult? = null
        var textFieldToRootTransform: ((Matrix) -> Unit)? = null
        var innerTextFieldBounds: Rect? = null
        var decorationBoxBounds: Rect? = null

        override fun startInput(
            value: TextFieldValue,
            imeOptions: ImeOptions,
            onEditCommand: (List<EditCommand>) -> Unit,
            onImeActionPerformed: (ImeAction) -> Unit
        ) {
            startInputCalls++
            inputStarted = true
            keyboardShown = true
            lastInputValue = value
            lastInputImeOptions = imeOptions
        }

        override fun stopInput() {
            stopInputCalls++
            inputStarted = false
            keyboardShown = false
        }

        override fun showSoftwareKeyboard() {
            keyboardShown = true
        }

        override fun hideSoftwareKeyboard() {
            keyboardShown = false
        }

        override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
            // Tests don't care.
        }

        override fun notifyFocusedRect(rect: Rect) {
            focusedRect = rect
        }

        override fun updateTextLayoutResult(
            textFieldValue: TextFieldValue,
            offsetMapping: OffsetMapping,
            textLayoutResult: TextLayoutResult,
            textFieldToRootTransform: (Matrix) -> Unit,
            innerTextFieldBounds: Rect,
            decorationBoxBounds: Rect
        ) {
            lastInputValue = textFieldValue
            this.offsetMapping = offsetMapping
            this.textLayoutResult = textLayoutResult
            this.textFieldToRootTransform = textFieldToRootTransform
            this.innerTextFieldBounds = innerTextFieldBounds
            this.decorationBoxBounds = decorationBoxBounds
        }
    }
}
