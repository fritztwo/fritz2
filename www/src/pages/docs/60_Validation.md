---
title: Validation
description: "Learn how to validate the data-models of fritz2 web-app"
layout: layouts/docs.njk
permalink: /docs/validation/
eleventyNavigation:
  key: validation
  parent: documentation
  title: Validation
  order: 60
---

## Overview

When accepting user input, it's a good idea to validate the data before processing it further.

To validate in fritz2, create a Validation object. Use the global validation function, which takes these type
parameters:

- the type of data to validate
- a type for metadata you want to forward from Handlers to the validation (Unit by default)
- a type describing the validation results (e.g., a message), which must implement the `ValidationMessage` interface

fritz2 simplifies data validation within your application by providing a combination of conventions, data types, and 
factory functions.

:::info
By utilizing [headless components](/headless/), validation messages are automatically associated with their 
respective components, requiring no additional effort.
:::

### Simple Example

We recommend placing your validation code in the companion object of your data class inside the commonMain source set of
your multiplatform project. Code in `commonMain` can be used from `jsMain` (frontend) and `jvmMain` (backend).

Inside the validation function you have access to an `Inspector` for your data model. You can get paths to model members
by calling `map()` with a lens (similar to store mapping). The result of `map()` is another Inspector with two
properties: data and path, which you can use when validating that specific member.

To add a validation message to the list, call the `add()` function.

```kotlin
enum class Severity {
    Info,
    Warning,
    Error
}

data class Message(override val path: String, val severity: Severity, val text: String) : ValidationMessage {
    override val isError: Boolean = severity > Severity.Warning
}

@Lenses
data class Person(
    val name: String,
    val age: Int
) {
    companion object {
        // Define some validation object by the `validation` factory function:
        val validate: Validation<Person, Unit, Message> = validation<Person, Message> { inspector ->
            // the `Inspector` will be explained in the upcoming sections
            // Please accept this as a requirement for message identification.
            val name = inspector.map(Person.name())
            //         ^^^^^^^^^     ^^^^^^^^^^^^^
            //         works like a     we need to
            //      read-only store!    pass a Lens
            if (name.data.trim().isBlank()) {
                add(Message(name.path, Severity.Error, "Please provide a name"))
            }

            val age = inspector.map(Person.age())
            if (age.data < 1) {
                add(Message(age.path, Severity.Error, "Please correct the age"))
            } else if (age.data > 100) {
                add(Message(age.path, Severity.Warning, "Is the person really older than 100 years‽"))
            }
        }
    }
}
```

You can structure and implement your validation-rules with everything Kotlin offers.

Now you can use the `Validation` object in your `commonMain`, `jsMain` or `jvmMain`-code:

```kotlin
val invalidPerson = Person("", 101)
Person.validate(invalidPerson)
// gives a List of Messages:
// [
// Message(path=.name, severity=Error, text=Please provide a name), 
// Message(path=.name, severity=Warning, text=Is the person really older than 100 years‽)
// ]
```

### Inspectors on the Surface

You might have wondered what exactly the `Inspector` is and why it is used for data access and processing instead of 
working directly with an object's fields.

Think of an `Inspector` as a read-only `Store`. In essence, it is the counterpart to the mutable stores in the UI that 
hold the application's state.

When performing validation, you are generally working with the same types that are managed in stores within the UI.

By wrapping these objects in an `Inspector` and consistently using the **same** `Lense`s as with the stores, 
messages are generated that refer to exactly the same part of the model (usually a specific property). 
This relationship allows the UI to later display messages precisely at the fields they were intended for.

This property is reflected in the `Inspector.path` field.

For a high-level overview, it is sufficient to accept this and know that you access the data via `Inspector.data`. 
By using various mapping functions, you can decompose the model down to its leaf nodes, passing the corresponding 
lenses as parameters.

If you follow these conventions, [Headless UI components](/headless), for example, will automatically provide all 
validation messages associated with a mapped store in a `value` field without any additional effort.

More in-depth information can be found in the section 
[Of Inspectors and Paths](#of-inspectors-and-paths).

## Essentials

### Integrate validation into stores

Since fritz2 stores are the central entities for managing state, it is only natural to combine the update and 
validation processes. This is achieved using the specialized `ValidatingStore`, which is a subtype of the standard 
fritz2 `Store`.

By default, a `ValidatingStore` automatically validates its data after changes and updates the message list.
You can access these validation messages via `store.messages`: a `Flow<List<M>>` where `M` is your `ValidationMessage`
type. Handle this `Flow` like any other `Flow` of a `List` — for example, render it to HTML:

```kotlin
// create a `ValidatingStore` by using an overloaded factory located in `dev.fritz2.validation`-package
val store = storeOf(Person("Chris", 48), Person::validate)

// create some messages with shady data
store.update(Person("", 101))

render {
    ul {
        store.messages.renderEach {
            li(baseClass = it.severity.name.toLowerCase()) {
                +it.text
            }
        }
    }
}
```

If you want to start the validation process in a specific handler you can do so by implementing the `ValidatingStore`
by yourself:

```kotlin
object PersonStore : ValidatingStore<Person, Unit, Message>(Person("", 0), Person.validation, job = Job()) {
    val save = handle {
        if (validate(it).valid) {
            // send request to server...
            Person("", 0)
        } else it
    }
    val reset = handle {
        resetMessages() // empties the list of messages
        Person("", 0)
    }
}
```

Call `resetMessages()` to manually clear the list of messages when needed.

Have a look at a more complete example [here](/examples/validation).

### Validating Object Hierarchies

Real-world applications often deal with complex object hierarchies. You can apply the same validation mechanisms to 
these structures without any extra effort.

We’ll start by extending our `Person` example with a new `Address` field, defined as follows:

```kotlin
@Lenses
data class Address(
    val street: String,
    val zipCode: String,
    val city: String,
    val country: String,
) {
    companion object {
        val validate: Validation<Address, Unit, Message> = validation { inspector ->
            val street = inspector.map(Address.street())
            if (street.data.isBlank()) {
                add(Message(street.path, Severity.Error, "Please provide a street"))
            }
            // Think of the missing validators for `zipCode`, `city` and `country` below in a similar way.
        }
    }
}
```

As you can see, the `Address` type offers nothing special or new: validation is implemented within its 
`companion object`, as recommended.

We can now call the `Address` validator from within the `Person` validator. In doing so, the appropriate `Inspector` 
must be passed—in this case, an `Inspector<Address>`. This approach should also be familiar, as it is implemented 
using the typical mapping operations.

Since every validator call always returns a list of messages, remember to add the results from the called validator 
to the calling validator's list:

```kotlin
@Lenses
data class Person(
    val name: String,
    val age: Int,
    val address: Address, // new field that establishes object hierarchy
) {
    companion object {
        val validate: Validation<Person, Unit, Message> = validation { inspector ->
            // like before validate own fields
            val name = inspector.map(Person.name())
            if (name.data.trim().isBlank()) {
                add(Message(name.path, Severity.Error, "Please provide a name"))
            }
            // ...
            
            // call validator of `address`-field and add all of its messages to this messages list
            addAll(Address.validate(inspector.map(Person.address())))
        }
    }
}
```

We can now continue to call the `Person` validator and pass an invalid `Address`. As a result, we will receive 
a message generated by the `Address` validator:

```kotlin
val invalidPerson = Person("Chris", 48, Address("", "", "", ""))
Person.validate(invalidPerson)
// [Message(path=.address.street, severity=Error, text=Please provide a street)]
```

### Validating Collections

Another typical manifestation of complex object hierarchies are collections, such as lists or maps, that appear as 
field types within objects.

We can extend our previous example and imagine that a `Person` can have multiple addresses. 
Thus, the field is updated to: `val addresses: List<Address>`.

```kotlin
@Lenses
data class Person(
    val name: String,
    val age: Int,
    val addresses: List<Address>, // multiple addresses are now possible
) {
    companion object {
        val validate: Validation<Person, Unit, Message> = validation { inspector ->
            // ... like before

            // create `Inspector<List<Address>>`...
            val addresses = inspector.map(Person.addresses())
            // ... and validate all of its entries with an appropriate convenience function
            addresses.inspectEach { address -> addAll(Address.validate(address)) }
        }
    }
}
```

For collections, the `Inspector` provides mapping functions analogous to those for `Store`s, as described in the
[Store Mapping](/docs/storemapping/#summary-of-store-mapping-factories) chapter. 
In this case, we use a convenience function `inspectEach` on a `List`, which internally uses the index as identifier
for each entry, which is the canonical choice for a field of type `List<T>`.

There is a [complete overview](#summary-of-inspector-mappings) of all functions of an `Inspector`

Just like with the simple integration of an external validator, we must not forget to include its validation messages.

The call — now featuring two invalid addresses and one valid address — looks like this:

```kotlin
val invalidPerson = Person(
    "Chris",
    48,
    listOf(
        Address("", "", "", ""),
        Address("Valid-Street 22", "", "", ""),
        Address("", "", "", ""),
    )
)
Person.validate(invalidPerson)
// [
// Message(path=.addresses.0.street, severity=Error, text=Please provide a street), 
// Message(path=.addresses.2.street, severity=Error, text=Please provide a street)
// ]
```

The messages are easy to distinguish because their paths include the index. Based on the zero-based index, 
the addresses at positions `0` and `2` are therefore invalid.

### Using Metadata

Up to this point, we have only looked at validations in isolation.

In reality, however, validations often require additional information, which we will henceforth refer to as *metadata*.

Examples of such data include the progress within a complex multi-step form, where data for a global model is 
entered incrementally. By keeping track of the user's current step, you can limit validation to the specific 
part of the model that should actually be present at that stage.

Furthermore, submodels often require information from other parts of the global model for their own validation. 
Metadata can be used here as well to keep the component being validated as independent from the overall model 
as possible.

Another common example is the current date or time. This information should not be determined inside the validation 
logic itself; instead, it should be passed in to simplify — or in some cases, even enable — testability.

To address these requirements, the `validation` factory API provides a second parameter for arbitrary metadata 
of type `T`:

```kotlin
fun <D, T, M> validation(validate: MutableList<M>.(Inspector<D>, T) -> Unit): Validation<D, T, M>
//      ^                                                        ^
//   type parameter defining the metadata type                2nd parameter of the invocation
//                                                            in order to pass the actual metadata
```

Let's revisit our familiar `Person` example. Imagine a UI where you first enter only the person's basic details, 
followed by their addresses in a second step. To control which parts of the validation should be executed, 
you need to know the current progress within the UI. For this purpose, we will introduce a dedicated `Enum` type:

```kotlin
enum class Progress {
    Core, Address
}
```

We can now use this `Progress` state within our validation code to ensure that specific data is only validated when 
required by the UI logic:

```kotlin
@Lenses
data class Person(
    val name: String,
    val age: Int,
    val addresses: List<Address>,
) {
    companion object {
        val validate: Validation<Person, Progress, Message> = validation { inspector, progress ->
            //                           ^^^^^^^^                                     ^^^^^^^^
            //                    beware the changed metatdata type         use the additional parameter
            val name = inspector.map(Person.name())
            if (name.data.trim().isBlank()) {
                add(Message(name.path, Severity.Error, "Please provide a name"))
            }

            // we omit this sub-validation until the user can provide such data via the UI
            if (progress >= Progress.Address) {
                val addresses = inspector.map(Person.addresses())
                addresses.inspectEach { address -> addAll(Address.validate(address)) }
            }
        }
    }
}
```

We can now call the validation with different `Progress` states and will receive only those messages that are actually
generated based on the `if` conditions in the code above:

```kotlin
val invalidPerson = Person(
    "",
    48,
    listOf(
        Address("", "", "", ""),
    )
)

Person.validate(invalidPerson, Progress.Core)
// [Message(path=.name, severity=Error, text=Please provide a name)]

Person.validate(invalidPerson, Progress.Address)
// [
// Message(path=.name, severity=Error, text=Please provide a name), 
// Message(path=.addresses.0.street, severity=Error, text=Please provide a street)
// ]
```

:::info
Metadata types can, of course, be as complex as needed!

If you require more than just a simple `Enum`, you should create a **dedicated** `data class` that encapsulates 
all the necessary data.
:::

### Dealing with reactive Metadata and ValidatingStore

Now that you are familiar with [integrating validation](#integrate-validation-into-stores) into a `Store` using
the specialized `ValidatingStore` and understand the motivation [behind metadata](#using-metadata) in validation, 
it is time to discuss how to use both together.

There is a significant difference between static metadata — those that do not change during the course of an 
application's use — and those that actually change analogously to the state of an application and are therefore
potentially different with each validation run.

Let's first look at the simpler case where the metadata is static and possibly only depends on the configuration
of the application instance.

We will continue using the example from the previous section.

We first create two data sets:
- a valid person
- an invalid person who should generate validation messages

```kotlin
val chris = Person(
    "Chris",
    48,
    listOf(
        Address(
            "Rosestreet 220",
            "12345",
            "Gosecamp",
            "Germany"
        )
    )
)

val invalidChris = chris.copy(
    name = "", // no name
    age = 101, // too old
    addresses = chris.addresses.map { address ->
        address.copy(
            street = "", // no street
        )
    }
)
```

With this, we can create a store:
```kotlin
val storedPerson = ValidatingStore(chris, Person.validate, Progress.Address, Job())
//                                                         ^^^^^^^^^^^^^^^^
//                                                         We must pass initial metadata
//                                                         We chose the maximum progress to see all messages
```

Note that the metadata we pass in the constructor is used for all validations. They are therefore static over the 
entire lifetime of the store.

We can demonstrate the validation with the following minimal UI:
```kotlin
render {
    val storedPerson = ValidatingStore(chris, Person.validate, Progress.Address, job)

    button {
        +"Set invalid Data"
        clicks.map { invalidChris } handledBy storedPerson.update
    }
    p {
        +"Messages: "
        ul {
            storedPerson.messages.renderEach { message ->
                li { +message.toString() }
            }
        }
    }
}
```

Initially, no messages are visible. After clicking the button, the following messages appear as expected:
```text
Message(path=.name, severity=Error, text=Please provide a name)
Message(path=.age, severity=Warning, text=Is the person really older than 100 years‽)
Message(path=.addresses.0.street, severity=Error, text=Please provide a street)
```

As a rule, however, metadata will not be static, but — just like the functional data — will be dynamically dependent 
on the user's actions. So how can we use such dynamic metadata for validations?

We must derive our own type from `ValidatingStore` and implement handlers within it that call the following function:
```kotlin
protected fun validate(data: D, metadata: T = metadataDefault): List<M>
```

The following implementation allows injecting a `Progress` value to call the validation with this metadata set:
```kotlin
val storedPerson = object : ValidatingStore<Person, Progress, Message>(
    invalidChris, Person.validate, Progress.Core, job
) {
    val validate = handle<Progress> { state, progress ->
    //                    ^^^^^^^^           ^^^^^^^^
    //                    Enable to pass a `Progress`-object
        
        validate(state, progress)
        //              ^^^^^^^^
        //              pass the Progress-object into the actual validation
        state
    }
}
```

Now we still need a store for the UI state — in our example, the `Progress` — and we must connect its reactive 
state with the handler created above:
```kotlin
val storedProgress = storeOf(Progress.Core, job)

// here the "magic" happens: the changed Progress will trigger a new validation
storedProgress.data handledBy storedPerson.validate
```

Now we can slightly adjust the example: We change the button so that it toggles the current progress back and forth 
between the `Progress.Core` and the `Progress.Address` value. We remember that the address validation only takes 
effect at the `Progress.Address` value. Only then will the message regarding the invalid street appear.
```kotlin
button {
    +"Toggle Progress"
    clicks.map {
        if (storedProgress.current == Progress.Address) Progress.Core else Progress.Address
    } handledBy storedProgress.update
}
p {
    +"Progress:"
    storedProgress.data.renderText()
}
p {
    +"Messages:"
    ul {
        storedPerson.messages.renderEach { message ->
            li { +message.toString() }
        }
    }
}
```

At the beginning, as expected, only these two messages are displayed in the `Progress.Core` state:
```text
Message(path=.name, severity=Error, text=Please provide a name)
Message(path=.age, severity=Warning, text=Is the person really older than 100 years‽)
```

If you switch to `Progress.Address`, the message about the missing street is also displayed:
```text
Message(path=.name, severity=Error, text=Please provide a name)
Message(path=.age, severity=Warning, text=Is the person really older than 100 years‽)
Message(path=.addresses.0.street, severity=Error, text=Please provide a street)
```

:::info
Of course, you can pass the metadata into the `Handler` in any way intended by fritz2, e.g., through an event
triggered by the user, such as clicking a "Next" button or similar.
:::

### Summary of Inspector-Mappings

| Factory                                                                                   | Use case                                                                                                                                                                                      |
|-------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Inspector<D>.map(lens: Lens<D, X>): Inspector<X>`                                        | Most generic map-function. Maps any `Inspector` given a `Lens`. Use for model destructuring with automatic generated lenses for example.                                                      |
| `Inspector<D?>.mapNull(default: D): Inspector<D>`                                         | Maps any nullable `Inspector` given a `Lens` to a `Inspector` of a definitely none nullable `T`.                                                                                              |
| `Inspector<T>.mapNullable(placeholder: T): Inspector<T?>`                                 | Maps a `Inspector` of `T` to a `Inspector` of `T?`, replacing the given `placeholder` from the parent with `null` in the sub inspector. This function is the reverse equivalent of `mapNull`. |
| `Inspector<List<D>>.mapByElement(element: D, idProvider: IdProvider<D, I>): Inspector<D>` | Maps a `Inspector` of a `List<T>` to one element of that list. Works for entities, as a stable Id is needed.                                                                                  |
| `Inspector<List<D>>.mapByIndex(index: Int): Inspector<D>`                                 | Maps a `Inspector` of a `List<T>` to one element of that list using the index.                                                                                                                    |
| `Inspector<Map<K, V>>.mapByKey(key: K): Inspector<V>`                                     | Maps a `Inspector` of a `Map<T>` to one element of that map using the key.                                                                                                                        |

There are also those convenience functions, that reduce the boilerplate of validating collections:

| Factory                                                                                        | Use case                                                                                      |
|------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| `Inspector<List<D>>.inspectEach(idProvider: IdProvider<D, I>, action: (Inspector<D>) -> Unit)` | Takes an `IdProvider<D, I>` and applies the given `action` to all elements of the list.       |
| `Inspector<List<D>>.inspectEach(action: (Inspector<D>) -> Unit)`                               | Applies the given `action` to all elements of the list. Uses the index as identifier.         |
| `Inspector<Map<K, V>>.inspectEach(action: (K, Inspector<V>) -> Unit)`                          | Applies the given `action` to all elements of the map. Uses the key of the map as identifier. |

## Advanced Topics

### Of Inspectors and Paths

As described at the beginning, the primary purpose of inspectors within validation is to encapsulate data access in 
such a way that it produces the same *mapped* path as the one generated when mapping the main store down to a leaf node.

By convention, every mapping call takes the ID of a `Lens` and appends it to the existing path using a dot (`.`). 
This process occurs during the mapping of both stores and inspectors:

```kotlin
val chris =  Person(
    "Chris",
    48,
    listOf(
        Address("Rosestreet", "22", "Oaker", "Germany"),
    )
)

val inspectedPerson = inspectorOf(chris)
val storedPerson = storeOf(chris, Job())
```

Evaluating these two objects allows us to observe the similarities:

```kotlin
inspectedPerson.data
// Person(name=Chris, age=48, addresses=[Address(street=Rosestreet, zipCode=22, city=Oaker, country=Germany)])

storedPerson.current
// Person(name=Chris, age=48, addresses=[Address(street=Rosestreet, zipCode=22, city=Oaker, country=Germany)])

inspectedPerson.path
// "" -> is empty as the person-objet is top-level

storedPerson.path
// "" -> is empty as the person-objet is top-level
```

Going two levels deeper into the object tree reveals the following:

```kotlin
val inspectedAddress = inspectedPerson.map(Person.addresses()).mapByIndex(0)
val storedAddress = storedPerson.map(Person.addresses()).mapByIndex(0)

inspectedAddress.data
// Address(street=Rosestreet, zipCode=22, city=Oaker, country=Germany)

storedAddress.data
// Address(street=Rosestreet, zipCode=22, city=Oaker, country=Germany)

inspectedAddress.path
// .addresses.0

storedAddress.path
// .addresses.0
```

You can clearly see the dot notation of the path, which — similar to [XPATH](https://en.wikipedia.org/wiki/XPath) 
or [JSON-Path](https://en.wikipedia.org/wiki/JSONPath) in their simplest forms — describes the position of an object 
within the overall model, starting from the root.

When using lenses generated by fritz2, the property name is consistently used for this purpose.

:::info
If you write your own lenses, you should also follow the convention of always using the actual property name as 
the `Lens.id`.
:::

On the UI side, mapping is used to create small, dedicated stores for an input field. On the validation side, 
mapping is used to create dedicated inspectors that contain the same path to a data field.

Using this path, you can generate validation messages that can then be uniquely assigned to a data field in the UI, 
based on the path of the mapped store.

The Headless components leverage this exact mechanism, providing an automatic assignment of validation messages to 
a component and its store path, which is set in their `value` property.

An example of this can be seen in the API of a [CheckboxGroup](/headless/checkboxgroup/#checkboxgroupvalidationmessages).

### Delegating Validation in Sealed Hierarchies

Since sub-`Inspector`s are created using their `map` function and a `Lens`, lenses generated for sealed class 
hierarchies can be utilized during validation just like with a `Store`.

Let's modify the wish list example from the 
[store mapping chapter](/docs/storemapping/#dealing-with-sealed-type-hierarchies) by adding some validation logic:

```kotlin
// Since this example does not use fritz2-headless, we define a validation message type on our own:
data class Message(
    override val isError: Boolean,
    override val path: String,
    val message: String
) : ValidationMessage

// In order to add our validation logic, we take the `Wish` class from the store mapping example and add a `Validation` 
// to its companion object
@Lenses
sealed interface Wish {
    val label: String

    companion object {
        val validation: Validation<Wish, Unit, Message> = validation { inspector ->
            if (inspector.data.label.isEmpty()) {
//              ^^^^^^^^^^^^^^
//              Since all wishes share a common name property, we simply validate it without any special lenses.
//              We could also use a delegating lens.
                add(Message(isError = true, inspector.path, "Name is missing"))
            }

            // The rest of the validation depends on the concrete type of `Wish`.
            // Just like with the store mapping, we need to manually check its type before using the respective
            // up-casting lens!
            addAll(
                when (inspector.data) {
                    is Computer -> Computer.validate(inspector.map(Wish.computer()))
//                                 ^^^^^^^^^^^^^^^^^               ^^^^^^^^^^^^^^^
//                      We delegate validation to the actual type         |
//                                                                        |
//                                 Being sure the actual type is `Computer`, we may now use the up-casting
//                                 lens to create the fitting typed `Inspector<Computer>`-
                
                    is LightSaber -> LightSaber.validate(inspector.map(Wish.lightSaber()))
//                                   ^^^^^^^^^^^^^^^^^^^               ^^^^^^^^^^^^^^^^^ 
//                      We delegate validation to the actual type         |
//                                                                        |
//                                 Just like with `Computer`, we use the respective up-casting lens for
//                                 `LightSaber` to validate lightsaber properties.
                }
            )
        }
    }
}

@Lenses
data class Computer(
    override val label: String,
    val ramInKb: Int
) : Wish {
    override val typeName: String = "Computer"
    companion object {
        // type specific validation as usual inside the companion object
        val validate: Validation<Computer, Unit, Message> = validation { inspector ->
            val ram = inspector.map(Computer.ramInKb())
            if (ram.data < 4096) {
                add(Message(isError = false, inspector.path,"Warning Low amount of RAM"))
            }
        }
    }
}

@Lenses
data class LightSaber(
    override val label: String,
    val color: Color
) : Wish {
    override val typeName: String = "Lightsaber"
    companion object {
        val validate: Validation<LightSaber, Unit, Message> = validation { inspector ->
            val color = inspector.map(LightSaber.color())
            if (color.data == Color.Petrol) {
                add(Message(isError = true, inspector.path, "Light saber sadly cannot be petrol!"))
            }
        }
    }
}
```