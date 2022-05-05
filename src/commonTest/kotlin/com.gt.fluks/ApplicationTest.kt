@file:OptIn(ExperimentalCoroutinesApi::class)

package com.gt.fluks

import kotlinx.coroutines.ExperimentalCoroutinesApi
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
                on<String> { state ->
                    AppState(count = state.count * 2)
                }
            }
        }

        assertEquals(initState, app.stateStream.value)
        app.dispatch("string data")
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
                on<Int> { state, event ->
                    AppState(count = state.count + event)
                }

                on<Modulo> { state, event ->
                    state.copy(count = state.count % event.value)
                }
            }
        }

        app.dispatch(3)
        assertEquals(5, app.stateStream.value.count)

        app.dispatch(Modulo(2))
        assertEquals(1, app.stateStream.value.count)
    }

    data class Modulo(val value: Int)
    data class AppState(val count: Int)
}