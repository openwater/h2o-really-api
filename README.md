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

[Jetty](http://www.eclipse.org/jetty/), [Simple](http://www.simpleframework.org/), and [Spark](http://www.sparkjava.com/) are supported.

Jetty:

    java -cp target/classes:"target/dependency/*" JettyServer

Simple:

    java -cp target/classes:"target/dependency/*" SimpleServer

Spark:

    java -cp target/classes:"target/dependency/*" SparkServer


Browse
------

For example:

    http://127.0.0.1:5000/?compact=true&page_size=2&page=1

Fast?
-----

I have done some basic benchmarking with [JMeter](http://jmeter.apache.org/) to see how we perform against the Python version.

For Python, I used:

* [Django](https://www.djangoproject.com/) 1.6 beta-2
* [Bjoern](https://github.com/jonashaag/bjoern) 1.3.4
* [Gunicorn](http://gunicorn.org/) 17.5

Java:

* Jetty 9.0.5.v20130815
* Simple Framework 5.1.5
* Spark 1.1 (internally using Jetty 9.0.2.v20130417)

The workload was 5 threads with a 1 second ramp-up and a loop count of 200 (giving an even 1000 requests for each server).
See `basic_test_plan.jmx`. I had the Servers configured as follows:

* Django dev server on port `8000` (baseline)
* Bjoern (8 workers) on `7777`
* Gunicorn (8 workers) on `8888`
* Simple on `5000`
* Jetty on `5050`
* Spark on `5555`

each tested in isolation.

For small responses, there isn't a huge difference (~30 req/s for Python vs ~80 req/s for Java).
However, for larger responses (e.g. `page_size=5000`), Python totally falls over and starts raising errors, but Java remains steady.

For example, even at `page_size=5000` on my laptop all Java implementations can sustain ~50 req/s.
Under those workloads, both Gunicorn and Bjoern dropped down to ~1.5 req/s with an error rate of ~25%.

At the extreme, a full export of all ~130k records via the API (in compact form; 16MB or so) takes ~1s with Java, and nearly a full minute in Python.

In all tests, all Java implementations showed similar numbers, with Jetty and Spark (which is based on Jetty) usually being marginally faster.

TODO
----

* ~~API response meta (total, previous / next, etc)~~
* ~~database connection pooling~~
* support for full measurement details
* ~~benchmarks~~
