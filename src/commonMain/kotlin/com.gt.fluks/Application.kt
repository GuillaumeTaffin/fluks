package com.gt.fluks

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

fun <STATE> application(
    initState: STATE,
    block: ApplicationBuilder<STATE>.() -> Unit = {}
): Application<STATE> {
    val builder = ApplicationBuilder(initState)
    builder.block()
    return builder.build()
}

class ApplicationBuilder<STATE>(
    private val initState: STATE
) {

    private val events = Events<STATE>()

    fun build(): Application<STATE> {
        return ApplicationImpl(initState, events.eventHandlers)
    }

    fun events(bloc: Events<STATE>.() -> Unit) {
        events.bloc()
    }
}

class Events<STATE> {

    val eventHandlers = mutableMapOf<String, (STATE, Any) -> STATE>()

    fun on(eventKey: String, handler: (STATE) -> STATE) {
        eventHandlers[eventKey] = { state, _ -> handler(state) }
    }

    fun <E> on(eventKey: String, handler: (STATE, E) -> STATE) {
        eventHandlers[eventKey] = handler as (STATE, Any) -> STATE
    }

}

typealias EventHandler<STATE> = (STATE) -> STATE

interface Application<STATE> {
    val stateStream: StateFlow<STATE>
    suspend fun dispatch(eventKey: String, data: Any? = null)
}

class ApplicationImpl<STATE>(
    initState: STATE,
    private val eventHandlers: Map<String, (STATE, Any) -> STATE>
) : Application<STATE> {
    private val _state = MutableStateFlow(initState)

    override val stateStream = _state.asStateFlow()

    override suspend fun dispatch(eventKey: String, data: Any?) {
        val handler = eventHandlers[eventKey] ?: throw Exception("No registered handler for event '$eventKey'")
        _state.emit(handler(_state.value, data ?: {}))
    }
}