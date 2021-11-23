# LWWElementGraph

Implementation of Last-Element-Wins Element Graph, which is an example of [Conflict-Free Resolution Data Type] (https://en.wikipedia.org/wiki/Conflict-free_replicated_data_type)

The main idea is that this data structure can be used in distributed systems, where each structure can be updated concurrently and independently, and it's always possible to resolve
any conflicts between them mathematically. 


This project covers such concepts like:
- [CRDT] (https://en.wikipedia.org/wiki/Conflict-free_replicated_data_type), specifically state-based
- Strong eventual consistency
- Commutativity/Associativity/Idempotency of operations

I found this kind of data structures interesting, as implementing them requires a paradigm shift, as for such structures one can remove structural items 
(e.g. vertex/edge for a graph) even BEFORE they are added. All operations on such structures should be associative, commutative and idempotent, effectively 
making the order of operations irrelevant. 
