# The Old Dog and Duck

A Clojure library designed to implement the ActivityPub protocol, obviously.

## Introduction

The Old Dog and Duck is clearly a pub, and it's a pub related to an activity; to whit, hunting ducks with dogs. Yes, of course one could also hunt dogs with ducks, but in practice that doesn't work so well. The point isn't whether or not I approve of hunting ducks with dogs (or vice versa); to be clear, I don't. The point is that it's a pub related to an activity, and is therefore an [ActivityPub](https://www.w3.org/TR/activitypub/).

Are we clear?

Good.

Let us proceed.

**The Old Dog and Duck** is intended to be a set of libraries to enable people to build stuff which interacts with ActivityPub. It isn't intended to be a replacement for, or clone of, Mastodon. I do think I might implement my own ActivityPub server on top of The Old Dog and Duck, that specifically might allow for user-pluggable feed-sorting algorithms and with my own user interface/user experience take, but that project is not this project.

## Architecture

There are a number of separate concerns required to implement ActivityPub. They include

1. Parsing ActivityStreams messages received from peers and from clients;
2. Persisting ActivityStreams objects;
3. Delivering ActivityStreams objects to peers;
4. Delivering ActivityStreams objects to clients.

**NOTE THAT** what Mastodon delivers to clients is not actually in ActivityStreams format; this seems to be an ad-hoc hack that's just never been fixed and has therefore become a de-facto standard for communication between ActivityPub hosts and their clients.

My proposal would be to deliver exactly the same ActivityStreams format to my client as to other servers. There may be a valid reason for not doing this, but if there is I will discover it in due course.

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

### Scratch

What the dog does when bored. Essentially, a place where I can learn how to make this stuff work, but perhaps eventually an ActivityPub server in its own right.

## Usage

FIXME

## License

Copyright © Simon Brooke, 2022 

This program and the accompanying materials are made available under the
terms of the GNU General Public License as published by the Free Software Foundation, either version 2 of the License, or (at your
option) any later version.
