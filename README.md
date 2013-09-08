H2O API
=======

High-performance HTTP API for H2O-Really.

Build
-----

`mvn package`

Run
---

Set environment variables `PORT` and `DATABASE_URL`, e.g.:

    export DATABASE_URL=postgresql://you:yourpass@localhost:5432/h2o_really
    export PORT=5000

Both [Jetty](http://www.eclipse.org/jetty/) and [Simple](http://www.simpleframework.org/) are supported.

Jetty:

    java -cp target/classes:"target/dependency/*" JettyServer

Simple:

    java -cp target/classes:"target/dependency/*" SimpleServer

TODO
----

* API response meta (total, previous / next, etc)
* database connection pooling
* support for full measurement details
* benchmarks