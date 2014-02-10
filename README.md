Snore
=====

#What
After spending some time with [Cowboy](https://github.com/extend/cowboy) I find myself wishing I had a similar approach for REST in Scala.  Rather than a pile of nested things, implicits that conflict with other stuff, route files, etc, I wanted:

* a set of functions that are called to define a resource in a specific order
* asynchronous handling of requests by default (but let the handling functions be defined synchronously if so desired)
* not _another_ JSON library (I like json4s, you probably like something else.  Good!  Use that instead!)

#Warning

This is a *horribly incomplete experiment*.  This is just here for a few people to look at and for me to figure out if I care enough to continue or abandon this for sanity in Erlang.

#How
Look at [the example file](https://github.com/j14159/snore/blob/master/src/main/scala/com/noisycode/snore/Example.scala) located in, of course, a completely inappropriate area.

#Warning (again)
This is the result of screwing around for a few hours on a weekend.  Don't expect it to do anything awesome.

#Possibly Next
Immediate stuff that sucks that I'll get to unless despair drives me back to Erlang:

* no distinction between PUT/POST.  I'll likely split these into create/modify sections or something like that.  I'll see how I feel after putting some more time into [the spec](http://www.w3.org/Protocols/rfc2616/rfc2616.html).
* proper delete callbacks.  Using DELETE just throws an exception right now.
* query parameters.  Stupid easy to do, just haven't yet.
* lazy request body fetching
* implement at least a significant chunk of what Cowboy has in its [REST handler awesomeness](https://github.com/extend/cowboy/blob/master/guide/rest_handlers.md)
