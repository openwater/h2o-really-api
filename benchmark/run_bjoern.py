#!/usr/bin/env python
# -*- coding: utf-8 -*-
import bjoern
import os
import signal
from openwater import wsgi


NUM_WORKERS = 8
worker_pids = []


bjoern.listen(wsgi.application, '127.0.0.1', 7777)
for _ in xrange(NUM_WORKERS):
    pid = os.fork()
    if pid > 0:
        # in master
        worker_pids.append(pid)
    elif pid == 0:
        # in worker
        try:
            bjoern.run()
        except KeyboardInterrupt:
            pass
        exit()

try:
    for _ in xrange(NUM_WORKERS):
        os.wait()
except KeyboardInterrupt:
    for pid in worker_pids:
        os.kill(pid, signal.SIGINT)
