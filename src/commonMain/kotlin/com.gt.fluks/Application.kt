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

    inline fun <reified E> on(noinline handler: (STATE) -> STATE) {
        on<E> { state, _ -> handler(state) }
    }

    inline fun <reified E> on(noinline handler: (STATE, E) -> STATE) {
        val eventKey = E::class.simpleName ?: throw Exception("Event class must have a name")
        eventHandlers[eventKey] = handler as (STATE, Any) -> STATE
    }

}

interface Application<STATE> {
    val stateStream: StateFlow<STATE>
    suspend fun dispatch(event: Any)
}

class ApplicationImpl<STATE>(
    initState: STATE,
    private val eventHandlers: Map<String, (STATE, Any) -> STATE>
) : Application<STATE> {
    private val _state = MutableStateFlow(initState)

    override val stateStream = _state.asStateFlow()

    override suspend fun dispatch(event: Any) {
        val eventKey = event::class.simpleName
        val handler = eventHandlers[eventKey]
            ?: throw Exception("No registered handler for event '$eventKey'")
        _state.emit(handler(_state.value, event))
    }
}