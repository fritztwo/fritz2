package dev.fritz2.validation

import dev.fritz2.core.Id
import dev.fritz2.core.lensOf
import dev.fritz2.core.render
import dev.fritz2.core.storeOf
import dev.fritz2.runTest
import dev.fritz2.validation.test.*
import kotlinx.browser.document
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLSpanElement
import kotlin.test.Test
import kotlin.test.assertEquals

import kotlin.time.Duration.Companion.milliseconds

class FactoryFunctionTests {
    private data class Person(val name: String) {
        companion object {
            val validateUnit: Validation<Person, Unit, Message> = validation { inspector ->
                add(Message("", "Data: ${inspector.data.name}"))
            }

            val validate: Validation<Person, Int, Message> = validation { inspector, meta ->
                add(Message("", "Data: ${inspector.data.name}; Meta: $meta"))
            }
        }
    }

    @Test
    fun testWithJobStoreOfWithFlowMetadataAndDataChangeFirst() = runTest {
        val storedMeta = storeOf(0)
        val sut = storeOf(initialData = Person("Chris"), validation = Person.validate, metadata = storedMeta.data)

        val id = Id.next()
        render {
            div(id = id) {
                sut.validate.map { messages -> messages.joinToString { it.text } }.renderText(into = this)
            }
        }

        delay(100.milliseconds)
        val divData = document.getElementById(id) as HTMLDivElement

        assertEquals("Data: Chris; Meta: 0", divData.textContent)

        sut.update(Person("Fritz II"))
        delay(100.milliseconds)
        assertEquals("Data: Fritz II; Meta: 0", divData.textContent)

        storedMeta.update(42)
        delay(100.milliseconds)
        assertEquals("Data: Fritz II; Meta: 42", divData.textContent)
    }

    @Test
    fun testWithJobStoreOfWithFlowMetadataAndMetaChangeFirst() = runTest {
        val storedMeta = storeOf(0)
        val sut = storeOf(initialData = Person("Chris"), validation = Person.validate, metadata = storedMeta.data)

        val id = Id.next()
        render {
            div(id = id) {
                sut.validate.map { messages -> messages.joinToString { it.text } }.renderText(into = this)
            }
        }

        delay(100.milliseconds)
        val divData = document.getElementById(id) as HTMLDivElement

        assertEquals("Data: Chris; Meta: 0", divData.textContent)

        storedMeta.update(42)
        delay(100.milliseconds)
        assertEquals("Data: Chris; Meta: 42", divData.textContent)

        sut.update(Person("Fritz II"))
        delay(100.milliseconds)
        assertEquals("Data: Fritz II; Meta: 42", divData.textContent)
    }

    @Test
    fun testWithJobStoreOfWithStaticMetadata() = runTest {
        val sut = storeOf(initialData = Person("Chris"), validation = Person.validate, metadata = 42)

        val id = Id.next()
        render {
            div(id = id) {
                sut.validate.map { messages -> messages.joinToString { it.text } }.renderText(into = this)
            }
        }

        delay(100.milliseconds)
        val divData = document.getElementById(id) as HTMLDivElement

        assertEquals("Data: Chris; Meta: 42", divData.textContent)

        sut.update(Person("Fritz II"))
        delay(100.milliseconds)
        assertEquals("Data: Fritz II; Meta: 42", divData.textContent)
    }

    @Test
    fun testWithJobStoreOfWithoutMetadata() = runTest {
        val sut = storeOf(initialData = Person("Chris"), validation = Person.validateUnit)

        val id = Id.next()
        render {
            div(id = id) {
                sut.validate.map { messages -> messages.joinToString { it.text } }.renderText(into = this)
            }
        }

        delay(100.milliseconds)
        val divData = document.getElementById(id) as HTMLDivElement

        assertEquals("Data: Chris", divData.textContent)

        sut.update(Person("Fritz II"))
        delay(100.milliseconds)
        assertEquals("Data: Fritz II", divData.textContent)
    }

    @Test
    fun testStoreOfWithFlowMetadataAndDataChangeFirst(): dynamic {
        val job = Job()

        return runTest {
            val storedMeta = storeOf(0, job)
            val sut = storeOf(Person("Chris"), validation = Person.validate, metadata = storedMeta.data, job)
            val id = Id.next()
            render {
                div(id = id) {
                    sut.validate.map { messages -> messages.joinToString { it.text } }.renderText(into = this)
                }
            }

            delay(100.milliseconds)
            val divData = document.getElementById(id) as HTMLDivElement

            assertEquals("Data: Chris; Meta: 0", divData.textContent)

            sut.update(Person("Fritz II"))
            delay(100.milliseconds)
            assertEquals("Data: Fritz II; Meta: 0", divData.textContent)

            storedMeta.update(42)
            delay(100.milliseconds)
            assertEquals("Data: Fritz II; Meta: 42", divData.textContent)
        }
    }

    @Test
    fun testStoreOfWithFlowMetadataAndMetaChangeFirst(): dynamic {
        val job = Job()

        return runTest {
            val storedMeta = storeOf(0, job)
            val sut = storeOf(Person("Chris"), validation = Person.validate, metadata = storedMeta.data, job)

            val id = Id.next()
            render {
                div(id = id) {
                    sut.validate.map { messages -> messages.joinToString { it.text } }.renderText(into = this)
                }
            }

            delay(100.milliseconds)
            val divData = document.getElementById(id) as HTMLDivElement

            assertEquals("Data: Chris; Meta: 0", divData.textContent)

            storedMeta.update(42)
            delay(100.milliseconds)
            assertEquals("Data: Chris; Meta: 42", divData.textContent)

            sut.update(Person("Fritz II"))
            delay(100.milliseconds)
            assertEquals("Data: Fritz II; Meta: 42", divData.textContent)
        }
    }

    @Test
    fun testStoreOfWithStaticMetadata(): dynamic {
        val job = Job()

        return runTest {
            val sut = storeOf(Person("Chris"), validation = Person.validate, metadata = 42, job)
            val id = Id.next()
            render {
                div(id = id) {
                    sut.validate.map { messages -> messages.joinToString { it.text } }.renderText(into = this)
                }
            }

            delay(100.milliseconds)
            val divData = document.getElementById(id) as HTMLDivElement

            assertEquals("Data: Chris; Meta: 42", divData.textContent)

            sut.update(Person("Fritz II"))
            delay(100.milliseconds)
            assertEquals("Data: Fritz II; Meta: 42", divData.textContent)
        }
    }

    @Test
    fun testStoreOfWithoutMetadata(): dynamic {
        val job = Job()

        return runTest {
            val sut = storeOf(Person("Chris"), validation = Person.validateUnit, job)
            val id = Id.next()
            render {
                div(id = id) {
                    sut.validate.map { messages -> messages.joinToString { it.text } }.renderText(into = this)
                }
            }

            delay(100.milliseconds)
            val divData = document.getElementById(id) as HTMLDivElement

            assertEquals("Data: Chris", divData.textContent)

            sut.update(Person("Fritz II"))
            delay(100.milliseconds)
            assertEquals("Data: Fritz II", divData.textContent)
        }
    }
}

class TriggerValidationTests {

    private data class Person(val name: String) {
        companion object {
            val validate: Validation<Person, Int, Message> = validation { inspector, meta ->
                add(Message("", "Data: ${inspector.data.name}; Meta: $meta"))
            }
        }
    }

    @Test
    fun testTriggerValidationEnablesStopValidatingInitialState() = runTest {
        val storedMeta = storeOf(0)
        val sut = object : ValidatingStore<Person, Int, Message> by ValidatingStore.of(
            initialData = Person("Chris"),
            job = job,
            id = Id.next(),
            validation = Person.validate,
            // We can achieve the old "drop initial data for validation"-behavior quite easily
            triggerValidation = { data -> data.flatMapLatest { storedMeta.data }.drop(1) }
        ) {}

        val id = Id.next()
        render {
            div(id = id) {
                sut.validate.map { messages -> messages.joinToString { it.text } }.renderText(into = this)
            }
        }

        delay(100.milliseconds)
        val divData = document.getElementById(id) as HTMLDivElement

        assertEquals("", divData.textContent)

        sut.update(Person("Fritz II"))
        delay(100.milliseconds)
        assertEquals("Data: Fritz II; Meta: 0", divData.textContent)

        storedMeta.update(42)
        delay(100.milliseconds)
        assertEquals("Data: Fritz II; Meta: 42", divData.textContent)
    }

    @Test
    fun testTriggerValidationDisablesAutomaticValidation() = runTest {
        val storedMeta = storeOf(0)
        val sut = object : ValidatingStore<Person, Int, Message> by ValidatingStore.of(
            initialData = Person("Chris"),
            job = job,
            id = Id.next(),
            validation = Person.validate,
            // We can disable any automatic validation by not letting pass any value through the flow
            triggerValidation = { storedMeta.data.filter { false } }
        ) {}

        val id = Id.next()
        render {
            div(id = id) {
                sut.validate.map { messages -> messages.joinToString { it.text } }.renderText(into = this)
            }
        }

        delay(100.milliseconds)
        val divData = document.getElementById(id) as HTMLDivElement

        assertEquals("", divData.textContent)

        sut.update(Person("Fritz II"))
        delay(100.milliseconds)
        assertEquals("", divData.textContent)

        storedMeta.update(42)
        delay(100.milliseconds)
        assertEquals("", divData.textContent)
    }

    @Test
    fun testTriggerValidationEnablesDependingOnExternalTrigger() = runTest {
        val readyForValidation = storeOf(false)
        val sut = object : ValidatingStore<Person, Int, Message> by ValidatingStore.of(
            initialData = Person("Chris"),
            job = job,
            id = Id.next(),
            validation = Person.validate,
            // Use the `readyForValidation` to disable the validation until the gate is `true`
            triggerValidation = { data ->
                readyForValidation.data.dropWhile { !it }.flatMapLatest { data.map { 42 } }
            }
        ) {}

        val id = Id.next()
        render {
            div(id = id) {
                sut.validate.map { messages -> messages.joinToString { it.text } }.renderText(into = this)
            }
        }

        delay(100.milliseconds)
        val divData = document.getElementById(id) as HTMLDivElement

        assertEquals("", divData.textContent)

        // no messages after changing content
        sut.update(Person("Fritz II"))
        delay(100.milliseconds)
        assertEquals("", divData.textContent)

        // allow validation by enabling the gate
        readyForValidation.update(true)
        delay(100.milliseconds)
        assertEquals("Data: Fritz II; Meta: 42", divData.textContent)

        // after enabled once, it will automatically trigger validation on data changes
        sut.update(Person("Napoleon"))
        delay(100.milliseconds)
        assertEquals("Data: Napoleon; Meta: 42", divData.textContent)
    }
}

class ValidationJSTests {

    @Test
    fun testValidation() = runTest {

        val carName = "ok"
        val c1 = Car("car1", Color(-1, -1, -1))
        val c2 = Car("car2", Color(256, 256, 256))
        val c3 = Car("car3", Color(256, -1, 120))

        val store: ValidatingStore<Car, Unit, Message> =
            storeOf(Car(carName, Color(120, 120, 120)), validation = Car.validator)

        val idData = "data-${Id.next()}"
        val idMessages = "messages-${Id.next()}"

        render {
            div {
                div(id = idData) {
                    store.data.map { it.name }.renderText()
                }
                div(id = idMessages) {
                    store.validate.renderEach(Message::text, into = this) {
                        p {
                            +it.text
                        }
                    }
                }
            }
        }

        delay(100.milliseconds)
        val divData = document.getElementById(idData) as HTMLDivElement
        val divMessages = document.getElementById(idMessages) as HTMLDivElement

        assertEquals(carName, divData.textContent, "initial car name is wrong")
        assertEquals(0, divMessages.childElementCount, "there are messages")

        store.update(c1)
        delay(100.milliseconds)

        assertEquals(c1.name, divData.innerText, "c1: car name has not changed")
        assertEquals(3, divMessages.childElementCount, "c1: there is not 3 message")
        assertEquals(
            colorValuesAreTooLow,
            divMessages.firstElementChild?.textContent,
            "c1: there is not a expected message"
        )

        store.update(c2)
        delay(100.milliseconds)

        assertEquals(c2.name, divData.innerText, "c2: car name has changed")
        assertEquals(3, divMessages.childElementCount, "c2: there is not 3 message")
        assertEquals(
            colorValuesAreTooHigh,
            divMessages.firstElementChild?.textContent,
            "c2: there is not expected message"
        )

        store.update(c3)
        delay(100.milliseconds)

        assertEquals(c3.name, divData.innerText, "c3: car name has changed")
        assertEquals(2, divMessages.childElementCount, "c3: there is not 3 message")
    }

    @Test
    fun testValidationWithSubTreeFiltering() = runTest {

        val store: ValidatingStore<Car, Unit, Message> =
            storeOf(Car("car", Color(120, 120, 120)), validation = Car.validator)
        val colorStore = store.map(Car.colorLens)
        val rColorStore = colorStore.map(Color.rLens)
        val gColorStore = colorStore.map(Color.gLens)
        val bColorStore = colorStore.map(Color.bLens)

        val idData = "data-${Id.next()}"
        val idMessagesIntermediateLevel = "messages-${Id.next()}"
        val idPathIntermediateLevel = "messages-path-${Id.next()}"
        val idMessagesR = "messages-r-${Id.next()}"
        val idMessagesG = "messages-g-${Id.next()}"
        val idMessagesB = "messages-b-${Id.next()}"

        render {
            div {
                div(id = idData) {
                    colorStore.data.map { "${it.r}, ${it.g}, ${it.b}" }.renderText()
                }
                div(id = idMessagesIntermediateLevel) {
                    colorStore.messagesOfSubModel<Message>()?.renderEach(Message::path, into = this) {
                        p {
                            +it.text
                        }
                    }
                }
                div(id = idPathIntermediateLevel) {
                    colorStore.messagesOfSubModel<Message>()
                        ?.map { messages -> messages.all { it.path.startsWith(".color") } }
                        ?.renderText(into = this)
                }
                div(id = idMessagesR) {
                    rColorStore.messagesOfSubModel<Message>()?.renderEach(Message::path, into = this) {
                        p {
                            +it.path
                        }
                    }
                }
                div(id = idMessagesG) {
                    gColorStore.messagesOfSubModel<Message>()?.renderEach(Message::path, into = this) {
                        p {
                            +it.path
                        }
                    }
                }
                div(id = idMessagesB) {
                    bColorStore.messagesOfSubModel<Message>()?.renderEach(Message::path, into = this) {
                        p {
                            +it.path
                        }
                    }
                }
            }
        }

        // Test Intermediate Level (color-Field of Car)
        delay(100.milliseconds)
        val divData = document.getElementById(idData) as HTMLDivElement
        val divMessagesIntermediateLevel = document.getElementById(idMessagesIntermediateLevel) as HTMLDivElement
        val divPathIntermediateLevel = document.getElementById(idPathIntermediateLevel) as HTMLDivElement
        val divMessagesR = document.getElementById(idMessagesR) as HTMLDivElement
        val divMessagesG = document.getElementById(idMessagesG) as HTMLDivElement
        val divMessagesB = document.getElementById(idMessagesB) as HTMLDivElement

        assertEquals("120, 120, 120", divData.textContent, "initial car color is wrong")
        assertEquals(0, divMessagesIntermediateLevel.childElementCount, "there are messages")

        store.update(Car("car1", Color(-1, -1, -1)))
        delay(100.milliseconds)

        assertEquals("-1, -1, -1", divData.textContent, "c1: car color has not changed")
        assertEquals(3, divMessagesIntermediateLevel.childElementCount, "c1: there is not 3 message")
        assertEquals(
            colorValuesAreTooLow,
            divMessagesIntermediateLevel.firstElementChild?.textContent,
            "c1: there is not a expected message"
        )
        assertEquals("true", divPathIntermediateLevel.textContent, "paths start not all with .color")

        // Test Leave Nodes (fields of Color)
        assertEquals(1, divMessagesR.childElementCount, "r-Field error message not present")
        assertEquals(".color.r", divMessagesR.textContent)
        assertEquals(1, divMessagesG.childElementCount, "g-Field error message not present")
        assertEquals(".color.g", divMessagesG.textContent)
        assertEquals(1, divMessagesB.childElementCount, "b-Field error message not present")
        assertEquals(".color.b", divMessagesB.textContent)
    }

}

data class Bar(val foo: String, val foobar: String) {
    companion object {
        val fooLens = lensOf("foo", Bar::foo) { p, v -> p.copy(foo = v) }
        val foobarLens = lensOf("foobar", Bar::foobar) { p, v -> p.copy(foobar = v) }

        val validate: Validation<Bar, Unit, Message> = validation { inspector ->
            add(Message(inspector.path, "bar ist falsch"))
            add(Message(inspector.map(fooLens).path, "foo ist falsch"))
            add(Message(inspector.map(foobarLens).path, "foobar ist falsch"))
        }
    }
}

data class Foo(val foo: String, val foobar: String, val bar: Bar) {
    companion object {
        val fooLens = lensOf("foo", Foo::foo) { p, v -> p.copy(foo = v) }
        val foobarLens = lensOf("foobar", Foo::foobar) { p, v -> p.copy(foobar = v) }
        val barLens = lensOf("bar", Foo::bar) { p, v -> p.copy(bar = v) }

        val validate: Validation<Foo, Unit, Message> = validation { inspector ->
            add(Message(inspector.map(fooLens).path, "foo ist falsch"))
            add(Message(inspector.map(foobarLens).path, "foobar ist falsch"))
            addAll(Bar.validate(inspector.map(barLens), Unit))
        }
    }
}

class MessageFilterTests {

    @Test
    fun testMessagesWithoutFilterExpressionOnlyMatchesExactlyFittingPathes() = runTest {
        val initial = Foo("", "", Bar("", ""))
        val store = storeOf(initial, validation = Foo.validate)
        store.update(initial.copy(foo = "a"))

        val id = Id.next()
        render {
            span(id = id) {
                store.map(Foo.barLens).messages<Message>()
                    ?.map { messages -> messages.joinToString { it.text } }
                    ?.renderText()
            }
        }

        delay(100.milliseconds)
        val span = document.getElementById(id) as HTMLSpanElement
        assertEquals("bar ist falsch", span.textContent)
    }

    @Test
    fun testMessagesOfSubTreeMatchesAllSubPathes() = runTest {
        val initial = Foo("", "", Bar("", ""))
        val store = storeOf(initial, validation = Foo.validate)
        store.update(initial.copy(foo = "a"))

        val id = Id.next()
        render {
            span(id = id) {
                store.map(Foo.barLens).messagesOfSubModel<Message>()?.map { it.size.toString() }?.renderText()
            }
        }

        delay(100.milliseconds)
        val span = document.getElementById(id) as HTMLSpanElement
        assertEquals("3", span.textContent)
    }

    @Test
    fun testMessagesRespectFilterExpression() = runTest {
        val initial = Foo("", "", Bar("", ""))
        val store = storeOf(initial, validation = Foo.validate)
        store.update(initial.copy(foo = "a"))

        val id = Id.next()
        render {
            span(id = id) {
                // 4
                store.map(Foo.barLens).messages<Message> { it.text.contains("foo") }
                    ?.map { it.size.toString() }
                    ?.renderText()
                // 0
                store.map(Foo.barLens).messages<Message> { it.text.contains("richtig") }
                    ?.map { it.size.toString() }
                    ?.renderText()
            }
        }

        delay(100.milliseconds)
        val span = document.getElementById(id) as HTMLSpanElement
        assertEquals("40", span.textContent)
    }

    @Test
    fun testOverlappingFieldnamesDoNotMatchEachOthersPathes() = runTest {
        val initial = Foo("", "", Bar("", ""))
        val store = storeOf(initial, validation = Foo.validate)
        store.update(initial.copy(foo = "a"))
        /*
        Validation should result in messages with these pathes:
        ```
        .foo
        .foobar
        .bar.foo
        .bar.foobar
        ```
        Messages of `foo` should not appear in mapped store of `foobar` -> so no overlapping!
        */
        val id = Id.next()

        render {
            span(id = id) {
                // 1
                store.map(Foo.fooLens).messagesOfSubModel<Message>()?.map { it.size.toString() }?.renderText()
                // 1
                store.map(Foo.foobarLens).messagesOfSubModel<Message>()?.map { it.size.toString() }?.renderText()
                // 2
                store.map(Foo.barLens).messagesOfSubModel<Message>()?.map { it.size.toString() }?.renderText()
                // 1
                store.map(Foo.fooLens).messagesOfSubModel<Message>()?.map { it.size.toString() }?.renderText()
                // 1
                store.map(Foo.fooLens).messagesOfSubModel<Message>()?.map { it.size.toString() }?.renderText()
            }
        }

        delay(100.milliseconds)
        val span = document.getElementById(id) as HTMLSpanElement
        assertEquals("11311", span.textContent)
    }
}
