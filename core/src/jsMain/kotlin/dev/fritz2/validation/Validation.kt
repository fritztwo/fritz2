@file:Suppress("unused")

package dev.fritz2.validation

import dev.fritz2.core.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*

/**
 * A [ValidatingStore] is a specialized [Store] that enhances the standard store API with a [validate] handler.
 *
 * This handler combines the current state of the store with the provided metadata of type [T]. Since it is implemented
 * as an [EmittingHandler], it emits the resulting validation messages [M] as a [List] upon invocation.
 *
 * A factory function named [of] is available to instantiate a fully configured [ValidatingStore].
 *
 * In addition to standard store requirements, you must provide a [Validation] object, which is utilized inside the
 * `validate` handler to perform the actual validation logic.
 *
 * By default, the implementation connects a data stream with the `validate` handler to enable automatic validation.
 * This stream is configured via the `triggerValidation` factory parameter — a lambda expression that receives the
 * store's data [Flow] and returns a flow of [T]. This lambda serves two core purposes:
 * 1. It is the place where the client provides the necessary metadata.
 * 2. It allows customizing the flow to implement individual validation strategies, such as:
 *    - Skipping validation for the initial store state by appending `.drop(1)`.
 *    - Disabling automatic validation entirely by appending `.filter { false }` (or similar mechanisms).
 *    - Combining an external flag (e.g., as a gatekeeper) to postpone validation until a specific condition is met.
 *
 * For standard use cases, several [storeOf] factory functions are provided. They vary based on:
 * - The type of metadata: a [Flow], a static value, or [Unit].
 * - The context/scope: whether created within a [RenderContext] (inheriting its scope) or outside of it
 * (requiring an explicit [Job]).
 *
 * By default, the created store triggers a validation on every state update, as well as on any metadata change
 * (if a flow is used).
 *
 * If you need to extend the created store with custom handlers, trackers, or other specific functionality, you can
 * leverage Kotlin's [Delegation Pattern](https://kotlinlang.org/docs/delegation.html):
 * ```kotlin
 * object PersonStore : ValidatingStore<Person, Unit, Message> by ValidatingStore.of(
 *     Person(), Job(), id = Person.id, personValidator, triggerImmediatelyOf(Unit)
 * ) {
 *     val save = handle { person ->
 *         if (personValidator(person, Unit).valid) {
 *             PersonListStore.add(person)
 *             cleanUpValMessages()
 *             Person()
 *         } else person
 *     }
 * }
 * ```
 *
 * If you require even more flexibility, you can create a custom implementation. Use the following minimal setup
 * as a starting point and adapt it to your specific needs:
 * ```kotlin
 * class MyCustomValidatingStore<D, T, M>(
 *     initialData: D,
 *     validation: Validation<D, T, M>,
 *     metadata: Flow<T>,
 *     job: Job,
 *     id: String,
 * ) : RootStore<D>(initialData, job, id), ValidatingStore<D, T, M> {
 *     override val validate: EmittingHandler<T, List<M>> = handleOnlyEmit { state, meta ->
 *         emit(validation(state, meta))
 *     }
 *
 *     init {
 *         data.flatMapLatest { metadata } handledBy validate
 *     }
 * }
 * ```
 */
interface ValidatingStore<D, T, M> : Store<D> {

    /**
     * [Handler] that allows to start a validation and emits a [Flow] of [List] of validation-messages.
     * Use this `Flow` to render out the validation-messages and to detect the valid state of the current [data] `Flow`.
     */
    val validate: EmittingHandler<T, List<M>>

    companion object {
        /**
         * Creates a [ValidatingStore] that automatically executes the provided [validation] logic based on the
         * data stream produced by the [triggerValidation] factory.
         *
         * The [triggerValidation] lambda can be used to customize the validation strategy to your needs, for example:
         * - Skipping validation for the initial store state by appending `.drop(1)`.
         * - Disabling automatic validation entirely by appending `.filter { false }` (or similar mechanisms).
         * - Combining an external flag (e.g., as a gatekeeper) to postpone validation until a specific condition is met.
         */
        fun <D, T, M> of(
            initialData: D,
            job: Job,
            id: String,
            validation: Validation<D, T, M>,
            triggerValidation: (Flow<D>) -> Flow<T>
        ): ValidatingStore<D, T, M> = object : ValidatingStore<D, T, M>, RootStore<D>(initialData, job, id) {

            override val validate: EmittingHandler<T, List<M>> = handleOnlyEmit { state, meta ->
                validation(state, meta).also { emit(it) }
            }

            init {
                triggerValidation(data) handledBy validate
            }
        }

        // FIXME: Should those rather be internal? Less API ist easier to maintain!
        //  Rational: We could (and should) show in Docs how to implement your own custom ValidatingStore.
        //  The canonical example would use the `data.flatMapLatest { metadata } handledBy validate` in its
        //  `init` block anyhow. So it is not really necessary to expose this.

        /**
         * Creates a common validation trigger, that calls the validation handler every time the [data]
         * or the [metadata] flows change.
         *
         * @param metadata a [Flow] of metadata, that the [Validation] object should use.
         */
        fun <D, T> triggerImmediatelyOf(metadata: Flow<T>): (Flow<D>) -> Flow<T> = { data ->
            data.flatMapLatest { metadata }
        }

        /**
         * Creates a common validation trigger, that calls the validation handler every time the [data] changes.
         *
         * @param metadata a static value for the metadata, that the [Validation] object should use.
         */
        fun <D, T> triggerImmediatelyOf(metadata: T): (Flow<D>) -> Flow<T> = { data ->
            data.map { metadata }
        }

        /**
         * Creates a common validation trigger, that calls the validation handler every time the [data] changes.
         */
        fun <D> triggerImmediately(): (Flow<D>) -> Flow<Unit> = { data ->
            data.map { }
        }
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
): ValidatingStore<D, T, M> = ValidatingStore.of(
    initialData,
    job,
    id,
    validation,
    ValidatingStore.triggerImmediatelyOf(metadata)
)

/**
 * Convenience function to create a simple [ValidatingStore] without any handlers, etc.
 *
 * The created [Store] validates its model after every update automatically.
 *
 * @param initialData first current value of this [Store]
 * @param validation [Validation] instance to use at the data on this [Store].
 * @param metadata default metadata to be used by the automatic validation (where no explicit values are given)
 * @param job Job to be used by the [Store]
 * @param id id of this [Store]. Ids of [SubStore]s will be concatenated.
 */
fun <D, T, M> storeOf(
    initialData: D,
    validation: Validation<D, T, M>,
    metadata: T,
    job: Job = Job(),
    id: String = Id.next(),
): ValidatingStore<D, T, M> = ValidatingStore.of(
    initialData,
    job,
    id,
    validation,
    ValidatingStore.triggerImmediatelyOf(metadata)
)

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
): ValidatingStore<D, Unit, M> = ValidatingStore.of(
    initialData,
    job,
    id,
    validation,
    ValidatingStore.triggerImmediately()
)

/**
 * Convenience function to create a simple [ValidatingStore] without any handlers, etc.
 *
 * The created [Store] validates its model after every update of the data or the metadata automatically.
 *
 * @param initialData first current value of this [Store]
 * @param validation [Validation] instance to use at the data on this [Store].
 * @param metadata metadata to be used by the automatic validation
 * @param id id of this [Store]. Ids of [SubStore]s will be concatenated.
 */
fun <D, T, M> WithJob.storeOf(
    initialData: D,
    validation: Validation<D, T, M>,
    metadata: Flow<T>,
    id: String = Id.next(),
): ValidatingStore<D, T, M> = ValidatingStore.of(
    initialData,
    job,
    id,
    validation,
    ValidatingStore.triggerImmediatelyOf(metadata)
)

/**
 * Convenience function to create a simple [ValidatingStore] without any handlers, etc.
 *
 * The created [Store] validates its model after every update automatically.
 *
 * @param initialData first current value of this [Store]
 * @param validation [Validation] instance to use at the data on this [Store].
 * @param metadata default metadata to be used by the automatic validation (where no explicit values are given)
 * @param id id of this [Store]. Ids of [SubStore]s will be concatenated.
 */
fun <D, T, M> WithJob.storeOf(
    initialData: D,
    validation: Validation<D, T, M>,
    metadata: T,
    id: String = Id.next(),
): ValidatingStore<D, T, M> = ValidatingStore.of(
    initialData,
    job,
    id,
    validation,
    ValidatingStore.triggerImmediatelyOf(metadata)
)

/**
 * Convenience function to create a simple [ValidatingStore] without any metadata and handlers.
 *
 * The created [Store] validates its model after every update automatically.
 *
 * @param initialData first current value of this [Store]
 * @param validation [Validation] instance to use at the data on this [Store].
 * @param id id of this [Store]. Ids of [SubStore]s will be concatenated.
 */
fun <D, M> WithJob.storeOf(
    initialData: D,
    validation: Validation<D, Unit, M>,
    id: String = Id.next(),
): ValidatingStore<D, Unit, M> = ValidatingStore.of(
    initialData,
    job,
    id,
    validation,
    ValidatingStore.triggerImmediately()
)

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
                this.validate.map { it.unsafeCast<List<M>>() }
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
                    store.validate.map { it.unsafeCast<List<M>>().filter(filterPredicate) }
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
