/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import kotlin.test.Test
import kotlin.test.assertEquals

class SnapshotStateObserverTestsCommon {

    @Test
    fun stateChangeTriggersCallback() {
        val data = "Hello World"
        var changes = 0

        val state = mutableStateOf(0)
        val stateObserver = SnapshotStateObserver { it() }
        try {
            stateObserver.start()

            val onChangeListener: (String) -> Unit = { affected ->
                assertEquals(data, affected)
                assertEquals(0, changes)
                changes++
            }

            stateObserver.observeReads(data, onChangeListener) {
                // read the value
                state.value
            }

            Snapshot.notifyObjectsInitialized()
            state.value++
            Snapshot.sendApplyNotifications()

            assertEquals(1, changes)
        } finally {
            stateObserver.stop()
        }
    }

    @Test
    fun multipleStagesWorksTogether() {
        val strStage1 = "Stage1"
        val strStage2 = "Stage2"
        val strStage3 = "Stage3"
        var stage1Changes = 0
        var stage2Changes = 0
        var stage3Changes = 0
        val stage1Model = mutableStateOf(0)
        val stage2Model = mutableStateOf(0)
        val stage3Model = mutableStateOf(0)

        val onChangeStage1: (String) -> Unit = { affectedData ->
            assertEquals(strStage1, affectedData)
            assertEquals(0, stage1Changes)
            stage1Changes++
        }
        val onChangeStage2: (String) -> Unit = { affectedData ->
            assertEquals(strStage2, affectedData)
            assertEquals(0, stage2Changes)
            stage2Changes++
        }
        val onChangeStage3: (String) -> Unit = { affectedData ->
            assertEquals(strStage3, affectedData)
            assertEquals(0, stage3Changes)
            stage3Changes++
        }
        val stateObserver = SnapshotStateObserver { it() }
        try {
            stateObserver.start()

            stateObserver.observeReads(strStage1, onChangeStage1) {
                stage1Model.value
            }

            stateObserver.observeReads(strStage2, onChangeStage2) {
                stage2Model.value
            }

            stateObserver.observeReads(strStage3, onChangeStage3) {
                stage3Model.value
            }

            Snapshot.notifyObjectsInitialized()

            stage1Model.value++
            stage2Model.value++
            stage3Model.value++

            Snapshot.sendApplyNotifications()

            assertEquals(1, stage1Changes)
            assertEquals(1, stage2Changes)
            assertEquals(1, stage3Changes)
        } finally {
            stateObserver.stop()
        }
    }

    @Test
    fun enclosedStagesCorrectlyObserveChanges() {
        val stage1Info = "stage 1"
        val stage2Info1 = "stage 1 - value 1"
        val stage2Info2 = "stage 2 - value 2"
        var stage1Changes = 0
        var stage2Changes1 = 0
        var stage2Changes2 = 0
        val stage1Data = mutableStateOf(0)
        val stage2Data1 = mutableStateOf(0)
        val stage2Data2 = mutableStateOf(0)

        val onChangeStage1Listener: (String) -> Unit = { affected ->
            assertEquals(affected, stage1Info)
            assertEquals(stage1Changes, 0)
            stage1Changes++
        }
        val onChangeState2Listener: (String) -> Unit = { affected ->
            when (affected) {
                stage2Info1 -> {
                    assertEquals(0, stage2Changes1)
                    stage2Changes1++
                }
                stage2Info2 -> {
                    assertEquals(0, stage2Changes2)
                    stage2Changes2++
                }
                stage1Info -> {
                    error("stage 1 called in stage 2")
                }
            }
        }

        val stateObserver = SnapshotStateObserver { it() }
        try {
            stateObserver.start()

            stateObserver.observeReads(stage2Info1, onChangeState2Listener) {
                stage2Data1.value
                stateObserver.observeReads(stage2Info2, onChangeState2Listener) {
                    stage2Data2.value
                    stateObserver.observeReads(stage1Info, onChangeStage1Listener) {
                        stage1Data.value
                    }
                }
            }

            Snapshot.notifyObjectsInitialized()

            stage2Data1.value++
            stage2Data2.value++
            stage1Data.value++

            Snapshot.sendApplyNotifications()

            assertEquals(1, stage1Changes)
            assertEquals(1, stage2Changes1)
            assertEquals(1, stage2Changes2)
        } finally {
            stateObserver.stop()
        }
    }

    @Test
    fun stateReadTriggersCallbackAfterSwitchingAdvancingGlobalWithinObserveReads() {
        val info = "Hello"
        var changes = 0

        val state = mutableStateOf(0)
        val onChangeListener: (String) -> Unit = { _ ->
            assertEquals(0, changes)
            changes++
        }

        val stateObserver = SnapshotStateObserver { it() }
        try {
            stateObserver.start()

            stateObserver.observeReads(info, onChangeListener) {
                // Create a sub-snapshot
                // this will be done by subcomposition, for example.
                val snapshot = Snapshot.takeMutableSnapshot()
                try {
                    // read the value
                    snapshot.enter { state.value }
                    snapshot.apply().check()
                } finally {
                    snapshot.dispose()
                }
            }

            state.value++

            Snapshot.sendApplyNotifications()

            assertEquals(1, changes)
        } finally {
            stateObserver.stop()
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun pauseStopsObserving() {
        val data = "data"
        var changes = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { changes++ }) {
                stateObserver.withNoObservations {
                    state.value
                }
            }
        }

        assertEquals(0, changes)
    }

    @Test
    fun withoutReadObservationStopsObserving() {
        val data = "data"
        var changes = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { changes++ }) {
                Snapshot.withoutReadObservation {
                    state.value
                }
            }
        }

        assertEquals(0, changes)
    }

    @Test
    fun changeAfterWithoutReadObservationIsObserving() {
        val data = "data"
        var changes = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { changes++ }) {
                Snapshot.withoutReadObservation {
                    state.value
                }
                state.value
            }
        }

        assertEquals(1, changes)
    }

    @Suppress("DEPRECATION")
    @Test
    fun nestedPauseStopsObserving() {
        val data = "data"
        var changes = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { _ -> changes++ }) {
                stateObserver.withNoObservations {
                    stateObserver.withNoObservations {
                        state.value
                    }
                    state.value
                }
            }
        }

        assertEquals(0, changes)
    }

    @Test
    fun nestedWithoutReadObservation() {
        val data = "data"
        var changes = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { changes++ }) {
                Snapshot.withoutReadObservation {
                    Snapshot.withoutReadObservation {
                        state.value
                    }
                    state.value
                }
            }
        }

        assertEquals(0, changes)
    }

    @Test
    fun simpleObserving() {
        val data = "data"
        var changes = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { _ -> changes++ }) {
                state.value
            }
        }

        assertEquals(1, changes)
    }

    @Suppress("DEPRECATION")
    @Test
    fun observeWithinPause() {
        val data = "data"
        var changes1 = 0
        var changes2 = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { _ -> changes1++ }) {
                stateObserver.withNoObservations {
                    stateObserver.observeReads(data, { _ -> changes2++ }) {
                        state.value
                    }
                }
            }
        }
        assertEquals(0, changes1)
        assertEquals(1, changes2)
    }

    @Test
    fun observeWithinWithoutReadObservation() {
        val data = "data"
        var changes1 = 0
        var changes2 = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { changes1++ }) {
                Snapshot.withoutReadObservation {
                    stateObserver.observeReads(data, { changes2++ }) {
                        state.value
                    }
                }
            }
        }
        assertEquals(0, changes1)
        assertEquals(1, changes2)
    }

    @Test
    fun withoutReadsPausesNestedObservation() {
        var changes1 = 0
        var changes2 = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads("scope1", { changes1++ }) {
                stateObserver.observeReads("scope2", { changes2++ }) {
                    Snapshot.withoutReadObservation {
                        state.value
                    }
                }
            }
        }
        assertEquals(0, changes1)
        assertEquals(0, changes2)
    }

    @Test
    fun withoutReadsPausesNestedObservationWhenNewMutableSnapshotIsEnteredWithin() {
        var changes1 = 0
        var changes2 = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads("scope1", { changes1++ }) {
                stateObserver.observeReads("scope2", { changes2++ }) {
                    Snapshot.withoutReadObservation {
                        val newSnapshot = Snapshot.takeMutableSnapshot()
                        newSnapshot.enter {
                            state.value
                        }
                        newSnapshot.apply().check()
                        newSnapshot.dispose()
                    }
                }
            }
        }
        assertEquals(0, changes1)
        assertEquals(0, changes2)
    }

    @Test
    fun withoutReadsPausesNestedObservationWhenNewSnapshotIsEnteredWithin() {
        var changes1 = 0
        var changes2 = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads("scope1", { changes1++ }) {
                stateObserver.observeReads("scope2", { changes2++ }) {
                    Snapshot.withoutReadObservation {
                        val newSnapshot = Snapshot.takeSnapshot()
                        newSnapshot.enter {
                            state.value
                        }
                        newSnapshot.dispose()
                    }
                }
            }
        }
        assertEquals(0, changes1)
        assertEquals(0, changes2)
    }

    @Test
    fun withoutReadsInReadOnlySnapshot() {
        var changes = 0

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads("scope", { changes++ }) {
                val newSnapshot = Snapshot.takeSnapshot()
                newSnapshot.enter {
                    Snapshot.withoutReadObservation {
                        state.value
                    }
                }
                newSnapshot.dispose()
            }
        }
        assertEquals(0, changes)
    }

    @Test
    fun derivedStateOfInvalidatesObserver() {
        var changes = 0

        runSimpleTest { stateObserver, state ->
            val derivedState = derivedStateOf { state.value }

            stateObserver.observeReads("scope", { changes++ }) {
                // read
                derivedState.value
            }
        }
        assertEquals(1, changes)
    }

    @Test
    fun derivedStateOfReferentialChangeDoesNotInvalidateObserver() {
        var changes = 0

        runSimpleTest { stateObserver, _ ->
            val state = mutableStateOf(mutableListOf(42), referentialEqualityPolicy())
            val derivedState = derivedStateOf { state.value }

            stateObserver.observeReads("scope", { changes++ }) {
                // read
                derivedState.value
            }

            state.value = mutableListOf(42)
        }
        assertEquals(0, changes)
    }

    @Test
    fun nestedDerivedStateOfInvalidatesObserver() {
        var changes = 0

        runSimpleTest { stateObserver, state ->
            val derivedState = derivedStateOf { state.value }
            val derivedState2 = derivedStateOf { derivedState.value }

            stateObserver.observeReads("scope", { changes++ }) {
                // read
                derivedState2.value
            }
        }
        assertEquals(1, changes)
    }

    @Test
    fun derivedStateOfWithReferentialMutationPolicy() {
        var changes = 0

        runSimpleTest { stateObserver, _ ->
            val state = mutableStateOf(mutableListOf(1), referentialEqualityPolicy())
            val derivedState = derivedStateOf(referentialEqualityPolicy()) { state.value }

            stateObserver.observeReads("scope", { changes++ }) {
                // read
                derivedState.value
            }

            state.value = mutableListOf(1)
        }
        assertEquals(1, changes)
    }

    @Test
    fun derivedStateOfWithStructuralMutationPolicy() {
        var changes = 0

        runSimpleTest { stateObserver, _ ->
            val state = mutableStateOf(mutableListOf(1), referentialEqualityPolicy())
            val derivedState = derivedStateOf(structuralEqualityPolicy()) { state.value }

            stateObserver.observeReads("scope", { changes++ }) {
                // read
                derivedState.value
            }

            state.value = mutableListOf(1)
        }
        assertEquals(0, changes)
    }

    @Test
    fun readingDerivedStateAndDependencyInvalidates() {
        var changes = 0

        runSimpleTest { stateObserver, state ->
            val derivedState = derivedStateOf { state.value >= 0 }

            stateObserver.observeReads("scope", { changes++ }) {
                // read derived state
                derivedState.value
                // read dependency
                state.value
            }
        }
        assertEquals(1, changes)
    }

    @Test
    fun readingDerivedStateWithDependencyChangeInvalidates() {
        var changes = 0

        runSimpleTest { stateObserver, state ->
            val state2 = mutableStateOf(false)
            val derivedState = derivedStateOf {
                if (state2.value) {
                    state.value
                } else {
                    null
                }
            }
            val onChange: (String) -> Unit = { changes++ }

            stateObserver.observeReads("scope", onChange) {
                // read derived state
                derivedState.value
            }

            state2.value = true
            // advance snapshot
            Snapshot.sendApplyNotifications()
            Snapshot.notifyObjectsInitialized()

            stateObserver.observeReads("scope", onChange) {
                // read derived state
                derivedState.value
            }
        }
        assertEquals(2, changes)
    }

    @Test
    fun readingDerivedStateConditionallyInvalidatesBothScopes() {
        var changes = 0

        runSimpleTest { stateObserver, state ->
            val derivedState = derivedStateOf { state.value }

            val onChange: (String) -> Unit = { changes++ }
            stateObserver.observeReads("scope", onChange) {
                // read derived state
                derivedState.value
            }

            // read the same state in other scope
            stateObserver.observeReads("other scope", onChange) {
                derivedState.value
            }

            // advance snapshot to invalidate reads
            Snapshot.notifyObjectsInitialized()

            // stop observing state in other scope
            stateObserver.observeReads("other scope", onChange) {
                /* no-op */
            }
        }
        assertEquals(1, changes)
    }

    @Test
    fun testRecursiveApplyChanges_SingleRecursive() {
        val stateObserver = SnapshotStateObserver { it() }
        val state1 = mutableStateOf(0)
        val state2 = mutableStateOf(0)
        try {
            stateObserver.start()
            Snapshot.notifyObjectsInitialized()

            val onChange: (String) -> Unit = { scope ->
                if (scope == "scope" && state1.value < 2) {
                    state1.value++
                    Snapshot.sendApplyNotifications()
                }
            }

            stateObserver.observeReads("scope", onChange) {
                state1.value
                state2.value
            }

            repeat(10) {
                stateObserver.observeReads("scope $it", onChange) {
                    state1.value
                    state2.value
                }
            }

            state1.value++
            state2.value++

            Snapshot.sendApplyNotifications()
        } finally {
            stateObserver.stop()
        }
    }

    @Test
    fun testRecursiveApplyChanges_MultiRecursive() {
        val stateObserver = SnapshotStateObserver { it() }
        val state1 = mutableStateOf(0)
        val state2 = mutableStateOf(0)
        val state3 = mutableStateOf(0)
        val state4 = mutableStateOf(0)
        try {
            stateObserver.start()
            Snapshot.notifyObjectsInitialized()

            val onChange: (String) -> Unit = { scope ->
                if (scope == "scope" && state1.value < 2) {
                    state1.value++
                    Snapshot.sendApplyNotifications()
                    state2.value++
                    Snapshot.sendApplyNotifications()
                    state3.value++
                    Snapshot.sendApplyNotifications()
                    state4.value++
                    Snapshot.sendApplyNotifications()
                }
            }

            stateObserver.observeReads("scope", onChange) {
                state1.value
                state2.value
                state3.value
                state4.value
            }

            repeat(10) {
                stateObserver.observeReads("scope $it", onChange) {
                    state1.value
                    state2.value
                    state3.value
                    state4.value
                }
            }

            state1.value++
            state2.value++
            state3.value++
            state4.value++

            Snapshot.sendApplyNotifications()
        } finally {
            stateObserver.stop()
        }
    }

    @Test
    fun readingValueAfterClearInvalidates() {
        var changes = 0

        runSimpleTest { stateObserver, state ->
            val changeBlock: (Any) -> Unit = { changes++ }
            // record observation
            stateObserver.observeReads("scope", changeBlock) {
                // read state
                state.value
            }

            // clear scope
            stateObserver.clear("scope")

            // record again
            stateObserver.observeReads("scope", changeBlock) {
                // read state
                state.value
            }
        }
        assertEquals(1, changes)
    }

    @Test
    fun readingDerivedState_invalidatesWhenValueNotChanged() {
        var changes = 0
        val changeBlock: (Any) -> Unit = { changes++ }

        runSimpleTest { stateObserver, state ->
            var condition by mutableStateOf(false)
            val derivedState = derivedStateOf {
                // the same initial value for both branches
                if (condition) state.value else 0
            }

            // record observation
            stateObserver.observeReads("scope", changeBlock) {
                // read state
                derivedState.value
            }

            condition = true
            Snapshot.sendApplyNotifications()
        }
        assertEquals(1, changes)
    }

    @Test
    fun readingDerivedState_invalidatesIfReadBeforeSnapshotAdvance() {
        var changes = 0
        val changeBlock: (Any) -> Unit = {
            if (it == "draw_1") {
                changes++
            }
        }

        runSimpleTest { stateObserver, layoutState ->
            val derivedState = derivedStateOf {
                layoutState.value
            }

            // record observation for a draw scope
            stateObserver.observeReads("draw", changeBlock) {
                derivedState.value
            }

            // record observation for a different draw scope
            stateObserver.observeReads("draw_1", changeBlock) {
                derivedState.value
            }

            Snapshot.sendApplyNotifications()

            // record
            layoutState.value += 1

            // record observation for the first draw scope
            stateObserver.observeReads("draw", changeBlock) {
                // read state
                derivedState.value
            }

            // second block should be invalidated after we read the value
            assertEquals(1, changes)

            // record observation for the second draw scope
            stateObserver.observeReads("draw_1", changeBlock) {
                // read state
                derivedState.value
            }
        }
        assertEquals(2, changes)
    }

    private fun runSimpleTest(
        block: (modelObserver: SnapshotStateObserver, data: MutableState<Int>) -> Unit
    ) {
        val stateObserver = SnapshotStateObserver { it() }
        val state = mutableStateOf(0)
        try {
            stateObserver.start()
            Snapshot.notifyObjectsInitialized()
            block(stateObserver, state)
            state.value++
            Snapshot.sendApplyNotifications()
        } finally {
            stateObserver.stop()
        }
    }
}
