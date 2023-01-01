# The Old Dog and Duck

A Clojure library designed to implement the ActivityPub protocol, obviously.

![The Dog and Duck, St George's Fields, London, 1647](https://simon-brooke.github.io/dog-and-duck/images/Dog_and_Duck_tavern.jpg)

## Introduction

The Old Dog and Duck is clearly a pub, and it's a pub related to an activity; to whit, hunting ducks with dogs. Yes, of course one could also hunt dogs with ducks, but in practice that doesn't work so well. The point isn't whether or not I approve of hunting ducks with dogs (or vice versa); to be clear, I don't. The point is that it's a pub related to an activity, and is therefore an [ActivityPub](https://www.w3.org/TR/activitypub/).

Are we clear?

Good.

Let us proceed.

**The Old Dog and Duck** is intended to be a set of libraries to enable people to build stuff which interacts with ActivityPub. It isn't intended to be a replacement for, or clone of, Mastodon. I do think I might implement my own ActivityPub server on top of The Old Dog and Duck, that specifically might allow for user-pluggable feed-sorting algorithms and with my own user interface/user experience take, but that project is not (yet, at any rate) this project.

## Status

This is a long way pre-alpha. Everything will change. Feel free to play, but do so at your own risk. Contributions welcome.

## Architecture

There are a number of separate concerns required to implement ActivityPub. They include

1. Parsing ActivityStreams messages received from peers and from clients;
2. Persisting ActivityStreams objects;
3. Delivering ActivityStreams objects to peers;
4. Delivering ActivityStreams objects to clients.

## Some motivations

### Empowering users

The [ActivityPub spec](https://www.w3.org/TR/activitypub/#Overview) starts by saying:

> ActivityPub provides two layers:
>
>   1. A server to server federation protocol (so decentralized websites can share information)
>   2. A client to server protocol (so users, including real-world users, bots, and other automated processes, can communicate with ActivityPub using their accounts on servers, from a phone or desktop or web application or whatever).

I'm interested in driving much more functionality down to the the client, for example feed ordering and presentation. This would allow users, for example, to choose their own (or roll their own) feed-ordering algorithms.

My proposal would be to deliver exactly the same ActivityStreams format to my client as to other servers. There may be a valid reason for not doing this, but if there is I will discover it in due course.

### Enhanced resiliency

The [ActivityStreams](https://www.w3.org/TR/activitystreams-core/) spec seems predicated on 'always up' communication between at least servers, which is perhaps why there is a two tier network of 'servers' and 'clients'. It also depends on HTTPS certificates to identify servers, which implies it's vulnerable to disruption by a hostile actor with the ability to revoke certificates.

My own history with social media dates back to Usenet over UUCP, a system designed explicitly for intermittent low bandwidth connections; such a system is immensely resilient in the face of disruption to infrastructure.

Social media is useful to concerted popular action in periods of disruption, whether in the case of civil ememrgency such as earthquakes, wild fires and floods, in the case of wars, or in the case of intrusive surveillance by authoritarian governments. But to be useful in such situations it needs to be resilient, and one of the things it needs to be resilient to is parts of the network being intermittently available, or requiring rerouting.

In this I'm influenced by and hope to try to implement ideas from [Ian Clarke](https://blog.locut.us/about/)'s [Freenet](https://en.wikipedia.org/wiki/Freenet) and [Tahrir](http://tahrirproject.org/) projects, especially [webs of trust](https://en.wikipedia.org/wiki/Web_of_trust).

To be clear, it is important for The Old Dog and Duck to be able to interact with other existing 'vanilla' ActivityStreams implementations, but I hope to experiment with enhanced communication between Dog and Duck servers to provide more FreeNet-like resiliency.

## Proposed dog-and-duck libraries

**NOTE THAT** at the present stage all the proposed libraries are in one package, namely this package, but that it is proposed that in future they will form separate libraries in separate packages.

### Bar

Where conversations happen. Handle interactions with clients.

### Cellar

Where things are stored. Persistance for ActivityStreams objects; I may at least initially simply copy the Mastodon postgres schema, but equally I may not.

### Pantry

Where deliveries are ordered and arrive; and from where deliveries onwards are despatched. Handle interactions with peers.

### Quack

Duck-typing for ActivityStreams objects.

As of version 0.1.0, this is substantially the only part that is yet at all useful, and it is still a long way from finished or robust.

### Bouncer

Enhanced tools for moderators (I have as yet absolutely no idea what this looks like).

### Scratch

What the dog does when bored. Essentially, a place where I can learn how to make this stuff work, but perhaps eventually an ActivityPub server in its own right.

## Usage

At present, only the duck-typing functions work. To play with them, use

```clojure
(require '[dog-and-duck.quack.quack :as q])
```

## Documentation

Full documentation is [here](https://simon-brooke.github.io/dog-and-duck/).

## Building

### clj-activitypub

**NOTE THAT** `dog-and-duck` depends on Jahfer's `clj-activitypub`, which is also currently not yet released and under rapid development and consequently currently *very* unstable. For this reason it's probably best to clone [my fork](https://github.com/simon-brooke/clj-activitypub) rather than [the original](https://github.com/jahfer/clj-activitypub), because that way you are less likely to encounter version incompatibilities.

`clj-activitypub` is configured to build with [tools.bui;d](https://clojure.org/guides/tools_build). To prepare `clj-activitypub` before building `dog-and-duck`, do

```bash
$ git clone git@github.com:simon-brooke/clj-activitypub.git
$ cd clj-activitypub/
$ clj -T:build jar
$ clj -T:build install
```

I shall keep `dog-and-duck` and my fork of `clj-activitypub` in sync at least until Jahfer makes a production release of his project to [Clojars]().

### Leiningen

`dog-and-duck` itself is still set up to build with [Leiningen](https://leiningen.org/). Yes, I know that's not what the cool kids are using any more but hey, I'm an old man, leave me be. To get `dog-and-duck` up to a point where you can start to play,

```bash
$ git clone git@github.com:simon-brooke/dog-and-duck.git
$ cd dog-and-duck
$ lein repl
```

## Testing

Prior to testing, you should clone [activitystreams-test-documents](https://github.com/w3c-social/activitystreams-test-documents) into the `resources` directory. You can then test with

```bash
lein test
```

## License

Copyright © Simon Brooke, 2022.

This program and the accompanying materials are made available under the
terms of the GNU General Public License as published by the Free Software Foundation, either version 2 of the License, or (at your
option) any later version.
