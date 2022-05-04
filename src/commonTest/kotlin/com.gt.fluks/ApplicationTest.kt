package com.gt.fluks

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ApplicationTest {

    @Test
    fun stateIsProvidedByTheApplicationOnSubscription() {
        val initState = AppState(count = 2)
        val app = application(initState)

        val givenState = app.stateStream.value

        assertEquals(initState, givenState)
    }

    @Test
    fun dispatchRegisteredEventAndGetUpdatedState() = runTest {
        val initState = AppState(count = 4)
        val app = application(initState) {
            events {
                on("my-event") { state ->
                    AppState(count = state.count * 2)
                }
            }
        }

        assertEquals(initState, app.stateStream.value)
        app.dispatch("my-event")
        assertEquals(8, app.stateStream.value.count)
    }

    @Test
    fun dispatchingNotHandledEventThrows() = runTest {
        val initState = AppState(3)
        val app = application(initState)

        assertFails {
            app.dispatch("unknown")
        }
    }

    @Test
    fun canDispatchAndExpectPayload() = runTest {
        val initState = AppState(2)
        val app = application(initState) {
            events {
                on<Int>("add-value") { state, event ->
                    AppState(count = state.count + event)
                }
            }
        }

        app.dispatch("add-value", 3)
        assertEquals(5, app.stateStream.value.count)

    }

    data class AppState(val count: Int)
}