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

* Jetty 7.6.0.v20120127
* Simple Framework 5.1.5

The workload was 5 threads with a 1 second ramp-up and a loop count of 200 (giving an even 1000 requests for each server).
See `basic_test_plan.jmx`. I had the Servers configured as follows:

* Django dev server on port `80` (baseline)
* Bjoern on `7777`
* Gunicorn on `8888`
* Simple on `5000`
* Jetty on `5050`

each run in isolation.

For small responses, there isn't a huge difference (~30 req/s for Python vs ~80 req/s for Java).
However, for larger responses (e.g. `page_size=5000`), Python totally falls over and starts raising errors, but Java remains steady.

For example, even at `page_size=5000` on my laptop both Java implementations can sustain ~18 req/s (with each returning ~6MB of JSON).
Under those workloads, both Gunicorn and Bjoern dropped down to ~1.5 req/s with an error rate of ~10%.

At the extreme, a full export of all ~130k records via the API (in compact form; 16MB or so) takes ~1s with Java, and nearly a full minute in Python.

In all tests, both Java implementations showed similar numbers, with Jetty usually being marginally slower.

TODO
----

* ~~API response meta (total, previous / next, etc)~~
* ~~database connection pooling~~
* support for full measurement details
* ~~benchmarks~~
