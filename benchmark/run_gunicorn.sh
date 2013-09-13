#!/bin/bash

gunicorn openwater.wsgi:application -b 127.0.0.1:8888 -w 8
