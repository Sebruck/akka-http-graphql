#!/bin/bash
set -e

GPG_TTY=$(tty) sbt +publishSigned

sbt sonatypeRelease
