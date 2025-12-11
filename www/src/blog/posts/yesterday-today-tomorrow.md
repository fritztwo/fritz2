---
layout: layouts/post.njk
image: yesterday-today-tomorrow.png
type: article
title: Yesterday - Today - Tomorrow.
description: "fritz2's development has moved from a single project into a dedicated organization. We want to explain
and inform the community about the reasons and chances about this change. TL;TR: The development continues and there
should be no impact for you as user!"
date: 2025-12-08
author: chausknecht
readtime: 5
---

## TL;TR:

- Snapshots are available from a new repository: [https://central.sonatype.com/repository/maven-snapshots/](https://central.sonatype.com/repository/maven-snapshots/)
- There is a new [organization](https://github.com/fritztwo) to gather all official fritz2 related projects. 


![yesterday-today-tomorrow](/img/yesterday-today-tomorrow.png)

## Yesterday

In late 2019, Jens Stegemann and Jan Weidenhaupt started investigating the possibility of creating web-driven frontends
in Kotlin using the brand-new multiplatform approach (which was barely in alpha state at the time).
Their goal was to streamline internal development at the insurance company where they worked.

The dream was to use a single language for both the backend (which was almost pure Java at the time) and the frontend
(a mixture of JavaScript-based frameworks and some experimental Scala approaches).

Kotlin's multiplatform vision seemed like the game-changer needed to achieve this!

If only there had been a web framework available—but that simply wasn't the case...

...you all know the result: fritz2 was born and has been actively developed and polished ever since. 
Over time, new developers were attracted to the project and joined the core development team.

## Today

The initial dream has actually become reality: The technology stack is now streamlined, and there are dozens of 
fritz2-based UI applications and backend services (using fritz2 validation) in production—including those by 
third-party organizations!

Meanwhile, fritz2 has become solid, stable, and works reliably in production.

As time went by, personal interests and professional duties changed. Consequently, both Jens and Jan left the company
to move on to other projects and technologies.

However, fritz2 development continued, as the core team was already strong enough to compensate for their departure.

In fact, we hope you haven't noticed this change at all until now, as this has been the status quo for the last two years.

To reflect these changes and simplify deployment for the core devs, we recently decided to create a new GitHub 
organization to bundle all official fritz2 projects in one place. So, please check out the
[organization](https://github.com/fritztwo) if you haven't done so yet.

You can find the core framework and the two template projects there. There are also some smaller projects that act 
as helpers or playgrounds for future developments.

This website's URL and the Maven Central repository remain the same! As mentioned, these changes are practically
transparent to you as a framework user. You do not need to change anything inside your projects, besides removing the
outdated repository for *SNAPSHOTs*, and use this 
[one instead](https://central.sonatype.com/repository/maven-snapshots/), if you have not already.

## Tomorrow

fritz2 is still on the road to version 1.0. We believe there isn't much work left to do, mostly polishing missing 
API features and documentation. We look forward to reaching this milestone soon — no later than 2026, we promise!

We also have plans for the evolution of the framework after the 1.0 release. These will cover core topics, such as 
reworking the store's behavior, as well as ideas for adding headless components and expanding the capabilities 
of the lenses-generator.

So stay tuned and curious about the upcoming releases!

(Trivia: The title of this article is derived from the 
[German title](https://de.wikipedia.org/wiki/Gestern,_heute,_morgen) of the brilliant series finale of 
Star Trek: The Next Generation. It is fitting—and our saga continues 😉)

## Final words

As this shift to an organization marks the final step of a smooth handover to new developers, we are happy to 
share a final message from fritz2 initiator Jens Stegemann:

> When we started this project, our aim was to craft a lightweight and easy-to-use web framework based on built-in 
> Kotlin technologies to enable a consistent and pleasant user experience throughout the different software layers.
> fritz2 filled that original gap.
>
> It is always hard to give up a project and move on, but I am very grateful that we were able to open up
> development early on and attract new contributors over time, ensuring development could continue without me and Jan.
>
> I am happy that fritz2 is in the best hands possible!

Thank you, Jens and Jan, for your passion and dedication over the years!