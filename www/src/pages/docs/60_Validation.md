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
- a type describing the validation results (e.g., a message), which must implement the ValidationMessage interface

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
        val validation: Validation<Person, Unit, Message> = validation<Person, Message> { inspector ->
            val name = inspector.map(Person.name())
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
Person.validation(invalidPerson)
// gives a List of Messages:
// [Message(path=.name, severity=Error, text=Please provide a name), 
// Message(path=.name, severity=Warning, text=Is the person really older than 100 years‽)]
```

## Essentials

### Integrate validation into stores

Since fritz2 stores are the central entities for managing state, it is only natural to combine the update and 
validation processes. This is achieved using the specialized `ValidatingStore`, which is a subtype of the standard 
fritz2 `Store`.

By default, a `ValidatingStore` automatically validates its data after changes and updates the message list.
You can access these validation messages via `store.messages`: a `Flow<List<M>>` where `M` is your `ValidationMessage`
type. Handle this `Flow` like any other `Flow` of a `List` — for example, render it to HTML:

```kotlin
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

### Using Meta Data

TODO:
- adapt validation to some "context"; often UI-State or some data from a different subtree of the model
- validate only some sub model due to UI portion (data for `page1`)

### Validating Collections

TODO:
- show using the application of mapping Lenses inside `forEach`

### About Inspectors and Paths

TODO:
- inspectors are some kind od "readonly"-Stores
- constructs paths by applying Lenses as mapped stores do
- paths can be used to "select / filter" messages to show in the UI
- refer headless components and their `ComponentValidationMessage`

## Advanced Topics

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
            when (inspector.data) {
                is Computer -> {
                    val ram = inspector.map(Wish.computer().ramInKb())
//                                          ^^^^^^^^^^^^^^^^^^^^^^^^^
//                                          Being sure the actual type is `Computer`, we may now use the up-casting
//                                          lens. We may also chain it in order to validate a `Computer`-specific
//                                          property like `ramInKb`.
                    if (ram.data < 4096) {
                        add(Message(isError = false, inspector.path,"Warning Low amount of RAM"))
                    }
                }
                is LightSaber -> {
                    val color = inspector.map(Wish.lightSaber().color())
//                                            ^^^^^^^^^^^^^^^^^^^^^^^^^
//                                            Just like with `Computer`, we use the respective up-casting lens for
//                                            `LightSaber` to validate lightsaber properties.
                    if (color.data == Color.Red) {
                        add(Message(isError = true, inspector.path, "Light saber cannot be red!"))
                    }
                }
            }
        }
    }
}
```