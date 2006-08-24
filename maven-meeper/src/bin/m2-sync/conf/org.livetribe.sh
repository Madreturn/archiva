#!/bin/sh

FROM=mavensync@repository.codehaus.org:/repository/org/livetribe/
TO=org/livetribe/
SSH_OPTS="-i $HOME/.ssh/new-id_dsa"
#RSYNC_OPTS="-L"

## NOTE that codehaus only honours some rsync options. Others may be summarily discarded and/or cause the rsync to break - check
## with them if changing them
