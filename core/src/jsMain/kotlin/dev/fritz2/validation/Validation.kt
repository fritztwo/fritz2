@file:Suppress("unused")

package dev.fritz2.validation

import dev.fritz2.core.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*


/**
 * A [ValidatingStore] is a [Store] which also contains a [Validation] for its model applies it to every update.
 *
 * You must provide some [metadata], which is reactive and thus by default provided by a [Flow]. There exist an
 * alternative variant, which accepts a *static* value.
 *
 * This store is intentionally configured to validate the data on each update. If the [metadata] changes,
 * the validation process is also triggered automatically.
 *
 * In order for the automatic validation to work, a [metadata] value must be specified. This is needed due to no
 * specific metadata being present during automatic validation. When calling [validate] handler manually, the
 * appropriate metadata must be supplied directly.
 *
 * @param initialData first current value of this [Store]
 * @property validation [Validation] function to use at the data on this [Store].
 * @property metadata [Flow] of metadata to be used by the automatic validation. If you habe only a static object,
 * use the overloaded constructor, which supports this too.
 * @param job Job to be used by the [Store]
 * @property id id of this [Store]. Ids of parent [Store]s will be concatenated.
 */
open class ValidatingStore<D, T, M>(
    initialData: D,
    protected val validation: Validation<D, T, M>,
    private val metadata: Flow<T>,
    job: Job,
    override val id: String = Id.next(),
) : RootStore<D>(initialData, job, id) {

    constructor(
        initialData: D,
        validation: Validation<D, T, M>,
        metadata: T,
        job: Job,
        id: String = Id.next(),
    ) : this(initialData, validation, flowOf(metadata), job, id)

    /**
     * This property allows for manipulating the automatic validation.
     *
     * Please note that this property may be deleted in the future, so use this with caution: Consider to change the
     * behavior as soon as you can and just use this as an intermediate workaround.
     *
     * By default, both the [data] flow and the [metadata] flow automatically trigger the
     * [validate] handler upon change, consequently updating the [validation messages][messages].
     *
     * However, this lambda expression is applied to the combined flow beforehand, allowing you
     * to change the behavior by overriding it.
     *
     * The following scenarios are typical for restoring the behavior implemented up until RC21 — where
     * the initial value is not validated — or for re-implementing the removed `validateAfterUpdate = false` option:
     *
     * ```kotlin
     * // stop validating initial state
     * val store = object : ValidatingStore(...) {
     *     override val modifier = { it.drop(1) }
     * }
     *
     * // do not validate automatically on state changes
     * val store = object : ValidatingStore(...) {
     *     override val modifier = { it.dropWhile(true) }
     * }
     * ```
     */
    protected open val modifier: (Flow<T>) -> Flow<T> = { it }

    private val validationMessages: MutableStateFlow<List<M>> = MutableStateFlow(emptyList())

    /**
     * [Flow] of the [List] of validation-messages.
     * Use this [Flow] to render out the validation-messages and to detect the valid state of the current [data] [Flow].
     */
    val messages: Flow<List<M>> = validationMessages.asStateFlow()

    /**
     * a [Handler] that allows to start a validation and updates the [messages] list.
     */
    val validate = handle<T> { state, meta ->
        validation(state, meta).also { validationMessages.value = it }
        state
    }

    /**
     * Resets the validation result.
     *
     * Beware that cleaning the messages should not be done, if the [data] [Flow] remains in an invalid state.
     * Please refer to the class's description for details about the need for a sound data and messages state.
     *
     * @param messages list of messages to reset to. Default is an empty list.
     */
    protected fun resetMessages(messages: List<M> = emptyList()) {
        validationMessages.value = messages
    }

    init {
        modifier(data.flatMapLatest { metadata }) handledBy validate
    }
}

/**
 * Checks if a [Flow] of a [List] of [ValidationMessage]s is valid.
 */
val <M : ValidationMessage> Flow<List<M>>.valid: Flow<Boolean>
    get() = this.map { it.valid }

/**
 * Convenience function to create a simple [ValidatingStore] without any handlers, etc.
 *
 * The created [Store] validates its model after every update of the data or the metadata automatically.
 *
 * @param initialData first current value of this [Store]
 * @param validation [Validation] instance to use at the data on this [Store].
 * @param metadata metadata to be used by the automatic validation
 * @param job Job to be used by the [Store]
 * @param id id of this [Store]. Ids of [SubStore]s will be concatenated.
 */
fun <D, T, M> storeOf(
    initialData: D,
    validation: Validation<D, T, M>,
    metadata: Flow<T>,
    job: Job = Job(),
    id: String = Id.next(),
): ValidatingStore<D, T, M> =
    ValidatingStore(
        initialData = initialData,
        validation = validation,
        metadata = metadata,
        job = job,
        id = id
    )

/**
 * Convenience function to create a simple [ValidatingStore] without any handlers, etc.
 *
 * The created [Store] validates its model after every update automatically.
 *
 * @param initialData first current value of this [Store]
 * @param validation [Validation] instance to use at the data on this [Store].
 * @param metadata static metadata to be used by the automatic validation
 * @param job Job to be used by the [Store]
 * @param id id of this [Store]. Ids of [SubStore]s will be concatenated.
 */
fun <D, T, M> storeOf(
    initialData: D,
    validation: Validation<D, T, M>,
    metadata: T,
    job: Job = Job(),
    id: String = Id.next(),
): ValidatingStore<D, T, M> =
    ValidatingStore(initialData, validation, metadata, job, id)

/**
 * Convenience function to create a simple [ValidatingStore] without any metadata and handlers.
 *
 * The created [Store] validates its model after every update automatically.
 *
 * @param initialData first current value of this [Store]
 * @param validation [Validation] instance to use at the data on this [Store].
 * @param job Job to be used by the [Store]
 * @param id id of this [Store]. Ids of [SubStore]s will be concatenated.
 */
fun <D, M> storeOf(
    initialData: D,
    validation: Validation<D, Unit, M>,
    job: Job,
    id: String = Id.next(),
): ValidatingStore<D, Unit, M> =
    ValidatingStore(initialData, validation, Unit, job, id)

/**
 * Convenience function to create a simple [ValidatingStore] without any handlers, etc.
 *
 * The created [Store] validates its model after every update of the data or the metadata automatically.
 *
 * @param initialData first current value of this [Store]
 * @param validation [Validation] instance to use at the data on this [Store].
 * @param metadata metadata to be used by the automatic validation
 * @param job Job to be used by the [Store]
 * @param id id of this [Store]. Ids of [SubStore]s will be concatenated.
 */
fun <D, T, M> WithJob.storeOf(
    initialData: D,
    validation: Validation<D, T, M>,
    metadata: Flow<T>,
    job: Job = this.job,
    id: String = Id.next(),
): ValidatingStore<D, T, M> =
    ValidatingStore(initialData, validation, metadata, job, id)

/**
 * Convenience function to create a simple [ValidatingStore] without any handlers, etc.
 *
 * The created [Store] validates its model after every update automatically.
 *
 * @param initialData first current value of this [Store]
 * @param validation [Validation] instance to use at the data on this [Store].
 * @param metadata static metadata to be used by the automatic validation
 * @param job Job to be used by the [Store]
 * @param id id of this [Store]. Ids of [SubStore]s will be concatenated.
 */
fun <D, T, M> WithJob.storeOf(
    initialData: D,
    validation: Validation<D, T, M>,
    metadata: T,
    job: Job = this.job,
    id: String = Id.next(),
): ValidatingStore<D, T, M> =
    ValidatingStore(initialData, validation, metadata, job, id)

/**
 * Convenience function to create a simple [ValidatingStore] without any metadata and handlers.
 *
 * The created [Store] validates its model after every update automatically.
 *
 * @param initialData first current value of this [Store]
 * @param validation [Validation] instance to use at the data on this [Store].
 * @param job Job to be used by the [Store]
 * @param id id of this [Store]. Ids of [SubStore]s will be concatenated.
 */
fun <D, M> WithJob.storeOf(
    initialData: D,
    validation: Validation<D, Unit, M>,
    job: Job = this.job,
    id: String = Id.next(),
): ValidatingStore<D, Unit, M> =
    ValidatingStore(initialData, validation, Unit, job, id)

/**
 * Finds all corresponding [ValidationMessage]s to this [Store] which satisfy the [filterPredicate]-expression.
 *
 * Be aware that the  filtering is based upon the correct usage of [Store.path]'s field. This can be reliably achieved
 * by using [dev.fritz2.core.Inspector]s and their mappings for creating the correct path values.
 *
 * @param filterPredicate expression to filter messages.
 */
fun <M : ValidationMessage> Store<*>.messages(filterPredicate: (M) -> Boolean): Flow<List<M>>? =
    when (this) {
        is ValidatingStore<*, *, *> -> {
            try {
                this.messages.map { it.unsafeCast<List<M>>() }
            } catch (e: Exception) {
                null
            }
        }

        is SubStore<*, *> -> {
            var store: Store<*> = this
            while (store is SubStore<*, *>) {
                store = store.parent
            }
            if (store is ValidatingStore<*, *, *>) {
                try {
                    store.messages.map { it.unsafeCast<List<M>>().filter(filterPredicate) }
                } catch (e: Exception) {
                    null
                }
            } else null
        }

        else -> null
    }

/**
 * Finds all exactly corresponding [ValidationMessage]s to this [Store], which means all messages, which have exactly
 * the same path as the [Store].
 *
 * Be aware that the  filtering is based upon the correct usage of [Store.path]'s field. This can be reliably achieved
 * by using [dev.fritz2.core.Inspector]s and their mappings for creating the correct path values.
 */
fun <M : ValidationMessage> Store<*>.messages(): Flow<List<M>>? = messages { message -> message.path == path }

/**
 * Finds all corresponding [ValidationMessage]s to this [Store], which means all messages, that fit exactly with their
 * path or which are sub-elements of this [Store]s data model.
 *
 * Consider the following example:
 * ```
 * Store path = ".person.address"
 *
 * // included
 * ".person.address"
 * ".person.address.street"
 * ".person.address.city"
 * ".person.address.coordinates.altitude"
 *
 * // not included
 * - ".person.addresses"
 * - ".person.other"
 * ```
 *
 * Be aware that the  filtering is based upon the correct usage of [Store.path]'s field. This can be reliably achieved
 * by using [dev.fritz2.core.Inspector]s and their mappings for creating the correct path values.
 */
fun <M : ValidationMessage> Store<*>.messagesOfSubModel(): Flow<List<M>>? = messages { message ->
    message.path == path || message.path.startsWith("$path.")
}
