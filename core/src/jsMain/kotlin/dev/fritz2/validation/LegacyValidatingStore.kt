package dev.fritz2.validation

import dev.fritz2.core.EmittingHandler
import dev.fritz2.core.Handler
import dev.fritz2.core.Id
import dev.fritz2.core.RootStore
import dev.fritz2.core.Store
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map

/**
 * A [LegacyValidatingStore] is a [Store] which also contains a [Validation] for its model and by default applies it
 * to every update.
 *
 * This store is intentionally configured to validate the data on each update, that is why the [validateAfterUpdate]
 * parameter is set to `true` by default.
 *
 * There might be special situations where it is reasonable to disable this behaviour by setting [validateAfterUpdate]
 * to `false` and to prefer applying the validation individually within custom handlers, for example if a model should
 * only be validated after the user has completed his input or if metadata is needed for the validation process.
 * Then be aware of the fact, that the call of the [validate] function actually updates the [messages] [Flow] already.
 *
 * In order for the automatic validation to work, a [metadataDefault] value must be specified. This is needed due to no
 * specific metadata being present during automatic validation. When calling [validate] manually, the appropriate
 * metadata can be supplied directly.
 *
 * If the new data is not passed to the store's state after validating it, the messages are probably out of sync with
 * the actual store's state!
 * This could lead to false assumptions and might produce hard to detect bugs in your application.
 *
 * @param initialData first current value of this [Store]
 * @param validation [Validation] function to use at the data on this [Store].
 * @param metadataDefault default metadata to be used by the automatic validation (where no explicit values are given)
 * @param job Job to be used by the [Store]
 * @param validateAfterUpdate flag to decide if a new value gets automatically validated after setting it to the [Store].
 * @param id id of this [Store]. Ids of parent [Store]s will be concatenated.
 */
open class LegacyValidatingStore<D, T, M>(
    initialData: D,
    private val validation: Validation<D, T, M>,
    private val metadataDefault: T,
    job: Job,
    private val validateAfterUpdate: Boolean = true,
    override val id: String = Id.next(),
) : RootStore<D>(initialData, job, id), ValidatingStore<D, T, M> {

    private val validationMessages: MutableStateFlow<List<M>> = MutableStateFlow(emptyList())

    /**
     * [Flow] of the [List] of validation-messages.
     * Use this [Flow] to render out the validation-messages and to detect the valid state of the current data Flow.
     */
    val messages: Flow<List<M>> = validationMessages.asStateFlow()

    override val validate: EmittingHandler<T, List<M>> = handleOnlyEmit { state, meta ->
        emit(doValidate(state, meta))
    }

    /**
     * Resets the validation result.
     *
     * Beware that cleaning the messages should not be done, if the data [Flow] remains in an invalid state.
     * Please refer to the class's description for details about the need for a sound data and messages state.
     *
     * @param messages list of messages to reset to. Default is an empty list.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    protected fun resetMessages(messages: List<M> = emptyList()) {
        validationMessages.value = messages
    }

    /**
     * Validates the given [data] using the given [metadata], updates the [messages] list and returns them.
     * If no metadata is specified, [metadataDefault] is used.
     *
     * Use this method from inside your [Handler]s to publish
     * the new state of the validation result via the [messages] flow.
     *
     * @param data data to validate
     * @param metadata metadata for validation
     * @return [List] of messages
     */
    @Deprecated(
        message = "Diese interne Validierungsmethode wurde umbenannt. Nutze stattdessen `doValidate`.",
        replaceWith = ReplaceWith("doValidate(data, metadata)")
    )
    protected fun validate(data: D, metadata: T = metadataDefault): List<M> = doValidate(data, metadata)

    /**
     * Validates the given [data] using the given [metadata], updates the [messages] list and returns them.
     * If no metadata is specified, [metadataDefault] is used.
     *
     * Use this method from inside your [Handler]s to publish
     * the new state of the validation result via the [messages] flow.
     *
     * @param data data to validate
     * @param metadata metadata for validation
     * @return [List] of messages
     */
    @Suppress("MemberVisibilityCanBePrivate")
    protected open fun doValidate(data: D, metadata: T = metadataDefault): List<M> =
        validation(data, metadata).also { validationMessages.value = it }

    init {
        if (validateAfterUpdate) data.drop(1).map { metadataDefault } handledBy validate
    }
}
